package com.kaiju.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Cuerpo de POST /productos y PUT /productos/{id}. Las reglas son las mismas que valida la app.
// "stockInicial" solo se usa al crear. El stock nunca se recibe: solo cambia con movimientos.
public record ProductoRequest(
        @NotBlank(message = "Campo obligatorio")
        @Pattern(regexp = "^[A-Za-z0-9-]{3,20}$", message = "Usa 3 a 20 letras, números o guiones (ej: KJ-0012)")
        String codigo,

        @NotBlank(message = "Campo obligatorio")
        @Size(min = 3, max = 60, message = "Debe tener entre 3 y 60 caracteres")
        String nombre,

        @Size(max = 200, message = "Máximo 200 caracteres")
        String descripcion,

        @NotBlank(message = "Campo obligatorio")
        @Size(min = 2, max = 30, message = "Debe tener entre 2 y 30 caracteres")
        String categoria,

        @NotBlank(message = "Campo obligatorio")
        @Pattern(regexp = "Producto terminado|Insumo|Perecible", message = "Tipo inválido")
        String tipo,

        @NotNull(message = "Campo obligatorio")
        @Min(value = 1, message = "Debe ser al menos 1")
        @Max(value = 10_000_000, message = "No puede superar 10000000")
        Integer precio,

        @NotNull(message = "Campo obligatorio")
        @Min(value = 0, message = "Debe ser al menos 0")
        @Max(value = 1_000_000, message = "No puede superar 1000000")
        Integer stockMinimo,

        @Min(value = 0, message = "Debe ser al menos 0")
        @Max(value = 1_000_000, message = "No puede superar 1000000")
        Integer stockInicial,

        @Size(max = 20, message = "Máximo 20 caracteres") String talla,
        @Size(max = 20, message = "Máximo 20 caracteres") String color,
        @Size(max = 30, message = "Máximo 30 caracteres") String modelo,
        @Size(max = 20, message = "Máximo 20 caracteres") String lote,
        String vencimiento
) {
}
