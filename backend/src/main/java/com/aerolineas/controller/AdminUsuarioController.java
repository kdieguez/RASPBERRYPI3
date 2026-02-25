package com.aerolineas.controller;


import com.aerolineas.dto.UsuarioAdminDTOs;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;

public class AdminUsuarioController {
  private final UsuarioDAO usuarios = new UsuarioDAO();

  public void createWs(Context ctx) throws Exception {
    var body = ctx.bodyValidator(Map.class)
        .check(b -> b.get("email") != null && String.valueOf(b.get("email")).contains("@"), "email inválido")
        .check(b -> b.get("password") != null && String.valueOf(b.get("password")).length() >= 8, "password mínimo 8")
        .check(b -> b.get("nombres") != null && !String.valueOf(b.get("nombres")).isBlank(), "nombres requeridos")
        .check(b -> b.get("apellidos") != null && !String.valueOf(b.get("apellidos")).isBlank(), "apellidos requeridos")
        .get();

    String email = String.valueOf(body.get("email")).trim().toLowerCase();
    String pass  = String.valueOf(body.get("password"));
    String nombres = String.valueOf(body.get("nombres")).trim();
    String apellidos = String.valueOf(body.get("apellidos")).trim();

    if (usuarios.findByEmail(email) != null) {
      ctx.status(409).json(Map.of("error", "email ya registrado"));
      return;
    }

    String hash = com.aerolineas.util.PasswordUtil.hash(pass);
    int idRol = 2;
    try {
      Object raw = body.get("idRol");
      if (raw != null) idRol = Integer.parseInt(String.valueOf(raw));
    } catch (Exception ignored) {}
    var u = usuarios.createWithRole(email, hash, nombres, apellidos, idRol);
    ctx.status(201).json(Map.of(
        "idUsuario", u.getIdUsuario(),
        "email", u.getEmail(),
        "nombres", u.getNombres(),
        "apellidos", u.getApellidos(),
        "idRol", u.getIdRol()
    ));
  }

  public void list(Context ctx) throws Exception {
    String q = ctx.queryParam("q");
    int offset = parseInt(ctx.queryParam("offset"), 0);
    int limit  = parseInt(ctx.queryParam("limit"), 25);
    List<UsuarioAdminDTOs.Row> items = usuarios.adminList(q, offset, limit);
    ctx.json(Map.of("items", items, "offset", offset, "limit", limit));
  }

  public void get(Context ctx) throws Exception {
    long id = Long.parseLong(ctx.pathParam("id"));
    var v = usuarios.adminGet(id);
    if (v == null) { ctx.status(404).json(Map.of("error","no encontrado")); return; }
    ctx.json(v);
  }

  public void update(Context ctx) throws Exception {
    long id = Long.parseLong(ctx.pathParam("id"));
    var body = ctx.bodyValidator(UsuarioAdminDTOs.UpdateAdmin.class)
        .check(b -> b.nombres()!=null && !b.nombres().isBlank(), "nombres requeridos")
        .check(b -> b.apellidos()!=null && !b.apellidos().isBlank(), "apellidos requeridos")
        .check(b -> b.idRol()!=null && b.idRol()>0, "rol inválido")
        .check(b -> b.habilitado()!=null && (b.habilitado()==0 || b.habilitado()==1), "habilitado inválido")
        .check(b -> b.pasaporte()==null || b.pasaporte().length()<=20, "pasaporte demasiado largo")
        .get();
    usuarios.adminUpdate(id, body);
    ctx.json(Map.of("ok", true));
  }

  private int parseInt(String s, int def) {
    try { return s==null?def:Integer.parseInt(s); } catch(Exception e){ return def; }
  }
}
