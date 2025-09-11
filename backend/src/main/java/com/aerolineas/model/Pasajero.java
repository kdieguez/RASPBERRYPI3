package com.aerolineas.model;

import java.time.LocalDate;

public class Pasajero {
  private Long idPasajero;
  private LocalDate fechaNacimiento;   
  private Long idPaisDocumento;       
  private String pasaporte;            
  private Long idUsuario;              

  public Pasajero() {}

  public Pasajero(Long idPasajero, LocalDate fechaNacimiento, Long idPaisDocumento,
                  String pasaporte, Long idUsuario) {
    this.idPasajero = idPasajero;
    this.fechaNacimiento = fechaNacimiento;
    this.idPaisDocumento = idPaisDocumento;
    this.pasaporte = pasaporte;
    this.idUsuario = idUsuario;
  }

  public Long getIdPasajero() { return idPasajero; }
  public void setIdPasajero(Long idPasajero) { this.idPasajero = idPasajero; }

  public LocalDate getFechaNacimiento() { return fechaNacimiento; }
  public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

  public Long getIdPaisDocumento() { return idPaisDocumento; }
  public void setIdPaisDocumento(Long idPaisDocumento) { this.idPaisDocumento = idPaisDocumento; }

  public String getPasaporte() { return pasaporte; }
  public void setPasaporte(String pasaporte) { this.pasaporte = pasaporte; }

  public Long getIdUsuario() { return idUsuario; }
  public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }
}
