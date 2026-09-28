package com.praxthon.sandbox_spei.controller;

import com.praxthon.sandbox_spei.dto.ErrorDetalleDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler({HttpMessageNotReadableException.class,
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> malFormada(Exception e) {
        return ResponseEntity.status(422).body(cuerpo(null, List.of(
                new ErrorDetalleDTO(null, null, "Solicitud mal formada: JSON inválido o tipo de dato incorrecto"))));
    }

    public static Map<String, Object> cuerpo(String ref, List<ErrorDetalleDTO> errores) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("referenciaSeguimiento", ref);
        map.put("errores", errores);
        return map;
    }
}