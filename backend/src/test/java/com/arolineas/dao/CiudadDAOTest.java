package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.dto.CiudadDTOs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CiudadDAOTest {  

    @Test
    @DisplayName("listAll(null) debe devolver la lista de ciudades mapeada correctamente")
    void listAll_sinFiltroPais_mapeaCorrectamente() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

            dbMock.when(() -> DB.table("CIUDAD")).thenReturn("CIUDAD");
            dbMock.when(() -> DB.table("PAIS")).thenReturn("PAIS");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String expectedSql =
                    "SELECT c.ID_CIUDAD, c.ID_PAIS, p.NOMBRE AS PAIS, c.NOMBRE, NVL(c.ACTIVO,1) AS ACTIVO " +
                            "FROM CIUDAD c JOIN PAIS p ON p.ID_PAIS = c.ID_PAIS " +
                            " ORDER BY p.NOMBRE, c.NOMBRE";

            when(cn.prepareStatement(expectedSql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true, true, false);

            when(rs.getLong("ID_CIUDAD")).thenReturn(1L, 2L);
            when(rs.getLong("ID_PAIS")).thenReturn(10L, 20L);
            when(rs.getString("PAIS")).thenReturn("Guatemala", "México");
            when(rs.getString("NOMBRE")).thenReturn("Ciudad A", "Ciudad B");

            when(rs.getInt("ACTIVO")).thenReturn(1, 0);

            CiudadDAO dao = new CiudadDAO();

            List<CiudadDTOs.View> result = dao.listAll(null);

            assertNotNull(result);
            assertEquals(2, result.size(), "Debe devolver 2 ciudades");

            CiudadDTOs.View c1 = result.get(0);
            assertEquals(1L, c1.idCiudad());
            assertEquals(10L, c1.idPais());
            assertEquals("Guatemala", c1.pais());
            assertEquals("Ciudad A", c1.nombre());
            assertTrue(c1.activo(), "La primera debe estar activa");

            CiudadDTOs.View c2 = result.get(1);
            assertEquals(2L, c2.idCiudad());
            assertEquals(20L, c2.idPais());
            assertEquals("México", c2.pais());
            assertEquals("Ciudad B", c2.nombre());
            assertFalse(c2.activo(), "La segunda debe estar inactiva");
        }
    }

    @Test
    @DisplayName("listAll(idPais) usa el WHERE y mapea correctamente")
    void listAll_conFiltroPais_mapeaCorrectamente() throws Exception {
        long idPaisFiltro = 10L;

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

            dbMock.when(() -> DB.table("CIUDAD")).thenReturn("CIUDAD");
            dbMock.when(() -> DB.table("PAIS")).thenReturn("PAIS");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String base =
                    "SELECT c.ID_CIUDAD, c.ID_PAIS, p.NOMBRE AS PAIS, c.NOMBRE, NVL(c.ACTIVO,1) AS ACTIVO " +
                            "FROM CIUDAD c JOIN PAIS p ON p.ID_PAIS = c.ID_PAIS ";
            String order = " ORDER BY p.NOMBRE, c.NOMBRE";
            String expectedSql = base + " WHERE c.ID_PAIS=? " + order;

            when(cn.prepareStatement(expectedSql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true, false);
            when(rs.getLong("ID_CIUDAD")).thenReturn(7L);
            when(rs.getLong("ID_PAIS")).thenReturn(idPaisFiltro);
            when(rs.getString("PAIS")).thenReturn("Guatemala");
            when(rs.getString("NOMBRE")).thenReturn("Ciudad Filtrada");
            when(rs.getInt("ACTIVO")).thenReturn(1);

            CiudadDAO dao = new CiudadDAO();

            List<CiudadDTOs.View> result = dao.listAll(idPaisFiltro);

            assertEquals(1, result.size());
            verify(ps).setLong(1, idPaisFiltro);

            CiudadDTOs.View c = result.get(0);
            assertEquals(7L, c.idCiudad());
            assertEquals(idPaisFiltro, c.idPais());
            assertEquals("Guatemala", c.pais());
            assertEquals("Ciudad Filtrada", c.nombre());
            assertTrue(c.activo());
        }
    }

    @Test
    @DisplayName("listForWeather() debe devolver solo ciudades con weatherQuery mapeadas correctamente")
    void listForWeather_mapeaCorrectamente() throws Exception {

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("CIUDAD")).thenReturn("CIUDAD");
            dbMock.when(() -> DB.table("PAIS")).thenReturn("PAIS");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String expectedSql =
                    "SELECT c.ID_CIUDAD, c.NOMBRE AS CIUDAD, p.NOMBRE AS PAIS, c.WEATHER_QUERY " +
                            "FROM CIUDAD c JOIN PAIS p ON p.ID_PAIS = c.ID_PAIS " +
                            "WHERE NVL(c.ACTIVO,1)=1 AND c.WEATHER_QUERY IS NOT NULL " +
                            "ORDER BY p.NOMBRE, c.NOMBRE";

            when(cn.prepareStatement(expectedSql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true, false);

            when(rs.getLong("ID_CIUDAD")).thenReturn(99L);
            when(rs.getString("CIUDAD")).thenReturn("Guatemala City");
            when(rs.getString("PAIS")).thenReturn("Guatemala");
            when(rs.getString("WEATHER_QUERY")).thenReturn("Guatemala City,GT");

            CiudadDAO dao = new CiudadDAO();

            List<CiudadDTOs.WeatherCity> result = dao.listForWeather();

            assertNotNull(result);
            assertEquals(1, result.size());

            CiudadDTOs.WeatherCity city = result.get(0);
            assertEquals(99L, city.idCiudad());
            assertEquals("Guatemala City", city.ciudad());
            assertEquals("Guatemala", city.pais());
            assertEquals("Guatemala City,GT", city.weatherQuery());
        }
    }

    

    @Test
    @DisplayName("toggleActiva(id) lanza IllegalArgumentException si no se actualiza ninguna ciudad")
    void toggleActiva_ciudadNoEncontrada_lanzaExcepcion() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("CIUDAD")).thenReturn("CIUDAD");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String expectedSql =
                    "UPDATE CIUDAD SET ACTIVO = CASE WHEN NVL(ACTIVO,1)=1 THEN 0 ELSE 1 END WHERE ID_CIUDAD=?";

            when(cn.prepareStatement(expectedSql)).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(0);

            CiudadDAO dao = new CiudadDAO();

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> dao.toggleActiva(123L)
            );

            assertEquals("Ciudad no encontrada.", ex.getMessage());

            verify(ps, times(1)).setLong(1, 123L);
            verify(ps, times(1)).executeUpdate();
        }
    }

    @Test
    @DisplayName("toggleActiva(id) no lanza excepción cuando sí se actualiza al menos una fila")
    void toggleActiva_ciudadEncontrada_ok() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("CIUDAD")).thenReturn("CIUDAD");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String expectedSql =
                    "UPDATE CIUDAD SET ACTIVO = CASE WHEN NVL(ACTIVO,1)=1 THEN 0 ELSE 1 END WHERE ID_CIUDAD=?";

            when(cn.prepareStatement(expectedSql)).thenReturn(ps);
            when(ps.executeUpdate()).thenReturn(1);

            CiudadDAO dao = new CiudadDAO();

            assertDoesNotThrow(() -> dao.toggleActiva(45L));

            verify(ps, times(1)).setLong(1, 45L);
            verify(ps, times(1)).executeUpdate();
        }
    }

    

    @Test
    @DisplayName("create() lanza IllegalArgumentException si dto es nulo o nombre vacío")
    void create_nombreInvalido_lanzaExcepcion() throws Exception {
        CiudadDAO dao = new CiudadDAO();

        assertThrows(IllegalArgumentException.class,
                () -> dao.create(null));

        CiudadDTOs.Create dtoSinNombre = new CiudadDTOs.Create(1L, "   ", null);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> dao.create(dtoSinNombre));
        assertTrue(ex.getMessage().contains("nombre de la ciudad es requerido"));
    }

    @Test
    @DisplayName("create() lanza IllegalArgumentException si el país no existe")
    void create_paisNoExiste_lanzaExcepcion() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psPais = mock(PreparedStatement.class);
        ResultSet rsPais = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("PAIS")).thenReturn("PAIS");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String sqlPais = "SELECT 1 FROM PAIS WHERE ID_PAIS=?";

            when(cn.prepareStatement(sqlPais)).thenReturn(psPais);
            when(psPais.executeQuery()).thenReturn(rsPais);
            when(rsPais.next()).thenReturn(false); 

            CiudadDAO dao = new CiudadDAO();
            CiudadDTOs.Create dto = new CiudadDTOs.Create(10L, "X", "Q");

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> dao.create(dto)
            );
            assertTrue(ex.getMessage().contains("país indicado no existe"));
        }
    }

    @Test
    @DisplayName("create() lanza IllegalArgumentException si ya existe ciudad en el país")
    void create_ciudadYaExisteEnPais_lanzaExcepcion() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psPais = mock(PreparedStatement.class);
        PreparedStatement psCiudad = mock(PreparedStatement.class);
        ResultSet rsPais = mock(ResultSet.class);
        ResultSet rsCiudad = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("PAIS")).thenReturn("PAIS");
            dbMock.when(() -> DB.table("CIUDAD")).thenReturn("CIUDAD");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String sqlPais = "SELECT 1 FROM PAIS WHERE ID_PAIS=?";
            String sqlCiudad =
                    "SELECT 1 FROM CIUDAD WHERE ID_PAIS=? AND UPPER(NOMBRE)=UPPER(?)";

            when(cn.prepareStatement(sqlPais)).thenReturn(psPais);
            when(cn.prepareStatement(sqlCiudad)).thenReturn(psCiudad);

            when(psPais.executeQuery()).thenReturn(rsPais);
            when(rsPais.next()).thenReturn(true); 

            when(psCiudad.executeQuery()).thenReturn(rsCiudad);
            when(rsCiudad.next()).thenReturn(true); 

            CiudadDAO dao = new CiudadDAO();
            CiudadDTOs.Create dto = new CiudadDTOs.Create(10L, "Ciudad A", "Q");

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> dao.create(dto)
            );
            assertTrue(ex.getMessage().contains("Ya existe una ciudad"));
        }
    }

    @Test
    @DisplayName("create() inserta correctamente cuando país existe, ciudad no existe y weatherQuery tiene valor")
    void create_ok_conWeatherQuery() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psPais = mock(PreparedStatement.class);
        PreparedStatement psCiudad = mock(PreparedStatement.class);
        PreparedStatement psInsert = mock(PreparedStatement.class);
        ResultSet rsPais = mock(ResultSet.class);
        ResultSet rsCiudad = mock(ResultSet.class);
        ResultSet rsKeys = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("PAIS")).thenReturn("PAIS");
            dbMock.when(() -> DB.table("CIUDAD")).thenReturn("CIUDAD");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String sqlPais = "SELECT 1 FROM PAIS WHERE ID_PAIS=?";
            String sqlCiudad =
                    "SELECT 1 FROM CIUDAD WHERE ID_PAIS=? AND UPPER(NOMBRE)=UPPER(?)";
            String sqlInsert =
                    "INSERT INTO CIUDAD (ID_PAIS, NOMBRE, ACTIVO, WEATHER_QUERY) VALUES (?,?,1,?)";

            when(cn.prepareStatement(sqlPais)).thenReturn(psPais);
            when(cn.prepareStatement(sqlCiudad)).thenReturn(psCiudad);
            when(cn.prepareStatement(eq(sqlInsert), any(String[].class)))
                    .thenReturn(psInsert);

            when(psPais.executeQuery()).thenReturn(rsPais);
            when(rsPais.next()).thenReturn(true); 

            when(psCiudad.executeQuery()).thenReturn(rsCiudad);
            when(rsCiudad.next()).thenReturn(false); 

            when(psInsert.executeUpdate()).thenReturn(1);
            when(psInsert.getGeneratedKeys()).thenReturn(rsKeys);
            when(rsKeys.next()).thenReturn(true);
            when(rsKeys.getLong(1)).thenReturn(123L);

            CiudadDAO dao = new CiudadDAO();
            CiudadDTOs.Create dto =
                    new CiudadDTOs.Create(10L, "Ciudad Nueva", "Ciudad Nueva,GT");

            long idGen = dao.create(dto);

            assertEquals(123L, idGen);
            verify(psInsert).setLong(1, 10L);
            verify(psInsert).setString(2, "Ciudad Nueva");
            verify(psInsert).setString(3, "Ciudad Nueva,GT");
        }
    }

    @Test
    @DisplayName("create() inserta usando setNull cuando weatherQuery es nulo o vacío")
    void create_ok_sinWeatherQuery() throws Exception {
        Connection cn = mock(Connection.class);
        PreparedStatement psPais = mock(PreparedStatement.class);
        PreparedStatement psCiudad = mock(PreparedStatement.class);
        PreparedStatement psInsert = mock(PreparedStatement.class);
        ResultSet rsPais = mock(ResultSet.class);
        ResultSet rsCiudad = mock(ResultSet.class);
        ResultSet rsKeys = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("PAIS")).thenReturn("PAIS");
            dbMock.when(() -> DB.table("CIUDAD")).thenReturn("CIUDAD");
            dbMock.when(DB::getConnection).thenReturn(cn);

            String sqlPais = "SELECT 1 FROM PAIS WHERE ID_PAIS=?";
            String sqlCiudad =
                    "SELECT 1 FROM CIUDAD WHERE ID_PAIS=? AND UPPER(NOMBRE)=UPPER(?)";
            String sqlInsert =
                    "INSERT INTO CIUDAD (ID_PAIS, NOMBRE, ACTIVO, WEATHER_QUERY) VALUES (?,?,1,?)";

            when(cn.prepareStatement(sqlPais)).thenReturn(psPais);
            when(cn.prepareStatement(sqlCiudad)).thenReturn(psCiudad);
            when(cn.prepareStatement(eq(sqlInsert), any(String[].class)))
                    .thenReturn(psInsert);

            when(psPais.executeQuery()).thenReturn(rsPais);
            when(rsPais.next()).thenReturn(true);

            when(psCiudad.executeQuery()).thenReturn(rsCiudad);
            when(rsCiudad.next()).thenReturn(false);

            when(psInsert.executeUpdate()).thenReturn(1);
            when(psInsert.getGeneratedKeys()).thenReturn(rsKeys);
            when(rsKeys.next()).thenReturn(true);
            when(rsKeys.getLong(1)).thenReturn(200L);

            CiudadDAO dao = new CiudadDAO();
            
            CiudadDTOs.Create dto =
                    new CiudadDTOs.Create(5L, "Sin Weather", "   ");

            long idGen = dao.create(dto);

            assertEquals(200L, idGen);
            verify(psInsert).setNull(3, Types.VARCHAR);
        }
    }
}
