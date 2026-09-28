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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AnexoAAceptacionTests {

    private static final String RUTA = "/api/v1/operaciones";
    private static final Pattern ID = Pattern.compile("\"id\":\"(op_\\d+)\"");
    private static final Pattern TOTAL = Pattern.compile("\"totalElementos\":(\\d+)");
    private static final String EMISOR = clabe("801", "0001");
    private static final String RECEPTOR_OK = clabe("802", "0011");

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Test
    void a01_t2tValida_seRegistraYSeLiquida() throws Exception {
        String id = crear(t2t(ref(), RECEPTOR_OK));
        verificar(id, "LIQUIDADO", 3, null);
    }

    @Test
    void a02_vntValida_seRegistraYSeLiquida() throws Exception {
        String id = crear(vnt(ref(), clabe("803", "0012")));
        verificar(id, "LIQUIDADO", 3, null);
    }

    @Test
    void a03_t2tSinCuentaEmisor_prx011() throws Exception {
        String emisor = "{\"institucion\":\"801\",\"nombre\":\"Ana Ruiz Delgado\"}";
        String body = cuerpo("T2T", ref(), emisor, receptor("802", RECEPTOR_OK), "1500.50", "MXN", "Pago de prueba", "123");
        verificarError(alta(body), "PRX-011", "emisor.cuenta");
    }

    @Test
    void a04_vntConCuentaEmisor_prx012() throws Exception {
        String body = cuerpo("VNT", ref(), emisorVnt(true, true), receptor("802", RECEPTOR_OK), "1500.50", "MXN", "Pago de prueba", "123");
        verificarError(alta(body), "PRX-012", "emisor.cuenta");
    }

    @Test
    void a05_vntSinSucursal_prx011() throws Exception {
        String body = cuerpo("VNT", ref(), emisorVnt(false, false), receptor("802", RECEPTOR_OK), "1500.50", "MXN", "Pago de prueba", "123");
        verificarError(alta(body), "PRX-011", "emisor.sucursal");
    }

    @Test
    void a06_cuentaReceptoraDeDiecisieteDigitos_prx001() throws Exception {
        String c17 = RECEPTOR_OK.substring(0, 17);
        String body = cuerpo("T2T", ref(), emisorT2T(EMISOR), receptor("802", c17), "1500.50", "MXN", "Pago de prueba", "123");
        verificarError(alta(body), "PRX-001", "receptor.cuenta");
    }

    @Test
    void a07_digitoVerificadorAlterado_prx002() throws Exception {
        String alterada = RECEPTOR_OK.substring(0, 17) + ((RECEPTOR_OK.charAt(17) - '0' + 1) % 10);
        String body = cuerpo("T2T", ref(), emisorT2T(EMISOR), receptor("802", alterada), "1500.50", "MXN", "Pago de prueba", "123");
        verificarError(alta(body), "PRX-002", "receptor.cuenta");
    }

    @Test
    void a08_institucionInexistente_prx003() throws Exception {
        String body = cuerpo("T2T", ref(), emisorT2T(EMISOR), receptor("899", RECEPTOR_OK), "1500.50", "MXN", "Pago de prueba", "123");
        verificarError(alta(body), "PRX-003", "receptor.institucion");
    }

    @Test
    void a09_cuentaNoCoincideConInstitucion_prx030() throws Exception {
        String body = cuerpo("T2T", ref(), emisorT2T(EMISOR), receptor("802", clabe("803", "0011")), "1500.50", "MXN", "Pago de prueba", "123");
        verificarError(alta(body), "PRX-030", "receptor.cuenta");
    }

    @Test
    void a10_importeCero_prx004() throws Exception {
        verificarError(alta(conImporte("0", "MXN")), "PRX-004", "importe.valor");
    }

    @Test
    void a11_importeNegativo_prx004() throws Exception {
        verificarError(alta(conImporte("-5.00", "MXN")), "PRX-004", "importe.valor");
    }

    @Test
    void a12_importeConTresDecimales_prx005() throws Exception {
        verificarError(alta(conImporte("1.005", "MXN")), "PRX-005", "importe.valor");
    }

    @Test
    void a13_importeMayorAlMaximo_prx005() throws Exception {
        verificarError(alta(conImporte("1000000.01", "MXN")), "PRX-005", "importe.valor");
    }

    @Test
    void a14_divisaUsd_prx006() throws Exception {
        verificarError(alta(conImporte("100.00", "USD")), "PRX-006", "importe.divisa");
    }

    @Test
    void a15_conceptoVacioYFolioCero_dosErroresEnUnaRespuesta() throws Exception {
        String body = cuerpo("T2T", ref(), emisorT2T(EMISOR), receptor("802", RECEPTOR_OK), "1500.50", "MXN", "", "0");
        ResultActions r = alta(body);
        verificarError(r, "PRX-007", "concepto");
        verificarError(r, "PRX-008", "folioNumerico");
    }

    @Test
    void a16_referenciaRepetida_prx010() throws Exception {
        String ref = ref();
        crear(t2t(ref, RECEPTOR_OK));
        verificarError(alta(t2t(ref, RECEPTOR_OK)), "PRX-010", "referenciaSeguimiento");
    }

    @Test
    void a17_emisorYReceptorIguales_prx013() throws Exception {
        String cuenta = clabe("801", "0011");
        String body = cuerpo("T2T", ref(), emisorT2T(cuenta), receptor("801", cuenta), "1500.50", "MXN", "Pago de prueba", "123");
        verificarError(alta(body), "PRX-013", "emisor.cuenta");
    }

    @Test
    void institucionEmisora804_noPermitida_prx003() throws Exception {
        String emisor = "{\"institucion\":\"804\",\"cuenta\":\"" + clabe("804", "0001") + "\",\"nombre\":\"Ana Ruiz Delgado\"}";
        String body = cuerpo("T2T", ref(), emisor, receptor("802", RECEPTOR_OK), "1500.50", "MXN", "Pago de prueba", "123");
        verificarError(alta(body), "PRX-003", "emisor.institucion");
    }

    @Test
    void a18_cuenta9002_devueltoPrx020() throws Exception {
        String id = crear(t2t(ref(), clabe("802", "9002")));
        verificar(id, "DEVUELTO", 3, "PRX-020");
    }

    @Test
    void a19_cuenta9003_devueltoPrx021() throws Exception {
        String id = crear(t2t(ref(), clabe("802", "9003")));
        verificar(id, "DEVUELTO", 3, "PRX-021");
    }

    @Test
    void cuenta9004_devueltoPrx022() throws Exception {
        String id = crear(t2t(ref(), clabe("802", "9004")));
        verificar(id, "DEVUELTO", 3, "PRX-022");
    }

    @Test
    void a20_institucion805_devueltoPrx022() throws Exception {
        String id = crear(t2t(ref(), clabe("805", "0011")));
        verificar(id, "DEVUELTO", 3, "PRX-022");
    }

    @Test
    void a26_cuenta9005_permaneceEnProceso() throws Exception {
        String id = crear(t2t(ref(), clabe("802", "9005")));
        verificar(id, "EN_PROCESO", 2, null);
    }

    @Test
    void cuenta9006_enInvestigacionPrx024() throws Exception {
        String id = crear(t2t(ref(), clabe("802", "9006")));
        verificar(id, "EN_INVESTIGACION", 3, "PRX-024");
    }

    @Test
    void escenarioForzadoS02_devueltoPrx020() throws Exception {
        String id = idDe(altaForzada(t2t(ref(), RECEPTOR_OK), "S02").andExpect(status().is(201)));
        verificar(id, "DEVUELTO", 3, "PRX-020");
    }

    @Test
    void a21_liquidadoADevuelto_prx014() throws Exception {
        String id = crear(t2t(ref(), RECEPTOR_OK));
        cambiarEstado(id, "DEVUELTO")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.errores[0].codigo").value("PRX-014"));
        verificar(id, "LIQUIDADO", 3, null);
    }

    @Test
    void devueltoEsTerminal_prx014() throws Exception {
        String id = crear(t2t(ref(), clabe("802", "9002")));
        cambiarEstado(id, "LIQUIDADO")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.errores[0].codigo").value("PRX-014"));
    }

    @Test
    void estadoInexistente_prx014() throws Exception {
        String id = crear(t2t(ref(), clabe("802", "9005")));
        cambiarEstado(id, "INVENTADO")
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.errores[0].codigo").value("PRX-014"));
    }

    @Test
    void enProcesoALiquidado_transicionPermitida() throws Exception {
        String id = crear(t2t(ref(), clabe("802", "9005")));
        cambiarEstado(id, "LIQUIDADO").andExpect(status().is(200));
        verificar(id, "LIQUIDADO", 3, null);
    }

    @Test
    void cambiarEstadoDeOperacionInexistente_404() throws Exception {
        cambiarEstado("op_999999", "LIQUIDADO").andExpect(status().is(404));
    }

    @Test
    void a22_consultaDeIdentificadorInexistente_404() throws Exception {
        mvc.perform(get(RUTA + "/op_999999")).andExpect(status().is(404));
    }

    @Test
    void a23_listadoSinParametros_vieneEnPaginas() throws Exception {
        for (int i = 0; i < 25; i++) {
            crear(t2t(ref(), RECEPTOR_OK));
        }
        mvc.perform(get(RUTA))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamano").value(20))
                .andExpect(jsonPath("$.contenido.length()").value(20));
        mvc.perform(get(RUTA + "?pagina=1&tamano=5"))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.contenido.length()").value(5));
    }

    @Test
    void a24_mismaClaveYMismoCuerpo_devuelveLaOperacionOriginal() throws Exception {
        String clave = UUID.randomUUID().toString();
        String body = t2t(ref(), RECEPTOR_OK);
        String id = crear(body, clave);
        int antes = total();
        alta(body, clave)
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.id").value(id));
        assertEquals(antes, total());
    }

    @Test
    void a25_mismaClaveConCuerpoDistinto_prx015() throws Exception {
        String clave = UUID.randomUUID().toString();
        String ref = ref();
        crear(t2t(ref, RECEPTOR_OK), clave);
        String distinto = cuerpo("T2T", ref, emisorT2T(EMISOR), receptor("802", RECEPTOR_OK), "1500.50", "MXN", "Otro concepto", "123");
        alta(distinto, clave)
                .andExpect(status().is(409))
                .andExpect(jsonPath("$.errores[0].codigo").value("PRX-015"));
    }

    @Test
    void catalogoDeInstituciones_devuelveLasCinco() throws Exception {
        mvc.perform(get("/api/v1/catalogos/instituciones"))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].codigo").value("801"));
    }

    private ResultActions alta(String body) throws Exception {
        return mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions alta(String body, String clave) throws Exception {
        return mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).header("Clave-Idempotencia", clave).content(body));
    }

    private ResultActions altaForzada(String body, String escenario) throws Exception {
        return mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).header("X-Escenario-Forzado", escenario).content(body));
    }

    private ResultActions cambiarEstado(String id, String nuevoEstado) throws Exception {
        return mvc.perform(patch(RUTA + "/" + id + "/estado").contentType(MediaType.APPLICATION_JSON).content("{\"nuevoEstado\":\"" + nuevoEstado + "\"}"));
    }

    private String crear(String body) throws Exception {
        return idDe(alta(body).andExpect(status().is(201)));
    }

    private String crear(String body, String clave) throws Exception {
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

    private void verificar(String id, String estado, int transiciones, String motivo) throws Exception {
        ResultActions r = mvc.perform(get(RUTA + "/" + id))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.estado").value(estado))
                .andExpect(jsonPath("$.transiciones.length()").value(transiciones))
                .andExpect(jsonPath("$.transiciones[0].estado").value("RECIBIDO"));
        if (motivo != null) {
            r.andExpect(jsonPath("$.transiciones[" + (transiciones - 1) + "].motivo").value(motivo));
        }
    }

    private void verificarError(ResultActions r, String codigo, String campo) throws Exception {
        r.andExpect(status().is(422))
                .andExpect(jsonPath("$.errores[?(@.codigo=='" + codigo + "' && @.campo=='" + campo + "')]").isNotEmpty());
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

    private static String emisorT2T(String cuenta) {
        return "{\"institucion\":\"801\",\"cuenta\":\"" + cuenta + "\",\"nombre\":\"Ana Ruiz Delgado\"}";
    }

    private static String emisorVnt(boolean conSucursal, boolean conCuenta) {
        return "{\"institucion\":\"801\","
                + (conCuenta ? "\"cuenta\":\"" + EMISOR + "\"," : "")
                + (conSucursal ? "\"sucursal\":\"0417\"," : "")
                + "\"nombre\":\"Marta Solis Vega\",\"documentoIdentidad\":{\"tipo\":\"INE\",\"numero\":\"IDMEX1734558\"}}";
    }

    private static String receptor(String institucion, String cuenta) {
        return "{\"institucion\":\"" + institucion + "\",\"cuenta\":\"" + cuenta + "\",\"nombre\":\"Luis Cano Mora\"}";
    }

    private static String cuerpo(String tipo, String ref, String emisor, String receptor,
                                 String valor, String divisa, String concepto, String folio) {
        return """
                {"tipoOperacion":"%s","referenciaSeguimiento":"%s","importe":{"valor":%s,"divisa":"%s"},"emisor":%s,"receptor":%s,"concepto":"%s","folioNumerico":%s}
                """.formatted(tipo, ref, valor, divisa, emisor, receptor, concepto, folio);
    }

    private static String t2t(String ref, String cuentaReceptor) {
        return cuerpo("T2T", ref, emisorT2T(EMISOR), receptor(cuentaReceptor.substring(0, 3), cuentaReceptor),
                "1500.50", "MXN", "Pago de prueba", "123");
    }

    private static String vnt(String ref, String cuentaReceptor) {
        return cuerpo("VNT", ref, emisorVnt(true, false), receptor(cuentaReceptor.substring(0, 3), cuentaReceptor),
                "1500.50", "MXN", "Pago de prueba", "123");
    }

    private static String conImporte(String valor, String divisa) {
        return cuerpo("T2T", ref(), emisorT2T(EMISOR), receptor("802", RECEPTOR_OK), valor, divisa, "Pago de prueba", "123");
    }
}