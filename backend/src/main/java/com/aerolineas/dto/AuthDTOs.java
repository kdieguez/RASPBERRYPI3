package com.aerolineas.dto;

public class AuthDTOs {

  // DTO para /api/auth/register
  public record RegisterRequest(
      String email,
      String password,
      String nombres,
      String apellidos
      // String captchaToken  // ← ya no se usa
  ) {}

  // DTO para /api/auth/login
  public record LoginRequest(
      String email,
      String password
  ) {}

  // Respuesta estándar de login / registro
  public record LoginResponse(
      String token,
      long expiresInSeconds,
      UserView user
  ) {}

  // Vista de usuario que se manda al frontend
  public record UserView(
      Long id,
      String email,
      String nombres,
      String apellidos,
      Integer idRol
  ) {}
}
