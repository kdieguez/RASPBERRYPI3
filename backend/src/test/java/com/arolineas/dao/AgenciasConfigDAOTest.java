package com.aerolineas.dao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AgenciasConfigDAOTest {

@Test
@DisplayName("listar(true) debe devolver solo agencias mapeadas correctamente")
void listarSoloHabilitadas_mapeaCorrectamente() throws Exception {
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);

    // Dos filas y luego false
    when(rs.next()).thenReturn(true, true, false);

    when(rs.getString("ID_AGENCIA")).thenReturn("AG001", "AG002");
    when(rs.getString("NOMBRE")).thenReturn("Agencia Uno", "Agencia Dos");
    when(rs.getString("API_URL"))
            .thenReturn("https://uno.test/api", "https://dos.test/api");

    when(rs.getObject("ID_USUARIO_WS")).thenReturn(100L, null);
    when(rs.getLong("ID_USUARIO_WS")).thenReturn(100L, 0L);

    when(rs.getInt("HABILITADO")).thenReturn(1, 1);

    Timestamp creado1 = Timestamp.from(Instant.parse("2024-01-01T10:00:00Z"));
    Timestamp creado2 = Timestamp.from(Instant.parse("2024-01-03T09:00:00Z"));
    Timestamp actualizado1 = Timestamp.from(Instant.parse("2024-01-02T12:00:00Z"));
    Timestamp actualizado2 = Timestamp.from(Instant.parse("2024-01-04T15:30:00Z"));

    // Solo nos importa que se mapeen, no el valor exacto
    when(rs.getTimestamp("CREADO_EN")).thenReturn(creado1, creado2);
    when(rs.getTimestamp("ACTUALIZADO_EN")).thenReturn(actualizado1, actualizado2);

    AgenciasConfigDAO dao = new AgenciasConfigDAO(conn);

    List<Map<String, Object>> agencias = dao.listar(true);

    assertNotNull(agencias);
    assertEquals(2, agencias.size(), "Debe devolver 2 agencias");

    Map<String, Object> a1 = agencias.get(0);
    assertEquals("AG001", a1.get("idAgencia"));
    assertEquals("Agencia Uno", a1.get("nombre"));
    assertEquals("https://uno.test/api", a1.get("apiUrl"));
    assertEquals(100L, a1.get("idUsuarioWs"));
    assertEquals(true, a1.get("habilitado"));
    assertNotNull(a1.get("creadoEn"));
    assertTrue(a1.get("creadoEn") instanceof String);
    assertNotNull(a1.get("actualizadoEn"));
    assertTrue(a1.get("actualizadoEn") instanceof String);

    Map<String, Object> a2 = agencias.get(1);
    assertEquals("AG002", a2.get("idAgencia"));
    assertEquals("Agencia Dos", a2.get("nombre"));
    assertEquals("https://dos.test/api", a2.get("apiUrl"));
    assertNull(a2.get("idUsuarioWs"));
    assertEquals(true, a2.get("habilitado"));
    assertNotNull(a2.get("creadoEn"));
    assertTrue(a2.get("creadoEn") instanceof String);
    assertNotNull(a2.get("actualizadoEn"));
    assertTrue(a2.get("actualizadoEn") instanceof String);
}


    @Test
    @DisplayName("listar(false) debe usar el mismo mapeo sin filtrar por habilitado")
    void listarTodas_mapeaCorrectamente() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        when(rs.next()).thenReturn(true, false);

        when(rs.getString("ID_AGENCIA")).thenReturn("AGX");
        when(rs.getString("NOMBRE")).thenReturn("Agencia X");
        when(rs.getString("API_URL")).thenReturn("https://x.test/api");
        when(rs.getObject("ID_USUARIO_WS")).thenReturn(null);
        when(rs.getInt("HABILITADO")).thenReturn(0);
        when(rs.getTimestamp("CREADO_EN")).thenReturn(null);
        when(rs.getTimestamp("ACTUALIZADO_EN")).thenReturn(null);

        AgenciasConfigDAO dao = new AgenciasConfigDAO(conn);

        List<Map<String, Object>> agencias = dao.listar(false);

        assertEquals(1, agencias.size());
        Map<String, Object> a = agencias.get(0);
        assertEquals("AGX", a.get("idAgencia"));
        assertEquals("Agencia X", a.get("nombre"));
        assertEquals("https://x.test/api", a.get("apiUrl"));
        assertNull(a.get("idUsuarioWs"));
        assertFalse((Boolean) a.get("habilitado"));
        assertFalse(a.containsKey("creadoEn"));
        assertFalse(a.containsKey("actualizadoEn"));
    }

    @Test
    @DisplayName("obtener(id) debe devolver la agencia cuando existe y sin ACTUALIZADO_EN")
    void obtener_agenciaExistente_sinActualizadoEn() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        when(rs.next()).thenReturn(true);

        when(rs.getString("ID_AGENCIA")).thenReturn("AG999");
        when(rs.getString("NOMBRE")).thenReturn("Agencia Test");
        when(rs.getString("API_URL")).thenReturn("https://test.agencia/api");
        when(rs.getObject("ID_USUARIO_WS")).thenReturn(200L);
        when(rs.getLong("ID_USUARIO_WS")).thenReturn(200L);
        when(rs.getInt("HABILITADO")).thenReturn(1);

        Timestamp creado = Timestamp.from(Instant.parse("2024-05-01T08:30:00Z"));
        when(rs.getTimestamp("CREADO_EN")).thenReturn(creado);
        when(rs.getTimestamp("ACTUALIZADO_EN")).thenReturn(null);

        AgenciasConfigDAO dao = new AgenciasConfigDAO(conn);

        Map<String, Object> agencia = dao.obtener("AG999");

        assertNotNull(agencia);
        assertEquals("AG999", agencia.get("idAgencia"));
        assertEquals("Agencia Test", agencia.get("nombre"));
        assertEquals("https://test.agencia/api", agencia.get("apiUrl"));
        assertEquals(200L, agencia.get("idUsuarioWs"));
        assertEquals(true, agencia.get("habilitado"));
        assertEquals("2024-05-01T08:30:00Z", agencia.get("creadoEn"));
        assertFalse(agencia.containsKey("actualizadoEn"));
    }

    @Test
    @DisplayName("obtener(id) debe mapear también ACTUALIZADO_EN cuando viene con valor")
    void obtener_agenciaExistente_conActualizadoEn() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        when(rs.next()).thenReturn(true);

        when(rs.getString("ID_AGENCIA")).thenReturn("AG100");
        when(rs.getString("NOMBRE")).thenReturn("Agencia Con Update");
        when(rs.getString("API_URL")).thenReturn("https://update.agencia/api");
        when(rs.getObject("ID_USUARIO_WS")).thenReturn(null);
        when(rs.getInt("HABILITADO")).thenReturn(1);

        Timestamp creado = Timestamp.from(Instant.parse("2024-06-01T09:00:00Z"));
        Timestamp actualizado = Timestamp.from(Instant.parse("2024-06-02T10:00:00Z"));
        when(rs.getTimestamp("CREADO_EN")).thenReturn(creado);
        when(rs.getTimestamp("ACTUALIZADO_EN")).thenReturn(actualizado);

        AgenciasConfigDAO dao = new AgenciasConfigDAO(conn);

        Map<String, Object> agencia = dao.obtener("AG100");

        assertNotNull(agencia);
        assertEquals("2024-06-01T09:00:00Z", agencia.get("creadoEn"));
        assertEquals("2024-06-02T10:00:00Z", agencia.get("actualizadoEn"));
    }

    @Test
    @DisplayName("obtener(id) devuelve null cuando la agencia no existe")
    void obtener_agenciaNoExistente_devuelveNull() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false); 

        AgenciasConfigDAO dao = new AgenciasConfigDAO(conn);

        Map<String, Object> agencia = dao.obtener("AG_NO_EXISTE");
        assertNull(agencia, "Debe devolver null cuando no se encuentra la agencia");
    }

    @Test
    @DisplayName("eliminar(id) debe lanzar SQLException cuando no se afecta ninguna fila")
    void eliminar_agenciaNoExistente_lanzaSQLException() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(0);

        AgenciasConfigDAO dao = new AgenciasConfigDAO(conn);

        SQLException ex = assertThrows(SQLException.class,
                () -> dao.eliminar("AG_NO_EXISTE"));

        assertTrue(ex.getMessage().contains("Agencia no encontrada"));
    }

    @Test
    @DisplayName("eliminar(id) debe ejecutarse correctamente cuando rows>0")
    void eliminar_agenciaExistente_ok() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        AgenciasConfigDAO dao = new AgenciasConfigDAO(conn);

        assertDoesNotThrow(() -> dao.eliminar("AG001"));
        verify(ps).setString(1, "AG001");
        verify(ps).executeUpdate();
    }

    @Test
    @DisplayName("actualizar(id) debe lanzar SQLException cuando no se actualiza ninguna fila")
    void actualizar_agenciaNoExistente_lanzaSQLException() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(0);

        AgenciasConfigDAO dao = new AgenciasConfigDAO(conn);

        SQLException ex = assertThrows(SQLException.class, () ->
                dao.actualizar(
                        "AG_NO_EXISTE",
                        "Nuevo Nombre",
                        "https://nueva.api",
                        123L,
                        true
                )
        );

        assertTrue(ex.getMessage().contains("Agencia no encontrada"));
    }

    @Test
    @DisplayName("actualizar(id) debe construir el SQL dinámico y actualizar cuando rows>0")
    void actualizar_agenciaExistente_ok() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);

        AgenciasConfigDAO dao = new AgenciasConfigDAO(conn);

        assertDoesNotThrow(() ->
                dao.actualizar(
                        "AG123",
                        "Nombre Nuevo",
                        "https://nueva.api",
                        77L,
                        true
                )
        );

        verify(ps).setString(1, "Nombre Nuevo");
        verify(ps).setString(2, "https://nueva.api");
        verify(ps).setLong(3, 77L);
        verify(ps).setInt(4, 1);
        verify(ps).setString(5, "AG123");
        verify(ps).executeUpdate();
    }

    @Test
    @DisplayName("crear() debe insertar con idUsuarioWs no nulo")
    void crear_conUsuarioWs_ok() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        when(conn.prepareStatement(anyString())).thenReturn(ps);

        AgenciasConfigDAO dao = new AgenciasConfigDAO(conn);

        assertDoesNotThrow(() ->
                dao.crear("AGC1", "Agencia C", "https://c.api", 999L)
        );

        verify(ps).setString(1, "AGC1");
        verify(ps).setString(2, "Agencia C");
        verify(ps).setString(3, "https://c.api");
        verify(ps).setLong(4, 999L);
        verify(ps).executeUpdate();
    }

    @Test
    @DisplayName("crear() debe insertar con idUsuarioWs nulo usando setNull")
    void crear_sinUsuarioWs_ok() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        when(conn.prepareStatement(anyString())).thenReturn(ps);

        AgenciasConfigDAO dao = new AgenciasConfigDAO(conn);

        assertDoesNotThrow(() ->
                dao.crear("AGC2", "Agencia Sin WS", "https://nowss.api", null)
        );

        verify(ps).setString(1, "AGC2");
        verify(ps).setString(2, "Agencia Sin WS");
        verify(ps).setString(3, "https://nowss.api");
        verify(ps).setNull(4, java.sql.Types.NUMERIC);
        verify(ps).executeUpdate();
    }

    @Test
    @DisplayName("obtenerPorUsuarioWs(idUsuarioWs) debe mapear lista de agencias habilitadas")
    void obtenerPorUsuarioWs_mapeaCorrectamente() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        when(rs.next()).thenReturn(true, true, false);

        when(rs.getString("ID_AGENCIA")).thenReturn("AG1", "AG2");
        when(rs.getString("NOMBRE")).thenReturn("Agencia 1", "Agencia 2");
        when(rs.getString("API_URL")).thenReturn("https://a1.api", "https://a2.api");
        when(rs.getLong("ID_USUARIO_WS")).thenReturn(10L, 10L);
        when(rs.getInt("HABILITADO")).thenReturn(1, 1);

        AgenciasConfigDAO dao = new AgenciasConfigDAO(conn);

        List<Map<String, Object>> out = dao.obtenerPorUsuarioWs(10L);

        assertEquals(2, out.size());
        assertEquals("AG1", out.get(0).get("idAgencia"));
        assertEquals("Agencia 1", out.get(0).get("nombre"));
        assertEquals("https://a1.api", out.get(0).get("apiUrl"));
        assertEquals(10L, out.get(0).get("idUsuarioWs"));
        assertEquals(true, out.get(0).get("habilitado"));
    }
}
