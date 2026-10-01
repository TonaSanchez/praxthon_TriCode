package com.praxthon.sandbox_spei.service;

import com.praxthon.sandbox_spei.dto.RespuestaOperacionDTO;
import com.praxthon.sandbox_spei.entity.ClaveIdempotencia;
import com.praxthon.sandbox_spei.entity.Operacion;
import com.praxthon.sandbox_spei.entity.Transicion;
import com.praxthon.sandbox_spei.repository.ClaveIdempotenciaRepository;
import com.praxthon.sandbox_spei.repository.OperacionRepository;
import com.praxthon.sandbox_spei.repository.TransicionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class MotorDePagosService {

    public record ComandoPago(
            String tipoOperacion,
            String referenciaSeguimiento,
            BigDecimal importeValor,
            String importeDivisa,
            String emisorNombre,
            String emisorInstitucion,
            String emisorCuenta,
            String emisorSucursal,
            String emisorDocumento,
            String receptorNombre,
            String receptorInstitucion,
            String receptorCuenta,
            String concepto,
            Long folioNumerico) {
    }

    public static class TransicionInvalidaException extends RuntimeException {
        public TransicionInvalidaException(String origen, String destino) {
            super("Transición no permitida: " + origen + " -> " + destino);
        }
    }

    public static class ClaveReutilizadaException extends RuntimeException {
        public ClaveReutilizadaException() {
            super("Clave de idempotencia reutilizada con cuerpo distinto");
        }
    }

    private final OperacionRepository operacionRepository;
    private final TransicionRepository transicionRepository;
    private final ClaveIdempotenciaRepository idempotenciaRepository;

    private static final Map<String, Set<String>> TRANSICIONES_PERMITIDAS = Map.of(
            "RECIBIDO", Set.of("EN_PROCESO", "RECHAZADO"),
            "EN_PROCESO", Set.of("LIQUIDADO", "DEVUELTO", "EN_INVESTIGACION"),
            "EN_INVESTIGACION", Set.of("LIQUIDADO", "DEVUELTO"),
            "LIQUIDADO", Set.of(),
            "DEVUELTO", Set.of(),
            "RECHAZADO", Set.of()
    );

    public MotorDePagosService(OperacionRepository operacionRepository,
                               TransicionRepository transicionRepository,
                               ClaveIdempotenciaRepository idempotenciaRepository) {
        this.operacionRepository = operacionRepository;
        this.transicionRepository = transicionRepository;
        this.idempotenciaRepository = idempotenciaRepository;
    }

    public static boolean transicionPermitida(String origen, String destino) {
        return destino != null && TRANSICIONES_PERMITIDAS.getOrDefault(origen, Set.of()).contains(destino);
    }

    public boolean existeReferencia(String referenciaSeguimiento) {
        if (referenciaSeguimiento == null) {
            return false;
        }
        return operacionRepository.existsByReferenciaSeguimiento(referenciaSeguimiento);
    }

    public Optional<ClaveIdempotencia> buscarIdempotencia(String clave) {
        if (clave == null || clave.trim().isEmpty()) {
            return Optional.empty();
        }
        Optional<ClaveIdempotencia> encontrada = idempotenciaRepository.findById(clave);
        if (encontrada.isPresent() && encontrada.get().getFechaExpiracion().isAfter(LocalDateTime.now())) {
            return encontrada;
        }
        return Optional.empty();
    }

    public Optional<RespuestaOperacionDTO> resolverReintento(String clave, String hash) {
        Optional<ClaveIdempotencia> idemOpt = buscarIdempotencia(clave);
        if (idemOpt.isEmpty()) {
            return Optional.empty();
        }
        ClaveIdempotencia idem = idemOpt.get();
        if (!idem.getHashCuerpo().equals(hash)) {
            throw new ClaveReutilizadaException();
        }
        return consultarPorId(idem.getOperacionId());
    }

    @Transactional
    public RespuestaOperacionDTO procesarPago(ComandoPago cmd, String claveIdem, String hashCuerpo, String escenarioForzado) {
        Operacion op = new Operacion();
        op.setTipoOperacion(cmd.tipoOperacion());
        op.setEstadoActual("RECIBIDO");

        op.setEmisorNombre(cmd.emisorNombre());
        op.setEmisorInstitucion(cmd.emisorInstitucion());
        op.setEmisorCuenta(cmd.emisorCuenta());
        op.setEmisorSucursal(cmd.emisorSucursal());
        op.setEmisorDocumento(cmd.emisorDocumento());

        op.setReceptorNombre(cmd.receptorNombre());
        op.setReceptorInstitucion(cmd.receptorInstitucion());
        op.setReceptorCuenta(cmd.receptorCuenta());

        op.setImporteValor(cmd.importeValor().setScale(2, RoundingMode.HALF_UP));
        op.setImporteDivisa(cmd.importeDivisa());
        op.setConcepto(cmd.concepto());
        op.setFolioNumerico(cmd.folioNumerico());
        op.setReferenciaSeguimiento(cmd.referenciaSeguimiento());

        op = operacionRepository.save(op);
        registrarTransicion(op.getId(), null, "RECIBIDO", null);

        RespuestaOperacionDTO respuestaRecibido = consultarPorId(op.getId()).orElseThrow();

        registrarClaveIdempotencia(op.getId(), claveIdem, hashCuerpo);

        String escenario = resolverEscenario(op, escenarioForzado);
        aplicarTransicion(op, "EN_PROCESO", "S05".equals(escenario) ? "PRX-023" : null);
        switch (escenario) {
            case "S02" -> aplicarTransicion(op, "DEVUELTO", "PRX-020");
            case "S03" -> aplicarTransicion(op, "DEVUELTO", "PRX-021");
            case "S04" -> aplicarTransicion(op, "DEVUELTO", "PRX-022");
            case "S05" -> { }
            case "S06" -> aplicarTransicion(op, "EN_INVESTIGACION", "PRX-024");
            default -> aplicarTransicion(op, "LIQUIDADO", null);
        }

        return respuestaRecibido;
    }

    private String resolverEscenario(Operacion op, String escenarioForzado) {
        if (escenarioForzado != null && !escenarioForzado.isBlank()) {
            return escenarioForzado;
        }
        if ("805".equals(op.getReceptorInstitucion())) {
            return "S04";
        }
        return switch (op.getReceptorCuenta().substring(13, 17)) {
            case "9002" -> "S02";
            case "9003" -> "S03";
            case "9004" -> "S04";
            case "9005" -> "S05";
            case "9006" -> "S06";
            default -> "S01";
        };
    }

    private void registrarClaveIdempotencia(Long operacionId, String clave, String hashCuerpo) {
        if (clave == null || clave.trim().isEmpty()) {
            return;
        }
        Optional<ClaveIdempotencia> previa = idempotenciaRepository.findById(clave);
        if (previa.isPresent()) {
            if (previa.get().getFechaExpiracion().isAfter(LocalDateTime.now())) {
                throw new DataIntegrityViolationException("Clave de idempotencia registrada por otra petición");
            }
            idempotenciaRepository.delete(previa.get());
            idempotenciaRepository.flush();
        }
        ClaveIdempotencia idem = new ClaveIdempotencia();
        idem.setClave(clave);
        idem.setOperacionId(operacionId);
        idem.setHashCuerpo(hashCuerpo);
        idempotenciaRepository.saveAndFlush(idem);
    }

    @Transactional
    public void aplicarTransicion(Operacion op, String nuevoEstado, String motivo) {
        String estadoActual = op.getEstadoActual();
        if (!transicionPermitida(estadoActual, nuevoEstado)) {
            throw new TransicionInvalidaException(estadoActual, nuevoEstado);
        }
        op.setEstadoActual(nuevoEstado);
        op.setMotivoActual(motivo);
        operacionRepository.save(op);
        registrarTransicion(op.getId(), estadoActual, nuevoEstado, motivo);
    }

    @Transactional
    public Optional<RespuestaOperacionDTO> cambiarEstado(Long id, String nuevoEstado, String motivo) {
        Optional<Operacion> op = operacionRepository.buscarParaActualizar(id);
        if (op.isEmpty()) {
            return Optional.empty();
        }
        aplicarTransicion(op.get(), nuevoEstado, motivo);
        return consultarPorId(id);
    }

    private void registrarTransicion(Long operacionId, String origen, String destino, String motivo) {
        Transicion t = new Transicion();
        t.setOperacionId(operacionId);
        t.setEstadoOrigen(origen);
        t.setEstadoDestino(destino);
        t.setMotivo(motivo);
        transicionRepository.save(t);
    }

    public Optional<RespuestaOperacionDTO> consultarPorId(Long id) {
        Optional<Operacion> op = operacionRepository.findById(id);
        if (op.isEmpty()) {
            return Optional.empty();
        }
        List<Transicion> historial = transicionRepository.findByOperacionIdOrderByFechaAscIdAsc(id);
        return Optional.of(new RespuestaOperacionDTO(op.get(), historial));
    }

    public Map<String, Object> listarPaginadoContrato(int pagina, int tamano) {
        PageRequest pageable = PageRequest.of(pagina, tamano, Sort.by(Sort.Order.desc("fechaRegistro"), Sort.Order.desc("id")));
        Page<Operacion> page = operacionRepository.findAll(pageable);

        List<RespuestaOperacionDTO> contenido = new ArrayList<>();
        for (Operacion op : page.getContent()) {
            List<Transicion> historial = transicionRepository.findByOperacionIdOrderByFechaAscIdAsc(op.getId());
            contenido.add(new RespuestaOperacionDTO(op, historial));
        }

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("contenido", contenido);
        respuesta.put("pagina", page.getNumber());
        respuesta.put("tamano", page.getSize());
        respuesta.put("totalElementos", page.getTotalElements());
        respuesta.put("totalPaginas", page.getTotalPages());
        return respuesta;
    }
}