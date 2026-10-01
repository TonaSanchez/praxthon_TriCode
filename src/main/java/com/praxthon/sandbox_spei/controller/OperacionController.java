package com.praxthon.sandbox_spei.controller;

import com.praxthon.sandbox_spei.dto.ErrorDetalleDTO;
import com.praxthon.sandbox_spei.dto.PeticionPagoDTO;
import com.praxthon.sandbox_spei.dto.RespuestaOperacionDTO;
import com.praxthon.sandbox_spei.mapper.PeticionMapper;
import com.praxthon.sandbox_spei.service.MotorDePagosService;
import com.praxthon.sandbox_spei.validation.ValidadorDeReglas;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class OperacionController {

    private final ValidadorDeReglas validador;
    private final MotorDePagosService motorService;

    public OperacionController(ValidadorDeReglas validador, MotorDePagosService motorService) {
        this.validador = validador;
        this.motorService = motorService;
    }

    @PostMapping("/operaciones")
    public ResponseEntity<?> crearOperacion(
            @RequestHeader(value = "Clave-Idempotencia", required = false) String claveIdempotencia,
            @RequestHeader(value = "X-Escenario-Forzado", required = false) String escenarioForzado,
            @RequestBody(required = false) PeticionPagoDTO peticion) {

        if (peticion == null) {
            return error(422, null, List.of(new ErrorDetalleDTO(null, null, "El cuerpo de la solicitud es obligatorio")));
        }
        String ref = peticion.getReferenciaSeguimiento();

        List<ErrorDetalleDTO> erroresEncabezado = validador.validarEncabezados(claveIdempotencia, escenarioForzado);
        if (!erroresEncabezado.isEmpty()) {
            return error(422, ref, erroresEncabezado);
        }

        String hash = PeticionMapper.huella(peticion);

        ResponseEntity<?> reintento = verificarReintentoIdempotente(claveIdempotencia, hash, ref);
        if (reintento != null) {
            return reintento;
        }

        List<ErrorDetalleDTO> errores = validador.validar(peticion);

        if (motorService.existeReferencia(ref)) {
            errores.add(referenciaDuplicada());
        }

        if (!errores.isEmpty()) {
            return error(422, ref, errores);
        }

        try {
            RespuestaOperacionDTO creada = motorService.procesarPago(
                    PeticionMapper.aComando(peticion), claveIdempotencia, hash, escenarioForzado);
            return ResponseEntity.status(201).body(creada);
        } catch (DataIntegrityViolationException e) {
            ResponseEntity<?> carrera = verificarReintentoIdempotente(claveIdempotencia, hash, ref);
            if (carrera != null) {
                return carrera;
            }
            return error(422, ref, List.of(referenciaDuplicada()));
        }
    }

    @GetMapping("/operaciones")
    public ResponseEntity<?> listarOperaciones(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        int paginaSegura = Math.max(pagina, 0);
        int tamanoSeguro = Math.min(Math.max(tamano, 1), 100);
        return ResponseEntity.ok(motorService.listarPaginadoContrato(paginaSegura, tamanoSeguro));
    }

    @GetMapping("/operaciones/{id}")
    public ResponseEntity<?> consultarPorId(@PathVariable String id) {
        Long idNumerico = extraerIdNumerico(id);
        if (idNumerico == null) {
            return noEncontrado();
        }
        Optional<RespuestaOperacionDTO> resultado = motorService.consultarPorId(idNumerico);
        if (resultado.isEmpty()) {
            return noEncontrado();
        }
        return ResponseEntity.ok(resultado.get());
    }

    @PatchMapping("/operaciones/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        Long idNumerico = extraerIdNumerico(id);
        if (idNumerico == null) {
            return noEncontrado();
        }
        Map<String, String> datos = body == null ? Map.of() : body;

        String motivo = datos.get("motivo");
        if (motivo != null && motivo.length() > 10) {
            return error(422, null, List.of(new ErrorDetalleDTO(null, "motivo", "El motivo admite máximo 10 caracteres")));
        }

        try {
            Optional<RespuestaOperacionDTO> resultado = motorService.cambiarEstado(idNumerico, datos.get("nuevoEstado"), motivo);
            if (resultado.isEmpty()) {
                return noEncontrado();
            }
            return ResponseEntity.ok(resultado.get());
        } catch (MotorDePagosService.TransicionInvalidaException e) {
            Optional<RespuestaOperacionDTO> opActual = motorService.consultarPorId(idNumerico);
            String ref = opActual.isPresent() ? opActual.get().getReferenciaSeguimiento() : null;
            return error(422, ref, List.of(new ErrorDetalleDTO("PRX-014", "estado", "Transición de estado no permitida")));
        }
    }

    @GetMapping("/catalogos/instituciones")
    public ResponseEntity<?> obtenerCatalogoInstituciones() {
        List<Map<String, String>> catalogo = new ArrayList<>();
        for (Map.Entry<String, String> entry : ValidadorDeReglas.CATALOGO_INSTITUCIONES.entrySet()) {
            catalogo.add(Map.of("codigo", entry.getKey(), "nombre", entry.getValue()));
        }
        return ResponseEntity.ok(catalogo);
    }

    private ResponseEntity<?> verificarReintentoIdempotente(String clave, String hash, String ref) {
        try {
            Optional<RespuestaOperacionDTO> existente = motorService.resolverReintento(clave, hash);
            if (existente.isPresent()) {
                return ResponseEntity.ok(existente.get());
            }
            return null;
        } catch (MotorDePagosService.ClaveReutilizadaException e) {
            return error(409, ref, List.of(new ErrorDetalleDTO("PRX-015",
                    "Clave-Idempotencia", "Clave de idempotencia reutilizada con cuerpo distinto")));
        }
    }

    private ErrorDetalleDTO referenciaDuplicada() {
        return new ErrorDetalleDTO("PRX-010", "referenciaSeguimiento",
                "La referencia de seguimiento ya fue registrada previamente");
    }

    private ResponseEntity<?> noEncontrado() {
        return error(404, null, List.of(new ErrorDetalleDTO(null, "id", "Identificador inexistente")));
    }

    private ResponseEntity<?> error(int status, String ref, List<ErrorDetalleDTO> errores) {
        return ResponseEntity.status(status).body(ManejadorErrores.cuerpo(ref, errores));
    }

    private Long extraerIdNumerico(String id) {
        try {
            String limpio = id.startsWith("op_") ? id.substring(3) : id;
            return Long.parseLong(limpio);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}