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


@Test
@DisplayName("updateQuantity disminuye cantidad sin validar cupo cuando cantidad nueva es menor")
void updateQuantity_disminuyeCantidad_ok() throws Exception {
    long userId = 123L;
    long cartId = 50L;
    long idItem = 777L;

    Connection cn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    ComprasDAO dao = spy(new ComprasDAO());
    doReturn(cartId).when(dao).ensureCartForUser(userId);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(DB::getConnection).thenReturn(cn);
        dbMock.when(() -> DB.table("CARRITO_ITEM")).thenReturn("CARRITO_ITEM");

        when(cn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        
        when(rs.next()).thenReturn(true);
        when(rs.getLong("ID_VUELO")).thenReturn(10L);
        when(rs.getInt("ID_CLASE")).thenReturn(1);
        when(rs.getInt("CANTIDAD")).thenReturn(5); 

        when(ps.executeUpdate()).thenReturn(1);

        
        assertDoesNotThrow(() ->
                dao.updateQuantity(userId, idItem, 3, false)
        );

        verify(cn).setAutoCommit(false);
        verify(cn).commit();
        verify(cn).setAutoCommit(true);
    }
}

@Test
@DisplayName("removeItem con syncPareja elimina el item original y el de la pareja")
void removeItem_syncPareja_eliminaAmbos() throws Exception {
    long userId = 123L;
    long cartId = 99L;
    long idItemOriginal = 1000L;

    Connection cn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    ComprasDAO dao = spy(new ComprasDAO());
    doReturn(cartId).when(dao).ensureCartForUser(userId);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(DB::getConnection).thenReturn(cn);
        dbMock.when(() -> DB.table("CARRITO_ITEM")).thenReturn("CARRITO_ITEM");
        dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");

        when(cn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        when(rs.next()).thenReturn(true, true, true, false);
        
        when(rs.getLong(1)).thenReturn(
                10L,  
                20L,  
                2000L 
        );
        when(rs.getInt(2)).thenReturn(1); 
        
        when(rs.wasNull()).thenReturn(false);
        
        when(ps.executeUpdate()).thenReturn(1);

        assertDoesNotThrow(() ->
                dao.removeItem(userId, idItemOriginal, true)
        );

        verify(cn).setAutoCommit(false);
        verify(cn).commit();
        verify(cn).setAutoCommit(true);

        verify(ps, atLeast(2)).executeUpdate();
    }
}


  @Test
@DisplayName("getCart mapea encabezado e items correctamente")
void getCart_mapeaCorrectamente() throws Exception {
    long userId = 123L;
    long cartId = 999L;

    Connection cn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    ComprasDAO dao = spy(new ComprasDAO());
    doReturn(cartId).when(dao).ensureCartForUser(userId);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(DB::getConnection).thenReturn(cn);
        dbMock.when(() -> DB.table("VW_CARRITO_RESUMEN")).thenReturn("VW_CARRITO_RESUMEN");
        dbMock.when(() -> DB.table("CARRITO_ITEM")).thenReturn("CARRITO_ITEM");
        dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");
        dbMock.when(() -> DB.table("CLASE_ASIENTO")).thenReturn("CLASE_ASIENTO");
        dbMock.when(() -> DB.table("RUTA")).thenReturn("RUTA");
        dbMock.when(() -> DB.table("CIUDAD")).thenReturn("CIUDAD");
        dbMock.when(() -> DB.table("PAIS")).thenReturn("PAIS");

        when(cn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        
        when(rs.next()).thenReturn(true, true, false);
        
        when(rs.getLong("ID_CARRITO")).thenReturn(cartId);
        when(rs.getLong("ID_USUARIO")).thenReturn(userId);
        Timestamp creada = Timestamp.valueOf("2024-01-01 10:00:00");
        when(rs.getTimestamp("FECHA_CREACION")).thenReturn(creada);
        when(rs.getBigDecimal("TOTAL")).thenReturn(new BigDecimal("150.00"));
        
        when(rs.getLong("ID_ITEM")).thenReturn(1L);
        when(rs.getLong("ID_VUELO")).thenReturn(10L);
        when(rs.getString("CODIGO_VUELO")).thenReturn("AV123");
        Timestamp salida = Timestamp.valueOf("2024-02-01 08:00:00");
        Timestamp llegada = Timestamp.valueOf("2024-02-01 12:00:00");
        when(rs.getTimestamp("FECHA_SALIDA")).thenReturn(salida);
        when(rs.getTimestamp("FECHA_LLEGADA")).thenReturn(llegada);
        when(rs.getInt("ID_CLASE")).thenReturn(1);
        when(rs.getString("NOMBRE_CLASE")).thenReturn("ECONÓMICA");
        when(rs.getInt("CANTIDAD")).thenReturn(2);
        when(rs.getBigDecimal("PRECIO_UNITARIO")).thenReturn(new BigDecimal("75.00"));
        when(rs.getBigDecimal("SUBTOTAL")).thenReturn(new BigDecimal("150.00"));
        when(rs.getString("PAIS_ORIGEN")).thenReturn("Guatemala");
        when(rs.getString("PAIS_DESTINO")).thenReturn("España");
        when(rs.getString("CIUDAD_ORIGEN")).thenReturn("Guatemala");
        when(rs.getString("CIUDAD_DESTINO")).thenReturn("Madrid");

        CompraDTO.CarritoResp cart = dao.getCart(userId);

        assertEquals(cartId, cart.idCarrito);
        assertEquals(userId, cart.idUsuario);
        assertEquals(new BigDecimal("150.00"), cart.total);
        assertNotNull(cart.fechaCreacion);

        assertNotNull(cart.items);
        assertEquals(1, cart.items.size());

        CompraDTO.CarritoItem it = cart.items.get(0);
        assertEquals(1L, it.idItem);
        assertEquals(10L, it.idVuelo);
        assertEquals("AV123", it.codigoVuelo);
        assertEquals(1, it.idClase);
        assertEquals("ECONÓMICA", it.clase);
        assertEquals(2, it.cantidad);
        assertEquals(new BigDecimal("75.00"), it.precioUnitario);
        assertEquals(new BigDecimal("150.00"), it.subtotal);
    }

    verify(dao).ensureCartForUser(userId);
}

@Test
@DisplayName("addOrIncrementItem agrega item cuando hay cupo suficiente y sin vuelo pareja")
void addOrIncrementItem_ok_sinPareja() throws Exception {
    long userId = 123L;
    long cartId = 999L;
    long idVuelo = 10L;
    int idClase = 1;

    Connection cn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    ComprasDAO dao = spy(new ComprasDAO());
    doReturn(cartId).when(dao).ensureCartForUser(userId);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(DB::getConnection).thenReturn(cn);

        dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");
        dbMock.when(() -> DB.table("ESTADOS")).thenReturn("ESTADOS");
        dbMock.when(() -> DB.table("SALIDA_CLASE")).thenReturn("SALIDA_CLASE");
        dbMock.when(() -> DB.table("VUELO_CLASE")).thenReturn("VUELO_CLASE");
        dbMock.when(() -> DB.table("RESERVA_ITEM")).thenReturn("RESERVA_ITEM");
        dbMock.when(() -> DB.table("RESERVA")).thenReturn("RESERVA");
        dbMock.when(() -> DB.table("CARRITO_ITEM")).thenReturn("CARRITO_ITEM");

        when(cn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        when(rs.next()).thenReturn(true, true, true, true, false);
        
        when(rs.getInt("ACTIVO")).thenReturn(1);
        when(rs.getString("ESTADO")).thenReturn("DISPONIBLE");
        
        when(rs.getInt("CUPO_TOTAL")).thenReturn(10);
        when(rs.getBigDecimal("PRECIO")).thenReturn(new BigDecimal("100.00"));
        
        when(rs.getInt(1)).thenReturn(0, 0);
        
        when(ps.executeUpdate()).thenReturn(1);

        assertDoesNotThrow(() ->
                dao.addOrIncrementItem(userId, idVuelo, idClase, 2, false)
        );

        verify(cn).setAutoCommit(false);
        verify(cn).commit();
        verify(cn).setAutoCommit(true);
    }
}

