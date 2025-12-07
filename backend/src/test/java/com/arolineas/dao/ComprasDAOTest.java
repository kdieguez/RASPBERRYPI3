package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.dto.CompraDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;


class ComprasDAOTest {

    private Object invokePrivate(ComprasDAO dao, String name, Class<?>[] types, Object... args) throws Exception {
        Method m = ComprasDAO.class.getDeclaredMethod(name, types);
        m.setAccessible(true);
        try {
            return m.invoke(dao, args);
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable target = ite.getTargetException();
            if (target instanceof Exception ex) {
                throw ex;
            }
            throw ite;
        }
    }

    @Test
    @DisplayName("getParejaId devuelve null cuando no hay vuelo pareja")
    void getParejaId_sinResultado_devuelveNull() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");

            String expectedSql = "SELECT ID_VUELO_PAREJA FROM VUELO WHERE ID_VUELO=?";
            when(cn.prepareStatement(expectedSql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(false);

            ComprasDAO dao = new ComprasDAO();
            Long pareja = (Long) invokePrivate(
                    dao,
                    "getParejaId",
                    new Class<?>[]{Connection.class, long.class},
                    cn, 123L
            );

            assertNull(pareja, "Debe devolver null cuando no hay vuelo pareja");
        }
    }

    @Test
    @DisplayName("getParejaId devuelve el id cuando existe vuelo pareja")
    void getParejaId_conResultado_devuelveId() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");

