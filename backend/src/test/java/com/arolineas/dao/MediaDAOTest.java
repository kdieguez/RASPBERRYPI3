package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.dto.MediaDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MediaDAOTest {

    @Test
    @DisplayName("crear inserta media con orden definido y devuelve el ID generado")
    void crear_conOrden_devuelveIdGenerado() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rsKeys = mock(ResultSet.class);

        long idSeccion = 10L;
        MediaDTO.Upsert dto = new MediaDTO.Upsert();
        dto.tipoMedia = "IMG";
        dto.url = "https://example.com/img.png";
        dto.orden = 5;

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("MEDIA_INFORMATIVA")).thenReturn("MEDIA_INFORMATIVA");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String expectedSql =
                    "INSERT INTO MEDIA_INFORMATIVA (ID_SECCION, TIPO_MEDIA, URL, ORDEN) " +
                    "VALUES (?,?,?, NVL(?,1))";

            when(cn.prepareStatement(expectedSql, new String[]{"ID_MEDIA"})).thenReturn(ps);
            when(ps.getGeneratedKeys()).thenReturn(rsKeys);
            when(rsKeys.next()).thenReturn(true);
            when(rsKeys.getLong(1)).thenReturn(123L);

            MediaDAO dao = new MediaDAO();
            long id = dao.crear(idSeccion, dto);

            assertEquals(123L, id);

            verify(ps).setLong(1, idSeccion);
            verify(ps).setString(2, "IMG");
            verify(ps).setString(3, "https://example.com/img.png");
            verify(ps).setInt(4, 5);
            verify(ps).executeUpdate();
        }
    }

    @Test
    @DisplayName("crear usa setNull cuando dto.orden es null")
    void crear_sinOrden_usaSetNull() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rsKeys = mock(ResultSet.class);

        long idSeccion = 20L;
        MediaDTO.Upsert dto = new MediaDTO.Upsert();
        dto.tipoMedia = "VIDEO";
        dto.url = "https://example.com/video.mp4";
        dto.orden = null; 

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("MEDIA_INFORMATIVA")).thenReturn("MEDIA_INFORMATIVA");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String expectedSql =
                    "INSERT INTO MEDIA_INFORMATIVA (ID_SECCION, TIPO_MEDIA, URL, ORDEN) " +
                    "VALUES (?,?,?, NVL(?,1))";

            when(cn.prepareStatement(expectedSql, new String[]{"ID_MEDIA"})).thenReturn(ps);
            when(ps.getGeneratedKeys()).thenReturn(rsKeys);
            when(rsKeys.next()).thenReturn(true);
            when(rsKeys.getLong(1)).thenReturn(200L);

            MediaDAO dao = new MediaDAO();
            long id = dao.crear(idSeccion, dto);

            assertEquals(200L, id);

            verify(ps).setLong(1, idSeccion);
            verify(ps).setString(2, "VIDEO");
            verify(ps).setString(3, "https://example.com/video.mp4");
            verify(ps).setNull(4, Types.INTEGER);  
        }
    }

    @Test
    @DisplayName("crear lanza SQLException cuando no se genera ID_MEDIA")
    void crear_sinGeneratedKey_lanzaSQLException() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        long idSeccion = 10L;
        MediaDTO.Upsert dto = new MediaDTO.Upsert();
        dto.tipoMedia = "IMG";
        dto.url = "https://ejemplo.com/img.png";
        dto.orden = 1;

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("MEDIA_INFORMATIVA")).thenReturn("MEDIA_INFORMATIVA");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String expectedSql =
                    "INSERT INTO MEDIA_INFORMATIVA (ID_SECCION, TIPO_MEDIA, URL, ORDEN) " +
                    "VALUES (?,?,?, NVL(?,1))";

            when(cn.prepareStatement(expectedSql, new String[]{"ID_MEDIA"})).thenReturn(ps);
            when(ps.getGeneratedKeys()).thenReturn(rs);
            when(rs.next()).thenReturn(false); 

            MediaDAO dao = new MediaDAO();

            SQLException ex = assertThrows(
                    SQLException.class,
                    () -> dao.crear(idSeccion, dto)
            );

            assertEquals("No se generó ID_MEDIA", ex.getMessage());
        }
    }

    @Test
    @DisplayName("eliminar borra el registro por ID_MEDIA")
    void eliminar_borraPorId() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        long idMedia = 99L;

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("MEDIA_INFORMATIVA")).thenReturn("MEDIA_INFORMATIVA");
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement("DELETE FROM MEDIA_INFORMATIVA WHERE ID_MEDIA=?"))
                    .thenReturn(ps);

            MediaDAO dao = new MediaDAO();
            dao.eliminar(idMedia);

            verify(ps).setLong(1, idMedia);
            verify(ps).executeUpdate();
        }
    }

    @Test
    @DisplayName("reordenar actualiza orden por batch y hace commit")
    void reordenar_haceBatchYCommit() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        long idSeccion = 5L;

        MediaDTO.Reordenar r1 = new MediaDTO.Reordenar();
        r1.idMedia = 1L;
        r1.orden = 10;

        MediaDTO.Reordenar r2 = new MediaDTO.Reordenar();
        r2.idMedia = 2L;
        r2.orden = 20;

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("MEDIA_INFORMATIVA")).thenReturn("MEDIA_INFORMATIVA");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String expectedSql =
                    "UPDATE MEDIA_INFORMATIVA SET ORDEN=? WHERE ID_MEDIA=? AND ID_SECCION=?";

            when(cn.prepareStatement(expectedSql)).thenReturn(ps);

            MediaDAO dao = new MediaDAO();
            dao.reordenar(idSeccion, List.of(r1, r2));

            verify(cn).setAutoCommit(false);
            
            verify(ps).setInt(1, 10);
            verify(ps).setLong(2, 1L);
            
            verify(ps).setInt(1, 20);
            verify(ps).setLong(2, 2L);

            verify(ps, times(2)).setLong(3, idSeccion);
            verify(ps, times(2)).addBatch();
            verify(ps).executeBatch();
            verify(cn).commit();
        }
    }

    @Test
    @DisplayName("listarPorSeccion devuelve la lista de media mapeada correctamente")
    void listarPorSeccion_mapeaCorrectamente() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        long idSeccion = 77L;

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("MEDIA_INFORMATIVA")).thenReturn("MEDIA_INFORMATIVA");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String expectedSql =
                    "SELECT ID_MEDIA, ID_SECCION, TIPO_MEDIA, URL, ORDEN " +
                    "FROM MEDIA_INFORMATIVA WHERE ID_SECCION=? " +
                    "ORDER BY ORDEN, ID_MEDIA";

            when(cn.prepareStatement(expectedSql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true, true, false);

            when(rs.getLong("ID_MEDIA")).thenReturn(1L, 2L);
            when(rs.getLong("ID_SECCION")).thenReturn(idSeccion, idSeccion);
            when(rs.getString("TIPO_MEDIA")).thenReturn("IMG", "VIDEO");
            when(rs.getString("URL")).thenReturn("url1", "url2");
            when(rs.getInt("ORDEN")).thenReturn(1, 2);

            MediaDAO dao = new MediaDAO();
            List<MediaDTO> lista = dao.listarPorSeccion(idSeccion);

            assertNotNull(lista);
            assertEquals(2, lista.size());

            MediaDTO m1 = lista.get(0);
            assertEquals(1L, m1.idMedia);
            assertEquals(idSeccion, m1.idSeccion);
            assertEquals("IMG", m1.tipoMedia);
            assertEquals("url1", m1.url);
            assertEquals(1, m1.orden);

            MediaDTO m2 = lista.get(1);
            assertEquals(2L, m2.idMedia);
            assertEquals(idSeccion, m2.idSeccion);
            assertEquals("VIDEO", m2.tipoMedia);
            assertEquals("url2", m2.url);
            assertEquals(2, m2.orden);

            verify(ps).setLong(1, idSeccion);
        }
    }
}
