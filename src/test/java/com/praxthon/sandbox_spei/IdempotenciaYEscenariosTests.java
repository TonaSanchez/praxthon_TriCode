package com.praxthon.sandbox_spei;

import com.praxthon.sandbox_spei.repository.OperacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class IdempotenciaYEscenariosTests {

    private static final String RUTA = "/api/v1/operaciones";
    private static final String EMISOR = clabe("801", "0001");
    private static final String RECEPTOR = clabe("802", "0011");

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private OperacionRepository operacionRepository;

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
        String referencia = ref();
        mvc.perform(post(RUTA)
                .contentType(APPLICATION_JSON)
                        .header("X-Escenario-Forzado", "S99")
                .content(cuerpo(referencia, "Luis Cano Mora", "802", "MXN")))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.errores[?(@.campo=='X-Escenario-Forzado')]").isNotEmpty());
        assertFalse(operacionRepository.existsByReferenciaSeguimiento(referencia));
    }

    @Test
    void escenarioForzadoS01_liquidado() throws Exception {
        String id = AyudaPruebas.idDe(altaForzada(cuerpo(ref(), "Luis Cano Mora", "802", "MXN"), "S01").andExpect(status().is(201)));
        mvc.perform(get(RUTA + "/" + id))
                .andExpect(jsonPath("$.estado").value("LIQUIDADO"));
    }

    @Test
    void escenarioForzadoS06_enInvestigacion() throws Exception {
        String id = AyudaPruebas.idDe(altaForzada(cuerpo(ref(), "Luis Cano Mora", "802", "MXN"), "S06").andExpect(status().is(201)));
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
        return mvc.perform(post(RUTA).contentType(APPLICATION_JSON).header("Clave-Idempotencia", clave).content(body));
    }

    private ResultActions altaForzada(String body, String escenario) throws Exception {
        return mvc.perform(post(RUTA).contentType(APPLICATION_JSON).header("X-Escenario-Forzado", escenario).content(body));
    }

    private String crear(String body, String clave) throws Exception {
        if (clave == null) {
            return AyudaPruebas.idDe(mvc.perform(post(RUTA).contentType(APPLICATION_JSON).content(body)).andExpect(status().is(201)));
        }
        return AyudaPruebas.idDe(alta(body, clave).andExpect(status().is(201)));
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