package com.aerolineas.controller;

import com.aerolineas.dao.PaisDAO;
import com.aerolineas.dto.PaisDTOs;
import com.aerolineas.middleware.Auth;
import io.javalin.Javalin;

import java.util.Map;

public class PaisController {
    private final PaisDAO dao = new PaisDAO();

    public void routes(Javalin app) {
        app.get("/api/public/paises", ctx -> ctx.json(dao.listAll()));

        app.before("/api/v1/paises", Auth.adminOrEmpleado());
        app.post("/api/v1/paises", ctx -> {
            var dto = ctx.bodyAsClass(PaisDTOs.Create.class);
            long id = dao.create(dto);
            ctx.status(201).json(Map.of("idPais", id));
        });
    }
}
