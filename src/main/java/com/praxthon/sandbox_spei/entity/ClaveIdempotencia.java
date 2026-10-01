package com.praxthon.sandbox_spei.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "clave_idempotencia")
public class ClaveIdempotencia {

    @Id
    @Column(name = "clave", length = 64, nullable = false)
    private String clave;

    @Column(name = "operacion_id", nullable = false, unique = true)
    private Long operacionId;

    @Column(name = "hash_cuerpo", length = 64, nullable = false, columnDefinition = "CHAR(64)")
    private String hashCuerpo;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @PrePersist
    protected void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        this.fechaCreacion = ahora;
        if (this.fechaExpiracion == null) {
            this.fechaExpiracion = ahora.plusHours(24);
        }
    }

    // Getters y Setters
    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public Long getOperacionId() { return operacionId; }
    public void setOperacionId(Long operacionId) { this.operacionId = operacionId; }
    public String getHashCuerpo() { return hashCuerpo; }
    public void setHashCuerpo(String hashCuerpo) { this.hashCuerpo = hashCuerpo; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public LocalDateTime getFechaExpiracion() { return fechaExpiracion; }
    public void setFechaExpiracion(LocalDateTime fechaExpiracion) { this.fechaExpiracion = fechaExpiracion; }
}