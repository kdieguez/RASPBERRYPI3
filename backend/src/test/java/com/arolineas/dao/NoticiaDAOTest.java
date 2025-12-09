package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.dto.NoticiaDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NoticiaDAOTest {

    private static final String TABLA = "NOTICIAS";

    @Test
    @DisplayName("listar mapea correctamente noticias con fecha y orden nulos o no nulos")
    void listar_mapeaCorrectamente() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        String sql =
                "SELECT ID_NOTICIA, TITULO, DBMS_LOB.SUBSTR(CONTENIDO, 4000, 1) AS CONTENIDO, " +
                "FECHA_PUBLICACION, ORDEN, URL_IMAGEN FROM " + TABLA +
                " ORDER BY ORDEN ASC, FECHA_PUBLICACION DESC, ID_NOTICIA DESC";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("NOTICIAS")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true, true, false);

            when(rs.getLong("ID_NOTICIA")).thenReturn(1L, 2L);
            when(rs.getString("TITULO")).thenReturn("N1", "N2");
            when(rs.getString("CONTENIDO")).thenReturn("C1", "C2");

            Timestamp ts1 = Timestamp.valueOf(LocalDateTime.of(2025, 1, 1, 10, 0));
            when(rs.getTimestamp("FECHA_PUBLICACION"))
                    .thenReturn(ts1, null); 

            when(rs.getInt("ORDEN")).thenReturn(1, 0);
            
            when(rs.wasNull()).thenReturn(false, true);

            when(rs.getString("URL_IMAGEN")).thenReturn("u1", "u2");

            NoticiaDAO dao = new NoticiaDAO();
            List<NoticiaDTO> out = dao.listar();

            assertEquals(2, out.size());
            assertEquals(1L, out.get(0).idNoticia);
            assertEquals(2L, out.get(1).idNoticia);
            assertNotNull(out.get(0).fechaPublicacion);
            assertNull(out.get(1).fechaPublicacion);
            assertEquals(Integer.valueOf(1), out.get(0).orden);
            assertNull(out.get(1).orden);
        }
    }

    @Test
    @DisplayName("obtenerPorId devuelve noticia cuando existe")
    void obtenerPorId_encontrada() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        String sql =
                "SELECT ID_NOTICIA, TITULO, DBMS_LOB.SUBSTR(CONTENIDO, 4000, 1) AS CONTENIDO, " +
                "FECHA_PUBLICACION, ORDEN, URL_IMAGEN FROM " + TABLA + " WHERE ID_NOTICIA = ?";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("NOTICIAS")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true);
            when(rs.getLong("ID_NOTICIA")).thenReturn(10L);
            when(rs.getString("TITULO")).thenReturn("Titulo");
            when(rs.getString("CONTENIDO")).thenReturn("Contenido");
            when(rs.getTimestamp("FECHA_PUBLICACION"))
                    .thenReturn(Timestamp.valueOf(LocalDateTime.of(2025, 1, 1, 0, 0)));
            when(rs.getInt("ORDEN")).thenReturn(3);
            when(rs.wasNull()).thenReturn(false);
            when(rs.getString("URL_IMAGEN")).thenReturn("img.png");

            NoticiaDAO dao = new NoticiaDAO();
            NoticiaDTO n = dao.obtenerPorId(10L);

            assertNotNull(n);
            assertEquals(10L, n.idNoticia);
            verify(ps).setLong(1, 10L);
        }
    }

    @Test
    @DisplayName("obtenerPorId devuelve null cuando no existe")
    void obtenerPorId_noEncontrada() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        String sql =
                "SELECT ID_NOTICIA, TITULO, DBMS_LOB.SUBSTR(CONTENIDO, 4000, 1) AS CONTENIDO, " +
                "FECHA_PUBLICACION, ORDEN, URL_IMAGEN FROM " + TABLA + " WHERE ID_NOTICIA = ?";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("NOTICIAS")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            NoticiaDAO dao = new NoticiaDAO();
            NoticiaDTO n = dao.obtenerPorId(99L);

            assertNull(n);
        }
    }    

    @Test
    @DisplayName("crear: dto.orden null => usa max+1, no reordena, fecha y url nulas")
    void crear_ordenNull_sinReorden_fechaYUrlNulas() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psMax = mock(PreparedStatement.class);
        PreparedStatement psShift = mock(PreparedStatement.class);
        PreparedStatement psIns = mock(PreparedStatement.class);
        ResultSet rsMax = mock(ResultSet.class);
        ResultSet rsKeys = mock(ResultSet.class);

        NoticiaDTO.Upsert dto = new NoticiaDTO.Upsert();
        dto.titulo = "T";
        dto.contenido = "C";
        dto.orden = null;
        dto.fechaPublicacion = null;
        dto.urlImagen = " "; 

        String qMax = "SELECT NVL(MAX(ORDEN), 0) AS MAXO FROM " + TABLA;
        String qShift = "UPDATE " + TABLA + " SET ORDEN = ORDEN + 1 WHERE ORDEN >= ?";
        String qIns = "INSERT INTO " + TABLA +
                " (TITULO, CONTENIDO, FECHA_PUBLICACION, ORDEN, URL_IMAGEN) " +
                "VALUES (?, ?, NVL(?, SYSDATE), ?, ?)";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("NOTICIAS")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(qMax)).thenReturn(psMax);
            when(psMax.executeQuery()).thenReturn(rsMax);
            when(rsMax.next()).thenReturn(true);
            when(rsMax.getInt("MAXO")).thenReturn(3); 

            when(cn.prepareStatement(qShift)).thenReturn(psShift); 

            when(cn.prepareStatement(qIns, new String[]{"ID_NOTICIA"})).thenReturn(psIns);
            when(psIns.getGeneratedKeys()).thenReturn(rsKeys);
            when(rsKeys.next()).thenReturn(true);
            when(rsKeys.getLong(1)).thenReturn(50L);

            NoticiaDAO dao = new NoticiaDAO();
            long id = dao.crear(dto);

            assertEquals(50L, id);
            verify(psIns).setString(1, "T");
            verify(psIns).setString(2, "C");
            
            verify(psIns).setNull(3, Types.TIMESTAMP);
            
            verify(psIns).setInt(4, 4);
            
            verify(psIns).setNull(5, Types.VARCHAR);
            verify(cn).commit();
        }
    }

    @Test
    @DisplayName("crear: dto.orden dentro de rango => usa dto.orden y reordena")
    void crear_ordenDentroRango_reordena() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psMax = mock(PreparedStatement.class);
        PreparedStatement psShift = mock(PreparedStatement.class);
        PreparedStatement psIns = mock(PreparedStatement.class);
        ResultSet rsMax = mock(ResultSet.class);
        ResultSet rsKeys = mock(ResultSet.class);

        NoticiaDTO.Upsert dto = new NoticiaDTO.Upsert();
        dto.titulo = "T";
        dto.contenido = "C";
        dto.orden = 2; 
        dto.fechaPublicacion = LocalDateTime.of(2025, 1, 2, 12, 0);
        dto.urlImagen = "img.png";

        String qMax = "SELECT NVL(MAX(ORDEN), 0) AS MAXO FROM " + TABLA;
        String qShift = "UPDATE " + TABLA + " SET ORDEN = ORDEN + 1 WHERE ORDEN >= ?";
        String qIns = "INSERT INTO " + TABLA +
                " (TITULO, CONTENIDO, FECHA_PUBLICACION, ORDEN, URL_IMAGEN) " +
                "VALUES (?, ?, NVL(?, SYSDATE), ?, ?)";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("NOTICIAS")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(qMax)).thenReturn(psMax);
            when(psMax.executeQuery()).thenReturn(rsMax);
            when(rsMax.next()).thenReturn(true);
            when(rsMax.getInt("MAXO")).thenReturn(5); 

            when(cn.prepareStatement(qShift)).thenReturn(psShift);

            when(cn.prepareStatement(qIns, new String[]{"ID_NOTICIA"})).thenReturn(psIns);
            when(psIns.getGeneratedKeys()).thenReturn(rsKeys);
            when(rsKeys.next()).thenReturn(true);
            when(rsKeys.getLong(1)).thenReturn(60L);

            NoticiaDAO dao = new NoticiaDAO();
            long id = dao.crear(dto);

            assertEquals(60L, id);
            
            verify(psShift).setInt(1, 2);
            verify(psShift).executeUpdate();

            verify(psIns).setTimestamp(eq(3), any(Timestamp.class));
            verify(psIns).setInt(4, 2);
            verify(psIns).setString(5, "img.png");
        }
    }

    @Test
    @DisplayName("crear: dto.orden muy grande => usa max+1 (else-if)")
    void crear_ordenMuyGranAjusta() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psMax = mock(PreparedStatement.class);
        PreparedStatement psIns = mock(PreparedStatement.class);
        ResultSet rsMax = mock(ResultSet.class);
        ResultSet rsKeys = mock(ResultSet.class);

        NoticiaDTO.Upsert dto = new NoticiaDTO.Upsert();
        dto.titulo = "T";
        dto.contenido = "C";
        dto.orden = 100; 
        dto.fechaPublicacion = LocalDateTime.now();
        dto.urlImagen = "img";

        String qMax = "SELECT NVL(MAX(ORDEN), 0) AS MAXO FROM " + TABLA;
        String qIns = "INSERT INTO " + TABLA +
                " (TITULO, CONTENIDO, FECHA_PUBLICACION, ORDEN, URL_IMAGEN) " +
                "VALUES (?, ?, NVL(?, SYSDATE), ?, ?)";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("NOTICIAS")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(qMax)).thenReturn(psMax);
            when(psMax.executeQuery()).thenReturn(rsMax);
            when(rsMax.next()).thenReturn(true);
            when(rsMax.getInt("MAXO")).thenReturn(3); 

            when(cn.prepareStatement(qIns, new String[]{"ID_NOTICIA"})).thenReturn(psIns);
            when(psIns.getGeneratedKeys()).thenReturn(rsKeys);
            when(rsKeys.next()).thenReturn(true);
            when(rsKeys.getLong(1)).thenReturn(70L);

            NoticiaDAO dao = new NoticiaDAO();
            long id = dao.crear(dto);

            assertEquals(70L, id);
            verify(psIns).setInt(4, 4);
        }
    }

    @Test
    @DisplayName("crear: sin generated key lanza SQLException y hace rollback (catch con instanceof true)")
    void crear_sinGeneratedKey_lanzaSQLException() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psMax = mock(PreparedStatement.class);
        PreparedStatement psIns = mock(PreparedStatement.class);
        ResultSet rsMax = mock(ResultSet.class);
        ResultSet rsKeys = mock(ResultSet.class);

        NoticiaDTO.Upsert dto = new NoticiaDTO.Upsert();
        dto.titulo = "T";
        dto.contenido = "C";

        String qMax = "SELECT NVL(MAX(ORDEN), 0) AS MAXO FROM " + TABLA;
        String qIns = "INSERT INTO " + TABLA +
                " (TITULO, CONTENIDO, FECHA_PUBLICACION, ORDEN, URL_IMAGEN) " +
                "VALUES (?, ?, NVL(?, SYSDATE), ?, ?)";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("NOTICIAS")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(qMax)).thenReturn(psMax);
            when(psMax.executeQuery()).thenReturn(rsMax);
            when(rsMax.next()).thenReturn(true);
            when(rsMax.getInt("MAXO")).thenReturn(0);

            when(cn.prepareStatement(qIns, new String[]{"ID_NOTICIA"})).thenReturn(psIns);
            when(psIns.getGeneratedKeys()).thenReturn(rsKeys);
            when(rsKeys.next()).thenReturn(false); 

            NoticiaDAO dao = new NoticiaDAO();

            SQLException ex = assertThrows(SQLException.class, () -> dao.crear(dto));
            assertEquals("No se generó ID_NOTICIA", ex.getMessage());
            verify(cn).rollback();
        }
    }

    @Test
    @DisplayName("crear: RuntimeException interno se envuelve en SQLException 'Error al crear noticia'")
    void crear_envuelveRuntimeException() throws Exception {
        Connection cn = mock(Connection.class);

        NoticiaDTO.Upsert dto = new NoticiaDTO.Upsert();
        dto.titulo = "T";
        dto.contenido = "C";

        String qMax = "SELECT NVL(MAX(ORDEN), 0) AS MAXO FROM " + TABLA;

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("NOTICIAS")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(qMax))
                    .thenThrow(new RuntimeException("boom"));

            NoticiaDAO dao = new NoticiaDAO();

            SQLException ex = assertThrows(SQLException.class, () -> dao.crear(dto));
            assertEquals("Error al crear noticia", ex.getMessage());
            assertTrue(ex.getCause() instanceof RuntimeException);
            verify(cn).rollback();
        }
    }

    @Test
    @DisplayName("actualizar: dto.orden null => mantiene ordenActual sin reordenar")
    void actualizar_sinCambioDeOrden() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psSelOrden = mock(PreparedStatement.class);
        PreparedStatement psMax = mock(PreparedStatement.class);
        PreparedStatement psUpdateFinal = mock(PreparedStatement.class);
        ResultSet rsSelOrden = mock(ResultSet.class);
        ResultSet rsMax = mock(ResultSet.class);

        NoticiaDTO.Upsert dto = new NoticiaDTO.Upsert();
        dto.titulo = "Nuevo";
        dto.contenido = "Cont";
        dto.orden = null;
        dto.fechaPublicacion = null;
        dto.urlImagen = " ";

        String qSelOrden =
                "SELECT ORDEN FROM " + TABLA + " WHERE ID_NOTICIA = ? FOR UPDATE";
        String qMax = "SELECT NVL(MAX(ORDEN), 0) AS MAXO FROM " + TABLA;
        String qUpdateFinal =
                "UPDATE " + TABLA +
                        " SET TITULO = ?, CONTENIDO = ?, FECHA_PUBLICACION = NVL(?, FECHA_PUBLICACION), " +
                        "ORDEN = ?, URL_IMAGEN = ? WHERE ID_NOTICIA = ?";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("NOTICIAS")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(qSelOrden)).thenReturn(psSelOrden);
            when(psSelOrden.executeQuery()).thenReturn(rsSelOrden);
            when(rsSelOrden.next()).thenReturn(true);
            when(rsSelOrden.getInt("ORDEN")).thenReturn(3); 

            when(cn.prepareStatement(qMax)).thenReturn(psMax);
            when(psMax.executeQuery()).thenReturn(rsMax);
            when(rsMax.next()).thenReturn(true);
            when(rsMax.getInt("MAXO")).thenReturn(5); 

            when(cn.prepareStatement(qUpdateFinal)).thenReturn(psUpdateFinal);
            when(psUpdateFinal.executeUpdate()).thenReturn(1);

            NoticiaDAO dao = new NoticiaDAO();
            dao.actualizar(10L, dto);

            
            verify(psUpdateFinal).setInt(4, 3);
            verify(cn).commit();
        }
    }

    @Test
    @DisplayName("actualizar: nuevoOrden < ordenActual => reordena hacia arriba (ORDEN + 1)")
    void actualizar_cambiaOrdenHaciaArriba() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psSelOrden = mock(PreparedStatement.class);
        PreparedStatement psMax = mock(PreparedStatement.class);
        PreparedStatement psReorderUp = mock(PreparedStatement.class);
        PreparedStatement psUpdateFinal = mock(PreparedStatement.class);
        ResultSet rsSelOrden = mock(ResultSet.class);
        ResultSet rsMax = mock(ResultSet.class);

        NoticiaDTO.Upsert dto = new NoticiaDTO.Upsert();
        dto.titulo = "Nuevo";
        dto.contenido = "Cont";
        dto.orden = 1; 
        dto.fechaPublicacion = LocalDateTime.now();
        dto.urlImagen = "img.png";

        String qSelOrden =
                "SELECT ORDEN FROM " + TABLA + " WHERE ID_NOTICIA = ? FOR UPDATE";
        String qMax = "SELECT NVL(MAX(ORDEN), 0) AS MAXO FROM " + TABLA;
        String qReorderUp =
                "UPDATE " + TABLA +
                        " SET ORDEN = ORDEN + 1 WHERE ORDEN >= ? AND ORDEN < ? AND ID_NOTICIA <> ?";
        String qUpdateFinal =
                "UPDATE " + TABLA +
                        " SET TITULO = ?, CONTENIDO = ?, FECHA_PUBLICACION = NVL(?, FECHA_PUBLICACION), " +
                        "ORDEN = ?, URL_IMAGEN = ? WHERE ID_NOTICIA = ?";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("NOTICIAS")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(qSelOrden)).thenReturn(psSelOrden);
            when(psSelOrden.executeQuery()).thenReturn(rsSelOrden);
            when(rsSelOrden.next()).thenReturn(true);
            when(rsSelOrden.getInt("ORDEN")).thenReturn(3); 

            when(cn.prepareStatement(qMax)).thenReturn(psMax);
            when(psMax.executeQuery()).thenReturn(rsMax);
            when(rsMax.next()).thenReturn(true);
            when(rsMax.getInt("MAXO")).thenReturn(5);

            when(cn.prepareStatement(qReorderUp)).thenReturn(psReorderUp);
            when(cn.prepareStatement(qUpdateFinal)).thenReturn(psUpdateFinal);
            when(psUpdateFinal.executeUpdate()).thenReturn(1);

            NoticiaDAO dao = new NoticiaDAO();
            dao.actualizar(10L, dto);

            verify(psReorderUp).setInt(1, 1);
            verify(psReorderUp).setInt(2, 3);
            verify(psReorderUp).setLong(3, 10L);
            
            verify(psUpdateFinal).setInt(4, 1);
            verify(psUpdateFinal).setString(5, "img.png");
        }
    }

    @Test
    @DisplayName("actualizar: nuevoOrden > ordenActual => reordena hacia abajo (ORDEN - 1)")
    void actualizar_cambiaOrdenHaciaAbajo() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psSelOrden = mock(PreparedStatement.class);
        PreparedStatement psMax = mock(PreparedStatement.class);
        PreparedStatement psReorderDown = mock(PreparedStatement.class);
        PreparedStatement psUpdateFinal = mock(PreparedStatement.class);
        ResultSet rsSelOrden = mock(ResultSet.class);
        ResultSet rsMax = mock(ResultSet.class);

        NoticiaDTO.Upsert dto = new NoticiaDTO.Upsert();
        dto.titulo = "Nuevo";
        dto.contenido = "Cont";
        dto.orden = 10; 
        dto.fechaPublicacion = LocalDateTime.now();
        dto.urlImagen = "img.png";

        String qSelOrden =
                "SELECT ORDEN FROM " + TABLA + " WHERE ID_NOTICIA = ? FOR UPDATE";
        String qMax = "SELECT NVL(MAX(ORDEN), 0) AS MAXO FROM " + TABLA;
        String qReorderDown =
                "UPDATE " + TABLA +
                        " SET ORDEN = ORDEN - 1 WHERE ORDEN <= ? AND ORDEN > ? AND ID_NOTICIA <> ?";
        String qUpdateFinal =
                "UPDATE " + TABLA +
                        " SET TITULO = ?, CONTENIDO = ?, FECHA_PUBLICACION = NVL(?, FECHA_PUBLICACION), " +
                        "ORDEN = ?, URL_IMAGEN = ? WHERE ID_NOTICIA = ?";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("NOTICIAS")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(qSelOrden)).thenReturn(psSelOrden);
            when(psSelOrden.executeQuery()).thenReturn(rsSelOrden);
            when(rsSelOrden.next()).thenReturn(true);
            when(rsSelOrden.getInt("ORDEN")).thenReturn(2); 

            when(cn.prepareStatement(qMax)).thenReturn(psMax);
            when(psMax.executeQuery()).thenReturn(rsMax);
            when(rsMax.next()).thenReturn(true);
            when(rsMax.getInt("MAXO")).thenReturn(5); 

            when(cn.prepareStatement(qReorderDown)).thenReturn(psReorderDown);
            when(cn.prepareStatement(qUpdateFinal)).thenReturn(psUpdateFinal);
            when(psUpdateFinal.executeUpdate()).thenReturn(1);

            NoticiaDAO dao = new NoticiaDAO();
            dao.actualizar(10L, dto);
            
            verify(psReorderDown).setInt(1, 5);
            verify(psReorderDown).setInt(2, 2);
            verify(psReorderDown).setLong(3, 10L);

            verify(psUpdateFinal).setInt(4, 5);
        }
    }

    @Test
    @DisplayName("actualizar: executeUpdate==0 lanza SQLException Noticia no encontrada (instanceof true)")
    void actualizar_sinFilas_lanzaSQLException() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psSelOrden = mock(PreparedStatement.class);
        PreparedStatement psMax = mock(PreparedStatement.class);
        PreparedStatement psUpdateFinal = mock(PreparedStatement.class);
        ResultSet rsSelOrden = mock(ResultSet.class);
        ResultSet rsMax = mock(ResultSet.class);

        NoticiaDTO.Upsert dto = new NoticiaDTO.Upsert();
        dto.titulo = "Nuevo";
        dto.contenido = "Cont";
        dto.orden = null;

        String qSelOrden =
                "SELECT ORDEN FROM " + TABLA + " WHERE ID_NOTICIA = ? FOR UPDATE";
        String qMax = "SELECT NVL(MAX(ORDEN), 0) AS MAXO FROM " + TABLA;
        String qUpdateFinal =
                "UPDATE " + TABLA +
                        " SET TITULO = ?, CONTENIDO = ?, FECHA_PUBLICACION = NVL(?, FECHA_PUBLICACION), " +
                        "ORDEN = ?, URL_IMAGEN = ? WHERE ID_NOTICIA = ?";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("NOTICIAS")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(qSelOrden)).thenReturn(psSelOrden);
            when(psSelOrden.executeQuery()).thenReturn(rsSelOrden);
            when(rsSelOrden.next()).thenReturn(true);
            when(rsSelOrden.getInt("ORDEN")).thenReturn(1);

            when(cn.prepareStatement(qMax)).thenReturn(psMax);
            when(psMax.executeQuery()).thenReturn(rsMax);
            when(rsMax.next()).thenReturn(true);
            when(rsMax.getInt("MAXO")).thenReturn(5);

            when(cn.prepareStatement(qUpdateFinal)).thenReturn(psUpdateFinal);
            when(psUpdateFinal.executeUpdate()).thenReturn(0); 

            NoticiaDAO dao = new NoticiaDAO();
            SQLException ex = assertThrows(SQLException.class, () -> dao.actualizar(10L, dto));
            assertEquals("Noticia no encontrada", ex.getMessage());
            verify(cn).rollback();
        }
    }

    @Test
    @DisplayName("actualizar: RuntimeException se envuelve en SQLException 'Error al actualizar noticia'")
    void actualizar_envuelveRuntimeException() throws Exception {
        Connection cn = mock(Connection.class);

        NoticiaDTO.Upsert dto = new NoticiaDTO.Upsert();
        dto.titulo = "T";

        String qSelOrden =
                "SELECT ORDEN FROM " + TABLA + " WHERE ID_NOTICIA = ? FOR UPDATE";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("NOTICIAS")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(qSelOrden))
                    .thenThrow(new RuntimeException("boom"));

            NoticiaDAO dao = new NoticiaDAO();

            SQLException ex = assertThrows(SQLException.class, () -> dao.actualizar(1L, dto));
            assertEquals("Error al actualizar noticia", ex.getMessage());
            assertTrue(ex.getCause() instanceof RuntimeException);
            verify(cn).rollback();
        }
    }

    @Test
    @DisplayName("eliminar: con orden reacomoda ordenes siguientes")
    void eliminar_conOrden_reacomoda() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psSel = mock(PreparedStatement.class);
        PreparedStatement psDel = mock(PreparedStatement.class);
        PreparedStatement psUpd = mock(PreparedStatement.class);
        ResultSet rsSel = mock(ResultSet.class);

        String qSel = "SELECT ORDEN FROM " + TABLA + " WHERE ID_NOTICIA = ?";
        String qDel = "DELETE FROM " + TABLA + " WHERE ID_NOTICIA = ?";
        String qUpd = "UPDATE " + TABLA + " SET ORDEN = ORDEN - 1 WHERE ORDEN > ?";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("NOTICIAS")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(qSel)).thenReturn(psSel);
            when(psSel.executeQuery()).thenReturn(rsSel);
            when(rsSel.next()).thenReturn(true);
            when(rsSel.getInt("ORDEN")).thenReturn(4);
            when(rsSel.wasNull()).thenReturn(false);

            when(cn.prepareStatement(qDel)).thenReturn(psDel);
            when(cn.prepareStatement(qUpd)).thenReturn(psUpd);

            NoticiaDAO dao = new NoticiaDAO();
            dao.eliminar(10L);

            verify(psUpd).setInt(1, 4);
            verify(psUpd).executeUpdate();
            verify(cn).commit();
        }
    }

    @Test
    @DisplayName("eliminar: sin orden (no encontrada o null) no reacomoda")
    void eliminar_sinOrden_noReacomoda() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psSel = mock(PreparedStatement.class);
        PreparedStatement psDel = mock(PreparedStatement.class);
        ResultSet rsSel = mock(ResultSet.class);

        String qSel = "SELECT ORDEN FROM " + TABLA + " WHERE ID_NOTICIA = ?";
        String qDel = "DELETE FROM " + TABLA + " WHERE ID_NOTICIA = ?";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("NOTICIAS")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(qSel)).thenReturn(psSel);
            when(psSel.executeQuery()).thenReturn(rsSel);
            when(rsSel.next()).thenReturn(false); 

            when(cn.prepareStatement(qDel)).thenReturn(psDel);

            NoticiaDAO dao = new NoticiaDAO();
            dao.eliminar(10L);

            
            verify(cn, never()).prepareStatement(
                    "UPDATE " + TABLA + " SET ORDEN = ORDEN - 1 WHERE ORDEN > ?");
            verify(cn).commit();
        }
    }

    @Test
    @DisplayName("eliminar: RuntimeException se envuelve en SQLException 'Error al eliminar noticia'")
    void eliminar_envuelveRuntimeException() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psSel = mock(PreparedStatement.class);
        ResultSet rsSel = mock(ResultSet.class);

        String qSel = "SELECT ORDEN FROM " + TABLA + " WHERE ID_NOTICIA = ?";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("NOTICIAS")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(qSel)).thenReturn(psSel);
            when(psSel.executeQuery()).thenReturn(rsSel);
            when(rsSel.next()).thenReturn(true);
            when(rsSel.getInt("ORDEN")).thenReturn(1);

            
            String qDel = "DELETE FROM " + TABLA + " WHERE ID_NOTICIA = ?";
            when(cn.prepareStatement(qDel))
                    .thenThrow(new RuntimeException("boom"));

            NoticiaDAO dao = new NoticiaDAO();

            SQLException ex = assertThrows(SQLException.class, () -> dao.eliminar(10L));
            assertEquals("Error al eliminar noticia", ex.getMessage());
            assertTrue(ex.getCause() instanceof RuntimeException);
            verify(cn).rollback();
        }
    }
}
