package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.dto.MediaDTO;

import java.sql.*;
import java.util.*;

public class MediaDAO {

  public long crear(long idSeccion, MediaDTO.Upsert dto) throws SQLException {
    String sql = "INSERT INTO MEDIA_INFORMATIVA (ID_SECCION, TIPO_MEDIA, URL, ORDEN) VALUES (?,?,?, NVL(?,1))";
    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql, new String[]{"ID_MEDIA"})) {
      ps.setLong(1, idSeccion);
      ps.setString(2, dto.tipoMedia);
      ps.setString(3, dto.url);
      if (dto.orden == null) ps.setNull(4, Types.INTEGER); else ps.setInt(4, dto.orden);
      ps.executeUpdate();
      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) return rs.getLong(1);
      }
    }
    throw new SQLException("No se generó ID_MEDIA");
  }

  public void eliminar(long idMedia) throws SQLException {
    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement("DELETE FROM MEDIA_INFORMATIVA WHERE ID_MEDIA=?")) {
      ps.setLong(1, idMedia);
      ps.executeUpdate();
    }
  }

  public void reordenar(long idSeccion, List<MediaDTO.Reordenar> items) throws SQLException {
    String up = "UPDATE MEDIA_INFORMATIVA SET ORDEN=? WHERE ID_MEDIA=? AND ID_SECCION=?";
    try (Connection cn = DB.getConnection()) {
      cn.setAutoCommit(false);
      try (PreparedStatement ps = cn.prepareStatement(up)) {
        for (MediaDTO.Reordenar it : items) {
          ps.setInt(1, it.orden);
          ps.setLong(2, it.idMedia);
          ps.setLong(3, idSeccion);
          ps.addBatch();
        }
        ps.executeBatch();
      }
      cn.commit();
    }
  }

  public List<MediaDTO> listarPorSeccion(long idSeccion) throws SQLException {
    String sql = """
      SELECT ID_MEDIA, ID_SECCION, TIPO_MEDIA, URL, ORDEN
        FROM MEDIA_INFORMATIVA
       WHERE ID_SECCION=?
       ORDER BY ORDEN, ID_MEDIA
      """;
    List<MediaDTO> out = new ArrayList<>();
    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setLong(1, idSeccion);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          MediaDTO m = new MediaDTO();
          m.idMedia = rs.getLong("ID_MEDIA");
          m.idSeccion = rs.getLong("ID_SECCION");
          m.tipoMedia = rs.getString("TIPO_MEDIA");
          m.url = rs.getString("URL");
          m.orden = rs.getInt("ORDEN");
          out.add(m);
        }
      }
    }
    return out;
  }
}
