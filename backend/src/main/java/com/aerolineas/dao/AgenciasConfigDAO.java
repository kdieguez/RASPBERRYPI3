package com.aerolineas.dao;

import com.aerolineas.config.DB;
import java.sql.*;
import java.util.*;

public class AgenciasConfigDAO {
    
    private final Connection conn;
    
    public AgenciasConfigDAO(Connection conn) {
        this.conn = conn;
    }
    
    /**
     * Lista todas las agencias configuradas, opcionalmente solo las habilitadas
     */
    public List<Map<String, Object>> listar(boolean soloHabilitadas) throws SQLException {
        String table = DB.table("AGENCIAS_CONFIG");
        String sql = soloHabilitadas
            ? "SELECT ID_AGENCIA, NOMBRE, API_URL, ID_USUARIO_WS, HABILITADO, CREADO_EN, ACTUALIZADO_EN FROM " + table + " WHERE HABILITADO = 1 ORDER BY NOMBRE"
            : "SELECT ID_AGENCIA, NOMBRE, API_URL, ID_USUARIO_WS, HABILITADO, CREADO_EN, ACTUALIZADO_EN FROM " + table + " ORDER BY NOMBRE";
        
        List<Map<String, Object>> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("idAgencia", rs.getString("ID_AGENCIA"));
                row.put("nombre", rs.getString("NOMBRE"));
                row.put("apiUrl", rs.getString("API_URL"));
                row.put("idUsuarioWs", rs.getObject("ID_USUARIO_WS") != null ? rs.getLong("ID_USUARIO_WS") : null);
                row.put("habilitado", rs.getInt("HABILITADO") == 1);
                if (rs.getTimestamp("CREADO_EN") != null) {
                    row.put("creadoEn", rs.getTimestamp("CREADO_EN").toInstant().toString());
                }
                if (rs.getTimestamp("ACTUALIZADO_EN") != null) {
                    row.put("actualizadoEn", rs.getTimestamp("ACTUALIZADO_EN").toInstant().toString());
                }
                out.add(row);
            }
        }
        return out;
    }
    
    /**
     * Obtiene una agencia por su ID
     */
    public Map<String, Object> obtener(String idAgencia) throws SQLException {
        String table = DB.table("AGENCIAS_CONFIG");
        String sql = "SELECT ID_AGENCIA, NOMBRE, API_URL, ID_USUARIO_WS, HABILITADO, CREADO_EN, ACTUALIZADO_EN FROM " + table + " WHERE ID_AGENCIA = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idAgencia);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("idAgencia", rs.getString("ID_AGENCIA"));
                    row.put("nombre", rs.getString("NOMBRE"));
                    row.put("apiUrl", rs.getString("API_URL"));
                    row.put("idUsuarioWs", rs.getObject("ID_USUARIO_WS") != null ? rs.getLong("ID_USUARIO_WS") : null);
                    row.put("habilitado", rs.getInt("HABILITADO") == 1);
                    if (rs.getTimestamp("CREADO_EN") != null) {
                        row.put("creadoEn", rs.getTimestamp("CREADO_EN").toInstant().toString());
                    }
                    if (rs.getTimestamp("ACTUALIZADO_EN") != null) {
                        row.put("actualizadoEn", rs.getTimestamp("ACTUALIZADO_EN").toInstant().toString());
                    }
                    return row;
                }
            }
        }
        return null;
    }
    
    /**
     * Crea una nueva agencia
     */
    public void crear(String idAgencia, String nombre, String apiUrl, Long idUsuarioWs) throws SQLException {
        String table = DB.table("AGENCIAS_CONFIG");
        String sql = "INSERT INTO " + table + " (ID_AGENCIA, NOMBRE, API_URL, ID_USUARIO_WS, HABILITADO, CREADO_EN) VALUES (?, ?, ?, ?, 1, CURRENT_TIMESTAMP)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idAgencia);
            ps.setString(2, nombre);
            ps.setString(3, apiUrl);
            if (idUsuarioWs != null) {
                ps.setLong(4, idUsuarioWs);
            } else {
                ps.setNull(4, Types.NUMERIC);
            }
            ps.executeUpdate();
        }
    }
    
    /**
     * Actualiza una agencia existente
     */
    public void actualizar(String idAgencia, String nombre, String apiUrl, Long idUsuarioWs, Boolean habilitado) throws SQLException {
        String table = DB.table("AGENCIAS_CONFIG");
        StringBuilder sql = new StringBuilder("UPDATE " + table + " SET ACTUALIZADO_EN = CURRENT_TIMESTAMP");
        List<Object> params = new ArrayList<>();
        int paramIndex = 1;
        
        if (nombre != null) {
            sql.append(", NOMBRE = ?");
            params.add(nombre);
        }
        if (apiUrl != null) {
            sql.append(", API_URL = ?");
            params.add(apiUrl);
        }
        if (idUsuarioWs != null) {
            sql.append(", ID_USUARIO_WS = ?");
            params.add(idUsuarioWs);
        }
        if (habilitado != null) {
            sql.append(", HABILITADO = ?");
            params.add(habilitado ? 1 : 0);
        }
        
        sql.append(" WHERE ID_AGENCIA = ?");
        params.add(idAgencia);
        
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof String) {
                    ps.setString(i + 1, (String) param);
                } else if (param instanceof Long) {
                    ps.setLong(i + 1, (Long) param);
                } else if (param instanceof Integer) {
                    ps.setInt(i + 1, (Integer) param);
                }
            }
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Agencia no encontrada: " + idAgencia);
            }
        }
    }
    
    /**
     * Elimina una agencia (soft delete: deshabilita)
     */
    public void eliminar(String idAgencia) throws SQLException {
        String table = DB.table("AGENCIAS_CONFIG");
        String sql = "DELETE FROM " + table + " WHERE ID_AGENCIA = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idAgencia);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("Agencia no encontrada: " + idAgencia);
            }
        }
    }
    
    /**
     * Obtiene agencias asociadas a un usuario webservice (útil para analíticos)
     */
    public List<Map<String, Object>> obtenerPorUsuarioWs(Long idUsuarioWs) throws SQLException {
        String table = DB.table("AGENCIAS_CONFIG");
        String sql = "SELECT ID_AGENCIA, NOMBRE, API_URL, ID_USUARIO_WS, HABILITADO FROM " + table + " WHERE ID_USUARIO_WS = ? AND HABILITADO = 1";
        List<Map<String, Object>> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idUsuarioWs);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("idAgencia", rs.getString("ID_AGENCIA"));
                    row.put("nombre", rs.getString("NOMBRE"));
                    row.put("apiUrl", rs.getString("API_URL"));
                    row.put("idUsuarioWs", rs.getLong("ID_USUARIO_WS"));
                    row.put("habilitado", rs.getInt("HABILITADO") == 1);
                    out.add(row);
                }
            }
        }
        return out;
    }
}


