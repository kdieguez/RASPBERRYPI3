package com.aerolineas.controller;

import com.aerolineas.config.DB;
import com.aerolineas.dao.ConfigDAO;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.sql.Connection;
import java.util.Map;

public class ConfigController {

    private final ConfigDAO injectedDao;

    public ConfigController() {
        this.injectedDao = null;
    }

    public ConfigController(ConfigDAO dao) {
        this.injectedDao = dao;
    }

    private ConfigDAO buildDao(Connection conn) {
        if (injectedDao != null) {
            return injectedDao;
        }
        return new ConfigDAO(conn);
    }

    public void getAll(Context ctx) {
        if (injectedDao != null) {
            try {
                ctx.json(injectedDao.getAll());
            } catch (Exception ex) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .json(Map.of("error", ex.getMessage()));
            }
            return;
        }

        try (Connection conn = DB.getConnection()) {
            ConfigDAO dao = buildDao(conn);
            ctx.json(dao.getAll());
        } catch (Exception ex) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("error", ex.getMessage()));
        }
    }

    public void getBySection(Context ctx) {
        String section = ctx.pathParam("section").toLowerCase();
        if (!section.equals("header") && !section.equals("footer")) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(Map.of("error", "section inválida"));
            return;
        }

        if (injectedDao != null) {
            try {
                ctx.json(injectedDao.getSection(section));
            } catch (Exception ex) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .json(Map.of("error", ex.getMessage()));
            }
            return;
        }

        try (Connection conn = DB.getConnection()) {
            ConfigDAO dao = buildDao(conn);
            ctx.json(dao.getSection(section));
        } catch (Exception ex) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("error", ex.getMessage()));
        }
    }

    public void upsertSection(Context ctx) {
        String section = ctx.pathParam("section").toLowerCase();
        if (!section.equals("header") && !section.equals("footer")) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(Map.of("error", "section inválida"));
            return;
        }

        Map<String, String> body = ctx.bodyAsClass(Map.class);

        if (injectedDao != null) {
            try {
                injectedDao.upsertSection(section, body);
                ctx.status(HttpStatus.NO_CONTENT);
            } catch (Exception ex) {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .json(Map.of("error", ex.getMessage()));
            }
            return;
        }

        try (Connection conn = DB.getConnection()) {
            ConfigDAO dao = buildDao(conn);
            dao.upsertSection(section, body);
            ctx.status(HttpStatus.NO_CONTENT);
        } catch (Exception ex) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(Map.of("error", ex.getMessage()));
        }
    }
}
