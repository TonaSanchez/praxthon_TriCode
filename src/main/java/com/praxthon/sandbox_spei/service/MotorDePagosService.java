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

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

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
            throw new IllegalStateException("PRX-015");
        }
        return consultarPorId(idem.getOperacionId());
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

        op.setImporteValor(req.getImporte().getValor().setScale(2, RoundingMode.HALF_UP));
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

        String digitos14a17 = op.getReceptorCuenta().substring(13, 17);
        boolean esEscenarioSinRespuesta = "S05".equals(escenarioForzado)
                || ((escenarioForzado == null || escenarioForzado.isBlank()) && "9005".equals(digitos14a17));

        if (esEscenarioSinRespuesta) {
            aplicarTransicion(op, "EN_PROCESO", "PRX-023");
            return consultarPorId(op.getId()).orElseThrow();
        }

        aplicarTransicion(op, "EN_PROCESO", null);

        if (escenarioForzado != null && !escenarioForzado.isBlank()) {
            switch (escenarioForzado) {
                case "S02" -> aplicarTransicion(op, "DEVUELTO", "PRX-020");
                case "S03" -> aplicarTransicion(op, "DEVUELTO", "PRX-021");
                case "S04" -> aplicarTransicion(op, "DEVUELTO", "PRX-022");
                case "S06" -> aplicarTransicion(op, "EN_INVESTIGACION", "PRX-024");
                default -> aplicarTransicion(op, "LIQUIDADO", null);
            }
        } else if ("805".equals(op.getReceptorInstitucion()) || "9004".equals(digitos14a17)) {
            aplicarTransicion(op, "DEVUELTO", "PRX-022");
        } else if ("9002".equals(digitos14a17)) {
            aplicarTransicion(op, "DEVUELTO", "PRX-020");
        } else if ("9003".equals(digitos14a17)) {
            aplicarTransicion(op, "DEVUELTO", "PRX-021");
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
        if (nuevoEstado == null || !permitidos.contains(nuevoEstado)) {
            throw new IllegalStateException("PRX-014");
        }
        op.setEstadoActual(nuevoEstado);
        op.setMotivoActual(motivo);
        operacionRepository.save(op);
        registrarTransicion(op.getId(), estadoActual, nuevoEstado, motivo);
    }

    @Transactional
    public Optional<RespuestaOperacionDTO> cambiarEstado(Long id, String nuevoEstado, String motivo) {
        Optional<Operacion> op = operacionRepository.findById(id);
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

    public String calcularHash(PeticionPagoDTO req) {
        PeticionPagoDTO.EmisorDTO emisor = req.getEmisor();
        PeticionPagoDTO.ReceptorDTO receptor = req.getReceptor();
        PeticionPagoDTO.ImporteDTO importe = req.getImporte();
        PeticionPagoDTO.DocumentoIdentidadDTO doc = emisor != null ? emisor.getDocumentoIdentidad() : null;

        String valorNormalizado = (importe != null && importe.getValor() != null)
                ? importe.getValor().stripTrailingZeros().toPlainString()
                : "";

        String cadena = String.join("|",
                textoSeguro(req.getTipoOperacion()),
                textoSeguro(req.getReferenciaSeguimiento()),
                valorNormalizado,
                textoSeguro(importe != null ? importe.getDivisa() : null),
                textoSeguro(emisor != null ? emisor.getInstitucion() : null),
                textoSeguro(emisor != null ? emisor.getCuenta() : null),
                textoSeguro(emisor != null ? emisor.getNombre() : null),
                textoSeguro(emisor != null ? emisor.getIdentificacionFiscal() : null),
                textoSeguro(emisor != null ? emisor.getSucursal() : null),
                textoSeguro(doc != null ? doc.getTipo() : null),
                textoSeguro(doc != null ? doc.getNumero() : null),
                textoSeguro(receptor != null ? receptor.getInstitucion() : null),
                textoSeguro(receptor != null ? receptor.getCuenta() : null),
                textoSeguro(receptor != null ? receptor.getNombre() : null),
                textoSeguro(req.getConcepto()),
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

    private String textoSeguro(String valor) {
        return valor == null ? "" : valor;
    }
}