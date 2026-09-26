package com.praxthon.sandbox_spei;

import com.praxthon.sandbox_spei.dto.ErrorDetalleDTO;
import com.praxthon.sandbox_spei.dto.PeticionPagoDTO;
import com.praxthon.sandbox_spei.dto.ValidadorDeReglas;
import com.praxthon.sandbox_spei.repository.OperacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class SandboxSpeiApplicationTests {

    private ValidadorDeReglas validador;
    private OperacionRepository operacionRepository;

    @BeforeEach
    void setUp() {
        operacionRepository = Mockito.mock(OperacionRepository.class);
        when(operacionRepository.existsByReferenciaSeguimiento(anyString())).thenReturn(false);
        validador = new ValidadorDeReglas(operacionRepository);
    }

    @Test
    void pruebaDigitoVerificadorOficial() {
        assertEquals(9, validador.digitoVerificador("03218000011835971"));
        assertEquals(6, validador.digitoVerificador("10315012415234578"));
    }

    @Test
    void casoA01_T2TValida_SinErrores() {
        PeticionPagoDTO req = crearPeticionT2TValida();
        List<ErrorDetalleDTO> errores = validador.validar(req);
        assertTrue(errores.isEmpty());
    }

    @Test
    void casoA03_T2TSinCuentaEmisor_DaPRX011() {
        PeticionPagoDTO req = crearPeticionT2TValida();
        req.getEmisor().setCuenta(null);
        List<ErrorDetalleDTO> errores = validador.validar(req);
        assertTrue(errores.stream().anyMatch(e -> "PRX-011".equals(e.getCodigo()) && "emisor.cuenta".equals(e.getCampo())));
    }

    @Test
    void casoA04_VNTConCuentaEmisor_DaPRX012() {
        PeticionPagoDTO req = crearPeticionT2TValida();
        req.setTipoOperacion("VNT");
        req.getEmisor().setSucursal("0417");
        PeticionPagoDTO.DocumentoIdentidadDTO doc = new PeticionPagoDTO.DocumentoIdentidadDTO();
        doc.setTipo("INE");
        doc.setNumero("IDMEX1734558");
        req.getEmisor().setDocumentoIdentidad(doc);
        List<ErrorDetalleDTO> errores = validador.validar(req);
        assertTrue(errores.stream().anyMatch(e -> "PRX-012".equals(e.getCodigo())));
    }

    @Test
    void casosA06_A07_CuentaReceptoraInvalida() {
        PeticionPagoDTO req = crearPeticionT2TValida();
        req.getReceptor().setCuenta("80218000011835971");
        assertTrue(validador.validar(req).stream().anyMatch(e -> "PRX-001".equals(e.getCodigo())));

        req.getReceptor().setCuenta("802180000118359710");
        assertTrue(validador.validar(req).stream().anyMatch(e -> "PRX-002".equals(e.getCodigo())));
    }

    @Test
    void casoA15_MultiplesErroresEnUnaSolaRespuesta() {
        PeticionPagoDTO req = crearPeticionT2TValida();
        req.setConcepto("");
        req.setFolioNumerico(0L);
        List<ErrorDetalleDTO> errores = validador.validar(req);
        assertTrue(errores.stream().anyMatch(e -> "PRX-007".equals(e.getCodigo())));
        assertTrue(errores.stream().anyMatch(e -> "PRX-008".equals(e.getCodigo())));
    }

    private PeticionPagoDTO crearPeticionT2TValida() {
        PeticionPagoDTO req = new PeticionPagoDTO();
        req.setTipoOperacion("T2T");

        PeticionPagoDTO.EmisorDTO emisor = new PeticionPagoDTO.EmisorDTO();
        emisor.setNombre("Juan Perez");
        emisor.setInstitucion("801");
        int dvEmisor = validador.digitoVerificador("80118000011835971");
        emisor.setCuenta("80118000011835971" + dvEmisor);
        req.setEmisor(emisor);

        PeticionPagoDTO.ReceptorDTO receptor = new PeticionPagoDTO.ReceptorDTO();
        receptor.setNombre("Maria Lopez");
        receptor.setInstitucion("802");
        int dvReceptor = validador.digitoVerificador("80218000011835971");
        receptor.setCuenta("80218000011835971" + dvReceptor);
        req.setReceptor(receptor);

        PeticionPagoDTO.ImporteDTO importe = new PeticionPagoDTO.ImporteDTO();
        importe.setValor(new BigDecimal("1500.50"));
        importe.setDivisa("MXN");
        req.setImporte(importe);

        req.setConcepto("Pago de prueba");
        req.setFolioNumerico(12345L);
        req.setReferenciaSeguimiento("REF2026ABC");
        return req;
    }
}