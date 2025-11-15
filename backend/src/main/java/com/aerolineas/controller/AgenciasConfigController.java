package com.aerolineas.controller;

import com.aerolineas.config.DB;
import com.aerolineas.dao.AgenciasConfigDAO;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.sql.Connection;
import java.util.Map;

public class AgenciasConfigController {
    
    public void list(Context ctx) {
        try (Connection conn = DB.getConnection()) {
            var dao = new AgenciasConfigDAO(conn);
            String soloHabilitadasParam = ctx.queryParam("soloHabilitadas");
            boolean soloHabilitadas = soloHabilitadasParam != null && Boolean.parseBoolean(soloHabilitadasParam);
            var lista = dao.listar(soloHabilitadas);
            ctx.json(lista);
        } catch (Exception ex) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", ex.getMessage()));
        }
    }
    
    public void get(Context ctx) {
        String idAgencia = ctx.pathParam("id");
        try (Connection conn = DB.getConnection()) {
            var dao = new AgenciasConfigDAO(conn);
            var agencia = dao.obtener(idAgencia);
            if (agencia == null) {
                ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", "Agencia no encontrada"));
                return;
            }
            ctx.json(agencia);
        } catch (Exception ex) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", ex.getMessage()));
        }
    }
    
    public void create(Context ctx) {
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String idAgencia = (String) body.get("idAgencia");
            String nombre = (String) body.get("nombre");
            String apiUrl = (String) body.get("apiUrl");
            Long idUsuarioWs = body.get("idUsuarioWs") != null 
                ? ((Number) body.get("idUsuarioWs")).longValue() 
                : null;
            
            if (idAgencia == null || idAgencia.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "idAgencia es requerido"));
                return;
            }
            if (nombre == null || nombre.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "nombre es requerido"));
                return;
            }
            if (apiUrl == null || apiUrl.isBlank()) {
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "apiUrl es requerido"));
                return;
            }
            
            try (Connection conn = DB.getConnection()) {
                var dao = new AgenciasConfigDAO(conn);
                dao.crear(idAgencia.trim(), nombre.trim(), apiUrl.trim(), idUsuarioWs);
                ctx.status(HttpStatus.CREATED).json(Map.of("ok", true, "idAgencia", idAgencia));
            }
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "Error al crear agencia";
            if (msg.contains("unique constraint") || msg.contains("duplicate")) {
                ctx.status(HttpStatus.CONFLICT).json(Map.of("error", "Ya existe una agencia con ese ID"));
            } else {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", msg));
            }
        }
    }
    
    public void update(Context ctx) {
        String idAgencia = ctx.pathParam("id");
        try {
            Map<String, Object> body = ctx.bodyAsClass(Map.class);
            String nombre = (String) body.get("nombre");
            String apiUrl = (String) body.get("apiUrl");
            Long idUsuarioWs = body.get("idUsuarioWs") != null 
                ? ((Number) body.get("idUsuarioWs")).longValue() 
                : null;
            Boolean habilitado = body.get("habilitado") != null 
                ? (Boolean) body.get("habilitado") 
                : null;
            
            try (Connection conn = DB.getConnection()) {
                var dao = new AgenciasConfigDAO(conn);
                dao.actualizar(idAgencia, nombre, apiUrl, idUsuarioWs, habilitado);
                ctx.status(HttpStatus.NO_CONTENT);
            }
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "Error al actualizar agencia";
            if (msg.contains("no encontrada")) {
                ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", msg));
            } else {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", msg));
            }
        }
    }
    
    public void delete(Context ctx) {
        String idAgencia = ctx.pathParam("id");
        try (Connection conn = DB.getConnection()) {
            var dao = new AgenciasConfigDAO(conn);
            dao.eliminar(idAgencia);
            ctx.status(HttpStatus.NO_CONTENT);
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "Error al eliminar agencia";
            if (msg.contains("no encontrada")) {
                ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", msg));
            } else {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", msg));
            }
        }
    }
}


