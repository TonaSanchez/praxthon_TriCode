package com.praxthon.sandbox_spei;

import com.praxthon.sandbox_spei.repository.OperacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ConcurrenciaTests {

    private static final String RUTA = "/api/v1/operaciones";
    private static final String EMISOR = clabe("801", "0001");
    private static final String RECEPTOR = clabe("802", "9005");

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
    void cambiosDeEstadoSimultaneos_soloUnoGana() throws Exception {
        String referencia = ref();
        String id = AyudaPruebas.idDe(mvc.perform(post(RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(referencia)))
                .andExpect(status().is(201)));

        Respuestas respuestas = ejecutarSimultaneamente(
                () -> mvc.perform(patch(RUTA + "/" + id + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevoEstado\":\"LIQUIDADO\"}")).andReturn(),
                () -> mvc.perform(patch(RUTA + "/" + id + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevoEstado\":\"DEVUELTO\"}")).andReturn());

        assertEquals(List.of(200, 422), List.of(
                respuestas.primera().getResponse().getStatus(),
                respuestas.segunda().getResponse().getStatus()).stream().sorted().toList());
        mvc.perform(get(RUTA + "/" + id))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.transiciones.length()").value(3));
    }

    @Test
    void altasSimultaneasConLaMismaClave_creanUnaOperacion() throws Exception {
        String referencia = ref();
        String clave = UUID.randomUUID().toString();
        String cuerpo = cuerpo(referencia);
        Respuestas respuestas = ejecutarSimultaneamente(
                () -> mvc.perform(post(RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Clave-Idempotencia", clave)
                        .content(cuerpo)).andReturn(),
                () -> mvc.perform(post(RUTA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Clave-Idempotencia", clave)
                        .content(cuerpo)).andReturn());

        assertEquals(List.of(200, 201), List.of(
                respuestas.primera().getResponse().getStatus(),
                respuestas.segunda().getResponse().getStatus()).stream().sorted().toList());
        String primerId = AyudaPruebas.extraerId(respuestas.primera().getResponse().getContentAsString());
        String segundoId = AyudaPruebas.extraerId(respuestas.segunda().getResponse().getContentAsString());
        assertEquals(primerId, segundoId);
        assertEquals(1, operacionRepository.findAll().stream()
                .filter(operacion -> referencia.equals(operacion.getReferenciaSeguimiento()))
                .count());
    }

    private Respuestas ejecutarSimultaneamente(Callable<MvcResult> primera,
                                                Callable<MvcResult> segunda) throws Exception {
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        CountDownLatch preparadas = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            Future<MvcResult> respuestaPrimera = ejecutor.submit(() -> ejecutarAlInicio(primera, preparadas, inicio));
            Future<MvcResult> respuestaSegunda = ejecutor.submit(() -> ejecutarAlInicio(segunda, preparadas, inicio));
            assertTrue(preparadas.await(10, TimeUnit.SECONDS), "Las solicitudes no quedaron listas a tiempo");
            inicio.countDown();
            return new Respuestas(respuestaPrimera.get(30, TimeUnit.SECONDS), respuestaSegunda.get(30, TimeUnit.SECONDS));
        } finally {
            ejecutor.shutdownNow();
        }
    }

    private MvcResult ejecutarAlInicio(Callable<MvcResult> solicitud,
                                       CountDownLatch preparadas,
                                       CountDownLatch inicio) throws Exception {
        preparadas.countDown();
        assertTrue(inicio.await(10, TimeUnit.SECONDS), "No se liberó la barrera de inicio");
        return solicitud.call();
    }

    private record Respuestas(MvcResult primera, MvcResult segunda) {
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

    private static String cuerpo(String referencia) {
        return """
                {"tipoOperacion":"T2T","referenciaSeguimiento":"%s","importe":{"valor":1500.50,"divisa":"MXN"},"emisor":{"institucion":"801","cuenta":"%s","nombre":"Ana Ruiz Delgado"},"receptor":{"institucion":"802","cuenta":"%s","nombre":"Luis Cano Mora"},"concepto":"Pago de prueba","folioNumerico":123}
                """.formatted(referencia, EMISOR, RECEPTOR);
    }
}