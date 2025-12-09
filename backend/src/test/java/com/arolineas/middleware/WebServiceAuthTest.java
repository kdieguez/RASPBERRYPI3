package com.arolineas.middleware; 

import com.aerolineas.dao.UsuarioDAO;
import com.aerolineas.model.Usuario;
import com.aerolineas.middleware.WebServiceAuth;
import com.aerolineas.util.PasswordUtil;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import io.javalin.http.UnauthorizedResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WebServiceAuthTest {

  private Handler handler() {
    return WebServiceAuth.validate();
  }

  @Test
  void options_seIgnoraSinValidarNada() throws Exception {
    Context ctx = mock(Context.class);
    when(ctx.method()).thenReturn(HandlerType.OPTIONS);

    handler().handle(ctx);

    verify(ctx, never()).attribute(eq("claims"), any());
  }

  @Test
  void faltaEmailOLPassword_lanzaUnauthorized() {
    Context ctx = mock(Context.class);
    when(ctx.method()).thenReturn(HandlerType.POST);
    when(ctx.header("X-WebService-Email")).thenReturn(null);
    when(ctx.header("X-WebService-Password")).thenReturn("secret");

    assertThrows(UnauthorizedResponse.class, () -> handler().handle(ctx));
  }

  @Test
  void usuarioNoEncontrado_lanzaUnauthorized() {
    Context ctx = mock(Context.class);
    when(ctx.method()).thenReturn(HandlerType.POST);
    when(ctx.header("X-WebService-Email")).thenReturn("ws@test.com");
    when(ctx.header("X-WebService-Password")).thenReturn("secret");

    try (MockedConstruction<UsuarioDAO> mocked = mockConstruction(UsuarioDAO.class,
        (mockDao, context) -> when(mockDao.findByEmail("ws@test.com")).thenReturn(null))) {

      assertThrows(UnauthorizedResponse.class, () -> handler().handle(ctx));

      List<UsuarioDAO> instancias = mocked.constructed();
      assertEquals(1, instancias.size());
      verify(instancias.get(0)).findByEmail("ws@test.com");
    }
  }

  @Test
  void usuarioDeshabilitado_lanzaUnauthorized() {
    Context ctx = mock(Context.class);
    when(ctx.method()).thenReturn(HandlerType.POST);
    when(ctx.header("X-WebService-Email")).thenReturn("ws@test.com");
    when(ctx.header("X-WebService-Password")).thenReturn("secret");

    Usuario u = new Usuario();
    u.setIdUsuario(10L);
    u.setEmail("ws@test.com");
    u.setIdRol(2);
    u.setHabilitado(false);
    u.setContrasenaHash("hash");

    try (MockedConstruction<UsuarioDAO> mocked = mockConstruction(UsuarioDAO.class,
        (mockDao, context) -> when(mockDao.findByEmail("ws@test.com")).thenReturn(u))) {

      assertThrows(UnauthorizedResponse.class, () -> handler().handle(ctx));
    }
  }

  @Test
  void usuarioNoEsRolWebService_lanzaUnauthorized() {
    Context ctx = mock(Context.class);
    when(ctx.method()).thenReturn(HandlerType.POST);
    when(ctx.header("X-WebService-Email")).thenReturn("ws@test.com");
    when(ctx.header("X-WebService-Password")).thenReturn("secret");

    Usuario u = new Usuario();
    u.setIdUsuario(10L);
    u.setEmail("ws@test.com");
    u.setIdRol(3);            
    u.setHabilitado(true);
    u.setContrasenaHash("hash");

    try (MockedConstruction<UsuarioDAO> mocked = mockConstruction(UsuarioDAO.class,
        (mockDao, context) -> when(mockDao.findByEmail("ws@test.com")).thenReturn(u))) {

      assertThrows(UnauthorizedResponse.class, () -> handler().handle(ctx));
    }
  }

  @Test
  void passwordInvalida_lanzaUnauthorized() {
    Context ctx = mock(Context.class);
    when(ctx.method()).thenReturn(HandlerType.POST);
    when(ctx.header("X-WebService-Email")).thenReturn("ws@test.com");
    when(ctx.header("X-WebService-Password")).thenReturn("secret");

    Usuario u = new Usuario();
    u.setIdUsuario(10L);
    u.setEmail("ws@test.com");
    u.setIdRol(2);
    u.setHabilitado(true);
    u.setContrasenaHash("hash-guardado");

    try (MockedConstruction<UsuarioDAO> mocked = mockConstruction(UsuarioDAO.class,
             (mockDao, context) -> when(mockDao.findByEmail("ws@test.com")).thenReturn(u));
         MockedStatic<PasswordUtil> pwdMock = mockStatic(PasswordUtil.class)) {

      pwdMock.when(() -> PasswordUtil.verify("secret", "hash-guardado"))
             .thenReturn(false);

      assertThrows(UnauthorizedResponse.class, () -> handler().handle(ctx));
    }
  }

  @Test
  void emailSeNormaliza_ySiTodoOk_seGuardaClaimsEnContext() throws Exception {
    Context ctx = mock(Context.class);
    when(ctx.method()).thenReturn(HandlerType.POST);
    
    when(ctx.header("X-WebService-Email")).thenReturn("  WS@TEST.COM  ");
    when(ctx.header("X-WebService-Password")).thenReturn("secret");

    Usuario u = new Usuario();
    u.setIdUsuario(123L);
    u.setEmail("ws@test.com");
    u.setIdRol(2);
    u.setHabilitado(true);
    u.setContrasenaHash("hash-ok");
    u.setNombres("Web");
    u.setApellidos("Service");

    try (MockedConstruction<UsuarioDAO> mocked = mockConstruction(UsuarioDAO.class,
             (mockDao, context) -> when(mockDao.findByEmail("ws@test.com")).thenReturn(u));
         MockedStatic<PasswordUtil> pwdMock = mockStatic(PasswordUtil.class)) {

      pwdMock.when(() -> PasswordUtil.verify("secret", "hash-ok"))
             .thenReturn(true);

      handler().handle(ctx);

      
      UsuarioDAO dao = mocked.constructed().get(0);
      verify(dao).findByEmail("ws@test.com");

      
      @SuppressWarnings("unchecked")
      ArgumentCaptor<Map<String, Object>> claimsCap = ArgumentCaptor.forClass(Map.class);
      verify(ctx).attribute(eq("claims"), claimsCap.capture());

      Map<String, Object> claims = claimsCap.getValue();
      assertNotNull(claims);

      assertEquals("123", claims.get("sub"));
      assertEquals("123", claims.get("idUsuario"));
      assertEquals("ws@test.com", claims.get("email"));
      assertEquals(2, claims.get("rol"));
      assertEquals("Web Service", claims.get("name"));
    }
  }
}
