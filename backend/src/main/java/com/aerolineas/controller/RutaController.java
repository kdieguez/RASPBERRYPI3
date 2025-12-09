package com.aerolineas.controller;

import com.aerolineas.dao.RutaDAO;
import com.aerolineas.dto.RutaDTOs;
import com.aerolineas.middleware.Auth;
import io.javalin.Javalin;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class RutaController {

    private final RutaDAO dao;

    public RutaController() {
        this(new RutaDAO());
    }

    public RutaController(@NotNull RutaDAO dao) {
        this.dao = dao;
    }

    public void routes(Javalin app) {
        Handler auth = Auth.adminOrEmpleado();

        app.before("/api/v1/rutas",   auth);
        app.before("/api/v1/rutas/*", auth);

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
