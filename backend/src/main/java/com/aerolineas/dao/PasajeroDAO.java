package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.model.Pasajero;

import java.sql.*;
import java.time.LocalDate;

public class PasajeroDAO {

  public Pasajero findByUsuario(long idUsuario) throws Exception {
    String pasajeroTable = DB.table("PASAJERO");
    String sql = "SELECT ID_PASAJERO, FECHA_NACIMIENTO, ID_PAIS_DOCUMENTO, PASAPORTE, ID_USUARIO FROM " + pasajeroTable + " WHERE ID_USUARIO = ?";
    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setLong(1, idUsuario);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? map(rs) : null;
      }
    }
  }

  public Pasajero upsert(long idUsuario, LocalDate fechaNac, Long idPais, String pasaporte) throws Exception {
    Pasajero cur = findByUsuario(idUsuario);
    if (cur == null) {
      String pasajeroTable = DB.table("PASAJERO");
      String ins = "INSERT INTO " + pasajeroTable + " (FECHA_NACIMIENTO, ID_PAIS_DOCUMENTO, PASAPORTE, ID_USUARIO) VALUES (?, ?, ?, ?)";
      try (Connection cn = DB.getConnection();
           PreparedStatement ps = cn.prepareStatement(ins)) {
        if (fechaNac != null) ps.setDate(1, Date.valueOf(fechaNac)); else ps.setNull(1, Types.DATE);
        if (idPais != null) ps.setLong(2, idPais); else ps.setNull(2, Types.NUMERIC);
        ps.setString(3, pasaporte);
        ps.setLong(4, idUsuario);
        ps.executeUpdate();
      }
    } else {
      String pasajeroTable = DB.table("PASAJERO");
      String upd = "UPDATE " + pasajeroTable + " SET FECHA_NACIMIENTO=?, ID_PAIS_DOCUMENTO=?, PASAPORTE=? WHERE ID_USUARIO=?";
      try (Connection cn = DB.getConnection();
           PreparedStatement ps = cn.prepareStatement(upd)) {
        if (fechaNac != null) ps.setDate(1, Date.valueOf(fechaNac)); else ps.setNull(1, Types.DATE);
        if (idPais != null) ps.setLong(2, idPais); else ps.setNull(2, Types.NUMERIC);
        ps.setString(3, pasaporte);
        ps.setLong(4, idUsuario);
        ps.executeUpdate();
      }
    }
    return findByUsuario(idUsuario);
  }

  private Pasajero map(ResultSet rs) throws SQLException {
    Pasajero p = new Pasajero();
    p.setIdPasajero(rs.getLong("ID_PASAJERO"));
    Date d = rs.getDate("FECHA_NACIMIENTO");
    p.setFechaNacimiento(d != null ? d.toLocalDate() : null);
    long idPais = rs.getLong("ID_PAIS_DOCUMENTO");
    p.setIdPaisDocumento(rs.wasNull() ? null : idPais);
    p.setPasaporte(rs.getString("PASAPORTE"));
    p.setIdUsuario(rs.getLong("ID_USUARIO"));
    return p;
  }
}
