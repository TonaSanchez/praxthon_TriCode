package com.praxthon.sandbox_spei.controller;

import com.praxthon.sandbox_spei.dto.ErrorDetalleDTO;
import com.praxthon.sandbox_spei.dto.PeticionPagoDTO;
import com.praxthon.sandbox_spei.dto.RespuestaOperacionDTO;
import com.praxthon.sandbox_spei.dto.ValidadorDeReglas;
import com.praxthon.sandbox_spei.entity.ClaveIdempotencia;
import com.praxthon.sandbox_spei.entity.Operacion;
import com.praxthon.sandbox_spei.repository.OperacionRepository;
import com.praxthon.sandbox_spei.service.MotorDePagosService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class OperacionController {

    private final ValidadorDeReglas validador;
    private final MotorDePagosService motorService;
    private final OperacionRepository operacionRepository;

    public OperacionController(ValidadorDeReglas validador,
                               MotorDePagosService motorService,
                               OperacionRepository operacionRepository) {
        this.validador = validador;
        this.motorService = motorService;
        this.operacionRepository = operacionRepository;
    }

    @PostMapping("/operaciones")
    public ResponseEntity<?> crearOperacion(
            @RequestHeader(value = "Clave-Idempotencia", required = false) String claveIdempotencia,
            @RequestHeader(value = "X-Escenario-Forzado", required = false) String escenarioForzado,
            @RequestBody PeticionPagoDTO peticion) {

        String hashActual = motorService.calcularHash(peticion);

        Optional<ClaveIdempotencia> idemExistente = motorService.buscarIdempotencia(claveIdempotencia);
        if (idemExistente.isPresent()) {
            ClaveIdempotencia idem = idemExistente.get();
            if (idem.getHashCuerpo().equals(hashActual)) {
                return ResponseEntity.ok(motorService.consultarPorId(idem.getOperacionId()).orElseThrow());
            } else {
                return ResponseEntity.status(409).body(armarRespuestaError(
                        peticion.getReferenciaSeguimiento(),
                        List.of(new ErrorDetalleDTO("PRX-015", "Clave-Idempotencia", "Clave de idempotencia reutilizada con cuerpo distinto"))
                ));
            }
        }

        List<ErrorDetalleDTO> errores = validador.validar(peticion);
        if (!errores.isEmpty()) {
            return ResponseEntity.status(422).body(armarRespuestaError(peticion.getReferenciaSeguimiento(), errores));
        }

        RespuestaOperacionDTO respuesta = motorService.procesarPago(peticion, claveIdempotencia, hashActual, escenarioForzado);
        return ResponseEntity.status(201).body(respuesta);
    }

    @GetMapping("/operaciones")
    public ResponseEntity<?> listarOperaciones(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return ResponseEntity.ok(motorService.listarPaginadoContrato(pagina, tamano));
    }

    @GetMapping("/operaciones/{id}")
    public ResponseEntity<?> consultarPorId(@PathVariable String id) {
        Long idNumerico = extraerIdNumerico(id);
        if (idNumerico == null) {
            return ResponseEntity.status(404).body(armarRespuestaError(null,
                    List.of(new ErrorDetalleDTO(null, "id", "Identificador inexistente"))));
        }
        return motorService.consultarPorId(idNumerico)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(armarRespuestaError(null,
                        List.of(new ErrorDetalleDTO(null, "id", "Identificador inexistente")))));
    }

    @PatchMapping("/operaciones/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable String id, @RequestBody Map<String, String> body) {
        Long idNumerico = extraerIdNumerico(id);
        Optional<Operacion> opOpt = (idNumerico != null) ? operacionRepository.findById(idNumerico) : Optional.empty();
        if (opOpt.isEmpty()) {
            return ResponseEntity.status(404).body(armarRespuestaError(null,
                    List.of(new ErrorDetalleDTO(null, "id", "Identificador inexistente"))));
        }
        try {
            motorService.aplicarTransicion(opOpt.get(), body.get("nuevoEstado"), body.get("motivo"));
            return ResponseEntity.ok(motorService.consultarPorId(idNumerico).orElseThrow());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(422).body(armarRespuestaError(opOpt.get().getReferenciaSeguimiento(),
                    List.of(new ErrorDetalleDTO("PRX-014", "estado", "Transición de estado no permitida"))));
        }
    }

    @GetMapping("/catalogos/instituciones")
    public ResponseEntity<?> obtenerCatalogoInstituciones() {
        return ResponseEntity.ok(List.of(
                Map.of("codigo", "801", "nombre", "Banco Praxis Alfa"),
                Map.of("codigo", "802", "nombre", "Banco Praxis Beta"),
                Map.of("codigo", "803", "nombre", "Banco Praxis Gamma"),
                Map.of("codigo", "804", "nombre", "Praxis Servicios de Pago"),
                Map.of("codigo", "805", "nombre", "Banco Praxis Delta")
        ));
    }

    private Long extraerIdNumerico(String id) {
        try {
            String limpio = id.startsWith("op_") ? id.substring(3) : id;
            return Long.parseLong(limpio);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Map<String, Object> armarRespuestaError(String ref, List<ErrorDetalleDTO> errores) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("referenciaSeguimiento", ref);
        map.put("errores", errores);
        return map;
    }
}