@Test
@DisplayName("addOrIncrementItem(4 args) delega al método con incluirPareja=false")
void addOrIncrementItem_simple_delegaAlCompleto() throws Exception {
    ComprasDAO dao = spy(new ComprasDAO());

    doNothing()
        .when(dao)
        .addOrIncrementItem(1L, 2L, 3, 4, false);

    dao.addOrIncrementItem(1L, 2L, 3, 4);

    verify(dao).addOrIncrementItem(1L, 2L, 3, 4, false);
}

@Test
@DisplayName("updateQuantity(3 args) delega al método con syncPareja=false")
void updateQuantity_simple_delegaAlCompleto() throws Exception {
    ComprasDAO dao = spy(new ComprasDAO());

    doNothing()
        .when(dao)
        .updateQuantity(1L, 2L, 3, false);

    dao.updateQuantity(1L, 2L, 3);

    verify(dao).updateQuantity(1L, 2L, 3, false);
}

@Test
@DisplayName("removeItem(2 args) delega al método con syncPareja=false")
void removeItem_simple_delegaAlCompleto() throws Exception {
    ComprasDAO dao = spy(new ComprasDAO());

    doNothing()
        .when(dao)
        .removeItem(1L, 2L, false);

    dao.removeItem(1L, 2L);

    verify(dao).removeItem(1L, 2L, false);
}

@Test
@DisplayName("cancelarVueloYAfectarReservas sin reservas solo marca el vuelo como cancelado")
void cancelarVueloYAfectarReservas_sinReservas() throws Exception {
    long idVuelo = 10L;

    Connection cn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    ComprasDAO dao = new ComprasDAO();

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(DB::getConnection).thenReturn(cn);
        dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");
        dbMock.when(() -> DB.table("ESTADOS")).thenReturn("ESTADOS");
        dbMock.when(() -> DB.table("RESERVA")).thenReturn("RESERVA");
        dbMock.when(() -> DB.table("RESERVA_ITEM")).thenReturn("RESERVA_ITEM");
        dbMock.when(() -> DB.table("SALIDA_CLASE")).thenReturn("SALIDA_CLASE");

        when(cn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);   
        when(ps.executeQuery()).thenReturn(rs);   
        when(rs.next()).thenReturn(false);        

        int updated = dao.cancelarVueloYAfectarReservas(idVuelo);

        assertEquals(0, updated);
        verify(cn).setAutoCommit(false);
        verify(cn).commit();
        verify(cn).setAutoCommit(true);
    }
}

@Test
@DisplayName("cancelarVueloYAfectarReservas con reservas afecta estados y devuelve el número de reservas")
void cancelarVueloYAfectarReservas_conReservas() throws Exception {
    long idVuelo = 10L;

    Connection cn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    ComprasDAO dao = new ComprasDAO();

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(DB::getConnection).thenReturn(cn);
        dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");
        dbMock.when(() -> DB.table("ESTADOS")).thenReturn("ESTADOS");
        dbMock.when(() -> DB.table("RESERVA")).thenReturn("RESERVA");
        dbMock.when(() -> DB.table("RESERVA_ITEM")).thenReturn("RESERVA_ITEM");
        dbMock.when(() -> DB.table("SALIDA_CLASE")).thenReturn("SALIDA_CLASE");

        when(cn.prepareStatement(anyString())).thenReturn(ps);

        when(ps.executeUpdate()).thenReturn(2);     
        when(ps.executeBatch()).thenReturn(new int[]{1});

        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(
                true, true, false,   
                true, false,         
                false                
        );

        when(rs.getLong(1)).thenReturn(100L, 200L);  
        when(rs.getInt("CANT")).thenReturn(3);
        when(rs.getLong("ID_VUELO")).thenReturn(idVuelo);
        when(rs.getInt("ID_CLASE")).thenReturn(1);

        int updated = dao.cancelarVueloYAfectarReservas(idVuelo);

        assertEquals(2, updated);
        verify(cn).setAutoCommit(false);
        verify(cn).commit();
        verify(cn).setAutoCommit(true);
    }
}

@Test
@DisplayName("cancelarReserva lanza IllegalStateException cuando solicitante no es dueño y no es admin")
void cancelarReserva_noAutorizado_lanzaExcepcion() throws Exception {
    Connection cn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(DB::getConnection).thenReturn(cn);
        dbMock.when(() -> DB.table("RESERVA")).thenReturn("RESERVA");
        dbMock.when(() -> DB.table("RESERVA_ITEM")).thenReturn("RESERVA_ITEM");
        dbMock.when(() -> DB.table("SALIDA_CLASE")).thenReturn("SALIDA_CLASE");

        when(cn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        when(rs.next()).thenReturn(true);
        when(rs.getLong(1)).thenReturn(999L); 
        when(rs.getInt(2)).thenReturn(1);     

        ComprasDAO dao = new ComprasDAO();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> dao.cancelarReserva(123L, 77L, false)
        );

        assertTrue(ex.getMessage().contains("No autorizado"));
        verify(cn).setAutoCommit(false);
        verify(cn).rollback();
        verify(cn).setAutoCommit(true);
    }
}

@Test
@DisplayName("cancelarReserva devuelve false cuando la reserva no está en estado 1")
void cancelarReserva_estadoNoCancelable_devuelveFalse() throws Exception {
    Connection cn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(DB::getConnection).thenReturn(cn);
        dbMock.when(() -> DB.table("RESERVA")).thenReturn("RESERVA");

        when(cn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        when(rs.next()).thenReturn(true);
        when(rs.getLong(1)).thenReturn(123L); 
        when(rs.getInt(2)).thenReturn(3);     

        ComprasDAO dao = new ComprasDAO();

        boolean ok = dao.cancelarReserva(123L, 77L, false);

        assertFalse(ok);
        verify(cn).setAutoCommit(false);
        verify(cn).rollback();
        verify(cn, never()).commit();
        verify(cn).setAutoCommit(true);
    }
}

