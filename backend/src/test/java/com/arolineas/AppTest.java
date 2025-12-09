package com.arolineas;

import com.aerolineas.App;
import com.aerolineas.middleware.Auth;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppTest {


  private static int callResolvePort(String... args) throws Exception {
    Method m = App.class.getDeclaredMethod("resolvePort", String[].class);
    m.setAccessible(true);
    return (int) m.invoke(null, (Object) args);
  }

  private static void callParseArgs(String... args) throws Exception {
    Method m = App.class.getDeclaredMethod("parseArgs", String[].class);
    m.setAccessible(true);
    m.invoke(null, (Object) args);
  }

  private static boolean callIsAdmin(Context ctx) throws Exception {
    Method m = App.class.getDeclaredMethod("isAdmin", io.javalin.http.Context.class);
    m.setAccessible(true);
    return (boolean) m.invoke(null, ctx);
  }

  private static void callRequireAuth(Context ctx, Handler next) throws Exception {
    Method m = App.class.getDeclaredMethod("requireAuth", Context.class, Handler.class);
    m.setAccessible(true);
    m.invoke(null, ctx, next);
  }

  private static void callRequireAdmin(Context ctx, Handler next) throws Exception {
    Method m = App.class.getDeclaredMethod("requireAdmin", Context.class, Handler.class);
    m.setAccessible(true);
    m.invoke(null, ctx, next);
  }

  @AfterEach
  void clearSystemProps() {
    System.clearProperty("schema");
    System.clearProperty("oracle.user");
    System.clearProperty("oracle.password");
    System.clearProperty("port");
  }


  @Test
  void resolvePort_sinNadaUsa8080() throws Exception {
    int port = callResolvePort((String[]) null);
    assertEquals(8080, port);
  }

  @Test
  void resolvePort_priorizaSystemPropertySobreArgs() throws Exception {
    System.setProperty("port", "9001");
    int port = callResolvePort("--port=7777");
    assertEquals(9001, port);
  }

  @Test
  void resolvePort_usaPortDeArgsSiNoHaySystemProperty() throws Exception {
    int port = callResolvePort("--port=7777");
    assertEquals(7777, port);
  }


  @Test
  void parseArgs_seteaSchemaUserYPasswordEnSystemProperties() throws Exception {
    callParseArgs(
        "--schema=TEST_SCHEMA",
        "--user=ORACLE_USER",
        "--password=ORACLE_PASS",
        "--port=9999"
    );

    assertEquals("TEST_SCHEMA", System.getProperty("schema"));
    assertEquals("ORACLE_USER", System.getProperty("oracle.user"));
    assertEquals("ORACLE_PASS", System.getProperty("oracle.password"));
  }


  @Test
  void isAdmin_devuelveTrueCuandoRolEs1() throws Exception {
    Context ctx = mock(Context.class);
    when(ctx.attribute("claims")).thenReturn(Map.of("rol", 1));

    boolean admin = callIsAdmin(ctx);
    assertTrue(admin);
  }

  @Test
  void isAdmin_devuelveFalseCuandoRolNoEs1_oNoHayClaims() throws Exception {
    Context ctx = mock(Context.class);

    when(ctx.attribute("claims")).thenReturn(Map.of("rol", 2));
    assertFalse(callIsAdmin(ctx));

    when(ctx.attribute("claims")).thenReturn(null);
    assertFalse(callIsAdmin(ctx));
  }


  @Test
  void requireAuth_conClaimsEjecutaNext() throws Exception {
    Context ctx = mock(Context.class);
    AtomicBoolean nextCalled = new AtomicBoolean(false);
    Handler next = c -> nextCalled.set(true);

    Map<String, Object> claims = Map.of("rol", 1);

    try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
      Handler authHandler = c -> c.attribute("claims", claims);
      authMock.when(Auth::jwt).thenReturn(authHandler);

      when(ctx.attribute("claims")).thenReturn(claims);

      callRequireAuth(ctx, next);

      assertTrue(nextCalled.get(), "El handler 'next' debe ejecutarse cuando hay claims");
    }
  }

  @Test
  void requireAuth_sinClaimsNoEjecutaNext() throws Exception {
    Context ctx = mock(Context.class);
    AtomicBoolean nextCalled = new AtomicBoolean(false);
    Handler next = c -> nextCalled.set(true);

    try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
      Handler authHandler = c -> { };
      authMock.when(Auth::jwt).thenReturn(authHandler);

      when(ctx.attribute("claims")).thenReturn(null);

      callRequireAuth(ctx, next);

      assertFalse(nextCalled.get(), "El handler 'next' NO debe ejecutarse cuando no hay claims");
    }
  }


  @Test
  void requireAdmin_conRolAdminEjecutaNext() throws Exception {
    Context ctx = mock(Context.class);
    AtomicBoolean nextCalled = new AtomicBoolean(false);
    Handler next = c -> nextCalled.set(true);

    Map<String, Object> claims = Map.of("rol", 1);

    try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
      Handler authHandler = c -> c.attribute("claims", claims);
      authMock.when(Auth::jwt).thenReturn(authHandler);

      when(ctx.attribute("claims")).thenReturn(claims);

      callRequireAdmin(ctx, next);

      assertTrue(nextCalled.get(), "El handler 'next' debe ejecutarse para rol admin");
      verify(ctx, never()).status(403);
    }
  }

  @Test
  void requireAdmin_conRolNoAdminResponde403YNoEjecutaNext() throws Exception {
    Context ctx = mock(Context.class);
    AtomicBoolean nextCalled = new AtomicBoolean(false);
    Handler next = c -> nextCalled.set(true);

    Map<String, Object> claims = Map.of("rol", 2);

    try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
      Handler authHandler = c -> c.attribute("claims", claims);
      authMock.when(Auth::jwt).thenReturn(authHandler);

      when(ctx.attribute("claims")).thenReturn(claims);
      when(ctx.status(403)).thenReturn(ctx);

      callRequireAdmin(ctx, next);

      assertFalse(nextCalled.get(), "El handler 'next' NO debe ejecutarse para rol distinto de admin");
      verify(ctx).status(403);
      verify(ctx).json(any(Map.class));
    }
  }
}
