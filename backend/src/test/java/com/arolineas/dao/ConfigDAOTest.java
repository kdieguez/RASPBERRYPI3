package com.aerolineas.dao;

import com.aerolineas.config.DB;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigDAOTest {

    @Test
    @DisplayName("getSection devuelve el mapa de nombre → descripción correctamente")
    void getSection_mapeaCorrectamente() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("DATOS_ESTRUCTURA")).thenReturn("DATOS_ESTRUCTURA");
            dbMock.when(() -> DB.table("ESTRUCTURA")).thenReturn("ESTRUCTURA");

            String expectedSql =
                    "SELECT d.NOMBRE, d.DESCRIPCION FROM DATOS_ESTRUCTURA d " +
                    "JOIN ESTRUCTURA e ON e.ID_ESTRUCTURA = d.ID_ESTRUCTURA " +
                    "WHERE LOWER(e.SECCION) = ?";

            when(cn.prepareStatement(expectedSql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true, true, false);
            when(rs.getString("NOMBRE")).thenReturn("titulo", "subtitulo");
            when(rs.getString("DESCRIPCION")).thenReturn("Mi título", "Mi subtítulo");

            ConfigDAO dao = new ConfigDAO(cn);
            Map<String, String> result = dao.getSection("HeAdEr"); 

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("Mi título", result.get("titulo"));
            assertEquals("Mi subtítulo", result.get("subtitulo"));

            
            verify(ps).setString(1, "header");
        }
    }

    @Test
    @DisplayName("upsertItem ejecuta el MERGE con los parámetros correctos")
    void upsertItem_ejecutaMergeCorrecto() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("DATOS_ESTRUCTURA")).thenReturn("DATOS_ESTRUCTURA");
            dbMock.when(() -> DB.table("ESTRUCTURA")).thenReturn("ESTRUCTURA");

            String expectedSql =
                "MERGE INTO DATOS_ESTRUCTURA dst " +
                "USING (SELECT ? AS NOMBRE, (SELECT ID_ESTRUCTURA FROM ESTRUCTURA WHERE LOWER(SECCION)=?) AS ID_ESTRUCTURA FROM dual) src " +
                "ON (dst.NOMBRE = src.NOMBRE AND dst.ID_ESTRUCTURA = src.ID_ESTRUCTURA) " +
                "WHEN MATCHED THEN UPDATE SET dst.DESCRIPCION = ? " +
                "WHEN NOT MATCHED THEN INSERT (NOMBRE, DESCRIPCION, ID_ESTRUCTURA) " +
                "VALUES (src.NOMBRE, ?, src.ID_ESTRUCTURA)";

            when(cn.prepareStatement(expectedSql)).thenReturn(ps);

            ConfigDAO dao = new ConfigDAO(cn);
            dao.upsertItem("Footer", "telefono", "12345678");
 
            verify(ps).setString(1, "telefono");
            verify(ps).setString(2, "footer");       
            verify(ps).setString(3, "12345678");
            verify(ps).setString(4, "12345678");
            verify(ps).executeUpdate();
        }
    }

    @Test
    @DisplayName("upsertSection recorre el mapa, hace commit y restaura autoCommit")
    void upsertSection_ok_commit() throws Exception {
        Connection cn = mock(Connection.class);
        when(cn.getAutoCommit()).thenReturn(true);

        ConfigDAO realDao = new ConfigDAO(cn);
        ConfigDAO dao = spy(realDao);

        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("titulo", "Mi título");
        entries.put("subtitulo", "Mi subtítulo");

        doNothing().when(dao).upsertItem(anyString(), anyString(), anyString());

        dao.upsertSection("header", entries);

        verify(cn).setAutoCommit(false);
        verify(dao, times(2)).upsertItem(eq("header"), anyString(), anyString());
        verify(cn).commit();
        verify(cn).setAutoCommit(true);
        verify(cn, never()).rollback();
    }

    @Test
    @DisplayName("upsertSection hace rollback y relanza la excepción cuando falla algún item")
    void upsertSection_error_rollback() throws Exception {
        Connection cn = mock(Connection.class);
        when(cn.getAutoCommit()).thenReturn(true);

        ConfigDAO realDao = new ConfigDAO(cn);
        ConfigDAO dao = spy(realDao);

        Map<String, String> entries = new LinkedHashMap<>();
        entries.put("titulo", "Mi título");
        entries.put("subtitulo", "Mi subtítulo");

        doThrow(new SQLException("Falla X"))
                .when(dao)
                .upsertItem(eq("header"), eq("titulo"), eq("Mi título"));

        SQLException ex = assertThrows(
                SQLException.class,
                () -> dao.upsertSection("header", entries)
        );
        assertEquals("Falla X", ex.getMessage());

        verify(cn).setAutoCommit(false);
        verify(cn).rollback();
        verify(cn).setAutoCommit(true);
        
        verify(cn, never()).commit();
    }

    @Test
    @DisplayName("getAll llama a getSection(header/footer) y arma el mapa compuesto")
    void getAll_usaGetSection() throws Exception {
        Connection cn = mock(Connection.class);
        ConfigDAO realDao = new ConfigDAO(cn);
        ConfigDAO dao = spy(realDao);

        Map<String, String> header = Map.of("titulo", "Header título");
        Map<String, String> footer = Map.of("telefono", "12345678");

        doReturn(header).when(dao).getSection("header");
        doReturn(footer).when(dao).getSection("footer");

        Map<String, Map<String, String>> result = dao.getAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(header, result.get("header"));
        assertEquals(footer, result.get("footer"));

        verify(dao).getSection("header");
        verify(dao).getSection("footer");
    }
}