@Test
@DisplayName("cancelarReserva en estado confirmado libera cupos y marca la reserva como cancelada")
void cancelarReserva_estadoConfirmado_devuelveTrue() throws Exception {
    Connection cn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(DB::getConnection).thenReturn(cn);
        dbMock.when(() -> DB.table("RESERVA")).thenReturn("RESERVA");
        dbMock.when(() -> DB.table("RESERVA_ITEM")).thenReturn("RESERVA_ITEM");
        dbMock.when(() -> DB.table("SALIDA_CLASE")).thenReturn("SALIDA_CLASE");

        when(cn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        when(rs.next()).thenReturn(
                true,        
                true, false  
        );

        when(rs.getLong(1)).thenReturn(123L); 
        when(rs.getInt(2)).thenReturn(1);     

        
        when(rs.getInt("CANT")).thenReturn(2);
        when(rs.getLong("ID_VUELO")).thenReturn(10L);
        when(rs.getInt("ID_CLASE")).thenReturn(1);

        when(ps.executeBatch()).thenReturn(new int[]{1});
        when(ps.executeUpdate()).thenReturn(1);

        ComprasDAO dao = new ComprasDAO();

        boolean ok = dao.cancelarReserva(123L, 77L, false);

        assertTrue(ok);
        verify(cn).setAutoCommit(false);
        verify(cn).commit();
        verify(cn, never()).rollback();
        verify(cn).setAutoCommit(true);
    }
}

@Test
@DisplayName("listReservasAdmin sin filtros y sin resultados devuelve lista vacía")
void listReservasAdmin_sinResultados_listaVacia() throws Exception {
    Connection cn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

        dbMock.when(DB::getConnection).thenReturn(cn);
        dbMock.when(() -> DB.table("RESERVA")).thenReturn("RESERVA");
        dbMock.when(() -> DB.table("USUARIO")).thenReturn("USUARIO");
        dbMock.when(() -> DB.table("ESTADO_RESERVA")).thenReturn("ESTADO_RESERVA");

        when(cn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        when(rs.next()).thenReturn(false);

        ComprasDAO dao = new ComprasDAO();

        var lista = dao.listReservasAdmin(
                null,  
                null,  
                null,  
                null,  
                null,  
                null,  
                null   
        );

        assertNotNull(lista);
        assertTrue(lista.isEmpty(), "Debe devolver lista vacía cuando no hay resultados");
    }
}
@Test
@DisplayName("getReservaDetalle lanza IllegalArgumentException cuando la reserva no existe para el usuario")
void getReservaDetalle_reservaNoEncontrada_lanzaIllegalArgument() throws Exception {
    Connection cn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(DB::getConnection).thenReturn(cn);

        dbMock.when(() -> DB.table("RESERVA")).thenReturn("RESERVA");
        dbMock.when(() -> DB.table("RESERVA_ITEM")).thenReturn("RESERVA_ITEM");
        dbMock.when(() -> DB.table("SALIDA_CLASE")).thenReturn("SALIDA_CLASE");
        dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");
        dbMock.when(() -> DB.table("RUTA")).thenReturn("RUTA");
        dbMock.when(() -> DB.table("CIUDAD")).thenReturn("CIUDAD");
        dbMock.when(() -> DB.table("PAIS")).thenReturn("PAIS");
        dbMock.when(() -> DB.table("CLASE_ASIENTO")).thenReturn("CLASE_ASIENTO");
        dbMock.when(() -> DB.table("ESTADO_RESERVA")).thenReturn("ESTADO_RESERVA");
        dbMock.when(() -> DB.table("USUARIO")).thenReturn("USUARIO");

        when(cn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        
        when(rs.next()).thenReturn(false);

        ComprasDAO dao = new ComprasDAO();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> dao.getReservaDetalle(123L, 999L)
        );

        assertTrue(ex.getMessage().contains("Reserva no encontrada"));
    }
}

@Test
@DisplayName("getReservaDetalleAdmin lanza IllegalArgumentException cuando la reserva no existe")
void getReservaDetalleAdmin_reservaNoEncontrada_lanzaIllegalArgument() throws Exception {
    Connection cn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(DB::getConnection).thenReturn(cn);

        dbMock.when(() -> DB.table("RESERVA")).thenReturn("RESERVA");
        dbMock.when(() -> DB.table("RESERVA_ITEM")).thenReturn("RESERVA_ITEM");
        dbMock.when(() -> DB.table("SALIDA_CLASE")).thenReturn("SALIDA_CLASE");
        dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");
        dbMock.when(() -> DB.table("RUTA")).thenReturn("RUTA");
        dbMock.when(() -> DB.table("CIUDAD")).thenReturn("CIUDAD");
        dbMock.when(() -> DB.table("PAIS")).thenReturn("PAIS");
        dbMock.when(() -> DB.table("CLASE_ASIENTO")).thenReturn("CLASE_ASIENTO");
        dbMock.when(() -> DB.table("ESTADO_RESERVA")).thenReturn("ESTADO_RESERVA");
        dbMock.when(() -> DB.table("USUARIO")).thenReturn("USUARIO");

        when(cn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        
        when(rs.next()).thenReturn(false);

        ComprasDAO dao = new ComprasDAO();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> dao.getReservaDetalleAdmin(999L)
        );

        assertTrue(ex.getMessage().contains("Reserva no encontrada"));
    }
}

@Test
@DisplayName("getReservaDetalle retorna DTO completo cuando la reserva existe")
void getReservaDetalle_mapeaCorrectamente() throws Exception {
    Connection cn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(DB::getConnection).thenReturn(cn);

        dbMock.when(() -> DB.table("RESERVA")).thenReturn("RESERVA");
        dbMock.when(() -> DB.table("RESERVA_ITEM")).thenReturn("RESERVA_ITEM");
        dbMock.when(() -> DB.table("SALIDA_CLASE")).thenReturn("SALIDA_CLASE");
        dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");
        dbMock.when(() -> DB.table("RUTA")).thenReturn("RUTA");
        dbMock.when(() -> DB.table("CIUDAD")).thenReturn("CIUDAD");
        dbMock.when(() -> DB.table("PAIS")).thenReturn("PAIS");
        dbMock.when(() -> DB.table("CLASE_ASIENTO")).thenReturn("CLASE_ASIENTO");
        dbMock.when(() -> DB.table("ESTADO_RESERVA")).thenReturn("ESTADO_RESERVA");
        dbMock.when(() -> DB.table("USUARIO")).thenReturn("USUARIO");

        when(cn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        when(rs.next()).thenReturn(
                true,   
                true,   
                false   
        );

        when(rs.getLong("ID_RESERVA")).thenReturn(10L);
        when(rs.getLong("ID_USUARIO")).thenReturn(123L);
        when(rs.getInt("ID_ESTADO")).thenReturn(1);
        when(rs.getBigDecimal("TOTAL")).thenReturn(new BigDecimal("200.00"));
        when(rs.getString("CODIGO")).thenReturn("RES-XYZ-123");

        when(rs.getInt("CANT")).thenReturn(2);
        when(rs.getInt("CANTIDAD")).thenReturn(2); 
        when(rs.getLong("ID_VUELO")).thenReturn(50L);
        when(rs.getInt("ID_CLASE")).thenReturn(1);
        when(rs.getBigDecimal("PRECIO_UNITARIO")).thenReturn(new BigDecimal("100.00"));

        ComprasDAO dao = new ComprasDAO();
        CompraDTO.ReservaDetalle dto = dao.getReservaDetalle(123L, 10L);

        assertNotNull(dto);
        assertEquals(10L, dto.idReserva);
        assertEquals(123L, dto.idUsuario);
        assertEquals(new BigDecimal("200.00"), dto.total);
        assertEquals("RES-XYZ-123", dto.codigo);
        assertEquals(1, dto.items.size());

        var it = dto.items.get(0);
        assertEquals(50L, it.idVuelo);
        assertEquals(1, it.idClase);
        assertEquals(2, it.cantidad);                
        assertEquals(new BigDecimal("100.00"), it.precioUnitario);
    }
}


