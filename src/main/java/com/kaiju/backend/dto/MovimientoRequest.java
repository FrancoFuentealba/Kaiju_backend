package com.kaiju.backend.dto;

import com.kaiju.backend.model.TipoMovimiento;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Cuerpo de POST /productos/{id}/movimientos. El servidor asigna id, fecha y stockResultante.
// En un AJUSTE, "cantidad" es el stock contado (el nuevo stock).
public record MovimientoRequest(
        @NotNull(message = "Ingresa el tipo de movimiento")
        TipoMovimiento tipo,

        @NotNull(message = "Ingresa la cantidad")
        @Min(value = 0, message = "La cantidad no puede ser negativa")
        @Max(value = 1_000_000, message = "No puede superar 1000000")
        Integer cantidad,

        @Size(max = 120, message = "Máximo 120 caracteres")
        String nota
) {
}
