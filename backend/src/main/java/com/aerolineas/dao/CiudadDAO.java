package com.aerolineas.dao;


import com.aerolineas.dto.CiudadDTOs;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CiudadDAO {

    public long create(CiudadDTOs.Create dto) throws Exception {
        if (dto == null || dto.nombre() == null || dto.nombre().isBlank())
            throw new IllegalArgumentException("El nombre de la ciudad es requerido.");

        try (Connection cn = DB.getConnection()) {
            if (!existePais(cn, dto.idPais()))
                throw new IllegalArgumentException("El país indicado no existe.");

            if (existeCiudadEnPais(cn, dto.idPais(), dto.nombre()))
                throw new IllegalArgumentException("Ya existe una ciudad con ese nombre en el país.");

            String ciudadTable = DB.table("CIUDAD");
            String sql = "INSERT INTO " + ciudadTable + " (ID_PAIS, NOMBRE, ACTIVO, WEATHER_QUERY) VALUES (?,?,1,?)";

            try (PreparedStatement ps = cn.prepareStatement(sql, new String[]{"ID_CIUDAD"})) {
                ps.setLong(1, dto.idPais());
                ps.setString(2, dto.nombre().trim());

                if (dto.weatherQuery() == null || dto.weatherQuery().isBlank()) {
                    ps.setNull(3, Types.VARCHAR);
                } else {
                    ps.setString(3, dto.weatherQuery().trim());
                }

                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getLong(1);
                    throw new SQLException("No se generó ID_CIUDAD");
                }
            }
        }
    }

    public List<CiudadDTOs.View> listAll(Long idPais) throws Exception {
        String ciudadTable = DB.table("CIUDAD");
        String paisTable = DB.table("PAIS");
        String base = "SELECT c.ID_CIUDAD, c.ID_PAIS, p.NOMBRE AS PAIS, c.NOMBRE, NVL(c.ACTIVO,1) AS ACTIVO FROM " + ciudadTable + " c JOIN " + paisTable + " p ON p.ID_PAIS = c.ID_PAIS ";
        String order = " ORDER BY p.NOMBRE, c.NOMBRE";

        try (Connection cn = DB.getConnection();
             PreparedStatement ps = (idPais == null)
                     ? cn.prepareStatement(base + order)
                     : cn.prepareStatement(base + " WHERE c.ID_PAIS=? " + order)) {

            if (idPais != null) ps.setLong(1, idPais);

            try (ResultSet rs = ps.executeQuery()) {
                List<CiudadDTOs.View> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new CiudadDTOs.View(
                            rs.getLong("ID_CIUDAD"),
                            rs.getLong("ID_PAIS"),
                            rs.getString("PAIS"),
                            rs.getString("NOMBRE"),
                            rs.getInt("ACTIVO") == 1
                    ));
                }
                return out;
            }
        }
    }

   public List<CiudadDTOs.WeatherCity> listForWeather() throws Exception {
        String ciudadTable = DB.table("CIUDAD");
        String paisTable = DB.table("PAIS");
        String sql = "SELECT c.ID_CIUDAD, c.NOMBRE AS CIUDAD, p.NOMBRE AS PAIS, c.WEATHER_QUERY FROM " + ciudadTable + " c JOIN " + paisTable + " p ON p.ID_PAIS = c.ID_PAIS WHERE NVL(c.ACTIVO,1)=1 AND c.WEATHER_QUERY IS NOT NULL ORDER BY p.NOMBRE, c.NOMBRE";

        try (Connection cn = DB.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<CiudadDTOs.WeatherCity> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new CiudadDTOs.WeatherCity(
                        rs.getLong("ID_CIUDAD"),
                        rs.getString("CIUDAD"),
                        rs.getString("PAIS"),
                        rs.getString("WEATHER_QUERY")
                ));
            }
            return out;
        }
    }

    public void toggleActiva(long idCiudad) throws Exception {
        String ciudadTable = DB.table("CIUDAD");
        String sql = "UPDATE " + ciudadTable + " SET ACTIVO = CASE WHEN NVL(ACTIVO,1)=1 THEN 0 ELSE 1 END WHERE ID_CIUDAD=?";
        try (Connection cn = DB.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, idCiudad);
            int n = ps.executeUpdate();
            if (n == 0) throw new IllegalArgumentException("Ciudad no encontrada.");
        }
    }

    private boolean existePais(Connection cn, long idPais) throws Exception {
        String paisTable = DB.table("PAIS");
        String sql = "SELECT 1 FROM " + paisTable + " WHERE ID_PAIS=?";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, idPais);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean existeCiudadEnPais(Connection cn, long idPais, String nombre) throws Exception {
        String ciudadTable = DB.table("CIUDAD");
        String sql = "SELECT 1 FROM " + ciudadTable + " WHERE ID_PAIS=? AND UPPER(NOMBRE)=UPPER(?)";
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, idPais);
            ps.setString(2, nombre.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