@Test
@DisplayName("getReservaDetalleAdmin retorna DTO completo cuando la reserva existe")
void getReservaDetalleAdmin_mapeaCorrectamente() throws Exception {
    Connection cn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(DB::getConnection).thenReturn(cn);

        dbMock.when(() -> DB.table("RESERVA")).thenReturn("RESERVA");
        dbMock.when(() -> DB.table("RESERVA_ITEM")).thenReturn("RESERVA_ITEM");
        dbMock.when(() -> DB.table("SALIDA_CLASE")).thenReturn("SALIDA_CLASE");
        dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");
        dbMock.when(() -> DB.table("RUTA")).thenReturn("RUTA");
        dbMock.when(() -> DB.table("CIUDAD")).thenReturn("CIUDAD");
        dbMock.when(() -> DB.table("PAIS")).thenReturn("PAIS");
        dbMock.when(() -> DB.table("CLASE_ASIENTO")).thenReturn("CLASE_ASIENTO");
        dbMock.when(() -> DB.table("ESTADO_RESERVA")).thenReturn("ESTADO_RESERVA");
        dbMock.when(() -> DB.table("USUARIO")).thenReturn("USUARIO");

        when(cn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        when(rs.next()).thenReturn(true, true, false);

        when(rs.getLong("ID_RESERVA")).thenReturn(10L);
        when(rs.getInt("ID_ESTADO")).thenReturn(1);
        when(rs.getBigDecimal("TOTAL")).thenReturn(new BigDecimal("200.00"));

        when(rs.getInt("CANT")).thenReturn(2);
        when(rs.getLong("ID_VUELO")).thenReturn(50L);
        when(rs.getInt("ID_CLASE")).thenReturn(1);
        when(rs.getBigDecimal("PRECIO")).thenReturn(new BigDecimal("100.00"));

        ComprasDAO dao = new ComprasDAO();
        CompraDTO.ReservaDetalle dto = dao.getReservaDetalleAdmin(10L);

        assertNotNull(dto);
        assertEquals(10L, dto.idReserva);
        assertEquals(1, dto.items.size());
    }
}

@Test
@DisplayName("getConn delega en DB.getConnection() y devuelve la misma conexión")
void getConn_delegaEnDB() throws Exception {
    Connection cn = mock(Connection.class);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(DB::getConnection).thenReturn(cn);

        ComprasDAO dao = new ComprasDAO();

        Connection res = (Connection) invokePrivate(
                dao,
                "getConn",
                new Class<?>[]{} 
        );

        assertSame(cn, res, "getConn debe devolver exactamente la conexión de DB.getConnection()");
    }
}

 @Test
@DisplayName("getClaseInfo usa precio de VUELO_CLASE cuando SALIDA_CLASE.PRECIO es null")
void getClaseInfo_precioNull_usaVueloClase() throws Exception {
    Connection cn = mock(Connection.class);
    PreparedStatement ps1 = mock(PreparedStatement.class);
    PreparedStatement ps2 = mock(PreparedStatement.class);
    ResultSet rs1 = mock(ResultSet.class);
    ResultSet rs2 = mock(ResultSet.class);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(() -> DB.table("SALIDA_CLASE")).thenReturn("SALIDA_CLASE");
        dbMock.when(() -> DB.table("VUELO_CLASE")).thenReturn("VUELO_CLASE");

        when(cn.prepareStatement(anyString())).thenReturn(ps1, ps2);

        when(ps1.executeQuery()).thenReturn(rs1);
        when(rs1.next()).thenReturn(true);
        when(rs1.getInt("CUPO_TOTAL")).thenReturn(100);
        when(rs1.getBigDecimal("PRECIO")).thenReturn(null);

        when(ps2.executeQuery()).thenReturn(rs2);
        when(rs2.next()).thenReturn(true);
        when(rs2.getBigDecimal(1)).thenReturn(new BigDecimal("123.45"));

        ComprasDAO dao = new ComprasDAO();

        Object res = invokePrivate(
                dao,
                "getClaseInfo",
                new Class<?>[]{Connection.class, long.class, int.class, boolean.class},
                cn, 10L, 1, false
        );

        assertNotNull(res);

        var cupoField = res.getClass().getDeclaredField("cupoTotal");
        var precioField = res.getClass().getDeclaredField("precio");
        cupoField.setAccessible(true);
        precioField.setAccessible(true);

        assertEquals(100, cupoField.getInt(res));
        assertEquals(new BigDecimal("123.45"), precioField.get(res));

        verify(cn, times(2)).prepareStatement(anyString());
    }
}

@Test
@DisplayName("ensureCartForUser crea carrito cuando no existe para el usuario")
void ensureCartForUser_creaCarritoCuandoNoExiste() throws Exception {
    long userId = 123L;

    Connection cn = mock(Connection.class);
    PreparedStatement psSelect1 = mock(PreparedStatement.class);
    PreparedStatement psInsert = mock(PreparedStatement.class);
    PreparedStatement psSelect2 = mock(PreparedStatement.class);
    ResultSet rs1 = mock(ResultSet.class);
    ResultSet rs2 = mock(ResultSet.class);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(() -> DB.table("CARRITO")).thenReturn("CARRITO");
        dbMock.when(DB::getConnection).thenReturn(cn);

        when(cn.prepareStatement(anyString())).thenReturn(psSelect1, psInsert, psSelect2);

        when(psSelect1.executeQuery()).thenReturn(rs1);
        when(rs1.next()).thenReturn(false);

        when(psInsert.executeUpdate()).thenReturn(1);

        when(psSelect2.executeQuery()).thenReturn(rs2);
        when(rs2.next()).thenReturn(true);
        when(rs2.getLong(1)).thenReturn(777L);

        ComprasDAO dao = new ComprasDAO();

        long id = dao.ensureCartForUser(userId);

        assertEquals(777L, id);
        verify(psSelect1).setLong(1, userId);
        verify(psInsert).setLong(1, userId);
        verify(psSelect2).setLong(1, userId);
    }
}

@Test
@DisplayName("checkout procesa el carrito y devuelve el id de reserva generado")
void checkout_ok() throws Exception {
    long userId = 123L;
    long cartId = 999L;

    Connection cn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    CallableStatement cs = mock(CallableStatement.class);

    ComprasDAO dao = spy(new ComprasDAO());
    doReturn(cartId).when(dao).ensureCartForUser(userId);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(() -> DB.table("CARRITO_ITEM")).thenReturn("CARRITO_ITEM");
        dbMock.when(DB::getConnection).thenReturn(cn);
        dbMock.when(DB::getSchema).thenReturn("SCHEMA");

        when(cn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt(1)).thenReturn(2);

        when(cn.prepareCall(anyString())).thenReturn(cs);
        when(cs.execute()).thenReturn(false); 
        when(cs.getLong(3)).thenReturn(555L);

        long idReserva = dao.checkout(userId);

        assertEquals(555L, idReserva);
        verify(cs).setLong(1, userId);
        verify(cs).setLong(2, cartId);
    }
}

