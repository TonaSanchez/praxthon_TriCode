package com.praxthon.sandbox_spei.validation;
import com.praxthon.sandbox_spei.dto.ErrorDetalleDTO;
import com.praxthon.sandbox_spei.dto.PeticionPagoDTO;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Component
public class ValidadorDeReglas {

    public static final Map<String, String> CATALOGO_INSTITUCIONES = Collections.unmodifiableMap(new TreeMap<>(Map.of(
            "801", "Banco Praxis Alfa",
            "802", "Banco Praxis Beta",
            "803", "Banco Praxis Gamma",
            "804", "Praxis Servicios de Pago",
            "805", "Banco Praxis Delta")));

    private static final Set<String> ESCENARIOS_VALIDOS = Set.of("S01", "S02", "S03", "S04", "S05", "S06");

    private static final BigDecimal IMPORTE_MAXIMO = new BigDecimal("1000000.00");
    private static final int DIGITOS_ENTEROS_MAXIMOS = 7;

    public List<ErrorDetalleDTO> validarEncabezados(String claveIdem, String escenario) {
        List<ErrorDetalleDTO> errores = new ArrayList<>();
        if (escenario != null && !escenario.isBlank() && !ESCENARIOS_VALIDOS.contains(escenario)) {
            errores.add(new ErrorDetalleDTO(null, "X-Escenario-Forzado", "Escenario forzado inválido; valores permitidos: S01 a S06"));
        }
        if (claveIdem != null && claveIdem.length() > 64) {
            errores.add(new ErrorDetalleDTO(null, "Clave-Idempotencia", "La clave de idempotencia no debe exceder 64 caracteres"));
        }
        return errores;
    }

