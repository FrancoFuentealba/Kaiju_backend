package com.kaiju.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

// Pruebas de la API completa sobre H2 en memoria. Cada test se revierte al terminar (@Transactional).
@SpringBootTest(properties = {
        "kaiju.seed=false",
        "spring.datasource.url=jdbc:h2:mem:kaiju_test;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@Transactional
class InventarioApiTest {

    @Autowired
    private MockMvc mvc;

    private static String cuerpoProducto(String codigo, String nombre, int stockMinimo, int stockInicial) {
        return """
                {"codigo":"%s","nombre":"%s","descripcion":"","categoria":"Ropa","tipo":"Producto terminado",
                 "precio":9990,"stockMinimo":%d,"stockInicial":%d}
                """.formatted(codigo, nombre, stockMinimo, stockInicial);
    }

    private long crear(String codigo, int stockMinimo, int stockInicial) throws Exception {
        String respuesta = mvc.perform(post("/api/v1/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoProducto(codigo, "Polera basica", stockMinimo, stockInicial)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private void movimiento(long id, String tipo, int cantidad) throws Exception {
        mvc.perform(post("/api/v1/productos/" + id + "/movimientos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tipo\":\"" + tipo + "\",\"cantidad\":" + cantidad + ",\"nota\":\"prueba\"}"));
    }

    @Test
    void crearProductoConStockInicialRegistraUnaEntrada() throws Exception {
        long id = crear("KJ-100", 5, 20);

        mvc.perform(get("/api/v1/productos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(20))
                .andExpect(jsonPath("$.codigo").value("KJ-100"));

        mvc.perform(get("/api/v1/productos/" + id + "/movimientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("ENTRADA"))
                .andExpect(jsonPath("$[0].nota").value("Stock inicial"))
                .andExpect(jsonPath("$[0].stockResultante").value(20));
    }

    @Test
    void codigoDuplicadoDevuelve409() throws Exception {
        crear("KJ-200", 5, 0);
        mvc.perform(post("/api/v1/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoProducto("kj-200", "Otro", 5, 0)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campos.codigo").exists());
    }

    @Test
    void datosInvalidosDevuelven400ConUnMensajePorCampo() throws Exception {
        mvc.perform(post("/api/v1/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"!!","nombre":"","categoria":"Ropa","tipo":"Producto terminado",
                                 "precio":0,"stockMinimo":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.codigo").exists())
                .andExpect(jsonPath("$.campos.nombre").value("Campo obligatorio"))
                .andExpect(jsonPath("$.campos.precio").exists());
    }

    @Test
    void vencimientoInvalidoDevuelve400() throws Exception {
        mvc.perform(post("/api/v1/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"KJ-300","nombre":"Yogurt","categoria":"Alimentos","tipo":"Perecible",
                                 "precio":1890,"stockMinimo":5,"vencimiento":"2027-13-45"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.vencimiento").exists());
    }

    @Test
    void productoInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/v1/productos/999999")).andExpect(status().isNotFound());
    }

    @Test
    void ventaDescuentaStockYGuardaElMovimiento() throws Exception {
        long id = crear("KJ-400", 5, 20);

        mvc.perform(post("/api/v1/productos/" + id + "/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"VENTA\",\"cantidad\":4,\"nota\":\"mostrador\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockResultante").value(16))
                .andExpect(jsonPath("$.productoId").value(id));

        mvc.perform(get("/api/v1/productos/" + id))
                .andExpect(jsonPath("$.stock").value(16));
    }

    @Test
    void ventaMayorAlStockDevuelve422YNoCambiaElStock() throws Exception {
        long id = crear("KJ-500", 1, 3);

        mvc.perform(post("/api/v1/productos/" + id + "/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"VENTA\",\"cantidad\":5}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje").value("Stock insuficiente: hay 3 unidades disponibles"));

        mvc.perform(get("/api/v1/productos/" + id)).andExpect(jsonPath("$.stock").value(3));
    }

    @Test
    void cantidadCeroEnUnaVentaDevuelve400() throws Exception {
        long id = crear("KJ-550", 1, 3);
        mvc.perform(post("/api/v1/productos/" + id + "/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"VENTA\",\"cantidad\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.cantidad").exists());
    }

    @Test
    void ajusteFijaElStockAlConteo() throws Exception {
        long id = crear("KJ-600", 5, 20);
        mvc.perform(post("/api/v1/productos/" + id + "/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"AJUSTE\",\"cantidad\":7}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockResultante").value(7));
    }

    @Test
    void tipoDeMovimientoInexistenteDevuelve400() throws Exception {
        long id = crear("KJ-650", 5, 20);
        mvc.perform(post("/api/v1/productos/" + id + "/movimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"REGALO\",\"cantidad\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editarNoCambiaElStock() throws Exception {
        long id = crear("KJ-700", 5, 20);
        mvc.perform(put("/api/v1/productos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"KJ-700","nombre":"Nombre nuevo","categoria":"Ropa","tipo":"Insumo",
                                 "precio":5000,"stockMinimo":8,"stockInicial":999}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nombre nuevo"))
                .andExpect(jsonPath("$.stockMinimo").value(8))
                .andExpect(jsonPath("$.stock").value(20));
    }

    @Test
    void eliminarBorraElProductoYSusMovimientos() throws Exception {
        long id = crear("KJ-800", 5, 20);

        mvc.perform(delete("/api/v1/productos/" + id)).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/productos/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/productos/" + id + "/movimientos")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/movimientos")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void elFiltroDeStockBajoSoloDevuelveLosProductosCriticos() throws Exception {
        crear("KJ-900", 5, 2);   // stock 2, mínimo 5: stock bajo
        crear("KJ-901", 5, 20);  // stock 20: normal

        mvc.perform(get("/api/v1/productos").param("soloStockBajo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("KJ-900"));

        mvc.perform(get("/api/v1/productos"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void laBusquedaFiltraPorCodigo() throws Exception {
        crear("KJ-950", 5, 10);
        crear("ZZ-001", 5, 10);

        mvc.perform(get("/api/v1/productos").param("busqueda", "kj-95"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("KJ-950"));
    }

    @Test
    void elHistorialGlobalIncluyeLosMovimientos() throws Exception {
        long id = crear("KJ-980", 5, 20);
        movimiento(id, "VENTA", 2);

        mvc.perform(get("/api/v1/movimientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
