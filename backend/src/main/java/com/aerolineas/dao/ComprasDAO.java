package com.aerolineas.dao;

import com.aerolineas.config.DB;
import java.sql.*;
import java.util.*;
import java.math.BigDecimal;

import com.aerolineas.dto.CompraDTO;
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

  private Long getParejaId(Connection cn, long idVuelo) throws SQLException {
    String vueloTable = DB.table("VUELO");
    try (PreparedStatement ps = cn.prepareStatement(
        "SELECT ID_VUELO_PAREJA FROM " + vueloTable + " WHERE ID_VUELO=?")) {
      ps.setLong(1, idVuelo);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          long v = rs.getLong(1);
          return rs.wasNull() ? null : v;
        }
      }
    }
    return null;
  }

  private void validarVueloDisponible(Connection cn, long idVuelo) throws SQLException {
    String vueloTable = DB.table("VUELO");
    String estadosTable = DB.table("ESTADOS");
    try (PreparedStatement ps = cn.prepareStatement(
        "SELECT NVL(v.ACTIVO,1) AS ACTIVO, UPPER(e.ESTADO) AS ESTADO " +
        "FROM " + vueloTable + " v JOIN " + estadosTable + " e ON e.ID_ESTADO=v.ID_ESTADO " +
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
  }

  private static class ClaseInfo {
    int cupoTotal;
    BigDecimal precio;
  }

  private ClaseInfo getClaseInfo(Connection cn, long idVuelo, int idClase, boolean forUpdate) throws SQLException {
    String salidaClaseTable = DB.table("SALIDA_CLASE");
    String vueloClaseTable = DB.table("VUELO_CLASE");
    String sql = "SELECT CUPO_TOTAL, PRECIO FROM " + salidaClaseTable + " WHERE ID_VUELO = ? AND ID_CLASE = ?"
               + (forUpdate ? " FOR UPDATE" : "");
    try (PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setLong(1, idVuelo);
      ps.setInt(2, idClase);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          ClaseInfo ci = new ClaseInfo();
          ci.cupoTotal = rs.getInt("CUPO_TOTAL");
          ci.precio = rs.getBigDecimal("PRECIO");
          if (ci.precio == null) {
            try (PreparedStatement ps2 = cn.prepareStatement(
                "SELECT PRECIO FROM " + vueloClaseTable + " WHERE ID_VUELO = ? AND ID_CLASE = ?")) {
              ps2.setLong(1, idVuelo);
              ps2.setInt(2, idClase);
              try (ResultSet rs2 = ps2.executeQuery()) {
                if (rs2.next()) ci.precio = rs2.getBigDecimal(1);
              }
            }
          }
          return ci;
        }
      }
    }
    return null;
  }

  private int getReservados(Connection cn, long idVuelo, int idClase) throws SQLException {
    String reservaItemTable = DB.table("RESERVA_ITEM");
    String reservaTable = DB.table("RESERVA");
    try (PreparedStatement ps = cn.prepareStatement(
        "SELECT NVL(COUNT(*),0) FROM " + reservaItemTable + " ri " +
        "JOIN " + reservaTable + " r ON r.ID_RESERVA = ri.ID_RESERVA " +
        "WHERE ri.ID_VUELO=? AND ri.ID_CLASE=? AND r.ID_ESTADO=1")) {
      ps.setLong(1, idVuelo);
      ps.setInt(2, idClase);
      try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
    }
    return 0;
  }

  private int getEnCarritos(Connection cn, long idVuelo, int idClase) throws SQLException {
    String carritoItemTable = DB.table("CARRITO_ITEM");
    try (PreparedStatement ps = cn.prepareStatement(
        "SELECT NVL(SUM(CANTIDAD),0) FROM " + carritoItemTable + " WHERE ID_VUELO=? AND ID_CLASE=?")) {
      ps.setLong(1, idVuelo);
      ps.setInt(2, idClase);
      try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
    }
    return 0;
  }

  private Long findCartItemIdFor(Connection cn, long cartId, long idVuelo, int idClase) throws SQLException {
    String carritoItemTable = DB.table("CARRITO_ITEM");
    try (PreparedStatement ps = cn.prepareStatement(
        "SELECT ID_ITEM FROM " + carritoItemTable + " WHERE ID_CARRITO=? AND ID_VUELO=? AND ID_CLASE=?")) {
      ps.setLong(1, cartId);
      ps.setLong(2, idVuelo);
      ps.setInt(3, idClase);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return rs.getLong(1);
      }
    }
    return null;
  }

  public long ensureCartForUser(long userId) throws Exception {
    String carritoTable = DB.table("CARRITO");
    try (Connection cn = getConn()) {
      try (PreparedStatement ps = cn.prepareStatement(
          "SELECT ID_CARRITO FROM " + carritoTable + " WHERE ID_USUARIO = ?")) {
        ps.setLong(1, userId);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) return rs.getLong(1);
        }
      }
      try (PreparedStatement ps = cn.prepareStatement(
          "INSERT INTO " + carritoTable + "(ID_USUARIO) VALUES (?)")) {
        ps.setLong(1, userId);
        ps.executeUpdate();
      }
      try (PreparedStatement ps = cn.prepareStatement(
          "SELECT ID_CARRITO FROM " + carritoTable + " WHERE ID_USUARIO = ?")) {
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
      String vwCarritoTable = DB.table("VW_CARRITO_RESUMEN");
      try (PreparedStatement ps = cn.prepareStatement(
          "SELECT ID_CARRITO, ID_USUARIO, FECHA_CREACION, TOTAL " +
          "FROM " + vwCarritoTable + " WHERE ID_CARRITO = ?")) {
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

      String carritoItemTable = DB.table("CARRITO_ITEM");
      String vueloTable = DB.table("VUELO");
      String claseTable = DB.table("CLASE_ASIENTO");
      String rutaTable = DB.table("RUTA");
      String ciudadTable = DB.table("CIUDAD");
      String paisTable = DB.table("PAIS");
      String SQL_ITEMS =
        "SELECT ci.ID_ITEM, ci.ID_CARRITO, ci.ID_VUELO, v.CODIGO AS CODIGO_VUELO, " +
        "       v.FECHA_SALIDA, v.FECHA_LLEGADA, " +
        "       ci.ID_CLASE, ca.NOMBRE AS NOMBRE_CLASE, " +
        "       ci.CANTIDAD, ci.PRECIO_UNITARIO, (ci.CANTIDAD*ci.PRECIO_UNITARIO) AS SUBTOTAL, " +
        "       po.NOMBRE AS PAIS_ORIGEN, pd.NOMBRE AS PAIS_DESTINO, " +
        "       co.NOMBRE AS CIUDAD_ORIGEN, cd.NOMBRE AS CIUDAD_DESTINO " +
        "FROM " + carritoItemTable + " ci " +
        "JOIN " + vueloTable + " v ON v.ID_VUELO = ci.ID_VUELO " +
        "JOIN " + claseTable + " ca ON ca.ID_CLASE = ci.ID_CLASE " +
        "JOIN " + rutaTable + " r ON r.ID_RUTA = v.ID_RUTA " +
        "JOIN " + ciudadTable + " co ON co.ID_CIUDAD = r.ID_CIUDAD_ORIGEN " +
        "JOIN " + ciudadTable + " cd ON cd.ID_CIUDAD = r.ID_CIUDAD_DESTINO " +
        "JOIN " + paisTable + " po ON po.ID_PAIS = co.ID_PAIS " +
        "JOIN " + paisTable + " pd ON pd.ID_PAIS = cd.ID_PAIS " +
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
            it.codigoVuelo = rs.getString("CODIGO_VUELO");

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
    addOrIncrementItem(userId, idVuelo, idClase, cantidad, false);
  }

  public void addOrIncrementItem(long userId, long idVuelo, int idClase, int cantidad, boolean incluirPareja) throws Exception {
    if (cantidad <= 0) cantidad = 1;
    long cartId = ensureCartForUser(userId);

    try (Connection cn = getConn()) {
      cn.setAutoCommit(false);
      try {

        validarVueloDisponible(cn, idVuelo);
        ClaseInfo info = getClaseInfo(cn, idVuelo, idClase, true);
        if (info == null) throw new SQLException("Clase no disponible para esta salida");
        if (info.precio == null) throw new SQLException("No se encontró precio para la clase/vuelo.");

        int dispBase = info.cupoTotal - getReservados(cn, idVuelo, idClase) - getEnCarritos(cn, idVuelo, idClase);
        if (dispBase < cantidad) throw new SQLException("Cupo insuficiente: quedan " + dispBase);

        Long parejaId = incluirPareja ? getParejaId(cn, idVuelo) : null;


        BigDecimal precioPareja = null;
        if (parejaId != null) {
          validarVueloDisponible(cn, parejaId);
          ClaseInfo infoP = getClaseInfo(cn, parejaId, idClase, true);
          if (infoP == null) throw new SQLException("Clase no disponible en el regreso");
          int dispP = infoP.cupoTotal - getReservados(cn, parejaId, idClase) - getEnCarritos(cn, parejaId, idClase);
          if (dispP < cantidad) throw new SQLException("Cupo insuficiente en regreso: quedan " + dispP);
          precioPareja = infoP.precio;
          if (precioPareja == null) throw new SQLException("No se encontró precio en regreso para la clase/vuelo.");
        }


        String carritoItemTable = DB.table("CARRITO_ITEM");
        try (PreparedStatement ps = cn.prepareStatement(
            "MERGE INTO " + carritoItemTable + " t " +
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
          ps.setBigDecimal(6, info.precio);
          ps.executeUpdate();
        }


        if (parejaId != null) {
          String carritoItemTable2 = DB.table("CARRITO_ITEM");
          try (PreparedStatement ps = cn.prepareStatement(
              "MERGE INTO " + carritoItemTable2 + " t " +
              "USING (SELECT ? idc, ? idv, ? idcl FROM dual) s " +
              "ON (t.ID_CARRITO = s.idc AND t.ID_VUELO = s.idv AND t.ID_CLASE = s.idcl) " +
              "WHEN MATCHED THEN UPDATE SET t.CANTIDAD = t.CANTIDAD + ? " +
              "WHEN NOT MATCHED THEN INSERT (ID_CARRITO, ID_VUELO, ID_CLASE, CANTIDAD, PRECIO_UNITARIO) " +
              "VALUES (s.idc, s.idv, s.idcl, ?, ?)")) {
            ps.setLong(1, cartId);
            ps.setLong(2, parejaId);
            ps.setInt(3, idClase);
            ps.setInt(4, cantidad);
            ps.setInt(5, cantidad);
            ps.setBigDecimal(6, precioPareja);
            ps.executeUpdate();
          }
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
    updateQuantity(userId, idItem, cantidad, false);
  }

  public void updateQuantity(long userId, long idItem, int cantidad, boolean syncPareja) throws Exception {
    if (cantidad <= 0) throw new IllegalArgumentException("Cantidad debe ser > 0");
    long cartId = ensureCartForUser(userId);

    try (Connection cn = getConn()) {
      cn.setAutoCommit(false);
      try {

        long idVuelo; int idClase; int cantActual;
        String carritoItemTable = DB.table("CARRITO_ITEM");
        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT ID_VUELO, ID_CLASE, CANTIDAD FROM " + carritoItemTable + " " +
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

        boolean aumentando = cantidad > cantActual;

        Long parejaId = null, parejaItemId = null;
        if (syncPareja) {
          parejaId = getParejaId(cn, idVuelo);
          if (parejaId != null) {
            parejaItemId = findCartItemIdFor(cn, cartId, parejaId, idClase);
          }
        }


        if (!aumentando) {
          try (PreparedStatement ps = cn.prepareStatement(
              "UPDATE " + carritoItemTable + " SET CANTIDAD = ? WHERE ID_ITEM = ? AND ID_CARRITO = ?")) {
            ps.setInt(1, cantidad);
            ps.setLong(2, idItem);
            ps.setLong(3, cartId);
            ps.executeUpdate();
          }
          if (syncPareja && parejaItemId != null) {
            try (PreparedStatement ps = cn.prepareStatement(
                "UPDATE " + carritoItemTable + " SET CANTIDAD = ? WHERE ID_ITEM = ? AND ID_CARRITO = ?")) {
              ps.setInt(1, cantidad);
              ps.setLong(2, parejaItemId);
              ps.setLong(3, cartId);
              ps.executeUpdate();
            }
          }
          cn.commit();
          return;
        }


        validarVueloDisponible(cn, idVuelo);
        ClaseInfo info = getClaseInfo(cn, idVuelo, idClase, true);
        if (info == null) throw new SQLException("Clase no disponible para esta salida");

        int delta = cantidad - cantActual;
        int disp = info.cupoTotal - getReservados(cn, idVuelo, idClase) - getEnCarritos(cn, idVuelo, idClase);
        if (disp < delta) throw new SQLException("Cupo insuficiente: puedes subir hasta " + (cantActual + disp));

        if (syncPareja && parejaId != null && parejaItemId != null) {
          validarVueloDisponible(cn, parejaId);
          ClaseInfo infoP = getClaseInfo(cn, parejaId, idClase, true);
          if (infoP == null) throw new SQLException("Clase no disponible en el regreso");

          int cantActualP;
          try (PreparedStatement ps = cn.prepareStatement(
              "SELECT CANTIDAD FROM " + carritoItemTable + " WHERE ID_ITEM=? AND ID_CARRITO=?")) {
            ps.setLong(1, parejaItemId);
            ps.setLong(2, cartId);
            try (ResultSet rs = ps.executeQuery()) { rs.next(); cantActualP = rs.getInt(1); }
          }
          int deltaP = cantidad - cantActualP;
          if (deltaP > 0) {
            int dispP = infoP.cupoTotal - getReservados(cn, parejaId, idClase) - getEnCarritos(cn, parejaId, idClase);
            if (dispP < deltaP) throw new SQLException("Cupo insuficiente en regreso: puedes subir hasta " + (cantActualP + dispP));
          }
        }


        try (PreparedStatement ps = cn.prepareStatement(
            "UPDATE " + carritoItemTable + " SET CANTIDAD = ? WHERE ID_ITEM = ? AND ID_CARRITO = ?")) {
          ps.setInt(1, cantidad);
          ps.setLong(2, idItem);
          ps.setLong(3, cartId);
          ps.executeUpdate();
        }
        if (syncPareja && parejaItemId != null) {
          try (PreparedStatement ps = cn.prepareStatement(
              "UPDATE " + carritoItemTable + " SET CANTIDAD = ? WHERE ID_ITEM = ? AND ID_CARRITO = ?")) {
            ps.setInt(1, cantidad);
            ps.setLong(2, parejaItemId);
            ps.setLong(3, cartId);
            ps.executeUpdate();
          }
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
    removeItem(userId, idItem, false);
  }

  public void removeItem(long userId, long idItem, boolean syncPareja) throws Exception {
    long cartId = ensureCartForUser(userId);
    try (Connection cn = getConn()) {
      cn.setAutoCommit(false);
      try {
        long idVuelo; int idClase;
        String carritoItemTable = DB.table("CARRITO_ITEM");
        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT ID_VUELO, ID_CLASE FROM " + carritoItemTable + " WHERE ID_ITEM = ? AND ID_CARRITO = ?")) {
          ps.setLong(1, idItem);
          ps.setLong(2, cartId);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new SQLException("Item no encontrado.");
            idVuelo = rs.getLong(1);
            idClase = rs.getInt(2);
          }
        }

        Long parejaItemId = null;
        if (syncPareja) {
          Long parejaId = getParejaId(cn, idVuelo);
          if (parejaId != null) {
            parejaItemId = findCartItemIdFor(cn, cartId, parejaId, idClase);
          }
        }

        try (PreparedStatement ps = cn.prepareStatement(
            "DELETE FROM " + carritoItemTable + " WHERE ID_ITEM = ? AND ID_CARRITO = ?")) {
          ps.setLong(1, idItem);
          ps.setLong(2, cartId);
          ps.executeUpdate();
        }

        if (syncPareja && parejaItemId != null) {
          try (PreparedStatement ps = cn.prepareStatement(
              "DELETE FROM " + carritoItemTable + " WHERE ID_ITEM = ? AND ID_CARRITO = ?")) {
            ps.setLong(1, parejaItemId);
            ps.setLong(2, cartId);
            ps.executeUpdate();
          }
        }

        cn.commit();
      } catch (Exception ex) {
        cn.rollback(); throw ex;
      } finally {
        cn.setAutoCommit(true);
      }
    }
  }

  public long checkout(long userId) throws Exception {
    long cartId = ensureCartForUser(userId);
    try (Connection cn = getConn()) {
      String carritoItemTable = DB.table("CARRITO_ITEM");
      try (PreparedStatement ps = cn.prepareStatement(
          "SELECT COUNT(*) FROM " + carritoItemTable + " WHERE ID_CARRITO = ?")) {
        ps.setLong(1, cartId);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next() && rs.getInt(1) == 0) {
            throw new IllegalStateException("El carrito está vacío o ya fue procesado.");
          }
        }
      }
      long idGenerado;
      String schema = DB.getSchema();
      try (CallableStatement cs = cn.prepareCall("{ call " + schema + ".PR_CHECKOUT_CARRITO(?,?,?) }")) {
        cs.setLong(1, userId);
        cs.setLong(2, cartId);
        cs.registerOutParameter(3, java.sql.Types.NUMERIC);
        cs.execute();
        idGenerado = cs.getLong(3);
      }
      return idGenerado;
    }
  }

  public long checkoutConClienteFinal(long userIdWebService, long userIdClienteFinal) throws Exception {
    long cartIdWebService = ensureCartForUser(userIdWebService);
    try (Connection cn = getConn()) {
      String carritoItemTable = DB.table("CARRITO_ITEM");
      
      
      int itemCount = 0;
      try (PreparedStatement ps = cn.prepareStatement(
          "SELECT COUNT(*) FROM " + carritoItemTable + " WHERE ID_CARRITO = ?")) {
        ps.setLong(1, cartIdWebService);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            itemCount = rs.getInt(1);
            if (itemCount == 0) {
              throw new IllegalStateException("El carrito está vacío o ya fue procesado.");
            }
          }
        }
      }
      
      
      long cartIdClienteFinal = crearCarritoClienteFinal(cn, cartIdWebService, userIdClienteFinal);
      System.out.println("[Checkout] Carrito creado para cliente final " + userIdClienteFinal + " con " + itemCount + " items");
      
      
      long idGenerado;
      String schema = DB.getSchema();
      try (CallableStatement cs = cn.prepareCall("{ call " + schema + ".PR_CHECKOUT_CARRITO(?,?,?) }")) {
        cs.setLong(1, userIdClienteFinal);
        cs.setLong(2, cartIdClienteFinal);
        cs.registerOutParameter(3, java.sql.Types.NUMERIC);
        cs.execute();
        idGenerado = cs.getLong(3);
      }
      return idGenerado;
    }
  }

  private long crearCarritoClienteFinal(Connection cn, long cartIdWebService, long userIdClienteFinal) throws Exception {
    String carritoTable = DB.table("CARRITO");
    String carritoItemTable = DB.table("CARRITO_ITEM");
    
    
    long cartIdClienteFinal;
    try (PreparedStatement ps = cn.prepareStatement(
        "SELECT ID_CARRITO FROM " + carritoTable + " WHERE ID_USUARIO = ?")) {
      ps.setLong(1, userIdClienteFinal);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          cartIdClienteFinal = rs.getLong(1);
          
          try (PreparedStatement del = cn.prepareStatement(
              "DELETE FROM " + carritoItemTable + " WHERE ID_CARRITO = ?")) {
            del.setLong(1, cartIdClienteFinal);
            del.executeUpdate();
          }
        } else {
          
          try (PreparedStatement ins = cn.prepareStatement(
              "INSERT INTO " + carritoTable + " (ID_USUARIO) VALUES (?)")) {
            ins.setLong(1, userIdClienteFinal);
            ins.executeUpdate();
          }
          
          try (PreparedStatement sel = cn.prepareStatement(
              "SELECT ID_CARRITO FROM " + carritoTable + " WHERE ID_USUARIO = ?")) {
            sel.setLong(1, userIdClienteFinal);
            try (ResultSet rs2 = sel.executeQuery()) {
              if (rs2.next()) {
                cartIdClienteFinal = rs2.getLong(1);
              } else {
                throw new SQLException("No se pudo obtener el ID del carrito creado para el cliente final");
              }
            }
          }
        }
      }
    }
    
    try (PreparedStatement ps = cn.prepareStatement(
        "MERGE INTO " + carritoItemTable + " t " +
        "USING (SELECT ? idc, ID_VUELO idv, ID_CLASE idcl, CANTIDAD cant, PRECIO_UNITARIO precio " +
        "       FROM " + carritoItemTable + " WHERE ID_CARRITO = ?) s " +
        "ON (t.ID_CARRITO = s.idc AND t.ID_VUELO = s.idv AND t.ID_CLASE = s.idcl) " +
        "WHEN MATCHED THEN UPDATE SET t.CANTIDAD = s.cant, t.PRECIO_UNITARIO = s.precio " +
        "WHEN NOT MATCHED THEN INSERT (ID_CARRITO, ID_VUELO, ID_CLASE, CANTIDAD, PRECIO_UNITARIO) " +
        "VALUES (s.idc, s.idv, s.idcl, s.cant, s.precio)")) {
      ps.setLong(1, cartIdClienteFinal);
      ps.setLong(2, cartIdWebService);
      int itemsCopiados = ps.executeUpdate();
      System.out.println("[Checkout] " + itemsCopiados + " items copiados/actualizados en el carrito del cliente final");
    }
    
    return cartIdClienteFinal;
  }

  public void guardarReservaWebService(long idReserva, long idUsuarioWebService) throws Exception {
    String tabla = DB.table("RESERVA_WEBSERVICE");
    String sql = "INSERT INTO " + tabla + " (ID_RESERVA, ID_USUARIO_WEBSERVICE) VALUES (?, ?)";
    try (Connection cn = getConn();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setLong(1, idReserva);
      ps.setLong(2, idUsuarioWebService);
      ps.executeUpdate();
    }
  }

  public List<ReservaListItem> listReservasByUser(long userId) throws Exception {
    var out = new ArrayList<ReservaListItem>();
    String reservaTable = DB.table("RESERVA");
    try (Connection cn = getConn();
         PreparedStatement ps = cn.prepareStatement(
           "SELECT ID_RESERVA, ID_USUARIO, ID_ESTADO, TOTAL, CREADA_EN, CODIGO " +
           "FROM " + reservaTable + " WHERE ID_USUARIO = ? " +
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
          r.codigo    = rs.getString("CODIGO");
          out.add(r);
        }
      }
    }
    return out;
  }

  public CompraDTO.ReservaDetalle getReservaDetalle(long userId, long idReserva) throws Exception {
  ReservaDetalle det = new ReservaDetalle();
  det.items = new ArrayList<>();

  try (Connection cn = getConn()) {
    String reservaTable = DB.table("RESERVA");
    String usuarioTable = DB.table("USUARIO");
    try (PreparedStatement ps = cn.prepareStatement(
         "SELECT r.ID_USUARIO, r.ID_ESTADO, r.TOTAL, r.CREADA_EN, r.CODIGO, " +
         "       u.NOMBRES, u.APELLIDOS, u.EMAIL " +
         "FROM " + reservaTable + " r " +
         "JOIN " + usuarioTable + " u ON u.ID_USUARIO = r.ID_USUARIO " +
         "WHERE r.ID_RESERVA = ?")) {
      ps.setLong(1, idReserva);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) throw new IllegalArgumentException("Reserva no encontrada.");
        long owner = rs.getLong("ID_USUARIO");
        if (owner != userId) throw new IllegalStateException("No autorizado.");
        det.idReserva = idReserva;
        det.idUsuario = owner;
        det.idEstado  = rs.getInt("ID_ESTADO");
        det.total     = rs.getBigDecimal("TOTAL");
        Timestamp ts  = rs.getTimestamp("CREADA_EN");
        det.creadaEn  = ts != null ? ts.toInstant().toString() : null;
        det.codigo    = rs.getString("CODIGO");

        String nombres   = rs.getString("NOMBRES");
        String apellidos = rs.getString("APELLIDOS");
        String email     = rs.getString("EMAIL");
        det.compradorNombre = ((nombres == null ? "" : nombres) + " " +
                               (apellidos == null ? "" : apellidos)).trim();
        det.compradorEmail  = email;
      }
    }

    String reservaItemTable = DB.table("RESERVA_ITEM");
    String vueloTable = DB.table("VUELO");
    String claseTable = DB.table("CLASE_ASIENTO");
    String rutaTable = DB.table("RUTA");
    String ciudadTable = DB.table("CIUDAD");
    String paisTable = DB.table("PAIS");
    final String SQL_DET =
      "SELECT (ri.ID_VUELO * 1000 + ri.ID_CLASE) AS ID_ITEM, ri.ID_VUELO, v.CODIGO AS CODIGO_VUELO, v.FECHA_SALIDA, v.FECHA_LLEGADA, ri.ID_CLASE, ca.NOMBRE AS NOMBRE_CLASE, COUNT(*) AS CANTIDAD, MIN(ri.PRECIO_UNITARIO) AS PRECIO_UNITARIO, SUM(ri.PRECIO_UNITARIO) AS SUBTOTAL, po.NOMBRE AS PAIS_ORIGEN, pd.NOMBRE AS PAIS_DESTINO, co.NOMBRE AS CIUDAD_ORIGEN, cd.NOMBRE AS CIUDAD_DESTINO, MIN(vp.CODIGO) AS REGRESO_CODIGO, MIN(vp.FECHA_SALIDA) AS REGRESO_SALIDA, MIN(vp.FECHA_LLEGADA) AS REGRESO_LLEGADA, MIN(corp.NOMBRE) AS REGRESO_CIUDAD_ORIGEN, MIN(pop.NOMBRE) AS REGRESO_PAIS_ORIGEN, MIN(cdp.NOMBRE) AS REGRESO_CIUDAD_DESTINO, MIN(pdp.NOMBRE) AS REGRESO_PAIS_DESTINO FROM " + reservaItemTable + " ri JOIN " + vueloTable + " v ON v.ID_VUELO = ri.ID_VUELO JOIN " + claseTable + " ca ON ca.ID_CLASE = ri.ID_CLASE JOIN " + rutaTable + " r ON r.ID_RUTA = v.ID_RUTA JOIN " + ciudadTable + " co ON co.ID_CIUDAD = r.ID_CIUDAD_ORIGEN JOIN " + ciudadTable + " cd ON cd.ID_CIUDAD = r.ID_CIUDAD_DESTINO JOIN " + paisTable + " po ON po.ID_PAIS = co.ID_PAIS JOIN " + paisTable + " pd ON pd.ID_PAIS = cd.ID_PAIS LEFT JOIN " + vueloTable + " vp ON vp.ID_VUELO = v.ID_VUELO_PAREJA LEFT JOIN " + rutaTable + " rp ON rp.ID_RUTA = vp.ID_RUTA LEFT JOIN " + ciudadTable + " corp ON corp.ID_CIUDAD = rp.ID_CIUDAD_ORIGEN LEFT JOIN " + ciudadTable + " cdp ON cdp.ID_CIUDAD = rp.ID_CIUDAD_DESTINO LEFT JOIN " + paisTable + " pop ON pop.ID_PAIS = corp.ID_PAIS LEFT JOIN " + paisTable + " pdp ON pdp.ID_PAIS = cdp.ID_PAIS WHERE ri.ID_RESERVA = ? GROUP BY ri.ID_VUELO, v.CODIGO, v.FECHA_SALIDA, v.FECHA_LLEGADA, ri.ID_CLASE, ca.NOMBRE, po.NOMBRE, pd.NOMBRE, co.NOMBRE, cd.NOMBRE ORDER BY v.FECHA_SALIDA, ri.ID_CLASE";

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

    
          setFieldIfExists(it, "regresoCodigo",        rs.getString("REGRESO_CODIGO"));
          Timestamp rsl = rs.getTimestamp("REGRESO_SALIDA");
          Timestamp rll = rs.getTimestamp("REGRESO_LLEGADA");
          setFieldIfExists(it, "regresoFechaSalida",   rsl != null ? rsl.toInstant().toString() : null);
          setFieldIfExists(it, "regresoFechaLlegada",  rll != null ? rll.toInstant().toString() : null);
          setFieldIfExists(it, "regresoCiudadOrigen",  rs.getString("REGRESO_CIUDAD_ORIGEN"));
          setFieldIfExists(it, "regresoPaisOrigen",    rs.getString("REGRESO_PAIS_ORIGEN"));
          setFieldIfExists(it, "regresoCiudadDestino", rs.getString("REGRESO_CIUDAD_DESTINO"));
          setFieldIfExists(it, "regresoPaisDestino",   rs.getString("REGRESO_PAIS_DESTINO"));

          det.items.add(it);
        }
      }
    }
  }

  return det;
}

  public List<CompraDTO.ReservaListItem> listReservasAdmin(
      String q, String usuario, String codigo, String vuelo,
      Timestamp desde, Timestamp hasta, Integer idEstado
  ) throws Exception {

    var out = new ArrayList<CompraDTO.ReservaListItem>();

    String reservaTable = DB.table("RESERVA");
    String usuarioTable = DB.table("USUARIO");
    String reservaItemTable = DB.table("RESERVA_ITEM");
    String vueloTable = DB.table("VUELO");
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT r.ID_RESERVA, r.ID_USUARIO, r.ID_ESTADO, r.TOTAL, r.CREADA_EN, r.CODIGO FROM " + reservaTable + " r JOIN " + usuarioTable + " u ON u.ID_USUARIO = r.ID_USUARIO ");

    boolean joinVuelo = (vuelo != null && !vuelo.isBlank());
    if (joinVuelo) {
      sb.append("JOIN " + reservaItemTable + " ri ON ri.ID_RESERVA = r.ID_RESERVA JOIN " + vueloTable + " v ON v.ID_VUELO = ri.ID_VUELO ");
    }

    sb.append(" WHERE 1=1 ");

    var params = new ArrayList<Object>();

    if (q != null && !q.isBlank()) {
      String like = "%" + q.trim().toLowerCase() + "%";
      sb.append(" AND ( ");
      sb.append("""
          LOWER(u.EMAIL) LIKE ? OR
          LOWER(u.NOMBRES) LIKE ? OR
          LOWER(u.APELLIDOS) LIKE ?
          """);
      params.add(like); params.add(like); params.add(like);
      try {
        long uid = Long.parseLong(q.trim());
        sb.append(" OR r.ID_USUARIO = ? ");
        params.add(uid);
      } catch (NumberFormatException ignore) {}
      sb.append(" ) ");
    }

    if (usuario != null && !usuario.isBlank()) {
      try {
        long uid = Long.parseLong(usuario.trim());
        sb.append(" AND r.ID_USUARIO = ? ");
        params.add(uid);
      } catch (NumberFormatException nfe) {
        sb.append(" AND LOWER(u.EMAIL) LIKE ? ");
        params.add("%" + usuario.trim().toLowerCase() + "%");
      }
    }

    if (codigo != null && !codigo.isBlank()) {
      sb.append(" AND LOWER(r.CODIGO) LIKE ? ");
      params.add("%" + codigo.trim().toLowerCase() + "%");
    }

    if (joinVuelo) {
      try {
        long idVuelo = Long.parseLong(vuelo.trim());
        sb.append(" AND v.ID_VUELO = ? ");
        params.add(idVuelo);
      } catch (NumberFormatException nfe) {
        sb.append(" AND LOWER(v.CODIGO) LIKE ? ");
        params.add("%" + vuelo.trim().toLowerCase() + "%");
      }
    }

    if (desde != null) { sb.append(" AND r.CREADA_EN >= ? "); params.add(desde); }
    if (hasta != null) { sb.append(" AND r.CREADA_EN <  ? "); params.add(hasta); }

    if (idEstado != null) {
      sb.append(" AND r.ID_ESTADO = ? ");
      params.add(idEstado);
    }

    sb.append(" GROUP BY r.ID_RESERVA, r.ID_USUARIO, r.ID_ESTADO, r.TOTAL, r.CREADA_EN, r.CODIGO ");
    sb.append(" ORDER BY r.ID_RESERVA DESC ");

    try (Connection cn = getConn();
         PreparedStatement ps = cn.prepareStatement(sb.toString())) {
      for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          var r = new CompraDTO.ReservaListItem();
          r.idReserva = rs.getLong("ID_RESERVA");
          r.idUsuario = rs.getLong("ID_USUARIO");
          r.idEstado  = rs.getInt("ID_ESTADO");
          r.total     = rs.getBigDecimal("TOTAL");
          Timestamp ts = rs.getTimestamp("CREADA_EN");
          r.creadaEn  = ts != null ? ts.toInstant().toString() : null;
          r.codigo    = rs.getString("CODIGO");
          out.add(r);
        }
      }
    }
    return out;
  }

  public CompraDTO.ReservaDetalle getReservaDetalleAdmin(long idReserva) throws Exception {
  CompraDTO.ReservaDetalle det = new CompraDTO.ReservaDetalle();
  det.items = new ArrayList<>();

  try (Connection cn = getConn()) {
    String reservaTable = DB.table("RESERVA");
    String usuarioTable = DB.table("USUARIO");
    try (PreparedStatement ps = cn.prepareStatement(
         "SELECT r.ID_USUARIO, r.ID_ESTADO, r.TOTAL, r.CREADA_EN, r.CODIGO, " +
         "       u.NOMBRES, u.APELLIDOS, u.EMAIL " +
         "FROM " + reservaTable + " r " +
         "JOIN " + usuarioTable + " u ON u.ID_USUARIO = r.ID_USUARIO " +
         "WHERE r.ID_RESERVA = ?")) {
      ps.setLong(1, idReserva);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) throw new IllegalArgumentException("Reserva no encontrada.");
        det.idReserva = idReserva;
        det.idUsuario = rs.getLong("ID_USUARIO");
        det.idEstado  = rs.getInt("ID_ESTADO");
        det.total     = rs.getBigDecimal("TOTAL");
        Timestamp ts  = rs.getTimestamp("CREADA_EN");
        det.creadaEn  = ts != null ? ts.toInstant().toString() : null;
        det.codigo    = rs.getString("CODIGO");

        String nombres   = rs.getString("NOMBRES");
        String apellidos = rs.getString("APELLIDOS");
        String email     = rs.getString("EMAIL");
        det.compradorNombre = ((nombres == null ? "" : nombres) + " " +
                               (apellidos == null ? "" : apellidos)).trim();
        det.compradorEmail  = email;
      }
    }

    String reservaItemTable = DB.table("RESERVA_ITEM");
    String vueloTable = DB.table("VUELO");
    String claseTable = DB.table("CLASE_ASIENTO");
    String rutaTable = DB.table("RUTA");
    String ciudadTable = DB.table("CIUDAD");
    String paisTable = DB.table("PAIS");
    final String SQL_DET =
      "SELECT (ri.ID_VUELO * 1000 + ri.ID_CLASE) AS ID_ITEM, ri.ID_VUELO, v.CODIGO AS CODIGO_VUELO, v.FECHA_SALIDA, v.FECHA_LLEGADA, ri.ID_CLASE, ca.NOMBRE AS NOMBRE_CLASE, COUNT(*) AS CANTIDAD, MIN(ri.PRECIO_UNITARIO) AS PRECIO_UNITARIO, SUM(ri.PRECIO_UNITARIO) AS SUBTOTAL, po.NOMBRE AS PAIS_ORIGEN, pd.NOMBRE AS PAIS_DESTINO, co.NOMBRE AS CIUDAD_ORIGEN, cd.NOMBRE AS CIUDAD_DESTINO, MIN(vp.CODIGO) AS REGRESO_CODIGO, MIN(vp.FECHA_SALIDA) AS REGRESO_SALIDA, MIN(vp.FECHA_LLEGADA) AS REGRESO_LLEGADA, MIN(corp.NOMBRE) AS REGRESO_CIUDAD_ORIGEN, MIN(pop.NOMBRE) AS REGRESO_PAIS_ORIGEN, MIN(cdp.NOMBRE) AS REGRESO_CIUDAD_DESTINO, MIN(pdp.NOMBRE) AS REGRESO_PAIS_DESTINO FROM " + reservaItemTable + " ri JOIN " + vueloTable + " v ON v.ID_VUELO = ri.ID_VUELO JOIN " + claseTable + " ca ON ca.ID_CLASE = ri.ID_CLASE JOIN " + rutaTable + " r ON r.ID_RUTA = v.ID_RUTA JOIN " + ciudadTable + " co ON co.ID_CIUDAD = r.ID_CIUDAD_ORIGEN JOIN " + ciudadTable + " cd ON cd.ID_CIUDAD = r.ID_CIUDAD_DESTINO JOIN " + paisTable + " po ON po.ID_PAIS = co.ID_PAIS JOIN " + paisTable + " pd ON pd.ID_PAIS = cd.ID_PAIS LEFT JOIN " + vueloTable + " vp ON vp.ID_VUELO = v.ID_VUELO_PAREJA LEFT JOIN " + rutaTable + " rp ON rp.ID_RUTA = vp.ID_RUTA LEFT JOIN " + ciudadTable + " corp ON corp.ID_CIUDAD = rp.ID_CIUDAD_ORIGEN LEFT JOIN " + ciudadTable + " cdp ON cdp.ID_CIUDAD = rp.ID_CIUDAD_DESTINO LEFT JOIN " + paisTable + " pop ON pop.ID_PAIS = corp.ID_PAIS LEFT JOIN " + paisTable + " pdp ON pdp.ID_PAIS = cdp.ID_PAIS WHERE ri.ID_RESERVA = ? GROUP BY ri.ID_VUELO, v.CODIGO, v.FECHA_SALIDA, v.FECHA_LLEGADA, ri.ID_CLASE, ca.NOMBRE, po.NOMBRE, pd.NOMBRE, co.NOMBRE, cd.NOMBRE ORDER BY v.FECHA_SALIDA, ri.ID_CLASE";

    try (PreparedStatement ps = cn.prepareStatement(SQL_DET)) {
      ps.setLong(1, idReserva);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          var it = new CompraDTO.ReservaItem();
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

          setFieldIfExists(it, "regresoCodigo",        rs.getString("REGRESO_CODIGO"));
          Timestamp rsl = rs.getTimestamp("REGRESO_SALIDA");
          Timestamp rll = rs.getTimestamp("REGRESO_LLEGADA");
          setFieldIfExists(it, "regresoFechaSalida",   rsl != null ? rsl.toInstant().toString() : null);
          setFieldIfExists(it, "regresoFechaLlegada",  rll != null ? rll.toInstant().toString() : null);
          setFieldIfExists(it, "regresoCiudadOrigen",  rs.getString("REGRESO_CIUDAD_ORIGEN"));
          setFieldIfExists(it, "regresoPaisOrigen",    rs.getString("REGRESO_PAIS_ORIGEN"));
          setFieldIfExists(it, "regresoCiudadDestino", rs.getString("REGRESO_CIUDAD_DESTINO"));
          setFieldIfExists(it, "regresoPaisDestino",   rs.getString("REGRESO_PAIS_DESTINO"));

          det.items.add(it);
        }
      }
    }
  }

  return det;
}
  
  public List<CompraDTO.EstadoReserva> listEstadosReserva() throws Exception {
    var out = new ArrayList<CompraDTO.EstadoReserva>();
    String estadoReservaTable = DB.table("ESTADO_RESERVA");
    try (Connection cn = getConn();
         PreparedStatement ps = cn.prepareStatement(
           "SELECT ID_ESTADO, ESTADO FROM " + estadoReservaTable + " ORDER BY ID_ESTADO")) {
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          var e = new CompraDTO.EstadoReserva();
          e.idEstado = rs.getInt("ID_ESTADO");
          e.nombre   = rs.getString("ESTADO");
          out.add(e);
        }
      }
    }
    return out;
  }

  public boolean cancelarReserva(long solicitanteId, long idReserva, boolean isAdmin) throws Exception {
    try (Connection cn = getConn()) {
      cn.setAutoCommit(false);
      try {
        long owner; int estado;

        String reservaTable = DB.table("RESERVA");
        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT ID_USUARIO, ID_ESTADO FROM " + reservaTable + " WHERE ID_RESERVA = ? FOR UPDATE")) {
          ps.setLong(1, idReserva);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new IllegalArgumentException("Reserva no encontrada.");
            owner  = rs.getLong(1);
            estado = rs.getInt(2);
          }
        }


        if (!isAdmin && owner != solicitanteId) {
          throw new IllegalStateException("No autorizado para cancelar esta reserva.");
        }


        if (estado != 1) {
          cn.rollback();
          return false;
        }


        String reservaItemTable = DB.table("RESERVA_ITEM");
        String salidaClaseTable = DB.table("SALIDA_CLASE");
        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT ID_VUELO, ID_CLASE, COUNT(*) AS CANT " +
            "FROM " + reservaItemTable + " WHERE ID_RESERVA = ? " +
            "GROUP BY ID_VUELO, ID_CLASE")) {
          ps.setLong(1, idReserva);
          try (ResultSet rs = ps.executeQuery()) {
            try (PreparedStatement upd = cn.prepareStatement(
                "UPDATE " + salidaClaseTable + " " +
                "SET CUPO_TOTAL = CUPO_TOTAL + ? " +
                "WHERE ID_VUELO = ? AND ID_CLASE = ?")) {
              while (rs.next()) {
                int cant = rs.getInt("CANT");
                long idVuelo = rs.getLong("ID_VUELO");
                int idClase = rs.getInt("ID_CLASE");
                upd.setInt(1, cant);
                upd.setLong(2, idVuelo);
                upd.setInt(3, idClase);
                upd.addBatch();
              }
              upd.executeBatch();
            }
          }
        }

        try (PreparedStatement ps = cn.prepareStatement(
            "UPDATE " + reservaTable + " SET ID_ESTADO = 2 WHERE ID_RESERVA = ?")) {
          ps.setLong(1, idReserva);
          ps.executeUpdate();
        }
        try (PreparedStatement ps = cn.prepareStatement(
            "UPDATE " + reservaItemTable + " SET ID_ESTADO_RESERVA = 2 WHERE ID_RESERVA = ?")) {
          ps.setLong(1, idReserva);
          ps.executeUpdate();
        }

        cn.commit();
        return true;
      } catch (Exception ex) {
        cn.rollback();
        throw ex;
      } finally {
        cn.setAutoCommit(true);
      }
    }
  }

  public void cancelarReservaUsuario(long userId, long idReserva) throws Exception {
    boolean ok = cancelarReserva(userId, idReserva, false);
    if (!ok) throw new IllegalStateException("La reserva no está en estado cancelable.");
  }

  public void cancelarReservaAdmin(long idReserva) throws Exception {
    boolean ok = cancelarReserva(0L, idReserva, true);
    if (!ok) throw new IllegalStateException("La reserva no está en estado cancelable.");
  }

  public int cancelarVueloYAfectarReservas(long idVuelo) throws Exception {
    try (Connection cn = getConn()) {
      cn.setAutoCommit(false);
      try {
        String vueloTable = DB.table("VUELO");
        String estadosTable = DB.table("ESTADOS");
        try (PreparedStatement ps = cn.prepareStatement(
            "UPDATE " + vueloTable + " SET ID_ESTADO = " +
            "(SELECT ID_ESTADO FROM " + estadosTable + " WHERE ESTADO = 'CANCELADO' FETCH FIRST 1 ROWS ONLY) " +
            "WHERE ID_VUELO = ?")) {
          ps.setLong(1, idVuelo);
          ps.executeUpdate();
        }

        List<Long> ids = new ArrayList<>();
        String reservaTable = DB.table("RESERVA");
        String reservaItemTable = DB.table("RESERVA_ITEM");
        try (PreparedStatement ps = cn.prepareStatement(
            "SELECT DISTINCT r.ID_RESERVA " +
            "FROM " + reservaTable + " r " +
            "JOIN " + reservaItemTable + " ri ON ri.ID_RESERVA = r.ID_RESERVA " +
            "WHERE r.ID_ESTADO = 1 AND ri.ID_VUELO = ?")) {
          ps.setLong(1, idVuelo);
          try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) ids.add(rs.getLong(1));
          }
        }

        int updated = 0;
        if (!ids.isEmpty()) {
          String salidaClaseTable = DB.table("SALIDA_CLASE");
          for (Long idRes : ids) {
            try (PreparedStatement ps = cn.prepareStatement(
                "SELECT ID_VUELO, ID_CLASE, COUNT(*) AS CANT " +
                "FROM " + reservaItemTable + " WHERE ID_RESERVA = ? " +
                "GROUP BY ID_VUELO, ID_CLASE")) {
              ps.setLong(1, idRes);
              try (ResultSet rs = ps.executeQuery()) {
                try (PreparedStatement upd = cn.prepareStatement(
                    "UPDATE " + salidaClaseTable + " SET CUPO_TOTAL = CUPO_TOTAL + ? WHERE ID_VUELO=? AND ID_CLASE=?")) {
                  while (rs.next()) {
                    upd.setInt(1, rs.getInt("CANT"));
                    upd.setLong(2, rs.getLong("ID_VUELO"));
                    upd.setInt(3, rs.getInt("ID_CLASE"));
                    upd.addBatch();
                  }
                  upd.executeBatch();
                }
              }
            }
          }

          String inSql = String.join(",", Collections.nCopies(ids.size(), "?"));
          try (PreparedStatement ps = cn.prepareStatement(
              "UPDATE " + reservaTable + " SET ID_ESTADO = 2 WHERE ID_RESERVA IN (" + inSql + ")")) {
            for (int i = 0; i < ids.size(); i++) ps.setLong(i + 1, ids.get(i));
            updated = ps.executeUpdate();
          }
        }

        cn.commit();
        return updated;
      } catch (Exception ex) {
        cn.rollback();
        throw ex;
      } finally {
        cn.setAutoCommit(true);
      }
    }
  }

  public List<CompraDTO.TopDestino> listTopDestinos(
    java.sql.Timestamp desde,
    java.sql.Timestamp hasta,
    int limit
    ) throws Exception {

    var out = new ArrayList<CompraDTO.TopDestino>();

    String reservaItemTable = DB.table("RESERVA_ITEM");
    String reservaTable = DB.table("RESERVA");
    String vueloTable = DB.table("VUELO");
    String rutaTable = DB.table("RUTA");
    String ciudadTable = DB.table("CIUDAD");
    String paisTable = DB.table("PAIS");
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT cd.ID_CIUDAD, cd.NOMBRE AS CIUDAD, pd.NOMBRE AS PAIS, COUNT(*) AS BOLETOS FROM " + reservaItemTable + " ri JOIN " + reservaTable + " r ON r.ID_RESERVA = ri.ID_RESERVA JOIN " + vueloTable + " v ON v.ID_VUELO = ri.ID_VUELO JOIN " + rutaTable + " ru ON ru.ID_RUTA = v.ID_RUTA JOIN " + ciudadTable + " cd ON cd.ID_CIUDAD = ru.ID_CIUDAD_DESTINO JOIN " + paisTable + " pd ON pd.ID_PAIS = cd.ID_PAIS WHERE r.ID_ESTADO = 1 ");

    var params = new ArrayList<Object>();

    if (desde != null) {
      sb.append(" AND r.CREADA_EN >= ? ");
      params.add(desde);
    }
    if (hasta != null) {
      sb.append(" AND r.CREADA_EN < ? ");
      params.add(hasta);
    }

    sb.append(" GROUP BY cd.ID_CIUDAD, cd.NOMBRE, pd.NOMBRE ");
    sb.append(" ORDER BY BOLETOS DESC ");

    if (limit > 0) {
      sb.append(" FETCH FIRST ").append(limit).append(" ROWS ONLY ");
    }

    try (Connection cn = getConn();
         PreparedStatement ps = cn.prepareStatement(sb.toString())) {

      for (int i = 0; i < params.size(); i++) {
        ps.setObject(i + 1, params.get(i));
      }

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          var d = new CompraDTO.TopDestino();
          d.idCiudadDestino = rs.getLong("ID_CIUDAD");
          d.ciudadDestino   = rs.getString("CIUDAD");
          d.paisDestino     = rs.getString("PAIS");
          d.boletos         = rs.getLong("BOLETOS");
          out.add(d);
        }
      }
    }
    return out;
  }

}
