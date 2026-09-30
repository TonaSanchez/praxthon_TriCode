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

    private static final int TOTAL_SEMILLA = 120;
    private static final String[] RESERVADAS_DEVUELTO = {"9002", "9003", "9004"};

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

        sembrarRechazada("SEMILLA0004", EMISOR_T2T_C, RECEPTOR_RECHAZADO, "750.00", 4L);

        for (int i = 5; i <= TOTAL_SEMILLA; i++) {
            String ref = String.format("SEMILLA%04d", i);
            String emisor = clabe("801", 2000 + i);
            switch (i % 4) {
                case 0 -> motor.procesarPago(t2t(ref, emisor, "802", clabe("802", 1000 + i),
                        "Luis Cano Mora", "100.00", "Pago semilla", i), null, null, null);
                case 1 -> motor.procesarPago(vnt(ref, "803", clabe("803", 1000 + i),
                        "Comercial Vega SA de CV", "250.00", "Deposito semilla", i), null, null, null);
                case 2 -> {
                    int reservada = Integer.parseInt(RESERVADAS_DEVUELTO[(i / 4) % RESERVADAS_DEVUELTO.length]);
                    motor.procesarPago(t2t(ref, emisor, "802", clabe("802", reservada),
                            "Maria Lopez Ruiz", "400.00", "Devolucion semilla", i), null, null, null);
                }
                default -> sembrarRechazada(ref, emisor, clabe("802", 3000 + i), "500.00", i);
            }
        }
    }

    private void sembrarRechazada(String ref, String cuentaEmisor, String cuentaReceptor, String importe, long folio) {
        Operacion op = new Operacion();
        op.setTipoOperacion("T2T");
        op.setEstadoActual("RECIBIDO");
        op.setEmisorNombre("Ana Ruiz Delgado");
        op.setEmisorInstitucion("801");
        op.setEmisorCuenta(cuentaEmisor);
        op.setReceptorNombre("Luis Cano Mora");
        op.setReceptorInstitucion("802");
        op.setReceptorCuenta(cuentaReceptor);
        op.setImporteValor(new BigDecimal(importe));
        op.setImporteDivisa("MXN");
        op.setConcepto("Rechazo de ejemplo");
        op.setFolioNumerico(folio);
        op.setReferenciaSeguimiento(ref);
        op = operacionRepository.save(op);

        Transicion inicial = new Transicion();
        inicial.setOperacionId(op.getId());
        inicial.setEstadoOrigen(null);
        inicial.setEstadoDestino("RECIBIDO");
        transicionRepository.save(inicial);

        motor.aplicarTransicion(op, "RECHAZADO", null);
    }

    private static int digito(String primeros17) {
        int[] pesos = {3, 7, 1};
        int suma = 0;
        for (int i = 0; i < 17; i++) {
            suma += ((primeros17.charAt(i) - '0') * pesos[i % 3]) % 10;
        }
        return (10 - (suma % 10)) % 10;
    }

    private static String clabe(String institucion, int ultimos4) {
        String base = institucion + "180" + "0000000" + String.format("%04d", ultimos4);
        return base + digito(base);
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