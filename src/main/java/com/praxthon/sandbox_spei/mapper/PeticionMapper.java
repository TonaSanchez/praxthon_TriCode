package com.praxthon.sandbox_spei.mapper;

import com.praxthon.sandbox_spei.dto.PeticionPagoDTO;
import com.praxthon.sandbox_spei.service.MotorDePagosService.ComandoPago;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class PeticionMapper {

    private PeticionMapper() {
    }

    public static ComandoPago aComando(PeticionPagoDTO req) {
        PeticionPagoDTO.EmisorDTO emisor = req.getEmisor();
        PeticionPagoDTO.ReceptorDTO receptor = req.getReceptor();
        PeticionPagoDTO.ImporteDTO importe = req.getImporte();
        PeticionPagoDTO.DocumentoIdentidadDTO doc = emisor.getDocumentoIdentidad();
        String documento = doc == null ? null : doc.getTipo() + ":" + doc.getNumero();
        return new ComandoPago(
                req.getTipoOperacion(),
                req.getReferenciaSeguimiento(),
                importe.getValor(),
                importe.getDivisa(),
                emisor.getNombre(),
                emisor.getInstitucion(),
                emisor.getCuenta(),
                emisor.getSucursal(),
                documento,
                receptor.getNombre(),
                receptor.getInstitucion(),
                receptor.getCuenta(),
                req.getConcepto(),
                req.getFolioNumerico());
    }

    public static String huella(PeticionPagoDTO req) {
        PeticionPagoDTO.EmisorDTO emisor = req.getEmisor();
        PeticionPagoDTO.ReceptorDTO receptor = req.getReceptor();
        PeticionPagoDTO.ImporteDTO importe = req.getImporte();
        PeticionPagoDTO.DocumentoIdentidadDTO doc = emisor != null ? emisor.getDocumentoIdentidad() : null;

        String valorNormalizado = (importe != null && importe.getValor() != null)
                ? importe.getValor().stripTrailingZeros().toString()
                : "";

        String cadena = String.join("|",
                texto(req.getTipoOperacion()),
                texto(req.getReferenciaSeguimiento()),
                valorNormalizado,
                texto(importe != null ? importe.getDivisa() : null),
                texto(emisor != null ? emisor.getInstitucion() : null),
                texto(emisor != null ? emisor.getCuenta() : null),
                texto(emisor != null ? emisor.getNombre() : null),
                texto(emisor != null ? emisor.getIdentificacionFiscal() : null),
                texto(emisor != null ? emisor.getSucursal() : null),
                texto(doc != null ? doc.getTipo() : null),
                texto(doc != null ? doc.getNumero() : null),
                texto(receptor != null ? receptor.getInstitucion() : null),
                texto(receptor != null ? receptor.getCuenta() : null),
                texto(receptor != null ? receptor.getNombre() : null),
                texto(req.getConcepto()),
                req.getFolioNumerico() != null ? String.valueOf(req.getFolioNumerico()) : ""
        );

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(cadena.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Error al generar el hash SHA-256 de la petición", e);
        }
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }
}