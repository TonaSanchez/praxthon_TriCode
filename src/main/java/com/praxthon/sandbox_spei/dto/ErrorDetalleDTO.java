package com.praxthon.sandbox_spei.dto;

public class ErrorDetalleDTO {
    private String codigo;
    private String campo;
    private String mensaje;

    public ErrorDetalleDTO(String codigo, String campo, String mensaje) {
        this.codigo = codigo;
        this.campo = campo;
        this.mensaje = mensaje;
    }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getCampo() { return campo; }
    public void setCampo(String campo) { this.campo = campo; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}