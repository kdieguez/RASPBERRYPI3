package com.aerolineas.dao;

import java.sql.*;
import java.util.*;
import java.math.BigDecimal;

import com.aerolineas.dto.CompraDTO.CarritoItem;
import com.aerolineas.dto.CompraDTO.CarritoResp;
import com.aerolineas.dto.CompraDTO.ReservaDetalle;
import com.aerolineas.dto.CompraDTO.ReservaItem;
import com.aerolineas.dto.CompraDTO.ReservaListItem;

public class ComprasDAO {
  private static Connection getConn() throws SQLException {
    try {
      Class<?> dbClass = Class.forName("com.aerolineas.config.DB");
      try {
        var m = dbClass.getMethod("getConnection");
        return (Connection) m.invoke(null);
      } catch (NoSuchMethodException nsme) {
        var m = dbClass.getMethod("get");
        return (Connection) m.invoke(null);
      }
    } catch (Exception e) {
      throw new SQLException("No se pudo obtener conexión desde DB: " + e.getMessage(), e);
    }
  }

  private static void setFieldIfExists(Object obj, String field, Object value) {
    try {
      var f = obj.getClass().getDeclaredField(field);
      f.setAccessible(true);
      f.set(obj, value);
    } catch (NoSuchFieldException ignore) {
    } catch (Exception ignore) {
    }
  }

