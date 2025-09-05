package com.aerolineas.controller;

import com.aerolineas.dao.CiudadDAO;
import com.aerolineas.dto.CiudadDTOs;
import com.aerolineas.middleware.Auth;
import io.javalin.Javalin;

import java.util.Map;

public class CiudadController {
    private final CiudadDAO dao = new CiudadDAO();

    public void routes(Javalin app) {
        app.get("/api/public/ciudades", ctx -> {
            Long idPais = ctx.queryParamAsClass("idPais", Long.class)
                             .allowNullable()
                             .get();
            ctx.json(dao.listAll(idPais));
        });

        // Admin: crear ciudad y toggle activo
        app.before("/api/v1/ciudades", Auth.adminOrEmpleado());
        app.before("/api/v1/ciudades/*", Auth.adminOrEmpleado());

        app.post("/api/v1/ciudades", ctx -> {
            var dto = ctx.bodyAsClass(CiudadDTOs.Create.class);
            long id = dao.create(dto);
            ctx.status(201).json(Map.of("idCiudad", id));
        });

        app.put("/api/v1/ciudades/{id}/toggle", ctx -> {
            long idCiudad = Long.parseLong(ctx.pathParam("id"));
            dao.toggleActiva(idCiudad);
            ctx.status(204);
        });
    }
}
