package com.aerolineas.controller;

import com.aerolineas.dao.VueloDAO;
import com.aerolineas.dto.VueloDTO;
import com.aerolineas.http.JsonErrorHandler;
import io.javalin.Javalin;

public class VueloController {
    private final VueloDAO dao = new VueloDAO();

    public void routes(Javalin app) {
        app.post("/api/v1/vuelos", ctx -> {
            VueloDTO.Create dto = ctx.bodyAsClass(VueloDTO.Create.class);
            dao.crearVuelo(dto);
            ctx.status(201).result("Vuelo creado");
        });

        app.get("/api/v1/vuelos", ctx -> {
            ctx.json(dao.listarVuelos());
        });

        app.exception(Exception.class, JsonErrorHandler.of(500));

        app.put("/api/v1/vuelos/{id}/estado", ctx -> {
            long idVuelo = ctx.pathParamAsClass("id", Long.class).get();
            var dto = ctx.bodyAsClass(com.aerolineas.dto.VueloDTO.EstadoUpdate.class);

            if (dto.idEstado() == null) {
                ctx.status(400).json(java.util.Map.of("error", "idEstado es requerido"));
                return;
            }

            try {
                new com.aerolineas.dao.VueloDAO().actualizarEstado(idVuelo, dto.idEstado());
                ctx.status(204);
            } catch (java.sql.SQLException e) {
                String msg = e.getMessage() == null ? "" : e.getMessage();
                if (msg.contains("no existe")) {
                    ctx.status(404).json(java.util.Map.of("error", msg));
                } else if (msg.contains("cancelado") || msg.contains("inválido")) {
                    ctx.status(409).json(java.util.Map.of("error", msg)); 
                } else {
                    ctx.status(400).json(java.util.Map.of("error", msg));
                }
            }
        });

    }
}
