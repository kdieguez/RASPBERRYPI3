package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.dto.RutaDTOs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RutaDAOTest {

    @Test
    @DisplayName("create lanza IllegalArgumentException si origen y destino son iguales")
    void create_mismoOrigenDestino_lanzaIAE() throws Exception {
        RutaDAO dao = new RutaDAO();
        RutaDTOs.Create dto = new RutaDTOs.Create(1L, 1L);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> dao.create(dto)
        );
        assertEquals("La ciudad de origen y destino no pueden ser iguales.", ex.getMessage());
    }

    @Test
    @DisplayName("create lanza IllegalArgumentException si ciudad de origen no existe o no está activa")
    void create_origenNoActivo_lanzaIAE() throws Exception {
        RutaDAO dao = new RutaDAO();
        RutaDTOs.Create dto = new RutaDTOs.Create(10L, 20L);

        Connection cn = mock(Connection.class);
        PreparedStatement psCiudad = mock(PreparedStatement.class);
        ResultSet rsCiudad = mock(ResultSet.class);

        String ciudadTable = "CIUDAD";
        String sqlCiudad = "SELECT 1 FROM " + ciudadTable +
                " WHERE ID_CIUDAD=? AND (ACTIVO=1 OR ACTIVO IS NULL)";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("CIUDAD")).thenReturn(ciudadTable);

            when(cn.prepareStatement(sqlCiudad)).thenReturn(psCiudad);
            when(psCiudad.executeQuery()).thenReturn(rsCiudad);
            when(rsCiudad.next()).thenReturn(false); 

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> dao.create(dto)
            );
            assertEquals("Ciudad de origen no existe o no está activa.", ex.getMessage());

            verify(psCiudad).setLong(1, 10L);
        }
    }

    @Test
    @DisplayName("create lanza IllegalArgumentException si ciudad de destino no existe o no está activa")
    void create_destinoNoActivo_lanzaIAE() throws Exception {
        RutaDAO dao = new RutaDAO();
        RutaDTOs.Create dto = new RutaDTOs.Create(10L, 20L);

        Connection cn = mock(Connection.class);
        PreparedStatement psCiudad1 = mock(PreparedStatement.class);
        PreparedStatement psCiudad2 = mock(PreparedStatement.class);
        ResultSet rsCiudad1 = mock(ResultSet.class);
        ResultSet rsCiudad2 = mock(ResultSet.class);

        String ciudadTable = "CIUDAD";
        String sqlCiudad = "SELECT 1 FROM " + ciudadTable +
                " WHERE ID_CIUDAD=? AND (ACTIVO=1 OR ACTIVO IS NULL)";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("CIUDAD")).thenReturn(ciudadTable);

            when(cn.prepareStatement(sqlCiudad)).thenReturn(psCiudad1, psCiudad2);

            when(psCiudad1.executeQuery()).thenReturn(rsCiudad1);
            when(rsCiudad1.next()).thenReturn(true);   

            when(psCiudad2.executeQuery()).thenReturn(rsCiudad2);
            when(rsCiudad2.next()).thenReturn(false); 

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> dao.create(dto)
            );
            assertEquals("Ciudad de destino no existe o no está activa.", ex.getMessage());

            verify(psCiudad1).setLong(1, 10L);
            verify(psCiudad2).setLong(1, 20L);
        }
    }

    @Test
    @DisplayName("create lanza IllegalArgumentException si la ruta ya existe")
    void create_rutaYaExiste_lanzaIAE() throws Exception {
        RutaDAO dao = new RutaDAO();
        RutaDTOs.Create dto = new RutaDTOs.Create(10L, 20L);

        Connection cn = mock(Connection.class);
        PreparedStatement psCiudad1 = mock(PreparedStatement.class);
        PreparedStatement psCiudad2 = mock(PreparedStatement.class);
        ResultSet rsCiudad1 = mock(ResultSet.class);
        ResultSet rsCiudad2 = mock(ResultSet.class);

        PreparedStatement psRutaCheck = mock(PreparedStatement.class);
        ResultSet rsRuta = mock(ResultSet.class);

        String ciudadTable = "CIUDAD";
        String rutaTable = "RUTA";
        String sqlCiudad = "SELECT 1 FROM " + ciudadTable +
                " WHERE ID_CIUDAD=? AND (ACTIVO=1 OR ACTIVO IS NULL)";
        String sqlRuta = "SELECT 1 FROM " + rutaTable +
                " WHERE ID_CIUDAD_ORIGEN=? AND ID_CIUDAD_DESTINO=?";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("CIUDAD")).thenReturn(ciudadTable);
            db.when(() -> DB.table("RUTA")).thenReturn(rutaTable);

            when(cn.prepareStatement(sqlCiudad)).thenReturn(psCiudad1, psCiudad2);
            when(psCiudad1.executeQuery()).thenReturn(rsCiudad1);
            when(rsCiudad1.next()).thenReturn(true);
            when(psCiudad2.executeQuery()).thenReturn(rsCiudad2);
            when(rsCiudad2.next()).thenReturn(true);

            when(cn.prepareStatement(sqlRuta)).thenReturn(psRutaCheck);
            when(psRutaCheck.executeQuery()).thenReturn(rsRuta);
            when(rsRuta.next()).thenReturn(true); 

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> dao.create(dto)
            );
            assertEquals("Ya existe una ruta con ese origen y destino.", ex.getMessage());
        }
    }

    @Test
    @DisplayName("create inserta nueva ruta y devuelve ID_RUTA")
    void create_exito_devuelveId() throws Exception {
        RutaDAO dao = new RutaDAO();
        RutaDTOs.Create dto = new RutaDTOs.Create(10L, 20L);

        Connection cn = mock(Connection.class);
        PreparedStatement psCiudad1 = mock(PreparedStatement.class);
        PreparedStatement psCiudad2 = mock(PreparedStatement.class);
        ResultSet rsCiudad1 = mock(ResultSet.class);
        ResultSet rsCiudad2 = mock(ResultSet.class);

        PreparedStatement psRutaCheck = mock(PreparedStatement.class);
        ResultSet rsRuta = mock(ResultSet.class);

        PreparedStatement psInsert = mock(PreparedStatement.class);
        ResultSet rsKeys = mock(ResultSet.class);

        String ciudadTable = "CIUDAD";
        String rutaTable = "RUTA";
        String sqlCiudad = "SELECT 1 FROM " + ciudadTable +
                " WHERE ID_CIUDAD=? AND (ACTIVO=1 OR ACTIVO IS NULL)";
        String sqlRuta = "SELECT 1 FROM " + rutaTable +
                " WHERE ID_CIUDAD_ORIGEN=? AND ID_CIUDAD_DESTINO=?";
        String sqlInsert = "INSERT INTO " + rutaTable +
                " (ID_CIUDAD_ORIGEN, ID_CIUDAD_DESTINO, ACTIVA) VALUES (?,?,1)";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("CIUDAD")).thenReturn(ciudadTable);
            db.when(() -> DB.table("RUTA")).thenReturn(rutaTable);

            
            when(cn.prepareStatement(sqlCiudad)).thenReturn(psCiudad1, psCiudad2);
            when(psCiudad1.executeQuery()).thenReturn(rsCiudad1);
            when(rsCiudad1.next()).thenReturn(true);
            when(psCiudad2.executeQuery()).thenReturn(rsCiudad2);
            when(rsCiudad2.next()).thenReturn(true);

            
            when(cn.prepareStatement(sqlRuta)).thenReturn(psRutaCheck);
            when(psRutaCheck.executeQuery()).thenReturn(rsRuta);
            when(rsRuta.next()).thenReturn(false);

            
            when(cn.prepareStatement(sqlInsert, new String[]{"ID_RUTA"}))
                    .thenReturn(psInsert);
            when(psInsert.getGeneratedKeys()).thenReturn(rsKeys);
            when(rsKeys.next()).thenReturn(true);
            when(rsKeys.getLong(1)).thenReturn(123L);

            long id = dao.create(dto);
            assertEquals(123L, id);

            verify(psInsert).setLong(1, 10L);
            verify(psInsert).setLong(2, 20L);
            verify(psInsert).executeUpdate();
        }
    }

    @Test
    @DisplayName("create lanza SQLException si no se genera ID_RUTA")
    void create_sinGeneratedKey_lanzaSQLException() throws Exception {
        RutaDAO dao = new RutaDAO();
        RutaDTOs.Create dto = new RutaDTOs.Create(10L, 20L);

        Connection cn = mock(Connection.class);
        PreparedStatement psCiudad1 = mock(PreparedStatement.class);
        PreparedStatement psCiudad2 = mock(PreparedStatement.class);
        ResultSet rsCiudad1 = mock(ResultSet.class);
        ResultSet rsCiudad2 = mock(ResultSet.class);

        PreparedStatement psRutaCheck = mock(PreparedStatement.class);
        ResultSet rsRuta = mock(ResultSet.class);

        PreparedStatement psInsert = mock(PreparedStatement.class);
        ResultSet rsKeys = mock(ResultSet.class);

        String ciudadTable = "CIUDAD";
        String rutaTable = "RUTA";
        String sqlCiudad = "SELECT 1 FROM " + ciudadTable +
                " WHERE ID_CIUDAD=? AND (ACTIVO=1 OR ACTIVO IS NULL)";
        String sqlRuta = "SELECT 1 FROM " + rutaTable +
                " WHERE ID_CIUDAD_ORIGEN=? AND ID_CIUDAD_DESTINO=?";
        String sqlInsert = "INSERT INTO " + rutaTable +
                " (ID_CIUDAD_ORIGEN, ID_CIUDAD_DESTINO, ACTIVA) VALUES (?,?,1)";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(DB::getConnection).thenReturn(cn);
            db.when(() -> DB.table("CIUDAD")).thenReturn(ciudadTable);
            db.when(() -> DB.table("RUTA")).thenReturn(rutaTable);

            when(cn.prepareStatement(sqlCiudad)).thenReturn(psCiudad1, psCiudad2);
            when(psCiudad1.executeQuery()).thenReturn(rsCiudad1);
            when(rsCiudad1.next()).thenReturn(true);
            when(psCiudad2.executeQuery()).thenReturn(rsCiudad2);
            when(rsCiudad2.next()).thenReturn(true);

            when(cn.prepareStatement(sqlRuta)).thenReturn(psRutaCheck);
            when(psRutaCheck.executeQuery()).thenReturn(rsRuta);
            when(rsRuta.next()).thenReturn(false);

            when(cn.prepareStatement(sqlInsert, new String[]{"ID_RUTA"}))
                    .thenReturn(psInsert);
            when(psInsert.getGeneratedKeys()).thenReturn(rsKeys);
            when(rsKeys.next()).thenReturn(false); 

            SQLException ex = assertThrows(
                    SQLException.class,
                    () -> dao.create(dto)
            );
            assertEquals("No se generó ID_RUTA", ex.getMessage());
        }
    }

    @Test
    @DisplayName("listAll mapea correctamente las rutas")
    void listAll_mapeaCorrectamente() throws Exception {
        RutaDAO dao = new RutaDAO();

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        String rutaTable = "RUTA";
        String ciudadTable = "CIUDAD";
        String sql = "SELECT r.ID_RUTA, r.ID_CIUDAD_ORIGEN, co.NOMBRE AS CIUDAD_ORIGEN, " +
                "r.ID_CIUDAD_DESTINO, cd.NOMBRE AS CIUDAD_DESTINO, r.ACTIVA " +
                "FROM " + rutaTable + " r " +
                "JOIN " + ciudadTable + " co ON co.ID_CIUDAD = r.ID_CIUDAD_ORIGEN " +
                "JOIN " + ciudadTable + " cd ON cd.ID_CIUDAD = r.ID_CIUDAD_DESTINO " +
                "ORDER BY co.NOMBRE, cd.NOMBRE";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(() -> DB.table("RUTA")).thenReturn(rutaTable);
            db.when(() -> DB.table("CIUDAD")).thenReturn(ciudadTable);
            db.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true, true, false);
            when(rs.getLong("ID_RUTA")).thenReturn(1L, 2L);
            when(rs.getLong("ID_CIUDAD_ORIGEN")).thenReturn(10L, 30L);
            when(rs.getString("CIUDAD_ORIGEN")).thenReturn("Guatemala", "Bogotá");
            when(rs.getLong("ID_CIUDAD_DESTINO")).thenReturn(20L, 40L);
            when(rs.getString("CIUDAD_DESTINO")).thenReturn("México", "Lima");
            when(rs.getInt("ACTIVA")).thenReturn(1, 0);

            List<RutaDTOs.View> list = dao.listAll();
            assertEquals(2, list.size());

            RutaDTOs.View r1 = list.get(0);
            assertEquals(1L, r1.idRuta());
            assertTrue(r1.activa());

            RutaDTOs.View r2 = list.get(1);
            assertEquals(2L, r2.idRuta());
            assertFalse(r2.activa());
        }
    }

    @Test
    @DisplayName("toggleActiva cambia el estado cuando la ruta existe")
    void toggleActiva_exito() throws Exception {
        RutaDAO dao = new RutaDAO();
        long idRuta = 99L;

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        String rutaTable = "RUTA";
        String sql = "UPDATE " + rutaTable +
                " SET ACTIVA = CASE WHEN ACTIVA=1 THEN 0 ELSE 1 END WHERE ID_RUTA=?";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(() -> DB.table("RUTA")).thenReturn(rutaTable);
            db.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(1);

            dao.toggleActiva(idRuta);

            verify(ps).setLong(1, idRuta);
            verify(ps).executeUpdate();
        }
    }

    @Test
    @DisplayName("toggleActiva lanza IllegalArgumentException si no se encontró la ruta")
    void toggleActiva_rutaNoEncontrada_lanzaIAE() throws Exception {
        RutaDAO dao = new RutaDAO();
        long idRuta = 123L;

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        String rutaTable = "RUTA";
        String sql = "UPDATE " + rutaTable +
                " SET ACTIVA = CASE WHEN ACTIVA=1 THEN 0 ELSE 1 END WHERE ID_RUTA=?";

        try (MockedStatic<DB> db = mockStatic(DB.class)) {
            db.when(() -> DB.table("RUTA")).thenReturn(rutaTable);
            db.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(0);

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> dao.toggleActiva(idRuta)
            );
            assertEquals("Ruta no encontrada.", ex.getMessage());
        }
    }
}
