package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.dto.ComentarioDTO;

import java.sql.*;
import java.util.*;

public class ComentarioDAO {

  public List<ComentarioDTO.View> listarPublic(long idVuelo) throws SQLException {
    String comentarioTable = DB.table("VUELO_COMENTARIO");
    String usuarioTable = DB.table("USUARIO");
    String resenaTable = DB.table("RESENA_VUELO");
    String sql = "SELECT vc.ID_COMENTARIO, vc.ID_VUELO, vc.ID_USUARIO, COALESCE(TRIM(NVL(u.NOMBRES,'') || ' ' || NVL(u.APELLIDOS,'')), u.EMAIL) AS AUTOR, vc.COMENTARIO, vc.ID_PADRE, vc.CREADA_EN, (SELECT rv.CALIFICACION FROM " + resenaTable + " rv WHERE rv.ID_VUELO = vc.ID_VUELO AND rv.ID_USUARIO_AUTOR = vc.ID_USUARIO FETCH FIRST 1 ROWS ONLY) AS RATING_AUTOR FROM " + comentarioTable + " vc JOIN " + usuarioTable + " u ON u.ID_USUARIO = vc.ID_USUARIO WHERE vc.ID_VUELO = ? ORDER BY vc.CREADA_EN ASC";

    Map<Long, ComentarioDTO.View> byId = new LinkedHashMap<>();
    List<ComentarioDTO.View> roots = new ArrayList<>();

    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setLong(1, idVuelo);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Long id = rs.getLong("ID_COMENTARIO");
          Long idPadre = rs.getObject("ID_PADRE") == null ? null : rs.getLong("ID_PADRE");
          Integer ratingAutor = null;
          Object rObj = rs.getObject("RATING_AUTOR");
          if (rObj != null) ratingAutor = ((Number) rObj).intValue();

          var node = new ComentarioDTO.View(
              id,
              rs.getLong("ID_VUELO"),
              rs.getLong("ID_USUARIO"),
              rs.getString("AUTOR"),
              rs.getString("COMENTARIO"),
              rs.getTimestamp("CREADA_EN").toLocalDateTime(),
              idPadre,
              ratingAutor   
          );
          byId.put(id, node);
        }
      }
    }

    for (ComentarioDTO.View v : byId.values()) {
      if (v.getIdPadre() == null) {
        roots.add(v);
      } else {
        ComentarioDTO.View padre = byId.get(v.getIdPadre());
        if (padre != null) padre.getRespuestas().add(v);
        else roots.add(v); 
      }
    }
    return roots;
  }

  public long crear(long idVuelo, long idUsuario, String comentario, Long idPadre) throws SQLException {
    if (comentario == null || comentario.trim().isEmpty())
      throw new SQLException("El comentario es requerido");
    if (comentario.length() > 2000)
      throw new SQLException("Máximo 2000 caracteres");

    String vueloTable = DB.table("VUELO");
    String comentarioTable = DB.table("VUELO_COMENTARIO");
    String chkVuelo = "SELECT 1 FROM " + vueloTable + " WHERE ID_VUELO=?";
    String chkPadre = "SELECT 1 FROM " + comentarioTable + " WHERE ID_COMENTARIO=? AND ID_VUELO=?";
    String ins = "INSERT INTO " + comentarioTable + " (ID_VUELO, ID_USUARIO, ID_PADRE, COMENTARIO, CREADA_EN) VALUES (?,?,?,?, SYSTIMESTAMP)";

    try (Connection cn = DB.getConnection()) {
      cn.setAutoCommit(false);
      try {
        try (PreparedStatement ps = cn.prepareStatement(chkVuelo)) {
          ps.setLong(1, idVuelo);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new SQLException("Vuelo no existe: " + idVuelo);
          }
        }

        if (idPadre != null) {
          try (PreparedStatement ps = cn.prepareStatement(chkPadre)) {
            ps.setLong(1, idPadre);
            ps.setLong(2, idVuelo);
            try (ResultSet rs = ps.executeQuery()) {
              if (!rs.next()) throw new SQLException("El comentario padre no existe en este vuelo");
            }
          }
        }

        try (PreparedStatement ps = cn.prepareStatement(ins, new String[]{"ID_COMENTARIO"})) {
          ps.setLong(1, idVuelo);
          ps.setLong(2, idUsuario);
          if (idPadre == null) ps.setNull(3, Types.NUMERIC); else ps.setLong(3, idPadre);
          ps.setString(4, comentario.trim());
          ps.executeUpdate();

          try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) {
              long id = rs.getLong(1);
              cn.commit();
              return id;
            }
          }
        }
        throw new SQLException("No se generó ID_COMENTARIO");
      } catch (Exception e) {
        cn.rollback();
        if (e instanceof SQLException) throw (SQLException) e;
        throw new SQLException("Error al crear comentario", e);
      } finally {
        cn.setAutoCommit(true);
      }
    }
  }
}
