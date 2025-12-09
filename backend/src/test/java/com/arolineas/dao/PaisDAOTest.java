package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.dto.PaisDTOs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaisDAOTest {

    private static final String TABLA = "PAIS";

    @Test
    @DisplayName("create lanza IAE si dto es null")
    void create_dtoNull() throws Exception {
        PaisDAO dao = new PaisDAO();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> dao.create(null)
        );
        assertEquals("El nombre del país es requerido.", ex.getMessage());
    }

    @Test
    @DisplayName("create lanza IAE si nombre es null")
    void create_nombreNull() throws Exception {
        PaisDAO dao = new PaisDAO();
        PaisDTOs.Create dto = new PaisDTOs.Create(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> dao.create(dto)
        );
        assertEquals("El nombre del país es requerido.", ex.getMessage());
    }

    @Test
    @DisplayName("create lanza IAE si nombre viene en blanco")
    void create_nombreBlanco() throws Exception {
        PaisDAO dao = new PaisDAO();
        PaisDTOs.Create dto = new PaisDTOs.Create("   ");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> dao.create(dto)
        );
        assertEquals("El nombre del país es requerido.", ex.getMessage());
    }

    @Test
    @DisplayName("create lanza IAE si ya existe país con ese nombre")
    void create_paisYaExiste() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psExiste = mock(PreparedStatement.class);
        ResultSet rsExiste = mock(ResultSet.class);

        String sqlExiste =
                "SELECT 1 FROM " + TABLA + " WHERE UPPER(NOMBRE)=UPPER(?)";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("PAIS")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sqlExiste)).thenReturn(psExiste);
            when(psExiste.executeQuery()).thenReturn(rsExiste);
            when(rsExiste.next()).thenReturn(true); 

            PaisDAO dao = new PaisDAO();
            PaisDTOs.Create dto = new PaisDTOs.Create("Guatemala");

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> dao.create(dto)
            );
            assertEquals("Ya existe un país con ese nombre.", ex.getMessage());

            
            verify(cn, never()).prepareStatement(
                    startsWith("INSERT INTO "), any(String[].class));
        }
    }

    @Test
    @DisplayName("create inserta país y devuelve ID_PAIS generado")
    void create_ok() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psExiste = mock(PreparedStatement.class);
        ResultSet rsExiste = mock(ResultSet.class);

        PreparedStatement psInsert = mock(PreparedStatement.class);
        ResultSet rsKeys = mock(ResultSet.class);

        String sqlExiste =
                "SELECT 1 FROM " + TABLA + " WHERE UPPER(NOMBRE)=UPPER(?)";
        String sqlInsert =
                "INSERT INTO " + TABLA + " (NOMBRE) VALUES (?)";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("PAIS")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sqlExiste)).thenReturn(psExiste);
            when(psExiste.executeQuery()).thenReturn(rsExiste);
            when(rsExiste.next()).thenReturn(false); 

            when(cn.prepareStatement(sqlInsert, new String[]{"ID_PAIS"}))
                    .thenReturn(psInsert);
            when(psInsert.getGeneratedKeys()).thenReturn(rsKeys);
            when(rsKeys.next()).thenReturn(true);
            when(rsKeys.getLong(1)).thenReturn(5L);

            PaisDAO dao = new PaisDAO();
            PaisDTOs.Create dto = new PaisDTOs.Create(" Guatemala "); 

            long id = dao.create(dto);

            assertEquals(5L, id);
            verify(psExiste).setString(1, "Guatemala");
            verify(psInsert).setString(1, "Guatemala");
            verify(psInsert).executeUpdate();
        }
    }    

    @Test
    @DisplayName("create lanza SQLException si no se genera ID_PAIS")
    void create_sinGeneratedKey() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psExiste = mock(PreparedStatement.class);
        ResultSet rsExiste = mock(ResultSet.class);

        PreparedStatement psInsert = mock(PreparedStatement.class);
        ResultSet rsKeys = mock(ResultSet.class);

        String sqlExiste =
                "SELECT 1 FROM " + TABLA + " WHERE UPPER(NOMBRE)=UPPER(?)";
        String sqlInsert =
                "INSERT INTO " + TABLA + " (NOMBRE) VALUES (?)";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("PAIS")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sqlExiste)).thenReturn(psExiste);
            when(psExiste.executeQuery()).thenReturn(rsExiste);
            when(rsExiste.next()).thenReturn(false);

            when(cn.prepareStatement(sqlInsert, new String[]{"ID_PAIS"}))
                    .thenReturn(psInsert);
            when(psInsert.getGeneratedKeys()).thenReturn(rsKeys);
            when(rsKeys.next()).thenReturn(false); 

            PaisDAO dao = new PaisDAO();
            PaisDTOs.Create dto = new PaisDTOs.Create("Mexico");

            SQLException ex = assertThrows(
                    SQLException.class,
                    () -> dao.create(dto)
            );
            assertEquals("No se generó ID_PAIS", ex.getMessage());
        }
    }

    @Test
    @DisplayName("listAll devuelve lista de países ordenada por nombre")
    void listAll_ok() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        String sqlList =
                "SELECT ID_PAIS, NOMBRE FROM " + TABLA + " ORDER BY NOMBRE";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("PAIS")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sqlList)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true, true, false);
            when(rs.getLong("ID_PAIS")).thenReturn(1L, 2L);
            when(rs.getString("NOMBRE")).thenReturn("El Salvador", "Guatemala");

            PaisDAO dao = new PaisDAO();
            List<PaisDTOs.View> lista = dao.listAll();

            assertEquals(2, lista.size());
            assertEquals(1L, lista.get(0).idPais());
            assertEquals("El Salvador", lista.get(0).nombre());
            assertEquals(2L, lista.get(1).idPais());
            assertEquals("Guatemala", lista.get(1).nombre());
        }
    }

    @Test
    @DisplayName("listAll puede devolver lista vacía")
    void listAll_vacio() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        String sqlList =
                "SELECT ID_PAIS, NOMBRE FROM " + TABLA + " ORDER BY NOMBRE";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("PAIS")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sqlList)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false); 

            PaisDAO dao = new PaisDAO();
            List<PaisDTOs.View> lista = dao.listAll();

            assertNotNull(lista);
            assertTrue(lista.isEmpty());
        }
    }
}