@Test
@DisplayName("checkoutConClienteFinal clona items al cliente final y ejecuta el procedimiento de checkout")
void checkoutConClienteFinal_ok() throws Exception {
    long userWs = 10L;
    long userFinal = 20L;
    long cartWsId = 100L;
    long cartFinalId = 200L;

    Connection cn = mock(Connection.class);
    PreparedStatement psCount = mock(PreparedStatement.class);
    PreparedStatement psSelectCart = mock(PreparedStatement.class);
    PreparedStatement psDeleteItems = mock(PreparedStatement.class);
    PreparedStatement psMerge = mock(PreparedStatement.class);
    ResultSet rsCount = mock(ResultSet.class);
    ResultSet rsSelectCart = mock(ResultSet.class);
    CallableStatement cs = mock(CallableStatement.class);

    ComprasDAO dao = spy(new ComprasDAO());
    doReturn(cartWsId).when(dao).ensureCartForUser(userWs);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(DB::getConnection).thenReturn(cn);
        dbMock.when(DB::getSchema).thenReturn("SCHEMA");
        dbMock.when(() -> DB.table("CARRITO_ITEM")).thenReturn("CARRITO_ITEM");
        dbMock.when(() -> DB.table("CARRITO")).thenReturn("CARRITO");

        when(cn.prepareStatement(anyString()))
                .thenReturn(psCount, psSelectCart, psDeleteItems, psMerge);

        when(psCount.executeQuery()).thenReturn(rsCount);
        when(rsCount.next()).thenReturn(true);
        when(rsCount.getInt(1)).thenReturn(3);

        when(psSelectCart.executeQuery()).thenReturn(rsSelectCart);
        when(rsSelectCart.next()).thenReturn(true);
        when(rsSelectCart.getLong(1)).thenReturn(cartFinalId);

        when(psDeleteItems.executeUpdate()).thenReturn(1);
        when(psMerge.executeUpdate()).thenReturn(3);

        when(cn.prepareCall(anyString())).thenReturn(cs);
        when(cs.execute()).thenReturn(false); 
        when(cs.getLong(3)).thenReturn(999L);

        long idReserva = dao.checkoutConClienteFinal(userWs, userFinal);

        assertEquals(999L, idReserva);
        verify(cs).setLong(1, userFinal);
        verify(cs).setLong(2, cartFinalId);
    }
}

    @Test
    @DisplayName("updateQuantity aumenta cantidad cuando hay cupo suficiente")
    void updateQuantity_aumentaCantidad_ok() throws Exception {
        long userId = 123L;
        long cartId = 50L;
        long idItem = 777L;

        Connection cn = mock(Connection.class);
        
        PreparedStatement ps1 = mock(PreparedStatement.class); 
        PreparedStatement ps2 = mock(PreparedStatement.class); 
        PreparedStatement ps3 = mock(PreparedStatement.class); 
        PreparedStatement ps4 = mock(PreparedStatement.class); 
        PreparedStatement ps5 = mock(PreparedStatement.class); 
        PreparedStatement ps6 = mock(PreparedStatement.class); 

        ResultSet rs1 = mock(ResultSet.class);
        ResultSet rs2 = mock(ResultSet.class);
        ResultSet rs3 = mock(ResultSet.class);
        ResultSet rs4 = mock(ResultSet.class);
        ResultSet rs5 = mock(ResultSet.class);

        ComprasDAO dao = spy(new ComprasDAO());
        doReturn(cartId).when(dao).ensureCartForUser(userId);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(DB::getConnection).thenReturn(cn);
            dbMock.when(() -> DB.table("CARRITO_ITEM")).thenReturn("CARRITO_ITEM");
            dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");
            dbMock.when(() -> DB.table("ESTADOS")).thenReturn("ESTADOS");
            dbMock.when(() -> DB.table("SALIDA_CLASE")).thenReturn("SALIDA_CLASE");
            dbMock.when(() -> DB.table("VUELO_CLASE")).thenReturn("VUELO_CLASE");
            dbMock.when(() -> DB.table("RESERVA_ITEM")).thenReturn("RESERVA_ITEM");
            dbMock.when(() -> DB.table("RESERVA")).thenReturn("RESERVA");

            when(cn.prepareStatement(anyString()))
                    .thenReturn(ps1, ps2, ps3, ps4, ps5, ps6);
           
            when(ps1.executeQuery()).thenReturn(rs1);
            when(rs1.next()).thenReturn(true);
            when(rs1.getLong("ID_VUELO")).thenReturn(10L);
            when(rs1.getInt("ID_CLASE")).thenReturn(1);
            when(rs1.getInt("CANTIDAD")).thenReturn(2); 
            
            when(ps2.executeQuery()).thenReturn(rs2);
            when(rs2.next()).thenReturn(true);
            when(rs2.getInt("ACTIVO")).thenReturn(1);
            when(rs2.getString("ESTADO")).thenReturn("DISPONIBLE");
            
            when(ps3.executeQuery()).thenReturn(rs3);
            when(rs3.next()).thenReturn(true);
            when(rs3.getInt("CUPO_TOTAL")).thenReturn(100);
            when(rs3.getBigDecimal("PRECIO")).thenReturn(new BigDecimal("50.00"));
            
            when(ps4.executeQuery()).thenReturn(rs4);
            when(rs4.next()).thenReturn(true);
            when(rs4.getInt(1)).thenReturn(10);
            
            when(ps5.executeQuery()).thenReturn(rs5);
            when(rs5.next()).thenReturn(true);
            when(rs5.getInt(1)).thenReturn(5);
            
            when(ps6.executeUpdate()).thenReturn(1);
            
            assertDoesNotThrow(() ->
                    dao.updateQuantity(userId, idItem, 5, false)
            );

            verify(cn).setAutoCommit(false);
            verify(cn).commit();
            verify(cn).setAutoCommit(true);
        }
    }

    @Test
    @DisplayName("addOrIncrementItem lanza SQLException cuando no hay cupo suficiente")
    void addOrIncrementItem_cupoInsuficiente_lanzaSQLException() throws Exception {
        long userId = 123L;
        long cartId = 999L;
        long idVuelo = 10L;
        int idClase = 1;

        Connection cn = mock(Connection.class);
        PreparedStatement ps1 = mock(PreparedStatement.class); 
        PreparedStatement ps2 = mock(PreparedStatement.class); 
        PreparedStatement ps3 = mock(PreparedStatement.class); 
        PreparedStatement ps4 = mock(PreparedStatement.class); 

        ResultSet rs1 = mock(ResultSet.class);
        ResultSet rs2 = mock(ResultSet.class);
        ResultSet rs3 = mock(ResultSet.class);
        ResultSet rs4 = mock(ResultSet.class);

        ComprasDAO dao = spy(new ComprasDAO());
        doReturn(cartId).when(dao).ensureCartForUser(userId);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(DB::getConnection).thenReturn(cn);
            dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");
            dbMock.when(() -> DB.table("ESTADOS")).thenReturn("ESTADOS");
            dbMock.when(() -> DB.table("SALIDA_CLASE")).thenReturn("SALIDA_CLASE");
            dbMock.when(() -> DB.table("VUELO_CLASE")).thenReturn("VUELO_CLASE");
            dbMock.when(() -> DB.table("RESERVA_ITEM")).thenReturn("RESERVA_ITEM");
            dbMock.when(() -> DB.table("RESERVA")).thenReturn("RESERVA");
            dbMock.when(() -> DB.table("CARRITO_ITEM")).thenReturn("CARRITO_ITEM");

            when(cn.prepareStatement(anyString()))
                    .thenReturn(ps1, ps2, ps3, ps4);

            when(ps1.executeQuery()).thenReturn(rs1);
            when(rs1.next()).thenReturn(true);
            when(rs1.getInt("ACTIVO")).thenReturn(1);
            when(rs1.getString("ESTADO")).thenReturn("DISPONIBLE");
            
            when(ps2.executeQuery()).thenReturn(rs2);
            when(rs2.next()).thenReturn(true);
            when(rs2.getInt("CUPO_TOTAL")).thenReturn(5);
            when(rs2.getBigDecimal("PRECIO")).thenReturn(new BigDecimal("100.00"));
            
            when(ps3.executeQuery()).thenReturn(rs3);
            when(rs3.next()).thenReturn(true);
            when(rs3.getInt(1)).thenReturn(3);
            
            when(ps4.executeQuery()).thenReturn(rs4);
            when(rs4.next()).thenReturn(true);
            when(rs4.getInt(1)).thenReturn(2); 

            SQLException ex = assertThrows(
                    SQLException.class,
                    () -> dao.addOrIncrementItem(userId, idVuelo, idClase, 2, false)
            );

            assertTrue(ex.getMessage().contains("Cupo insuficiente"));
            verify(cn).rollback();
        }
    }

    @Test
    @DisplayName("listReservasAdmin con filtros construye la consulta y mapea resultados")
    void listReservasAdmin_conFiltros_mapeaCorrectamente() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        String q = "john";       
        String usuario = "200";  
        String codigo = "RES-";
        String vuelo = "AV123";  
        Timestamp desde = Timestamp.valueOf("2024-01-01 00:00:00");
        Timestamp hasta = Timestamp.valueOf("2024-12-31 00:00:00");
        Integer idEstado = 1;

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(DB::getConnection).thenReturn(cn);
            dbMock.when(() -> DB.table("RESERVA")).thenReturn("RESERVA");
            dbMock.when(() -> DB.table("USUARIO")).thenReturn("USUARIO");
            dbMock.when(() -> DB.table("RESERVA_ITEM")).thenReturn("RESERVA_ITEM");
            dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");

            when(cn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true, false);
            when(rs.getLong("ID_RESERVA")).thenReturn(10L);
            when(rs.getLong("ID_USUARIO")).thenReturn(200L);
            when(rs.getInt("ID_ESTADO")).thenReturn(1);
            when(rs.getBigDecimal("TOTAL")).thenReturn(new BigDecimal("300.00"));
            when(rs.getTimestamp("CREADA_EN")).thenReturn(desde);
            when(rs.getString("CODIGO")).thenReturn("RES-XYZ-001");

            ComprasDAO dao = new ComprasDAO();

            List<CompraDTO.ReservaListItem> lista = dao.listReservasAdmin(
                    q, usuario, codigo, vuelo, desde, hasta, idEstado
            );

            assertNotNull(lista);
            assertEquals(1, lista.size());
            var r = lista.get(0);
            assertEquals(10L, r.idReserva);
            assertEquals(200L, r.idUsuario);
            assertEquals(1, r.idEstado);
            assertEquals(new BigDecimal("300.00"), r.total);
            assertEquals("RES-XYZ-001", r.codigo);
        }
    }

    @Test
