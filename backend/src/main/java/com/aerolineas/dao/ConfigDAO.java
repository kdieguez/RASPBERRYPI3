package com.aerolineas.dao;

import java.sql.*;
import java.util.*;

public class ConfigDAO {

    private final Connection conn;

    public ConfigDAO(Connection conn) {
        this.conn = conn;
    }

    public Map<String, String> getSection(String section) throws SQLException {
        String sql = """
            SELECT d.NOMBRE, d.DESCRIPCION
              FROM AEROLINEA.DATOS_ESTRUCTURA d
              JOIN AEROLINEA.ESTRUCTURA e ON e.ID_ESTRUCTURA = d.ID_ESTRUCTURA
             WHERE LOWER(e.SECCION) = ?
            """;
        Map<String,String> out = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, section.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getString("NOMBRE"), rs.getString("DESCRIPCION"));
                }
            }
        }
        return out;
    }

    public void upsertItem(String section, String name, String value) throws SQLException {
        String sql = """
            MERGE INTO AEROLINEA.DATOS_ESTRUCTURA dst
            USING (
                SELECT ? AS NOMBRE,
                       (SELECT ID_ESTRUCTURA FROM AEROLINEA.ESTRUCTURA WHERE LOWER(SECCION)=?) AS ID_ESTRUCTURA
                FROM dual
            ) src
            ON (dst.NOMBRE = src.NOMBRE AND dst.ID_ESTRUCTURA = src.ID_ESTRUCTURA)
            WHEN MATCHED THEN UPDATE SET dst.DESCRIPCION = ?
            WHEN NOT MATCHED THEN INSERT (NOMBRE, DESCRIPCION, ID_ESTRUCTURA)
                 VALUES (src.NOMBRE, ?, src.ID_ESTRUCTURA)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, section.toLowerCase());
            ps.setString(3, value);
            ps.setString(4, value);
            ps.executeUpdate();
        }
    }

    public void upsertSection(String section, Map<String,String> entries) throws SQLException {
        boolean oldAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            for (Map.Entry<String,String> e : entries.entrySet()) {
                upsertItem(section, e.getKey(), e.getValue());
            }
            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }
    }

    public Map<String, Map<String,String>> getAll() throws SQLException {
        Map<String, Map<String,String>> out = new LinkedHashMap<>();
        out.put("header", getSection("header"));
        out.put("footer", getSection("footer"));
        return out;
    }
}
