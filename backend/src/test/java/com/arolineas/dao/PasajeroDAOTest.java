package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.model.Pasajero;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.*;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasajeroDAOTest {

    private static final String TABLA = "PASAJERO";

    @Test
    @DisplayName("findByUsuario devuelve pasajero mapeado cuando existe registro")
    void findByUsuario_existente() throws Exception {
        long idUsuario = 10L;

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        String sql = "SELECT ID_PASAJERO, FECHA_NACIMIENTO, ID_PAIS_DOCUMENTO, PASAPORTE, ID_USUARIO " +
                     "FROM " + TABLA + " WHERE ID_USUARIO = ?";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("PASAJERO")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true);

            Date fecha = Date.valueOf(LocalDate.of(1990, 1, 2));

            when(rs.getLong("ID_PASAJERO")).thenReturn(1L);
            when(rs.getDate("FECHA_NACIMIENTO")).thenReturn(fecha);
            when(rs.getLong("ID_PAIS_DOCUMENTO")).thenReturn(502L);
            when(rs.wasNull()).thenReturn(false);
            when(rs.getString("PASAPORTE")).thenReturn("P123");
            when(rs.getLong("ID_USUARIO")).thenReturn(idUsuario);

            PasajeroDAO dao = new PasajeroDAO();
            Pasajero p = dao.findByUsuario(idUsuario);

            assertNotNull(p);
            assertEquals(1L, p.getIdPasajero());
            assertEquals(LocalDate.of(1990, 1, 2), p.getFechaNacimiento());
            assertEquals(Long.valueOf(502L), p.getIdPaisDocumento());
            assertEquals("P123", p.getPasaporte());
            assertEquals(idUsuario, p.getIdUsuario());

            verify(ps).setLong(1, idUsuario);
        }
    }

    @Test
    @DisplayName("findByUsuario devuelve null cuando no hay registro")
    void findByUsuario_noExiste() throws Exception {
        long idUsuario = 20L;

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        String sql = "SELECT ID_PASAJERO, FECHA_NACIMIENTO, ID_PAIS_DOCUMENTO, PASAPORTE, ID_USUARIO " +
                     "FROM " + TABLA + " WHERE ID_USUARIO = ?";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("PASAJERO")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(false);  

            PasajeroDAO dao = new PasajeroDAO();
            Pasajero p = dao.findByUsuario(idUsuario);

            assertNull(p);
        }
    }

    @Test
    @DisplayName("findByUsuario mapea fecha y país como null cuando vienen null en BD")
    void findByUsuario_fechaYPaisNull() throws Exception {
        long idUsuario = 30L;

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        String sql = "SELECT ID_PASAJERO, FECHA_NACIMIENTO, ID_PAIS_DOCUMENTO, PASAPORTE, ID_USUARIO " +
                     "FROM " + TABLA + " WHERE ID_USUARIO = ?";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("PASAJERO")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(sql)).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true);

            when(rs.getLong("ID_PASAJERO")).thenReturn(2L);
            when(rs.getDate("FECHA_NACIMIENTO")).thenReturn(null); 
            when(rs.getLong("ID_PAIS_DOCUMENTO")).thenReturn(0L);
            when(rs.wasNull()).thenReturn(true); 
            when(rs.getString("PASAPORTE")).thenReturn("P999");
            when(rs.getLong("ID_USUARIO")).thenReturn(idUsuario);

            PasajeroDAO dao = new PasajeroDAO();
            Pasajero p = dao.findByUsuario(idUsuario);

            assertNotNull(p);
            assertNull(p.getFechaNacimiento());
            assertNull(p.getIdPaisDocumento());
        }
    }

    @Test
    @DisplayName("upsert inserta cuando no existe pasajero y usa setDate/setLong")
    void upsert_insert_conValores() throws Exception {
        long idUsuario = 40L;
        LocalDate fecha = LocalDate.of(2000, 5, 10);
        Long idPais = 100L;
        String pasaporte = "PX123";

        Pasajero resultado = new Pasajero();
        resultado.setIdUsuario(idUsuario);

        PasajeroDAO realDao = new PasajeroDAO();
        PasajeroDAO dao = spy(realDao);
   
        doReturn(null, resultado).when(dao).findByUsuario(idUsuario);

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        String insSql = "INSERT INTO " + TABLA +
                " (FECHA_NACIMIENTO, ID_PAIS_DOCUMENTO, PASAPORTE, ID_USUARIO) VALUES (?, ?, ?, ?)";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("PASAJERO")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(insSql)).thenReturn(ps);

            Pasajero res = dao.upsert(idUsuario, fecha, idPais, pasaporte);

            assertSame(resultado, res);

            verify(ps).setDate(1, Date.valueOf(fecha));
            verify(ps).setLong(2, idPais);
            verify(ps).setString(3, pasaporte);
            verify(ps).setLong(4, idUsuario);
            verify(ps).executeUpdate();
        }
    }

    @Test
    @DisplayName("upsert inserta usando setNull cuando fecha e idPais son null")
    void upsert_insert_conNulls() throws Exception {
        long idUsuario = 41L;
        String pasaporte = "NOPAS";

        Pasajero resultado = new Pasajero();
        resultado.setIdUsuario(idUsuario);

        PasajeroDAO realDao = new PasajeroDAO();
        PasajeroDAO dao = spy(realDao);
        
        doReturn(null, resultado).when(dao).findByUsuario(idUsuario);

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        String insSql = "INSERT INTO " + TABLA +
                " (FECHA_NACIMIENTO, ID_PAIS_DOCUMENTO, PASAPORTE, ID_USUARIO) VALUES (?, ?, ?, ?)";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("PASAJERO")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(insSql)).thenReturn(ps);

            Pasajero res = dao.upsert(idUsuario, null, null, pasaporte);

            assertSame(resultado, res);

            verify(ps).setNull(1, Types.DATE);
            verify(ps).setNull(2, Types.NUMERIC);
            verify(ps).setString(3, pasaporte);
            verify(ps).setLong(4, idUsuario);
            verify(ps).executeUpdate();
        }
    }

    @Test
    @DisplayName("upsert actualiza cuando ya existe pasajero y usa setNull para fecha y país")
    void upsert_update_conNulls() throws Exception {
        long idUsuario = 50L;
        String nuevoPasaporte = "NEWP";

        Pasajero existente = new Pasajero();
        existente.setIdUsuario(idUsuario);

        Pasajero actualizado = new Pasajero();
        actualizado.setIdUsuario(idUsuario);
        actualizado.setPasaporte(nuevoPasaporte);

        PasajeroDAO realDao = new PasajeroDAO();
        PasajeroDAO dao = spy(realDao);
   
        doReturn(existente, actualizado).when(dao).findByUsuario(idUsuario);

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        String updSql = "UPDATE " + TABLA +
                " SET FECHA_NACIMIENTO=?, ID_PAIS_DOCUMENTO=?, PASAPORTE=? WHERE ID_USUARIO=?";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("PASAJERO")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(updSql)).thenReturn(ps);

            Pasajero res = dao.upsert(idUsuario, null, null, nuevoPasaporte);

            assertSame(actualizado, res);

            verify(ps).setNull(1, Types.DATE);
            verify(ps).setNull(2, Types.NUMERIC);
            verify(ps).setString(3, nuevoPasaporte);
            verify(ps).setLong(4, idUsuario);
            verify(ps).executeUpdate();
        }
    }

    @Test
    @DisplayName("upsert actualiza usando setDate/setLong cuando fecha e idPais no son null")
    void upsert_update_conValores() throws Exception {
        long idUsuario = 51L;
        LocalDate fecha = LocalDate.of(1995, 3, 15);
        Long idPais = 502L;
        String pasaporte = "UPD123";

        Pasajero existente = new Pasajero();
        existente.setIdUsuario(idUsuario);

        Pasajero actualizado = new Pasajero();
        actualizado.setIdUsuario(idUsuario);
        actualizado.setPasaporte(pasaporte);

        PasajeroDAO realDao = new PasajeroDAO();
        PasajeroDAO dao = spy(realDao);

        
        doReturn(existente, actualizado).when(dao).findByUsuario(idUsuario);

        Connection cn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        String updSql = "UPDATE " + TABLA +
                " SET FECHA_NACIMIENTO=?, ID_PAIS_DOCUMENTO=?, PASAPORTE=? WHERE ID_USUARIO=?";

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table("PASAJERO")).thenReturn(TABLA);
            dbMock.when(DB::getConnection).thenReturn(cn);

            when(cn.prepareStatement(updSql)).thenReturn(ps);

            Pasajero res = dao.upsert(idUsuario, fecha, idPais, pasaporte);

            assertSame(actualizado, res);

            verify(ps).setDate(1, Date.valueOf(fecha));
            verify(ps).setLong(2, idPais);
            verify(ps).setString(3, pasaporte);
            verify(ps).setLong(4, idUsuario);
            verify(ps).executeUpdate();
        }
    }
}