@DisplayName("validarVueloDisponible lanza SQLException cuando el vuelo no existe")
void validarVueloDisponible_vueloNoExiste_lanzaSQLException() throws Exception {
    Connection cn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");
        dbMock.when(() -> DB.table("ESTADOS")).thenReturn("ESTADOS");

        when(cn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false); 

        ComprasDAO dao = new ComprasDAO();

        SQLException ex = assertThrows(
                SQLException.class,
                () -> invokePrivate(
                        dao,
                        "validarVueloDisponible",
                        new Class<?>[]{Connection.class, long.class},
                        cn, 999L
                )
        );

        assertTrue(ex.getMessage().contains("Vuelo no existe"));
    }
}

    @Test
@DisplayName("crearCarritoClienteFinal crea carrito nuevo y copia items cuando el cliente final no tiene carrito")
void crearCarritoClienteFinal_creaYcopiaItems() throws Exception {
    long cartWsId = 100L;
    long userFinal = 200L;

    Connection cn = mock(Connection.class);
    PreparedStatement psSelect = mock(PreparedStatement.class);
    PreparedStatement psInsert = mock(PreparedStatement.class);
    PreparedStatement psSelectNew = mock(PreparedStatement.class);
    PreparedStatement psMerge = mock(PreparedStatement.class);
    ResultSet rsSelect = mock(ResultSet.class);
    ResultSet rsNew = mock(ResultSet.class);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(() -> DB.table("CARRITO")).thenReturn("CARRITO");
        dbMock.when(() -> DB.table("CARRITO_ITEM")).thenReturn("CARRITO_ITEM");

        when(cn.prepareStatement(anyString()))
                .thenReturn(psSelect, psInsert, psSelectNew, psMerge);

        when(psSelect.executeQuery()).thenReturn(rsSelect);
        when(rsSelect.next()).thenReturn(false); 

        when(psInsert.executeUpdate()).thenReturn(1);

        when(psSelectNew.executeQuery()).thenReturn(rsNew);
        when(rsNew.next()).thenReturn(true);
        when(rsNew.getLong(1)).thenReturn(777L); 

        when(psMerge.executeUpdate()).thenReturn(3);

        ComprasDAO dao = new ComprasDAO();

        long id = (long) invokePrivate(
                dao,
                "crearCarritoClienteFinal",
                new Class<?>[]{Connection.class, long.class, long.class},
                cn, cartWsId, userFinal
        );

        assertEquals(777L, id);
        verify(psMerge).setLong(1, 777L);
        verify(psMerge).setLong(2, cartWsId);
    }
}

@Test
@DisplayName("listReservasAdmin con solo fecha y estado mapea correctamente")
void listReservasAdmin_soloFechaYEstado_mapeaCorrectamente() throws Exception {
    Connection cn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    Timestamp desde = Timestamp.valueOf("2024-06-01 00:00:00");
    Timestamp hasta = Timestamp.valueOf("2024-06-30 23:59:59");
    Integer idEstado = 2;

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(DB::getConnection).thenReturn(cn);
        dbMock.when(() -> DB.table("RESERVA")).thenReturn("RESERVA");
        dbMock.when(() -> DB.table("USUARIO")).thenReturn("USUARIO");
        dbMock.when(() -> DB.table("RESERVA_ITEM")).thenReturn("RESERVA_ITEM");
        dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");
        dbMock.when(() -> DB.table("ESTADO_RESERVA")).thenReturn("ESTADO_RESERVA");

        when(cn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        when(rs.next()).thenReturn(true, false);
        when(rs.getLong("ID_RESERVA")).thenReturn(50L);
        when(rs.getLong("ID_USUARIO")).thenReturn(321L);
        when(rs.getInt("ID_ESTADO")).thenReturn(idEstado);
        when(rs.getBigDecimal("TOTAL")).thenReturn(new BigDecimal("500.00"));
        when(rs.getTimestamp("CREADA_EN")).thenReturn(desde);
        when(rs.getString("CODIGO")).thenReturn("RES-ABC-999");

        ComprasDAO dao = new ComprasDAO();

        List<CompraDTO.ReservaListItem> lista = dao.listReservasAdmin(
                null,   
                null,   
                null,   
                null,   
                desde,  
                hasta,  
                idEstado
        );

        assertNotNull(lista);
        assertEquals(1, lista.size());

        CompraDTO.ReservaListItem r = lista.get(0);
        assertEquals(50L, r.idReserva);
        assertEquals(321L, r.idUsuario);
        assertEquals(idEstado.intValue(), r.idEstado);
        assertEquals(new BigDecimal("500.00"), r.total);
        assertEquals("RES-ABC-999", r.codigo);
    }
}

