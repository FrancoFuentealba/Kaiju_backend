package com.kaiju.backend.service;

import com.kaiju.backend.dto.MovimientoRequest;
import com.kaiju.backend.dto.MovimientoResponse;
import com.kaiju.backend.dto.ProductoRequest;
import com.kaiju.backend.dto.ProductoResponse;
import com.kaiju.backend.exception.ApiException;
import com.kaiju.backend.model.Movimiento;
import com.kaiju.backend.model.Producto;
import com.kaiju.backend.model.ReglasStock;
import com.kaiju.backend.model.TipoMovimiento;
import com.kaiju.backend.repository.MovimientoRepository;
import com.kaiju.backend.repository.ProductoRepository;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Lógica del inventario. Aquí viven las reglas: el stock solo cambia con movimientos,
// y el movimiento y el cambio de stock se guardan en una sola transacción.
@Service
@Transactional
public class InventarioService {

    private final ProductoRepository productos;
    private final MovimientoRepository movimientos;

    public InventarioService(ProductoRepository productos, MovimientoRepository movimientos) {
        this.productos = productos;
        this.movimientos = movimientos;
    }

    // ---------- Productos ----------

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(String busqueda, boolean soloStockBajo) {
        String q = busqueda == null ? "" : busqueda.trim();
        return productos.buscar(q, soloStockBajo).stream().map(ProductoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return ProductoResponse.de(buscar(id));
    }

    public ProductoResponse crear(ProductoRequest r) {
        String vencimiento = validarVencimiento(r.vencimiento());
        String codigo = normalizarCodigo(r.codigo());
        if (productos.existsByCodigo(codigo)) {
            throw codigoDuplicado(codigo);
        }
        Producto p = new Producto();
        aplicar(p, r, codigo, vencimiento);
        p.setStock(0);
        p = productos.save(p);

        // El stock inicial no se escribe directo: pasa por un movimiento para tener trazabilidad.
        int inicial = r.stockInicial() == null ? 0 : r.stockInicial();
        if (inicial > 0) {
            registrar(p, TipoMovimiento.ENTRADA, inicial, "Stock inicial");
        }
        return ProductoResponse.de(p);
    }

    public ProductoResponse editar(Long id, ProductoRequest r) {
        Producto p = buscar(id);
        String vencimiento = validarVencimiento(r.vencimiento());
        String codigo = normalizarCodigo(r.codigo());
        if (productos.existsByCodigoAndIdNot(codigo, id)) {
            throw codigoDuplicado(codigo);
        }
        aplicar(p, r, codigo, vencimiento); // no toca el stock
        return ProductoResponse.de(productos.save(p));
    }

    public void eliminar(Long id) {
        buscar(id);
        movimientos.deleteByProductoId(id);
        productos.deleteById(id);
    }

    // ---------- Movimientos ----------

    @Transactional(readOnly = true)
    public List<MovimientoResponse> movimientosDe(Long productoId) {
        buscar(productoId);
        return movimientos.findByProductoIdOrderByFechaDescIdDesc(productoId).stream()
                .map(MovimientoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<MovimientoResponse> todosLosMovimientos() {
        return movimientos.findAllByOrderByFechaDescIdDesc().stream().map(MovimientoResponse::de).toList();
    }

    public MovimientoResponse registrarMovimiento(Long productoId, MovimientoRequest r) {
        Producto p = productos.buscarParaActualizar(productoId)
                .orElseThrow(() -> ApiException.noEncontrado("El producto no existe"));
        int cantidad = r.cantidad();
        TipoMovimiento tipo = r.tipo();

        if (tipo != TipoMovimiento.AJUSTE) {
            if (cantidad <= 0) {
                throw ApiException.invalido("Hay campos con errores",
                        Map.of("cantidad", "La cantidad debe ser mayor a 0"));
            }
            int resultante = ReglasStock.calcularStockResultante(p.getStock(), tipo, cantidad);
            if (resultante < 0) {
                throw ApiException.stockInsuficiente(
                        "Stock insuficiente: hay " + p.getStock() + " unidades disponibles");
            }
        }
        String nota = r.nota() == null ? "" : r.nota().trim();
        return MovimientoResponse.de(registrar(p, tipo, cantidad, nota));
    }

    // ---------- Internos ----------

    // Guarda el movimiento y actualiza el stock (dentro de la transacción del llamador).
    private Movimiento registrar(Producto p, TipoMovimiento tipo, int cantidad, String nota) {
        int nuevoStock = ReglasStock.calcularStockResultante(p.getStock(), tipo, cantidad);
        Movimiento m = new Movimiento();
        m.setProductoId(p.getId());
        m.setTipo(tipo);
        m.setCantidad(cantidad);
        m.setStockResultante(nuevoStock);
        m.setFecha(System.currentTimeMillis());
        m.setNota(nota);
        m = movimientos.save(m);
        p.setStock(nuevoStock);
        productos.save(p);
        return m;
    }

    private Producto buscar(Long id) {
        return productos.findById(id).orElseThrow(() -> ApiException.noEncontrado("El producto no existe"));
    }

    private void aplicar(Producto p, ProductoRequest r, String codigo, String vencimiento) {
        p.setCodigo(codigo);
        p.setNombre(r.nombre().trim());
        p.setDescripcion(r.descripcion() == null ? "" : r.descripcion().trim());
        p.setCategoria(r.categoria().trim());
        p.setTipo(r.tipo());
        p.setPrecio(r.precio());
        p.setStockMinimo(r.stockMinimo());
        p.setTalla(opcional(r.talla()));
        p.setColor(opcional(r.color()));
        p.setModelo(opcional(r.modelo()));
        p.setLote(opcional(r.lote()));
        p.setVencimiento(vencimiento);
    }

    private static String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    private static String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    // Fecha opcional: si viene, debe ser una fecha real AAAA-MM-DD.
    private static String validarVencimiento(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim()).toString();
        } catch (DateTimeParseException e) {
            throw ApiException.invalido("Hay campos con errores",
                    Map.of("vencimiento", "Fecha inválida. Usa el formato AAAA-MM-DD (ej: 2027-01-20)"));
        }
    }

    private static ApiException codigoDuplicado(String codigo) {
        return ApiException.conflicto("Ya existe un producto con el código " + codigo,
                Map.of("codigo", "Ya existe un producto con ese código"));
    }
}
