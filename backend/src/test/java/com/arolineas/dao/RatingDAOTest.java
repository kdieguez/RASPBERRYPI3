package com.aerolineas.dao;

import com.aerolineas.config.DB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.math.BigDecimal;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RatingDAOTest {

    @Test
    @DisplayName("resumen devuelve promedio, total y mi rating cuando hay datos")
    void resumen_conDatosYMiRating() throws Exception {
        long idVuelo = 10L;
        long idUsuario = 5L;

        Connection cn = mock(Connection.class);
        PreparedStatement psSum = mock(PreparedStatement.class);
        PreparedStatement psMine = mock(PreparedStatement.class);
        ResultSet rsSum = mock(ResultSet.class);
        ResultSet rsMine = mock(ResultSet.class);

        String table = "RESENA_VUELO";
        String qSum  = "SELECT AVG(CALIFICACION) AS PROM, COUNT(*) AS TOT FROM " + table + " WHERE ID_VUELO = ?";
        String qMine = "SELECT CALIFICACION FROM " + table + " WHERE ID_VUELO = ? AND ID_USUARIO_AUTOR = ?";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(() -> DB.table("RESENA_VUELO")).thenReturn(table);
            db.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(qSum)).thenReturn(psSum);
            when(psSum.executeQuery()).thenReturn(rsSum);
            when(rsSum.next()).thenReturn(true);
            when(rsSum.getBigDecimal("PROM")).thenReturn(new BigDecimal("4.5"));
            when(rsSum.getLong("TOT")).thenReturn(3L);

            when(cn.prepareStatement(qMine)).thenReturn(psMine);
            when(psMine.executeQuery()).thenReturn(rsMine);
            when(rsMine.next()).thenReturn(true);
            when(rsMine.getInt(1)).thenReturn(4);
            when(rsMine.wasNull()).thenReturn(false);

            RatingDAO dao = new RatingDAO();
            RatingDAO.Resumen r = dao.resumen(idVuelo, idUsuario);

            assertEquals(4.5, r.promedio(), 0.0001);
            assertEquals(3L, r.total());
            assertEquals(4, r.miRating());

            verify(psSum).setLong(1, idVuelo);
            verify(psMine).setLong(1, idVuelo);
            verify(psMine).setLong(2, idUsuario);
        }
    }

    @Test
    @DisplayName("resumen sin registros y sin usuario retorna 0,0,null")
    void resumen_sinDatos_ySinUsuario() throws Exception {
        long idVuelo = 20L;

        Connection cn = mock(Connection.class);
        PreparedStatement psSum = mock(PreparedStatement.class);
        ResultSet rsSum = mock(ResultSet.class);

        String table = "RESENA_VUELO";
        String qSum  = "SELECT AVG(CALIFICACION) AS PROM, COUNT(*) AS TOT FROM " + table + " WHERE ID_VUELO = ?";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(() -> DB.table("RESENA_VUELO")).thenReturn(table);
            db.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(qSum)).thenReturn(psSum);
            when(psSum.executeQuery()).thenReturn(rsSum);
            when(rsSum.next()).thenReturn(false);   

            RatingDAO dao = new RatingDAO();
            RatingDAO.Resumen r = dao.resumen(idVuelo, null);

            assertEquals(0.0, r.promedio(), 0.0001);
            assertEquals(0L, r.total());
            assertNull(r.miRating());

            verify(psSum).setLong(1, idVuelo);
            
            verify(cn, never()).prepareStatement(contains("ID_USUARIO_AUTOR"));
        }
    }

    @Test
    @DisplayName("resumen con usuario pero sin rating propio deja miRating en null")
    void resumen_sinMiRating() throws Exception {
        long idVuelo = 30L;
        long idUsuario = 7L;

        Connection cn = mock(Connection.class);
        PreparedStatement psSum  = mock(PreparedStatement.class);
        PreparedStatement psMine = mock(PreparedStatement.class);
        ResultSet rsSum  = mock(ResultSet.class);
        ResultSet rsMine = mock(ResultSet.class);

        String table = "RESENA_VUELO";
        String qSum  = "SELECT AVG(CALIFICACION) AS PROM, COUNT(*) AS TOT FROM " + table + " WHERE ID_VUELO = ?";
        String qMine = "SELECT CALIFICACION FROM " + table + " WHERE ID_VUELO = ? AND ID_USUARIO_AUTOR = ?";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(() -> DB.table("RESENA_VUELO")).thenReturn(table);
            db.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(qSum)).thenReturn(psSum);
            when(psSum.executeQuery()).thenReturn(rsSum);
            when(rsSum.next()).thenReturn(true);
            when(rsSum.getBigDecimal("PROM")).thenReturn(null); 
            when(rsSum.getLong("TOT")).thenReturn(0L);

            when(cn.prepareStatement(qMine)).thenReturn(psMine);
            when(psMine.executeQuery()).thenReturn(rsMine);
            when(rsMine.next()).thenReturn(false);              

            RatingDAO dao = new RatingDAO();
            RatingDAO.Resumen r = dao.resumen(idVuelo, idUsuario);

            assertEquals(0.0, r.promedio(), 0.0001);
            assertEquals(0L, r.total());
            assertNull(r.miRating());
        }
    }

    @Test
    @DisplayName("resumen trata miRating como null cuando wasNull() es true")
    void resumen_miRatingWasNull() throws Exception {
        long idVuelo = 40L;
        long idUsuario = 8L;

        Connection cn = mock(Connection.class);
        PreparedStatement psSum  = mock(PreparedStatement.class);
        PreparedStatement psMine = mock(PreparedStatement.class);
        ResultSet rsSum  = mock(ResultSet.class);
        ResultSet rsMine = mock(ResultSet.class);

        String table = "RESENA_VUELO";
        String qSum  = "SELECT AVG(CALIFICACION) AS PROM, COUNT(*) AS TOT FROM " + table + " WHERE ID_VUELO = ?";
        String qMine = "SELECT CALIFICACION FROM " + table + " WHERE ID_VUELO = ? AND ID_USUARIO_AUTOR = ?";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(() -> DB.table("RESENA_VUELO")).thenReturn(table);
            db.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(qSum)).thenReturn(psSum);
            when(psSum.executeQuery()).thenReturn(rsSum);
            when(rsSum.next()).thenReturn(true);
            when(rsSum.getBigDecimal("PROM")).thenReturn(new BigDecimal("3.0"));
            when(rsSum.getLong("TOT")).thenReturn(1L);

            when(cn.prepareStatement(qMine)).thenReturn(psMine);
            when(psMine.executeQuery()).thenReturn(rsMine);
            when(rsMine.next()).thenReturn(true);
            when(rsMine.getInt(1)).thenReturn(0);     
            when(rsMine.wasNull()).thenReturn(true);  

            RatingDAO dao = new RatingDAO();
            RatingDAO.Resumen r = dao.resumen(idVuelo, idUsuario);

            assertEquals(3.0, r.promedio(), 0.0001);
            assertEquals(1L, r.total());
            assertNull(r.miRating());
        }
    }
    
    @Test
    @DisplayName("upsertRating lanza SQLException si calificacion está fuera de 1..5")
    void upsertRating_calificacionInvalida() throws Exception {
        RatingDAO dao = new RatingDAO();

        SQLException ex1 = assertThrows(SQLException.class,
                () -> dao.upsertRating(1L, 1L, 0));
        assertEquals("calificacion debe ser 1..5", ex1.getMessage());

        SQLException ex2 = assertThrows(SQLException.class,
                () -> dao.upsertRating(1L, 1L, 6));
        assertEquals("calificacion debe ser 1..5", ex2.getMessage());
    }

    @Test
    @DisplayName("upsertRating actualiza rating existente sin insertar")
    void upsertRating_actualizaSinInsert() throws Exception {
        long idVuelo = 10L;
        long idUsuario = 20L;
        int calificacion = 4;

        Connection cn = mock(Connection.class);
        PreparedStatement psUpd = mock(PreparedStatement.class);

        String table = "RESENA_VUELO";
        String upd = "UPDATE " + table + " SET CALIFICACION = ?, CREADA_EN = SYSTIMESTAMP WHERE ID_VUELO = ? AND ID_USUARIO_AUTOR = ?";
        String ins = "INSERT INTO " + table + " (ID_VUELO, ID_USUARIO_AUTOR, CALIFICACION, CREADA_EN) VALUES (?,?,?, SYSTIMESTAMP)";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(() -> DB.table("RESENA_VUELO")).thenReturn(table);
            db.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(upd)).thenReturn(psUpd);
            when(psUpd.executeUpdate()).thenReturn(1); 

            RatingDAO dao = new RatingDAO();
            dao.upsertRating(idVuelo, idUsuario, calificacion);

            verify(cn).setAutoCommit(false);
            verify(psUpd).setInt(1, calificacion);
            verify(psUpd).setLong(2, idVuelo);
            verify(psUpd).setLong(3, idUsuario);
            verify(psUpd).executeUpdate();
            verify(cn).commit();
            verify(cn).setAutoCommit(true);

            
            verify(cn, never()).prepareStatement(ins);
        }
    }

    @Test
    @DisplayName("upsertRating inserta rating cuando el update no afecta filas")
    void upsertRating_insertaCuandoNoExiste() throws Exception {
        long idVuelo = 11L;
        long idUsuario = 21L;
        int calificacion = 5;

        Connection cn = mock(Connection.class);
        PreparedStatement psUpd = mock(PreparedStatement.class);
        PreparedStatement psIns = mock(PreparedStatement.class);

        String table = "RESENA_VUELO";
        String upd = "UPDATE " + table + " SET CALIFICACION = ?, CREADA_EN = SYSTIMESTAMP WHERE ID_VUELO = ? AND ID_USUARIO_AUTOR = ?";
        String ins = "INSERT INTO " + table + " (ID_VUELO, ID_USUARIO_AUTOR, CALIFICACION, CREADA_EN) VALUES (?,?,?, SYSTIMESTAMP)";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(() -> DB.table("RESENA_VUELO")).thenReturn(table);
            db.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(upd)).thenReturn(psUpd);
            when(psUpd.executeUpdate()).thenReturn(0); 

            when(cn.prepareStatement(ins)).thenReturn(psIns);

            RatingDAO dao = new RatingDAO();
            dao.upsertRating(idVuelo, idUsuario, calificacion);

            verify(psUpd).executeUpdate();
            verify(psIns).setLong(1, idVuelo);
            verify(psIns).setLong(2, idUsuario);
            verify(psIns).setInt(3, calificacion);
            verify(psIns).executeUpdate();
            verify(cn).commit();
            verify(cn).setAutoCommit(true);
        }
    }

    @Test
    @DisplayName("upsertRating propaga SQLException y hace rollback")
    void upsertRating_lanzaSQLExceptionYRollback() throws Exception {
        long idVuelo = 12L;
        long idUsuario = 22L;

        Connection cn = mock(Connection.class);

        String table = "RESENA_VUELO";
        String upd = "UPDATE " + table + " SET CALIFICACION = ?, CREADA_EN = SYSTIMESTAMP WHERE ID_VUELO = ? AND ID_USUARIO_AUTOR = ?";

        SQLException boom = new SQLException("DB error");

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(() -> DB.table("RESENA_VUELO")).thenReturn(table);
            db.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(upd)).thenThrow(boom);

            RatingDAO dao = new RatingDAO();
            SQLException ex = assertThrows(SQLException.class,
                    () -> dao.upsertRating(idVuelo, idUsuario, 3));

            assertSame(boom, ex); 
            verify(cn).setAutoCommit(false);
            verify(cn).rollback();
            verify(cn).setAutoCommit(true);
        }
    }

    @Test
    @DisplayName("upsertRating envuelve excepciones no SQL en SQLException con mensaje personalizado")
    void upsertRating_envuelveRuntimeEnSQLException() throws Exception {
        long idVuelo = 13L;
        long idUsuario = 23L;

        Connection cn = mock(Connection.class);
        PreparedStatement psUpd = mock(PreparedStatement.class);

        String table = "RESENA_VUELO";
        String upd = "UPDATE " + table + " SET CALIFICACION = ?, CREADA_EN = SYSTIMESTAMP WHERE ID_VUELO = ? AND ID_USUARIO_AUTOR = ?";

        RuntimeException boom = new RuntimeException("boom");

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(() -> DB.table("RESENA_VUELO")).thenReturn(table);
            db.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(upd)).thenReturn(psUpd);
            when(psUpd.executeUpdate()).thenThrow(boom);

            RatingDAO dao = new RatingDAO();
            SQLException ex = assertThrows(SQLException.class,
                    () -> dao.upsertRating(idVuelo, idUsuario, 2));

            assertEquals("No se pudo registrar el rating", ex.getMessage());
            assertSame(boom, ex.getCause());
            verify(cn).rollback();
            verify(cn).setAutoCommit(true);
        }
    }
}
