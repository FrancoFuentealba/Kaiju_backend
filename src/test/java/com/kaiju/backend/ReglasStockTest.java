package com.kaiju.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kaiju.backend.model.ReglasStock;
import com.kaiju.backend.model.TipoMovimiento;
import org.junit.jupiter.api.Test;

class ReglasStockTest {

    @Test
    void entradaSuma() {
        assertEquals(15, ReglasStock.calcularStockResultante(10, TipoMovimiento.ENTRADA, 5));
    }

    @Test
    void salidaYVentaRestan() {
        assertEquals(6, ReglasStock.calcularStockResultante(10, TipoMovimiento.SALIDA, 4));
        assertEquals(7, ReglasStock.calcularStockResultante(10, TipoMovimiento.VENTA, 3));
    }

    @Test
    void ajusteFijaElStockAlConteo() {
        assertEquals(3, ReglasStock.calcularStockResultante(10, TipoMovimiento.AJUSTE, 3));
    }

    @Test
    void stockBajoCuandoLlegaOCaeBajoElMinimo() {
        assertTrue(ReglasStock.stockBajo(5, 5));
        assertTrue(ReglasStock.stockBajo(2, 5));
        assertFalse(ReglasStock.stockBajo(6, 5));
    }
}
