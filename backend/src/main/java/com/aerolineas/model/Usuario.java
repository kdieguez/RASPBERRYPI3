package com.aerolineas.model;

public class Usuario {
  private Long idUsuario;
  private String email;
  private String contrasenaHash; 
  private String nombres;
  private String apellidos;
  private boolean habilitado;
  private Integer idRol;

  public Long getIdUsuario() { return idUsuario; }
  public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getContrasenaHash() { return contrasenaHash; }
  public void setContrasenaHash(String contrasenaHash) { this.contrasenaHash = contrasenaHash; }
  public String getNombres() { return nombres; }
  public void setNombres(String nombres) { this.nombres = nombres; }
  public String getApellidos() { return apellidos; }
  public void setApellidos(String apellidos) { this.apellidos = apellidos; }
  public boolean isHabilitado() { return habilitado; }
  public void setHabilitado(boolean habilitado) { this.habilitado = habilitado; }
  public Integer getIdRol() { return idRol; }
  public void setIdRol(Integer idRol) { this.idRol = idRol; }
}
