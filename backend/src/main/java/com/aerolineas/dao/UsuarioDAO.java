package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.model.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UsuarioDAO {
  private Usuario map(ResultSet rs) throws SQLException {
    Usuario u = new Usuario();
    u.setIdUsuario(rs.getLong("ID_USUARIO"));
    u.setEmail(rs.getString("EMAIL"));
    u.setContrasenaHash(rs.getString("CONTRASENA"));
    u.setNombres(rs.getString("NOMBRES"));
    u.setApellidos(rs.getString("APELLIDOS"));
    u.setHabilitado(rs.getInt("HABILITADO") == 1);
    u.setIdRol(rs.getInt("ID_ROL"));
    return u;
  }

  private void applyTimeout(PreparedStatement ps) {
    try { ps.setQueryTimeout(30); } catch (Exception ignore) {}
  }

  public Usuario findByEmail(String email) {
    final String sql = """
      SELECT ID_USUARIO, EMAIL, CONTRASENA, NOMBRES, APELLIDOS, HABILITADO, ID_ROL
      FROM USUARIO
      WHERE LOWER(EMAIL) = LOWER(?)
    """;
    System.out.println("[DAO] findByEmail start " + email);
    try (Connection conn = DB.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
         applyTimeout(ps);
      ps.setString(1, email);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          System.out.println("[DAO] findByEmail row found");
          return map(rs);
        }
      }
      System.out.println("[DAO] findByEmail no row");
    } catch (SQLException e) {
      System.out.println("[DAO] findByEmail ERROR: " + e.getMessage());
      throw new RuntimeException(e);
    }
    return null;
  }

  public Usuario create(String email, String passHash, String nombres, String apellidos) {
    final String insert = """
      INSERT INTO USUARIO (EMAIL, CONTRASENA, NOMBRES, APELLIDOS, HABILITADO, ID_ROL)
      VALUES (?, ?, ?, ?, 1, 3)
    """;
    System.out.println("[DAO] create start " + email);
    try (Connection conn = DB.getConnection();
         PreparedStatement ps = conn.prepareStatement(insert)) {
      applyTimeout(ps);
      ps.setString(1, email);
      ps.setString(2, passHash);
      ps.setString(3, nombres);
      ps.setString(4, apellidos);
      ps.executeUpdate();
      System.out.println("[DAO] create OK, requery...");
      return findByEmail(email);
    } catch (SQLException e) {
      System.out.println("[DAO] create ERROR: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }
}
