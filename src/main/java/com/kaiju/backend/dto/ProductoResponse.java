package com.kaiju.backend.dto;

import com.kaiju.backend.model.Producto;

public record ProductoResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        String categoria,
        String tipo,
        int precio,
        int stock,
        int stockMinimo,
        String talla,
        String color,
        String modelo,
        String lote,
        String vencimiento
) {
    public static ProductoResponse de(Producto p) {
        return new ProductoResponse(
                p.getId(), p.getCodigo(), p.getNombre(), p.getDescripcion(), p.getCategoria(), p.getTipo(),
                p.getPrecio(), p.getStock(), p.getStockMinimo(),
                p.getTalla(), p.getColor(), p.getModelo(), p.getLote(), p.getVencimiento()
        );
    }
}
