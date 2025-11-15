package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.dto.MediaDTO;
import com.aerolineas.dto.SeccionDTO;

import java.sql.*;
import java.util.*;

public class SeccionDAO {

  public long crear(long idPagina, SeccionDTO.Upsert dto) throws SQLException {
    String seccionTable = DB.table("SECCION_INFORMATIVA");
    String sql = "INSERT INTO " + seccionTable + " (ID_PAGINA, NOMBRE_SECCION, DESCRIPCION, ORDEN) VALUES (?,?,?,?)";
    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql, new String[]{"ID_SECCION"})) {
      ps.setLong(1, idPagina);
      ps.setString(2, dto.nombreSeccion);
      if (dto.descripcion == null) {
        ps.setNull(3, Types.CLOB);
      } else {
        ps.setString(3, dto.descripcion);
      }
      if (dto.orden == null) {
        ps.setNull(4, Types.INTEGER);
      } else {
        ps.setInt(4, dto.orden);
      }
      ps.executeUpdate();
      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) return rs.getLong(1);
      }
    }
    throw new SQLException("No se generó ID_SECCION");
  }

  public void actualizar(long idSeccion, SeccionDTO.Upsert dto) throws SQLException {
    String seccionTable = DB.table("SECCION_INFORMATIVA");
    String sql = "UPDATE " + seccionTable + " SET NOMBRE_SECCION = ?, DESCRIPCION = ?, ORDEN = NVL(?, ORDEN) WHERE ID_SECCION = ?";
    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, dto.nombreSeccion);
      if (dto.descripcion == null) {
        ps.setNull(2, Types.CLOB);
      } else {
        ps.setString(2, dto.descripcion);
      }
      if (dto.orden == null) {
        ps.setNull(3, Types.INTEGER);
      } else {
        ps.setInt(3, dto.orden);
      }
      ps.setLong(4, idSeccion);
      if (ps.executeUpdate() == 0) throw new SQLException("Sección no encontrada");
    }
  }

  public void eliminar(long idSeccion) throws SQLException {
    String seccionTable = DB.table("SECCION_INFORMATIVA");
    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(
           "DELETE FROM " + seccionTable + " WHERE ID_SECCION = ?")) {
      ps.setLong(1, idSeccion);
      ps.executeUpdate();
    }
  }

  public List<SeccionDTO> listarPorPagina(long idPagina) throws SQLException {
    String seccionTable = DB.table("SECCION_INFORMATIVA");
    String sql = "SELECT ID_SECCION, ID_PAGINA, NOMBRE_SECCION, DBMS_LOB.SUBSTR(DESCRIPCION, 4000, 1) AS DESCRIPCION, ORDEN FROM " + seccionTable + " WHERE ID_PAGINA = ? ORDER BY ORDEN ASC, ID_SECCION ASC";
    List<SeccionDTO> out = new ArrayList<>();
    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setLong(1, idPagina);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          SeccionDTO s = new SeccionDTO();
          s.idSeccion = rs.getLong("ID_SECCION");
          s.idPagina = rs.getLong("ID_PAGINA");
          s.nombreSeccion = rs.getString("NOMBRE_SECCION");
          s.descripcion = rs.getString("DESCRIPCION");
          s.orden = rs.getInt("ORDEN");
          s.media = new MediaDAO().listarPorSeccion(s.idSeccion);
          out.add(s);
        }
      }
    }
    return out;
  }

  public void reordenar(long idPagina, List<SeccionDTO.Reordenar> items) throws SQLException {
    String seccionTable = DB.table("SECCION_INFORMATIVA");
    String shiftSql = "UPDATE " + seccionTable + " SET ORDEN = -ORDEN WHERE ID_PAGINA = ? AND ORDEN IS NOT NULL";
    String upSql = "UPDATE " + seccionTable + " SET ORDEN = ? WHERE ID_SECCION = ? AND ID_PAGINA = ?";
    try (Connection cn = DB.getConnection()) {
      cn.setAutoCommit(false);

      try (PreparedStatement psShift = cn.prepareStatement(shiftSql)) {
        psShift.setLong(1, idPagina);
        psShift.executeUpdate();
      }
      
      try (PreparedStatement ps = cn.prepareStatement(upSql)) {
        for (SeccionDTO.Reordenar it : items) {
          ps.setInt(1, it.orden);      
          ps.setLong(2, it.idSeccion);
          ps.setLong(3, idPagina);
          ps.addBatch();
        }
        ps.executeBatch();
      }

      cn.commit();
    }
  }
}