  public long ensureCartForUser(long userId) throws Exception {
    try (Connection cn = getConn()) {
      try (PreparedStatement ps = cn.prepareStatement(
          "SELECT ID_CARRITO FROM AEROLINEA.CARRITO WHERE ID_USUARIO = ?")) {
        ps.setLong(1, userId);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) return rs.getLong(1);
        }
      }
      try (PreparedStatement ps = cn.prepareStatement(
          "INSERT INTO AEROLINEA.CARRITO(ID_USUARIO) VALUES (?)")) {
        ps.setLong(1, userId);
        ps.executeUpdate();
      }
      try (PreparedStatement ps = cn.prepareStatement(
          "SELECT ID_CARRITO FROM AEROLINEA.CARRITO WHERE ID_USUARIO = ?")) {
        ps.setLong(1, userId);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) return rs.getLong(1);
        }
      }
    }
    throw new SQLException("No fue posible crear/obtener el carrito.");
  }

  public CarritoResp getCart(long userId) throws Exception {
    long cartId = ensureCartForUser(userId);
    CarritoResp out = new CarritoResp();
    out.items = new ArrayList<>();

    try (Connection cn = getConn()) {
      try (PreparedStatement ps = cn.prepareStatement(
          "SELECT ID_CARRITO, ID_USUARIO, FECHA_CREACION, TOTAL " +
          "FROM AEROLINEA.VW_CARRITO_RESUMEN WHERE ID_CARRITO = ?")) {
        ps.setLong(1, cartId);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            out.idCarrito = rs.getLong("ID_CARRITO");
            out.idUsuario = rs.getLong("ID_USUARIO");
            Timestamp ts = rs.getTimestamp("FECHA_CREACION");
            out.fechaCreacion = ts != null ? ts.toInstant().toString() : null;
            out.total = rs.getBigDecimal("TOTAL");
          }
        }
      }
      if (out.total == null) out.total = BigDecimal.ZERO;

      String SQL_ITEMS =
        "SELECT ci.ID_ITEM, ci.ID_CARRITO, ci.ID_VUELO, v.CODIGO AS CODIGO_VUELO, " +
        "       v.FECHA_SALIDA, v.FECHA_LLEGADA, " +
        "       ci.ID_CLASE, ca.NOMBRE AS NOMBRE_CLASE, " +
        "       ci.CANTIDAD, ci.PRECIO_UNITARIO, (ci.CANTIDAD*ci.PRECIO_UNITARIO) AS SUBTOTAL, " +
        "       po.NOMBRE AS PAIS_ORIGEN, pd.NOMBRE AS PAIS_DESTINO, " +
        "       co.NOMBRE AS CIUDAD_ORIGEN, cd.NOMBRE AS CIUDAD_DESTINO " +
        "FROM AEROLINEA.CARRITO_ITEM ci " +
        "JOIN AEROLINEA.VUELO v ON v.ID_VUELO = ci.ID_VUELO " +
        "JOIN AEROLINEA.CLASE_ASIENTO ca ON ca.ID_CLASE = ci.ID_CLASE " +
        "JOIN AEROLINEA.RUTA r ON r.ID_RUTA = v.ID_RUTA " +
        "JOIN AEROLINEA.CIUDAD co ON co.ID_CIUDAD = r.ID_CIUDAD_ORIGEN " +
        "JOIN AEROLINEA.CIUDAD cd ON cd.ID_CIUDAD = r.ID_CIUDAD_DESTINO " +
        "JOIN AEROLINEA.PAIS po ON po.ID_PAIS = co.ID_PAIS " +
        "JOIN AEROLINEA.PAIS pd ON pd.ID_PAIS = cd.ID_PAIS " +
        "WHERE ci.ID_CARRITO = ? " +
        "ORDER BY ci.ID_ITEM";

      try (PreparedStatement ps = cn.prepareStatement(SQL_ITEMS)) {
        ps.setLong(1, cartId);
        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            CarritoItem it = new CarritoItem();
            it.idItem = rs.getLong("ID_ITEM");
            it.idVuelo = rs.getLong("ID_VUELO");
            it.codigoVuelo = rs.getString("CODIGO_VUELO");
            Timestamp ts1 = rs.getTimestamp("FECHA_SALIDA");
            Timestamp ts2 = rs.getTimestamp("FECHA_LLEGADA");
            it.fechaSalida = ts1 != null ? ts1.toInstant().toString() : null;
            it.fechaLlegada = ts2 != null ? ts2.toInstant().toString() : null;
            it.idClase = rs.getInt("ID_CLASE");
            it.clase = rs.getString("NOMBRE_CLASE");
            it.cantidad = rs.getInt("CANTIDAD");
            it.precioUnitario = rs.getBigDecimal("PRECIO_UNITARIO");
            it.subtotal = rs.getBigDecimal("SUBTOTAL");
            it.codigoVuelo = rs.getString("CODIGO");

            setFieldIfExists(it, "paisOrigen",    rs.getString("PAIS_ORIGEN"));
            setFieldIfExists(it, "paisDestino",   rs.getString("PAIS_DESTINO"));
            setFieldIfExists(it, "ciudadOrigen",  rs.getString("CIUDAD_ORIGEN"));
            setFieldIfExists(it, "ciudadDestino", rs.getString("CIUDAD_DESTINO"));

            out.items.add(it);
          }
        }
      }
    }
    return out;
  }


  public void addOrIncrementItem(long userId, long idVuelo, int idClase, int cantidad) throws Exception {
    if (cantidad <= 0) cantidad = 1;
    long cartId = ensureCartForUser(userId);

    try (Connection cn = getConn()) {
      cn.setAutoCommit(false);
      try {
        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT NVL(v.ACTIVO,1) AS ACTIVO, UPPER(e.ESTADO) AS ESTADO " +
            "FROM AEROLINEA.VUELO v JOIN AEROLINEA.ESTADOS e ON e.ID_ESTADO=v.ID_ESTADO " +
            "WHERE v.ID_VUELO = ?")) {
          ps.setLong(1, idVuelo);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new SQLException("Vuelo no existe");
            int activo = rs.getInt("ACTIVO");
            String estado = rs.getString("ESTADO");
            if (activo != 1 || "CANCELADO".equalsIgnoreCase(estado)) {
              throw new SQLException("Vuelo no disponible para compra");
            }
          }
        }

        Integer cupoTotal = null;
        BigDecimal precio = null;

        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT CUPO_TOTAL, PRECIO " +
            "FROM AEROLINEA.SALIDA_CLASE " +
            "WHERE ID_VUELO = ? AND ID_CLASE = ? FOR UPDATE")) {
          ps.setLong(1, idVuelo);
          ps.setInt(2, idClase);
          try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
              cupoTotal = rs.getInt("CUPO_TOTAL");
              precio = rs.getBigDecimal("PRECIO");
            }
          }
        }

        if (cupoTotal == null) {
          throw new SQLException("Clase no disponible para esta salida");
        }
        if (precio == null) {
          try (PreparedStatement ps = cn.prepareStatement(
              "SELECT PRECIO FROM AEROLINEA.VUELO_CLASE WHERE ID_VUELO = ? AND ID_CLASE = ?")) {
            ps.setLong(1, idVuelo);
            ps.setInt(2, idClase);
            try (ResultSet rs = ps.executeQuery()) {
              if (rs.next()) precio = rs.getBigDecimal(1);
            }
          }
        }
        if (precio == null) throw new SQLException("No se encontró precio para la clase/vuelo.");

        int reservados = 0;
        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT NVL(COUNT(*),0) FROM AEROLINEA.RESERVA_ITEM ri " +
            "JOIN AEROLINEA.RESERVA r ON r.ID_RESERVA = ri.ID_RESERVA " +
            "WHERE ri.ID_VUELO=? AND ri.ID_CLASE=? AND r.ID_ESTADO=1")) {
          ps.setLong(1, idVuelo);
          ps.setInt(2, idClase);
          try (ResultSet rs = ps.executeQuery()) { if (rs.next()) reservados = rs.getInt(1); }
        }

        int enCarritos = 0;
        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT NVL(SUM(CANTIDAD),0) FROM AEROLINEA.CARRITO_ITEM " +
            "WHERE ID_VUELO=? AND ID_CLASE=?")) {
          ps.setLong(1, idVuelo);
          ps.setInt(2, idClase);
          try (ResultSet rs = ps.executeQuery()) { if (rs.next()) enCarritos = rs.getInt(1); }
        }

        int disponible = cupoTotal - reservados - enCarritos;
        if (disponible <= 0) throw new SQLException("Sin cupo disponible");
        if (cantidad > disponible) throw new SQLException("Cupo insuficiente: quedan " + disponible);

        try (PreparedStatement ps = cn.prepareStatement(
            "MERGE INTO AEROLINEA.CARRITO_ITEM t " +
            "USING (SELECT ? idc, ? idv, ? idcl FROM dual) s " +
            "ON (t.ID_CARRITO = s.idc AND t.ID_VUELO = s.idv AND t.ID_CLASE = s.idcl) " +
            "WHEN MATCHED THEN UPDATE SET t.CANTIDAD = t.CANTIDAD + ? " +
            "WHEN NOT MATCHED THEN INSERT (ID_CARRITO, ID_VUELO, ID_CLASE, CANTIDAD, PRECIO_UNITARIO) " +
            "VALUES (s.idc, s.idv, s.idcl, ?, ?)")) {
          ps.setLong(1, cartId);
          ps.setLong(2, idVuelo);
          ps.setInt(3, idClase);
          ps.setInt(4, cantidad);
          ps.setInt(5, cantidad);
          ps.setBigDecimal(6, precio);
          ps.executeUpdate();
        }

        cn.commit();
      } catch (Exception ex) {
        cn.rollback(); throw ex;
      } finally {
        cn.setAutoCommit(true);
      }
    }
  }

  public void updateQuantity(long userId, long idItem, int cantidad) throws Exception {
    if (cantidad <= 0) throw new IllegalArgumentException("Cantidad debe ser > 0");
    long cartId = ensureCartForUser(userId);

    try (Connection cn = getConn()) {
      cn.setAutoCommit(false);
      try {
        long idVuelo;
        int idClase;
        int cantActual;
        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT ID_VUELO, ID_CLASE, CANTIDAD FROM AEROLINEA.CARRITO_ITEM " +
            "WHERE ID_ITEM = ? AND ID_CARRITO = ?")) {
          ps.setLong(1, idItem);
          ps.setLong(2, cartId);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new SQLException("Item no encontrado.");
            idVuelo = rs.getLong("ID_VUELO");
            idClase = rs.getInt("ID_CLASE");
            cantActual = rs.getInt("CANTIDAD");
          }
        }

        if (cantidad <= cantActual) {
          try (PreparedStatement ps = cn.prepareStatement(
              "UPDATE AEROLINEA.CARRITO_ITEM SET CANTIDAD = ? WHERE ID_ITEM = ? AND ID_CARRITO = ?")) {
            ps.setInt(1, cantidad);
            ps.setLong(2, idItem);
            ps.setLong(3, cartId);
            ps.executeUpdate();
          }
          cn.commit();
          return;
        }

        int delta = cantidad - cantActual;

        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT NVL(v.ACTIVO,1) AS ACTIVO, UPPER(e.ESTADO) AS ESTADO " +
            "FROM AEROLINEA.VUELO v JOIN AEROLINEA.ESTADOS e ON e.ID_ESTADO=v.ID_ESTADO " +
            "WHERE v.ID_VUELO = ?")) {
          ps.setLong(1, idVuelo);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new SQLException("Vuelo no existe");
            int activo = rs.getInt("ACTIVO");
            String estado = rs.getString("ESTADO");
            if (activo != 1 || "CANCELADO".equalsIgnoreCase(estado)) {
              throw new SQLException("Vuelo no disponible para compra");
            }
          }
        }

        Integer cupoTotal = null;
        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT CUPO_TOTAL FROM AEROLINEA.SALIDA_CLASE " +
            "WHERE ID_VUELO = ? AND ID_CLASE = ? FOR UPDATE")) {
          ps.setLong(1, idVuelo);
          ps.setInt(2, idClase);
          try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) cupoTotal = rs.getInt(1);
          }
        }
        if (cupoTotal == null) throw new SQLException("Clase no disponible para esta salida");

        int reservados = 0;
        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT NVL(COUNT(*),0) FROM AEROLINEA.RESERVA_ITEM ri " +
            "JOIN AEROLINEA.RESERVA r ON r.ID_RESERVA = ri.ID_RESERVA " +
            "WHERE ri.ID_VUELO=? AND ri.ID_CLASE=? AND r.ID_ESTADO=1")) {
          ps.setLong(1, idVuelo);
          ps.setInt(2, idClase);
          try (ResultSet rs = ps.executeQuery()) { if (rs.next()) reservados = rs.getInt(1); }
        }

        int enCarritos = 0;
        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT NVL(SUM(CANTIDAD),0) FROM AEROLINEA.CARRITO_ITEM " +
            "WHERE ID_VUELO=? AND ID_CLASE=?")) {
          ps.setLong(1, idVuelo);
          ps.setInt(2, idClase);
          try (ResultSet rs = ps.executeQuery()) { if (rs.next()) enCarritos = rs.getInt(1); }
        }

        int disponible = cupoTotal - reservados - enCarritos;
        if (disponible <= 0) throw new SQLException("Sin cupo disponible");
        if (delta > disponible) throw new SQLException("Cupo insuficiente: puedes subir hasta " + (cantActual + disponible));

        try (PreparedStatement ps = cn.prepareStatement(
            "UPDATE AEROLINEA.CARRITO_ITEM SET CANTIDAD = ? WHERE ID_ITEM = ? AND ID_CARRITO = ?")) {
          ps.setInt(1, cantidad);
          ps.setLong(2, idItem);
          ps.setLong(3, cartId);
          ps.executeUpdate();
        }

        cn.commit();
      } catch (Exception ex) {
        cn.rollback(); throw ex;
      } finally {
        cn.setAutoCommit(true);
      }
    }
  }


  public void removeItem(long userId, long idItem) throws Exception {
    long cartId = ensureCartForUser(userId);
    try (Connection cn = getConn();
         PreparedStatement ps = cn.prepareStatement(
           "DELETE FROM AEROLINEA.CARRITO_ITEM WHERE ID_ITEM = ? AND ID_CARRITO = ?")) {
      ps.setLong(1, idItem);
      ps.setLong(2, cartId);
      ps.executeUpdate();
    }
  }

  public long checkout(long userId) throws Exception {
    long cartId = ensureCartForUser(userId);
    try (Connection cn = getConn()) {
      try (PreparedStatement ps = cn.prepareStatement(
          "SELECT COUNT(*) FROM AEROLINEA.CARRITO_ITEM WHERE ID_CARRITO = ?")) {
        ps.setLong(1, cartId);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next() && rs.getInt(1) == 0) {
            throw new IllegalStateException("El carrito está vacío o ya fue procesado.");
          }
        }
      }
      try (CallableStatement cs = cn.prepareCall("{ call AEROLINEA.PR_CHECKOUT_CARRITO(?,?,?) }")) {
        cs.setLong(1, userId);
        cs.setLong(2, cartId);
        cs.registerOutParameter(3, java.sql.Types.NUMERIC);
        cs.execute();
        return cs.getLong(3);
      }
    }
  }

  public List<ReservaListItem> listReservasByUser(long userId) throws Exception {
    var out = new ArrayList<ReservaListItem>();
    try (Connection cn = getConn();
         PreparedStatement ps = cn.prepareStatement(
           "SELECT ID_RESERVA, ID_USUARIO, ID_ESTADO, TOTAL, CREADA_EN, CODIGO " +
           "FROM AEROLINEA.RESERVA WHERE ID_USUARIO = ? " +
           "ORDER BY ID_RESERVA DESC")) {
      ps.setLong(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          var r = new ReservaListItem();
          r.idReserva = rs.getLong("ID_RESERVA");
          r.idUsuario = rs.getLong("ID_USUARIO");
          r.idEstado  = rs.getInt("ID_ESTADO");
          r.total     = rs.getBigDecimal("TOTAL");
          Timestamp ts = rs.getTimestamp("CREADA_EN");
          r.creadaEn  = ts != null ? ts.toInstant().toString() : null;
          r.codigo = rs.getString("CODIGO");
          out.add(r);
        }
      }
    }
    return out;
  }
  public ReservaDetalle getReservaDetalle(long userId, long idReserva) throws Exception {
  ReservaDetalle det = new ReservaDetalle();
  det.items = new ArrayList<>();

  try (Connection cn = getConn()) {
    try (PreparedStatement ps = cn.prepareStatement(
         "SELECT ID_USUARIO, ID_ESTADO, TOTAL, CREADA_EN, CODIGO " +
         "FROM AEROLINEA.RESERVA WHERE ID_RESERVA = ?")) {
      ps.setLong(1, idReserva);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) throw new IllegalArgumentException("Reserva no encontrada.");
        long owner = rs.getLong("ID_USUARIO");
        if (owner != userId) throw new IllegalStateException("No autorizado.");
        det.idReserva = idReserva;
        det.idUsuario = owner;
        det.idEstado  = rs.getInt("ID_ESTADO");
        det.total     = rs.getBigDecimal("TOTAL");
        Timestamp ts = rs.getTimestamp("CREADA_EN");
        det.creadaEn  = ts != null ? ts.toInstant().toString() : null;
        det.codigo    = rs.getString("CODIGO");
      }
    }

    String SQL_DET =
      """
      SELECT
          (ri.ID_VUELO * 1000 + ri.ID_CLASE)                 AS ID_ITEM,
          ri.ID_VUELO,
          v.CODIGO                                           AS CODIGO_VUELO,
          v.FECHA_SALIDA,
          v.FECHA_LLEGADA,
          ri.ID_CLASE,
          ca.NOMBRE                                          AS NOMBRE_CLASE,

          COUNT(*)                                           AS CANTIDAD,
          MIN(ri.PRECIO_UNITARIO)                            AS PRECIO_UNITARIO,
          SUM(ri.PRECIO_UNITARIO)                            AS SUBTOTAL,

          -- Ruta principal (origen/destino)
          po.NOMBRE                                          AS PAIS_ORIGEN,
          pd.NOMBRE                                          AS PAIS_DESTINO,
          co.NOMBRE                                          AS CIUDAD_ORIGEN,
          cd.NOMBRE                                          AS CIUDAD_DESTINO,

          -- Escala (si existe; 0..1 en tu modelo)
          MIN(cesc.NOMBRE)                                   AS ESCALA_CIUDAD,
          MIN(pesc.NOMBRE)                                   AS ESCALA_PAIS,
          MIN(ve.LLEGADA)                                    AS ESCALA_LLEGADA,
          MIN(ve.SALIDA)                                     AS ESCALA_SALIDA,

          -- Regreso (vuelo pareja, si existe)
          MIN(vp.CODIGO)                                     AS REGRESO_CODIGO,
          MIN(vp.FECHA_SALIDA)                               AS REGRESO_SALIDA,
          MIN(vp.FECHA_LLEGADA)                              AS REGRESO_LLEGADA,
          MIN(corp.NOMBRE)                                   AS REGRESO_CIUDAD_ORIGEN,
          MIN(pop.NOMBRE)                                    AS REGRESO_PAIS_ORIGEN,
          MIN(cdp.NOMBRE)                                    AS REGRESO_CIUDAD_DESTINO,
          MIN(pdp.NOMBRE)                                    AS REGRESO_PAIS_DESTINO

      FROM AEROLINEA.RESERVA_ITEM ri
      JOIN AEROLINEA.VUELO v            ON v.ID_VUELO = ri.ID_VUELO
      JOIN AEROLINEA.CLASE_ASIENTO ca   ON ca.ID_CLASE = ri.ID_CLASE
      JOIN AEROLINEA.RUTA r             ON r.ID_RUTA = v.ID_RUTA
      JOIN AEROLINEA.CIUDAD co          ON co.ID_CIUDAD = r.ID_CIUDAD_ORIGEN
      JOIN AEROLINEA.CIUDAD cd          ON cd.ID_CIUDAD = r.ID_CIUDAD_DESTINO
      JOIN AEROLINEA.PAIS po            ON po.ID_PAIS = co.ID_PAIS
      JOIN AEROLINEA.PAIS pd            ON pd.ID_PAIS = cd.ID_PAIS

      -- Escala de la salida (0..1)
      LEFT JOIN AEROLINEA.VUELO_ESCALA ve ON ve.ID_VUELO = v.ID_VUELO
      LEFT JOIN AEROLINEA.CIUDAD cesc      ON cesc.ID_CIUDAD = ve.ID_CIUDAD
      LEFT JOIN AEROLINEA.PAIS pesc        ON pesc.ID_PAIS   = cesc.ID_PAIS

      -- Vuelo pareja (regreso)
      LEFT JOIN AEROLINEA.VUELO vp        ON vp.ID_VUELO = v.ID_VUELO_PAREJA
      LEFT JOIN AEROLINEA.RUTA rp         ON rp.ID_RUTA = vp.ID_RUTA
      LEFT JOIN AEROLINEA.CIUDAD corp     ON corp.ID_CIUDAD = rp.ID_CIUDAD_ORIGEN
      LEFT JOIN AEROLINEA.CIUDAD cdp      ON cdp.ID_CIUDAD  = rp.ID_CIUDAD_DESTINO
      LEFT JOIN AEROLINEA.PAIS pop        ON pop.ID_PAIS    = corp.ID_PAIS
      LEFT JOIN AEROLINEA.PAIS pdp        ON pdp.ID_PAIS    = cdp.ID_PAIS

      WHERE ri.ID_RESERVA = ?
      GROUP BY
          ri.ID_VUELO, v.CODIGO, v.FECHA_SALIDA, v.FECHA_LLEGADA,
          ri.ID_CLASE, ca.NOMBRE,
          po.NOMBRE, pd.NOMBRE, co.NOMBRE, cd.NOMBRE
      ORDER BY v.FECHA_SALIDA, ri.ID_CLASE
      """;

    try (PreparedStatement ps = cn.prepareStatement(SQL_DET)) {
      ps.setLong(1, idReserva);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          ReservaItem it = new ReservaItem();
          it.idItem         = rs.getLong("ID_ITEM");
          it.idVuelo        = rs.getLong("ID_VUELO");
          it.codigoVuelo    = rs.getString("CODIGO_VUELO");
          Timestamp ts1     = rs.getTimestamp("FECHA_SALIDA");
          Timestamp ts2     = rs.getTimestamp("FECHA_LLEGADA");
          it.fechaSalida    = ts1 != null ? ts1.toInstant().toString() : null;
          it.fechaLlegada   = ts2 != null ? ts2.toInstant().toString() : null;
          it.idClase        = rs.getInt("ID_CLASE");
          it.clase          = rs.getString("NOMBRE_CLASE");
          it.cantidad       = rs.getInt("CANTIDAD");
          it.precioUnitario = rs.getBigDecimal("PRECIO_UNITARIO");
          it.subtotal       = rs.getBigDecimal("SUBTOTAL");

          setFieldIfExists(it, "paisOrigen",    rs.getString("PAIS_ORIGEN"));
          setFieldIfExists(it, "paisDestino",   rs.getString("PAIS_DESTINO"));
          setFieldIfExists(it, "ciudadOrigen",  rs.getString("CIUDAD_ORIGEN"));
          setFieldIfExists(it, "ciudadDestino", rs.getString("CIUDAD_DESTINO"));

          setFieldIfExists(it, "escalaCiudad",  rs.getString("ESCALA_CIUDAD"));
          setFieldIfExists(it, "escalaPais",    rs.getString("ESCALA_PAIS"));
          Timestamp esl = rs.getTimestamp("ESCALA_LLEGADA");
          Timestamp ess = rs.getTimestamp("ESCALA_SALIDA");
          setFieldIfExists(it, "escalaLlegada", esl != null ? esl.toInstant().toString() : null);

          setFieldIfExists(it, "escalaSalida",  ess != null ? ess.toInstant().toString() : null);
          setFieldIfExists(it, "regresoCodigo",          rs.getString("REGRESO_CODIGO"));
          Timestamp rsl = rs.getTimestamp("REGRESO_SALIDA");
          Timestamp rll = rs.getTimestamp("REGRESO_LLEGADA");
          setFieldIfExists(it, "regresoFechaSalida",          rsl != null ? rsl.toInstant().toString() : null);
          setFieldIfExists(it, "regresoFechaLlegada",         rll != null ? rll.toInstant().toString() : null);
          setFieldIfExists(it, "regresoCiudadOrigen",    rs.getString("REGRESO_CIUDAD_ORIGEN"));
          setFieldIfExists(it, "regresoPaisOrigen",      rs.getString("REGRESO_PAIS_ORIGEN"));
          setFieldIfExists(it, "regresoCiudadDestino",   rs.getString("REGRESO_CIUDAD_DESTINO"));
          setFieldIfExists(it, "regresoPaisDestino",     rs.getString("REGRESO_PAIS_DESTINO"));

          det.items.add(it);
        }
      }
    }
  }

  return det;
}


}
