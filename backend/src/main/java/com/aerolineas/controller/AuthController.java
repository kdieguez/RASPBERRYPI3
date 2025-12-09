package com.aerolineas.controller;

import com.aerolineas.dao.UsuarioDAO;
import com.aerolineas.dto.AuthDTOs.LoginRequest;
import com.aerolineas.dto.AuthDTOs.LoginResponse;
import com.aerolineas.dto.AuthDTOs.RegisterRequest;
import com.aerolineas.dto.AuthDTOs.UserView;
import com.aerolineas.model.Usuario;
import com.aerolineas.util.JwtUtil;
import com.aerolineas.util.PasswordUtil;
import io.javalin.http.Context;

import java.util.Map;

public class AuthController {

  private final UsuarioDAO usuarios;

  public AuthController() {
    this.usuarios = new UsuarioDAO();
  }
  
  public AuthController(UsuarioDAO usuarios) {
    this.usuarios = usuarios;
  }

  private static String normEmail(String e) {
    return e == null ? null : e.trim().toLowerCase();
  }

  private long expSeconds() {
    return Long.parseLong(System.getenv().getOrDefault("JWT_EXP_MIN", "120")) * 60L;
  }

  private String tokenFor(Usuario u) {
    return JwtUtil.generate(Map.of(
        "sub", String.valueOf(u.getIdUsuario()),
        "email", u.getEmail(),
        "rol", u.getIdRol(),
        "name", u.getNombres() + " " + u.getApellidos()
    ));
  }

  public void register(Context ctx) throws Exception {
    
    RegisterRequest body = ctx.bodyAsClass(RegisterRequest.class);

    if (body.email() == null || !body.email().contains("@")) {
      ctx.status(400).json(Map.of("error", "email inválido"));
      return;
    }
    if (body.password() == null || body.password().length() < 8) {
      ctx.status(400).json(Map.of("error", "password mínimo 8"));
      return;
    }
    if (body.nombres() == null || body.nombres().isBlank()) {
      ctx.status(400).json(Map.of("error", "nombres requeridos"));
      return;
    }
    if (body.apellidos() == null || body.apellidos().isBlank()) {
      ctx.status(400).json(Map.of("error", "apellidos requeridos"));
      return;
    }
    

    final String email = normEmail(body.email());

    if (usuarios.findByEmail(email) != null) {
      ctx.status(409).json(Map.of("error", "email ya registrado"));
      return;
    }

    String hash = PasswordUtil.hash(body.password());
    Usuario u = usuarios.create(email, hash, body.nombres().trim(), body.apellidos().trim());

    UserView view = new UserView(
        u.getIdUsuario(), u.getEmail(), u.getNombres(), u.getApellidos(), u.getIdRol()
    );

    String token = tokenFor(u);
    ctx.status(201).json(new LoginResponse(token, expSeconds(), view));
  }

  public void login(Context ctx) throws Exception {
    LoginRequest body = ctx.bodyAsClass(LoginRequest.class);

    if (body.email() == null || !body.email().contains("@")) {
      ctx.status(400).json(Map.of("error", "email inválido"));
      return;
    }
    if (body.password() == null || body.password().isBlank()) {
      ctx.status(400).json(Map.of("error", "password requerido"));
      return;
    }

    final String email = normEmail(body.email());
    Usuario u = usuarios.findByEmail(email);

    if (u == null || !u.isHabilitado() ||
        !PasswordUtil.verify(body.password(), u.getContrasenaHash())) {
      ctx.status(401).json(Map.of("error", "credenciales no válidas"));
      return;
    }

    UserView view = new UserView(
        u.getIdUsuario(), u.getEmail(), u.getNombres(), u.getApellidos(), u.getIdRol()
    );

    String token = tokenFor(u);
    ctx.json(new LoginResponse(token, expSeconds(), view));
  }

  @SuppressWarnings("unchecked")
  public void me(Context ctx) {
    Map<String, Object> claims = ctx.attribute("claims");
    if (claims == null) {
      ctx.status(401).json(Map.of("error", "no autenticado"));
      return;
    }
    ctx.json(claims);
  }
}
