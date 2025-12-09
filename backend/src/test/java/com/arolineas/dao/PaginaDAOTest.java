package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.dto.PaginaDTO;
import com.aerolineas.dto.SeccionDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.sql.*;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaginaDAOTest {

    private static final String TABLA = "PAGINA_INFORMATIVA";

    @Test
    @DisplayName("crear inserta página y devuelve ID_PAGINA generado")
    void crear_ok_devuelveId() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rsKeys = mock(ResultSet.class);

        PaginaDTO.Upsert dto = new PaginaDTO.Upsert();
        dto.nombrePagina = "home";
        dto.titulo = "Home";
        dto.descripcion = "desc";

        String sql = "INSERT INTO " + TABLA + " (NOMBRE_PAGINA, TITULO, DESCRIPCION) VALUES (?,?,?)";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("PAGINA_INFORMATIVA")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sql, new String[]{"ID_PAGINA"})).thenReturn(ps);
            when(ps.getGeneratedKeys()).thenReturn(rsKeys);
            when(rsKeys.next()).thenReturn(true);
            when(rsKeys.getLong(1)).thenReturn(10L);

            PaginaDAO dao = new PaginaDAO();
            long id = dao.crear(dto);

            assertEquals(10L, id);
            verify(ps).setString(1, "home");
            verify(ps).setString(2, "Home");
            verify(ps).setString(3, "desc");
            verify(ps).executeUpdate();
        }
    }

    @Test
    @DisplayName("crear lanza SQLException cuando no se genera ID_PAGINA")
    void crear_sinGeneratedKey_lanzaSQLException() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rsKeys = mock(ResultSet.class);

        PaginaDTO.Upsert dto = new PaginaDTO.Upsert();
        dto.nombrePagina = "home";
        dto.titulo = "Home";
        dto.descripcion = "desc";

        String sql = "INSERT INTO " + TABLA + " (NOMBRE_PAGINA, TITULO, DESCRIPCION) VALUES (?,?,?)";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("PAGINA_INFORMATIVA")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sql, new String[]{"ID_PAGINA"})).thenReturn(ps);
            when(ps.getGeneratedKeys()).thenReturn(rsKeys);
            when(rsKeys.next()).thenReturn(false); 

            PaginaDAO dao = new PaginaDAO();
            SQLException ex = assertThrows(SQLException.class, () -> dao.crear(dto));
            assertEquals("No se generó ID_PAGINA", ex.getMessage());
        }
    }

    @Test
    @DisplayName("actualizar modifica la página cuando existe")
    void actualizar_ok() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        PaginaDTO.Upsert dto = new PaginaDTO.Upsert();
        dto.nombrePagina = "home2";
        dto.titulo = "Home 2";
        dto.descripcion = "desc2";

        String sql = "UPDATE " + TABLA +
                " SET NOMBRE_PAGINA=?, TITULO=?, DESCRIPCION=? WHERE ID_PAGINA=?";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("PAGINA_INFORMATIVA")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(1); 

            PaginaDAO dao = new PaginaDAO();
            dao.actualizar(10L, dto);

            verify(ps).setString(1, "home2");
            verify(ps).setString(2, "Home 2");
            verify(ps).setString(3, "desc2");
            verify(ps).setLong(4, 10L);
        }
    }

    @Test
    @DisplayName("actualizar lanza SQLException cuando no encuentra la página")
    void actualizar_paginaNoEncontrada() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        PaginaDTO.Upsert dto = new PaginaDTO.Upsert();
        dto.nombrePagina = "home";
        dto.titulo = "Home";
        dto.descripcion = "desc";

        String sql = "UPDATE " + TABLA +
                " SET NOMBRE_PAGINA=?, TITULO=?, DESCRIPCION=? WHERE ID_PAGINA=?";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("PAGINA_INFORMATIVA")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(0); 

            PaginaDAO dao = new PaginaDAO();
            SQLException ex = assertThrows(SQLException.class, () -> dao.actualizar(99L, dto));
            assertEquals("Página no encontrada", ex.getMessage());
        }
    }

    @Test
    @DisplayName("eliminar borra la página por ID_PAGINA")
    void eliminar_ok() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        String sql = "DELETE FROM " + TABLA + " WHERE ID_PAGINA=?";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("PAGINA_INFORMATIVA")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sql)).thenReturn(ps);

            PaginaDAO dao = new PaginaDAO();
            dao.eliminar(5L);

            verify(ps).setLong(1, 5L);
            verify(ps).executeUpdate();
        }
    }

    @Test
    @DisplayName("listar devuelve lista de páginas mapeada correctamente")
    void listar_ok() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        String sql = "SELECT ID_PAGINA, NOMBRE_PAGINA, TITULO, " +
                "DBMS_LOB.SUBSTR(DESCRIPCION, 4000, 1) AS DESCRIPCION FROM " +
                TABLA + " ORDER BY ID_PAGINA";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("PAGINA_INFORMATIVA")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true, true, false);
            when(rs.getLong("ID_PAGINA")).thenReturn(1L, 2L);
            when(rs.getString("NOMBRE_PAGINA")).thenReturn("home", "about");
            when(rs.getString("TITULO")).thenReturn("Home", "About");
            when(rs.getString("DESCRIPCION")).thenReturn("d1", "d2");

            PaginaDAO dao = new PaginaDAO();
            List<PaginaDTO> paginas = dao.listar();

            assertEquals(2, paginas.size());
            assertEquals("home", paginas.get(0).nombrePagina);
            assertEquals("about", paginas.get(1).nombrePagina);
        }
    }    

    @Test
    @DisplayName("obtenerConContenido devuelve página con secciones cuando existe")
    void obtenerConContenido_encontrada() throws Exception {
        Connection cnPagina = mock(Connection.class);
        PreparedStatement psPag = mock(PreparedStatement.class);
        ResultSet rsPag = mock(ResultSet.class);

        String sqlPag = "SELECT ID_PAGINA, NOMBRE_PAGINA, TITULO, " +
                "DBMS_LOB.SUBSTR(DESCRIPCION, 4000, 1) AS DESCRIPCION FROM " +
                TABLA + " WHERE ID_PAGINA=?";
                   
        SeccionDTO sec = new SeccionDTO();
        sec.idSeccion = 1L;
        List<SeccionDTO> secciones = List.of(sec);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<SeccionDAO> secCtor = mockConstruction(
                     SeccionDAO.class,
                     (mock, ctx) -> when(mock.listarPorPagina(anyLong()))
                             .thenReturn(secciones)
             )) {

            dbMock.when(() -> DB.table("PAGINA_INFORMATIVA")).thenReturn(TABLA);
            
            dbMock.when(DB::getConnection).thenReturn(cnPagina);

            when(cnPagina.prepareStatement(sqlPag)).thenReturn(psPag);
            when(psPag.executeQuery()).thenReturn(rsPag);
            when(rsPag.next()).thenReturn(true);
            when(rsPag.getLong(1)).thenReturn(10L);
            when(rsPag.getString(2)).thenReturn("home");
            when(rsPag.getString(3)).thenReturn("Home");
            when(rsPag.getString(4)).thenReturn("desc");

            PaginaDAO dao = new PaginaDAO();
            PaginaDTO p = dao.obtenerConContenido(10L);

            assertNotNull(p);
            assertEquals(10L, p.idPagina);
            assertEquals("home", p.nombrePagina);
            assertNotNull(p.secciones);
            assertEquals(1, p.secciones.size());
        }
    }

    @Test
    @DisplayName("obtenerConContenido devuelve null cuando página no existe")
    void obtenerConContenido_noEncontrada() throws Exception {
        Connection cnPagina = mock(Connection.class);
        PreparedStatement psPag = mock(PreparedStatement.class);
        ResultSet rsPag = mock(ResultSet.class);

        String sqlPag = "SELECT ID_PAGINA, NOMBRE_PAGINA, TITULO, " +
                "DBMS_LOB.SUBSTR(DESCRIPCION, 4000, 1) AS DESCRIPCION FROM " +
                TABLA + " WHERE ID_PAGINA=?";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<SeccionDAO> ignored = mockConstruction(
                     SeccionDAO.class,
                     (mock, ctx) -> when(mock.listarPorPagina(anyLong()))
                             .thenReturn(Collections.emptyList())
             )) {

            dbMock.when(() -> DB.table("PAGINA_INFORMATIVA")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cnPagina);

            when(cnPagina.prepareStatement(sqlPag)).thenReturn(psPag);
            when(psPag.executeQuery()).thenReturn(rsPag);
            when(rsPag.next()).thenReturn(false); 

            PaginaDAO dao = new PaginaDAO();
            PaginaDTO p = dao.obtenerConContenido(10L);

            assertNull(p);
        }
    }

    @Test
    @DisplayName("obtenerPorNombreConContenido devuelve página con secciones cuando existe")
    void obtenerPorNombreConContenido_encontrada() throws Exception {
        Connection cnPagina = mock(Connection.class);
        PreparedStatement psPag = mock(PreparedStatement.class);
        ResultSet rsPag = mock(ResultSet.class);

        String sqlPag = "SELECT ID_PAGINA, NOMBRE_PAGINA, TITULO, " +
                "DBMS_LOB.SUBSTR(DESCRIPCION, 4000, 1) AS DESCRIPCION FROM " +
                TABLA + " WHERE NOMBRE_PAGINA=?";

        SeccionDTO sec = new SeccionDTO();
        sec.idSeccion = 1L;
        List<SeccionDTO> secciones = List.of(sec);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<SeccionDAO> secCtor = mockConstruction(
                     SeccionDAO.class,
                     (mock, ctx) -> when(mock.listarPorPagina(anyLong()))
                             .thenReturn(secciones)
             )) {

            dbMock.when(() -> DB.table("PAGINA_INFORMATIVA")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cnPagina);

            when(cnPagina.prepareStatement(sqlPag)).thenReturn(psPag);
            when(psPag.executeQuery()).thenReturn(rsPag);
            when(rsPag.next()).thenReturn(true);
            when(rsPag.getLong(1)).thenReturn(20L);
            when(rsPag.getString(2)).thenReturn("nosotros");
            when(rsPag.getString(3)).thenReturn("Nosotros");
            when(rsPag.getString(4)).thenReturn("desc");

            PaginaDAO dao = new PaginaDAO();
            PaginaDTO p = dao.obtenerPorNombreConContenido("nosotros");

            assertNotNull(p);
            assertEquals(20L, p.idPagina);
            assertEquals("nosotros", p.nombrePagina);
            assertEquals(1, p.secciones.size());
        }
    }

    @Test
    @DisplayName("obtenerPorNombreConContenido devuelve null cuando no existe")
    void obtenerPorNombreConContenido_noEncontrada() throws Exception {
        Connection cnPagina = mock(Connection.class);
        PreparedStatement psPag = mock(PreparedStatement.class);
        ResultSet rsPag = mock(ResultSet.class);

        String sqlPag = "SELECT ID_PAGINA, NOMBRE_PAGINA, TITULO, " +
                "DBMS_LOB.SUBSTR(DESCRIPCION, 4000, 1) AS DESCRIPCION FROM " +
                TABLA + " WHERE NOMBRE_PAGINA=?";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<SeccionDAO> ignored = mockConstruction(
                     SeccionDAO.class,
                     (mock, ctx) -> when(mock.listarPorPagina(anyLong()))
                             .thenReturn(Collections.emptyList())
             )) {

            dbMock.when(() -> DB.table("PAGINA_INFORMATIVA")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cnPagina);

            when(cnPagina.prepareStatement(sqlPag)).thenReturn(psPag);
            when(psPag.executeQuery()).thenReturn(rsPag);
            when(rsPag.next()).thenReturn(false);

            PaginaDAO dao = new PaginaDAO();
            PaginaDTO p = dao.obtenerPorNombreConContenido("x");

            assertNull(p);
        }
    }
}
