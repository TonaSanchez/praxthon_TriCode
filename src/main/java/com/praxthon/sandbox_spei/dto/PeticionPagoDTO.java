package com.praxthon.sandbox_spei.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

public class PeticionPagoDTO {
    private String tipoOperacion;
    private String referenciaSeguimiento;
    private ImporteDTO importe;
    private EmisorDTO emisor;
    private ReceptorDTO receptor;
    private String concepto;
    private Long folioNumerico;

    public static class EmisorDTO {
        private String institucion;
        private String cuenta;
        private String nombre;
        private String identificacionFiscal;
        private String sucursal;
        private DocumentoIdentidadDTO documentoIdentidad;
        private final Map<String, Object> camposDesconocidos = new LinkedHashMap<>();

        @JsonAnySetter
        public void agregarCampoDesconocido(String nombre, Object valor) {
            camposDesconocidos.put(nombre, valor);
        }

        @JsonIgnore
        public Map<String, Object> getCamposDesconocidos() { return camposDesconocidos; }

        public String getInstitucion() { return institucion; }
        public void setInstitucion(String institucion) { this.institucion = institucion; }
        public String getCuenta() { return cuenta; }
        public void setCuenta(String cuenta) { this.cuenta = cuenta; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getIdentificacionFiscal() { return identificacionFiscal; }
        public void setIdentificacionFiscal(String identificacionFiscal) { this.identificacionFiscal = identificacionFiscal; }
        public String getSucursal() { return sucursal; }
        public void setSucursal(String sucursal) { this.sucursal = sucursal; }
        public DocumentoIdentidadDTO getDocumentoIdentidad() { return documentoIdentidad; }
        public void setDocumentoIdentidad(DocumentoIdentidadDTO documentoIdentidad) { this.documentoIdentidad = documentoIdentidad; }
    }

    public static class DocumentoIdentidadDTO {
        private String tipo;
        private String numero;

        public String getTipo() { return tipo; }
        public void setTipo(String tipo) { this.tipo = tipo; }
        public String getNumero() { return numero; }
        public void setNumero(String numero) { this.numero = numero; }
    }

    public static class ReceptorDTO {
        private String institucion;
        private String cuenta;
        private String nombre;

        public String getInstitucion() { return institucion; }
        public void setInstitucion(String institucion) { this.institucion = institucion; }
        public String getCuenta() { return cuenta; }
        public void setCuenta(String cuenta) { this.cuenta = cuenta; }
        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
    }

    public static class ImporteDTO {
        private BigDecimal valor;
        private String divisa;

        public ImporteDTO() {}
        public ImporteDTO(BigDecimal valor, String divisa) {
            this.valor = valor;
            this.divisa = divisa;
        }
        public BigDecimal getValor() { return valor; }
        public void setValor(BigDecimal valor) { this.valor = valor; }
        public String getDivisa() { return divisa; }
        public void setDivisa(String divisa) { this.divisa = divisa; }
    }

    public String getTipoOperacion() { return tipoOperacion; }
    public void setTipoOperacion(String tipoOperacion) { this.tipoOperacion = tipoOperacion; }
    public String getReferenciaSeguimiento() { return referenciaSeguimiento; }
    public void setReferenciaSeguimiento(String referenciaSeguimiento) { this.referenciaSeguimiento = referenciaSeguimiento; }
    public ImporteDTO getImporte() { return importe; }
    public void setImporte(ImporteDTO importe) { this.importe = importe; }
    public EmisorDTO getEmisor() { return emisor; }
    public void setEmisor(EmisorDTO emisor) { this.emisor = emisor; }
    public ReceptorDTO getReceptor() { return receptor; }
    public void setReceptor(ReceptorDTO receptor) { this.receptor = receptor; }
    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }
    public Long getFolioNumerico() { return folioNumerico; }
    public void setFolioNumerico(Long folioNumerico) { this.folioNumerico = folioNumerico; }
}