package com.aerolineas.controller;

import com.aerolineas.dao.NoticiaDAO;
import com.aerolineas.dao.TipDAO;
import com.aerolineas.dto.NoticiaDTO;
import com.aerolineas.dto.TipDTO;
import com.aerolineas.http.JsonErrorHandler;
import com.aerolineas.middleware.Auth;
import io.javalin.Javalin;

import java.util.List;
import java.util.Map;

public class ContenidoHomeController {

    private final TipDAO tipDAO;
    private final NoticiaDAO noticiaDAO;

    public ContenidoHomeController() {
        this(new TipDAO(), new NoticiaDAO());
    }

    public ContenidoHomeController(TipDAO tipDAO, NoticiaDAO noticiaDAO) {
        this.tipDAO = tipDAO;
        this.noticiaDAO = noticiaDAO;
    }

    public void routes(Javalin app) {

        app.get("/api/public/tips", ctx -> {
            List<TipDTO> tips = tipDAO.listar();
            ctx.json(tips);
        });

        app.get("/api/public/noticias", ctx -> {
            List<NoticiaDTO> noticias = noticiaDAO.listar();
            ctx.json(noticias);
        });

        app.get("/api/v1/admin/tips", ctx -> {
            Auth.adminOrEmpleado().handle(ctx);
            ctx.json(tipDAO.listar());
        });

        app.post("/api/v1/admin/tips", ctx -> {
            Auth.adminOrEmpleado().handle(ctx);
            TipDTO.Upsert dto = ctx.bodyAsClass(TipDTO.Upsert.class);
            long id = tipDAO.crear(dto);
            ctx.status(201).json(Map.of("idTip", id));
        });

        app.put("/api/v1/admin/tips/{id}", ctx -> {
            Auth.adminOrEmpleado().handle(ctx);
            long id = ctx.pathParamAsClass("id", Long.class).get();
            TipDTO.Upsert dto = ctx.bodyAsClass(TipDTO.Upsert.class);
            tipDAO.actualizar(id, dto);
            ctx.status(204);
        });

        app.delete("/api/v1/admin/tips/{id}", ctx -> {
            Auth.adminOrEmpleado().handle(ctx);
            long id = ctx.pathParamAsClass("id", Long.class).get();
            tipDAO.eliminar(id);
            ctx.status(204);
        });

        app.get("/api/v1/admin/noticias", ctx -> {
            Auth.adminOrEmpleado().handle(ctx);
            ctx.json(noticiaDAO.listar());
        });

        app.post("/api/v1/admin/noticias", ctx -> {
            Auth.adminOrEmpleado().handle(ctx);
            NoticiaDTO.Upsert dto = ctx.bodyAsClass(NoticiaDTO.Upsert.class);
            long id = noticiaDAO.crear(dto);
            ctx.status(201).json(Map.of("idNoticia", id));
        });

        app.put("/api/v1/admin/noticias/{id}", ctx -> {
            Auth.adminOrEmpleado().handle(ctx);
            long id = ctx.pathParamAsClass("id", Long.class).get();
            NoticiaDTO.Upsert dto = ctx.bodyAsClass(NoticiaDTO.Upsert.class);
            noticiaDAO.actualizar(id, dto);
            ctx.status(204);
        });

        app.delete("/api/v1/admin/noticias/{id}", ctx -> {
            Auth.adminOrEmpleado().handle(ctx);
            long id = ctx.pathParamAsClass("id", Long.class).get();
            noticiaDAO.eliminar(id);
            ctx.status(204);
        });

        app.exception(Exception.class, JsonErrorHandler.of(500));
    }
}
