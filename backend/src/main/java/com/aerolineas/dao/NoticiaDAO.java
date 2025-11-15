package com.aerolineas.dao;
import com.aerolineas.config.DB;
import com.aerolineas.dto.NoticiaDTO;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NoticiaDAO {
  
  public List<NoticiaDTO> listar() throws SQLException {
    String noticiasTable = DB.table("NOTICIAS");
    String sql = "SELECT ID_NOTICIA, TITULO, DBMS_LOB.SUBSTR(CONTENIDO, 4000, 1) AS CONTENIDO, FECHA_PUBLICACION, ORDEN, URL_IMAGEN FROM " + noticiasTable + " ORDER BY ORDEN ASC, FECHA_PUBLICACION DESC, ID_NOTICIA DESC";
    List<NoticiaDTO> out = new ArrayList<>();
    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        NoticiaDTO n = new NoticiaDTO();
        n.idNoticia = rs.getLong("ID_NOTICIA");
        n.titulo = rs.getString("TITULO");
        n.contenido = rs.getString("CONTENIDO");

        Timestamp ts = rs.getTimestamp("FECHA_PUBLICACION");
        n.fechaPublicacion = (ts != null ? ts.toLocalDateTime() : null);

        int ord = rs.getInt("ORDEN");
        n.orden = rs.wasNull() ? null : ord;

        n.urlImagen = rs.getString("URL_IMAGEN");
        out.add(n);
      }
    }
    return out;
  }

  public NoticiaDTO obtenerPorId(long idNoticia) throws SQLException {
    String noticiasTable = DB.table("NOTICIAS");
    String sql = "SELECT ID_NOTICIA, TITULO, DBMS_LOB.SUBSTR(CONTENIDO, 4000, 1) AS CONTENIDO, FECHA_PUBLICACION, ORDEN, URL_IMAGEN FROM " + noticiasTable + " WHERE ID_NOTICIA = ?";
    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setLong(1, idNoticia);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) return null;
        NoticiaDTO n = new NoticiaDTO();
        n.idNoticia = rs.getLong("ID_NOTICIA");
        n.titulo = rs.getString("TITULO");
        n.contenido = rs.getString("CONTENIDO");

        Timestamp ts = rs.getTimestamp("FECHA_PUBLICACION");
        n.fechaPublicacion = (ts != null ? ts.toLocalDateTime() : null);

        int ord = rs.getInt("ORDEN");
        n.orden = rs.wasNull() ? null : ord;

        n.urlImagen = rs.getString("URL_IMAGEN");
        return n;
      }
    }
  }

  public long crear(NoticiaDTO.Upsert dto) throws SQLException {
    try (Connection cn = DB.getConnection()) {
      cn.setAutoCommit(false);
      try {
        
        String noticiasTable = DB.table("NOTICIAS");
        int maxOrden = 0;
        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT NVL(MAX(ORDEN), 0) AS MAXO FROM " + noticiasTable);
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
              "UPDATE " + noticiasTable + " SET ORDEN = ORDEN + 1 WHERE ORDEN >= ?")) {
            ps.setInt(1, nuevoOrden);
            ps.executeUpdate();
          }
        }
        
        String sql = "INSERT INTO " + noticiasTable + " (TITULO, CONTENIDO, FECHA_PUBLICACION, ORDEN, URL_IMAGEN) VALUES (?, ?, NVL(?, SYSDATE), ?, ?)";
        try (PreparedStatement ps = cn.prepareStatement(sql, new String[]{"ID_NOTICIA"})) {
          ps.setString(1, dto.titulo);
          ps.setString(2, dto.contenido);

          if (dto.fechaPublicacion != null) {
            ps.setTimestamp(3, Timestamp.valueOf(dto.fechaPublicacion));
          } else {
            ps.setNull(3, Types.TIMESTAMP); 
          }

          ps.setInt(4, nuevoOrden);

          if (dto.urlImagen == null || dto.urlImagen.isBlank()) {
            ps.setNull(5, Types.VARCHAR);
          } else {
            ps.setString(5, dto.urlImagen);
          }

          ps.executeUpdate();
          try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) {
              long id = rs.getLong(1);
              cn.commit();
              return id;
            }
          }
        }

        throw new SQLException("No se generó ID_NOTICIA");

      } catch (Exception ex) {
        cn.rollback();
        if (ex instanceof SQLException) throw (SQLException) ex;
        throw new SQLException("Error al crear noticia", ex);
      } finally {
        cn.setAutoCommit(true);
      }
    }
  }

  public void actualizar(long idNoticia, NoticiaDTO.Upsert dto) throws SQLException {
    try (Connection cn = DB.getConnection()) {
      cn.setAutoCommit(false);
      try {
        
        String noticiasTable = DB.table("NOTICIAS");
        int ordenActual;
        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT ORDEN FROM " + noticiasTable + " WHERE ID_NOTICIA = ? FOR UPDATE")) {
          ps.setLong(1, idNoticia);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new SQLException("Noticia no encontrada");
            ordenActual = rs.getInt("ORDEN");
          }
        }

        int maxOrden = 0;
        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT NVL(MAX(ORDEN), 0) AS MAXO FROM " + noticiasTable);
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
                "UPDATE " + noticiasTable + " SET ORDEN = ORDEN + 1 WHERE ORDEN >= ? AND ORDEN < ? AND ID_NOTICIA <> ?")) {
              ps.setInt(1, nuevoOrden);
              ps.setInt(2, ordenActual);
              ps.setLong(3, idNoticia);
              ps.executeUpdate();
            }
          } else {
            
            try (PreparedStatement ps = cn.prepareStatement(
                "UPDATE " + noticiasTable + " SET ORDEN = ORDEN - 1 WHERE ORDEN <= ? AND ORDEN > ? AND ID_NOTICIA <> ?")) {
              ps.setInt(1, nuevoOrden);
              ps.setInt(2, ordenActual);
              ps.setLong(3, idNoticia);
              ps.executeUpdate();
            }
          }
        }
        
        String up = "UPDATE " + noticiasTable + " SET TITULO = ?, CONTENIDO = ?, FECHA_PUBLICACION = NVL(?, FECHA_PUBLICACION), ORDEN = ?, URL_IMAGEN = ? WHERE ID_NOTICIA = ?";
        try (PreparedStatement ps = cn.prepareStatement(up)) {
          ps.setString(1, dto.titulo);
          ps.setString(2, dto.contenido);

          if (dto.fechaPublicacion != null) {
            ps.setTimestamp(3, Timestamp.valueOf(dto.fechaPublicacion));
          } else {
            ps.setNull(3, Types.TIMESTAMP);
          }

          ps.setInt(4, nuevoOrden);

          if (dto.urlImagen == null || dto.urlImagen.isBlank()) {
            ps.setNull(5, Types.VARCHAR);
          } else {
            ps.setString(5, dto.urlImagen);
          }

          ps.setLong(6, idNoticia);

          if (ps.executeUpdate() == 0) {
            throw new SQLException("Noticia no encontrada");
          }
        }

        cn.commit();

      } catch (Exception ex) {
        cn.rollback();
        if (ex instanceof SQLException) throw (SQLException) ex;
        throw new SQLException("Error al actualizar noticia", ex);
      } finally {
        cn.setAutoCommit(true);
      }
    }
  }

  public void eliminar(long idNoticia) throws SQLException {
    try (Connection cn = DB.getConnection()) {
      cn.setAutoCommit(false);
      try {
        String noticiasTable = DB.table("NOTICIAS");
        Integer orden = null;
        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT ORDEN FROM " + noticiasTable + " WHERE ID_NOTICIA = ?")) {
          ps.setLong(1, idNoticia);
          try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
              orden = rs.getInt("ORDEN");
              if (rs.wasNull()) orden = null;
            }
          }
        }

        try (PreparedStatement ps = cn.prepareStatement(
            "DELETE FROM " + noticiasTable + " WHERE ID_NOTICIA = ?")) {
          ps.setLong(1, idNoticia);
          ps.executeUpdate();
        }

        if (orden != null) {
          try (PreparedStatement ps = cn.prepareStatement(
              "UPDATE " + noticiasTable + " SET ORDEN = ORDEN - 1 WHERE ORDEN > ?")) {
            ps.setInt(1, orden);
            ps.executeUpdate();
          }
        }

        cn.commit();
      } catch (Exception ex) {
        cn.rollback();
        if (ex instanceof SQLException) throw (SQLException) ex;
        throw new SQLException("Error al eliminar noticia", ex);
      } finally {
        cn.setAutoCommit(true);
      }
    }
  }
}
