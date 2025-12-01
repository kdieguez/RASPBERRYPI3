package com.aerolineas.controller;

import com.aerolineas.dao.UsuarioDAO;
import com.aerolineas.dto.AuthDTOs.LoginRequest;
import com.aerolineas.dto.AuthDTOs.LoginResponse;
import com.aerolineas.dto.AuthDTOs.RegisterRequest;
import com.aerolineas.dto.AuthDTOs.UserView;
import com.aerolineas.model.Usuario;
// import com.aerolineas.util.CaptchaUtil;  // ya no lo usamos
import com.aerolineas.util.JwtUtil;
import com.aerolineas.util.PasswordUtil;
import io.javalin.http.Context;

import java.util.Map;

public class AuthController {

  private final UsuarioDAO usuarios = new UsuarioDAO();

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

  public void register(Context ctx) {
    RegisterRequest body = ctx.bodyValidator(RegisterRequest.class)
        .check(b -> b.email != null && b.email.contains("@"), "email inválido")
        .check(b -> b.password != null && b.password.length() >= 8, "password mínimo 8")
        .check(b -> b.nombres != null && !b.nombres.isBlank(), "nombres requeridos")
        .check(b -> b.apellidos != null && !b.apellidos.isBlank(), "apellidos requeridos")
        // .check(b -> CaptchaUtil.verify(b.captchaToken), "captcha inválido") // desactivado
        .get();

    final String email = normEmail(body.email);

    if (usuarios.findByEmail(email) != null) {
      ctx.status(409).json(Map.of("error", "email ya registrado"));
      return;
    }

    String hash = PasswordUtil.hash(body.password);
    Usuario u = usuarios.create(email, hash, body.nombres.trim(), body.apellidos.trim());

    UserView view = new UserView(
        u.getIdUsuario(), u.getEmail(), u.getNombres(), u.getApellidos(), u.getIdRol()
    );

    String token = tokenFor(u);
    ctx.status(201).json(new LoginResponse(token, expSeconds(), view));
  }

  public void login(Context ctx) {
    LoginRequest body = ctx.bodyValidator(LoginRequest.class)
        .check(b -> b.email != null && b.email.contains("@"), "email inválido")
        .check(b -> b.password != null && !b.password.isBlank(), "password requerido")
        .get();

    final String email = normEmail(body.email);
    Usuario u = usuarios.findByEmail(email);

    if (u == null || !u.isHabilitado() || !PasswordUtil.verify(body.password, u.getContrasenaHash())) {
      ctx.status(401).json(Map.of("error", "credenciales no válidas"));
      return;
    }

    UserView view = new UserView(
        u.getIdUsuario(), u.getEmail(), u.getNombres(), u.getApellidos(), u.getIdRol()
    );

    String token = tokenFor(u);
    ctx.json(new LoginResponse(token, expSeconds(), view));
  }

  public void me(Context ctx) {
    @SuppressWarnings("unchecked")
    Map<String, Object> claims = ctx.attribute("claims");
    if (claims == null) {
      ctx.status(401).json(Map.of("error", "no autenticado"));
      return;
    }
    ctx.json(claims);
  }
}
