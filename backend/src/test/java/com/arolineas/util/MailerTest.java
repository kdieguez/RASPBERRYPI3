package com.aerolineas.util;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MailerTest {

  private static String callEnv(String key, String def) throws Exception {
    Method m = Mailer.class.getDeclaredMethod("env", String.class, String.class);
    m.setAccessible(true);
    return (String) m.invoke(null, key, def);
  }

  private static boolean callBool(String key, boolean def) throws Exception {
    Method m = Mailer.class.getDeclaredMethod("bool", String.class, boolean.class);
    m.setAccessible(true);
    return (boolean) m.invoke(null, key, def);
  }

  private static Session callBuildSession() throws Exception {
    Method m = Mailer.class.getDeclaredMethod("buildSession");
    m.setAccessible(true);
    return (Session) m.invoke(null);
  }
  @AfterEach
  void cleanProps() {
    for (String k : new String[]{
        "MAIL_USER","MAIL_PASS","MAIL_HOST","MAIL_PORT",
        "MAIL_AUTH","MAIL_TLS","MAIL_DEBUG","MAIL_FROM"
    }) {
      System.clearProperty(k);
    }
  }

  @Test
  void env_sinVariableNiPropiedadDevuelveDefault() throws Exception {
    String key = "MAILER_TEST_NO_EXISTE_" + System.nanoTime();
    System.clearProperty(key);

    String v = callEnv(key, "valor-defecto");

    assertEquals("valor-defecto", v);
  }

  @Test
  void env_conPropiedadDevuelveValor() throws Exception {
    String key = "MAILER_ENV_TEST_" + System.nanoTime();
    System.setProperty(key, "  hola  ");

    String v = callEnv(key, "otro");

    assertEquals("hola", v);
  }

  @Test
  void bool_conNadaDevuelveDefault() throws Exception {
    String key = "MAILER_BOOL_NO_EXISTE_" + System.nanoTime();
    System.clearProperty(key);

    boolean b = callBool(key, true);

    assertTrue(b);
  }

  @Test
  void bool_conPropiedadTrueDevuelveTrue() throws Exception {
    String key = "MAILER_BOOL_TEST_" + System.nanoTime();
    System.setProperty(key, "TrUe");

    boolean b = callBool(key, false);

    assertTrue(b);
  }

  @Test
  void bool_conPropiedadRandomDevuelveFalse() throws Exception {
    String key = "MAILER_BOOL_TEST2_" + System.nanoTime();
    System.setProperty(key, "no");

    boolean b = callBool(key, true);

    assertFalse(b);
  }

  @Test
  void buildSession_lanzaExcepcionSiNoHayCredenciales() {
    System.clearProperty("MAIL_USER");
    System.clearProperty("MAIL_PASS");

    assertThrows(IllegalStateException.class, () -> {
      try {
        callBuildSession();
      } catch (Exception e) {
        if (e.getCause() instanceof IllegalStateException cause) {
          throw cause;
        }
        throw e;
      }
    });
  }

  @Test
  void buildSession_creaSessionConPropiedadesBasicas() throws Exception {
    System.setProperty("MAIL_USER", "user@test.com");
    System.setProperty("MAIL_PASS", "secret");
    System.setProperty("MAIL_HOST", "smtp.test.local");
    System.setProperty("MAIL_PORT", "465");
    System.setProperty("MAIL_AUTH", "true");
    System.setProperty("MAIL_TLS", "true");
    System.setProperty("MAIL_DEBUG", "true");

    Session s = callBuildSession();
    assertNotNull(s);

    Properties p = s.getProperties();
    assertEquals("smtp.test.local", p.getProperty("mail.smtp.host"));
    assertEquals("465", p.getProperty("mail.smtp.port"));
    assertEquals("true", p.getProperty("mail.smtp.auth"));
    assertEquals("true", p.getProperty("mail.smtp.starttls.enable"));
    assertEquals("true", p.getProperty("mail.smtp.starttls.required"));
    assertEquals("true", p.getProperty("mail.smtp.ssl.enable"));
  }

  @Test
  void send_conDestinatarioVacioLanzaIllegalArgument() {
    assertThrows(IllegalArgumentException.class,
        () -> Mailer.send("  ", "Asunto", "<b>Hola</b>"));
  }

  @Test
  void send_conDatosValidosInvocaTransportSend() throws Exception {
    System.setProperty("MAIL_USER", "noreply@test.com");
    System.setProperty("MAIL_PASS", "secret");
    System.setProperty("MAIL_FROM", "from@test.com");
    System.setProperty("MAIL_HOST", "smtp.test.local");
    System.setProperty("MAIL_PORT", "587");
    System.setProperty("MAIL_AUTH", "true");
    System.setProperty("MAIL_TLS", "true");
    System.setProperty("MAIL_DEBUG", "false");

    try (MockedStatic<Transport> transportMock = mockStatic(Transport.class)) {
      transportMock.when(() -> Transport.send(any(Message.class))).thenAnswer(inv -> null);

      Mailer.send("dest@test.com", "Prueba",
          "<h1>Hola&nbsp;Mundo</h1><p>Texto <b>de prueba</b></p>");

      transportMock.verify(() -> Transport.send(any(Message.class)), times(1));
    }
  }
}
