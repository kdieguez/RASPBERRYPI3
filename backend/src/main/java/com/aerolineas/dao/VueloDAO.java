package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.dto.VueloDTO;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

import static com.aerolineas.util.EstadosVuelo.*;

public class VueloDAO {

  // ======= FLAG para desactivar totalmente VUELO_ESCALA =======
  private static final boolean ESCALAS_ENABLED = false;

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

    // ---- Escalas desactivadas ----
    if (ESCALAS_ENABLED) {
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

    // ---- Escalas desactivadas ----
    if (ESCALAS_ENABLED) {
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

    // ---- Escalas desactivadas ----
    if (ESCALAS_ENABLED) {
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

    // ---- Escalas desactivadas ----
    if (ESCALAS_ENABLED) {
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

    // ---- Escalas desactivadas ----
    if (ESCALAS_ENABLED) {
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

        // ---- Escalas desactivadas ----
        if (ESCALAS_ENABLED && dto.escalas() != null) {
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

        // ---- Escalas desactivadas ----
        if (ESCALAS_ENABLED && dto.escalas() != null && !dto.escalas().isEmpty()) {
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

  // =========================
  //  Vuelos con Escala (modelo compuesto) — SIGUEN IGUAL
  //  (Usan tablas VUELO_CON_ESCALA y VUELO_CON_ESCALA_CLASE)
  // =========================

  public long crearVueloConEscala(VueloDTO.VueloConEscalaCreate dto) throws Exception {
    try (Connection conn = DB.getConnection()) {
      conn.setAutoCommit(false);
      try {
        validarCompatibilidadEscala(conn, dto.idVueloPrimerTramo(), dto.idVueloSegundoTramo());

        String sql = "INSERT INTO VUELO_CON_ESCALA (CODIGO, ID_VUELO_PRIMER_TRAMO, ID_VUELO_SEGUNDO_TRAMO, ACTIVO) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, new String[]{"ID_VUELO_CON_ESCALA"})) {
          ps.setString(1, dto.codigo());
          ps.setLong(2, dto.idVueloPrimerTramo());
          ps.setLong(3, dto.idVueloSegundoTramo());
          ps.setInt(4, dto.activo() != null && dto.activo() ? 1 : 0);
          ps.executeUpdate();

          try (ResultSet rs = ps.getGeneratedKeys()) {
            if (!rs.next()) throw new SQLException("No se generó ID_VUELO_CON_ESCALA");
            long idVueloConEscala = rs.getLong(1);

            if (dto.clases() != null && !dto.clases().isEmpty()) {
              String sqlClases = "INSERT INTO VUELO_CON_ESCALA_CLASE (ID_VUELO_CON_ESCALA, ID_CLASE, CUPO_TOTAL, PRECIO) VALUES (?, ?, ?, ?)";
              try (PreparedStatement psClases = conn.prepareStatement(sqlClases)) {
                for (VueloDTO.ClaseConfig clase : dto.clases()) {
                  psClases.setLong(1, idVueloConEscala);
                  psClases.setInt(2, clase.idClase());
                  psClases.setInt(3, clase.cupoTotal());
                  psClases.setDouble(4, clase.precio());
                  psClases.addBatch();
                }
                psClases.executeBatch();
              }
            }

            conn.commit();
            return idVueloConEscala;
          }
        }
      } catch (Exception e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(true);
      }
    }
  }

  public void validarCompatibilidadEscala(Connection conn, long idVuelo1, long idVuelo2) throws SQLException {
    if (idVuelo1 <= 0 || idVuelo2 <= 0) {
      throw new SQLException("IDs de vuelo inválidos");
    }
    if (idVuelo1 == idVuelo2) {
      throw new SQLException("Un vuelo no puede ser escala de sí mismo");
    }

    String sql = """
        SELECT v1.ID_VUELO, v1.FECHA_LLEGADA, v2.FECHA_SALIDA,
               co1.NOMBRE AS ORIGEN1, cd1.NOMBRE AS DESTINO1,
               co2.NOMBRE AS ORIGEN2, cd2.NOMBRE AS DESTINO2,
               v1.ACTIVO AS ACTIVO1, v2.ACTIVO AS ACTIVO2,
               e1.Estado AS ESTADO1, e2.Estado AS ESTADO2
        FROM VUELO v1
        JOIN RUTA r1 ON r1.ID_RUTA = v1.ID_RUTA
        JOIN CIUDAD co1 ON co1.ID_CIUDAD = r1.ID_CIUDAD_ORIGEN
        JOIN CIUDAD cd1 ON cd1.ID_CIUDAD = r1.ID_CIUDAD_DESTINO
        JOIN ESTADOS e1 ON e1.ID_ESTADO = v1.ID_ESTADO
        CROSS JOIN VUELO v2
        JOIN RUTA r2 ON r2.ID_RUTA = v2.ID_RUTA
        JOIN CIUDAD co2 ON co2.ID_CIUDAD = r2.ID_CIUDAD_ORIGEN
        JOIN CIUDAD cd2 ON cd2.ID_CIUDAD = r2.ID_CIUDAD_DESTINO
        JOIN ESTADOS e2 ON e2.ID_ESTADO = v2.ID_ESTADO
        WHERE v1.ID_VUELO = ? AND v2.ID_VUELO = ?
        """;

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setLong(1, idVuelo1);
      ps.setLong(2, idVuelo2);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          throw new SQLException("Uno o ambos vuelos no existen");
        }

        if (rs.getInt("ACTIVO1") != 1 || rs.getInt("ACTIVO2") != 1) {
          throw new SQLException("Ambos vuelos deben estar activos");
        }

        String estado1 = rs.getString("ESTADO1").toLowerCase();
        String estado2 = rs.getString("ESTADO2").toLowerCase();
        if (estado1.contains("cancelado") || estado2.contains("cancelado")) {
          throw new SQLException("Los vuelos no pueden estar cancelados");
        }

        String destino1 = rs.getString("DESTINO1");
        String origen2 = rs.getString("ORIGEN2");
        if (!destino1.equals(origen2)) {
          throw new SQLException("El destino del primer vuelo debe ser el origen del segundo vuelo");
        }

        LocalDateTime llegada1 = rs.getTimestamp("FECHA_LLEGADA").toLocalDateTime();
        LocalDateTime salida2 = rs.getTimestamp("FECHA_SALIDA").toLocalDateTime();
        if (llegada1.isAfter(salida2)) {
          throw new SQLException("La llegada del primer vuelo debe ser antes o igual a la salida del segundo vuelo");
        }
      }
    }
  }

  public VueloDTO.VueloConEscalaView obtenerVueloConEscala(long id) throws Exception {
    String sql = """
        SELECT vce.ID_VUELO_CON_ESCALA, vce.CODIGO, vce.ACTIVO,
               v1.ID_VUELO AS ID_VUELO1, v1.CODIGO AS CODIGO1, v1.FECHA_SALIDA AS FECHA_SALIDA1, v1.FECHA_LLEGADA AS FECHA_LLEGADA1,
               v2.ID_VUELO AS ID_VUELO2, v2.CODIGO AS CODIGO2, v2.FECHA_SALIDA AS FECHA_SALIDA2, v2.FECHA_LLEGADA AS FECHA_LLEGADA2,
               co1.NOMBRE AS ORIGEN1, cd1.NOMBRE AS DESTINO1, po1.NOMBRE AS ORIGEN_PAIS1, pd1.NOMBRE AS DESTINO_PAIS1,
               co2.NOMBRE AS ORIGEN2, cd2.NOMBRE AS DESTINO2, po2.NOMBRE AS ORIGEN_PAIS2, pd2.NOMBRE AS DESTINO_PAIS2,
               e1.Estado AS ESTADO1, e2.Estado AS ESTADO2,
               vcec.ID_CLASE, vcec.CUPO_TOTAL, vcec.PRECIO
        FROM VUELO_CON_ESCALA vce
        JOIN VUELO v1 ON v1.ID_VUELO = vce.ID_VUELO_PRIMER_TRAMO
        JOIN VUELO v2 ON v2.ID_VUELO = vce.ID_VUELO_SEGUNDO_TRAMO
        JOIN RUTA r1 ON r1.ID_RUTA = v1.ID_RUTA
        JOIN RUTA r2 ON r2.ID_RUTA = v2.ID_RUTA
        JOIN CIUDAD co1 ON co1.ID_CIUDAD = r1.ID_CIUDAD_ORIGEN
        JOIN CIUDAD cd1 ON cd1.ID_CIUDAD = r1.ID_CIUDAD_DESTINO
        JOIN CIUDAD co2 ON co2.ID_CIUDAD = r2.ID_CIUDAD_ORIGEN
        JOIN CIUDAD cd2 ON cd2.ID_CIUDAD = r2.ID_CIUDAD_DESTINO
        JOIN PAIS po1 ON po1.ID_PAIS = co1.ID_PAIS
        JOIN PAIS pd1 ON pd1.ID_PAIS = cd1.ID_PAIS
        JOIN PAIS po2 ON po2.ID_PAIS = co2.ID_PAIS
        JOIN PAIS pd2 ON pd2.ID_PAIS = cd2.ID_PAIS
        JOIN ESTADOS e1 ON e1.ID_ESTADO = v1.ID_ESTADO
        JOIN ESTADOS e2 ON e2.ID_ESTADO = v2.ID_ESTADO
        LEFT JOIN VUELO_CON_ESCALA_CLASE vcec ON vcec.ID_VUELO_CON_ESCALA = vce.ID_VUELO_CON_ESCALA
        WHERE vce.ID_VUELO_CON_ESCALA = ?
        """;

    VueloDTO.VueloConEscalaView view = null;
    List<VueloDTO.ClaseConfig> clases = new ArrayList<>();

    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          if (view == null) {
            VueloDTO.View primerTramo = new VueloDTO.View(
                rs.getLong("ID_VUELO1"),
                rs.getString("CODIGO1"),
                rs.getLong("ID_VUELO1"),
                rs.getString("ORIGEN1"),
                rs.getString("DESTINO1"),
                rs.getTimestamp("FECHA_SALIDA1").toLocalDateTime(),
                rs.getTimestamp("FECHA_LLEGADA1").toLocalDateTime(),
                true,
                null,
                rs.getString("ESTADO1"),
                new ArrayList<>(),
                new ArrayList<>(),
                null,
                rs.getString("ORIGEN_PAIS1"),
                rs.getString("DESTINO_PAIS1")
            );

            VueloDTO.View segundoTramo = new VueloDTO.View(
                rs.getLong("ID_VUELO2"),
                rs.getString("CODIGO2"),
                rs.getLong("ID_VUELO2"),
                rs.getString("ORIGEN2"),
                rs.getString("DESTINO2"),
                rs.getTimestamp("FECHA_SALIDA2").toLocalDateTime(),
                rs.getTimestamp("FECHA_LLEGADA2").toLocalDateTime(),
                true,
                null,
                rs.getString("ESTADO2"),
                new ArrayList<>(),
                new ArrayList<>(),
                null,
                rs.getString("ORIGEN_PAIS2"),
                rs.getString("DESTINO_PAIS2")
            );

            view = new VueloDTO.VueloConEscalaView(
                rs.getLong("ID_VUELO_CON_ESCALA"),
                rs.getString("CODIGO"),
                primerTramo,
                segundoTramo,
                rs.getString("ORIGEN1"),
                rs.getString("DESTINO2"),
                rs.getString("ORIGEN_PAIS1"),
                rs.getString("DESTINO_PAIS2"),
                rs.getTimestamp("FECHA_SALIDA1").toLocalDateTime(),
                rs.getTimestamp("FECHA_LLEGADA2").toLocalDateTime(),
                rs.getInt("ACTIVO") == 1,
                null,
                "ACTIVO",
                clases,
                null
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

    return view;
  }

  public List<VueloDTO.VueloConEscalaView> listarVuelosConEscala() throws SQLException {
    String sql = """
        SELECT vce.ID_VUELO_CON_ESCALA, vce.CODIGO, vce.ACTIVO,
               v1.ID_VUELO AS ID_VUELO1, v1.CODIGO AS CODIGO1, v1.FECHA_SALIDA AS FECHA_SALIDA1, v1.FECHA_LLEGADA AS FECHA_LLEGADA1,
               v2.ID_VUELO AS ID_VUELO2, v2.CODIGO AS CODIGO2, v2.FECHA_SALIDA AS FECHA_SALIDA2, v2.FECHA_LLEGADA AS FECHA_LLEGADA2,
               co1.NOMBRE AS ORIGEN1, cd1.NOMBRE AS DESTINO1, po1.NOMBRE AS ORIGEN_PAIS1, pd1.NOMBRE AS DESTINO_PAIS1,
               co2.NOMBRE AS ORIGEN2, cd2.NOMBRE AS DESTINO2, po2.NOMBRE AS ORIGEN_PAIS2, pd2.NOMBRE AS DESTINO_PAIS2,
               e1.Estado AS ESTADO1, e2.Estado AS ESTADO2,
               vcec.ID_CLASE, vcec.CUPO_TOTAL, vcec.PRECIO
        FROM VUELO_CON_ESCALA vce
        JOIN VUELO v1 ON v1.ID_VUELO = vce.ID_VUELO_PRIMER_TRAMO
        JOIN VUELO v2 ON v2.ID_VUELO = vce.ID_VUELO_SEGUNDO_TRAMO
        JOIN RUTA r1 ON r1.ID_RUTA = v1.ID_RUTA
        JOIN RUTA r2 ON r2.ID_RUTA = v2.ID_RUTA
        JOIN CIUDAD co1 ON co1.ID_CIUDAD = r1.ID_CIUDAD_ORIGEN
        JOIN CIUDAD cd1 ON cd1.ID_CIUDAD = r1.ID_CIUDAD_DESTINO
        JOIN CIUDAD co2 ON co2.ID_CIUDAD = r2.ID_CIUDAD_ORIGEN
        JOIN CIUDAD cd2 ON cd2.ID_CIUDAD = r2.ID_CIUDAD_DESTINO
        JOIN PAIS po1 ON po1.ID_PAIS = co1.ID_PAIS
        JOIN PAIS pd1 ON pd1.ID_PAIS = cd1.ID_PAIS
        JOIN PAIS po2 ON po2.ID_PAIS = co2.ID_PAIS
        JOIN PAIS pd2 ON pd2.ID_PAIS = cd2.ID_PAIS
        JOIN ESTADOS e1 ON e1.ID_ESTADO = v1.ID_ESTADO
        JOIN ESTADOS e2 ON e2.ID_ESTADO = v2.ID_ESTADO
        LEFT JOIN VUELO_CON_ESCALA_CLASE vcec ON vcec.ID_VUELO_CON_ESCALA = vce.ID_VUELO_CON_ESCALA
        WHERE vce.ACTIVO = 1
        ORDER BY vce.ID_VUELO_CON_ESCALA
        """;

    Map<Long, VueloDTO.VueloConEscalaView> vuelos = new LinkedHashMap<>();

    try (Connection conn = DB.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

      while (rs.next()) {
        long idVueloConEscala = rs.getLong("ID_VUELO_CON_ESCALA");
        VueloDTO.VueloConEscalaView view = vuelos.get(idVueloConEscala);

        if (view == null) {
          VueloDTO.View primerTramo = new VueloDTO.View(
              rs.getLong("ID_VUELO1"),
              rs.getString("CODIGO1"),
              rs.getLong("ID_VUELO1"),
              rs.getString("ORIGEN1"),
              rs.getString("DESTINO1"),
              rs.getTimestamp("FECHA_SALIDA1").toLocalDateTime(),
              rs.getTimestamp("FECHA_LLEGADA1").toLocalDateTime(),
              true,
              null,
              rs.getString("ESTADO1"),
              new ArrayList<>(),
              new ArrayList<>(),
              null,
              rs.getString("ORIGEN_PAIS1"),
              rs.getString("DESTINO_PAIS1")
          );

          VueloDTO.View segundoTramo = new VueloDTO.View(
              rs.getLong("ID_VUELO2"),
              rs.getString("CODIGO2"),
              rs.getLong("ID_VUELO2"),
              rs.getString("ORIGEN2"),
              rs.getString("DESTINO2"),
              rs.getTimestamp("FECHA_SALIDA2").toLocalDateTime(),
              rs.getTimestamp("FECHA_LLEGADA2").toLocalDateTime(),
              true,
              null,
              rs.getString("ESTADO2"),
              new ArrayList<>(),
              new ArrayList<>(),
              null,
              rs.getString("ORIGEN_PAIS2"),
              rs.getString("DESTINO_PAIS2")
          );

          view = new VueloDTO.VueloConEscalaView(
              idVueloConEscala,
              rs.getString("CODIGO"),
              primerTramo,
              segundoTramo,
              rs.getString("ORIGEN1"),
              rs.getString("DESTINO2"),
              rs.getString("ORIGEN_PAIS1"),
              rs.getString("DESTINO_PAIS2"),
              rs.getTimestamp("FECHA_SALIDA1").toLocalDateTime(),
              rs.getTimestamp("FECHA_LLEGADA2").toLocalDateTime(),
              rs.getInt("ACTIVO") == 1,
              null,
              "ACTIVO",
              new ArrayList<>(),
              null
          );
          vuelos.put(idVueloConEscala, view);
        }

        int idClase = rs.getInt("ID_CLASE");
        if (!rs.wasNull()) {
          view.clases().add(new VueloDTO.ClaseConfig(
              idClase,
              rs.getInt("CUPO_TOTAL"),
              rs.getDouble("PRECIO")
          ));
        }
      }
    }

    return new ArrayList<>(vuelos.values());
  }

  public List<VueloDTO.VueloConEscalaView> listarVuelosConEscalaPublic() throws SQLException {
    String sql = """
        SELECT vce.ID_VUELO_CON_ESCALA, vce.CODIGO, vce.ACTIVO,
               v1.ID_VUELO AS ID_VUELO1, v1.CODIGO AS CODIGO1, v1.FECHA_SALIDA AS FECHA_SALIDA1, v1.FECHA_LLEGADA AS FECHA_LLEGADA1,
               v2.ID_VUELO AS ID_VUELO2, v2.CODIGO AS CODIGO2, v2.FECHA_SALIDA AS FECHA_SALIDA2, v2.FECHA_LLEGADA AS FECHA_LLEGADA2,
               co1.NOMBRE AS ORIGEN1, cd1.NOMBRE AS DESTINO1, po1.NOMBRE AS ORIGEN_PAIS1, pd1.NOMBRE AS DESTINO_PAIS1,
               co2.NOMBRE AS ORIGEN2, cd2.NOMBRE AS DESTINO2, po2.NOMBRE AS ORIGEN_PAIS2, pd2.NOMBRE AS DESTINO_PAIS2,
               e1.Estado AS ESTADO1, e2.Estado AS ESTADO2,
               vcec.ID_CLASE, vcec.CUPO_TOTAL, vcec.PRECIO
        FROM VUELO_CON_ESCALA vce
        JOIN VUELO v1 ON v1.ID_VUELO = vce.ID_VUELO_PRIMER_TRAMO
        JOIN VUELO v2 ON v2.ID_VUELO = vce.ID_VUELO_SEGUNDO_TRAMO
        JOIN RUTA r1 ON r1.ID_RUTA = v1.ID_RUTA
        JOIN RUTA r2 ON r2.ID_RUTA = v2.ID_RUTA
        JOIN CIUDAD co1 ON co1.ID_CIUDAD = r1.ID_CIUDAD_ORIGEN
        JOIN CIUDAD cd1 ON cd1.ID_CIUDAD = r1.ID_CIUDAD_DESTINO
        JOIN CIUDAD co2 ON co2.ID_CIUDAD = r2.ID_CIUDAD_ORIGEN
        JOIN CIUDAD cd2 ON cd2.ID_CIUDAD = r2.ID_CIUDAD_DESTINO
        JOIN PAIS po1 ON po1.ID_PAIS = co1.ID_PAIS
        JOIN PAIS pd1 ON pd1.ID_PAIS = cd1.ID_PAIS
        JOIN PAIS po2 ON po2.ID_PAIS = co2.ID_PAIS
        JOIN PAIS pd2 ON pd2.ID_PAIS = cd2.ID_PAIS
        JOIN ESTADOS e1 ON e1.ID_ESTADO = v1.ID_ESTADO
        JOIN ESTADOS e2 ON e2.ID_ESTADO = v2.ID_ESTADO
        LEFT JOIN VUELO_CON_ESCALA_CLASE vcec ON vcec.ID_VUELO_CON_ESCALA = vce.ID_VUELO_CON_ESCALA
        WHERE vce.ACTIVO = 1
          AND NVL(v1.ACTIVO,1) = 1
          AND NVL(v2.ACTIVO,1) = 1
          AND UPPER(e1.Estado) <> 'CANCELADO'
          AND UPPER(e2.Estado) <> 'CANCELADO'
        ORDER BY vce.ID_VUELO_CON_ESCALA
        """;

    Map<Long, VueloDTO.VueloConEscalaView> vuelos = new LinkedHashMap<>();

    try (Connection conn = DB.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

      while (rs.next()) {
        long idVueloConEscala = rs.getLong("ID_VUELO_CON_ESCALA");
        VueloDTO.VueloConEscalaView view = vuelos.get(idVueloConEscala);

        if (view == null) {
          VueloDTO.View primerTramo = new VueloDTO.View(
              rs.getLong("ID_VUELO1"),
              rs.getString("CODIGO1"),
              rs.getLong("ID_VUELO1"),
              rs.getString("ORIGEN1"),
              rs.getString("DESTINO1"),
              rs.getTimestamp("FECHA_SALIDA1").toLocalDateTime(),
              rs.getTimestamp("FECHA_LLEGADA1").toLocalDateTime(),
              true,
              null,
              rs.getString("ESTADO1"),
              new ArrayList<>(),
              new ArrayList<>(),
              null,
              rs.getString("ORIGEN_PAIS1"),
              rs.getString("DESTINO_PAIS1")
          );

          VueloDTO.View segundoTramo = new VueloDTO.View(
              rs.getLong("ID_VUELO2"),
              rs.getString("CODIGO2"),
              rs.getLong("ID_VUELO2"),
              rs.getString("ORIGEN2"),
              rs.getString("DESTINO2"),
              rs.getTimestamp("FECHA_SALIDA2").toLocalDateTime(),
              rs.getTimestamp("FECHA_LLEGADA2").toLocalDateTime(),
              true,
              null,
              rs.getString("ESTADO2"),
              new ArrayList<>(),
              new ArrayList<>(),
              null,
              rs.getString("ORIGEN_PAIS2"),
              rs.getString("DESTINO_PAIS2")
          );

          view = new VueloDTO.VueloConEscalaView(
              idVueloConEscala,
              rs.getString("CODIGO"),
              primerTramo,
              segundoTramo,
              rs.getString("ORIGEN1"),
              rs.getString("DESTINO2"),
              rs.getString("ORIGEN_PAIS1"),
              rs.getString("DESTINO_PAIS2"),
              rs.getTimestamp("FECHA_SALIDA1").toLocalDateTime(),
              rs.getTimestamp("FECHA_LLEGADA2").toLocalDateTime(),
              rs.getInt("ACTIVO") == 1,
              null,
              "ACTIVO",
              new ArrayList<>(),
              null
          );
          vuelos.put(idVueloConEscala, view);
        }

        int idClase = rs.getInt("ID_CLASE");
        if (!rs.wasNull()) {
          view.clases().add(new VueloDTO.ClaseConfig(
              idClase,
              rs.getInt("CUPO_TOTAL"),
              rs.getDouble("PRECIO")
          ));
        }
      }
    }

    return new ArrayList<>(vuelos.values());
  }

  public VueloDTO.VueloConEscalaView obtenerVueloConEscalaPublic(long id) throws Exception {
    String sql = """
        SELECT vce.ID_VUELO_CON_ESCALA, vce.CODIGO, vce.ACTIVO,
               v1.ID_VUELO AS ID_VUELO1, v1.CODIGO AS CODIGO1, v1.FECHA_SALIDA AS FECHA_SALIDA1, v1.FECHA_LLEGADA AS FECHA_LLEGADA1,
               v2.ID_VUELO AS ID_VUELO2, v2.CODIGO AS CODIGO2, v2.FECHA_SALIDA AS FECHA_SALIDA2, v2.FECHA_LLEGADA AS FECHA_LLEGADA2,
               co1.NOMBRE AS ORIGEN1, cd1.NOMBRE AS DESTINO1, po1.NOMBRE AS ORIGEN_PAIS1, pd1.NOMBRE AS DESTINO_PAIS1,
               co2.NOMBRE AS ORIGEN2, cd2.NOMBRE AS DESTINO2, po2.NOMBRE AS ORIGEN_PAIS2, pd2.NOMBRE AS DESTINO_PAIS2,
               e1.Estado AS ESTADO1, e2.Estado AS ESTADO2,
               vcec.ID_CLASE, vcec.CUPO_TOTAL, vcec.PRECIO
        FROM VUELO_CON_ESCALA vce
        JOIN VUELO v1 ON v1.ID_VUELO = vce.ID_VUELO_PRIMER_TRAMO
        JOIN VUELO v2 ON v2.ID_VUELO = vce.ID_VUELO_SEGUNDO_TRAMO
        JOIN RUTA r1 ON r1.ID_RUTA = v1.ID_RUTA
        JOIN RUTA r2 ON r2.ID_RUTA = v2.ID_RUTA
        JOIN CIUDAD co1 ON co1.ID_CIUDAD = r1.ID_CIUDAD_ORIGEN
        JOIN CIUDAD cd1 ON cd1.ID_CIUDAD = r1.ID_CIUDAD_DESTINO
        JOIN CIUDAD co2 ON co2.ID_CIUDAD = r2.ID_CIUDAD_ORIGEN
        JOIN CIUDAD cd2 ON cd2.ID_CIUDAD = r2.ID_CIUDAD_DESTINO
        JOIN PAIS po1 ON po1.ID_PAIS = co1.ID_PAIS
        JOIN PAIS pd1 ON pd1.ID_PAIS = cd1.ID_PAIS
        JOIN PAIS po2 ON po2.ID_PAIS = co2.ID_PAIS
        JOIN PAIS pd2 ON pd2.ID_PAIS = cd2.ID_PAIS
        JOIN ESTADOS e1 ON e1.ID_ESTADO = v1.ID_ESTADO
        JOIN ESTADOS e2 ON e2.ID_ESTADO = v2.ID_ESTADO
        LEFT JOIN VUELO_CON_ESCALA_CLASE vcec ON vcec.ID_VUELO_CON_ESCALA = vce.ID_VUELO_CON_ESCALA
        WHERE vce.ID_VUELO_CON_ESCALA = ?
          AND vce.ACTIVO = 1
          AND NVL(v1.ACTIVO,1) = 1
          AND NVL(v2.ACTIVO,1) = 1
          AND UPPER(e1.Estado) <> 'CANCELADO'
          AND UPPER(e2.Estado) <> 'CANCELADO'
        """;

    VueloDTO.VueloConEscalaView view = null;
    List<VueloDTO.ClaseConfig> clases = new ArrayList<>();

    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          if (view == null) {
            VueloDTO.View primerTramo = new VueloDTO.View(
                rs.getLong("ID_VUELO1"),
                rs.getString("CODIGO1"),
                rs.getLong("ID_VUELO1"),
                rs.getString("ORIGEN1"),
                rs.getString("DESTINO1"),
                rs.getTimestamp("FECHA_SALIDA1").toLocalDateTime(),
                rs.getTimestamp("FECHA_LLEGADA1").toLocalDateTime(),
                true,
                null,
                rs.getString("ESTADO1"),
                new ArrayList<>(),
                new ArrayList<>(),
                null,
                rs.getString("ORIGEN_PAIS1"),
                rs.getString("DESTINO_PAIS1")
            );

            VueloDTO.View segundoTramo = new VueloDTO.View(
                rs.getLong("ID_VUELO2"),
                rs.getString("CODIGO2"),
                rs.getLong("ID_VUELO2"),
                rs.getString("ORIGEN2"),
                rs.getString("DESTINO2"),
                rs.getTimestamp("FECHA_SALIDA2").toLocalDateTime(),
                rs.getTimestamp("FECHA_LLEGADA2").toLocalDateTime(),
                true,
                null,
                rs.getString("ESTADO2"),
                new ArrayList<>(),
                new ArrayList<>(),
                null,
                rs.getString("ORIGEN_PAIS2"),
                rs.getString("DESTINO_PAIS2")
            );

            view = new VueloDTO.VueloConEscalaView(
                rs.getLong("ID_VUELO_CON_ESCALA"),
                rs.getString("CODIGO"),
                primerTramo,
                segundoTramo,
                rs.getString("ORIGEN1"),
                rs.getString("DESTINO2"),
                rs.getString("ORIGEN_PAIS1"),
                rs.getString("DESTINO_PAIS2"),
                rs.getTimestamp("FECHA_SALIDA1").toLocalDateTime(),
                rs.getTimestamp("FECHA_LLEGADA2").toLocalDateTime(),
                rs.getInt("ACTIVO") == 1,
                null,
                "ACTIVO",
                clases,
                null
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

    return view;
  }
}
