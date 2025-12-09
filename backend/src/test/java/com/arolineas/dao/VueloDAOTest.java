package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.dto.VueloDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.mockito.InOrder;

import static com.aerolineas.util.EstadosVuelo.CANCELADO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class VueloDAOTest {

    private Timestamp ts(LocalDateTime dt) {
        return Timestamp.valueOf(dt);
    }   

    @Test
    @DisplayName("listarVuelosPublic solo devuelve vuelos con cupos disponibles (>0)")
    void listarVuelosPublic_devuelveSoloVuelosConCupo() throws Exception {
        
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockPs = mock(PreparedStatement.class);
        ResultSet mockRs = mock(ResultSet.class);        
        LocalDateTime salida = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime llegada = LocalDateTime.of(2025, 1, 1, 12, 0);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            
            dbMock.when(() -> DB.table(anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, String.class));
            
            dbMock.when(DB::getConnection).thenReturn(mockConn);

            when(mockConn.prepareStatement(anyString())).thenReturn(mockPs);
            when(mockPs.executeQuery()).thenReturn(mockRs);
            
            when(mockRs.next())
                    .thenReturn(true)   
                    .thenReturn(true)   
                    .thenReturn(false); 

            when(mockRs.getInt("DISPONIBLE"))
                    .thenReturn(5)
                    .thenReturn(0);

            when(mockRs.wasNull()).thenReturn(false);
            
            when(mockRs.getLong("ID_VUELO")).thenReturn(1L);
            when(mockRs.getString("CODIGO")).thenReturn("AV123");
            when(mockRs.getLong("ID_RUTA")).thenReturn(10L);
            when(mockRs.getString("ORIGEN")).thenReturn("Ciudad A");
            when(mockRs.getString("DESTINO")).thenReturn("Ciudad B");
            when(mockRs.getString("ORIGEN_PAIS")).thenReturn("Pais A");
            when(mockRs.getString("DESTINO_PAIS")).thenReturn("Pais B");
            when(mockRs.getTimestamp("FECHA_SALIDA")).thenReturn(ts(salida));
            when(mockRs.getTimestamp("FECHA_LLEGADA")).thenReturn(ts(llegada));
            when(mockRs.getInt("ACTIVO")).thenReturn(1);
            when(mockRs.getObject("ID_ESTADO")).thenReturn(1);
            when(mockRs.getString("ESTADO")).thenReturn("ACTIVO");
            when(mockRs.getObject("PAREJA")).thenReturn(null);
            
            when(mockRs.getInt("ID_CLASE")).thenReturn(1);
            when(mockRs.getInt("CUPO_TOTAL")).thenReturn(100);
            when(mockRs.getDouble("PRECIO")).thenReturn(500.0);

            VueloDAO dao = new VueloDAO();

            List<VueloDTO.View> resultado = dao.listarVuelosPublic();

            assertEquals(1, resultado.size(), "Debe devolver solo vuelos con cupo > 0");

            VueloDTO.View vuelo = resultado.get(0);
            assertEquals(1L, vuelo.idVuelo());
            assertEquals("AV123", vuelo.codigo());
            assertEquals("Ciudad A", vuelo.origen());
            assertEquals("Ciudad B", vuelo.destino());
            assertEquals(salida, vuelo.fechaSalida());
            assertEquals(llegada, vuelo.fechaLlegada());
            assertTrue(vuelo.activo());
            assertEquals("ACTIVO", vuelo.estado());

            assertNotNull(vuelo.clases());
            assertEquals(1, vuelo.clases().size());
            VueloDTO.ClaseConfig clase = vuelo.clases().get(0);
            assertEquals(1, clase.idClase());
            assertEquals(100, clase.cupoTotal());
            assertEquals(500.0, clase.precio());
        }
    } 

    @Test
    @DisplayName("obtenerVueloPublic devuelve null si no hay cupo disponible")
    void obtenerVueloPublic_sinCupoDevuelveNull() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockPs = mock(PreparedStatement.class);
        ResultSet mockRs = mock(ResultSet.class);

        LocalDateTime salida = LocalDateTime.of(2025, 1, 2, 8, 0);
        LocalDateTime llegada = LocalDateTime.of(2025, 1, 2, 10, 0);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

            dbMock.when(() -> DB.table(anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, String.class));
            dbMock.when(DB::getConnection).thenReturn(mockConn);

            when(mockConn.prepareStatement(anyString())).thenReturn(mockPs);
            when(mockPs.executeQuery()).thenReturn(mockRs);

            when(mockRs.next())
                    .thenReturn(true)
                    .thenReturn(false);

            
            when(mockRs.getInt("DISPONIBLE")).thenReturn(0);
            when(mockRs.wasNull()).thenReturn(false);

            when(mockRs.getLong("ID_VUELO")).thenReturn(99L);
            when(mockRs.getString("CODIGO")).thenReturn("NOIMPORTA");
            when(mockRs.getLong("ID_RUTA")).thenReturn(999L);
            when(mockRs.getTimestamp("FECHA_SALIDA")).thenReturn(ts(salida));
            when(mockRs.getTimestamp("FECHA_LLEGADA")).thenReturn(ts(llegada));

            VueloDAO dao = new VueloDAO();

            VueloDTO.View view = dao.obtenerVueloPublic(99L);

            assertNull(view, "Si no hay cupo DISPONIBLE>0, obtenerVueloPublic debe devolver null");
        }
    }

    @Test
    @DisplayName("obtenerVueloPublic devuelve vuelo cuando hay cupo disponible")
    void obtenerVueloPublic_conCupoDevuelveVuelo() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockPs = mock(PreparedStatement.class);
        ResultSet mockRs = mock(ResultSet.class);

        LocalDateTime salida = LocalDateTime.of(2025, 1, 3, 6, 0);
        LocalDateTime llegada = LocalDateTime.of(2025, 1, 3, 9, 0);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

            dbMock.when(() -> DB.table(anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, String.class));
            dbMock.when(DB::getConnection).thenReturn(mockConn);

            when(mockConn.prepareStatement(anyString())).thenReturn(mockPs);
            when(mockPs.executeQuery()).thenReturn(mockRs);

            when(mockRs.next())
                    .thenReturn(true)
                    .thenReturn(false);

            when(mockRs.getInt("DISPONIBLE")).thenReturn(10);
            when(mockRs.wasNull()).thenReturn(false);

            when(mockRs.getLong("ID_VUELO")).thenReturn(7L);
            when(mockRs.getString("CODIGO")).thenReturn("AV777");
            when(mockRs.getLong("ID_RUTA")).thenReturn(70L);
            when(mockRs.getString("ORIGEN")).thenReturn("Ciudad X");
            when(mockRs.getString("DESTINO")).thenReturn("Ciudad Y");
            when(mockRs.getString("ORIGEN_PAIS")).thenReturn("Pais X");
            when(mockRs.getString("DESTINO_PAIS")).thenReturn("Pais Y");
            when(mockRs.getTimestamp("FECHA_SALIDA")).thenReturn(ts(salida));
            when(mockRs.getTimestamp("FECHA_LLEGADA")).thenReturn(ts(llegada));
            when(mockRs.getInt("ACTIVO")).thenReturn(1);
            when(mockRs.getObject("ID_ESTADO")).thenReturn(2);
            when(mockRs.getString("ESTADO")).thenReturn("PROGRAMADO");
            when(mockRs.getObject("PAREJA_ID")).thenReturn(null);

            when(mockRs.getInt("ID_CLASE")).thenReturn(2);
            when(mockRs.getInt("CUPO_TOTAL")).thenReturn(50);
            when(mockRs.getDouble("PRECIO")).thenReturn(750.0);

            VueloDAO dao = new VueloDAO();

            VueloDTO.View view = dao.obtenerVueloPublic(7L);

            assertNotNull(view, "Debe devolver un vuelo cuando hay cupo disponible");
            assertEquals(7L, view.idVuelo());
            assertEquals("AV777", view.codigo());
            assertEquals("Ciudad X", view.origen());
            assertEquals("Ciudad Y", view.destino());
            assertEquals(salida, view.fechaSalida());
            assertEquals(llegada, view.fechaLlegada());
            assertEquals("PROGRAMADO", view.estado());
            assertEquals("Pais X", view.origenPais());
            assertEquals("Pais Y", view.destinoPais());

            assertNotNull(view.clases());
            assertEquals(1, view.clases().size());
            VueloDTO.ClaseConfig c = view.clases().get(0);
            assertEquals(2, c.idClase());
            assertEquals(50, c.cupoTotal());
            assertEquals(750.0, c.precio());
        }
    }

    @Test
    @DisplayName("listarVuelos agrupa clases por ID_VUELO")
    void listarVuelos_agrupaClasesPorVuelo() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockPs = mock(PreparedStatement.class);
        ResultSet mockRs = mock(ResultSet.class);

        LocalDateTime salida = LocalDateTime.of(2025, 2, 1, 10, 0);
        LocalDateTime llegada = LocalDateTime.of(2025, 2, 1, 12, 0);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

            dbMock.when(() -> DB.table(anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, String.class));
            dbMock.when(DB::getConnection).thenReturn(mockConn);

            when(mockConn.prepareStatement(anyString())).thenReturn(mockPs);
            when(mockPs.executeQuery()).thenReturn(mockRs);
            
            when(mockRs.next())
                    .thenReturn(true)   
                    .thenReturn(true)   
                    .thenReturn(false);

            when(mockRs.getLong("ID_VUELO")).thenReturn(1L);
            when(mockRs.getString("CODIGO")).thenReturn("COD1");
            when(mockRs.getLong("ID_RUTA")).thenReturn(10L);
            when(mockRs.getString("ORIGEN")).thenReturn("Origen");
            when(mockRs.getString("DESTINO")).thenReturn("Destino");
            when(mockRs.getString("ORIGEN_PAIS")).thenReturn("PaisO");
            when(mockRs.getString("DESTINO_PAIS")).thenReturn("PaisD");
            when(mockRs.getTimestamp("FECHA_SALIDA")).thenReturn(ts(salida));
            when(mockRs.getTimestamp("FECHA_LLEGADA")).thenReturn(ts(llegada));
            when(mockRs.getInt("ACTIVO")).thenReturn(1);
            when(mockRs.getObject("ID_ESTADO")).thenReturn(1);
            when(mockRs.getString("ESTADO")).thenReturn("ACTIVO");
            when(mockRs.getObject("PAREJA")).thenReturn(null);
            
            when(mockRs.getInt("ID_CLASE"))
                    .thenReturn(1)
                    .thenReturn(2);
            when(mockRs.getInt("CUPO_TOTAL"))
                    .thenReturn(100)
                    .thenReturn(200);
            when(mockRs.getDouble("PRECIO"))
                    .thenReturn(500.0)
                    .thenReturn(800.0);

            VueloDAO dao = new VueloDAO();

            List<VueloDTO.View> lista = dao.listarVuelos(false);

            assertEquals(1, lista.size(), "Debe haber solo un vuelo");
            VueloDTO.View v = lista.get(0);

            assertEquals(2, v.clases().size(), "Debe agrupar las dos clases en el mismo vuelo");
            assertEquals(1, v.clases().get(0).idClase());
            assertEquals(2, v.clases().get(1).idClase());
        }
    }

    @Test
    @DisplayName("crearVueloReturnId inserta vuelo y clases y devuelve ID generado")
    void crearVueloReturnId_insertaVueloYClases() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement psInsert = mock(PreparedStatement.class);
        PreparedStatement psClase = mock(PreparedStatement.class);
        ResultSet rsKeys = mock(ResultSet.class);

        LocalDateTime salida = LocalDateTime.of(2025, 3, 1, 14, 0);
        LocalDateTime llegada = LocalDateTime.of(2025, 3, 1, 16, 0);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

            dbMock.when(() -> DB.table(anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, String.class));
            dbMock.when(DB::getConnection).thenReturn(mockConn);

            
            when(mockConn.prepareStatement(anyString(), any(String[].class)))
                    .thenReturn(psInsert);

            
            when(mockConn.prepareStatement(contains("SALIDA_CLASE")))
                    .thenReturn(psClase);

            when(psInsert.getGeneratedKeys()).thenReturn(rsKeys);
            when(rsKeys.next()).thenReturn(true);
            when(rsKeys.getLong(1)).thenReturn(123L);

            VueloDAO dao = new VueloDAO();

            VueloDTO.Create dto = new VueloDTO.Create(
                    "CODTEST",
                    10L,
                    salida,
                    llegada,
                    List.of(new VueloDTO.ClaseConfig(1, 100, 500.0)),
                    null,
                    true
            );

            long idGenerado = dao.crearVueloReturnId(dto);

            assertEquals(123L, idGenerado, "Debe devolver el ID generado por la BD (mockeada)");

            verify(psInsert, times(1)).executeUpdate();
            verify(psClase, times(1)).executeUpdate();
            verify(mockConn, times(1)).commit();
        }
    }
    
    @Test
    @DisplayName("actualizarEstado lanza SQLException si estado no es válido")
    void actualizarEstado_estadoInvalidoLanzaExcepcion() {
        VueloDAO dao = new VueloDAO();
        assertThrows(SQLException.class,
                () -> dao.actualizarEstado(1L, -999, "motivo"),
                "Debe lanzar SQLException para estados no válidos");
    }

    @Test
    @DisplayName("actualizarEstado cambia a CANCELADO e inserta motivo")
    void actualizarEstado_cancelaVueloInsertaMotivo() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement psSel = mock(PreparedStatement.class);
        PreparedStatement psUpd = mock(PreparedStatement.class);
        PreparedStatement psMotivo = mock(PreparedStatement.class);
        ResultSet rsSel = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

            dbMock.when(() -> DB.table(anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, String.class));
            dbMock.when(DB::getConnection).thenReturn(mockConn);

            
            when(mockConn.prepareStatement(anyString()))
                    .thenReturn(psSel, psUpd, psMotivo);

            when(psSel.executeQuery()).thenReturn(rsSel);
            when(rsSel.next()).thenReturn(true);
            
            when(rsSel.getInt(1)).thenReturn(1);

            VueloDAO dao = new VueloDAO();

            assertDoesNotThrow(() ->
                    dao.actualizarEstado(10L, CANCELADO, "Cancelado por clima"));

            verify(psUpd, times(1)).executeUpdate();
            verify(psMotivo, times(1)).executeUpdate();
            verify(mockConn, times(1)).commit();
        }
    }

    @Test
    @DisplayName("vincularPareja lanza error si IDs inválidos o iguales")
    void vincularPareja_precondiciones() {
        VueloDAO dao = new VueloDAO();

        assertThrows(SQLException.class,
                () -> dao.vincularPareja(0, 2),
                "ID ida inválido debe lanzar excepción");

        assertThrows(SQLException.class,
                () -> dao.vincularPareja(1, 0),
                "ID regreso inválido debe lanzar excepción");

        assertThrows(SQLException.class,
                () -> dao.vincularPareja(5, 5),
                "Un vuelo no puede ser pareja de sí mismo");
    }

    @Test
    @DisplayName("validarCompatibilidadEscala no permite usar el mismo vuelo dos veces")
    void validarCompatibilidadEscala_mismoVueloLanzaExcepcion() {
        VueloDAO dao = new VueloDAO();
        assertThrows(SQLException.class,
                () -> dao.validarCompatibilidadEscala(mock(Connection.class), 3L, 3L),
                "Un vuelo no puede ser escala de sí mismo");
    }

        @Test
    @DisplayName("actualizarEstado lanza error si el vuelo no existe")
    void actualizarEstado_vueloNoExiste() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement psSel = mock(PreparedStatement.class);
        ResultSet rsSel = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table(anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, String.class));
            dbMock.when(DB::getConnection).thenReturn(mockConn);

            when(mockConn.prepareStatement(anyString())).thenReturn(psSel);
            when(psSel.executeQuery()).thenReturn(rsSel);
            
            when(rsSel.next()).thenReturn(false);

            VueloDAO dao = new VueloDAO();

            SQLException ex = assertThrows(SQLException.class,
                    () -> dao.actualizarEstado(999L, CANCELADO, "motivo X"),
                    "Debe fallar si el vuelo no existe");

            assertTrue(ex.getMessage().contains("Vuelo no existe"),
                    "El mensaje debe indicar que el vuelo no existe");
            verify(mockConn, times(1)).rollback();
        }
    }
    @Test
    @DisplayName("actualizarEstado no permite cambiar desde CANCELADO a otro estado")
    void actualizarEstado_desdeCanceladoNoPermiteCambio() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement psSel = mock(PreparedStatement.class);
        ResultSet rsSel = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table(anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, String.class));
            dbMock.when(DB::getConnection).thenReturn(mockConn);

            when(mockConn.prepareStatement(anyString())).thenReturn(psSel);
            when(psSel.executeQuery()).thenReturn(rsSel);
            when(rsSel.next()).thenReturn(true);
            
            when(rsSel.getInt(1)).thenReturn(CANCELADO);

            VueloDAO dao = new VueloDAO();

            SQLException ex = assertThrows(SQLException.class,
                    () -> dao.actualizarEstado(10L, 1, "intento de cambio"),
                    "No debe permitir cambiar estado si ya está cancelado");

            assertTrue(ex.getMessage().contains("ya está cancelado"),
                    "El mensaje debe indicar que ya está cancelado");
            verify(mockConn, times(1)).rollback();
        }
    }
    @Test
    @DisplayName("actualizarVueloAdmin actualiza vuelo, clases y motivo de cambio")
    void actualizarVueloAdmin_actualizaCorrectamente() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement psChk = mock(PreparedStatement.class);
        PreparedStatement psUpdate = mock(PreparedStatement.class);
        PreparedStatement psDelClase = mock(PreparedStatement.class);
        PreparedStatement psInsClase = mock(PreparedStatement.class);
        PreparedStatement psInsMotivo = mock(PreparedStatement.class);
        ResultSet rsChk = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table(anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, String.class));
            dbMock.when(DB::getConnection).thenReturn(mockConn);

            
            
            
            
            
            
            when(mockConn.prepareStatement(anyString()))
                    .thenReturn(psChk, psUpdate, psDelClase, psInsClase, psInsMotivo);

            when(psChk.executeQuery()).thenReturn(rsChk);
            when(rsChk.next()).thenReturn(true);
            
            when(rsChk.getInt(1)).thenReturn(1);

            
            when(psUpdate.executeUpdate()).thenReturn(1);

            VueloDAO dao = new VueloDAO();

            VueloDTO.UpdateAdmin dto = new VueloDTO.UpdateAdmin(
                    "COD-NEW",
                    20L,
                    LocalDateTime.of(2025, 4, 1, 10, 0),
                    LocalDateTime.of(2025, 4, 1, 12, 0),
                    Boolean.TRUE,
                    List.of(new VueloDTO.ClaseConfig(1, 120, 600.0)),
                    null,
                    "Cambio de horario"
            );

            assertDoesNotThrow(() -> dao.actualizarVueloAdmin(5L, dto));

            verify(psUpdate, times(1)).executeUpdate();
            verify(psDelClase, times(1)).executeUpdate();
            verify(psInsClase, atLeastOnce()).addBatch();
            verify(psInsClase, times(1)).executeBatch();
            verify(psInsMotivo, times(1)).executeUpdate();
            verify(mockConn, times(1)).commit();
        }
    }
    @Test
    @DisplayName("crearVueloConEscala inserta vuelo con escala y clases")
    void crearVueloConEscala_insertaCorrectamente() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement psInsert = mock(PreparedStatement.class);
        PreparedStatement psClases = mock(PreparedStatement.class);
        ResultSet rsKeys = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {
            dbMock.when(() -> DB.table(anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, String.class));
            dbMock.when(DB::getConnection).thenReturn(mockConn);

            
            when(mockConn.prepareStatement(startsWith("INSERT INTO VUELO_CON_ESCALA "), any(String[].class)))
                    .thenReturn(psInsert);

            
            when(mockConn.prepareStatement(startsWith("INSERT INTO VUELO_CON_ESCALA_CLASE")))
                    .thenReturn(psClases);

            when(psInsert.getGeneratedKeys()).thenReturn(rsKeys);
            when(rsKeys.next()).thenReturn(true);
            when(rsKeys.getLong(1)).thenReturn(555L);

            VueloDAO realDao = new VueloDAO();
            VueloDAO dao = spy(realDao);

            
            doNothing().when(dao).validarCompatibilidadEscala(eq(mockConn), eq(1L), eq(2L));

            VueloDTO.VueloConEscalaCreate dto = new VueloDTO.VueloConEscalaCreate(
                    "ESC555",
                    1L,
                    2L,
                    List.of(new VueloDTO.ClaseConfig(1, 80, 900.0)),
                    true
            );

            long id = dao.crearVueloConEscala(dto);

            assertEquals(555L, id, "Debe devolver el ID generado para el vuelo con escala");
            verify(dao, times(1)).validarCompatibilidadEscala(mockConn, 1L, 2L);
            verify(psInsert, times(1)).executeUpdate();
            verify(psClases, atLeastOnce()).addBatch();
            verify(psClases, times(1)).executeBatch();
            verify(mockConn, times(1)).commit();
        }
    }

        @Test
    @DisplayName("obtenerVuelo devuelve los datos completos del vuelo con clases")
    void obtenerVuelo_devuelveVueloConClases() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockPs = mock(PreparedStatement.class);
        ResultSet mockRs = mock(ResultSet.class);

        LocalDateTime salida = LocalDateTime.of(2025, 5, 1, 10, 0);
        LocalDateTime llegada = LocalDateTime.of(2025, 5, 1, 12, 0);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

            dbMock.when(() -> DB.table(anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, String.class));

            dbMock.when(DB::getConnection).thenReturn(mockConn);

            when(mockConn.prepareStatement(anyString())).thenReturn(mockPs);
            when(mockPs.executeQuery()).thenReturn(mockRs);
            
            when(mockRs.next())
                    .thenReturn(true)
                    .thenReturn(false);

            when(mockRs.getLong("ID_VUELO")).thenReturn(100L);
            when(mockRs.getString("CODIGO")).thenReturn("AV100");
            when(mockRs.getLong("ID_RUTA")).thenReturn(50L);
            when(mockRs.getString("ORIGEN")).thenReturn("Ciudad Origen");
            when(mockRs.getString("DESTINO")).thenReturn("Ciudad Destino");
            when(mockRs.getString("ORIGEN_PAIS")).thenReturn("Pais Origen");
            when(mockRs.getString("DESTINO_PAIS")).thenReturn("Pais Destino");
            when(mockRs.getTimestamp("FECHA_SALIDA")).thenReturn(ts(salida));
            when(mockRs.getTimestamp("FECHA_LLEGADA")).thenReturn(ts(llegada));
            when(mockRs.getInt("ACTIVO")).thenReturn(1);

            when(mockRs.getObject("ID_ESTADO")).thenReturn(3);
            when(mockRs.getInt("ID_ESTADO")).thenReturn(3);
            when(mockRs.getString("ESTADO")).thenReturn("EN HORARIO");

            when(mockRs.getObject("PAREJA_ID")).thenReturn(200L);
            when(mockRs.getLong("PAREJA_ID")).thenReturn(200L);
            when(mockRs.getString("PAREJA_CODIGO")).thenReturn("AV200");

            when(mockRs.getInt("ID_CLASE")).thenReturn(1);
            when(mockRs.wasNull()).thenReturn(false);
            when(mockRs.getInt("CUPO_TOTAL")).thenReturn(150);
            when(mockRs.getDouble("PRECIO")).thenReturn(350.0);

            VueloDAO dao = new VueloDAO();

            VueloDTO.View view = dao.obtenerVuelo(100L);

            assertNotNull(view, "Debe devolver un vuelo");
            assertEquals(100L, view.idVuelo());
            assertEquals("AV100", view.codigo());
            assertEquals(50L, view.idRuta());
            assertEquals("Ciudad Origen", view.origen());
            assertEquals("Ciudad Destino", view.destino());
            assertEquals(salida, view.fechaSalida());
            assertEquals(llegada, view.fechaLlegada());
            assertTrue(view.activo());
            assertEquals(3, view.idEstado());
            assertEquals("EN HORARIO", view.estado());
            assertEquals(200L, view.idVueloPareja());
            assertEquals("Pais Origen", view.origenPais());
            assertEquals("Pais Destino", view.destinoPais());

            assertNotNull(view.clases());
            assertEquals(1, view.clases().size());
            VueloDTO.ClaseConfig clase = view.clases().get(0);
            assertEquals(1, clase.idClase());
            assertEquals(150, clase.cupoTotal());
            assertEquals(350.0, clase.precio());

            
            assertNotNull(view.escalas());
            assertTrue(view.escalas().isEmpty());
        }
    }

    @Test
    @DisplayName("obtenerVueloAdmin devuelve los datos completos del vuelo para admin")
    void obtenerVueloAdmin_devuelveVueloAdminConClases() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockPs = mock(PreparedStatement.class);
        ResultSet mockRs = mock(ResultSet.class);

        LocalDateTime salida = LocalDateTime.of(2025, 6, 1, 8, 0);
        LocalDateTime llegada = LocalDateTime.of(2025, 6, 1, 11, 0);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

            dbMock.when(() -> DB.table(anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, String.class));
            dbMock.when(DB::getConnection).thenReturn(mockConn);

            when(mockConn.prepareStatement(anyString())).thenReturn(mockPs);
            when(mockPs.executeQuery()).thenReturn(mockRs);

            when(mockRs.next())
                    .thenReturn(true)
                    .thenReturn(false);

            when(mockRs.getLong("ID_VUELO")).thenReturn(300L);
            when(mockRs.getString("CODIGO")).thenReturn("ADM300");
            when(mockRs.getLong("ID_RUTA")).thenReturn(70L);
            when(mockRs.getString("ORIGEN")).thenReturn("AdminOrigen");
            when(mockRs.getString("DESTINO")).thenReturn("AdminDestino");
            when(mockRs.getString("ORIGEN_PAIS")).thenReturn("AdminPaisO");
            when(mockRs.getString("DESTINO_PAIS")).thenReturn("AdminPaisD");
            when(mockRs.getTimestamp("FECHA_SALIDA")).thenReturn(ts(salida));
            when(mockRs.getTimestamp("FECHA_LLEGADA")).thenReturn(ts(llegada));
            when(mockRs.getInt("ACTIVO")).thenReturn(1);
            when(mockRs.getObject("ID_ESTADO")).thenReturn(5);
            when(mockRs.getInt("ID_ESTADO")).thenReturn(5);
            when(mockRs.getString("ESTADO")).thenReturn("REPROGRAMADO");
            when(mockRs.getObject("PAREJA_ID")).thenReturn(400L);
            when(mockRs.getLong("PAREJA_ID")).thenReturn(400L);
            when(mockRs.getString("PAREJA_CODIGO")).thenReturn("ADM400");

            when(mockRs.getInt("ID_CLASE")).thenReturn(2);
            when(mockRs.wasNull()).thenReturn(false);
            when(mockRs.getInt("CUPO_TOTAL")).thenReturn(90);
            when(mockRs.getDouble("PRECIO")).thenReturn(999.0);

            VueloDAO dao = new VueloDAO();

            VueloDTO.ViewAdmin view = dao.obtenerVueloAdmin(300L);

            assertNotNull(view, "Debe devolver un vuelo admin");
            assertEquals(300L, view.idVuelo());
            assertEquals("ADM300", view.codigo());
            assertEquals("AdminOrigen", view.origen());
            assertEquals("AdminDestino", view.destino());
            assertEquals(salida, view.fechaSalida());
            assertEquals(llegada, view.fechaLlegada());
            assertTrue(view.activo());
            assertEquals(5, view.idEstado());
            assertEquals("REPROGRAMADO", view.estado());
            assertEquals(400L, view.idVueloPareja());
            assertEquals("ADM400", view.codigoPareja());
            assertEquals("AdminPaisO", view.origenPais());
            assertEquals("AdminPaisD", view.destinoPais());

            assertNotNull(view.clases());
            assertEquals(1, view.clases().size());
            VueloDTO.ClaseConfig c = view.clases().get(0);
            assertEquals(2, c.idClase());
            assertEquals(90, c.cupoTotal());
            assertEquals(999.0, c.precio());

            assertNotNull(view.escalas());
            assertTrue(view.escalas().isEmpty());
        }
    }

    @Test
    @DisplayName("listarVuelosConEscala agrupa clases por vuelo con escala")
    void listarVuelosConEscala_agrupaClases() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockPs = mock(PreparedStatement.class);
        ResultSet mockRs = mock(ResultSet.class);

        LocalDateTime salida1 = LocalDateTime.of(2025, 7, 1, 7, 0);
        LocalDateTime llegada1 = LocalDateTime.of(2025, 7, 1, 9, 0);
        LocalDateTime salida2 = LocalDateTime.of(2025, 7, 1, 10, 0);
        LocalDateTime llegada2 = LocalDateTime.of(2025, 7, 1, 13, 0);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

            dbMock.when(() -> DB.table(anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, String.class));
            dbMock.when(DB::getConnection).thenReturn(mockConn);

            when(mockConn.prepareStatement(anyString())).thenReturn(mockPs);
            when(mockPs.executeQuery()).thenReturn(mockRs);

            
            when(mockRs.next())
                    .thenReturn(true)
                    .thenReturn(true)
                    .thenReturn(false);

            when(mockRs.getLong("ID_VUELO_CON_ESCALA")).thenReturn(1000L);
            when(mockRs.getString("CODIGO")).thenReturn("ESC-1000");
            when(mockRs.getInt("ACTIVO")).thenReturn(1);

            
            when(mockRs.getLong("ID_VUELO1")).thenReturn(10L);
            when(mockRs.getString("CODIGO1")).thenReturn("TRAMO1");
            when(mockRs.getTimestamp("FECHA_SALIDA1")).thenReturn(ts(salida1));
            when(mockRs.getTimestamp("FECHA_LLEGADA1")).thenReturn(ts(llegada1));
            when(mockRs.getString("ORIGEN1")).thenReturn("Ciudad A");
            when(mockRs.getString("DESTINO1")).thenReturn("Ciudad B");
            when(mockRs.getString("ORIGEN_PAIS1")).thenReturn("Pais A");
            when(mockRs.getString("DESTINO_PAIS1")).thenReturn("Pais B");

            
            when(mockRs.getLong("ID_VUELO2")).thenReturn(20L);
            when(mockRs.getString("CODIGO2")).thenReturn("TRAMO2");
            when(mockRs.getTimestamp("FECHA_SALIDA2")).thenReturn(ts(salida2));
            when(mockRs.getTimestamp("FECHA_LLEGADA2")).thenReturn(ts(llegada2));
            when(mockRs.getString("ORIGEN2")).thenReturn("Ciudad B");
            when(mockRs.getString("DESTINO2")).thenReturn("Ciudad C");
            when(mockRs.getString("ORIGEN_PAIS2")).thenReturn("Pais B");
            when(mockRs.getString("DESTINO_PAIS2")).thenReturn("Pais C");

            when(mockRs.getString("ESTADO1")).thenReturn("ACTIVO");
            when(mockRs.getString("ESTADO2")).thenReturn("ACTIVO");

            
            when(mockRs.getInt("ID_CLASE"))
                    .thenReturn(1)
                    .thenReturn(2);
            when(mockRs.getInt("CUPO_TOTAL"))
                    .thenReturn(30)
                    .thenReturn(40);
            when(mockRs.getDouble("PRECIO"))
                    .thenReturn(1000.0)
                    .thenReturn(1500.0);

            VueloDAO dao = new VueloDAO();

            List<VueloDTO.VueloConEscalaView> lista = dao.listarVuelosConEscala();

            assertEquals(1, lista.size(), "Debe haber un solo vuelo con escala");
            VueloDTO.VueloConEscalaView view = lista.get(0);

            assertEquals(1000L, view.idVueloConEscala());
            assertEquals("ESC-1000", view.codigo());
            assertEquals("Ciudad A", view.origen());
            assertEquals("Ciudad C", view.destino());
            assertEquals(salida1, view.fechaSalida());
            assertEquals(llegada2, view.fechaLlegada());
            assertTrue(view.activo());
            assertEquals("ACTIVO", view.estado());

            
            assertEquals("Ciudad A", view.primerTramo().origen());
            assertEquals("Ciudad B", view.primerTramo().destino());
            
            assertEquals("Ciudad B", view.segundoTramo().origen());
            assertEquals("Ciudad C", view.segundoTramo().destino());

            
            assertNotNull(view.clases());
            assertEquals(2, view.clases().size());
            assertEquals(1, view.clases().get(0).idClase());
            assertEquals(2, view.clases().get(1).idClase());
        }
    }
    @Test
    @DisplayName("obtenerVueloConEscalaPublic devuelve un vuelo con escala y clases")
    void obtenerVueloConEscalaPublic_devuelveVueloConEscala() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockPs = mock(PreparedStatement.class);
        ResultSet mockRs = mock(ResultSet.class);

        LocalDateTime salida1 = LocalDateTime.of(2025, 8, 1, 6, 0);
        LocalDateTime llegada1 = LocalDateTime.of(2025, 8, 1, 8, 0);
        LocalDateTime salida2 = LocalDateTime.of(2025, 8, 1, 9, 0);
        LocalDateTime llegada2 = LocalDateTime.of(2025, 8, 1, 12, 0);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

            dbMock.when(() -> DB.table(anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, String.class));
            dbMock.when(DB::getConnection).thenReturn(mockConn);

            when(mockConn.prepareStatement(anyString())).thenReturn(mockPs);
            when(mockPs.executeQuery()).thenReturn(mockRs);

            when(mockRs.next())
                    .thenReturn(true)
                    .thenReturn(false);

            when(mockRs.getLong("ID_VUELO_CON_ESCALA")).thenReturn(2000L);
            when(mockRs.getString("CODIGO")).thenReturn("PUB2000");
            when(mockRs.getInt("ACTIVO")).thenReturn(1);

            
            when(mockRs.getLong("ID_VUELO1")).thenReturn(11L);
            when(mockRs.getString("CODIGO1")).thenReturn("PUBT1");
            when(mockRs.getTimestamp("FECHA_SALIDA1")).thenReturn(ts(salida1));
            when(mockRs.getTimestamp("FECHA_LLEGADA1")).thenReturn(ts(llegada1));
            when(mockRs.getString("ORIGEN1")).thenReturn("P1");
            when(mockRs.getString("DESTINO1")).thenReturn("P2");
            when(mockRs.getString("ORIGEN_PAIS1")).thenReturn("Pais1");
            when(mockRs.getString("DESTINO_PAIS1")).thenReturn("Pais2");

            
            when(mockRs.getLong("ID_VUELO2")).thenReturn(22L);
            when(mockRs.getString("CODIGO2")).thenReturn("PUBT2");
            when(mockRs.getTimestamp("FECHA_SALIDA2")).thenReturn(ts(salida2));
            when(mockRs.getTimestamp("FECHA_LLEGADA2")).thenReturn(ts(llegada2));
            when(mockRs.getString("ORIGEN2")).thenReturn("P2");
            when(mockRs.getString("DESTINO2")).thenReturn("P3");
            when(mockRs.getString("ORIGEN_PAIS2")).thenReturn("Pais2");
            when(mockRs.getString("DESTINO_PAIS2")).thenReturn("Pais3");

            when(mockRs.getString("ESTADO1")).thenReturn("ACTIVO");
            when(mockRs.getString("ESTADO2")).thenReturn("ACTIVO");

            
            when(mockRs.getInt("ID_CLASE")).thenReturn(3);
            when(mockRs.wasNull()).thenReturn(false);
            when(mockRs.getInt("CUPO_TOTAL")).thenReturn(60);
            when(mockRs.getDouble("PRECIO")).thenReturn(1200.0);

            VueloDAO dao = new VueloDAO();

            VueloDTO.VueloConEscalaView view = dao.obtenerVueloConEscalaPublic(2000L);

            assertNotNull(view, "Debe devolver un vuelo con escala");
            assertEquals(2000L, view.idVueloConEscala());
            assertEquals("PUB2000", view.codigo());
            assertEquals("P1", view.origen());
            assertEquals("P3", view.destino());
            assertEquals("Pais1", view.origenPais());
            assertEquals("Pais3", view.destinoPais());
            assertEquals(salida1, view.fechaSalida());
            assertEquals(llegada2, view.fechaLlegada());
            assertTrue(view.activo());
            assertEquals("ACTIVO", view.estado());

            assertNotNull(view.primerTramo());
            assertEquals("P1", view.primerTramo().origen());
            assertEquals("P2", view.primerTramo().destino());

            assertNotNull(view.segundoTramo());
            assertEquals("P2", view.segundoTramo().origen());
            assertEquals("P3", view.segundoTramo().destino());

            assertNotNull(view.clases());
            assertEquals(1, view.clases().size());
            VueloDTO.ClaseConfig c = view.clases().get(0);
            assertEquals(3, c.idClase());
            assertEquals(60, c.cupoTotal());
            assertEquals(1200.0, c.precio());
        }
    }

        @Test
    @DisplayName("obtenerVueloPublic devuelve vuelo con clases cuando hay disponibilidad")
    void obtenerVueloPublic_devuelveVueloConClases() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockPs = mock(PreparedStatement.class);
        ResultSet mockRs = mock(ResultSet.class);

        LocalDateTime salida = LocalDateTime.of(2025, 9, 1, 15, 0);
        LocalDateTime llegada = LocalDateTime.of(2025, 9, 1, 18, 0);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

            dbMock.when(() -> DB.table(anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, String.class));
            dbMock.when(DB::getConnection).thenReturn(mockConn);

            when(mockConn.prepareStatement(anyString())).thenReturn(mockPs);
            when(mockPs.executeQuery()).thenReturn(mockRs);

            
            when(mockRs.next())
                    .thenReturn(true)
                    .thenReturn(false);

            
            when(mockRs.getLong("ID_VUELO")).thenReturn(500L);
            when(mockRs.getString("CODIGO")).thenReturn("PUB500");
            when(mockRs.getLong("ID_RUTA")).thenReturn(33L);
            when(mockRs.getString("ORIGEN")).thenReturn("OrigenPub");
            when(mockRs.getString("DESTINO")).thenReturn("DestinoPub");
            when(mockRs.getString("ORIGEN_PAIS")).thenReturn("PaisOrigenPub");
            when(mockRs.getString("DESTINO_PAIS")).thenReturn("PaisDestinoPub");
            when(mockRs.getTimestamp("FECHA_SALIDA")).thenReturn(ts(salida));
            when(mockRs.getTimestamp("FECHA_LLEGADA")).thenReturn(ts(llegada));
            
            when(mockRs.getInt("ACTIVO")).thenReturn(1);

            when(mockRs.getObject("ID_ESTADO")).thenReturn(2);
            when(mockRs.getInt("ID_ESTADO")).thenReturn(2);
            when(mockRs.getString("ESTADO")).thenReturn("DISPONIBLE");
            when(mockRs.getObject("PAREJA_ID")).thenReturn(null);

            
            when(mockRs.getInt("DISPONIBLE")).thenReturn(10);
            
            when(mockRs.wasNull()).thenReturn(false, false);

            
            when(mockRs.getInt("ID_CLASE")).thenReturn(1);
            when(mockRs.getInt("CUPO_TOTAL")).thenReturn(100);
            when(mockRs.getDouble("PRECIO")).thenReturn(750.0);

            VueloDAO dao = new VueloDAO();

            VueloDTO.View view = dao.obtenerVueloPublic(500L);

            assertNotNull(view, "Debe devolver el vuelo público");
            assertEquals(500L, view.idVuelo());
            assertEquals("PUB500", view.codigo());
            assertEquals(33L, view.idRuta());
            assertEquals("OrigenPub", view.origen());
            assertEquals("DestinoPub", view.destino());
            assertEquals("PaisOrigenPub", view.origenPais());
            assertEquals("PaisDestinoPub", view.destinoPais());
            assertEquals(salida, view.fechaSalida());
            assertEquals(llegada, view.fechaLlegada());
            assertTrue(view.activo());
            assertEquals(2, view.idEstado());
            assertEquals("DISPONIBLE", view.estado());

            assertNotNull(view.clases());
            assertEquals(1, view.clases().size());
            VueloDTO.ClaseConfig c = view.clases().get(0);
            assertEquals(1, c.idClase());
            assertEquals(100, c.cupoTotal());
            assertEquals(750.0, c.precio());

            
            assertNotNull(view.escalas());
            assertTrue(view.escalas().isEmpty());
        }
    }

    @Test
    @DisplayName("obtenerVueloPublic devuelve null si no hay disponibilidad en ninguna clase")
    void obtenerVueloPublic_sinDisponibilidadDevuelveNull() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockPs = mock(PreparedStatement.class);
        ResultSet mockRs = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

            dbMock.when(() -> DB.table(anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, String.class));
            dbMock.when(DB::getConnection).thenReturn(mockConn);

            when(mockConn.prepareStatement(anyString())).thenReturn(mockPs);
            when(mockPs.executeQuery()).thenReturn(mockRs);

            
            when(mockRs.next())
                    .thenReturn(true)
                    .thenReturn(false);

            
            when(mockRs.getInt("DISPONIBLE")).thenReturn(0);
            
            when(mockRs.wasNull()).thenReturn(false);

            VueloDAO dao = new VueloDAO();

            VueloDTO.View view = dao.obtenerVueloPublic(123L);

            assertNull(view, "Debe devolver null si no hay disponibilidad");
        }
    }

    @Test
    @DisplayName("listarVuelosPublic agrupa clases y solo incluye vuelos con disponibilidad")
    void listarVuelosPublic_filtraDisponibilidadYAgrupaClases() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockPs = mock(PreparedStatement.class);
        ResultSet mockRs = mock(ResultSet.class);

        LocalDateTime salida = LocalDateTime.of(2025, 10, 1, 9, 0);
        LocalDateTime llegada = LocalDateTime.of(2025, 10, 1, 11, 0);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

            dbMock.when(() -> DB.table(anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, String.class));
            dbMock.when(DB::getConnection).thenReturn(mockConn);

            when(mockConn.prepareStatement(anyString())).thenReturn(mockPs);
            when(mockPs.executeQuery()).thenReturn(mockRs);

            
            
            
            
            when(mockRs.next())
                    .thenReturn(true)   
                    .thenReturn(true)   
                    .thenReturn(true)   
                    .thenReturn(false);

            
            when(mockRs.getLong("ID_VUELO"))
                    .thenReturn(1L) 
                    .thenReturn(1L) 
                    .thenReturn(2L); 

            when(mockRs.getString("CODIGO"))
                    .thenReturn("PUB1") 
                    .thenReturn("PUB1") 
                    .thenReturn("PUB2"); 

            when(mockRs.getLong("ID_RUTA"))
                    .thenReturn(99L)
                    .thenReturn(99L)
                    .thenReturn(100L);

            when(mockRs.getString("ORIGEN"))
                    .thenReturn("O1")
                    .thenReturn("O1")
                    .thenReturn("O2");
            when(mockRs.getString("DESTINO"))
                    .thenReturn("D1")
                    .thenReturn("D1")
                    .thenReturn("D2");
            when(mockRs.getString("ORIGEN_PAIS"))
                    .thenReturn("PO1")
                    .thenReturn("PO1")
                    .thenReturn("PO2");
            when(mockRs.getString("DESTINO_PAIS"))
                    .thenReturn("PD1")
                    .thenReturn("PD1")
                    .thenReturn("PD2");

            when(mockRs.getTimestamp("FECHA_SALIDA"))
                    .thenReturn(ts(salida))
                    .thenReturn(ts(salida))
                    .thenReturn(ts(salida.plusHours(1)));
            when(mockRs.getTimestamp("FECHA_LLEGADA"))
                    .thenReturn(ts(llegada))
                    .thenReturn(ts(llegada))
                    .thenReturn(ts(llegada.plusHours(1)));

            when(mockRs.getInt("ACTIVO")).thenReturn(1, 1, 1);
            when(mockRs.getObject("ID_ESTADO")).thenReturn(1, 1, 1);
            when(mockRs.getInt("ID_ESTADO")).thenReturn(1, 1, 1);
            when(mockRs.getString("ESTADO")).thenReturn("ACTIVO", "ACTIVO", "ACTIVO");
            when(mockRs.getObject("PAREJA")).thenReturn(null, null, null);

            
            when(mockRs.getInt("DISPONIBLE"))
                    .thenReturn(5)   
                    .thenReturn(3)   
                    .thenReturn(0);  

            
            when(mockRs.wasNull())
                    .thenReturn(false) 
                    .thenReturn(false) 
                    .thenReturn(false); 

            
            when(mockRs.getInt("ID_CLASE"))
                    .thenReturn(1)   
                    .thenReturn(2)   
                    .thenReturn(1);  
            when(mockRs.getInt("CUPO_TOTAL"))
                    .thenReturn(50)
                    .thenReturn(60)
                    .thenReturn(10);
            when(mockRs.getDouble("PRECIO"))
                    .thenReturn(500.0)
                    .thenReturn(600.0)
                    .thenReturn(700.0);

            VueloDAO dao = new VueloDAO();

            List<VueloDTO.View> lista = dao.listarVuelosPublic();

            
            assertEquals(1, lista.size(), "Solo debe incluir vuelos con DISPONIBLE > 0");
            VueloDTO.View v1 = lista.get(0);

            assertEquals(1L, v1.idVuelo());
            assertEquals("PUB1", v1.codigo());
            assertEquals("O1", v1.origen());
            assertEquals("D1", v1.destino());

            assertNotNull(v1.clases());
            assertEquals(2, v1.clases().size(), "Debe agrupar las 2 clases del mismo vuelo");
            assertEquals(1, v1.clases().get(0).idClase());
            assertEquals(2, v1.clases().get(1).idClase());
        }
    }
    @Test
    @DisplayName("listarVuelos devuelve lista vacía inmodificable cuando no hay datos")
    void listarVuelos_sinResultadosDevuelveListaVacia() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockPs = mock(PreparedStatement.class);
        ResultSet mockRs = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

            dbMock.when(() -> DB.table(anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, String.class));
            dbMock.when(DB::getConnection).thenReturn(mockConn);

            when(mockConn.prepareStatement(anyString())).thenReturn(mockPs);
            when(mockPs.executeQuery()).thenReturn(mockRs);

            
            when(mockRs.next()).thenReturn(false);

            VueloDAO dao = new VueloDAO();

            List<VueloDTO.View> lista = dao.listarVuelos(true);

            assertNotNull(lista, "Nunca debe devolver null");
            assertTrue(lista.isEmpty(), "Debe devolver lista vacía si no hay vuelos");

            
            assertThrows(UnsupportedOperationException.class, () -> lista.add(null));
        }
    }
        @Test
    @DisplayName("vincularPareja vincula ida y regreso correctamente cuando todo es válido")
    void vincularPareja_ok() throws Exception {
        long idIda = 1L;
        long idRegreso = 2L;

        Connection mockConn = mock(Connection.class);
        PreparedStatement psSel1 = mock(PreparedStatement.class);
        PreparedStatement psSel2 = mock(PreparedStatement.class);
        PreparedStatement psChk  = mock(PreparedStatement.class);
        PreparedStatement psClear = mock(PreparedStatement.class);
        PreparedStatement psSet   = mock(PreparedStatement.class);
        ResultSet rsSel1 = mock(ResultSet.class);
        ResultSet rsSel2 = mock(ResultSet.class);
        ResultSet rsChk  = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

            dbMock.when(() -> DB.table(anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, String.class));
            dbMock.when(DB::getConnection).thenReturn(mockConn);
            
            when(mockConn.prepareStatement(startsWith("SELECT ID_VUELO, ID_VUELO_PAREJA FROM VUELO")))
                    .thenReturn(psSel1, psSel2);

            when(psSel1.executeQuery()).thenReturn(rsSel1);
            when(psSel2.executeQuery()).thenReturn(rsSel2);

            when(rsSel1.next()).thenReturn(true);  
            when(rsSel2.next()).thenReturn(true);  

            
            when(mockConn.prepareStatement(startsWith("SELECT COUNT(*) FROM VUELO")))
                    .thenReturn(psChk);
            when(psChk.executeQuery()).thenReturn(rsChk);
            when(rsChk.next()).thenReturn(true);
            when(rsChk.getInt(1)).thenReturn(0);   

            
            when(mockConn.prepareStatement(startsWith("UPDATE VUELO SET ID_VUELO_PAREJA = NULL")))
                    .thenReturn(psClear);

            
            when(mockConn.prepareStatement(startsWith("UPDATE VUELO SET ID_VUELO_PAREJA = ?")))
                    .thenReturn(psSet);

            VueloDAO dao = new VueloDAO();
            dao.vincularPareja(idIda, idRegreso);

            
            InOrder inOrder = inOrder(mockConn);
            inOrder.verify(mockConn).setAutoCommit(false);
            inOrder.verify(mockConn).commit();
            inOrder.verify(mockConn).setAutoCommit(true);

            
            verify(psClear).setLong(1, idRegreso);
            verify(psClear).executeUpdate();

            verify(psSet).setLong(1, idRegreso);
            verify(psSet).setLong(2, idIda);
            verify(psSet).executeUpdate();
        }
    }

    @Test
    @DisplayName("vincularPareja lanza SQLException si los IDs son inválidos (<=0)")
    void vincularPareja_idsInvalidos() {
        VueloDAO dao = new VueloDAO();

        SQLException ex1 = assertThrows(SQLException.class,
                () -> dao.vincularPareja(0L, 2L));
        assertTrue(ex1.getMessage().contains("IDs de vuelo inválidos"));

        SQLException ex2 = assertThrows(SQLException.class,
                () -> dao.vincularPareja(1L, 0L));
        assertTrue(ex2.getMessage().contains("IDs de vuelo inválidos"));
    }

    @Test
    @DisplayName("vincularPareja lanza SQLException si ida y regreso son el mismo vuelo")
    void vincularPareja_mismoId() {
        VueloDAO dao = new VueloDAO();

        SQLException ex = assertThrows(SQLException.class,
                () -> dao.vincularPareja(5L, 5L));

        assertTrue(ex.getMessage().contains("pareja de sí mismo"));
    }

    @Test
    @DisplayName("vincularPareja lanza SQLException si el vuelo de ida no existe")
    void vincularPareja_vueloIdaNoExiste() throws Exception {
        long idIda = 10L;
        long idRegreso = 20L;

        Connection mockConn = mock(Connection.class);
        PreparedStatement psSel1 = mock(PreparedStatement.class);
        ResultSet rsSel1 = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

            dbMock.when(() -> DB.table(anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, String.class));
            dbMock.when(DB::getConnection).thenReturn(mockConn);

            when(mockConn.prepareStatement(startsWith("SELECT ID_VUELO, ID_VUELO_PAREJA FROM VUELO")))
                    .thenReturn(psSel1);
            when(psSel1.executeQuery()).thenReturn(rsSel1);
            when(rsSel1.next()).thenReturn(false); 

            VueloDAO dao = new VueloDAO();

            SQLException ex = assertThrows(SQLException.class,
                    () -> dao.vincularPareja(idIda, idRegreso));

            assertTrue(ex.getMessage().contains("Vuelo ida no existe"));

            verify(mockConn).setAutoCommit(false);
            verify(mockConn).rollback();
            verify(mockConn).setAutoCommit(true);
        }
    }

    @Test
    @DisplayName("vincularPareja lanza SQLException si otro vuelo ya apunta al regreso")
    void vincularPareja_otroVueloApuntaAlRegreso() throws Exception {
        long idIda = 1L;
        long idRegreso = 2L;

        Connection mockConn = mock(Connection.class);
        PreparedStatement psSel1 = mock(PreparedStatement.class);
        PreparedStatement psSel2 = mock(PreparedStatement.class);
        PreparedStatement psChk  = mock(PreparedStatement.class);
        ResultSet rsSel1 = mock(ResultSet.class);
        ResultSet rsSel2 = mock(ResultSet.class);
        ResultSet rsChk  = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

            dbMock.when(() -> DB.table(anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, String.class));
            dbMock.when(DB::getConnection).thenReturn(mockConn);

            
            when(mockConn.prepareStatement(startsWith("SELECT ID_VUELO, ID_VUELO_PAREJA FROM VUELO")))
                    .thenReturn(psSel1, psSel2);

            when(psSel1.executeQuery()).thenReturn(rsSel1);
            when(psSel2.executeQuery()).thenReturn(rsSel2);

            when(rsSel1.next()).thenReturn(true);
            when(rsSel2.next()).thenReturn(true);

            
            when(mockConn.prepareStatement(startsWith("SELECT COUNT(*) FROM VUELO")))
                    .thenReturn(psChk);
            when(psChk.executeQuery()).thenReturn(rsChk);
            when(rsChk.next()).thenReturn(true);
            when(rsChk.getInt(1)).thenReturn(1);

            VueloDAO dao = new VueloDAO();

            SQLException ex = assertThrows(SQLException.class,
                    () -> dao.vincularPareja(idIda, idRegreso));

            assertTrue(ex.getMessage().contains("ya está enlazado por otra ida"));

            verify(mockConn).setAutoCommit(false);
            verify(mockConn).rollback();
            verify(mockConn).setAutoCommit(true);
        }
    }

    @Test
    @DisplayName("desvincularPareja limpia ID_VUELO_PAREJA del vuelo y de otros que lo apunten")
    void desvincularPareja_ok() throws Exception {
        long idVuelo = 50L;

        Connection mockConn = mock(Connection.class);
        PreparedStatement psSel = mock(PreparedStatement.class);
        PreparedStatement psClearSelf = mock(PreparedStatement.class);
        PreparedStatement psClearPointing = mock(PreparedStatement.class);
        ResultSet rsSel = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

            dbMock.when(() -> DB.table(anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, String.class));
            dbMock.when(DB::getConnection).thenReturn(mockConn);

            
            when(mockConn.prepareStatement(startsWith("SELECT 1 FROM VUELO")))
                    .thenReturn(psSel);
            when(psSel.executeQuery()).thenReturn(rsSel);
            when(rsSel.next()).thenReturn(true); 

            
            when(mockConn.prepareStatement(startsWith("UPDATE VUELO SET ID_VUELO_PAREJA = NULL WHERE ID_VUELO = ?")))
                    .thenReturn(psClearSelf);

            
            when(mockConn.prepareStatement(startsWith("UPDATE VUELO SET ID_VUELO_PAREJA = NULL WHERE ID_VUELO_PAREJA = ?")))
                    .thenReturn(psClearPointing);

            VueloDAO dao = new VueloDAO();
            dao.desvincularPareja(idVuelo);

            InOrder inOrder = inOrder(mockConn);
            inOrder.verify(mockConn).setAutoCommit(false);
            inOrder.verify(mockConn).commit();
            inOrder.verify(mockConn).setAutoCommit(true);

            verify(psClearSelf).setLong(1, idVuelo);
            verify(psClearSelf).executeUpdate();
            verify(psClearPointing).setLong(1, idVuelo);
            verify(psClearPointing).executeUpdate();
        }
    }

    @Test
    @DisplayName("desvincularPareja lanza SQLException si el ID es inválido (<=0)")
    void desvincularPareja_idInvalido() {
        VueloDAO dao = new VueloDAO();

        SQLException ex = assertThrows(SQLException.class,
                () -> dao.desvincularPareja(0L));

        assertTrue(ex.getMessage().contains("ID de vuelo inválido"));
    }

    @Test
    @DisplayName("desvincularPareja lanza SQLException si el vuelo no existe")
    void desvincularPareja_vueloNoExiste() throws Exception {
        long idVuelo = 99L;

        Connection mockConn = mock(Connection.class);
        PreparedStatement psSel = mock(PreparedStatement.class);
        ResultSet rsSel = mock(ResultSet.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

            dbMock.when(() -> DB.table(anyString()))
                    .thenAnswer(inv -> inv.getArgument(0, String.class));
            dbMock.when(DB::getConnection).thenReturn(mockConn);

            when(mockConn.prepareStatement(startsWith("SELECT 1 FROM VUELO")))
                    .thenReturn(psSel);
            when(psSel.executeQuery()).thenReturn(rsSel);
            when(rsSel.next()).thenReturn(false); 

            VueloDAO dao = new VueloDAO();

            SQLException ex = assertThrows(SQLException.class,
                    () -> dao.desvincularPareja(idVuelo));

            assertTrue(ex.getMessage().contains("Vuelo no encontrado"));

            verify(mockConn).setAutoCommit(false);
            verify(mockConn).rollback();
            verify(mockConn).setAutoCommit(true);
        }
    }

    @Test
