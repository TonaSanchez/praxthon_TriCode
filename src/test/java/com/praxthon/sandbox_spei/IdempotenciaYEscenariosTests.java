package com.praxthon.sandbox_spei;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class IdempotenciaYEscenariosTests {

    private static final String RUTA = "/api/v1/operaciones";
    private static final Pattern ID = Pattern.compile("\"id\":\"(op_\\d+)\"");
    private static final Pattern TOTAL = Pattern.compile("\"totalElementos\":(\\d+)");
    private static final String EMISOR = clabe("801", "0001");
    private static final String RECEPTOR = clabe("802", "0011");

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Test
    void mismaClaveConOtroNombreDeReceptor_prx015() throws Exception {
        String clave = UUID.randomUUID().toString();
        String ref = ref();
        crear(cuerpo(ref, "Luis Cano Mora", "802", "MXN"), clave);
        alta(cuerpo(ref, "Otra Persona", "802", "MXN"), clave)
                .andExpect(status().is(409))
                .andExpect(jsonPath("$.errores[0].codigo").value("PRX-015"));
    }

    @Test
    void mismaClaveConOtraInstitucionDeReceptor_prx015() throws Exception {
        String clave = UUID.randomUUID().toString();
        String ref = ref();
        crear(cuerpo(ref, "Luis Cano Mora", "802", "MXN"), clave);
        alta(cuerpo(ref, "Luis Cano Mora", "803", "MXN"), clave)
                .andExpect(status().is(409))
                .andExpect(jsonPath("$.errores[0].codigo").value("PRX-015"));
    }

    @Test
    void mismaClaveConOtraDivisa_prx015() throws Exception {
        String clave = UUID.randomUUID().toString();
        String ref = ref();
        crear(cuerpo(ref, "Luis Cano Mora", "802", "MXN"), clave);
        alta(cuerpo(ref, "Luis Cano Mora", "802", "USD"), clave)
                .andExpect(status().is(409))
                .andExpect(jsonPath("$.errores[0].codigo").value("PRX-015"));
    }

    @Test
    void mismaClaveYMismoCuerpo_sigueDevolviendoLaOriginal() throws Exception {
        String clave = UUID.randomUUID().toString();
        String body = cuerpo(ref(), "Luis Cano Mora", "802", "MXN");
        String id = crear(body, clave);
        alta(body, clave)
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void escenarioForzadoInvalido_422SinRegistrarLaOperacion() throws Exception {
        int antes = total();
        mvc.perform(post(RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Escenario-Forzado", "S99")
                        .content(cuerpo(ref(), "Luis Cano Mora", "802", "MXN")))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.errores[?(@.campo=='X-Escenario-Forzado')]").isNotEmpty());
        assertEquals(antes, total());
    }

    @Test
    void escenarioForzadoS01_liquidado() throws Exception {
        String id = idDe(altaForzada(cuerpo(ref(), "Luis Cano Mora", "802", "MXN"), "S01").andExpect(status().is(201)));
        mvc.perform(get(RUTA + "/" + id))
                .andExpect(jsonPath("$.estado").value("LIQUIDADO"));
    }

    @Test
    void escenarioForzadoS06_enInvestigacion() throws Exception {
        String id = idDe(altaForzada(cuerpo(ref(), "Luis Cano Mora", "802", "MXN"), "S06").andExpect(status().is(201)));
        mvc.perform(get(RUTA + "/" + id))
                .andExpect(jsonPath("$.estado").value("EN_INVESTIGACION"))
                .andExpect(jsonPath("$.transiciones[2].motivo").value("PRX-024"));
    }

    @Test
    void historialSiempreEnOrden_recibidoEnProcesoLiquidado() throws Exception {
        for (int i = 0; i < 15; i++) {
            String id = crear(cuerpo(ref(), "Luis Cano Mora", "802", "MXN"), null);
            mvc.perform(get(RUTA + "/" + id))
                    .andExpect(jsonPath("$.transiciones[0].estado").value("RECIBIDO"))
                    .andExpect(jsonPath("$.transiciones[1].estado").value("EN_PROCESO"))
                    .andExpect(jsonPath("$.transiciones[2].estado").value("LIQUIDADO"));
        }
    }

    private ResultActions alta(String body, String clave) throws Exception {
        return mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).header("Clave-Idempotencia", clave).content(body));
    }

    private ResultActions altaForzada(String body, String escenario) throws Exception {
        return mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).header("X-Escenario-Forzado", escenario).content(body));
    }

    private String crear(String body, String clave) throws Exception {
        if (clave == null) {
            return idDe(mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().is(201)));
        }
        return idDe(alta(body, clave).andExpect(status().is(201)));
    }

    private String idDe(ResultActions r) throws Exception {
        String respuesta = r.andReturn().getResponse().getContentAsString();
        Matcher m = ID.matcher(respuesta);
        assertTrue(m.find());
        return m.group(1);
    }

    private int total() throws Exception {
        String respuesta = mvc.perform(get(RUTA)).andExpect(status().is(200)).andReturn().getResponse().getContentAsString();
        Matcher m = TOTAL.matcher(respuesta);
        assertTrue(m.find());
        return Integer.parseInt(m.group(1));
    }

    private static String ref() {
        return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    private static int digito(String primeros17) {
        int[] pesos = {3, 7, 1};
        int suma = 0;
        for (int i = 0; i < 17; i++) {
            suma += ((primeros17.charAt(i) - '0') * pesos[i % 3]) % 10;
        }
        return (10 - (suma % 10)) % 10;
    }

    private static String clabe(String institucion, String ultimos4) {
        String base = institucion + "180" + "0000000" + ultimos4;
        return base + digito(base);
    }

    private static String cuerpo(String ref, String nombreReceptor, String institucionReceptor, String divisa) {
        return """
                {"tipoOperacion":"T2T","referenciaSeguimiento":"%s","importe":{"valor":1500.50,"divisa":"%s"},"emisor":{"institucion":"801","cuenta":"%s","nombre":"Ana Ruiz Delgado"},"receptor":{"institucion":"%s","cuenta":"%s","nombre":"%s"},"concepto":"Pago de prueba","folioNumerico":123}
                """.formatted(ref, divisa, EMISOR, institucionReceptor, RECEPTOR, nombreReceptor);
    }
}