@Test
@DisplayName("addOrIncrementItem con incluirPareja=true agrega ida y regreso cuando hay cupo suficiente")
void addOrIncrementItem_conPareja_ok() throws Exception {
    long userId = 123L;
    long cartId = 999L;
    long idVuelo = 10L;
    int idClase = 1;

    Connection cn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    ComprasDAO dao = spy(new ComprasDAO());
    doReturn(cartId).when(dao).ensureCartForUser(userId);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(DB::getConnection).thenReturn(cn);

        dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");
        dbMock.when(() -> DB.table("ESTADOS")).thenReturn("ESTADOS");
        dbMock.when(() -> DB.table("SALIDA_CLASE")).thenReturn("SALIDA_CLASE");
        dbMock.when(() -> DB.table("VUELO_CLASE")).thenReturn("VUELO_CLASE");
        dbMock.when(() -> DB.table("RESERVA_ITEM")).thenReturn("RESERVA_ITEM");
        dbMock.when(() -> DB.table("RESERVA")).thenReturn("RESERVA");
        dbMock.when(() -> DB.table("CARRITO_ITEM")).thenReturn("CARRITO_ITEM");

        when(cn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        when(rs.next()).thenReturn(
                true,  
                true,  
                true,  
                true,  
                true,  
                true,  
                true,  
                true,  
                true   
        );

        
        when(rs.getInt("ACTIVO")).thenReturn(1);
        when(rs.getString("ESTADO")).thenReturn("DISPONIBLE");

        
        when(rs.getInt("CUPO_TOTAL")).thenReturn(10);
        when(rs.getBigDecimal("PRECIO")).thenReturn(new BigDecimal("100.00"));

        when(rs.getInt(1)).thenReturn(0);
        
        when(rs.getLong(1)).thenReturn(20L);
        when(rs.wasNull()).thenReturn(false);

        when(ps.executeUpdate()).thenReturn(1);

        assertDoesNotThrow(() ->
                dao.addOrIncrementItem(userId, idVuelo, idClase, 2, true)
        );

        verify(cn).setAutoCommit(false);
        verify(cn).commit();
        verify(cn).setAutoCommit(true);
        
        verify(ps, atLeast(2)).executeUpdate();
    }
}

@Test
@DisplayName("updateQuantity disminuye cantidad y sincroniza con vuelo pareja cuando syncPareja=true")
void updateQuantity_disminuyeCantidad_syncPareja_ok() throws Exception {
    long userId = 123L;
    long cartId = 50L;
    long idItem = 1000L;

    Connection cn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    ComprasDAO dao = spy(new ComprasDAO());
    doReturn(cartId).when(dao).ensureCartForUser(userId);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(DB::getConnection).thenReturn(cn);
        dbMock.when(() -> DB.table("CARRITO_ITEM")).thenReturn("CARRITO_ITEM");
        dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");

        when(cn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        when(rs.next()).thenReturn(true, true, true);
        
        when(rs.getLong("ID_VUELO")).thenReturn(10L);
        when(rs.getInt("ID_CLASE")).thenReturn(1);
        when(rs.getInt("CANTIDAD")).thenReturn(5); 
        
        when(rs.getLong(1)).thenReturn(20L, 2000L);
        when(rs.wasNull()).thenReturn(false);

        when(ps.executeUpdate()).thenReturn(1);

        assertDoesNotThrow(() ->
                dao.updateQuantity(userId, idItem, 3, true) 
        );

        verify(cn).setAutoCommit(false);
        verify(cn).commit();
        verify(cn).setAutoCommit(true);

        verify(ps, atLeast(2)).executeUpdate();
    }
}

@Test
@DisplayName("getReservaDetalle lanza IllegalStateException cuando el usuario no es dueño de la reserva")
void getReservaDetalle_noAutorizado_lanzaIllegalState() throws Exception {
    Connection cn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(DB::getConnection).thenReturn(cn);
        dbMock.when(() -> DB.table("RESERVA")).thenReturn("RESERVA");
        dbMock.when(() -> DB.table("USUARIO")).thenReturn("USUARIO");

        when(cn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        when(rs.next()).thenReturn(true); 
        when(rs.getLong("ID_USUARIO")).thenReturn(999L); 

        ComprasDAO dao = new ComprasDAO();

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> dao.getReservaDetalle(123L, 10L)
        );

        assertTrue(ex.getMessage().contains("No autorizado"));
    }
}
 @Test
    @DisplayName("ensureCartForUser lanza SQLException cuando no logra obtener el carrito después del insert")
    void ensureCartForUser_fallaDespuesInsert_lanzaSQLException() throws Exception {
        long userId = 123L;

        Connection cn = mock(Connection.class);
        PreparedStatement psSelect1 = mock(PreparedStatement.class);
        PreparedStatement psInsert  = mock(PreparedStatement.class);
        PreparedStatement psSelect2 = mock(PreparedStatement.class);
        ResultSet rs1 = mock(ResultSet.class);
        ResultSet rs2 = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("CARRITO")).thenReturn("CARRITO");
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(anyString()))
                    .thenReturn(psSelect1, psInsert, psSelect2);

           
            when(psSelect1.executeQuery()).thenReturn(rs1);
            when(rs1.next()).thenReturn(false);

           
            when(psInsert.executeUpdate()).thenReturn(1);

           
            when(psSelect2.executeQuery()).thenReturn(rs2);
            when(rs2.next()).thenReturn(false);

            ComprasDAO dao = new ComprasDAO();

            SQLException ex = assertThrows(SQLException.class,
                    () -> dao.ensureCartForUser(userId));

            assertTrue(ex.getMessage().contains("No fue posible crear/obtener el carrito."));
        }
    }

    @Test
