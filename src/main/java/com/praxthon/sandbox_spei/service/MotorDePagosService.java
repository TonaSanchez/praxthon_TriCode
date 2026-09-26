package com.praxthon.sandbox_spei.service;

import com.praxthon.sandbox_spei.dto.PeticionPagoDTO;
import com.praxthon.sandbox_spei.dto.RespuestaOperacionDTO;
import com.praxthon.sandbox_spei.entity.ClaveIdempotencia;
import com.praxthon.sandbox_spei.entity.Operacion;
import com.praxthon.sandbox_spei.entity.Transicion;
import com.praxthon.sandbox_spei.repository.ClaveIdempotenciaRepository;
import com.praxthon.sandbox_spei.repository.OperacionRepository;
import com.praxthon.sandbox_spei.repository.TransicionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MotorDePagosService {

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

    public Optional<ClaveIdempotencia> buscarIdempotencia(String clave) {
        if (clave == null || clave.trim().isEmpty()) return Optional.empty();
        return idempotenciaRepository.findById(clave);
    }

    @Transactional
    public RespuestaOperacionDTO procesarPago(PeticionPagoDTO req, String claveIdem, String hashCuerpo, String escenarioForzado) {
        Operacion op = new Operacion();
        op.setTipoOperacion(req.getTipoOperacion());
        op.setEstadoActual("RECIBIDO");

        op.setEmisorNombre(req.getEmisor().getNombre());
        op.setEmisorInstitucion(req.getEmisor().getInstitucion());
        op.setEmisorCuenta(req.getEmisor().getCuenta());
        op.setEmisorSucursal(req.getEmisor().getSucursal());
        if (req.getEmisor().getDocumentoIdentidad() != null) {
            op.setEmisorDocumento(req.getEmisor().getDocumentoIdentidad().getTipo() + ":" + req.getEmisor().getDocumentoIdentidad().getNumero());
        }

        op.setReceptorNombre(req.getReceptor().getNombre());
        op.setReceptorInstitucion(req.getReceptor().getInstitucion());
        op.setReceptorCuenta(req.getReceptor().getCuenta());

        op.setImporteValor(req.getImporte().getValor());
        op.setImporteDivisa(req.getImporte().getDivisa());
        op.setConcepto(req.getConcepto());
        op.setFolioNumerico(req.getFolioNumerico());
        op.setReferenciaSeguimiento(req.getReferenciaSeguimiento());
        op.setClaveIdempotencia(claveIdem);

        op = operacionRepository.save(op);
        registrarTransicion(op.getId(), null, "RECIBIDO", null);

        if (claveIdem != null && !claveIdem.trim().isEmpty()) {
            ClaveIdempotencia idem = new ClaveIdempotencia();
            idem.setClave(claveIdem);
            idem.setOperacionId(op.getId());
            idem.setHashCuerpo(hashCuerpo);
            idempotenciaRepository.save(idem);
        }

        aplicarTransicion(op, "EN_PROCESO", null);

        String digitos14a17 = op.getReceptorCuenta().substring(13, 17);

        if (escenarioForzado != null && !escenarioForzado.isBlank()) {
            switch (escenarioForzado) {
                case "S02" -> aplicarTransicion(op, "DEVUELTO", "PRX-020");
                case "S03" -> aplicarTransicion(op, "DEVUELTO", "PRX-021");
                case "S04" -> aplicarTransicion(op, "DEVUELTO", "PRX-022");
                case "S05" -> { op.setMotivoActual("PRX-023"); operacionRepository.save(op); }
                case "S06" -> aplicarTransicion(op, "EN_INVESTIGACION", "PRX-024");
                default -> aplicarTransicion(op, "LIQUIDADO", null);
            }
        } else if ("805".equals(op.getReceptorInstitucion()) || "9004".equals(digitos14a17)) {
            aplicarTransicion(op, "DEVUELTO", "PRX-022");
        } else if ("9002".equals(digitos14a17)) {
            aplicarTransicion(op, "DEVUELTO", "PRX-020");
        } else if ("9003".equals(digitos14a17)) {
            aplicarTransicion(op, "DEVUELTO", "PRX-021");
        } else if ("9005".equals(digitos14a17)) {
            op.setMotivoActual("PRX-023");
            operacionRepository.save(op);
        } else if ("9006".equals(digitos14a17)) {
            aplicarTransicion(op, "EN_INVESTIGACION", "PRX-024");
        } else {
            aplicarTransicion(op, "LIQUIDADO", null);
        }

        return consultarPorId(op.getId()).orElseThrow();
    }

    @Transactional
    public void aplicarTransicion(Operacion op, String nuevoEstado, String motivo) {
        String estadoActual = op.getEstadoActual();
        Set<String> permitidos = TRANSICIONES_PERMITIDAS.getOrDefault(estadoActual, Set.of());
        if (!permitidos.contains(nuevoEstado)) {
            throw new IllegalStateException("PRX-014");
        }
        op.setEstadoActual(nuevoEstado);
        op.setMotivoActual(motivo);
        operacionRepository.save(op);
        registrarTransicion(op.getId(), estadoActual, nuevoEstado, motivo);
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
        return operacionRepository.findById(id).map(op -> {
            List<Transicion> historial = transicionRepository.findByOperacionIdOrderByFechaAsc(id);
            return new RespuestaOperacionDTO(op, historial);
        });
    }

    public Map<String, Object> listarPaginadoContrato(int pagina, int tamano) {
        PageRequest pageable = PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "fechaRegistro"));
        Page<Operacion> page = operacionRepository.findAll(pageable);
        List<RespuestaOperacionDTO> contenido = page.getContent().stream()
                .map(op -> new RespuestaOperacionDTO(op, transicionRepository.findByOperacionIdOrderByFechaAsc(op.getId())))
                .collect(Collectors.toList());

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("contenido", contenido);
        respuesta.put("pagina", page.getNumber());
        respuesta.put("tamano", page.getSize());
        respuesta.put("totalElementos", page.getTotalElements());
        respuesta.put("totalPaginas", page.getTotalPages());
        return respuesta;
    }

    public String calcularHash(PeticionPagoDTO req) {
        try {
            String doc = (req.getEmisor() != null && req.getEmisor().getDocumentoIdentidad() != null)
                    ? req.getEmisor().getDocumentoIdentidad().getTipo() + req.getEmisor().getDocumentoIdentidad().getNumero() : "";
            String raw = req.getTipoOperacion() + "|" +
                    (req.getEmisor() != null ? req.getEmisor().getCuenta() + req.getEmisor().getNombre() + doc : "") + "|" +
                    (req.getReceptor() != null ? req.getReceptor().getCuenta() : "") + "|" +
                    (req.getImporte() != null ? req.getImporte().getValor() : "") + "|" +
                    req.getConcepto() + "|" + req.getFolioNumerico() + "|" + req.getReferenciaSeguimiento();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return String.valueOf(req.hashCode());
        }
    }
}