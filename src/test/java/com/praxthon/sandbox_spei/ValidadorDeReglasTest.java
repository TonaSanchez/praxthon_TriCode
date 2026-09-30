package com.praxthon.sandbox_spei;

import com.praxthon.sandbox_spei.dto.ErrorDetalleDTO;
import com.praxthon.sandbox_spei.dto.PeticionPagoDTO;
import com.praxthon.sandbox_spei.validation.ValidadorDeReglas;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorDeReglasTest {

    private final ValidadorDeReglas validador = new ValidadorDeReglas();

    @Test
    void vectorUno_digito9() {
        assertEquals(9, validador.digitoVerificador("03218000011835971"));
    }

    @Test
    void vectorDos_digito6() {
        assertEquals(6, validador.digitoVerificador("10315012415234578"));
    }

    @Test
    void t2tValida_sinErrores() {
        List<ErrorDetalleDTO> errores = validador.validar(t2tValida());
        assertTrue(errores.isEmpty(), descripcion(errores));
    }

    @Test
    void vntValida_sinErrores() {
        List<ErrorDetalleDTO> errores = validador.validar(vntValida());
        assertTrue(errores.isEmpty(), descripcion(errores));
    }

    @Test
    void v01_receptorCon17Digitos() {
        PeticionPagoDTO p = t2tValida();
        p.getReceptor().setCuenta(p.getReceptor().getCuenta().substring(0, 17));
        assertError(p, "PRX-001", "receptor.cuenta");
    }

    @Test
    void v01_receptorConLetras() {
        PeticionPagoDTO p = t2tValida();
        p.getReceptor().setCuenta("80218000000001100A");
        assertError(p, "PRX-001", "receptor.cuenta");
    }

    @Test
    void v02_receptorConDigitoVerificadorAlterado() {
        PeticionPagoDTO p = t2tValida();
        p.getReceptor().setCuenta(alterarDigito(p.getReceptor().getCuenta()));
        assertError(p, "PRX-002", "receptor.cuenta");
    }

    @Test
    void v03_emisorCon17Digitos() {
        PeticionPagoDTO p = t2tValida();
        p.getEmisor().setCuenta(p.getEmisor().getCuenta().substring(0, 17));
        assertError(p, "PRX-001", "emisor.cuenta");
    }

    @Test
    void v03_emisorConDigitoVerificadorAlterado() {
        PeticionPagoDTO p = t2tValida();
        p.getEmisor().setCuenta(alterarDigito(p.getEmisor().getCuenta()));
        assertError(p, "PRX-002", "emisor.cuenta");
    }

    @Test
    void v04_receptorConInstitucionInexistente() {
        PeticionPagoDTO p = t2tValida();
        p.getReceptor().setInstitucion("899");
        assertError(p, "PRX-003", "receptor.institucion");
    }

    @Test
    void v04_emisorConInstitucionInexistente() {
        PeticionPagoDTO p = t2tValida();
        p.getEmisor().setInstitucion("899");
        assertError(p, "PRX-003", "emisor.institucion");
    }

    @Test
    void v04_institucion804SoloPuedeSerReceptora() {
        PeticionPagoDTO p = t2tValida();
        p.getEmisor().setInstitucion("804");
        assertError(p, "PRX-003", "emisor.institucion");

        PeticionPagoDTO q = t2tValida();
        q.getReceptor().setInstitucion("804");
        q.getReceptor().setCuenta(clabe("804", "0011"));
        assertTrue(validador.validar(q).isEmpty(), "804 como receptor debe ser válida");
    }

    @Test
    void v05_receptorNoCoincideConInstitucion() {
        PeticionPagoDTO p = t2tValida();
        p.getReceptor().setCuenta(clabe("803", "0011"));
        assertError(p, "PRX-030", "receptor.cuenta");
    }

    @Test
    void v05_emisorNoCoincideConInstitucion() {
        PeticionPagoDTO p = t2tValida();
        p.getEmisor().setCuenta(clabe("802", "0001"));
        assertError(p, "PRX-030", "emisor.cuenta");
    }

    @Test
    void v06_importeCeroYNegativo() {
        PeticionPagoDTO p = t2tValida();
        p.getImporte().setValor(BigDecimal.ZERO);
        assertError(p, "PRX-004", "importe.valor");

        p.getImporte().setValor(new BigDecimal("-1.00"));
        assertError(p, "PRX-004", "importe.valor");
    }

    @Test
    void v07_importeMayorAlMaximoOConTresDecimales() {
        PeticionPagoDTO p = t2tValida();
        p.getImporte().setValor(new BigDecimal("1000000.01"));
        assertError(p, "PRX-005", "importe.valor");

        p.getImporte().setValor(new BigDecimal("1.005"));
        assertError(p, "PRX-005", "importe.valor");
    }

    @Test
    void v07_limitesValidos() {
        PeticionPagoDTO p = t2tValida();
        p.getImporte().setValor(new BigDecimal("1000000.00"));
        assertTrue(validador.validar(p).isEmpty());

        p.getImporte().setValor(new BigDecimal("0.01"));
        assertTrue(validador.validar(p).isEmpty());
    }

    @Test
    void v08_divisaDistintaDeMxn() {
        PeticionPagoDTO p = t2tValida();
        p.getImporte().setDivisa("USD");
        assertError(p, "PRX-006", "importe.divisa");
    }

    @Test
    void v09_conceptoVacioOMuyLargo() {
        PeticionPagoDTO p = t2tValida();
        p.setConcepto("");
        assertError(p, "PRX-007", "concepto");

        p.setConcepto("x".repeat(41));
        assertError(p, "PRX-007", "concepto");
    }

    @Test
    void v09_conceptoDe40CaracteresEsValido() {
        PeticionPagoDTO p = t2tValida();
        p.setConcepto("x".repeat(40));
        assertTrue(validador.validar(p).isEmpty());
    }

    @Test
    void v10_folioFueraDeRango() {
        PeticionPagoDTO p = t2tValida();
        p.setFolioNumerico(0L);
        assertError(p, "PRX-008", "folioNumerico");

        p.setFolioNumerico(10_000_000L);
        assertError(p, "PRX-008", "folioNumerico");

        p.setFolioNumerico(null);
        assertError(p, "PRX-008", "folioNumerico");
    }

    @Test
    void v10_folioLimiteValido() {
        PeticionPagoDTO p = t2tValida();
        p.setFolioNumerico(9_999_999L);
        assertTrue(validador.validar(p).isEmpty());
    }

    @Test
    void v11_referenciaInvalida() {
        PeticionPagoDTO p = t2tValida();
        p.setReferenciaSeguimiento("REF-1");
        assertError(p, "PRX-009", "referenciaSeguimiento");

        p.setReferenciaSeguimiento("");
        assertError(p, "PRX-009", "referenciaSeguimiento");

        p.setReferenciaSeguimiento("A".repeat(31));
        assertError(p, "PRX-009", "referenciaSeguimiento");

        p.setReferenciaSeguimiento(null);
        assertError(p, "PRX-009", "referenciaSeguimiento");
    }

    @Test
    void v11_referenciaDe30CaracteresEsValida() {
        PeticionPagoDTO p = t2tValida();
        p.setReferenciaSeguimiento("A".repeat(30));
        assertTrue(validador.validar(p).isEmpty());
    }

    @Test
    void v13_tipoInvalido() {
        PeticionPagoDTO p = t2tValida();
        p.setTipoOperacion("XXX");
        assertError(p, "PRX-031", "tipoOperacion");
    }

    @Test
    void v14_t2tSinCuentaEmisor() {
        PeticionPagoDTO p = t2tValida();
        p.getEmisor().setCuenta(null);
        assertError(p, "PRX-011", "emisor.cuenta");
    }

    @Test
    void v15_t2tConSucursal() {
        PeticionPagoDTO p = t2tValida();
        p.getEmisor().setSucursal("0417");
        assertError(p, "PRX-012", "emisor.sucursal");
    }

    @Test
    void v15_t2tConSucursalVacia() {
        PeticionPagoDTO p = t2tValida();
        p.getEmisor().setSucursal("");
        assertError(p, "PRX-012", "emisor.sucursal");
    }

    @Test
    void v15_t2tConDocumentoIdentidad() {
        PeticionPagoDTO p = t2tValida();
        p.getEmisor().setDocumentoIdentidad(documento());
        assertError(p, "PRX-012", "emisor.documentoIdentidad");
    }

    @Test
    void v16_vntSinSucursal() {
        PeticionPagoDTO p = vntValida();
        p.getEmisor().setSucursal(null);
        assertError(p, "PRX-011", "emisor.sucursal");
    }

    @Test
    void v16_vntSinDocumentoIdentidad() {
        PeticionPagoDTO p = vntValida();
        p.getEmisor().setDocumentoIdentidad(null);
        assertError(p, "PRX-011", "emisor.documentoIdentidad");
    }

    @Test
    void v17_vntConCuentaEmisor() {
        PeticionPagoDTO p = vntValida();
        p.getEmisor().setCuenta(clabe("801", "0001"));
        assertError(p, "PRX-012", "emisor.cuenta");
    }

    @Test
    void v17_vntConCuentaEmisorVacia() {
        PeticionPagoDTO p = vntValida();
        p.getEmisor().setCuenta("");
        assertError(p, "PRX-012", "emisor.cuenta");
    }

    @Test
    void v18_nombreEmisorVacio() {
        PeticionPagoDTO p = t2tValida();
        p.getEmisor().setNombre("");
        assertError(p, "PRX-011", "emisor.nombre");
    }

    @Test
    void v18_nombreReceptorMuyLargo() {
        PeticionPagoDTO p = t2tValida();
        p.getReceptor().setNombre("N".repeat(41));
        assertError(p, "PRX-011", "receptor.nombre");
    }

    @Test
    void v19_t2tConEmisorYReceptorIguales() {
        PeticionPagoDTO p = t2tValida();
        String cuenta = clabe("801", "0011");
        p.getEmisor().setCuenta(cuenta);
        p.getReceptor().setInstitucion("801");
        p.getReceptor().setCuenta(cuenta);
        assertError(p, "PRX-013", "emisor.cuenta");
    }

    @Test
    void variosErrores_seAcumulanEnUnaSolaRespuesta() {
        PeticionPagoDTO p = t2tValida();
        p.setConcepto("");
        p.setFolioNumerico(0L);
        p.getImporte().setDivisa("USD");
        assertError(p, "PRX-007", "concepto");
        assertError(p, "PRX-008", "folioNumerico");
        assertError(p, "PRX-006", "importe.divisa");
    }

    private void assertError(PeticionPagoDTO p, String codigo, String campo) {
        List<ErrorDetalleDTO> errores = validador.validar(p);
        boolean hay = errores.stream().anyMatch(e -> codigo.equals(e.getCodigo()) && campo.equals(e.getCampo()));
        assertTrue(hay, "Se esperaba " + codigo + " en " + campo + " pero llegó: " + descripcion(errores));
    }

    private String descripcion(List<ErrorDetalleDTO> errores) {
        return errores.stream().map(e -> e.getCodigo() + "@" + e.getCampo()).toList().toString();
    }

    private String clabe(String institucion, String ultimos4) {
        String base = institucion + "180" + "0000000" + ultimos4;
        return base + validador.digitoVerificador(base);
    }

    private String alterarDigito(String clabe) {
        int malo = (clabe.charAt(17) - '0' + 1) % 10;
        return clabe.substring(0, 17) + malo;
    }

    private PeticionPagoDTO.DocumentoIdentidadDTO documento() {
        PeticionPagoDTO.DocumentoIdentidadDTO doc = new PeticionPagoDTO.DocumentoIdentidadDTO();
        doc.setTipo("INE");
        doc.setNumero("IDMEX1734558");
        return doc;
    }

    private PeticionPagoDTO t2tValida() {
        PeticionPagoDTO p = new PeticionPagoDTO();
        p.setTipoOperacion("T2T");
        p.setReferenciaSeguimiento("REF001");
        p.setImporte(new PeticionPagoDTO.ImporteDTO(new BigDecimal("100.00"), "MXN"));

        PeticionPagoDTO.EmisorDTO emisor = new PeticionPagoDTO.EmisorDTO();
        emisor.setInstitucion("801");
        emisor.setCuenta(clabe("801", "0001"));
        emisor.setNombre("Ana Ruiz Delgado");
        p.setEmisor(emisor);

        PeticionPagoDTO.ReceptorDTO receptor = new PeticionPagoDTO.ReceptorDTO();
        receptor.setInstitucion("802");
        receptor.setCuenta(clabe("802", "0011"));
        receptor.setNombre("Luis Cano Mora");
        p.setReceptor(receptor);

        p.setConcepto("Pago de prueba");
        p.setFolioNumerico(1L);
        return p;
    }

    private PeticionPagoDTO vntValida() {
        PeticionPagoDTO p = t2tValida();
        p.setTipoOperacion("VNT");
        p.getEmisor().setCuenta(null);
        p.getEmisor().setSucursal("0417");
        p.getEmisor().setNombre("Marta Solis Vega");
        p.getEmisor().setDocumentoIdentidad(documento());
        return p;
    }
}