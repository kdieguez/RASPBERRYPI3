package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.dto.ClaseDTOs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClaseDAOTest {

    @Test
    @DisplayName("listAll() debe devolver la lista de clases mapeada correctamente")
    void listAll_mapeaCorrectamente() throws Exception {
        
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            
            dbMock.when(() -> DB.table("CLASE_ASIENTO")).thenReturn("CLASE_ASIENTO");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String expectedSql = "SELECT ID_CLASE, NOMBRE FROM CLASE_ASIENTO ORDER BY ID_CLASE";

            when(cn.prepareStatement(expectedSql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
   
            when(rs.next()).thenReturn(true, true, false);

            when(rs.getInt("ID_CLASE")).thenReturn(1, 2);
            when(rs.getString("NOMBRE")).thenReturn("Económica", "Ejecutiva");

            ClaseDAO dao = new ClaseDAO();
            
            List<ClaseDTOs.View> result = dao.listAll();
           
            assertNotNull(result);
            assertEquals(2, result.size(), "Debe devolver 2 clases");

            ClaseDTOs.View c1 = result.get(0);
            assertEquals(1, c1.idClase());
            assertEquals("Económica", c1.nombre());

            ClaseDTOs.View c2 = result.get(1);
            assertEquals(2, c2.idClase());
            assertEquals("Ejecutiva", c2.nombre());
           
            verify(cn, times(1)).prepareStatement(expectedSql);
            verify(ps, times(1)).executeQuery();
        }
    }

    @Test
    @DisplayName("listAll() debe devolver lista vacía cuando no hay clases")
    void listAll_sinResultados_devuelveListaVacia() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("CLASE_ASIENTO")).thenReturn("CLASE_ASIENTO");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String expectedSql = "SELECT ID_CLASE, NOMBRE FROM CLASE_ASIENTO ORDER BY ID_CLASE";

            when(cn.prepareStatement(expectedSql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
           
            when(rs.next()).thenReturn(false);

            ClaseDAO dao = new ClaseDAO();

            List<ClaseDTOs.View> result = dao.listAll();

            assertNotNull(result);
            assertTrue(result.isEmpty(), "Debe devolver lista vacía si no hay registros");
        }
    }
}
