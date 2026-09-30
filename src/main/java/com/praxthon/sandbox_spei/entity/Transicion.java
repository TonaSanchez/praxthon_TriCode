package com.praxthon.sandbox_spei.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "transicion", indexes = @Index(name = "idx_transicion_operacion", columnList = "operacion_id"))
public class Transicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operacion_id", nullable = false)
    private Long operacionId;

    @Column(name = "estado_origen", length = 20)
    private String estadoOrigen;

    @Column(name = "estado_destino", length = 20, nullable = false)
    private String estadoDestino;

    @Column(name = "motivo", length = 10)
    private String motivo;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    @PrePersist
    protected void onCreate() {
        if (this.fecha == null) {
            this.fecha = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOperacionId() { return operacionId; }
    public void setOperacionId(Long operacionId) { this.operacionId = operacionId; }
    public String getEstadoOrigen() { return estadoOrigen; }
    public void setEstadoOrigen(String estadoOrigen) { this.estadoOrigen = estadoOrigen; }
    public String getEstadoDestino() { return estadoDestino; }
    public void setEstadoDestino(String estadoDestino) { this.estadoDestino = estadoDestino; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}