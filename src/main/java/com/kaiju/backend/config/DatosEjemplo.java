package com.kaiju.backend.config;

import com.kaiju.backend.dto.MovimientoRequest;
import com.kaiju.backend.dto.ProductoRequest;
import com.kaiju.backend.model.TipoMovimiento;
import com.kaiju.backend.repository.ProductoRepository;
import com.kaiju.backend.service.InventarioService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// Carga datos FICTICIOS la primera vez (el caso no permite datos reales).
// Se desactiva con kaiju.seed=false.
@Component
@ConditionalOnProperty(name = "kaiju.seed", havingValue = "true")
public class DatosEjemplo implements CommandLineRunner {

    private final ProductoRepository productos;
    private final InventarioService servicio;

    public DatosEjemplo(ProductoRepository productos, InventarioService servicio) {
        this.productos = productos;
        this.servicio = servicio;
    }

    @Override
    public void run(String... args) {
        if (productos.count() > 0) {
            return;
        }
        crear("KJ-0001", "Polera algodón básica", "Ropa", "Producto terminado", 9990, 10, 40, "M", "Negro", null);
        crear("KJ-0002", "Polerón con capucha", "Ropa", "Producto terminado", 24990, 6, 25, "L", "Gris", null);
        crear("KJ-0003", "Cuaderno universitario", "Papelería", "Producto terminado", 2490, 15, 60, null, null, null);
        crear("KJ-0004", "Caja de cartón mediana", "Embalaje", "Insumo", 890, 50, 200, null, null, null);
        crear("KJ-0005", "Yogurt natural 1 L", "Alimentos", "Perecible", 1890, 12, 30, null, null, "2027-01-20");
        crear("KJ-0006", "Mochila escolar", "Accesorios", "Producto terminado", 19990, 5, 18, null, "Azul", null);

        // Algunas ventas para que haya historial y productos con stock bajo.
        vender("KJ-0001", 32);
        vender("KJ-0002", 21);
        vender("KJ-0005", 22);
        vender("KJ-0006", 4);
    }

    private void crear(String codigo, String nombre, String categoria, String tipo, int precio,
                       int stockMinimo, int stockInicial, String talla, String color, String vencimiento) {
        servicio.crear(new ProductoRequest(codigo, nombre, "", categoria, tipo, precio, stockMinimo,
                stockInicial, talla, color, null, null, vencimiento));
    }

    private void vender(String codigo, int cantidad) {
        productos.findAll().stream()
                .filter(p -> p.getCodigo().equals(codigo))
                .findFirst()
                .ifPresent(p -> servicio.registrarMovimiento(p.getId(),
                        new MovimientoRequest(TipoMovimiento.VENTA, cantidad, "Venta de ejemplo")));
    }
}