@DisplayName("listarVuelos() sin argumentos devuelve lista vacía con DB mockeada")
void listarVuelos_sinArgumentosDelegaalConActivoTrue() throws Exception {
    Connection mockConn = mock(Connection.class);
    PreparedStatement mockPs = mock(PreparedStatement.class);
    ResultSet mockRs = mock(ResultSet.class);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

        dbMock.when(DB::getConnection).thenReturn(mockConn);
        dbMock.when(() -> DB.table(anyString()))
              .thenAnswer(inv -> inv.getArgument(0, String.class));

        when(mockConn.prepareStatement(anyString())).thenReturn(mockPs);
        when(mockPs.executeQuery()).thenReturn(mockRs);
        
        when(mockRs.next()).thenReturn(false);

        VueloDAO dao = new VueloDAO();
        
        List<VueloDTO.View> lista = dao.listarVuelos();

        assertNotNull(lista, "Nunca debe devolver null");
        assertTrue(lista.isEmpty(), "Debe devolver lista vacía si no hay vuelos");
    }
}

@Test
@DisplayName("crearVuelo funciona con DB mockeada (inserta vuelo y clases sin BD real)")
void crearVuelo_conMocksNoUsaBDReal() throws Exception {
    Connection mockConn = mock(Connection.class);
    PreparedStatement psInsert = mock(PreparedStatement.class);
    PreparedStatement psClase = mock(PreparedStatement.class);
    ResultSet rsKeys = mock(ResultSet.class);

    LocalDateTime salida = LocalDateTime.of(2025, 11, 1, 10, 0);
    LocalDateTime llegada = LocalDateTime.of(2025, 11, 1, 12, 0);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

        dbMock.when(DB::getConnection).thenReturn(mockConn);
        dbMock.when(() -> DB.table(anyString()))
              .thenAnswer(inv -> inv.getArgument(0, String.class));
    
        when(mockConn.prepareStatement(anyString(), any(String[].class)))
                .thenReturn(psInsert);

        when(mockConn.prepareStatement(contains("SALIDA_CLASE")))
                .thenReturn(psClase);
    
        when(psInsert.getGeneratedKeys()).thenReturn(rsKeys);
        when(rsKeys.next()).thenReturn(true);
        when(rsKeys.getLong(1)).thenReturn(999L);

        VueloDAO dao = new VueloDAO();

        VueloDTO.Create dto = new VueloDTO.Create(
                "COD-SPY",
                1L,
                salida,
                llegada,
                List.of(new VueloDTO.ClaseConfig(1, 100, 500.0)),
                null,
                true
        );
    
        assertDoesNotThrow(() -> dao.crearVuelo(dto));

        verify(psInsert, times(1)).executeUpdate();
        verify(psClase, atLeastOnce()).executeUpdate();
    }
}