    public List<ErrorDetalleDTO> validar(PeticionPagoDTO req) {
        List<ErrorDetalleDTO> errores = new ArrayList<>();

        String tipo = req.getTipoOperacion();
        boolean esT2T = "T2T".equals(tipo);
        boolean esVNT = "VNT".equals(tipo);
        if (!esT2T && !esVNT) {
            errores.add(new ErrorDetalleDTO("PRX-031", "tipoOperacion", "El tipo de operación debe ser T2T o VNT"));
        }

        if (req.getReceptor() != null) {
            String recNombre = req.getReceptor().getNombre();
            String recInst = req.getReceptor().getInstitucion();
            String recCuenta = req.getReceptor().getCuenta();
            boolean instReceptoraValida = recInst != null && CATALOGO_INSTITUCIONES.containsKey(recInst);

            if (recNombre == null || recNombre.trim().isEmpty() || recNombre.length() > 40) {
                errores.add(new ErrorDetalleDTO("PRX-011", "receptor.nombre", "Nombre del receptor obligatorio (1 a 40 caracteres)"));
            }
            if (!instReceptoraValida) {
                errores.add(new ErrorDetalleDTO("PRX-003", "receptor.institucion", "Institución receptora no existe en el catálogo"));
            }
            if (recCuenta == null || !recCuenta.matches("\\d{18}")) {
                errores.add(new ErrorDetalleDTO("PRX-001", "receptor.cuenta", "La cuenta receptora debe tener 18 dígitos numéricos"));
            } else {
                if (!validarDigitoVerificador(recCuenta)) {
                    errores.add(new ErrorDetalleDTO("PRX-002", "receptor.cuenta", "Dígito verificador de la CLABE incorrecto"));
                }
                if (instReceptoraValida && !recCuenta.startsWith(recInst)) {
                    errores.add(new ErrorDetalleDTO("PRX-030", "receptor.cuenta", "Los 3 primeros dígitos no coinciden con la institución"));
                }
            }
        } else {
            errores.add(new ErrorDetalleDTO("PRX-011", "receptor", "Datos del receptor obligatorios"));
        }

        if (req.getEmisor() != null) {
            String emiNombre = req.getEmisor().getNombre();
            String emiInst = req.getEmisor().getInstitucion();
            String emiCuenta = req.getEmisor().getCuenta();
            String emiSucursal = req.getEmisor().getSucursal();
            PeticionPagoDTO.DocumentoIdentidadDTO emiDoc = req.getEmisor().getDocumentoIdentidad();
            boolean instEmisoraValida = emiInst != null && CATALOGO_INSTITUCIONES.containsKey(emiInst) && !"804".equals(emiInst);

            for (String campoExtra : req.getEmisor().getCamposDesconocidos().keySet()) {
                errores.add(new ErrorDetalleDTO(null, "emisor." + campoExtra, "Campo no permitido en el emisor"));
            }

            if (emiNombre == null || emiNombre.trim().isEmpty() || emiNombre.length() > 40) {
                errores.add(new ErrorDetalleDTO("PRX-011", "emisor.nombre", "Nombre del emisor obligatorio (1 a 40 caracteres)"));
            }
            if (!instEmisoraValida) {
                errores.add(new ErrorDetalleDTO("PRX-003", "emisor.institucion", "Institución emisora inválida o no permitida para enviar"));
            }

            if (esT2T) {
                if (emiCuenta == null || emiCuenta.trim().isEmpty()) {
                    errores.add(new ErrorDetalleDTO("PRX-011", "emisor.cuenta", "La cuenta emisora es obligatoria en T2T"));
                } else {
                    if (!emiCuenta.matches("\\d{18}")) {
                        errores.add(new ErrorDetalleDTO("PRX-001", "emisor.cuenta", "La cuenta emisora debe tener 18 dígitos numéricos"));
                    } else {
                        if (!validarDigitoVerificador(emiCuenta)) {
                            errores.add(new ErrorDetalleDTO("PRX-002", "emisor.cuenta", "Dígito verificador de cuenta emisora incorrecto"));
                        }
                        if (instEmisoraValida && !emiCuenta.startsWith(emiInst)) {
                            errores.add(new ErrorDetalleDTO("PRX-030", "emisor.cuenta", "Los 3 primeros dígitos no coinciden con la institución emisora"));
                        }
                    }
                    if (req.getReceptor() != null && emiCuenta.equals(req.getReceptor().getCuenta())) {
                        errores.add(new ErrorDetalleDTO("PRX-013", "emisor.cuenta", "La cuenta emisora y receptora no pueden ser iguales"));
                    }
                }
                if (emiSucursal != null) {
                    errores.add(new ErrorDetalleDTO("PRX-012", "emisor.sucursal", "La sucursal no debe enviarse en T2T"));
                }
                if (emiDoc != null) {
                    errores.add(new ErrorDetalleDTO("PRX-012", "emisor.documentoIdentidad", "El documento de identidad no debe enviarse en T2T"));
                }
            }

            if (esVNT) {
                if (emiSucursal == null || emiSucursal.trim().isEmpty()) {
                    errores.add(new ErrorDetalleDTO("PRX-011", "emisor.sucursal", "La sucursal es obligatoria en VNT"));
                } else if (emiSucursal.length() > 20) {
                    errores.add(new ErrorDetalleDTO(null, "emisor.sucursal", "La sucursal admite máximo 20 caracteres"));
                }
                if (emiDoc == null || emiDoc.getTipo() == null || emiDoc.getTipo().trim().isEmpty()
                        || emiDoc.getNumero() == null || emiDoc.getNumero().trim().isEmpty()) {
                    errores.add(new ErrorDetalleDTO("PRX-011", "emisor.documentoIdentidad", "El documento de identidad (tipo y numero) es obligatorio en VNT"));
                } else if (emiDoc.getTipo().length() + 1 + emiDoc.getNumero().length() > 30) {
                    errores.add(new ErrorDetalleDTO(null, "emisor.documentoIdentidad", "Tipo y número del documento exceden 30 caracteres"));
                }
                if (emiCuenta != null) {
                    errores.add(new ErrorDetalleDTO("PRX-012", "emisor.cuenta", "La cuenta emisora no debe enviarse en VNT"));
                }
                if (req.getEmisor().getIdentificacionFiscal() != null) {
                    errores.add(new ErrorDetalleDTO("PRX-012", "emisor.identificacionFiscal", "La identificación fiscal no debe enviarse en VNT"));
                }
            }
        } else {
            errores.add(new ErrorDetalleDTO("PRX-011", "emisor", "Datos del emisor obligatorios"));
        }

        if (req.getImporte() != null) {
            BigDecimal valor = req.getImporte().getValor();
            String divisa = req.getImporte().getDivisa();

            if (valor == null || valor.signum() <= 0) {
                errores.add(new ErrorDetalleDTO("PRX-004", "importe.valor", "El importe debe ser mayor que cero"));
            } else if (excedeMagnitud(valor) || valor.compareTo(IMPORTE_MAXIMO) > 0 || valor.stripTrailingZeros().scale() > 2) {
                errores.add(new ErrorDetalleDTO("PRX-005", "importe.valor", "Importe máximo 1,000,000.00 y máximo 2 decimales"));
            }
            if (!"MXN".equals(divisa)) {
                errores.add(new ErrorDetalleDTO("PRX-006", "importe.divisa", "La divisa debe ser MXN"));
            }
        } else {
            errores.add(new ErrorDetalleDTO("PRX-004", "importe.valor", "El importe es obligatorio"));
        }

        if (req.getConcepto() == null || req.getConcepto().trim().isEmpty() || req.getConcepto().length() > 40) {
            errores.add(new ErrorDetalleDTO("PRX-007", "concepto", "Concepto obligatorio entre 1 y 40 caracteres"));
        }

        if (req.getFolioNumerico() == null || req.getFolioNumerico() < 1 || req.getFolioNumerico() > 9999999) {
            errores.add(new ErrorDetalleDTO("PRX-008", "folioNumerico", "El folio numérico debe estar entre 1 y 9,999,999"));
        }

        String ref = req.getReferenciaSeguimiento();
        if (ref == null || !ref.matches("[a-zA-Z0-9]{1,30}")) {
            errores.add(new ErrorDetalleDTO("PRX-009", "referenciaSeguimiento", "Referencia obligatoria, alfanumérica de 1 a 30 caracteres"));
        }

        return errores;
    }

    private boolean excedeMagnitud(BigDecimal valor) {
        return (long) valor.precision() - (long) valor.scale() > DIGITOS_ENTEROS_MAXIMOS;
    }

    public boolean validarDigitoVerificador(String clabe) {
        String primeros17 = clabe.substring(0, 17);
        int digitoReal = clabe.charAt(17) - '0';
        return digitoVerificador(primeros17) == digitoReal;
    }

    public int digitoVerificador(String primeros17) {
        int[] pesos = {3, 7, 1};
        int suma = 0;
        for (int i = 0; i < 17; i++) {
            int digito = primeros17.charAt(i) - '0';
            suma += (digito * pesos[i % 3]) % 10;
        }
        return (10 - (suma % 10)) % 10;
    }
}