package com.praxthon.sandbox_spei.controller;

import com.praxthon.sandbox_spei.dto.ErrorDetalleDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    private static final Pattern PROPIEDAD = Pattern.compile("\\[\"([^\"]+)\"\\]");

    private static final Map<String, String> CODIGO_POR_CAMPO = Map.of(
            "folioNumerico", "PRX-008",
            "importe.valor", "PRX-004");

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> jsonNoLegible(HttpMessageNotReadableException e) {
        String campo = extraerCampo(e);
        String codigo = campo == null ? null : CODIGO_POR_CAMPO.get(campo);
        String mensaje = campo == null
                ? "Solicitud mal formada: JSON inválido"
                : "Tipo de dato incorrecto en el campo " + campo;
        return ResponseEntity.status(422).body(cuerpo(null, List.of(new ErrorDetalleDTO(codigo, campo, mensaje))));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.status(422).body(cuerpo(null, List.of(
                new ErrorDetalleDTO(null, e.getName(), "Parámetro con tipo de dato incorrecto"))));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception e) {
        if (e instanceof ErrorResponse er) {
            return ResponseEntity.status(er.getStatusCode()).body(cuerpo(null, List.of(
                    new ErrorDetalleDTO(null, null, "Solicitud no procesable"))));
        }
        log.error("Error inesperado", e);
        return ResponseEntity.status(500).body(cuerpo(null, List.of(
                new ErrorDetalleDTO(null, null, "Error interno del servidor"))));
    }

    private static String extraerCampo(HttpMessageNotReadableException e) {
        String texto = e.getMostSpecificCause().getMessage();
        if (texto == null) {
            return null;
        }
        Matcher m = PROPIEDAD.matcher(texto);
        List<String> partes = new ArrayList<>();
        while (m.find()) {
            partes.add(m.group(1));
        }
        return partes.isEmpty() ? null : String.join(".", partes);
    }

    public static Map<String, Object> cuerpo(String ref, List<ErrorDetalleDTO> errores) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("referenciaSeguimiento", ref);
        map.put("errores", errores);
        return map;
    }
}