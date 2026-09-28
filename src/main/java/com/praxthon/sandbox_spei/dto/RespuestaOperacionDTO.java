package com.praxthon.sandbox_spei.dto;
import com.praxthon.sandbox_spei.entity.Operacion;
import com.praxthon.sandbox_spei.entity.Transicion;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

public class RespuestaOperacionDTO {
    private String id;
    private String referenciaSeguimiento;
    private String estado;
    private String tipoOperacion;
    private PeticionPagoDTO.ImporteDTO importe;
    private OffsetDateTime fechaRegistro;
    private List<TransicionDTO> transiciones;

    public static class TransicionDTO {
        private String estado;
        private OffsetDateTime momento;
        private String motivo;

        public TransicionDTO(String estado, OffsetDateTime momento, String motivo) {
            this.estado = estado;
            this.momento = momento;
            this.motivo = motivo;
        }
        public String getEstado() { return estado; }
        public OffsetDateTime getMomento() { return momento; }
        public String getMotivo() { return motivo; }
    }

    public RespuestaOperacionDTO(Operacion op, List<Transicion> historial) {
        this.id = "op_" + op.getId();
        this.referenciaSeguimiento = op.getReferenciaSeguimiento();
        this.estado = op.getEstadoActual();
        this.tipoOperacion = op.getTipoOperacion();
        this.importe = new PeticionPagoDTO.ImporteDTO(op.getImporteValor(), op.getImporteDivisa());
        this.fechaRegistro = aOffset(op.getFechaRegistro());
        this.transiciones = historial.stream()
                .map(t -> new TransicionDTO(t.getEstadoDestino(), aOffset(t.getFecha()), t.getMotivo()))
                .collect(Collectors.toList());
    }

    private static OffsetDateTime aOffset(LocalDateTime fecha) {
        return fecha == null ? null : fecha.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    public String getId() { return id; }
    public String getReferenciaSeguimiento() { return referenciaSeguimiento; }
    public String getEstado() { return estado; }
    public String getTipoOperacion() { return tipoOperacion; }
    public PeticionPagoDTO.ImporteDTO getImporte() { return importe; }
    public OffsetDateTime getFechaRegistro() { return fechaRegistro; }
    public List<TransicionDTO> getTransiciones() { return transiciones; }
}