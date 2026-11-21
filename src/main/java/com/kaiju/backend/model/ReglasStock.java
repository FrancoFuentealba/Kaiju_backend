package com.kaiju.backend.model;

// Reglas del stock (misma lógica que ReglasStock de la app). Sin dependencias: se prueba con un test simple.
public final class ReglasStock {

    private ReglasStock() {
    }

    // Stock que queda después de aplicar un movimiento.
    public static int calcularStockResultante(int stockActual, TipoMovimiento tipo, int cantidad) {
        return switch (tipo) {
            case ENTRADA -> stockActual + cantidad;
            case SALIDA, VENTA -> stockActual - cantidad;
            case AJUSTE -> cantidad; // el ajuste fija el stock al conteo real
        };
    }

    // Un producto tiene stock bajo cuando llega o cae por debajo de su stock mínimo.
    public static boolean stockBajo(int stock, int stockMinimo) {
        return stock <= stockMinimo;
    }
}
