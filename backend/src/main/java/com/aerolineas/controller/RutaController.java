package com.aerolineas.controller;

import com.aerolineas.dao.RutaDAO;
import com.aerolineas.dto.RutaDTOs;
import com.aerolineas.middleware.Auth;
import io.javalin.Javalin;

import java.util.Map;

public class RutaController {
    private final RutaDAO dao = new RutaDAO();

    public void routes(Javalin app) {
        app.before("/api/v1/rutas", Auth.adminOrEmpleado());
        app.before("/api/v1/rutas/*", Auth.adminOrEmpleado());

        app.get("/api/v1/rutas", ctx -> ctx.json(dao.listAll()));

        app.post("/api/v1/rutas", ctx -> {
            var dto = ctx.bodyAsClass(RutaDTOs.Create.class);
            long id = dao.create(dto);
            ctx.status(201).json(Map.of("idRuta", id));
        });

        app.put("/api/v1/rutas/{id}/toggle", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            dao.toggleActiva(id);
            ctx.status(204);
        });
    }
}
