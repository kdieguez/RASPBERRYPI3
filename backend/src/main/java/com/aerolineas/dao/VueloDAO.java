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
    String chkOtroApuntaRegreso = "SELECT COUNT(*) FROM VUELO WHERE ID_VUELO_PAREJA = ? AND ID_VUELO <> ?";
    String setParejaIda = "UPDATE VUELO SET ID_VUELO_PAREJA = ? WHERE ID_VUELO = ?";
    String clearRegreso = "UPDATE VUELO SET ID_VUELO_PAREJA = NULL WHERE ID_VUELO = ?";

    try (Connection conn = DB.getConnection()) {
      conn.setAutoCommit(false);
      try {
      
        try (PreparedStatement ps = conn.prepareStatement(sel)) {
          ps.setLong(1, idIda);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new SQLException("Vuelo ida no existe: " + idIda);
          }
        }
        try (PreparedStatement ps = conn.prepareStatement(sel)) {
          ps.setLong(1, idRegreso);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new SQLException("Vuelo regreso no existe: " + idRegreso);
          }
        }

        try (PreparedStatement ps = conn.prepareStatement(chkOtroApuntaRegreso)) {
          ps.setLong(1, idRegreso);
          ps.setLong(2, idIda);
          try (ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
              throw new SQLException("El vuelo de regreso " + idRegreso + " ya está enlazado por otra ida.");
            }
          }
        }

        try (PreparedStatement ps = conn.prepareStatement(clearRegreso)) {
          ps.setLong(1, idRegreso);
          ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement(setParejaIda)) {
          ps.setLong(1, idRegreso);
          ps.setLong(2, idIda);
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

  public void desvincularPareja(long idVuelo) throws SQLException {
    if (idVuelo <= 0) throw new SQLException("ID de vuelo inválido");

    String sqlExists = "SELECT 1 FROM VUELO WHERE ID_VUELO = ?";
    String sqlClearSelf = "UPDATE VUELO SET ID_VUELO_PAREJA = NULL WHERE ID_VUELO = ?";
    String sqlClearPointingTo = "UPDATE VUELO SET ID_VUELO_PAREJA = NULL WHERE ID_VUELO_PAREJA = ?";

    try (Connection conn = DB.getConnection()) {
      conn.setAutoCommit(false);
      try {
        try (PreparedStatement ps = conn.prepareStatement(sqlExists)) {
          ps.setLong(1, idVuelo);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new SQLException("Vuelo no encontrado: " + idVuelo);
          }
        }

        try (PreparedStatement ps = conn.prepareStatement(sqlClearSelf)) {
          ps.setLong(1, idVuelo);
          ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement(sqlClearPointingTo)) {
          ps.setLong(1, idVuelo);
          ps.executeUpdate();
        }

        conn.commit();
      } catch (Exception e) {
        conn.rollback();
        if (e instanceof SQLException) throw (SQLException) e;
        throw new SQLException("Error al desvincular vuelo", e);
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
               po.NOMBRE AS ORIGEN_PAIS,
               pd.NOMBRE AS DESTINO_PAIS,
               v.FECHA_SALIDA,
               v.FECHA_LLEGADA,
               NVL(v.ACTIVO,1) AS ACTIVO,
               v.ID_ESTADO,
               e.Estado AS ESTADO,
               v.ID_VUELO_PAREJA AS PAREJA_ID,
               vp.CODIGO AS PAREJA_CODIGO,
               sc.ID_CLASE,
               sc.CUPO_TOTAL,
               sc.PRECIO
        FROM VUELO v
        JOIN RUTA r    ON r.ID_RUTA = v.ID_RUTA
        JOIN CIUDAD co ON co.ID_CIUDAD = r.ID_CIUDAD_ORIGEN
        JOIN CIUDAD cd ON cd.ID_CIUDAD = r.ID_CIUDAD_DESTINO
        JOIN PAIS po ON po.ID_PAIS = co.ID_PAIS
        JOIN PAIS pd ON pd.ID_PAIS = cd.ID_PAIS
        JOIN ESTADOS e ON e.ID_ESTADO = v.ID_ESTADO
        LEFT JOIN VUELO vp ON vp.ID_VUELO = v.ID_VUELO_PAREJA
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
            Integer idEstado = rs.getObject("ID_ESTADO") == null ? null : rs.getInt("ID_ESTADO");
            Long parejaId = rs.getObject("PAREJA_ID") == null ? null : rs.getLong("PAREJA_ID");

            view = new VueloDTO.View(
                rs.getLong("ID_VUELO"),
                rs.getString("CODIGO"),
                rs.getLong("ID_RUTA"),
                rs.getString("ORIGEN"),
                rs.getString("DESTINO"),
                rs.getTimestamp("FECHA_SALIDA").toLocalDateTime(),
                rs.getTimestamp("FECHA_LLEGADA").toLocalDateTime(),
                rs.getInt("ACTIVO") == 1,
                idEstado,
                rs.getString("ESTADO"),
                clases,
                escalas,
                parejaId,
                rs.getString("ORIGEN_PAIS"),
                rs.getString("DESTINO_PAIS")
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

  public VueloDTO.View obtenerVueloPublic(long id) throws Exception {
    String sql = """
        SELECT v.ID_VUELO,
               v.CODIGO,
               v.ID_RUTA,
               co.NOMBRE AS ORIGEN,
               cd.NOMBRE AS DESTINO,
               po.NOMBRE AS ORIGEN_PAIS,
               pd.NOMBRE AS DESTINO_PAIS,
               v.FECHA_SALIDA,
               v.FECHA_LLEGADA,
               NVL(v.ACTIVO,1) AS ACTIVO,
               v.ID_ESTADO,
               e.Estado AS ESTADO,
               v.ID_VUELO_PAREJA AS PAREJA_ID,
               vp.CODIGO AS PAREJA_CODIGO,
               sc.ID_CLASE,
               sc.CUPO_TOTAL,
               sc.PRECIO,
               ( sc.CUPO_TOTAL
                 - NVL((SELECT COUNT(*) 
                         FROM RESERVA_ITEM ri 
                         JOIN RESERVA r ON r.ID_RESERVA = ri.ID_RESERVA
                        WHERE ri.ID_VUELO = v.ID_VUELO 
                          AND ri.ID_CLASE = sc.ID_CLASE
                          AND r.ID_ESTADO = 1), 0)
                 - NVL((SELECT SUM(ci.CANTIDAD) 
                         FROM CARRITO_ITEM ci
                        WHERE ci.ID_VUELO = v.ID_VUELO
                          AND ci.ID_CLASE = sc.ID_CLASE), 0)
               ) AS DISPONIBLE
        FROM VUELO v
        JOIN RUTA r    ON r.ID_RUTA = v.ID_RUTA
        JOIN CIUDAD co ON co.ID_CIUDAD = r.ID_CIUDAD_ORIGEN
        JOIN CIUDAD cd ON cd.ID_CIUDAD = r.ID_CIUDAD_DESTINO
        JOIN PAIS po   ON po.ID_PAIS   = co.ID_PAIS
        JOIN PAIS pd   ON pd.ID_PAIS   = cd.ID_PAIS
        JOIN ESTADOS e ON e.ID_ESTADO  = v.ID_ESTADO
        LEFT JOIN VUELO vp ON vp.ID_VUELO = v.ID_VUELO_PAREJA
        LEFT JOIN SALIDA_CLASE sc ON v.ID_VUELO = sc.ID_VUELO
        WHERE v.ID_VUELO = ?
          AND NVL(v.ACTIVO,1) = 1
          AND UPPER(e.Estado) <> 'CANCELADO'
        """;

    VueloDTO.View view = null;
    List<VueloDTO.ClaseConfig> clases = new ArrayList<>();
    List<VueloDTO.EscalaView>  escalas = new ArrayList<>();

    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          int disp = rs.getInt("DISPONIBLE");
          if (rs.wasNull() || disp <= 0) {
            continue;
          }
          if (view == null) {
            Integer idEstado = rs.getObject("ID_ESTADO") == null ? null : rs.getInt("ID_ESTADO");
            Long parejaId = rs.getObject("PAREJA_ID") == null ? null : rs.getLong("PAREJA_ID");

            view = new VueloDTO.View(
                rs.getLong("ID_VUELO"),
                rs.getString("CODIGO"),
                rs.getLong("ID_RUTA"),
                rs.getString("ORIGEN"),
                rs.getString("DESTINO"),
                rs.getTimestamp("FECHA_SALIDA").toLocalDateTime(),
                rs.getTimestamp("FECHA_LLEGADA").toLocalDateTime(),
                true,
                idEstado,
                rs.getString("ESTADO"),
                clases,
                escalas,
                parejaId,
                rs.getString("ORIGEN_PAIS"),
                rs.getString("DESTINO_PAIS")
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

    if (view == null || clases.isEmpty()) return null;

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

  public List<VueloDTO.View> listarVuelosPublic() throws SQLException {
    String sql = """
        SELECT v.ID_VUELO,
               v.CODIGO,
               v.ID_RUTA,
               co.NOMBRE AS ORIGEN,
               cd.NOMBRE AS DESTINO,
               po.NOMBRE AS ORIGEN_PAIS,
               pd.NOMBRE AS DESTINO_PAIS,
               v.FECHA_SALIDA,
               v.FECHA_LLEGADA,
               NVL(v.ACTIVO,1) AS ACTIVO,
               v.ID_ESTADO,
               e.Estado AS ESTADO,
               v.ID_VUELO_PAREJA AS PAREJA,
               sc.ID_CLASE,
               sc.CUPO_TOTAL,
               sc.PRECIO,
               ( sc.CUPO_TOTAL
                 - NVL((SELECT COUNT(*) 
                         FROM RESERVA_ITEM ri 
                         JOIN RESERVA r ON r.ID_RESERVA = ri.ID_RESERVA
                        WHERE ri.ID_VUELO = v.ID_VUELO 
                          AND ri.ID_CLASE = sc.ID_CLASE
                          AND r.ID_ESTADO = 1), 0)
                 - NVL((SELECT SUM(ci.CANTIDAD) 
                         FROM CARRITO_ITEM ci
                        WHERE ci.ID_VUELO = v.ID_VUELO
                          AND ci.ID_CLASE = sc.ID_CLASE), 0)
               ) AS DISPONIBLE
        FROM VUELO v
        JOIN RUTA r    ON r.ID_RUTA = v.ID_RUTA
        JOIN CIUDAD co ON co.ID_CIUDAD = r.ID_CIUDAD_ORIGEN
        JOIN CIUDAD cd ON cd.ID_CIUDAD = r.ID_CIUDAD_DESTINO
        JOIN PAIS po   ON po.ID_PAIS   = co.ID_PAIS
        JOIN PAIS pd   ON pd.ID_PAIS   = cd.ID_PAIS
        JOIN ESTADOS e ON e.ID_ESTADO  = v.ID_ESTADO
        JOIN SALIDA_CLASE sc ON v.ID_VUELO = sc.ID_VUELO
        WHERE NVL(v.ACTIVO,1)=1
          AND UPPER(e.Estado) <> 'CANCELADO'
        ORDER BY v.ID_VUELO
        """;

    Map<Long, VueloDTO.View> vuelos = new LinkedHashMap<>();

    try (Connection conn = DB.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

      while (rs.next()) {
        int disp = rs.getInt("DISPONIBLE");
        if (rs.wasNull() || disp <= 0) continue;

        long idVuelo = rs.getLong("ID_VUELO");
        VueloDTO.View view = vuelos.get(idVuelo);

        VueloDTO.ClaseConfig clase = new VueloDTO.ClaseConfig(
            rs.getInt("ID_CLASE"),
            rs.getInt("CUPO_TOTAL"),
            rs.getDouble("PRECIO")
        );

        if (view == null) {
          Integer idEstado = rs.getObject("ID_ESTADO") == null ? null : rs.getInt("ID_ESTADO");
          Long pareja = (rs.getObject("PAREJA") == null) ? null : rs.getLong("PAREJA");
          view = new VueloDTO.View(
              idVuelo,
              rs.getString("CODIGO"),
              rs.getLong("ID_RUTA"),
              rs.getString("ORIGEN"),
              rs.getString("DESTINO"),
              rs.getTimestamp("FECHA_SALIDA").toLocalDateTime(),
              rs.getTimestamp("FECHA_LLEGADA").toLocalDateTime(),
              true,
              idEstado,
              rs.getString("ESTADO"),
              new ArrayList<>(List.of(clase)),
              new ArrayList<>(),
              pareja,
              rs.getString("ORIGEN_PAIS"),
              rs.getString("DESTINO_PAIS")
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

    String sqlEsc = """
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

  public VueloDTO.ViewAdmin obtenerVueloAdmin(long id) throws Exception {
    String sql = """
        SELECT v.ID_VUELO,
               v.CODIGO,
               v.ID_RUTA,
               co.NOMBRE AS ORIGEN,
               cd.NOMBRE AS DESTINO,
               po.NOMBRE AS ORIGEN_PAIS,
               pd.NOMBRE AS DESTINO_PAIS,
               v.FECHA_SALIDA,
               v.FECHA_LLEGADA,
               NVL(v.ACTIVO,1) AS ACTIVO,
               v.ID_ESTADO,
               e.Estado AS ESTADO,
               v.ID_VUELO_PAREJA AS PAREJA_ID,
               vp.CODIGO AS PAREJA_CODIGO,
               sc.ID_CLASE,
               sc.CUPO_TOTAL,
               sc.PRECIO
        FROM VUELO v
        JOIN RUTA r    ON r.ID_RUTA = v.ID_RUTA
        JOIN CIUDAD co ON co.ID_CIUDAD = r.ID_CIUDAD_ORIGEN
        JOIN CIUDAD cd ON cd.ID_CIUDAD = r.ID_CIUDAD_DESTINO
        JOIN PAIS po ON po.ID_PAIS = co.ID_PAIS
        JOIN PAIS pd ON pd.ID_PAIS = cd.ID_PAIS
        JOIN ESTADOS e  ON e.ID_ESTADO = v.ID_ESTADO
        LEFT JOIN VUELO vp ON vp.ID_VUELO = v.ID_VUELO_PAREJA
        LEFT JOIN SALIDA_CLASE sc ON sc.ID_VUELO = v.ID_VUELO
        WHERE v.ID_VUELO = ?
        """;

    VueloDTO.ViewAdmin view = null;
    List<VueloDTO.ClaseConfig> clases = new ArrayList<>();
    List<VueloDTO.EscalaView>  escalas = new ArrayList<>();

    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          if (view == null) {
            Integer idEstado = rs.getObject("ID_ESTADO") == null ? null : rs.getInt("ID_ESTADO");
            view = new VueloDTO.ViewAdmin(
                rs.getLong("ID_VUELO"),
                rs.getString("CODIGO"),
                rs.getLong("ID_RUTA"),
                rs.getString("ORIGEN"),
                rs.getString("DESTINO"),
                rs.getTimestamp("FECHA_SALIDA").toLocalDateTime(),
                rs.getTimestamp("FECHA_LLEGADA").toLocalDateTime(),
                rs.getInt("ACTIVO") == 1,
                idEstado,
                rs.getString("ESTADO"),
                clases,
                escalas,
                rs.getObject("PAREJA_ID") == null ? null : rs.getLong("PAREJA_ID"),
                rs.getString("PAREJA_CODIGO"),
                rs.getString("ORIGEN_PAIS"),
                rs.getString("DESTINO_PAIS")
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
               po.NOMBRE AS ORIGEN_PAIS,
               pd.NOMBRE AS DESTINO_PAIS,
               v.FECHA_SALIDA,
               v.FECHA_LLEGADA,
               NVL(v.ACTIVO,1) AS ACTIVO,
               v.ID_ESTADO,
               e.Estado AS ESTADO,
               v.ID_VUELO_PAREJA AS PAREJA,
               sc.ID_CLASE,
               sc.CUPO_TOTAL,
               sc.PRECIO
        FROM VUELO v
        JOIN RUTA r    ON r.ID_RUTA = v.ID_RUTA
        JOIN CIUDAD co ON co.ID_CIUDAD = r.ID_CIUDAD_ORIGEN
        JOIN CIUDAD cd ON cd.ID_CIUDAD = r.ID_CIUDAD_DESTINO
        JOIN PAIS po ON po.ID_PAIS = co.ID_PAIS
        JOIN PAIS pd ON pd.ID_PAIS = cd.ID_PAIS
        JOIN ESTADOS e  ON e.ID_ESTADO = v.ID_ESTADO
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
          Integer idEstado = rs.getObject("ID_ESTADO") == null ? null : rs.getInt("ID_ESTADO");
          Long pareja = (rs.getObject("PAREJA") == null) ? null : rs.getLong("PAREJA");
          view = new VueloDTO.View(
              idVuelo,
              rs.getString("CODIGO"),
              rs.getLong("ID_RUTA"),
              rs.getString("ORIGEN"),
              rs.getString("DESTINO"),
              rs.getTimestamp("FECHA_SALIDA").toLocalDateTime(),
              rs.getTimestamp("FECHA_LLEGADA").toLocalDateTime(),
              rs.getInt("ACTIVO") == 1,
              idEstado,
              rs.getString("ESTADO"),
              new ArrayList<>(List.of(clase)),
              new ArrayList<>(),
              pareja,
              rs.getString("ORIGEN_PAIS"),
              rs.getString("DESTINO_PAIS")
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

  public void actualizarEstado(long idVuelo, int idEstado, String motivo) throws SQLException {
    if (!VALID.contains(idEstado)) {
      throw new SQLException("Estado inválido: " + idEstado);
    }

    String sqlSel = "SELECT ID_ESTADO FROM VUELO WHERE ID_VUELO = ?";
    String sqlUpd = "UPDATE VUELO SET ID_ESTADO = ? WHERE ID_VUELO = ?";
    String sqlInsMotivo = "INSERT INTO VUELO_MOTIVO (ID_VUELO, TIPO, MOTIVO) VALUES (?,?,?)";

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

        if (idEstado == CANCELADO) {
          if (motivo == null || motivo.isBlank()) {
            throw new SQLException("Debe proporcionar el motivo de cancelación.");
          }
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

        if (idEstado == CANCELADO) {
          try (PreparedStatement ps = conn.prepareStatement(sqlInsMotivo)) {
            ps.setLong(1, idVuelo);
            ps.setString(2, "CANCELACION");
            ps.setString(3, motivo.trim());
            ps.executeUpdate();
          }
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
        int estadoActual = -1;
        try (PreparedStatement psChk = cn.prepareStatement(
            "SELECT ID_ESTADO FROM VUELO WHERE ID_VUELO=?")) {
          psChk.setLong(1, idVuelo);
          try (ResultSet rs = psChk.executeQuery()) {
            if (!rs.next()) throw new SQLException("Vuelo no existe: " + idVuelo);
            estadoActual = rs.getInt(1);
          }
        }
        if (estadoActual == CANCELADO) {
          throw new SQLException("Vuelo cancelado: no se permite modificar.");
        }

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
            ps.setInt(i++, dto.activo() ? 1 : 0);
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
            try (PreparedStatement ps = cn.prepareStatement(insE)) {
              ps.setLong(1, idVuelo);
              ps.setLong(2, e.idCiudad());
              ps.setTimestamp(3, Timestamp.valueOf(e.llegada()));
              ps.setTimestamp(4, Timestamp.valueOf(e.salida()));
              ps.executeUpdate();
            }
          }
        }

        if (dto.motivoCambio() != null && !dto.motivoCambio().isBlank()) {
          try (PreparedStatement ps = cn.prepareStatement(
              "INSERT INTO VUELO_MOTIVO (ID_VUELO, TIPO, MOTIVO) VALUES (?,?,?)")) {
            ps.setLong(1, idVuelo);
            ps.setString(2, "MODIFICACION");
            ps.setString(3, dto.motivoCambio().trim());
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

  private long crearVueloTx(Connection conn, VueloDTO.Create dto) throws Exception {
    final boolean hasActivo = dto.activo() != null;
    final String sqlVuelo = hasActivo
        ? "INSERT INTO VUELO (CODIGO, ID_RUTA, FECHA_SALIDA, FECHA_LLEGADA, ACTIVO) VALUES (?,?,?,?,?)"
        : "INSERT INTO VUELO (CODIGO, ID_RUTA, FECHA_SALIDA, FECHA_LLEGADA) VALUES (?,?,?,?)";

    try (PreparedStatement ps = conn.prepareStatement(sqlVuelo, new String[]{"ID_VUELO"})) {
      int i = 1;
      ps.setString(i++, dto.codigo());
      ps.setLong(i++, dto.idRuta());
      ps.setTimestamp(i++, Timestamp.valueOf(dto.fechaSalida()));
      ps.setTimestamp(i++, Timestamp.valueOf(dto.fechaLlegada()));
      if (hasActivo) {
        ps.setInt(i++, dto.activo() ? 1 : 0);
      }
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
