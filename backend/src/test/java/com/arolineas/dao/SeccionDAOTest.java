package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.dto.SeccionDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SeccionDAOTest {

    @Test
    @DisplayName("crear inserta sección con descripción y orden nulos y devuelve ID_SECCION")
    void crear_conNulls_devuelveId() throws Exception {
        long idPagina = 5L;
        SeccionDTO.Upsert dto = new SeccionDTO.Upsert();
        dto.nombreSeccion = "Historia";
        dto.descripcion = null;
        dto.orden = null;

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rsKeys = mock(ResultSet.class);

        String seccionTable = "SECCION_INFORMATIVA";
        String sql = "INSERT INTO " + seccionTable +
                " (ID_PAGINA, NOMBRE_SECCION, DESCRIPCION, ORDEN) VALUES (?,?,?,?)";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("SECCION_INFORMATIVA")).thenReturn(seccionTable);

            when(cn.prepareStatement(sql, new String[]{"ID_SECCION"})).thenReturn(ps);
            when(ps.getGeneratedKeys()).thenReturn(rsKeys);
            when(rsKeys.next()).thenReturn(true);
            when(rsKeys.getLong(1)).thenReturn(123L);

            SeccionDAO dao = new SeccionDAO();
            long id = dao.crear(idPagina, dto);

            assertEquals(123L, id);
            verify(ps).setLong(1, idPagina);
            verify(ps).setString(2, "Historia");
            verify(ps).setNull(3, Types.CLOB);
            verify(ps).setNull(4, Types.INTEGER);
            verify(ps).executeUpdate();
        }
    }

    @Test
    @DisplayName("crear lanza SQLException cuando no se genera ID_SECCION")
    void crear_sinGeneratedKey_lanzaSQLException() throws Exception {
        long idPagina = 5L;
        SeccionDTO.Upsert dto = new SeccionDTO.Upsert();
        dto.nombreSeccion = "Historia";
        dto.descripcion = "desc";
        dto.orden = 1;

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rsKeys = mock(ResultSet.class);

        String seccionTable = "SECCION_INFORMATIVA";
        String sql = "INSERT INTO " + seccionTable +
                " (ID_PAGINA, NOMBRE_SECCION, DESCRIPCION, ORDEN) VALUES (?,?,?,?)";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("SECCION_INFORMATIVA")).thenReturn(seccionTable);

            when(cn.prepareStatement(sql, new String[]{"ID_SECCION"})).thenReturn(ps);
            when(ps.getGeneratedKeys()).thenReturn(rsKeys);
            when(rsKeys.next()).thenReturn(false);  

            SeccionDAO dao = new SeccionDAO();

            SQLException ex = assertThrows(
                    SQLException.class,
                    () -> dao.crear(idPagina, dto)
            );
            assertEquals("No se generó ID_SECCION", ex.getMessage());
        }
    }

    @Test
    @DisplayName("actualizar modifica la sección; usa setNull cuando descripción/orden son null")
    void actualizar_ok_conNulls() throws Exception {
        long idSeccion = 7L;
        SeccionDTO.Upsert dto = new SeccionDTO.Upsert();
        dto.nombreSeccion = "Misión";
        dto.descripcion = null;
        dto.orden = null;

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        String seccionTable = "SECCION_INFORMATIVA";
        String sql = "UPDATE " + seccionTable +
                " SET NOMBRE_SECCION = ?, DESCRIPCION = ?, ORDEN = NVL(?, ORDEN) WHERE ID_SECCION = ?";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("SECCION_INFORMATIVA")).thenReturn(seccionTable);

            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(1);

            SeccionDAO dao = new SeccionDAO();
            dao.actualizar(idSeccion, dto);

            verify(ps).setString(1, "Misión");
            verify(ps).setNull(2, Types.CLOB);
            verify(ps).setNull(3, Types.INTEGER);
            verify(ps).setLong(4, idSeccion);
        }
    }

    @Test
    @DisplayName("actualizar lanza SQLException cuando no se encuentra la sección")
    void actualizar_sinFilas_lanzaSQLException() throws Exception {
        long idSeccion = 7L;
        SeccionDTO.Upsert dto = new SeccionDTO.Upsert();
        dto.nombreSeccion = "Visión";
        dto.descripcion = "Nueva desc";
        dto.orden = 2;

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        String seccionTable = "SECCION_INFORMATIVA";
        String sql = "UPDATE " + seccionTable +
                " SET NOMBRE_SECCION = ?, DESCRIPCION = ?, ORDEN = NVL(?, ORDEN) WHERE ID_SECCION = ?";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("SECCION_INFORMATIVA")).thenReturn(seccionTable);

            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(0);

            SeccionDAO dao = new SeccionDAO();

            SQLException ex = assertThrows(
                    SQLException.class,
                    () -> dao.actualizar(idSeccion, dto)
            );
            assertEquals("Sección no encontrada", ex.getMessage());
        }
    }

    @Test
    @DisplayName("eliminar borra la sección por ID_SECCION")
    void eliminar_ok() throws Exception {
        long idSeccion = 9L;

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        String seccionTable = "SECCION_INFORMATIVA";
        String sql = "DELETE FROM " + seccionTable + " WHERE ID_SECCION = ?";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("SECCION_INFORMATIVA")).thenReturn(seccionTable);

            when(cn.prepareStatement(sql)).thenReturn(ps);

            SeccionDAO dao = new SeccionDAO();
            dao.eliminar(idSeccion);

            verify(ps).setLong(1, idSeccion);
            verify(ps).executeUpdate();
        }
    }

    @Test
    @DisplayName("listarPorPagina mapea secciones y llama a MediaDAO.listarPorSeccion para cada una")
    void listarPorPagina_mapeaYUsaMediaDAO() throws Exception {
        long idPagina = 3L;

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        String seccionTable = "SECCION_INFORMATIVA";
        String sql = "SELECT ID_SECCION, ID_PAGINA, NOMBRE_SECCION, " +
                "DBMS_LOB.SUBSTR(DESCRIPCION, 4000, 1) AS DESCRIPCION, ORDEN FROM " +
                seccionTable +
                " WHERE ID_PAGINA = ? ORDER BY ORDEN ASC, ID_SECCION ASC";

        try (MockedStatic<DB> db = mockStatic(DB.class);
             MockedConstruction<MediaDAO> mediaMock = mockConstruction(
                     MediaDAO.class,
                     (mock, context) -> when(mock.listarPorSeccion(anyLong()))
                             .thenReturn(List.of())
             )) {

            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("SECCION_INFORMATIVA")).thenReturn(seccionTable);

            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true, true, false);
            when(rs.getLong("ID_SECCION")).thenReturn(1L, 2L);
            when(rs.getLong("ID_PAGINA")).thenReturn(idPagina, idPagina);
            when(rs.getString("NOMBRE_SECCION")).thenReturn("Hist", "Mis");
            when(rs.getString("DESCRIPCION")).thenReturn("desc1", "desc2");
            when(rs.getInt("ORDEN")).thenReturn(1, 2);

            SeccionDAO dao = new SeccionDAO();
            List<SeccionDTO> lista = dao.listarPorPagina(idPagina);

            assertEquals(2, lista.size());
            SeccionDTO s1 = lista.get(0);
            assertEquals(1L, s1.idSeccion);
            assertEquals("Hist", s1.nombreSeccion);
            assertEquals(1, s1.orden);

            SeccionDTO s2 = lista.get(1);
            assertEquals(2L, s2.idSeccion);
            assertEquals("Mis", s2.nombreSeccion);
            assertEquals(2, s2.orden);

            assertEquals(2, mediaMock.constructed().size());
            verify(mediaMock.constructed().get(0)).listarPorSeccion(1L);
            verify(mediaMock.constructed().get(1)).listarPorSeccion(2L);
        }
    }

    @Test
    @DisplayName("reordenar aplica shift negativo y luego batch con nuevos órdenes")
    void reordenar_realizaShiftYBatch() throws Exception {
        long idPagina = 4L;

        SeccionDTO.Reordenar r1 = new SeccionDTO.Reordenar();
        r1.idSeccion = 10L;
        r1.orden = 1;

        SeccionDTO.Reordenar r2 = new SeccionDTO.Reordenar();
        r2.idSeccion = 20L;
        r2.orden = 2;

        Connection cn = mock(Connection.class);
        PreparedStatement psShift = mock(PreparedStatement.class);
        PreparedStatement psUp = mock(PreparedStatement.class);

        String seccionTable = "SECCION_INFORMATIVA";
        String shiftSql = "UPDATE " + seccionTable +
                " SET ORDEN = -ORDEN WHERE ID_PAGINA = ? AND ORDEN IS NOT NULL";
        String upSql = "UPDATE " + seccionTable +
                " SET ORDEN = ? WHERE ID_SECCION = ? AND ID_PAGINA = ?";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("SECCION_INFORMATIVA")).thenReturn(seccionTable);

            when(cn.prepareStatement(shiftSql)).thenReturn(psShift);
            when(cn.prepareStatement(upSql)).thenReturn(psUp);

            SeccionDAO dao = new SeccionDAO();
            dao.reordenar(idPagina, List.of(r1, r2));

            verify(cn).setAutoCommit(false);

            verify(psShift).setLong(1, idPagina);
            verify(psShift).executeUpdate();

            verify(psUp, times(2)).addBatch();
            verify(psUp).executeBatch();

            verify(psUp).setInt(1, 1);
            verify(psUp).setLong(2, 10L);

            verify(psUp).setInt(1, 2);
            verify(psUp).setLong(2, 20L);

            verify(psUp, times(2)).setLong(3, idPagina);

            verify(cn).commit();
        }
    }

}
