package com.kaiju.backend.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

// Cuerpo de todas las respuestas de error. "campos" solo viene en errores de validación.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String mensaje, Map<String, String> campos) {
}