            String expectedSql = "SELECT ID_VUELO_PAREJA FROM VUELO WHERE ID_VUELO=?";
            when(cn.prepareStatement(expectedSql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true);
            when(rs.getLong(1)).thenReturn(999L);
            when(rs.wasNull()).thenReturn(false);

            ComprasDAO dao = new ComprasDAO();
            Long pareja = (Long) invokePrivate(
                    dao,
                    "getParejaId",
                    new Class<?>[]{Connection.class, long.class},
                    cn, 123L
            );

            assertEquals(999L, pareja);
        }
    }

    @Test
    @DisplayName("validarVueloDisponible no lanza excepción cuando vuelo está activo y no cancelado")
    void validarVueloDisponible_ok() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");
            dbMock.when(() -> DB.table("ESTADOS")).thenReturn("ESTADOS");

            String expectedSql =
                    "SELECT NVL(v.ACTIVO,1) AS ACTIVO, UPPER(e.ESTADO) AS ESTADO " +
                    "FROM VUELO v JOIN ESTADOS e ON e.ID_ESTADO=v.ID_ESTADO " +
                    "WHERE v.ID_VUELO = ?";
            when(cn.prepareStatement(expectedSql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true);
            when(rs.getInt("ACTIVO")).thenReturn(1);
            when(rs.getString("ESTADO")).thenReturn("DISPONIBLE");

            ComprasDAO dao = new ComprasDAO();

            assertDoesNotThrow(() ->
                    invokePrivate(
                            dao,
                            "validarVueloDisponible",
                            new Class<?>[]{Connection.class, long.class},
                            cn, 10L
                    )
            );
        }
    }

    @Test
    @DisplayName("validarVueloDisponible lanza SQLException cuando el vuelo está cancelado")
    void validarVueloDisponible_cancelado_lanzaExcepcion() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");
            dbMock.when(() -> DB.table("ESTADOS")).thenReturn("ESTADOS");

            String expectedSql =
                    "SELECT NVL(v.ACTIVO,1) AS ACTIVO, UPPER(e.ESTADO) AS ESTADO " +
                    "FROM VUELO v JOIN ESTADOS e ON e.ID_ESTADO=v.ID_ESTADO " +
                    "WHERE v.ID_VUELO = ?";
            when(cn.prepareStatement(expectedSql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true);
            when(rs.getInt("ACTIVO")).thenReturn(1);
            when(rs.getString("ESTADO")).thenReturn("CANCELADO");

            ComprasDAO dao = new ComprasDAO();

            SQLException ex = assertThrows(SQLException.class, () ->
                    invokePrivate(
                            dao,
                            "validarVueloDisponible",
                            new Class<?>[]{Connection.class, long.class},
                            cn, 10L
                    )
            );
            assertTrue(ex.getMessage().contains("Vuelo no disponible"));
        }
    }

    @Test
    @DisplayName("getReservados devuelve el conteo de reservas confirmadas")
    void getReservados_devuelveConteo() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("RESERVA_ITEM")).thenReturn("RESERVA_ITEM");
            dbMock.when(() -> DB.table("RESERVA")).thenReturn("RESERVA");

            String expectedSql =
                    "SELECT NVL(COUNT(*),0) FROM RESERVA_ITEM ri " +
                    "JOIN RESERVA r ON r.ID_RESERVA = ri.ID_RESERVA " +
                    "WHERE ri.ID_VUELO=? AND ri.ID_CLASE=? AND r.ID_ESTADO=1";
            when(cn.prepareStatement(expectedSql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true);
            when(rs.getInt(1)).thenReturn(5);

            ComprasDAO dao = new ComprasDAO();
            int reservados = (int) invokePrivate(
                    dao,
                    "getReservados",
                    new Class<?>[]{Connection.class, long.class, int.class},
                    cn, 50L, 2
            );

            assertEquals(5, reservados);
        }
    }

    @Test
    @DisplayName("getEnCarritos devuelve la suma de cantidades en carritos")
    void getEnCarritos_devuelveSuma() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("CARRITO_ITEM")).thenReturn("CARRITO_ITEM");

            String expectedSql =
                    "SELECT NVL(SUM(CANTIDAD),0) FROM CARRITO_ITEM WHERE ID_VUELO=? AND ID_CLASE=?";
            when(cn.prepareStatement(expectedSql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true);
            when(rs.getInt(1)).thenReturn(3);

            ComprasDAO dao = new ComprasDAO();
            int enCarritos = (int) invokePrivate(
                    dao,
                    "getEnCarritos",
                    new Class<?>[]{Connection.class, long.class, int.class},
                    cn, 50L, 2
            );

            assertEquals(3, enCarritos);
        }
    }

    @Test
    @DisplayName("findCartItemIdFor devuelve null cuando no existe item en ese carrito")
    void findCartItemIdFor_sinItem_devuelveNull() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("CARRITO_ITEM")).thenReturn("CARRITO_ITEM");

            String expectedSql =
                    "SELECT ID_ITEM FROM CARRITO_ITEM WHERE ID_CARRITO=? AND ID_VUELO=? AND ID_CLASE=?";
            when(cn.prepareStatement(expectedSql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(false);

            ComprasDAO dao = new ComprasDAO();
            Long id = (Long) invokePrivate(
                    dao,
                    "findCartItemIdFor",
                    new Class<?>[]{Connection.class, long.class, long.class, int.class},
                    cn, 1L, 50L, 2
            );

            assertNull(id);
        }
    }

    @Test
    @DisplayName("findCartItemIdFor devuelve el ID_ITEM cuando existe")
    void findCartItemIdFor_conItem_devuelveId() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("CARRITO_ITEM")).thenReturn("CARRITO_ITEM");

            String expectedSql =
                    "SELECT ID_ITEM FROM CARRITO_ITEM WHERE ID_CARRITO=? AND ID_VUELO=? AND ID_CLASE=?";
            when(cn.prepareStatement(expectedSql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true);
            when(rs.getLong(1)).thenReturn(777L);

            ComprasDAO dao = new ComprasDAO();
            Long id = (Long) invokePrivate(
                    dao,
                    "findCartItemIdFor",
                    new Class<?>[]{Connection.class, long.class, long.class, int.class},
                    cn, 1L, 50L, 2
            );

            assertEquals(777L, id);
        }
    }

    @Test
    @DisplayName("cancelarReservaAdmin() usa cancelarReserva con isAdmin=true")
    void cancelarReservaAdmin_ok() throws Exception {
        ComprasDAO dao = spy(new ComprasDAO());

        doReturn(true)
            .when(dao)
            .cancelarReserva(0L, 555L, true);

        assertDoesNotThrow(() -> dao.cancelarReservaAdmin(555L));

        verify(dao).cancelarReserva(0L, 555L, true);
    }

    @Test
    @DisplayName("cancelarReservaUsuario() lanza IllegalStateException cuando cancelarReserva devuelve false")
    void cancelarReservaUsuario_noCancelable_lanzaExcepcion() throws Exception {
        ComprasDAO dao = spy(new ComprasDAO());

        doReturn(false)
            .when(dao)
            .cancelarReserva(123L, 999L, false);

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            () -> dao.cancelarReservaUsuario(123L, 999L)
        );

        assertEquals("La reserva no está en estado cancelable.", ex.getMessage());
        verify(dao).cancelarReserva(123L, 999L, false);
    }

    @Test
    @DisplayName("cancelarReservaUsuario() no lanza excepción cuando la reserva se cancela correctamente")
    void cancelarReservaUsuario_ok() throws Exception {
        ComprasDAO dao = spy(new ComprasDAO());

        doReturn(true)
            .when(dao)
            .cancelarReserva(123L, 999L, false);

        assertDoesNotThrow(() -> dao.cancelarReservaUsuario(123L, 999L));

        verify(dao).cancelarReserva(123L, 999L, false);
    }

    @Test
    @DisplayName("listTopDestinos() debe mapear los destinos más vendidos correctamente")
    void listTopDestinos_mapeaCorrectamente() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        Timestamp desde = Timestamp.valueOf("2024-01-01 00:00:00");
        Timestamp hasta = Timestamp.valueOf("2024-12-31 00:00:00");

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("RESERVA_ITEM")).thenReturn("RESERVA_ITEM");
            dbMock.when(() -> DB.table("RESERVA")).thenReturn("RESERVA");
            dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");
            dbMock.when(() -> DB.table("RUTA")).thenReturn("RUTA");
            dbMock.when(() -> DB.table("CIUDAD")).thenReturn("CIUDAD");
            dbMock.when(() -> DB.table("PAIS")).thenReturn("PAIS");
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true, true, false);
            when(rs.getLong("ID_CIUDAD")).thenReturn(10L, 20L);
            when(rs.getString("CIUDAD")).thenReturn("Madrid", "Roma");
            when(rs.getString("PAIS")).thenReturn("España", "Italia");
            when(rs.getLong("BOLETOS")).thenReturn(50L, 30L);

            ComprasDAO dao = new ComprasDAO();

            List<CompraDTO.TopDestino> lista = dao.listTopDestinos(desde, hasta, 5);

            assertNotNull(lista);
            assertEquals(2, lista.size());

            CompraDTO.TopDestino d1 = lista.get(0);
            assertEquals(10L, d1.idCiudadDestino);
            assertEquals("Madrid", d1.ciudadDestino);
            assertEquals("España", d1.paisDestino);
            assertEquals(50L, d1.boletos);

            CompraDTO.TopDestino d2 = lista.get(1);
            assertEquals(20L, d2.idCiudadDestino);
            assertEquals("Roma", d2.ciudadDestino);
            assertEquals("Italia", d2.paisDestino);
            assertEquals(30L, d2.boletos);

            verify(ps).setObject(1, desde);
            verify(ps).setObject(2, hasta);
        }
    }

    @Test
    @DisplayName("listEstadosReserva() debe retornar la lista de estados mapeada correctamente")
    void listEstadosReserva_mapeaCorrectamente() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("ESTADO_RESERVA")).thenReturn("ESTADO_RESERVA");
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true, true, false);
            when(rs.getInt("ID_ESTADO")).thenReturn(1, 2);
            when(rs.getString("ESTADO")).thenReturn("CREADA", "CANCELADA");

            ComprasDAO dao = new ComprasDAO();

            List<CompraDTO.EstadoReserva> lista = dao.listEstadosReserva();

            assertNotNull(lista);
            assertEquals(2, lista.size());

            assertEquals(1, lista.get(0).idEstado);
            assertEquals("CREADA", lista.get(0).nombre);

            assertEquals(2, lista.get(1).idEstado);
            assertEquals("CANCELADA", lista.get(1).nombre);
        }
    }

        @Test
    @DisplayName("guardarReservaWebService inserta correctamente en la tabla RESERVA_WEBSERVICE")
    void guardarReservaWebService_insertaCorrectamente() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        long idReserva = 100L;
        long idUsuarioWs = 200L;

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("RESERVA_WEBSERVICE")).thenReturn("RESERVA_WEBSERVICE");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String expectedSql =
                    "INSERT INTO RESERVA_WEBSERVICE (ID_RESERVA, ID_USUARIO_WEBSERVICE) VALUES (?, ?)";
            when(cn.prepareStatement(expectedSql)).thenReturn(ps);

            ComprasDAO dao = new ComprasDAO();

            dao.guardarReservaWebService(idReserva, idUsuarioWs);

            verify(ps).setLong(1, idReserva);
            verify(ps).setLong(2, idUsuarioWs);
            verify(ps).executeUpdate();
        }
    }

    @Test
    @DisplayName("listReservasByUser devuelve la lista mapeada correctamente")
    void listReservasByUser_mapeaCorrectamente() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        long userId = 123L;

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("RESERVA")).thenReturn("RESERVA");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String expectedSql =
                    "SELECT ID_RESERVA, ID_USUARIO, ID_ESTADO, TOTAL, CREADA_EN, CODIGO " +
                    "FROM RESERVA WHERE ID_USUARIO = ? " +
                    "ORDER BY ID_RESERVA DESC";
            when(cn.prepareStatement(expectedSql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true, false);
            when(rs.getLong("ID_RESERVA")).thenReturn(10L);
            when(rs.getLong("ID_USUARIO")).thenReturn(userId);
            when(rs.getInt("ID_ESTADO")).thenReturn(1);
            when(rs.getBigDecimal("TOTAL")).thenReturn(new java.math.BigDecimal("150.00"));
            
            when(rs.getTimestamp("CREADA_EN")).thenReturn(null);
            when(rs.getString("CODIGO")).thenReturn("RES-ABC-123");

            ComprasDAO dao = new ComprasDAO();

            List<CompraDTO.ReservaListItem> lista = dao.listReservasByUser(userId);

            assertNotNull(lista);
            assertEquals(1, lista.size());

            CompraDTO.ReservaListItem r = lista.get(0);
            assertEquals(10L, r.idReserva);
            assertEquals(userId, r.idUsuario);
            assertEquals(1, r.idEstado);
            assertEquals(new java.math.BigDecimal("150.00"), r.total);
            assertNull(r.creadaEn); 
            assertEquals("RES-ABC-123", r.codigo);

            verify(ps).setLong(1, userId);
        }
    }

    @Test
    @DisplayName("checkout lanza IllegalStateException cuando el carrito está vacío")
    void checkout_carritoVacio_lanzaExcepcion() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        long userId = 123L;
        long cartId = 999L;

        ComprasDAO dao = spy(new ComprasDAO());
        doReturn(cartId).when(dao).ensureCartForUser(userId);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("CARRITO_ITEM")).thenReturn("CARRITO_ITEM");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String expectedSql =
                    "SELECT COUNT(*) FROM CARRITO_ITEM WHERE ID_CARRITO = ?";
            when(cn.prepareStatement(expectedSql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true);
            when(rs.getInt(1)).thenReturn(0); 

            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> dao.checkout(userId)
            );

            assertEquals("El carrito está vacío o ya fue procesado.", ex.getMessage());
            verify(ps).setLong(1, cartId);
        }
    }

        @Test
    @DisplayName("updateQuantity lanza IllegalArgumentException si cantidad <= 0")
    void updateQuantity_cantidadInvalida_lanzaIllegalArgument() {
        ComprasDAO dao = new ComprasDAO();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> dao.updateQuantity(123L, 999L, 0)
        );

        assertEquals("Cantidad debe ser > 0", ex.getMessage());
    }

        @Test
    @DisplayName("getClaseInfo devuelve null cuando no hay registro para la clase")
    void getClaseInfo_sinResultado_devuelveNull() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("SALIDA_CLASE")).thenReturn("SALIDA_CLASE");
            dbMock.when(() -> DB.table("VUELO_CLASE")).thenReturn("VUELO_CLASE");

            when(cn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            ComprasDAO dao = new ComprasDAO();

            Object res = invokePrivate(
                    dao,
                    "getClaseInfo",
                    new Class<?>[]{Connection.class, long.class, int.class, boolean.class},
                    cn, 10L, 1, false
            );

            assertNull(res);
        }
    }

        @Test
    @DisplayName("getClaseInfo devuelve cupoTotal y precio cuando existe registro")
    void getClaseInfo_conResultado_devuelveInfo() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("SALIDA_CLASE")).thenReturn("SALIDA_CLASE");
            dbMock.when(() -> DB.table("VUELO_CLASE")).thenReturn("VUELO_CLASE");

            when(cn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true);
            when(rs.getInt("CUPO_TOTAL")).thenReturn(100);
            when(rs.getBigDecimal("PRECIO")).thenReturn(new BigDecimal("250.00"));

            ComprasDAO dao = new ComprasDAO();

            Object res = invokePrivate(
                    dao,
                    "getClaseInfo",
                    new Class<?>[]{Connection.class, long.class, int.class, boolean.class},
                    cn, 10L, 1, false
            );

            assertNotNull(res);

            var cupoField = res.getClass().getDeclaredField("cupoTotal");
            cupoField.setAccessible(true);
            var precioField = res.getClass().getDeclaredField("precio");
            precioField.setAccessible(true);

            assertEquals(100, cupoField.getInt(res));
            assertEquals(new BigDecimal("250.00"), precioField.get(res));
        }
    }

        @Test
    @DisplayName("ensureCartForUser devuelve el ID del carrito existente cuando ya hay uno")
    void ensureCartForUser_yaExiste() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("CARRITO")).thenReturn("CARRITO");
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true);
            when(rs.getLong(1)).thenReturn(42L);

            ComprasDAO dao = new ComprasDAO();

            long id = dao.ensureCartForUser(123L);

            assertEquals(42L, id);
            verify(ps).setLong(1, 123L);
        }
    }

        @Test
    @DisplayName("checkoutConClienteFinal lanza IllegalStateException cuando el carrito del webservice está vacío")
    void checkoutConClienteFinal_carritoVacio_lanzaExcepcion() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        long userWs = 10L;
        long userFinal = 20L;
        long cartWsId = 999L;

        ComprasDAO dao = spy(new ComprasDAO());
        doReturn(cartWsId).when(dao).ensureCartForUser(userWs);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("CARRITO_ITEM")).thenReturn("CARRITO_ITEM");
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true);
            when(rs.getInt(1)).thenReturn(0);

            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> dao.checkoutConClienteFinal(userWs, userFinal)
            );

            assertEquals("El carrito está vacío o ya fue procesado.", ex.getMessage());
            verify(ps).setLong(1, cartWsId);
        }
    }


