package com.aerolineas.controller;

import com.aerolineas.dao.UsuarioDAO;
import com.aerolineas.dto.UsuarioAdminDTOs;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;

public class AdminUsuarioController {
  private final UsuarioDAO usuarios = new UsuarioDAO();

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
