package com.aerolineas.dto;

public class AuthDTOs {
  public record RegisterRequest(String email, String password, String nombres, String apellidos, String captchaToken) {}
  public record LoginRequest(String email, String password) {}
  public record LoginResponse(String token, long expiresInSeconds, UserView user) {}
  public record UserView(Long id, String email, String nombres, String apellidos, Integer idRol) {}
}
