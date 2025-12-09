package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.dto.ComentarioDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.*;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ComentarioDAOTest {

    @Test
    @DisplayName("listarPublic(idVuelo) arma correctamente un comentario raíz con una respuesta")
    void listarPublic_armaArbolComentarios() throws Exception {
        long idVuelo = 10L;

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("VUELO_COMENTARIO")).thenReturn("VUELO_COMENTARIO");
            dbMock.when(() -> DB.table("USUARIO")).thenReturn("USUARIO");
            dbMock.when(() -> DB.table("RESENA_VUELO")).thenReturn("RESENA_VUELO");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String comentarioTable = "VUELO_COMENTARIO";
            String usuarioTable = "USUARIO";
            String resenaTable = "RESENA_VUELO";

            String expectedSql =
                "SELECT vc.ID_COMENTARIO, vc.ID_VUELO, vc.ID_USUARIO, " +
                "COALESCE(TRIM(NVL(u.NOMBRES,'') || ' ' || NVL(u.APELLIDOS,'')), u.EMAIL) AS AUTOR, " +
                "vc.COMENTARIO, vc.ID_PADRE, vc.CREADA_EN, " +
                "(SELECT rv.CALIFICACION FROM " + resenaTable + " rv " +
                "WHERE rv.ID_VUELO = vc.ID_VUELO AND rv.ID_USUARIO_AUTOR = vc.ID_USUARIO " +
                "FETCH FIRST 1 ROWS ONLY) AS RATING_AUTOR " +
                "FROM " + comentarioTable + " vc JOIN " + usuarioTable + " u ON u.ID_USUARIO = vc.ID_USUARIO " +
                "WHERE vc.ID_VUELO = ? ORDER BY vc.CREADA_EN ASC";

            when(cn.prepareStatement(expectedSql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true, true, false);

            when(rs.getLong("ID_COMENTARIO")).thenReturn(1L, 2L);
            when(rs.getLong("ID_VUELO")).thenReturn(idVuelo, idVuelo);
            when(rs.getLong("ID_USUARIO")).thenReturn(100L, 101L);
            when(rs.getString("AUTOR")).thenReturn("Autor 1", "Autor 2");
            when(rs.getString("COMENTARIO"))
                    .thenReturn("Comentario root", "Comentario respuesta");

            when(rs.getObject("ID_PADRE")).thenReturn(null, 1L);
            when(rs.getLong("ID_PADRE")).thenReturn(1L);

            Timestamp t1 = Timestamp.from(Instant.parse("2024-01-01T10:00:00Z"));
            Timestamp t2 = Timestamp.from(Instant.parse("2024-01-01T11:00:00Z"));
            when(rs.getTimestamp("CREADA_EN")).thenReturn(t1, t2);

            when(rs.getObject("RATING_AUTOR")).thenReturn(5, 4);

            ComentarioDAO dao = new ComentarioDAO();

            List<ComentarioDTO.View> roots = dao.listarPublic(idVuelo);

            assertNotNull(roots);
            assertEquals(1, roots.size(), "Debe haber 1 comentario raíz");

            ComentarioDTO.View root = roots.get(0);
            assertEquals("Comentario root", root.getComentario());
            assertEquals(1L, root.getIdComentario());
            assertNull(root.getIdPadre());

            assertEquals(1, root.getRespuestas().size(), "El root debe tener 1 respuesta");
            ComentarioDTO.View reply = root.getRespuestas().get(0);
            assertEquals("Comentario respuesta", reply.getComentario());
            assertEquals(root.getIdComentario(), reply.getIdPadre());
        }
    }

    @Test
    @DisplayName("crear() debe lanzar SQLException si el comentario es nulo o vacío")
    void crear_comentarioVacio_lanzaExcepcion() {
        ComentarioDAO dao = new ComentarioDAO();

        SQLException ex1 = assertThrows(
                SQLException.class,
                () -> dao.crear(1L, 1L, null, null)
        );
        assertTrue(ex1.getMessage().contains("El comentario es requerido"));

        SQLException ex2 = assertThrows(
                SQLException.class,
                () -> dao.crear(1L, 1L, "   ", null)
        );
        assertTrue(ex2.getMessage().contains("El comentario es requerido"));
    }

    @Test
    @DisplayName("crear() debe lanzar SQLException si el comentario supera los 2000 caracteres")
    void crear_comentarioMuyLargo_lanzaExcepcion() {
        ComentarioDAO dao = new ComentarioDAO();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2001; i++) {
            sb.append('x');
        }
        String largo = sb.toString();

        SQLException ex = assertThrows(
                SQLException.class,
                () -> dao.crear(1L, 1L, largo, null)
        );
        assertTrue(ex.getMessage().contains("2000"));
    }

    @Test
    @DisplayName("crear() inserta correctamente cuando vuelo existe y no hay padre")
    void crear_ok_sinPadre() throws Exception {
        long idVuelo = 10L;
        long idUsuario = 50L;

        Connection cn = mock(Connection.class);
        PreparedStatement psChkVuelo = mock(PreparedStatement.class);
        ResultSet rsChkVuelo = mock(ResultSet.class);

        PreparedStatement psInsert = mock(PreparedStatement.class);
        ResultSet rsGenKeys = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");
            dbMock.when(() -> DB.table("VUELO_COMENTARIO")).thenReturn("VUELO_COMENTARIO");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String chkVueloSql = "SELECT 1 FROM VUELO WHERE ID_VUELO=?";
            String insSql = "INSERT INTO VUELO_COMENTARIO (ID_VUELO, ID_USUARIO, ID_PADRE, COMENTARIO, CREADA_EN) VALUES (?,?,?,?, SYSTIMESTAMP)";

            when(cn.prepareStatement(chkVueloSql)).thenReturn(psChkVuelo);
            when(cn.prepareStatement(eq(insSql), any(String[].class))).thenReturn(psInsert);

            when(psChkVuelo.executeQuery()).thenReturn(rsChkVuelo);
            when(rsChkVuelo.next()).thenReturn(true);

            when(psInsert.executeUpdate()).thenReturn(1);
            when(psInsert.getGeneratedKeys()).thenReturn(rsGenKeys);
            when(rsGenKeys.next()).thenReturn(true);
            when(rsGenKeys.getLong(1)).thenReturn(123L);

            ComentarioDAO dao = new ComentarioDAO();

            long idGenerado = dao.crear(idVuelo, idUsuario, "Hola mundo", null);

            assertEquals(123L, idGenerado);
            verify(cn).setAutoCommit(false);
            verify(cn).commit();
            verify(cn).setAutoCommit(true);

            verify(psInsert).setLong(1, idVuelo);
            verify(psInsert).setLong(2, idUsuario);
            verify(psInsert).setNull(3, Types.NUMERIC);
            verify(psInsert).setString(4, "Hola mundo");
        }
    }

    @Test
    @DisplayName("crear() inserta correctamente cuando el comentario tiene padre válido")
    void crear_ok_conPadre() throws Exception {
        long idVuelo = 10L;
        long idUsuario = 50L;
        long idPadre = 999L;

        Connection cn = mock(Connection.class);

        PreparedStatement psChkVuelo = mock(PreparedStatement.class);
        ResultSet rsChkVuelo = mock(ResultSet.class);

        PreparedStatement psChkPadre = mock(PreparedStatement.class);
        ResultSet rsChkPadre = mock(ResultSet.class);

        PreparedStatement psInsert = mock(PreparedStatement.class);
        ResultSet rsGenKeys = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");
            dbMock.when(() -> DB.table("VUELO_COMENTARIO")).thenReturn("VUELO_COMENTARIO");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String chkVueloSql = "SELECT 1 FROM VUELO WHERE ID_VUELO=?";
            String chkPadreSql = "SELECT 1 FROM VUELO_COMENTARIO WHERE ID_COMENTARIO=? AND ID_VUELO=?";
            String insSql = "INSERT INTO VUELO_COMENTARIO (ID_VUELO, ID_USUARIO, ID_PADRE, COMENTARIO, CREADA_EN) VALUES (?,?,?,?, SYSTIMESTAMP)";

            when(cn.prepareStatement(chkVueloSql)).thenReturn(psChkVuelo);
            when(cn.prepareStatement(chkPadreSql)).thenReturn(psChkPadre);
            when(cn.prepareStatement(eq(insSql), any(String[].class))).thenReturn(psInsert);

            when(psChkVuelo.executeQuery()).thenReturn(rsChkVuelo);
            when(rsChkVuelo.next()).thenReturn(true);

            when(psChkPadre.executeQuery()).thenReturn(rsChkPadre);
            when(rsChkPadre.next()).thenReturn(true);

            when(psInsert.executeUpdate()).thenReturn(1);
            when(psInsert.getGeneratedKeys()).thenReturn(rsGenKeys);
            when(rsGenKeys.next()).thenReturn(true);
            when(rsGenKeys.getLong(1)).thenReturn(555L);

            ComentarioDAO dao = new ComentarioDAO();

            long idGenerado = dao.crear(idVuelo, idUsuario, "Hola con padre válido", idPadre);

            assertEquals(555L, idGenerado);
            verify(cn).setAutoCommit(false);
            verify(cn).commit();
            verify(cn).setAutoCommit(true);

            verify(psInsert).setLong(1, idVuelo);
            verify(psInsert).setLong(2, idUsuario);
            verify(psInsert).setLong(3, idPadre);
            verify(psInsert).setString(4, "Hola con padre válido");
        }
    }

    @Test
    @DisplayName("crear() lanza SQLException si el comentario padre no existe en el vuelo")
    void crear_conPadreNoExiste_lanzaExcepcion() throws Exception {
        long idVuelo = 10L;
        long idUsuario = 50L;
        long idPadre = 999L;

        Connection cn = mock(Connection.class);
        PreparedStatement psChkVuelo = mock(PreparedStatement.class);
        ResultSet rsChkVuelo = mock(ResultSet.class);

        PreparedStatement psChkPadre = mock(PreparedStatement.class);
        ResultSet rsChkPadre = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");
            dbMock.when(() -> DB.table("VUELO_COMENTARIO")).thenReturn("VUELO_COMENTARIO");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String chkVueloSql = "SELECT 1 FROM VUELO WHERE ID_VUELO=?";
            String chkPadreSql = "SELECT 1 FROM VUELO_COMENTARIO WHERE ID_COMENTARIO=? AND ID_VUELO=?";

            when(cn.prepareStatement(chkVueloSql)).thenReturn(psChkVuelo);
            when(cn.prepareStatement(chkPadreSql)).thenReturn(psChkPadre);

            when(psChkVuelo.executeQuery()).thenReturn(rsChkVuelo);
            when(rsChkVuelo.next()).thenReturn(true);

            when(psChkPadre.executeQuery()).thenReturn(rsChkPadre);
            when(rsChkPadre.next()).thenReturn(false);

            ComentarioDAO dao = new ComentarioDAO();

            SQLException ex = assertThrows(
                    SQLException.class,
                    () -> dao.crear(idVuelo, idUsuario, "Hola con padre", idPadre)
            );

            assertTrue(ex.getMessage().contains("padre no existe"), "Debe indicar que el comentario padre no existe");

            verify(cn).setAutoCommit(false);
            verify(cn).rollback();
            verify(cn).setAutoCommit(true);
        }
    }

    @Test
    @DisplayName("crear() lanza SQLException cuando no se genera ID_COMENTARIO")
    void crear_noGeneraId_lanzaSQLException() throws Exception {
        long idVuelo = 10L;
        long idUsuario = 50L;

        Connection cn = mock(Connection.class);
        PreparedStatement psChkVuelo = mock(PreparedStatement.class);
        ResultSet rsChkVuelo = mock(ResultSet.class);

        PreparedStatement psInsert = mock(PreparedStatement.class);
        ResultSet rsGenKeys = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");
            dbMock.when(() -> DB.table("VUELO_COMENTARIO")).thenReturn("VUELO_COMENTARIO");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String chkVueloSql = "SELECT 1 FROM VUELO WHERE ID_VUELO=?";
            String insSql = "INSERT INTO VUELO_COMENTARIO (ID_VUELO, ID_USUARIO, ID_PADRE, COMENTARIO, CREADA_EN) VALUES (?,?,?,?, SYSTIMESTAMP)";

            when(cn.prepareStatement(chkVueloSql)).thenReturn(psChkVuelo);
            when(cn.prepareStatement(eq(insSql), any(String[].class))).thenReturn(psInsert);

            when(psChkVuelo.executeQuery()).thenReturn(rsChkVuelo);
            when(rsChkVuelo.next()).thenReturn(true);

            when(psInsert.executeUpdate()).thenReturn(1);
            when(psInsert.getGeneratedKeys()).thenReturn(rsGenKeys);
            when(rsGenKeys.next()).thenReturn(false); 

            ComentarioDAO dao = new ComentarioDAO();

            SQLException ex = assertThrows(
                    SQLException.class,
                    () -> dao.crear(idVuelo, idUsuario, "Hola sin id", null)
            );

            assertTrue(ex.getMessage().contains("No se generó ID_COMENTARIO"));

            verify(cn).setAutoCommit(false);
            verify(cn).rollback();
            verify(cn).setAutoCommit(true);
            verify(cn, never()).commit();
        }
    }

    @Test
    @DisplayName("crear() envuelve excepciones no SQL en SQLException 'Error al crear comentario'")
    void crear_errorGenerico_envuelveSQLException() throws Exception {
        long idVuelo = 10L;
        long idUsuario = 50L;

        Connection cn = mock(Connection.class);
        PreparedStatement psChkVuelo = mock(PreparedStatement.class);
        ResultSet rsChkVuelo = mock(ResultSet.class);

        PreparedStatement psInsert = mock(PreparedStatement.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("VUELO")).thenReturn("VUELO");
            dbMock.when(() -> DB.table("VUELO_COMENTARIO")).thenReturn("VUELO_COMENTARIO");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String chkVueloSql = "SELECT 1 FROM VUELO WHERE ID_VUELO=?";
            String insSql = "INSERT INTO VUELO_COMENTARIO (ID_VUELO, ID_USUARIO, ID_PADRE, COMENTARIO, CREADA_EN) VALUES (?,?,?,?, SYSTIMESTAMP)";

            when(cn.prepareStatement(chkVueloSql)).thenReturn(psChkVuelo);
            when(cn.prepareStatement(eq(insSql), any(String[].class))).thenReturn(psInsert);

            when(psChkVuelo.executeQuery()).thenReturn(rsChkVuelo);
            when(rsChkVuelo.next()).thenReturn(true);

            when(psInsert.executeUpdate()).thenThrow(new RuntimeException("Falla rara"));

            ComentarioDAO dao = new ComentarioDAO();

            SQLException ex = assertThrows(
                    SQLException.class,
                    () -> dao.crear(idVuelo, idUsuario, "Hola con fallo", null)
            );

            assertEquals("Error al crear comentario", ex.getMessage());
            assertNotNull(ex.getCause());
            assertTrue(ex.getCause() instanceof RuntimeException);

            verify(cn).setAutoCommit(false);
            verify(cn).rollback();
            verify(cn).setAutoCommit(true);
            verify(cn, never()).commit();
        }
    }
}
