package com.kaiju.backend.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String OBLIGATORIO = "Campo obligatorio";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> negocio(ApiException ex) {
        return ResponseEntity.status(ex.getEstado()).body(new ApiError(ex.getMessage(), ex.getCampos()));
    }

    // Falla una regla de @Valid: 400 con un mensaje por campo.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            String mensaje = error.getDefaultMessage() == null ? "Valor inválido" : error.getDefaultMessage();
            // Si un campo falla varias reglas, se prefiere "Campo obligatorio".
            campos.merge(error.getField(), mensaje,
                    (actual, nuevo) -> OBLIGATORIO.equals(actual) ? actual : (OBLIGATORIO.equals(nuevo) ? nuevo : actual));
        }
        return ResponseEntity.badRequest().body(new ApiError("Hay campos con errores", campos));
    }

    // JSON mal formado o con un valor que no corresponde (por ejemplo un tipo de movimiento inexistente).
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> jsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ApiError("El cuerpo de la petición no es válido", null));
    }

    // Por ejemplo /productos/abc (el id no es un número).
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(new ApiError("Parámetro inválido: " + ex.getName(), null));
    }

    // Dos peticiones crearon el mismo código al mismo tiempo: lo frena la restricción única de la base.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integridad(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("Ya existe un producto con ese código", Map.of("codigo", "Ya existe un producto con ese código")));
    }
}
