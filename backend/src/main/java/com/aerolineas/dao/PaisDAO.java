package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.dto.PaisDTOs;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaisDAO {

    public long create(PaisDTOs.Create dto) throws Exception {
        if (dto == null || dto.nombre() == null || dto.nombre().isBlank())
            throw new IllegalArgumentException("El nombre del país es requerido.");

        try (Connection cn = DB.getConnection()) {
            if (existePais(cn, dto.nombre())) {
                throw new IllegalArgumentException("Ya existe un país con ese nombre.");
            }
            String sql = "INSERT INTO PAIS (NOMBRE) VALUES (?)";
            try (PreparedStatement ps = cn.prepareStatement(sql, new String[]{"ID_PAIS"})) {
                ps.setString(1, dto.nombre().trim());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getLong(1);
                    throw new SQLException("No se generó ID_PAIS");
                }
            }
        }
    }

    public List<PaisDTOs.View> listAll() throws Exception {
        String sql = "SELECT ID_PAIS, NOMBRE FROM PAIS ORDER BY NOMBRE";
        try (Connection cn = DB.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<PaisDTOs.View> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new PaisDTOs.View(rs.getLong("ID_PAIS"), rs.getString("NOMBRE")));
            }
            return out;
        }
    }

    private boolean existePais(Connection cn, String nombre) throws Exception {
        String sql = "SELECT 1 FROM PAIS WHERE UPPER(NOMBRE)=UPPER(?)";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }
}
