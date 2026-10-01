package com.praxthon.sandbox_spei.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "operacion")
public class Operacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_operacion", length = 3, nullable = false)
    private String tipoOperacion;

    @Column(name = "estado_actual", length = 20, nullable = false)
    private String estadoActual;

    @Column(name = "motivo_actual", length = 10)
    private String motivoActual;

    @Column(name = "emisor_nombre", length = 40, nullable = false)
    private String emisorNombre;

    @Column(name = "emisor_institucion", length = 3, nullable = false)
    private String emisorInstitucion;

    @Column(name = "emisor_cuenta", length = 18)
    private String emisorCuenta;

    @Column(name = "emisor_sucursal", length = 20)
    private String emisorSucursal;

    @Column(name = "emisor_documento", length = 30)
    private String emisorDocumento;

    @Column(name = "receptor_nombre", length = 40, nullable = false)
    private String receptorNombre;

    @Column(name = "receptor_institucion", length = 3, nullable = false)
    private String receptorInstitucion;

    @Column(name = "receptor_cuenta", length = 18, nullable = false)
    private String receptorCuenta;

    @Column(name = "importe_valor", precision = 12, scale = 2, nullable = false)
    private BigDecimal importeValor;

    @Column(name = "importe_divisa", length = 3, nullable = false)
    private String importeDivisa = "MXN";

    @Column(name = "concepto", length = 40, nullable = false)
    private String concepto;

    @Column(name = "folio_numerico", nullable = false)
    private Long folioNumerico;

    @Column(name = "referencia_seguimiento", length = 30, nullable = false, unique = true)
    private String referenciaSeguimiento;

    @Column(name = "clave_idempotencia", length = 64, unique = true)
    private String claveIdempotencia;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void onCreate() {
        LocalDateTime ahora = LocalDateTime.now();
        this.fechaRegistro = ahora;
        this.fechaActualizacion = ahora;
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTipoOperacion() { return tipoOperacion; }
    public void setTipoOperacion(String tipoOperacion) { this.tipoOperacion = tipoOperacion; }
    public String getEstadoActual() { return estadoActual; }
    public void setEstadoActual(String estadoActual) { this.estadoActual = estadoActual; }
    public String getMotivoActual() { return motivoActual; }
    public void setMotivoActual(String motivoActual) { this.motivoActual = motivoActual; }
    public String getEmisorNombre() { return emisorNombre; }
    public void setEmisorNombre(String emisorNombre) { this.emisorNombre = emisorNombre; }
    public String getEmisorInstitucion() { return emisorInstitucion; }
    public void setEmisorInstitucion(String emisorInstitucion) { this.emisorInstitucion = emisorInstitucion; }
    public String getEmisorCuenta() { return emisorCuenta; }
    public void setEmisorCuenta(String emisorCuenta) { this.emisorCuenta = emisorCuenta; }
    public String getEmisorSucursal() { return emisorSucursal; }
    public void setEmisorSucursal(String emisorSucursal) { this.emisorSucursal = emisorSucursal; }
    public String getEmisorDocumento() { return emisorDocumento; }
    public void setEmisorDocumento(String emisorDocumento) { this.emisorDocumento = emisorDocumento; }
    public String getReceptorNombre() { return receptorNombre; }
    public void setReceptorNombre(String receptorNombre) { this.receptorNombre = receptorNombre; }
    public String getReceptorInstitucion() { return receptorInstitucion; }
    public void setReceptorInstitucion(String receptorInstitucion) { this.receptorInstitucion = receptorInstitucion; }
    public String getReceptorCuenta() { return receptorCuenta; }
    public void setReceptorCuenta(String receptorCuenta) { this.receptorCuenta = receptorCuenta; }
    public BigDecimal getImporteValor() { return importeValor; }
    public void setImporteValor(BigDecimal importeValor) { this.importeValor = importeValor; }
    public String getImporteDivisa() { return importeDivisa; }
    public void setImporteDivisa(String importeDivisa) { this.importeDivisa = importeDivisa; }
    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }
    public Long getFolioNumerico() { return folioNumerico; }
    public void setFolioNumerico(Long folioNumerico) { this.folioNumerico = folioNumerico; }
    public String getReferenciaSeguimiento() { return referenciaSeguimiento; }
    public void setReferenciaSeguimiento(String referenciaSeguimiento) { this.referenciaSeguimiento = referenciaSeguimiento; }
    public String getClaveIdempotencia() { return claveIdempotencia; }
    public void setClaveIdempotencia(String claveIdempotencia) { this.claveIdempotencia = claveIdempotencia; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}