package com.aerolineas.service;

import com.aerolineas.config.DB;
import com.aerolineas.dao.VueloDAO;
import com.aerolineas.dto.VueloDTO;
import com.aerolineas.util.Mailer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificacionesServiceTest {

  @Test
  void notificarCambio_sinVuelo_noEnviaCorreos() {
    long idVuelo = 100L;

    try (MockedConstruction<VueloDAO> mockedVueloDao = mockConstruction(VueloDAO.class,
             (mockDao, context) -> when(mockDao.obtenerVuelo(idVuelo)).thenReturn(null));
         MockedStatic<Mailer> mailerMock = mockStatic(Mailer.class)) {

      NotificacionesService svc = new NotificacionesService();
      svc.notificarCambio(idVuelo, "motivo-x");

      mailerMock.verifyNoInteractions();
    }
  }

  @Test
  void notificarCambio_conDestinatarios_enviaCorreoPersonalizado() throws Exception {
    long idVuelo = 200L;

    VueloDTO.View mockView = mock(VueloDTO.View.class);
    when(mockView.codigo()).thenReturn("AV123");
    when(mockView.fechaSalida()).thenReturn(LocalDateTime.of(2025, 1, 10, 15, 30));
    when(mockView.fechaLlegada()).thenReturn(LocalDateTime.of(2025, 1, 10, 18, 0));
    when(mockView.origen()).thenReturn("Ciudad de Guatemala");
    when(mockView.origenPais()).thenReturn("Guatemala");
    when(mockView.destino()).thenReturn("San Salvador");
    when(mockView.destinoPais()).thenReturn("El Salvador");

    Connection cn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    try (MockedConstruction<VueloDAO> mockedVueloDao = mockConstruction(VueloDAO.class,
             (mockDao, context) -> when(mockDao.obtenerVuelo(idVuelo)).thenReturn(mockView));
         MockedStatic<DB> dbMock = mockStatic(DB.class);
         MockedStatic<Mailer> mailerMock = mockStatic(Mailer.class)) {

      dbMock.when(() -> DB.table(anyString()))
            .thenAnswer(inv -> inv.getArgument(0));

      dbMock.when(DB::getConnection).thenReturn(cn);
      when(cn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);

      when(rs.next()).thenReturn(true, false);
      when(rs.getLong("ID_RESERVA")).thenReturn(10L);
      when(rs.getLong("ID_USUARIO")).thenReturn(20L);
      when(rs.getString("EMAIL")).thenReturn("cliente@example.com");
      when(rs.getString("NOMBRES")).thenReturn("Ana");
      when(rs.getString("APELLIDOS")).thenReturn("López");

      mailerMock.when(() -> Mailer.send(anyString(), anyString(), anyString()))
                .thenAnswer(inv -> null);

      NotificacionesService svc = new NotificacionesService();
      svc.notificarCambio(idVuelo, "Cambio de horario");

      ArgumentCaptor<String> toCap = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> subjectCap = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> htmlCap = ArgumentCaptor.forClass(String.class);

      mailerMock.verify(() -> Mailer.send(
          toCap.capture(),
          subjectCap.capture(),
          htmlCap.capture()
      ), times(1));

      assertEquals("cliente@example.com", toCap.getValue());
      assertTrue(subjectCap.getValue().contains("Actualización de tu vuelo AV123"));
      assertTrue(htmlCap.getValue().contains("Hola Ana López"));
      assertTrue(htmlCap.getValue().contains("ha sido <strong>actualizado</strong>"));
    }
  }

  @Test
  void notificarCancelacion_conDestinatarios_enviaCorreoDeCancelacion() throws Exception {
    long idVuelo = 300L;

    VueloDTO.View mockView = mock(VueloDTO.View.class);
    when(mockView.codigo()).thenReturn("AV999");
    when(mockView.fechaSalida()).thenReturn(LocalDateTime.of(2025, 2, 5, 8, 0));
    when(mockView.fechaLlegada()).thenReturn(LocalDateTime.of(2025, 2, 5, 10, 30));
    when(mockView.origen()).thenReturn("Ciudad de Guatemala");
    when(mockView.origenPais()).thenReturn("Guatemala");
    when(mockView.destino()).thenReturn("Miami");
    when(mockView.destinoPais()).thenReturn("Estados Unidos");

    Connection cn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);

    try (MockedConstruction<VueloDAO> mockedVueloDao = mockConstruction(VueloDAO.class,
             (mockDao, context) -> when(mockDao.obtenerVuelo(idVuelo)).thenReturn(mockView));
         MockedStatic<DB> dbMock = mockStatic(DB.class);
         MockedStatic<Mailer> mailerMock = mockStatic(Mailer.class)) {

      dbMock.when(() -> DB.table(anyString()))
            .thenAnswer(inv -> inv.getArgument(0));
      dbMock.when(DB::getConnection).thenReturn(cn);
      when(cn.prepareStatement(anyString())).thenReturn(ps);
      when(ps.executeQuery()).thenReturn(rs);

      when(rs.next()).thenReturn(true, false);
      when(rs.getLong("ID_RESERVA")).thenReturn(50L);
      when(rs.getLong("ID_USUARIO")).thenReturn(60L);
      when(rs.getString("EMAIL")).thenReturn("otro@example.com");
      when(rs.getString("NOMBRES")).thenReturn("Carlos");
      when(rs.getString("APELLIDOS")).thenReturn("Pérez");

      mailerMock.when(() -> Mailer.send(anyString(), anyString(), anyString()))
                .thenAnswer(inv -> null);

      NotificacionesService svc = new NotificacionesService();
      svc.notificarCancelacion(idVuelo, "Condiciones climáticas");

      ArgumentCaptor<String> toCap = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> subjectCap = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> htmlCap = ArgumentCaptor.forClass(String.class);

      mailerMock.verify(() -> Mailer.send(
          toCap.capture(),
          subjectCap.capture(),
          htmlCap.capture()
      ), times(1));

      assertEquals("otro@example.com", toCap.getValue());
      assertTrue(subjectCap.getValue().contains("Cancelación de tu vuelo AV999"));
      assertTrue(htmlCap.getValue().contains("Hola Carlos Pérez"));
      assertTrue(htmlCap.getValue().contains("ha sido <strong>cancelado</strong>"));
    }
  }

  @Test
  void helpers_privados_ruta_dt_buildNombre_y_safe_funcionanComoSeEspera() throws Exception {
    NotificacionesService svc = new NotificacionesService();

    Method mRuta = NotificacionesService.class
        .getDeclaredMethod("ruta", String.class, String.class);
    mRuta.setAccessible(true);

    assertEquals("—", mRuta.invoke(svc, "", ""));
    assertEquals("Guatemala", mRuta.invoke(svc, "", "Guatemala"));
    assertEquals("Ciudad de Guatemala", mRuta.invoke(svc, "Ciudad de Guatemala", ""));
    assertEquals("Ciudad de Guatemala, Guatemala",
        mRuta.invoke(svc, "Ciudad de Guatemala", "Guatemala"));

    Method mDt = NotificacionesService.class
        .getDeclaredMethod("dt", LocalDateTime.class);
    mDt.setAccessible(true);

    assertEquals("—", mDt.invoke(svc, new Object[]{null}));
    String formatted = (String) mDt.invoke(svc, LocalDateTime.of(2025, 1, 1, 12, 0));
    assertNotNull(formatted);
    assertFalse(formatted.isBlank());
    assertTrue(formatted.contains("2025"));

    Method mNombre = NotificacionesService.class
        .getDeclaredMethod("buildNombre", String.class, String.class);
    mNombre.setAccessible(true);

    assertEquals("cliente", mNombre.invoke(svc, null, null));
    assertEquals("cliente", mNombre.invoke(svc, "   ", "   "));
    assertEquals("Ana", mNombre.invoke(svc, "Ana", "  "));
    assertEquals("Ana López", mNombre.invoke(svc, "Ana", "López"));

    Method mSafe = NotificacionesService.class
        .getDeclaredMethod("safe", String.class);
    mSafe.setAccessible(true);

    assertEquals("", mSafe.invoke(svc, new Object[]{null}));
    assertEquals("texto", mSafe.invoke(svc, "texto"));
  }
}
