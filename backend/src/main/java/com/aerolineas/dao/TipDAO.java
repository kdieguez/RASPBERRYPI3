package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.dto.TipDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TipDAO {

  public List<TipDTO> listar() throws SQLException {
    String tipsTable = DB.table("TIPS");
    String sql = "SELECT ID_TIP, TITULO, DBMS_LOB.SUBSTR(DESCRIPCION, 4000, 1) AS DESCRIPCION, ORDEN FROM " + tipsTable + " ORDER BY ORDEN ASC, ID_TIP ASC";
    List<TipDTO> out = new ArrayList<>();
    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        TipDTO t = new TipDTO();
        t.idTip = rs.getLong("ID_TIP");
        t.titulo = rs.getString("TITULO");
        t.descripcion = rs.getString("DESCRIPCION");
        t.orden = rs.getInt("ORDEN");
        out.add(t);
      }
    }
    return out;
  }

  public TipDTO obtenerPorId(long idTip) throws SQLException {
    String tipsTable = DB.table("TIPS");
    String sql = "SELECT ID_TIP, TITULO, DBMS_LOB.SUBSTR(DESCRIPCION, 4000, 1) AS DESCRIPCION, ORDEN FROM " + tipsTable + " WHERE ID_TIP = ?";
    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setLong(1, idTip);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) return null;
        TipDTO t = new TipDTO();
        t.idTip = rs.getLong("ID_TIP");
        t.titulo = rs.getString("TITULO");
        t.descripcion = rs.getString("DESCRIPCION");
        t.orden = rs.getInt("ORDEN");
        return t;
      }
    }
  }

  public long crear(TipDTO.Upsert dto) throws SQLException {
    try (Connection cn = DB.getConnection()) {
      cn.setAutoCommit(false);
      try {
        String tipsTable = DB.table("TIPS");
        int maxOrden = 0;
        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT NVL(MAX(ORDEN), 0) AS MAXO FROM " + tipsTable);
             ResultSet rs = ps.executeQuery()) {
          if (rs.next()) maxOrden = rs.getInt("MAXO");
        }

        int nuevoOrden;
        if (dto.orden == null || dto.orden <= 0) {
          nuevoOrden = maxOrden + 1;                
        } else if (dto.orden > maxOrden + 1) {
          nuevoOrden = maxOrden + 1;                
        } else {
          nuevoOrden = dto.orden;                   
        }

        if (nuevoOrden <= maxOrden) {
          try (PreparedStatement ps = cn.prepareStatement(
              "UPDATE " + tipsTable + " SET ORDEN = ORDEN + 1 WHERE ORDEN >= ?")) {
            ps.setInt(1, nuevoOrden);
            ps.executeUpdate();
          }
        }

        String sql = "INSERT INTO " + tipsTable + " (TITULO, DESCRIPCION, ORDEN) VALUES (?,?,?)";
        try (PreparedStatement ps = cn.prepareStatement(sql, new String[]{"ID_TIP"})) {
          ps.setString(1, dto.titulo);
          ps.setString(2, dto.descripcion);
          ps.setInt(3, nuevoOrden);
          ps.executeUpdate();

          try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) {
              long id = rs.getLong(1);
              cn.commit();
              return id;
            }
          }
        }

        throw new SQLException("No se generó ID_TIP");

      } catch (Exception ex) {
        cn.rollback();
        if (ex instanceof SQLException) throw (SQLException) ex;
        throw new SQLException("Error al crear tip", ex);
      } finally {
        cn.setAutoCommit(true);
      }
    }
  }

  public void actualizar(long idTip, TipDTO.Upsert dto) throws SQLException {
    try (Connection cn = DB.getConnection()) {
      cn.setAutoCommit(false);
      try {
        String tipsTable = DB.table("TIPS");
        int ordenActual;
        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT ORDEN FROM " + tipsTable + " WHERE ID_TIP = ? FOR UPDATE")) {
          ps.setLong(1, idTip);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new SQLException("Tip no encontrado");
            ordenActual = rs.getInt("ORDEN");
          }
        }

        int maxOrden = 0;
        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT NVL(MAX(ORDEN), 0) AS MAXO FROM " + tipsTable);
             ResultSet rs = ps.executeQuery()) {
          if (rs.next()) maxOrden = rs.getInt("MAXO");
        }

        int nuevoOrden;
        if (dto.orden == null || dto.orden <= 0) {
          nuevoOrden = ordenActual;                 
        } else if (dto.orden > maxOrden) {
          nuevoOrden = maxOrden;                    
        } else {
          nuevoOrden = dto.orden;
        }
        
        if (nuevoOrden != ordenActual) {
          if (nuevoOrden < ordenActual) {
        
            try (PreparedStatement ps = cn.prepareStatement(
                "UPDATE " + tipsTable + " SET ORDEN = ORDEN + 1 WHERE ORDEN >= ? AND ORDEN < ? AND ID_TIP <> ?")) {
              ps.setInt(1, nuevoOrden);
              ps.setInt(2, ordenActual);
              ps.setLong(3, idTip);
              ps.executeUpdate();
            }
          } else {
            try (PreparedStatement ps = cn.prepareStatement(
                "UPDATE " + tipsTable + " SET ORDEN = ORDEN - 1 WHERE ORDEN <= ? AND ORDEN > ? AND ID_TIP <> ?")) {
              ps.setInt(1, nuevoOrden);
              ps.setInt(2, ordenActual);
              ps.setLong(3, idTip);
              ps.executeUpdate();
            }
          }
        }

        String up = "UPDATE " + tipsTable + " SET TITULO = ?, DESCRIPCION = ?, ORDEN = ? WHERE ID_TIP = ?";
        try (PreparedStatement ps = cn.prepareStatement(up)) {
          ps.setString(1, dto.titulo);
          ps.setString(2, dto.descripcion);
          ps.setInt(3, nuevoOrden);
          ps.setLong(4, idTip);

          if (ps.executeUpdate() == 0) {
            throw new SQLException("Tip no encontrado");
          }
        }

        cn.commit();

      } catch (Exception ex) {
        cn.rollback();
        if (ex instanceof SQLException) throw (SQLException) ex;
        throw new SQLException("Error al actualizar tip", ex);
      } finally {
        cn.setAutoCommit(true);
      }
    }
  }

  public void eliminar(long idTip) throws SQLException {
    try (Connection cn = DB.getConnection()) {
      cn.setAutoCommit(false);
      try {
        String tipsTable = DB.table("TIPS");
        Integer orden = null;
        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT ORDEN FROM " + tipsTable + " WHERE ID_TIP = ?")) {
          ps.setLong(1, idTip);
          try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
              orden = rs.getInt("ORDEN");
            }
          }
        }

        try (PreparedStatement ps = cn.prepareStatement(
            "DELETE FROM " + tipsTable + " WHERE ID_TIP = ?")) {
          ps.setLong(1, idTip);
          ps.executeUpdate();
        }

        if (orden != null) {
          try (PreparedStatement ps = cn.prepareStatement(
              "UPDATE " + tipsTable + " SET ORDEN = ORDEN - 1 WHERE ORDEN > ?")) {
            ps.setInt(1, orden);
            ps.executeUpdate();
          }
        }

        cn.commit();
      } catch (Exception ex) {
        cn.rollback();
        if (ex instanceof SQLException) throw (SQLException) ex;
        throw new SQLException("Error al eliminar tip", ex);
      } finally {
        cn.setAutoCommit(true);
      }
    }
  }
}
