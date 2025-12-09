package com.aerolineas.middleware;

import com.aerolineas.util.JwtUtil;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import io.javalin.http.UnauthorizedResponse;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthTest {

  @Test
  void jwt_conTokenValido_adjuntaClaimsEnContext() throws Exception {
    Context ctx = mock(Context.class);
    when(ctx.header("Authorization")).thenReturn("Bearer token123");

    Claims claims = mock(Claims.class);
    when(claims.get("sub")).thenReturn("u1");
    when(claims.get("email")).thenReturn("user@example.com");
    when(claims.get("rol")).thenReturn(1);
    when(claims.get("name")).thenReturn("User Test");

    try (MockedStatic<JwtUtil> jwtMock = mockStatic(JwtUtil.class)) {
      jwtMock.when(() -> JwtUtil.parse("token123")).thenReturn(claims);

      Handler h = Auth.jwt();
      h.handle(ctx);

      ArgumentCaptor<Map<String,Object>> cap = ArgumentCaptor.forClass(Map.class);
      verify(ctx).attribute(eq("claims"), cap.capture());

      Map<String,Object> stored = cap.getValue();
      assertEquals("u1", stored.get("sub"));
      assertEquals("user@example.com", stored.get("email"));
      assertEquals(1, stored.get("rol"));
      assertEquals("User Test", stored.get("name"));
    }
  }

  @Test
  void jwt_sinToken_lanzaUnauthorized() {
    Context ctx = mock(Context.class);
    when(ctx.header("Authorization")).thenReturn(null);

    Handler h = Auth.jwt();

    assertThrows(UnauthorizedResponse.class, () -> h.handle(ctx));
  }

  @Test
  void adminOrEmpleado_opcionesNoRequiereToken() throws Exception {
    Context ctx = mock(Context.class);
    when(ctx.method()).thenReturn(HandlerType.OPTIONS);

    Handler h = Auth.adminOrEmpleado();

    assertDoesNotThrow(() -> h.handle(ctx));
  }

  @Test
  void adminOrEmpleado_conRolAdminPermiteContinuar() throws Exception {
    Context ctx = mock(Context.class);
    when(ctx.method()).thenReturn(HandlerType.GET);
    when(ctx.header("Authorization")).thenReturn("Bearer adminToken");

    Claims claims = mock(Claims.class);
    when(claims.get("sub")).thenReturn("u1");
    when(claims.get("email")).thenReturn("admin@example.com");
    when(claims.get("rol")).thenReturn(1); 
    when(claims.get("name")).thenReturn("Admin User");

    try (MockedStatic<JwtUtil> jwtMock = mockStatic(JwtUtil.class)) {
      jwtMock.when(() -> JwtUtil.parse("adminToken")).thenReturn(claims);

      Handler h = Auth.adminOrEmpleado();
      assertDoesNotThrow(() -> h.handle(ctx));
    }
  }

  @Test
  void adminOrEmpleado_conRolNoAdminLanzaUnauthorized() throws Exception {
    Context ctx = mock(Context.class);
    when(ctx.method()).thenReturn(HandlerType.GET);
    when(ctx.header("Authorization")).thenReturn("Bearer nonAdminToken");

    Claims claims = mock(Claims.class);
    when(claims.get("sub")).thenReturn("u2");
    when(claims.get("email")).thenReturn("empleado@example.com");
    when(claims.get("rol")).thenReturn("2");
    when(claims.get("name")).thenReturn("Empleado");

    try (MockedStatic<JwtUtil> jwtMock = mockStatic(JwtUtil.class)) {
      jwtMock.when(() -> JwtUtil.parse("nonAdminToken")).thenReturn(claims);

      Handler h = Auth.adminOrEmpleado();
      assertThrows(UnauthorizedResponse.class, () -> h.handle(ctx));
    }
  }

  @Test
  void claims_devuelveMapCuandoAttributeEsMap() {
    Context ctx = mock(Context.class);
    Map<String,Object> map = Map.of("k", "v");
    when(ctx.attribute("claims")).thenReturn(map);

    Map<String,Object> result = Auth.claims(ctx);

    assertSame(map, result);
  }

  @Test
  void claims_devuelveNullCuandoAttributeNoEsMap() {
    Context ctx = mock(Context.class);
    when(ctx.attribute("claims")).thenReturn("no-es-un-map");

    Map<String,Object> result = Auth.claims(ctx);

    assertNull(result);
  }
}
