package com.aerolineas.controller;

import com.aerolineas.dao.VueloDAO;
import com.aerolineas.dto.VueloDTO;
import com.aerolineas.http.JsonErrorHandler;
import com.aerolineas.middleware.Auth;
import io.javalin.Javalin;

import java.sql.SQLException;
import java.util.Map;

public class VueloController {
  private final VueloDAO dao = new VueloDAO();

  static record RoundtripReq(VueloDTO.Create ida, VueloDTO.Create regreso) {}
  static record LinkReq(Long idIda, Long idRegreso) {}

  public void routes(Javalin app) {

    app.get("/api/public/vuelos", ctx -> ctx.json(dao.listarVuelos(true)));

    app.get("/api/v1/vuelos", ctx -> ctx.json(dao.listarVuelos(true)));

    app.get("/api/v1/admin/vuelos", ctx -> {
      Auth.adminOrEmpleado().handle(ctx);
      ctx.json(dao.listarVuelos(false));
    });

    app.get("/api/v1/admin/vuelos/{id}", ctx -> {
      Auth.adminOrEmpleado().handle(ctx);
      long id = ctx.pathParamAsClass("id", Long.class).get();
      var v = dao.obtenerVuelo(id);
      if (v == null) { ctx.status(404).json(Map.of("error","Vuelo no encontrado")); return; }
      ctx.json(v);
    });

    app.put("/api/v1/admin/vuelos/{id}", ctx -> {
      Auth.adminOrEmpleado().handle(ctx);
      long id = ctx.pathParamAsClass("id", Long.class).get();
      var dto = ctx.bodyAsClass(VueloDTO.UpdateAdmin.class); 

      if (dto.codigo()==null || dto.codigo().isBlank()) {
        ctx.status(400).json(Map.of("error","Código requerido")); return;
      }
      if (dto.fechaSalida().isAfter(dto.fechaLlegada())) {
        ctx.status(400).json(Map.of("error","La salida debe ser menor que la llegada")); return;
      }
      if (dto.clases()==null || dto.clases().isEmpty()) {
        ctx.status(400).json(Map.of("error","Debe indicar al menos una clase")); return;
      }

      try {
        dao.actualizarVueloAdmin(id, dto);
        ctx.status(204);
      } catch (SQLException e) {
        String msg = e.getMessage()==null? "" : e.getMessage();
        ctx.status(400).json(Map.of("error", msg.isBlank() ? "No se pudo actualizar" : msg));
      } catch (Exception e) {
        ctx.status(500).json(Map.of("error","Error al actualizar"));
      }
    });

    app.post("/api/v1/vuelos", ctx -> {
      Auth.adminOrEmpleado().handle(ctx);
      VueloDTO.Create dto = ctx.bodyAsClass(VueloDTO.Create.class);
      dao.crearVuelo(dto);
      ctx.status(201).result("Vuelo creado");
    });

    app.post("/api/v1/vuelos/roundtrip", ctx -> {
      Auth.adminOrEmpleado().handle(ctx);
      RoundtripReq req = ctx.bodyAsClass(RoundtripReq.class);
      if (req == null || req.ida() == null || req.regreso() == null) {
        ctx.status(400).json(Map.of("error", "Se requieren objetos 'ida' y 'regreso'"));
        return;
      }
      try {
        long idIda     = dao.crearVueloReturnId(req.ida());
        long idRegreso = dao.crearVueloReturnId(req.regreso());
        dao.vincularPareja(idIda, idRegreso);
        ctx.status(201).json(Map.of("idIda", idIda, "idRegreso", idRegreso));
      } catch (SQLException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        ctx.status(400).json(Map.of("error", msg.isBlank() ? "No se pudo crear roundtrip" : msg));
      } catch (Exception e) {
        ctx.status(500).json(Map.of("error", "Error al crear roundtrip"));
      }
    });

    app.post("/api/v1/vuelos/link", ctx -> {
      Auth.adminOrEmpleado().handle(ctx);
      LinkReq req = ctx.bodyAsClass(LinkReq.class);
      if (req == null || req.idIda() == null || req.idRegreso() == null) {
        ctx.status(400).json(Map.of("error", "idIda e idRegreso son requeridos"));
        return;
      }
      try {
        dao.vincularPareja(req.idIda(), req.idRegreso());
        ctx.status(204);
      } catch (SQLException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        ctx.status(400).json(Map.of("error", msg.isBlank() ? "No se pudo vincular vuelos" : msg));
      }
    });

    app.put("/api/v1/vuelos/{id}/estado", ctx -> {
      Auth.adminOrEmpleado().handle(ctx);
      long idVuelo = ctx.pathParamAsClass("id", Long.class).get();
      var dto = ctx.bodyAsClass(VueloDTO.EstadoUpdate.class);
      if (dto.idEstado() == null) {
        ctx.status(400).json(Map.of("error", "idEstado es requerido"));
        return;
      }
      try {
        dao.actualizarEstado(idVuelo, dto.idEstado());
        ctx.status(204);
      } catch (SQLException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (msg.contains("no existe")) {
          ctx.status(404).json(Map.of("error", msg));
        } else if (msg.contains("cancelado") || msg.contains("inválido")) {
          ctx.status(409).json(Map.of("error", msg));
        } else {
          ctx.status(400).json(Map.of("error", msg));
        }
      }
    });

    app.exception(Exception.class, JsonErrorHandler.of(500));
  }
}