@Test
@DisplayName("listarVuelosConEscalaPublic agrupa clases por vuelo con escala")
void listarVuelosConEscalaPublic_agrupaClases() throws Exception {
    Connection mockConn = mock(Connection.class);
    PreparedStatement mockPs = mock(PreparedStatement.class);
    ResultSet mockRs = mock(ResultSet.class);

    LocalDateTime salida1 = LocalDateTime.of(2025, 7, 1, 7, 0);
    LocalDateTime llegada1 = LocalDateTime.of(2025, 7, 1, 9, 0);
    LocalDateTime salida2 = LocalDateTime.of(2025, 7, 1, 10, 0);
    LocalDateTime llegada2 = LocalDateTime.of(2025, 7, 1, 13, 0);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

        dbMock.when(() -> DB.table(anyString()))
              .thenAnswer(inv -> inv.getArgument(0, String.class));
        dbMock.when(DB::getConnection).thenReturn(mockConn);

        when(mockConn.prepareStatement(anyString())).thenReturn(mockPs);
        when(mockPs.executeQuery()).thenReturn(mockRs);

        when(mockRs.next())
                .thenReturn(true)
                .thenReturn(true)
                .thenReturn(false);

        when(mockRs.getLong("ID_VUELO_CON_ESCALA")).thenReturn(1000L);
        when(mockRs.getString("CODIGO")).thenReturn("ESC-1000");
        when(mockRs.getInt("ACTIVO")).thenReturn(1);
        
        when(mockRs.getLong("ID_VUELO1")).thenReturn(10L);
        when(mockRs.getString("CODIGO1")).thenReturn("TRAMO1");
        when(mockRs.getTimestamp("FECHA_SALIDA1")).thenReturn(Timestamp.valueOf(salida1));
        when(mockRs.getTimestamp("FECHA_LLEGADA1")).thenReturn(Timestamp.valueOf(llegada1));
        when(mockRs.getString("ORIGEN1")).thenReturn("Ciudad A");
        when(mockRs.getString("DESTINO1")).thenReturn("Ciudad B");
        when(mockRs.getString("ORIGEN_PAIS1")).thenReturn("Pais A");
        when(mockRs.getString("DESTINO_PAIS1")).thenReturn("Pais B");
        
        when(mockRs.getLong("ID_VUELO2")).thenReturn(20L);
        when(mockRs.getString("CODIGO2")).thenReturn("TRAMO2");
        when(mockRs.getTimestamp("FECHA_SALIDA2")).thenReturn(Timestamp.valueOf(salida2));
        when(mockRs.getTimestamp("FECHA_LLEGADA2")).thenReturn(Timestamp.valueOf(llegada2));
        when(mockRs.getString("ORIGEN2")).thenReturn("Ciudad B");
        when(mockRs.getString("DESTINO2")).thenReturn("Ciudad C");
        when(mockRs.getString("ORIGEN_PAIS2")).thenReturn("Pais B");
        when(mockRs.getString("DESTINO_PAIS2")).thenReturn("Pais C");

        when(mockRs.getString("ESTADO1")).thenReturn("ACTIVO");
        when(mockRs.getString("ESTADO2")).thenReturn("ACTIVO");

        when(mockRs.getInt("ID_CLASE"))
                .thenReturn(1)
                .thenReturn(2);
        when(mockRs.getInt("CUPO_TOTAL"))
                .thenReturn(30)
                .thenReturn(40);
        when(mockRs.getDouble("PRECIO"))
                .thenReturn(1000.0)
                .thenReturn(1500.0);

        VueloDAO dao = new VueloDAO();

        List<VueloDTO.VueloConEscalaView> lista = dao.listarVuelosConEscalaPublic();

        assertEquals(1, lista.size(), "Debe haber un solo vuelo con escala");
        VueloDTO.VueloConEscalaView view = lista.get(0);

        assertEquals(1000L, view.idVueloConEscala());
        assertEquals("ESC-1000", view.codigo());
        assertEquals("Ciudad A", view.origen());
        assertEquals("Ciudad C", view.destino());
        assertEquals(salida1, view.fechaSalida());
        assertEquals(llegada2, view.fechaLlegada());
        assertTrue(view.activo());
        assertEquals("ACTIVO", view.estado());

        assertNotNull(view.clases());
        assertEquals(2, view.clases().size());
        assertEquals(1, view.clases().get(0).idClase());
        assertEquals(2, view.clases().get(1).idClase());
    }
}

