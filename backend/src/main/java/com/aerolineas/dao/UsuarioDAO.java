package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.model.Usuario;
import com.aerolineas.dto.UsuarioAdminDTOs;
import com.aerolineas.dto.VueloDTO;
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
    String usuarioTable = DB.table("USUARIO");
    final String sql = "SELECT ID_USUARIO, EMAIL, CONTRASENA, NOMBRES, APELLIDOS, HABILITADO, ID_ROL FROM " + usuarioTable + " WHERE LOWER(EMAIL) = LOWER(?)";
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
    String usuarioTable = DB.table("USUARIO");
    final String insert = "INSERT INTO " + usuarioTable + " (EMAIL, CONTRASENA, NOMBRES, APELLIDOS, HABILITADO, ID_ROL) VALUES (?, ?, ?, ?, 1, 3)";
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

  public Usuario createWithRole(String email, String passHash, String nombres, String apellidos, int idRol) {
    String usuarioTable = DB.table("USUARIO");
    final String insert = "INSERT INTO " + usuarioTable + " (EMAIL, CONTRASENA, NOMBRES, APELLIDOS, HABILITADO, ID_ROL) VALUES (?, ?, ?, ?, 1, ?)";
    try (Connection conn = DB.getConnection();
         PreparedStatement ps = conn.prepareStatement(insert)) {
      applyTimeout(ps);
      ps.setString(1, email);
      ps.setString(2, passHash);
      ps.setString(3, nombres);
      ps.setString(4, apellidos);
      ps.setInt(5, idRol);
      ps.executeUpdate();
      return findByEmail(email);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public Usuario findById(long id) throws Exception {
    String usuarioTable = DB.table("USUARIO");
    String sql = "SELECT ID_USUARIO, EMAIL, CONTRASENA, NOMBRES, APELLIDOS, HABILITADO, ID_ROL FROM " + usuarioTable + " WHERE ID_USUARIO = ?";
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
    String usuarioTable = DB.table("USUARIO");
    String rolTable = DB.table("ROL");
    String base = "SELECT u.ID_USUARIO, u.EMAIL, u.NOMBRES, u.APELLIDOS, u.ID_ROL, r.NOMBRE AS ROL_NOMBRE, u.HABILITADO FROM " + usuarioTable + " u JOIN " + rolTable + " r ON r.ID_ROL = u.ID_ROL WHERE 1=1 ";
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
    String usuarioTable = DB.table("USUARIO");
    String rolTable = DB.table("ROL");
    String pasajeroTable = DB.table("PASAJERO");
    String sql = "SELECT u.ID_USUARIO, u.EMAIL, u.NOMBRES, u.APELLIDOS, u.ID_ROL, r.NOMBRE AS ROL_NOMBRE, u.HABILITADO, p.FECHA_NACIMIENTO, p.ID_PAIS_DOCUMENTO, p.PASAPORTE FROM " + usuarioTable + " u JOIN " + rolTable + " r ON r.ID_ROL = u.ID_ROL LEFT JOIN " + pasajeroTable + " p ON p.ID_USUARIO = u.ID_USUARIO WHERE u.ID_USUARIO = ?";
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
        String usuarioTable = DB.table("USUARIO");
        String up = "UPDATE " + usuarioTable + " SET NOMBRES=?, APELLIDOS=?, ID_ROL=?, HABILITADO=?"
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
        String pasajeroTable = DB.table("PASAJERO");
        boolean exists;
        try (PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM " + pasajeroTable + " WHERE ID_USUARIO=?")) {
          ps.setLong(1, id);
          try (ResultSet rs = ps.executeQuery()) { exists = rs.next(); }
        }
        if (!exists) {
          String ins = "INSERT INTO " + pasajeroTable + " (FECHA_NACIMIENTO, ID_PAIS_DOCUMENTO, PASAPORTE, ID_USUARIO) VALUES (?,?,?,?)";
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
          String upd = "UPDATE " + pasajeroTable + " SET FECHA_NACIMIENTO=?, ID_PAIS_DOCUMENTO=?, PASAPORTE=? WHERE ID_USUARIO=?";
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
        String usuarioTable = DB.table("USUARIO");
        String up = "UPDATE " + usuarioTable + " SET NOMBRES=?, APELLIDOS=?"
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

        String pasajeroTable = DB.table("PASAJERO");
        boolean exists;
        try (PreparedStatement ps = cn.prepareStatement("SELECT 1 FROM " + pasajeroTable + " WHERE ID_USUARIO=?")) {
          ps.setLong(1, id);
          try (ResultSet rs = ps.executeQuery()) { exists = rs.next(); }
        }
        if (!exists) {
          String ins = "INSERT INTO " + pasajeroTable + " (FECHA_NACIMIENTO, ID_PAIS_DOCUMENTO, PASAPORTE, ID_USUARIO) VALUES (?,?,?,?)";
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
          String upd = "UPDATE " + pasajeroTable + " SET FECHA_NACIMIENTO=?, ID_PAIS_DOCUMENTO=?, PASAPORTE=? WHERE ID_USUARIO=?";
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

  public VueloDTO.View obtenerVuelo(long id) throws Exception {
    String vueloTable = DB.table("VUELO");
    String salidaClaseTable = DB.table("SALIDA_CLASE");
    String sql = "SELECT v.ID_VUELO, v.CODIGO, v.ID_RUTA, v.FECHA_SALIDA, v.FECHA_LLEGADA, v.ACTIVO, sc.ID_CLASE, sc.CUPO_TOTAL, sc.PRECIO FROM " + vueloTable + " v LEFT JOIN " + salidaClaseTable + " sc ON v.ID_VUELO = sc.ID_VUELO WHERE v.ID_VUELO = ?";

    VueloDTO.View view = null;
    List<VueloDTO.ClaseConfig> clases = new ArrayList<>();
    List<VueloDTO.EscalaView> escalas = new ArrayList<>();

    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          if (view == null) {
            view = new VueloDTO.View(
                rs.getLong("ID_VUELO"),
                rs.getString("CODIGO"),
                rs.getLong("ID_RUTA"),
                rs.getTimestamp("FECHA_SALIDA").toLocalDateTime(),
                rs.getTimestamp("FECHA_LLEGADA").toLocalDateTime(),
                rs.getInt("ACTIVO") == 1,
                clases,
                escalas
            );
          }
          int idClase = rs.getInt("ID_CLASE");
          if (!rs.wasNull()) {
            clases.add(new VueloDTO.ClaseConfig(
                idClase,
                rs.getInt("CUPO_TOTAL"),
                rs.getDouble("PRECIO")));
          }
        }
      }
    }
    if (view == null) return null;

    String vueloEscalaTable = DB.table("VUELO_ESCALA");
    String ciudadTable = DB.table("CIUDAD");
    String paisTable = DB.table("PAIS");
    String sqlEsc = "SELECT ve.ID_CIUDAD, c.NOMBRE AS CIUDAD, p.NOMBRE AS PAIS, ve.LLEGADA, ve.SALIDA FROM " + vueloEscalaTable + " ve JOIN " + ciudadTable + " c ON c.ID_CIUDAD = ve.ID_CIUDAD JOIN " + paisTable + " p ON p.ID_PAIS = c.ID_PAIS WHERE ve.ID_VUELO = ?";
    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sqlEsc)) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          escalas.add(new VueloDTO.EscalaView(
              rs.getLong("ID_CIUDAD"),
              rs.getString("CIUDAD"),
              rs.getString("PAIS"),
              rs.getTimestamp("LLEGADA").toLocalDateTime(),
              rs.getTimestamp("SALIDA").toLocalDateTime()
          ));
        }
      }
    }
    return view;
  }

  public void actualizarVueloAdmin(long idVuelo, VueloDTO.UpdateAdmin dto) throws Exception {
    try (Connection cn = DB.getConnection()) {
      cn.setAutoCommit(false);
      try {
        String vueloTable = DB.table("VUELO");
        String salidaClaseTable = DB.table("SALIDA_CLASE");
        String vueloEscalaTable = DB.table("VUELO_ESCALA");
        String up = "UPDATE " + vueloTable + " SET CODIGO=?, ID_RUTA=?, FECHA_SALIDA=?, FECHA_LLEGADA=?, ACTIVO=? WHERE ID_VUELO=?";
        try (PreparedStatement ps = cn.prepareStatement(up)) {
          ps.setString(1, dto.codigo().trim());
          ps.setLong(2, dto.idRuta());
          ps.setTimestamp(3, Timestamp.valueOf(dto.fechaSalida()));
          ps.setTimestamp(4, Timestamp.valueOf(dto.fechaLlegada()));
          ps.setInt(5, (dto.activo()!=null && dto.activo()) ? 1 : 0);
          ps.setLong(6, idVuelo);
          int n = ps.executeUpdate();
          if (n == 0) throw new SQLException("Vuelo no existe: " + idVuelo);
        }

        // Reemplazar clases
        try (PreparedStatement del = cn.prepareStatement("DELETE FROM " + salidaClaseTable + " WHERE ID_VUELO=?")) {
          del.setLong(1, idVuelo);
          del.executeUpdate();
        }
        if (dto.clases() != null) {
          for (VueloDTO.ClaseConfig c : dto.clases()) {
            try (PreparedStatement ins = cn.prepareStatement(
                "INSERT INTO " + salidaClaseTable + " (ID_VUELO, ID_CLASE, CUPO_TOTAL, PRECIO) VALUES (?,?,?,?)")) {
              ins.setLong(1, idVuelo);
              ins.setInt(2, c.idClase());
              ins.setInt(3, c.cupoTotal());
              ins.setDouble(4, c.precio());
              ins.executeUpdate();
            }
          }
        }

        // Reemplazar escala (0..1)
        try (PreparedStatement delE = cn.prepareStatement("DELETE FROM " + vueloEscalaTable + " WHERE ID_VUELO=?")) {
          delE.setLong(1, idVuelo);
          delE.executeUpdate();
        }
        if (dto.escalas()!=null && !dto.escalas().isEmpty()) {
          if (dto.escalas().size() > 1) throw new SQLException("Solo se permite 1 escala por vuelo");
          var e = dto.escalas().get(0);
          if (e.llegada().isAfter(e.salida()))
            throw new SQLException("La hora de SALIDA de la escala debe ser >= LLEGADA");
          try (PreparedStatement insE = cn.prepareStatement(
              "INSERT INTO " + vueloEscalaTable + " (ID_VUELO, ID_CIUDAD, LLEGADA, SALIDA) VALUES (?,?,?,?)")) {
            insE.setLong(1, idVuelo);
            insE.setLong(2, e.idCiudad());
            insE.setTimestamp(3, Timestamp.valueOf(e.llegada()));
            insE.setTimestamp(4, Timestamp.valueOf(e.salida()));
            insE.executeUpdate();
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
