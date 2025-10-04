package com.aerolineas.controller;

import com.aerolineas.config.DB;             
import com.aerolineas.dao.ConfigDAO;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.sql.Connection;
import java.util.Map;

public class ConfigController {

    public void getAll(Context ctx) {
        try (Connection conn = DB.getConnection()) {
            var dao = new ConfigDAO(conn);
            ctx.json(dao.getAll());
        } catch (Exception ex) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", ex.getMessage()));
        }
    }

    public void getBySection(Context ctx) {
        String section = ctx.pathParam("section").toLowerCase();
        if (!section.equals("header") && !section.equals("footer")) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error","section inválida"));
            return;
        }
        try (Connection conn = DB.getConnection()) {
            var dao = new ConfigDAO(conn);
            ctx.json(dao.getSection(section));
        } catch (Exception ex) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", ex.getMessage()));
        }
    }

    public void upsertSection(Context ctx) {
        String section = ctx.pathParam("section").toLowerCase();
        if (!section.equals("header") && !section.equals("footer")) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error","section inválida"));
            return;
        }
        try (Connection conn = DB.getConnection()) {
            Map<String,String> body = ctx.bodyAsClass(Map.class);
            var dao = new ConfigDAO(conn);
            dao.upsertSection(section, body);
            ctx.status(HttpStatus.NO_CONTENT);
        } catch (Exception ex) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", ex.getMessage()));
        }
    }
}
