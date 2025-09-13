package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.dto.VueloDTO;

import java.sql.*;
import java.util.*;

import static com.aerolineas.util.EstadosVuelo.*;

public class VueloDAO {

  public void crearVuelo(VueloDTO.Create dto) throws SQLException {
    try (Connection conn = DB.getConnection()) {
      conn.setAutoCommit(false);
      try {
        crearVueloTx(conn, dto);
        conn.commit();
      } catch (Exception e) {
        conn.rollback();
        if (e instanceof SQLException) throw (SQLException) e;
        throw new SQLException("Error al crear vuelo", e);
      } finally {
        conn.setAutoCommit(true);
      }
    }
  }

  public long crearVueloReturnId(VueloDTO.Create dto) throws Exception {
    try (Connection conn = DB.getConnection()) {
      conn.setAutoCommit(false);
      try {
        long id = crearVueloTx(conn, dto);
        conn.commit();
        return id;
      } catch (Exception e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(true);
      }
    }
  }

  public void vincularPareja(long idIda, long idRegreso) throws SQLException {
    if (idIda <= 0 || idRegreso <= 0) throw new SQLException("IDs de vuelo inválidos");
    if (idIda == idRegreso) throw new SQLException("Un vuelo no puede ser pareja de sí mismo");

    String sel = "SELECT ID_VUELO, ID_VUELO_PAREJA FROM VUELO WHERE ID_VUELO=?";
    String upd = "UPDATE VUELO SET ID_VUELO_PAREJA=? WHERE ID_VUELO=?";

    try (Connection conn = DB.getConnection()) {
      conn.setAutoCommit(false);
      try {
        Long parejaA, parejaB;

        try (PreparedStatement ps = conn.prepareStatement(sel)) {
          ps.setLong(1, idIda);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new SQLException("Vuelo ida no existe: " + idIda);
            long pair = rs.getLong("ID_VUELO_PAREJA");
            parejaA = rs.wasNull() ? null : pair;
          }
        }
        try (PreparedStatement ps = conn.prepareStatement(sel)) {
          ps.setLong(1, idRegreso);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new SQLException("Vuelo regreso no existe: " + idRegreso);
            long pair = rs.getLong("ID_VUELO_PAREJA");
            parejaB = rs.wasNull() ? null : pair;
          }
        }

        if (parejaA != null && !parejaA.equals(idRegreso))
          throw new SQLException("El vuelo " + idIda + " ya está enlazado con " + parejaA);
        if (parejaB != null && !parejaB.equals(idIda))
          throw new SQLException("El vuelo " + idRegreso + " ya está enlazado con " + parejaB);

        try (PreparedStatement ps = conn.prepareStatement(upd)) {
          ps.setLong(1, idRegreso);
          ps.setLong(2, idIda);
          ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(upd)) {
          ps.setLong(1, idIda);
          ps.setLong(2, idRegreso);
          ps.executeUpdate();
        }

        conn.commit();
      } catch (Exception e) {
        conn.rollback();
        if (e instanceof SQLException) throw (SQLException) e;
        throw new SQLException("Error al vincular vuelos", e);
      } finally {
        conn.setAutoCommit(true);
      }
    }
  }

  public VueloDTO.View obtenerVuelo(long id) throws Exception {
    String sql = """
        SELECT v.ID_VUELO,
               v.CODIGO,
               v.ID_RUTA,
               co.NOMBRE AS ORIGEN,
               cd.NOMBRE AS DESTINO,
               v.FECHA_SALIDA,
               v.FECHA_LLEGADA,
               NVL(v.ACTIVO,1) AS ACTIVO,
               sc.ID_CLASE,
               sc.CUPO_TOTAL,
               sc.PRECIO
        FROM VUELO v
        JOIN RUTA r    ON r.ID_RUTA = v.ID_RUTA
        JOIN CIUDAD co ON co.ID_CIUDAD = r.ID_CIUDAD_ORIGEN
        JOIN CIUDAD cd ON cd.ID_CIUDAD = r.ID_CIUDAD_DESTINO
        LEFT JOIN SALIDA_CLASE sc ON v.ID_VUELO = sc.ID_VUELO
        WHERE v.ID_VUELO = ?
        """;

    VueloDTO.View view = null;
    List<VueloDTO.ClaseConfig> clases = new ArrayList<>();
    List<VueloDTO.EscalaView>  escalas = new ArrayList<>();

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
                rs.getString("ORIGEN"),
                rs.getString("DESTINO"),
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
                rs.getDouble("PRECIO")
            ));
          }
        }
      }
    }

    if (view == null) return null;

    String sqlEsc = """
        SELECT ve.ID_CIUDAD, c.NOMBRE AS CIUDAD, p.NOMBRE AS PAIS,
               ve.LLEGADA, ve.SALIDA
        FROM VUELO_ESCALA ve
        JOIN CIUDAD c ON c.ID_CIUDAD = ve.ID_CIUDAD
        JOIN PAIS   p ON p.ID_PAIS   = c.ID_PAIS
        WHERE ve.ID_VUELO = ?
        """;
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

  public List<VueloDTO.View> listarVuelos(boolean soloActivos) throws SQLException {
    String sql =
        """
        SELECT v.ID_VUELO,
               v.CODIGO,
               v.ID_RUTA,
               co.NOMBRE AS ORIGEN,
               cd.NOMBRE AS DESTINO,
               v.FECHA_SALIDA,
               v.FECHA_LLEGADA,
               NVL(v.ACTIVO,1) AS ACTIVO,
               sc.ID_CLASE,
               sc.CUPO_TOTAL,
               sc.PRECIO
        FROM VUELO v
        JOIN RUTA r    ON r.ID_RUTA = v.ID_RUTA
        JOIN CIUDAD co ON co.ID_CIUDAD = r.ID_CIUDAD_ORIGEN
        JOIN CIUDAD cd ON cd.ID_CIUDAD = r.ID_CIUDAD_DESTINO
        JOIN SALIDA_CLASE sc ON v.ID_VUELO = sc.ID_VUELO
        """ +
        (soloActivos ? " WHERE NVL(v.ACTIVO,1)=1 " : "") +
        " ORDER BY v.ID_VUELO";

    Map<Long, VueloDTO.View> vuelos = new LinkedHashMap<>();

    try (Connection conn = DB.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

      while (rs.next()) {
        long idVuelo = rs.getLong("ID_VUELO");
        VueloDTO.View view = vuelos.get(idVuelo);

        VueloDTO.ClaseConfig clase = new VueloDTO.ClaseConfig(
            rs.getInt("ID_CLASE"),
            rs.getInt("CUPO_TOTAL"),
            rs.getDouble("PRECIO")
        );

        if (view == null) {
          view = new VueloDTO.View(
              idVuelo,
              rs.getString("CODIGO"),
              rs.getLong("ID_RUTA"),
              rs.getString("ORIGEN"),
              rs.getString("DESTINO"),
              rs.getTimestamp("FECHA_SALIDA").toLocalDateTime(),
              rs.getTimestamp("FECHA_LLEGADA").toLocalDateTime(),
              rs.getInt("ACTIVO") == 1,
              new ArrayList<>(List.of(clase)),
              new ArrayList<>()
          );
          vuelos.put(idVuelo, view);
        } else {
          view.clases().add(clase);
        }
      }
    }

    if (vuelos.isEmpty()) return List.of();

    StringBuilder placeholders = new StringBuilder();
    int size = vuelos.size();
    for (int i = 0; i < size; i++) {
      if (i > 0) placeholders.append(',');
      placeholders.append('?');
    }

    String sqlEsc =
        """
        SELECT ve.ID_VUELO, ve.ID_CIUDAD, c.NOMBRE AS CIUDAD, p.NOMBRE AS PAIS,
               ve.LLEGADA, ve.SALIDA
        FROM VUELO_ESCALA ve
        JOIN CIUDAD c ON c.ID_CIUDAD = ve.ID_CIUDAD
        JOIN PAIS   p ON p.ID_PAIS   = c.ID_PAIS
        WHERE ve.ID_VUELO IN (%s)
        """.formatted(placeholders.toString());

    try (Connection conn = DB.getConnection();
         PreparedStatement psE = conn.prepareStatement(sqlEsc)) {
      int idx = 1;
      for (Long id : vuelos.keySet()) psE.setLong(idx++, id);

      try (ResultSet rsE = psE.executeQuery()) {
        while (rsE.next()) {
          VueloDTO.View view = vuelos.get(rsE.getLong("ID_VUELO"));
          view.escalas().add(new VueloDTO.EscalaView(
              rsE.getLong("ID_CIUDAD"),
              rsE.getString("CIUDAD"),
              rsE.getString("PAIS"),
              rsE.getTimestamp("LLEGADA").toLocalDateTime(),
              rsE.getTimestamp("SALIDA").toLocalDateTime()
          ));
        }
      }
    }

    return new ArrayList<>(vuelos.values());
  }

  public List<VueloDTO.View> listarVuelos() throws SQLException {
    return listarVuelos(false);
  }

  public void actualizarEstado(long idVuelo, int idEstado) throws SQLException {
    if (!VALID.contains(idEstado)) {
      throw new SQLException("Estado inválido: " + idEstado);
    }

    String sqlSel = "SELECT ID_ESTADO FROM VUELO WHERE ID_VUELO = ?";
    String sqlUpd = "UPDATE VUELO SET ID_ESTADO = ? WHERE ID_VUELO = ?";

    try (Connection conn = DB.getConnection()) {
      conn.setAutoCommit(false);
      try {
        int estadoActual;

        try (PreparedStatement ps = conn.prepareStatement(sqlSel)) {
          ps.setLong(1, idVuelo);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new SQLException("Vuelo no existe: " + idVuelo);
            estadoActual = rs.getInt(1);
          }
        }

        if (estadoActual == CANCELADO && idEstado != CANCELADO) {
          throw new SQLException("El vuelo ya está cancelado; no se permite cambiar a otro estado.");
        }
        if (estadoActual == idEstado) {
          conn.commit();
          return;
        }

        try (PreparedStatement ps = conn.prepareStatement(sqlUpd)) {
          ps.setInt(1, idEstado);
          ps.setLong(2, idVuelo);
          ps.executeUpdate();
        }

        conn.commit();
      } catch (Exception e) {
        conn.rollback();
        if (e instanceof SQLException) throw (SQLException) e;
        throw new SQLException("Error al actualizar estado", e);
      }
    }
  }

  public void actualizarVueloAdmin(long idVuelo, VueloDTO.UpdateAdmin dto) throws Exception {
    try (Connection cn = DB.getConnection()) {
      cn.setAutoCommit(false);
      try {
        String up = "UPDATE VUELO SET CODIGO=?, ID_RUTA=?, FECHA_SALIDA=?, FECHA_LLEGADA=?"
                  + (dto.activo() != null ? ", ACTIVO=?" : "")
                  + " WHERE ID_VUELO=?";
        try (PreparedStatement ps = cn.prepareStatement(up)) {
          int i = 1;
          ps.setString(i++, dto.codigo().trim());
          ps.setLong(i++, dto.idRuta());
          ps.setTimestamp(i++, Timestamp.valueOf(dto.fechaSalida()));
          ps.setTimestamp(i++, Timestamp.valueOf(dto.fechaLlegada()));
          if (dto.activo() != null) {
            ps.setInt(i++, dto.activo() ? 1 : 0); // 1 activo, 0 inactivo
          }
          ps.setLong(i++, idVuelo);
          int n = ps.executeUpdate();
          if (n == 0) throw new SQLException("Vuelo no existe: " + idVuelo);
        }

        try (PreparedStatement del = cn.prepareStatement("DELETE FROM SALIDA_CLASE WHERE ID_VUELO=?")) {
          del.setLong(1, idVuelo);
          del.executeUpdate();
        }
        if (dto.clases() != null && !dto.clases().isEmpty()) {
          String ins = "INSERT INTO SALIDA_CLASE (ID_VUELO, ID_CLASE, CUPO_TOTAL, PRECIO) VALUES (?,?,?,?)";
          try (PreparedStatement ps = cn.prepareStatement(ins)) {
            for (VueloDTO.ClaseConfig c : dto.clases()) {
              ps.setLong(1, idVuelo);
              ps.setInt (2, c.idClase());
              ps.setInt (3, c.cupoTotal());
              ps.setDouble(4, c.precio());
              ps.addBatch();
            }
            ps.executeBatch();
          }
        } else {
          throw new SQLException("Debe indicar al menos una clase");
        }

        if (dto.escalas() != null) {
          try (PreparedStatement delE = cn.prepareStatement("DELETE FROM VUELO_ESCALA WHERE ID_VUELO=?")) {
            delE.setLong(1, idVuelo);
            delE.executeUpdate();
          }
          if (!dto.escalas().isEmpty()) {
            if (dto.escalas().size() > 1) throw new SQLException("Solo se permite 1 escala por vuelo");
            var e = dto.escalas().get(0);
            if (e.llegada().isAfter(e.salida())) {
              throw new SQLException("La hora de SALIDA de la escala debe ser >= LLEGADA");
            }
            String insE = "INSERT INTO VUELO_ESCALA (ID_VUELO, ID_CIUDAD, LLEGADA, SALIDA) VALUES (?,?,?,?)";
            try (PreparedStatement psE = cn.prepareStatement(insE)) {
              psE.setLong(1, idVuelo);
              psE.setLong(2, e.idCiudad());
              psE.setTimestamp(3, Timestamp.valueOf(e.llegada()));
              psE.setTimestamp(4, Timestamp.valueOf(e.salida()));
              psE.executeUpdate();
            }
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

  private long crearVueloTx(Connection conn, VueloDTO.Create dto) throws Exception {
    String sqlVuelo = "INSERT INTO VUELO (CODIGO, ID_RUTA, FECHA_SALIDA, FECHA_LLEGADA) VALUES (?,?,?,?)";

    try (PreparedStatement ps = conn.prepareStatement(sqlVuelo, new String[]{"ID_VUELO"})) {
      ps.setString(1, dto.codigo());
      ps.setLong(2, dto.idRuta());
      ps.setTimestamp(3, Timestamp.valueOf(dto.fechaSalida()));
      ps.setTimestamp(4, Timestamp.valueOf(dto.fechaLlegada()));
      ps.executeUpdate();

      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (!rs.next()) throw new SQLException("No se generó ID_VUELO");
        long idVuelo = rs.getLong(1);

        if (dto.clases() != null) {
          for (VueloDTO.ClaseConfig clase : dto.clases()) {
            try (PreparedStatement ps2 = conn.prepareStatement(
                "INSERT INTO SALIDA_CLASE (ID_VUELO, ID_CLASE, CUPO_TOTAL, PRECIO) VALUES (?,?,?,?)")) {
              ps2.setLong(1, idVuelo);
              ps2.setInt(2, clase.idClase());
              ps2.setInt(3, clase.cupoTotal());
              ps2.setDouble(4, clase.precio());
              ps2.executeUpdate();
            }
          }
        }

        if (dto.escalas() != null && !dto.escalas().isEmpty()) {
          if (dto.escalas().size() > 1) throw new SQLException("Solo se permite 1 escala por vuelo");
          var e = dto.escalas().get(0);
          if (e.llegada().isAfter(e.salida()))
            throw new SQLException("La hora de SALIDA de la escala debe ser >= LLEGADA");

          try (PreparedStatement psE = conn.prepareStatement(
              "INSERT INTO VUELO_ESCALA (ID_VUELO, ID_CIUDAD, LLEGADA, SALIDA) VALUES (?,?,?,?)")) {
            psE.setLong(1, idVuelo);
            psE.setLong(2, e.idCiudad());
            psE.setTimestamp(3, Timestamp.valueOf(e.llegada()));
            psE.setTimestamp(4, Timestamp.valueOf(e.salida()));
            psE.executeUpdate();
          }
        }

        return idVuelo;
      }
    }
  }
}
