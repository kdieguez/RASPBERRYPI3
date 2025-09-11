package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.model.Usuario;
import com.aerolineas.dto.UsuarioAdminDTOs;
import com.aerolineas.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

  public Usuario findById(long id) throws Exception {
    String sql = """
      SELECT ID_USUARIO, EMAIL, CONTRASENA, NOMBRES, APELLIDOS, HABILITADO, ID_ROL
      FROM USUARIO
      WHERE ID_USUARIO = ?
    """;
    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) return null;

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
    }
  }

  public List<UsuarioAdminDTOs.Row> adminList(String q, int offset, int limit) throws Exception {
    String base = """
      SELECT u.ID_USUARIO, u.EMAIL, u.NOMBRES, u.APELLIDOS, u.ID_ROL, r.NOMBRE AS ROL_NOMBRE, u.HABILITADO
      FROM USUARIO u
      JOIN ROL r ON r.ID_ROL = u.ID_ROL
      WHERE 1=1
    """;
    boolean hasQ = q != null && !q.isBlank();
    String where = hasQ
        ? " AND (LOWER(u.EMAIL) LIKE ? OR LOWER(u.NOMBRES) LIKE ? OR LOWER(u.APELLIDOS) LIKE ?) "
        : "";
    String paging = " ORDER BY u.ID_USUARIO DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
    String sql = base + where + paging;

    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      int idx = 1;
      if (hasQ) {
        String like = "%" + q.trim().toLowerCase() + "%";
        ps.setString(idx++, like);
        ps.setString(idx++, like);
        ps.setString(idx++, like);
      }
      ps.setInt(idx++, offset);
      ps.setInt(idx, limit);

      try (ResultSet rs = ps.executeQuery()) {
        List<UsuarioAdminDTOs.Row> out = new ArrayList<>();
        while (rs.next()) {
          out.add(new UsuarioAdminDTOs.Row(
              rs.getLong("ID_USUARIO"),
              rs.getString("EMAIL"),
              rs.getString("NOMBRES"),
              rs.getString("APELLIDOS"),
              rs.getInt("ID_ROL"),
              rs.getString("ROL_NOMBRE"),
              rs.getInt("HABILITADO")
          ));
        }
        return out;
      }
    }
  }

  public UsuarioAdminDTOs.View adminGet(long id) throws Exception {
    String sql = """
      SELECT u.ID_USUARIO, u.EMAIL, u.NOMBRES, u.APELLIDOS, u.ID_ROL, r.NOMBRE AS ROL_NOMBRE, u.HABILITADO,
             p.FECHA_NACIMIENTO, p.ID_PAIS_DOCUMENTO, p.PASAPORTE
      FROM USUARIO u
      JOIN ROL r ON r.ID_ROL = u.ID_ROL
      LEFT JOIN PASAJERO p ON p.ID_USUARIO = u.ID_USUARIO
      WHERE u.ID_USUARIO = ?
    """;
    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) return null;
        Date d = rs.getDate("FECHA_NACIMIENTO");
        String f = (d == null) ? null : d.toLocalDate().toString();

        return new UsuarioAdminDTOs.View(
            rs.getLong("ID_USUARIO"),
            rs.getString("EMAIL"),
            rs.getString("NOMBRES"),
            rs.getString("APELLIDOS"),
            rs.getInt("ID_ROL"),
            rs.getString("ROL_NOMBRE"),
            rs.getInt("HABILITADO"),
            f,
            rs.getObject("ID_PAIS_DOCUMENTO") == null ? null : rs.getLong("ID_PAIS_DOCUMENTO"),
            rs.getString("PASAPORTE")
        );
      }
    }
  }

  public void adminUpdate(long id, UsuarioAdminDTOs.UpdateAdmin dto) throws Exception {
    try (Connection cn = DB.getConnection()) {
      cn.setAutoCommit(false);
      try {
        String up = "UPDATE USUARIO SET NOMBRES=?, APELLIDOS=?, ID_ROL=?, HABILITADO=?"
                  + (dto.newPassword()!=null && !dto.newPassword().isBlank() ? ", CONTRASENA=?" : "")
                  + " WHERE ID_USUARIO=?";
        try (PreparedStatement ps = cn.prepareStatement(up)) {
          int i = 1;
          ps.setString(i++, dto.nombres().trim());
          ps.setString(i++, dto.apellidos().trim());
          ps.setInt(i++, dto.idRol());
          ps.setInt(i++, dto.habilitado()!=null && dto.habilitado()==1 ? 1 : 0);
          if (dto.newPassword()!=null && !dto.newPassword().isBlank()) {
            ps.setString(i++, PasswordUtil.hash(dto.newPassword()));
          }
          ps.setLong(i, id);
          ps.executeUpdate();
        }

        // Pasajero (upsert)
        boolean exists;
        try (PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM PASAJERO WHERE ID_USUARIO=?")) {
          ps.setLong(1, id);
          try (ResultSet rs = ps.executeQuery()) { exists = rs.next(); }
        }
        if (!exists) {
          String ins = "INSERT INTO PASAJERO (FECHA_NACIMIENTO, ID_PAIS_DOCUMENTO, PASAPORTE, ID_USUARIO) VALUES (?,?,?,?)";
          try (PreparedStatement ps = cn.prepareStatement(ins)) {
            if (dto.fechaNacimiento()!=null && !dto.fechaNacimiento().isBlank())
              ps.setDate(1, Date.valueOf(dto.fechaNacimiento()));
            else ps.setNull(1, Types.DATE);

            if (dto.idPais()!=null) ps.setLong(2, dto.idPais()); else ps.setNull(2, Types.NUMERIC);
            ps.setString(3, dto.pasaporte());
            ps.setLong(4, id);
            ps.executeUpdate();
          }
        } else {
          String upd = "UPDATE PASAJERO SET FECHA_NACIMIENTO=?, ID_PAIS_DOCUMENTO=?, PASAPORTE=? WHERE ID_USUARIO=?";
          try (PreparedStatement ps = cn.prepareStatement(upd)) {
            if (dto.fechaNacimiento()!=null && !dto.fechaNacimiento().isBlank())
              ps.setDate(1, Date.valueOf(dto.fechaNacimiento()));
            else ps.setNull(1, Types.DATE);

            if (dto.idPais()!=null) ps.setLong(2, dto.idPais()); else ps.setNull(2, Types.NUMERIC);
            ps.setString(3, dto.pasaporte());
            ps.setLong(4, id);
            ps.executeUpdate();
          }
        }

        cn.commit();
      } catch (Exception e) {
        cn.rollback();
        throw e;
      } finally {
        cn.setAutoCommit(true);
      }
    }
  }

  public void selfUpdate(long id, UsuarioAdminDTOs.UpdateSelf dto) throws Exception {
    try (Connection cn = DB.getConnection()) {
      cn.setAutoCommit(false);
      try {
        String up = "UPDATE USUARIO SET NOMBRES=?, APELLIDOS=?"
                  + (dto.newPassword()!=null && !dto.newPassword().isBlank() ? ", CONTRASENA=?" : "")
                  + " WHERE ID_USUARIO=?";
        try (PreparedStatement ps = cn.prepareStatement(up)) {
          int i=1;
          ps.setString(i++, dto.nombres().trim());
          ps.setString(i++, dto.apellidos().trim());
          if (dto.newPassword()!=null && !dto.newPassword().isBlank()) {
            ps.setString(i++, PasswordUtil.hash(dto.newPassword()));
          }
          ps.setLong(i, id);
          ps.executeUpdate();
        }

        boolean exists;
        try (PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM PASAJERO WHERE ID_USUARIO=?")) {
          ps.setLong(1, id);
          try (ResultSet rs = ps.executeQuery()) { exists = rs.next(); }
        }
        if (!exists) {
          String ins = "INSERT INTO PASAJERO (FECHA_NACIMIENTO, ID_PAIS_DOCUMENTO, PASAPORTE, ID_USUARIO) VALUES (?,?,?,?)";
          try (PreparedStatement ps = cn.prepareStatement(ins)) {
            if (dto.fechaNacimiento()!=null && !dto.fechaNacimiento().isBlank())
              ps.setDate(1, Date.valueOf(dto.fechaNacimiento()));
            else ps.setNull(1, Types.DATE);

            if (dto.idPais()!=null) ps.setLong(2, dto.idPais()); else ps.setNull(2, Types.NUMERIC);
            ps.setString(3, dto.pasaporte());
            ps.setLong(4, id);
            ps.executeUpdate();
          }
        } else {
          String upd = "UPDATE PASAJERO SET FECHA_NACIMIENTO=?, ID_PAIS_DOCUMENTO=?, PASAPORTE=? WHERE ID_USUARIO=?";
          try (PreparedStatement ps = cn.prepareStatement(upd)) {
            if (dto.fechaNacimiento()!=null && !dto.fechaNacimiento().isBlank())
              ps.setDate(1, Date.valueOf(dto.fechaNacimiento()));
            else ps.setNull(1, Types.DATE);

            if (dto.idPais()!=null) ps.setLong(2, dto.idPais()); else ps.setNull(2, Types.NUMERIC);
            ps.setString(3, dto.pasaporte());
            ps.setLong(4, id);
            ps.executeUpdate();
          }
        }

        cn.commit();
      } catch (Exception e) {
        cn.rollback();
        throw e;
      } finally {
        cn.setAutoCommit(true);
      }
    }
  }
}