@Test
@DisplayName("obtenerVueloConEscala devuelve vuelo con escala y clases")
void obtenerVueloConEscala_devuelveVueloConEscala() throws Exception {
    Connection mockConn = mock(Connection.class);
    PreparedStatement mockPs = mock(PreparedStatement.class);
    ResultSet mockRs = mock(ResultSet.class);

    LocalDateTime salida1 = LocalDateTime.of(2025, 8, 1, 6, 0);
    LocalDateTime llegada1 = LocalDateTime.of(2025, 8, 1, 8, 0);
    LocalDateTime salida2 = LocalDateTime.of(2025, 8, 1, 9, 0);
    LocalDateTime llegada2 = LocalDateTime.of(2025, 8, 1, 12, 0);

    try (MockedStatic<DB> dbMock = mockStatic(DB.class)) {

        dbMock.when(() -> DB.table(anyString()))
              .thenAnswer(inv -> inv.getArgument(0, String.class));
        dbMock.when(DB::getConnection).thenReturn(mockConn);

        when(mockConn.prepareStatement(anyString())).thenReturn(mockPs);
        when(mockPs.executeQuery()).thenReturn(mockRs);

        when(mockRs.next())
                .thenReturn(true)
                .thenReturn(false);

        when(mockRs.getLong("ID_VUELO_CON_ESCALA")).thenReturn(2000L);
        when(mockRs.getString("CODIGO")).thenReturn("ESC2000");
        when(mockRs.getInt("ACTIVO")).thenReturn(1);

        
        when(mockRs.getLong("ID_VUELO1")).thenReturn(11L);
        when(mockRs.getString("CODIGO1")).thenReturn("T1");
        when(mockRs.getTimestamp("FECHA_SALIDA1")).thenReturn(Timestamp.valueOf(salida1));
        when(mockRs.getTimestamp("FECHA_LLEGADA1")).thenReturn(Timestamp.valueOf(llegada1));
        when(mockRs.getString("ORIGEN1")).thenReturn("P1");
        when(mockRs.getString("DESTINO1")).thenReturn("P2");
        when(mockRs.getString("ORIGEN_PAIS1")).thenReturn("Pais1");
        when(mockRs.getString("DESTINO_PAIS1")).thenReturn("Pais2");

        
        when(mockRs.getLong("ID_VUELO2")).thenReturn(22L);
        when(mockRs.getString("CODIGO2")).thenReturn("T2");
        when(mockRs.getTimestamp("FECHA_SALIDA2")).thenReturn(Timestamp.valueOf(salida2));
        when(mockRs.getTimestamp("FECHA_LLEGADA2")).thenReturn(Timestamp.valueOf(llegada2));
        when(mockRs.getString("ORIGEN2")).thenReturn("P2");
        when(mockRs.getString("DESTINO2")).thenReturn("P3");
        when(mockRs.getString("ORIGEN_PAIS2")).thenReturn("Pais2");
        when(mockRs.getString("DESTINO_PAIS2")).thenReturn("Pais3");

        when(mockRs.getString("ESTADO1")).thenReturn("PROGRAMADO");
        when(mockRs.getString("ESTADO2")).thenReturn("PROGRAMADO");

        when(mockRs.getInt("ID_CLASE")).thenReturn(3);
        when(mockRs.wasNull()).thenReturn(false);
        when(mockRs.getInt("CUPO_TOTAL")).thenReturn(60);
        when(mockRs.getDouble("PRECIO")).thenReturn(1200.0);

        VueloDAO dao = new VueloDAO();

        
        VueloDTO.VueloConEscalaView view = dao.obtenerVueloConEscala(2000L);

        assertNotNull(view, "Debe devolver un vuelo con escala");
        assertEquals(2000L, view.idVueloConEscala());
        assertEquals("ESC2000", view.codigo());
        assertEquals("P1", view.origen());
        assertEquals("P3", view.destino());
        assertEquals("Pais1", view.origenPais());
        assertEquals("Pais3", view.destinoPais());
        assertEquals(salida1, view.fechaSalida());
        assertEquals(llegada2, view.fechaLlegada());
        assertTrue(view.activo());
        assertEquals("ACTIVO", view.estado());
        assertNotNull(view.clases());
        assertEquals(1, view.clases().size());
    }
}

}
