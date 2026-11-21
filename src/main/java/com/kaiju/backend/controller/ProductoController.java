package com.kaiju.backend.controller;

import com.kaiju.backend.dto.MovimientoRequest;
import com.kaiju.backend.dto.MovimientoResponse;
import com.kaiju.backend.dto.ProductoRequest;
import com.kaiju.backend.dto.ProductoResponse;
import com.kaiju.backend.service.InventarioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ProductoController {

    private final InventarioService servicio;

    public ProductoController(InventarioService servicio) {
        this.servicio = servicio;
    }

    // GET /productos?busqueda=texto&soloStockBajo=true
    @GetMapping("/productos")
    public List<ProductoResponse> listar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(defaultValue = "false") boolean soloStockBajo) {
        return servicio.listar(busqueda, soloStockBajo);
    }

    @GetMapping("/productos/{id}")
    public ProductoResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping("/productos")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest cuerpo) {
        return servicio.crear(cuerpo);
    }

    @PutMapping("/productos/{id}")
    public ProductoResponse editar(@PathVariable Long id, @Valid @RequestBody ProductoRequest cuerpo) {
        return servicio.editar(id, cuerpo);
    }

    @DeleteMapping("/productos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }

    @GetMapping("/productos/{id}/movimientos")
    public List<MovimientoResponse> movimientosDe(@PathVariable Long id) {
        return servicio.movimientosDe(id);
    }

    @PostMapping("/productos/{id}/movimientos")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoResponse registrarMovimiento(@PathVariable Long id, @Valid @RequestBody MovimientoRequest cuerpo) {
        return servicio.registrarMovimiento(id, cuerpo);
    }

    @GetMapping("/movimientos")
    public List<MovimientoResponse> todosLosMovimientos() {
        return servicio.todosLosMovimientos();
    }
}
