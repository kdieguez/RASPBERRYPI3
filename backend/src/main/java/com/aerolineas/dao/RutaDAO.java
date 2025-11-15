package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.dto.RutaDTOs;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RutaDAO {

    public long create(RutaDTOs.Create dto) throws Exception {
        if (dto.idCiudadOrigen() == dto.idCiudadDestino()) {
            throw new IllegalArgumentException("La ciudad de origen y destino no pueden ser iguales.");
        }

        try (Connection cn = DB.getConnection()) {
            if (!existeCiudadActiva(cn, dto.idCiudadOrigen())) {
                throw new IllegalArgumentException("Ciudad de origen no existe o no está activa.");
            }
            if (!existeCiudadActiva(cn, dto.idCiudadDestino())) {
                throw new IllegalArgumentException("Ciudad de destino no existe o no está activa.");
            }

            if (existeRuta(cn, dto.idCiudadOrigen(), dto.idCiudadDestino())) {
                throw new IllegalArgumentException("Ya existe una ruta con ese origen y destino.");
            }

            String rutaTable = DB.table("RUTA");
            String sql = "INSERT INTO " + rutaTable + " (ID_CIUDAD_ORIGEN, ID_CIUDAD_DESTINO, ACTIVA) VALUES (?,?,1)";
            try (PreparedStatement ps = cn.prepareStatement(sql, new String[]{"ID_RUTA"})) {
                ps.setLong(1, dto.idCiudadOrigen());
                ps.setLong(2, dto.idCiudadDestino());
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getLong(1);
                    throw new SQLException("No se generó ID_RUTA");
                }
            }
        }
    }

    public List<RutaDTOs.View> listAll() throws Exception {
        String rutaTable = DB.table("RUTA");
        String ciudadTable = DB.table("CIUDAD");
        String sql = "SELECT r.ID_RUTA, r.ID_CIUDAD_ORIGEN, co.NOMBRE AS CIUDAD_ORIGEN, r.ID_CIUDAD_DESTINO, cd.NOMBRE AS CIUDAD_DESTINO, r.ACTIVA FROM " + rutaTable + " r JOIN " + ciudadTable + " co ON co.ID_CIUDAD = r.ID_CIUDAD_ORIGEN JOIN " + ciudadTable + " cd ON cd.ID_CIUDAD = r.ID_CIUDAD_DESTINO ORDER BY co.NOMBRE, cd.NOMBRE";
        try (Connection cn = DB.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<RutaDTOs.View> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new RutaDTOs.View(
                        rs.getLong("ID_RUTA"),
                        rs.getLong("ID_CIUDAD_ORIGEN"),
                        rs.getString("CIUDAD_ORIGEN"),
                        rs.getLong("ID_CIUDAD_DESTINO"),
                        rs.getString("CIUDAD_DESTINO"),
                        rs.getInt("ACTIVA") == 1
                ));
            }
            return out;
        }
    }

    public void toggleActiva(long idRuta) throws Exception {
        String rutaTable = DB.table("RUTA");
        String sql = "UPDATE " + rutaTable + " SET ACTIVA = CASE WHEN ACTIVA=1 THEN 0 ELSE 1 END WHERE ID_RUTA=?";
        try (Connection cn = DB.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, idRuta);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new IllegalArgumentException("Ruta no encontrada.");
        }
    }

    // ===== Helpers =====
    private boolean existeCiudadActiva(Connection cn, long idCiudad) throws Exception {
        String ciudadTable = DB.table("CIUDAD");
        String sql = "SELECT 1 FROM " + ciudadTable + " WHERE ID_CIUDAD=? AND (ACTIVO=1 OR ACTIVO IS NULL)";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, idCiudad);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private boolean existeRuta(Connection cn, long idOrigen, long idDestino) throws Exception {
        String rutaTable = DB.table("RUTA");
        String sql = "SELECT 1 FROM " + rutaTable + " WHERE ID_CIUDAD_ORIGEN=? AND ID_CIUDAD_DESTINO=?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, idOrigen);
            ps.setLong(2, idDestino);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }
}
