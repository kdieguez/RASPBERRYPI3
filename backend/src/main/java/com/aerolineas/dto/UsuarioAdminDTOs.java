package com.aerolineas.dto;

public class UsuarioAdminDTOs {

  public record Row(
      long idUsuario,
      String email,
      String nombres,
      String apellidos,
      Integer idRol,
      String rolNombre,
      Integer habilitado
  ) {}

  public record View(
      long idUsuario,
      String email,
      String nombres,
      String apellidos,
      Integer idRol,
      String rolNombre,
      Integer habilitado,
      String fechaNacimiento,
      Long idPais,
      String pasaporte
  ) {}

  public record UpdateAdmin(
      String nombres,
      String apellidos,
      String newPassword,
      Integer idRol,
      Integer habilitado,
      String fechaNacimiento,
      Long idPais,
      String pasaporte
  ) {}

  public record UpdateSelf(
      String nombres,
      String apellidos,
      String newPassword,
      String fechaNacimiento,
      Long idPais,
      String pasaporte
  ) {}
}