@Test
@DisplayName("setFieldIfExists asigna el valor cuando el campo existe")
void setFieldIfExists_campoExiste_asignaValor() throws Exception {
    class Dummy {
        private String ciudadOrigen;
    }

    Dummy d = new Dummy();

    Method m = ComprasDAO.class.getDeclaredMethod(
            "setFieldIfExists",
            Object.class,
            String.class,
            Object.class
    );
    m.setAccessible(true);

    m.invoke(null, d, "ciudadOrigen", "Madrid");

    java.lang.reflect.Field f = Dummy.class.getDeclaredField("ciudadOrigen");
    f.setAccessible(true);
    Object value = f.get(d);

    assertEquals("Madrid", value);
}

@Test
@DisplayName("setFieldIfExists no lanza excepción cuando el campo no existe")
void setFieldIfExists_campoNoExiste_noLanza() throws Exception {
    class Dummy {
        private String otroCampo;
    }

    Dummy d = new Dummy();

    Method m = ComprasDAO.class.getDeclaredMethod(
            "setFieldIfExists",
            Object.class,
            String.class,
            Object.class
    );
    m.setAccessible(true);

    assertDoesNotThrow(() -> {
        try {
            m.invoke(null, d, "campoInexistente", "X");
        } catch (java.lang.reflect.InvocationTargetException ite) {
    
            throw new RuntimeException(ite.getTargetException());
        }
    });
}


}
