package com.aerolineas.controller;

import com.aerolineas.dao.*;
import com.aerolineas.dto.*;
import com.aerolineas.http.JsonErrorHandler;
import com.aerolineas.middleware.Auth;
import io.javalin.Javalin;

import java.util.*;

public class PaginasController {

  private final PaginaDAO paginaDAO = new PaginaDAO();
  private final SeccionDAO seccionDAO = new SeccionDAO();
  private final MediaDAO   mediaDAO   = new MediaDAO();

  public void routes(Javalin app) {

  
    app.get("/api/public/paginas", ctx -> ctx.json(paginaDAO.listar()));

    app.get("/api/public/paginas/{id}", ctx -> {
      long id = ctx.pathParamAsClass("id", Long.class).get();
      var p = paginaDAO.obtenerConContenido(id);
      if (p == null) { ctx.status(404).json(Map.of("error","Página no encontrada")); return; }
      ctx.json(p);
    });

    app.get("/api/public/paginas/by-name/{nombre}", ctx -> {
      String nombre = ctx.pathParam("nombre");
      var p = paginaDAO.obtenerPorNombreConContenido(nombre);
      if (p == null) { ctx.status(404).json(Map.of("error","Página no encontrada")); return; }
      ctx.json(p);
    });

    app.post("/api/v1/admin/paginas", ctx -> {
      Auth.adminOrEmpleado().handle(ctx);
      PaginaDTO.Upsert dto = ctx.bodyAsClass(PaginaDTO.Upsert.class);
      long id = paginaDAO.crear(dto);
      ctx.status(201).json(Map.of("idPagina", id));
    });

    app.put("/api/v1/admin/paginas/{id}", ctx -> {
      Auth.adminOrEmpleado().handle(ctx);
      long id = ctx.pathParamAsClass("id", Long.class).get();
      PaginaDTO.Upsert dto = ctx.bodyAsClass(PaginaDTO.Upsert.class);
      paginaDAO.actualizar(id, dto);
      ctx.status(204);
    });

    app.delete("/api/v1/admin/paginas/{id}", ctx -> {
      Auth.adminOrEmpleado().handle(ctx);
      long id = ctx.pathParamAsClass("id", Long.class).get();
      paginaDAO.eliminar(id);
      ctx.status(204);
    });

    app.post("/api/v1/admin/paginas/{idPagina}/secciones", ctx -> {
      Auth.adminOrEmpleado().handle(ctx);
      long idPag = ctx.pathParamAsClass("idPagina", Long.class).get();
      SeccionDTO.Upsert dto = ctx.bodyAsClass(SeccionDTO.Upsert.class);
      long id = seccionDAO.crear(idPag, dto);
      ctx.status(201).json(Map.of("idSeccion", id));
    });

    app.put("/api/v1/admin/secciones/{idSeccion}", ctx -> {
      Auth.adminOrEmpleado().handle(ctx);
      long idSec = ctx.pathParamAsClass("idSeccion", Long.class).get();
      SeccionDTO.Upsert dto = ctx.bodyAsClass(SeccionDTO.Upsert.class);
      seccionDAO.actualizar(idSec, dto);
      ctx.status(204);
    });

    app.delete("/api/v1/admin/secciones/{idSeccion}", ctx -> {
      Auth.adminOrEmpleado().handle(ctx);
      long idSec = ctx.pathParamAsClass("idSeccion", Long.class).get();
      seccionDAO.eliminar(idSec);
      ctx.status(204);
    });

    app.put("/api/v1/admin/paginas/{idPagina}/secciones/reordenar", ctx -> {
      Auth.adminOrEmpleado().handle(ctx);
      long idPag = ctx.pathParamAsClass("idPagina", Long.class).get();
      SeccionDTO.Reordenar[] arr = ctx.bodyAsClass(SeccionDTO.Reordenar[].class);
      seccionDAO.reordenar(idPag, Arrays.asList(arr));
      ctx.status(204);
    });

    app.post("/api/v1/admin/secciones/{idSeccion}/media", ctx -> {
      Auth.adminOrEmpleado().handle(ctx);
      long idSeccion = ctx.pathParamAsClass("idSeccion", Long.class).get();
      MediaDTO.Upsert dto = ctx.bodyAsClass(MediaDTO.Upsert.class);
      long id = mediaDAO.crear(idSeccion, dto);
      ctx.status(201).json(Map.of("idMedia", id));
    });

    app.delete("/api/v1/admin/media/{idMedia}", ctx -> {
      Auth.adminOrEmpleado().handle(ctx);
      long idMedia = ctx.pathParamAsClass("idMedia", Long.class).get();
      mediaDAO.eliminar(idMedia);
      ctx.status(204);
    });

    app.put("/api/v1/admin/secciones/{idSeccion}/media/reordenar", ctx -> {
      Auth.adminOrEmpleado().handle(ctx);
      long idSeccion = ctx.pathParamAsClass("idSeccion", Long.class).get();
      MediaDTO.Reordenar[] arr = ctx.bodyAsClass(MediaDTO.Reordenar[].class);
      mediaDAO.reordenar(idSeccion, Arrays.asList(arr));
      ctx.status(204);
    });

    app.exception(Exception.class, JsonErrorHandler.of(500));
  }
}
