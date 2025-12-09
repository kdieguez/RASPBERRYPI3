package com.aerolineas.controller;

import com.aerolineas.dao.ComentarioDAO;
import com.aerolineas.dto.ComentarioDTO;
import com.aerolineas.http.JsonErrorHandler;
import com.aerolineas.middleware.Auth;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;

import java.util.Map;

public class ComentarioController {

  private final ComentarioDAO dao;
  private final Handler jwtHandler;

  public ComentarioController() {
    this(new ComentarioDAO(), Auth.jwt());
  }

  public ComentarioController(ComentarioDAO dao, Handler jwtHandler) {
    this.dao = dao;
    this.jwtHandler = jwtHandler;
  }

  @SuppressWarnings("unchecked")
  Long userIdFromClaims(Context ctx) {
    var claims = (Map<String, Object>) ctx.attribute("claims");
    if (claims == null) return null;
    Object v = claims.getOrDefault("idUsuario", claims.get("id"));
    if (v == null) return null;
    try {
      return Long.parseLong(String.valueOf(v));
    } catch (Exception e) {
      return null;
    }
  }

  public void routes(Javalin app) {

    app.get("/api/public/vuelos/{id}/comentarios", ctx -> {
      long idVuelo = ctx.pathParamAsClass("id", Long.class).get();
      ctx.json(dao.listarPublic(idVuelo));
    });

    app.post("/api/v1/vuelos/{id}/comentarios", ctx -> {
  
      jwtHandler.handle(ctx);
      if (ctx.attribute("claims") == null) return;

      Long userId = userIdFromClaims(ctx);
      if (userId == null) {
        ctx.status(401).json(Map.of("error", "No autenticado"));
        return;
      }

      long idVuelo = ctx.pathParamAsClass("id", Long.class).get();
      ComentarioDTO.Create body = ctx.bodyAsClass(ComentarioDTO.Create.class);
      if (body == null || body.comentario() == null || body.comentario().isBlank()) {
        ctx.status(400).json(Map.of("error", "comentario es requerido"));
        return;
      }

      try {
        long id = dao.crear(idVuelo, userId, body.comentario(), body.idPadre());
        ctx.status(201).json(Map.of("idComentario", id));
      } catch (Exception e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        ctx.status(400).json(Map.of("error", msg.isBlank() ? "No se pudo crear" : msg));
      }
    });

    app.exception(Exception.class, JsonErrorHandler.of(500));
  }
}
