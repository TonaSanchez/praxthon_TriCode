package com.praxthon.sandbox_spei.dto;

import com.praxthon.sandbox_spei.entity.Operacion;
import com.praxthon.sandbox_spei.entity.Transicion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class RespuestaOperacionDTO {
    private String id;
    private String referenciaSeguimiento;
    private String estado;
    private String tipoOperacion;
    private PeticionPagoDTO.ImporteDTO importe;
    private LocalDateTime fechaRegistro;
    private List<TransicionDTO> transiciones;

    public static class TransicionDTO {
        private String estado;
        private LocalDateTime momento;
        private String motivo;

        public TransicionDTO(String estado, LocalDateTime momento, String motivo) {
            this.estado = estado;
            this.momento = momento;
            this.motivo = motivo;
        }
        public String getEstado() { return estado; }
        public LocalDateTime getMomento() { return momento; }
        public String getMotivo() { return motivo; }
    }

    public RespuestaOperacionDTO(Operacion op, List<Transicion> historial) {
        this.id = "op_" + op.getId();
        this.referenciaSeguimiento = op.getReferenciaSeguimiento();
        this.estado = op.getEstadoActual();
        this.tipoOperacion = op.getTipoOperacion();
        this.importe = new PeticionPagoDTO.ImporteDTO(op.getImporteValor(), op.getImporteDivisa());
        this.fechaRegistro = op.getFechaRegistro();
        this.transiciones = historial.stream()
                .map(t -> new TransicionDTO(t.getEstadoDestino(), t.getFecha(), t.getMotivo()))
                .collect(Collectors.toList());
    }

    public String getId() { return id; }
    public String getReferenciaSeguimiento() { return referenciaSeguimiento; }
    public String getEstado() { return estado; }
    public String getTipoOperacion() { return tipoOperacion; }
    public PeticionPagoDTO.ImporteDTO getImporte() { return importe; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public List<TransicionDTO> getTransiciones() { return transiciones; }
}