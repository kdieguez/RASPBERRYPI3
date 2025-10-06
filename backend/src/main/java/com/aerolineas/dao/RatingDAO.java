package com.aerolineas.dao;

import com.aerolineas.config.DB;

import java.math.BigDecimal;
import java.sql.*;

public class RatingDAO {

  public static record Resumen(Double promedio, Long total, Integer miRating) {}

  public Resumen resumen(long idVuelo, Long idUsuario) throws SQLException {
    String qSum = """
      SELECT AVG(CALIFICACION) AS PROM, COUNT(*) AS TOT
        FROM AEROLINEA.RESENA_VUELO
       WHERE ID_VUELO = ?
    """;
    String qMine = """
      SELECT CALIFICACION
        FROM AEROLINEA.RESENA_VUELO
       WHERE ID_VUELO = ? AND ID_USUARIO_AUTOR = ?
    """;

    Double prom = 0d;
    long tot = 0L;
    Integer mine = null;

    try (Connection cn = DB.getConnection()) {
      try (PreparedStatement ps = cn.prepareStatement(qSum)) {
        ps.setLong(1, idVuelo);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            BigDecimal p = rs.getBigDecimal("PROM");
            prom = (p == null) ? 0d : p.doubleValue();
            tot  = rs.getLong("TOT");
          }
        }
      }

      if (idUsuario != null) {
        try (PreparedStatement ps = cn.prepareStatement(qMine)) {
          ps.setLong(1, idVuelo);
          ps.setLong(2, idUsuario);
          try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
              mine = rs.getInt(1);
              if (rs.wasNull()) mine = null;
            }
          }
        }
      }
    }
    return new Resumen(prom, tot, mine);
  }

  public void upsertRating(long idVuelo, long idUsuario, int calificacion) throws SQLException {
    if (calificacion < 1 || calificacion > 5)
      throw new SQLException("calificacion debe ser 1..5");

    String upd = """
      UPDATE AEROLINEA.RESENA_VUELO
         SET CALIFICACION = ?, CREADA_EN = SYSTIMESTAMP
       WHERE ID_VUELO = ? AND ID_USUARIO_AUTOR = ?
    """;
    String ins = """
      INSERT INTO AEROLINEA.RESENA_VUELO (ID_VUELO, ID_USUARIO_AUTOR, CALIFICACION, CREADA_EN)
      VALUES (?,?,?, SYSTIMESTAMP)
    """;

    try (Connection cn = DB.getConnection()) {
      cn.setAutoCommit(false);
      try {
        int n;
        try (PreparedStatement ps = cn.prepareStatement(upd)) {
          ps.setInt(1, calificacion);
          ps.setLong(2, idVuelo);
          ps.setLong(3, idUsuario);
          n = ps.executeUpdate();
        }
        if (n == 0) {
          try (PreparedStatement ps = cn.prepareStatement(ins)) {
            ps.setLong(1, idVuelo);
            ps.setLong(2, idUsuario);
            ps.setInt(3, calificacion);
            ps.executeUpdate();
          }
        }
        cn.commit();
      } catch (Exception e) {
        cn.rollback();
        if (e instanceof SQLException se) throw se;
        throw new SQLException("No se pudo registrar el rating", e);
      } finally {
        cn.setAutoCommit(true);
      }
    }
  }
}
