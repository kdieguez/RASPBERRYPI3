package com.aerolineas.controller;

import com.aerolineas.dao.RatingDAO;
import com.aerolineas.http.JsonErrorHandler;
import com.aerolineas.middleware.Auth;
import com.aerolineas.util.JwtUtil;
import io.javalin.Javalin;

import java.util.Map;

public class RatingController {
  private final RatingDAO dao = new RatingDAO();

  static record CreateReq(Integer calificacion) {}

  private Long tryUserId(io.javalin.http.Context ctx) {
    try {
      String auth = ctx.header("Authorization");
      if (auth == null || !auth.startsWith("Bearer ")) return null;
      var c = JwtUtil.parse(auth.substring(7));
      Object sub = c.get("sub");
      if (sub == null) return null;
      return Long.parseLong(String.valueOf(sub));
    } catch (Exception ignored) {
      return null;
    }
  }

  public void routes(Javalin app) {


    app.get("/api/public/vuelos/{id}/ratings/resumen", ctx -> {
      long idV = ctx.pathParamAsClass("id", Long.class).get();
      Long idU = tryUserId(ctx);             
      var r = dao.resumen(idV, idU);
      ctx.json(Map.of(
          "promedio", r.promedio(),
          "total", r.total(),
          "miRating", r.miRating()
      ));
    });


    app.post("/api/v1/vuelos/{id}/ratings", ctx -> {
      Auth.jwt().handle(ctx);                
      long idV = ctx.pathParamAsClass("id", Long.class).get();
      var cl = Auth.claims(ctx);
      long idU = ((Number)cl.get("sub")).longValue();

      var body = ctx.bodyAsClass(CreateReq.class);
      if (body == null || body.calificacion() == null) {
        ctx.status(400).json(Map.of("error","calificacion requerida"));
        return;
      }

      dao.upsertRating(idV, idU, body.calificacion());
      ctx.status(201);
    });

    app.exception(Exception.class, JsonErrorHandler.of(500));
  }
}
