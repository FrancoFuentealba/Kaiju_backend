package com.kaiju.backend.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

// Error de negocio que se convierte en una respuesta HTTP con el formato { mensaje, campos }.
public class ApiException extends RuntimeException {

    private final HttpStatus estado;
    private final Map<String, String> campos;

    public ApiException(HttpStatus estado, String mensaje, Map<String, String> campos) {
        super(mensaje);
        this.estado = estado;
        this.campos = campos;
    }

    public HttpStatus getEstado() {
        return estado;
    }

    public Map<String, String> getCampos() {
        return campos;
    }

    // 404
    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(HttpStatus.NOT_FOUND, mensaje, null);
    }

    // 409
    public static ApiException conflicto(String mensaje, Map<String, String> campos) {
        return new ApiException(HttpStatus.CONFLICT, mensaje, campos);
    }

    // 400
    public static ApiException invalido(String mensaje, Map<String, String> campos) {
        return new ApiException(HttpStatus.BAD_REQUEST, mensaje, campos);
    }

    // 422
    public static ApiException stockInsuficiente(String mensaje) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, mensaje, null);
    }
}