@DisplayName("getCart con carrito sin resumen ni ítems deja total en 0 y lista vacía")
void getCart_sinResumen_niItems_devuelveTotalCeroYListaVacia() throws Exception {
    long userId = 123L;
    long cartId = 999L;

    Connection cn = mock(Connection.class);
    PreparedStatement psHeader = mock(PreparedStatement.class);
    PreparedStatement psItems  = mock(PreparedStatement.class);
    ResultSet rsHeader = mock(ResultSet.class);
    ResultSet rsItems  = mock(ResultSet.class);

    ComprasDAO dao = spy(new ComprasDAO());
    doReturn(cartId).when(dao).ensureCartForUser(userId);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(DB::getConnection).thenReturn(cn);
        dbMock.when(() -> DB.table("VW_CARRITO_RESUMEN")).thenReturn("VW_CARRITO_RESUMEN");
        dbMock.when(() -> DB.table("CARRITO_ITEM")).thenReturn("CARRITO_ITEM");
        dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");
        dbMock.when(() -> DB.table("CLASE_ASIENTO")).thenReturn("CLASE_ASIENTO");
        dbMock.when(() -> DB.table("RUTA")).thenReturn("RUTA");
        dbMock.when(() -> DB.table("CIUDAD")).thenReturn("CIUDAD");
        dbMock.when(() -> DB.table("PAIS")).thenReturn("PAIS");

        when(cn.prepareStatement(anyString()))
                .thenReturn(psHeader, psItems);

        when(psHeader.executeQuery()).thenReturn(rsHeader);
        when(rsHeader.next()).thenReturn(false);

        when(psItems.executeQuery()).thenReturn(rsItems);
        when(rsItems.next()).thenReturn(false);

        CompraDTO.CarritoResp cart = dao.getCart(userId);

        assertNotNull(cart);
        assertEquals(BigDecimal.ZERO, cart.total);
        assertNotNull(cart.items);
        assertTrue(cart.items.isEmpty());
    }
}

@Test
@DisplayName("getConn envuelve cualquier error en SQLException con mensaje descriptivo")
void getConn_excepcionEnDB_envuelveSQLException() throws Exception {
    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

        dbMock.when(DB::getConnection).thenThrow(new RuntimeException("Fallo demo"));

        Method m = ComprasDAO.class.getDeclaredMethod("getConn");
        m.setAccessible(true);

        ComprasDAO dao = new ComprasDAO();

        SQLException ex = assertThrows(SQLException.class, () -> {
            try {

                m.invoke(dao);
            } catch (java.lang.reflect.InvocationTargetException ite) {
                Throwable target = ite.getTargetException();
                if (target instanceof Exception e) throw e;
                throw ite;
            }
        });

        assertTrue(ex.getMessage().startsWith("No se pudo obtener conexión desde DB:"));
        assertTrue(ex.getMessage().contains("Fallo demo"));
    }
}

    @Test
    @DisplayName("crearCarritoClienteFinal reutiliza carrito existente y limpia ítems anteriores")
    void crearCarritoClienteFinal_clienteYaTieneCarrito_reemplazaItems() throws Exception {
        long cartWsId   = 100L;
        long userFinal  = 200L;
        long existingId = 555L;

        Connection cn = mock(Connection.class);
        PreparedStatement psSelect = mock(PreparedStatement.class);
        PreparedStatement psDelete = mock(PreparedStatement.class);
        PreparedStatement psMerge  = mock(PreparedStatement.class);
        ResultSet rsSelect = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("CARRITO")).thenReturn("CARRITO");
            dbMock.when(() -> DB.table("CARRITO_ITEM")).thenReturn("CARRITO_ITEM");

            when(cn.prepareStatement(anyString()))
                    .thenReturn(psSelect, psDelete, psMerge);

           
            when(psSelect.executeQuery()).thenReturn(rsSelect);
            when(rsSelect.next()).thenReturn(true);
            when(rsSelect.getLong(1)).thenReturn(existingId);

            when(psDelete.executeUpdate()).thenReturn(2);
            when(psMerge.executeUpdate()).thenReturn(3); 

            ComprasDAO dao = new ComprasDAO();

            long id = (long) invokePrivate(
                    dao,
                    "crearCarritoClienteFinal",
                    new Class<?>[]{Connection.class, long.class, long.class},
                    cn, cartWsId, userFinal
            );

            assertEquals(existingId, id);
           
            verify(psDelete).setLong(1, existingId);
           
            verify(psMerge).setLong(1, existingId);
            verify(psMerge).setLong(2, cartWsId);
        }
    }


      @Test
    @DisplayName("getParejaId devuelve null cuando la columna viene en NULL")
    void getParejaId_columnaNull_devuelveNull() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");

            String expectedSql = "SELECT ID_VUELO_PAREJA FROM VUELO WHERE ID_VUELO=?";
            when(cn.prepareStatement(expectedSql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true);
            when(rs.getLong(1)).thenReturn(0L);   
            when(rs.wasNull()).thenReturn(true);  

            ComprasDAO dao = new ComprasDAO();
            Long pareja = (Long) invokePrivate(
                    dao,
                    "getParejaId",
                    new Class<?>[]{Connection.class, long.class},
                    cn, 123L
            );

            assertNull(pareja, "Debe devolver null cuando la columna es NULL");
        }
    }

    @Test
    @DisplayName("validarVueloDisponible lanza SQLException cuando el vuelo está inactivo")
    void validarVueloDisponible_inactivo_lanzaExcepcion() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");
            dbMock.when(() -> DB.table("ESTADOS")).thenReturn("ESTADOS");

            when(cn.prepareStatement(anyString())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true);
            when(rs.getInt("ACTIVO")).thenReturn(0);         
            when(rs.getString("ESTADO")).thenReturn("OK");   

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
@DisplayName("getConn cuando la excepción no tiene mensaje usa solo el texto base")
void getConn_excepcionSinMensaje_usaMensajeBase() throws Exception {
    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

        dbMock.when(DB::getConnection)
              .thenThrow(new RuntimeException()); 

        Method m = ComprasDAO.class.getDeclaredMethod("getConn");
        m.setAccessible(true);
        ComprasDAO dao = new ComprasDAO();

        SQLException ex = assertThrows(SQLException.class, () -> {
            try {
                m.invoke(dao);
            } catch (java.lang.reflect.InvocationTargetException ite) {
                Throwable target = ite.getTargetException();
                if (target instanceof Exception e) throw e;
                throw ite;
            }
        });

        assertEquals("No se pudo obtener conexión desde DB:", ex.getMessage());
    }
}

@Test
@DisplayName("getRealConnection usa DB.getConnection cuando está disponible")
void getRealConnection_usaDBgetConnection() throws Exception {
    Connection cn = mock(Connection.class);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(DB::getConnection).thenReturn(cn);

        Method m = ComprasDAO.class.getDeclaredMethod("getRealConnection");
        m.setAccessible(true);

        Connection res;
        try {
            res = (Connection) m.invoke(null);
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable target = ite.getTargetException();
            if (target instanceof Exception e) throw e;
            throw ite;
        }

        assertSame(cn, res, "getRealConnection debe devolver la conexión de DB.getConnection()");
        dbMock.verify(DB::getConnection);
    }
}

@Test
@DisplayName("getRealConnection envuelve errores en SQLException con mensaje descriptivo")
void getRealConnection_error_envuelveSQLException() throws Exception {
    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
        dbMock.when(DB::getConnection).thenThrow(new SQLException("Fallo demo"));

        Method m = ComprasDAO.class.getDeclaredMethod("getRealConnection");
        m.setAccessible(true);

        SQLException ex = assertThrows(SQLException.class, () -> {
            try {
                m.invoke(null);
            } catch (java.lang.reflect.InvocationTargetException ite) {
                Throwable target = ite.getTargetException();
                if (target instanceof Exception e) throw e;
                throw ite;
            }
        });

        assertTrue(ex.getMessage().startsWith("No se pudo obtener conexión desde DB:"));

        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof SQLException);
    }
}

}
