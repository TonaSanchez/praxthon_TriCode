package com.praxthon.sandbox_spei.config;

import com.praxthon.sandbox_spei.entity.Operacion;
import com.praxthon.sandbox_spei.entity.Transicion;
import com.praxthon.sandbox_spei.repository.OperacionRepository;
import com.praxthon.sandbox_spei.repository.TransicionRepository;
import com.praxthon.sandbox_spei.service.MotorDePagosService;
import com.praxthon.sandbox_spei.service.MotorDePagosService.ComandoPago;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DatosSemilla implements CommandLineRunner {

    private static final String EMISOR_T2T_A = "801180000000100016";
    private static final String EMISOR_T2T_B = "801180000000500027";
    private static final String EMISOR_T2T_C = "801180000000600037";

    private static final String RECEPTOR_LIQUIDADO = "802180000000200119";
    private static final String RECEPTOR_VNT_LIQUIDADO = "803180000000300128";
    private static final String RECEPTOR_DEVUELTO_9002 = "802180000000490026";
    private static final String RECEPTOR_RECHAZADO = "802180000000700130";

    private final OperacionRepository operacionRepository;
    private final TransicionRepository transicionRepository;
    private final MotorDePagosService motor;

    public DatosSemilla(OperacionRepository operacionRepository,
                        TransicionRepository transicionRepository,
                        MotorDePagosService motor) {
        this.operacionRepository = operacionRepository;
        this.transicionRepository = transicionRepository;
        this.motor = motor;
    }

    @Override
    public void run(String... args) {
        if (operacionRepository.count() > 0) {
            return;
        }

        motor.procesarPago(t2t("SEMILLA0001", EMISOR_T2T_A, "802", RECEPTOR_LIQUIDADO,
                "Luis Cano Mora", "1500.50", "Pago de servicios", 1L), null, null, null);

        motor.procesarPago(vnt("SEMILLA0002", "803", RECEPTOR_VNT_LIQUIDADO,
                "Comercial Vega SA de CV", "3200.00", "Deposito en ventanilla", 2L), null, null, null);

        motor.procesarPago(t2t("SEMILLA0003", EMISOR_T2T_B, "802", RECEPTOR_DEVUELTO_9002,
                "Maria Lopez Ruiz", "980.00", "Pago devuelto ejemplo", 3L), null, null, null);

        sembrarRechazada();
    }

    private void sembrarRechazada() {
        Operacion op = new Operacion();
        op.setTipoOperacion("T2T");
        op.setEstadoActual("RECIBIDO");
        op.setEmisorNombre("Ana Ruiz Delgado");
        op.setEmisorInstitucion("801");
        op.setEmisorCuenta(EMISOR_T2T_C);
        op.setReceptorNombre("Luis Cano Mora");
        op.setReceptorInstitucion("802");
        op.setReceptorCuenta(RECEPTOR_RECHAZADO);
        op.setImporteValor(new BigDecimal("750.00"));
        op.setImporteDivisa("MXN");
        op.setConcepto("Rechazo de ejemplo");
        op.setFolioNumerico(4L);
        op.setReferenciaSeguimiento("SEMILLA0004");
        op = operacionRepository.save(op);

        Transicion inicial = new Transicion();
        inicial.setOperacionId(op.getId());
        inicial.setEstadoOrigen(null);
        inicial.setEstadoDestino("RECIBIDO");
        transicionRepository.save(inicial);

        motor.aplicarTransicion(op, "RECHAZADO", null);
    }

    private ComandoPago t2t(String ref, String cuentaEmisor, String instReceptor,
                            String cuentaReceptor, String nombreReceptor,
                            String valor, String concepto, long folio) {
        return new ComandoPago(
                "T2T",
                ref,
                new BigDecimal(valor),
                "MXN",
                "Ana Ruiz Delgado",
                "801",
                cuentaEmisor,
                null,
                null,
                nombreReceptor,
                instReceptor,
                cuentaReceptor,
                concepto,
                folio);
    }

    private ComandoPago vnt(String ref, String instReceptor, String cuentaReceptor,
                            String nombreReceptor, String valor, String concepto, long folio) {
        return new ComandoPago(
                "VNT",
                ref,
                new BigDecimal(valor),
                "MXN",
                "Marta Solis Vega",
                "801",
                null,
                "0417",
                "INE:IDMEX1734558",
                nombreReceptor,
                instReceptor,
                cuentaReceptor,
                concepto,
                folio);
    }
}