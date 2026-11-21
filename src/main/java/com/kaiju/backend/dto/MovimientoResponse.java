package com.kaiju.backend.dto;

import com.kaiju.backend.model.Movimiento;
import com.kaiju.backend.model.TipoMovimiento;

public record MovimientoResponse(
        Long id,
        Long productoId,
        TipoMovimiento tipo,
        int cantidad,
        int stockResultante,
        long fecha,
        String nota
) {
    public static MovimientoResponse de(Movimiento m) {
        return new MovimientoResponse(
                m.getId(), m.getProductoId(), m.getTipo(), m.getCantidad(),
                m.getStockResultante(), m.getFecha(), m.getNota()
        );
    }
}
