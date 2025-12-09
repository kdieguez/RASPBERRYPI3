package com.aerolineas.controller;

import com.aerolineas.dao.UsuarioDAO;
import com.aerolineas.dto.AuthDTOs.LoginRequest;
import com.aerolineas.dto.AuthDTOs.LoginResponse;
import com.aerolineas.dto.AuthDTOs.RegisterRequest;
import com.aerolineas.model.Usuario;
import com.aerolineas.util.PasswordUtil;
import io.javalin.http.Context;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {  

    private Usuario buildUser(String rawPassword) {
        Usuario u = new Usuario();
        u.setIdUsuario(1L);
        u.setEmail("test@x.com");
        u.setNombres("Test");
        u.setApellidos("User");
        u.setIdRol(3); 
        u.setHabilitado(true);
        u.setContrasenaHash(PasswordUtil.hash(rawPassword));
        return u;
    }
   

    @Test
    @DisplayName("register OK crea usuario y devuelve LoginResponse 201")
    void register_ok() throws Exception {
        Context ctx = mock(Context.class);
        UsuarioDAO dao = mock(UsuarioDAO.class);
        AuthController controller = new AuthController(dao);

        RegisterRequest body = new RegisterRequest(
                "test@x.com",
                "12345678",
                "Test",
                "User"
        );

        when(ctx.bodyAsClass(RegisterRequest.class)).thenReturn(body);
        when(ctx.status(anyInt())).thenReturn(ctx);

        when(dao.findByEmail("test@x.com")).thenReturn(null);
        Usuario u = buildUser("12345678");
        when(dao.create(eq("test@x.com"), anyString(), eq("Test"), eq("User")))
                .thenReturn(u);

        controller.register(ctx);

        verify(dao).findByEmail("test@x.com");
        verify(dao).create(eq("test@x.com"), anyString(), eq("Test"), eq("User"));
        verify(ctx).status(201);
        verify(ctx).json(any(LoginResponse.class));
    }

    @Test
    @DisplayName("register devuelve 409 si email ya existe")
    void register_emailDuplicado() throws Exception {
        Context ctx = mock(Context.class);
        UsuarioDAO dao = mock(UsuarioDAO.class);
        AuthController controller = new AuthController(dao);

        RegisterRequest body = new RegisterRequest(
                "test@x.com",
                "12345678",
                "Test",
                "User"
        );

        when(ctx.bodyAsClass(RegisterRequest.class)).thenReturn(body);
        when(ctx.status(anyInt())).thenReturn(ctx);

        
        when(dao.findByEmail("test@x.com")).thenReturn(buildUser("12345678"));

        controller.register(ctx);

        verify(dao).findByEmail("test@x.com");
        verify(ctx).status(409);
        verify(ctx).json(Map.of("error", "email ya registrado"));
        verify(dao, never()).create(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("register devuelve 400 si email es inválido")
    void register_emailInvalido() throws Exception {
        Context ctx = mock(Context.class);
        UsuarioDAO dao = mock(UsuarioDAO.class);
        AuthController controller = new AuthController(dao);

        RegisterRequest body = new RegisterRequest(
                "sin-arroba",
                "12345678",
                "Test",
                "User"
        );

        when(ctx.bodyAsClass(RegisterRequest.class)).thenReturn(body);
        when(ctx.status(anyInt())).thenReturn(ctx);

        controller.register(ctx);

        verify(ctx).status(400);
        verify(ctx).json(Map.of("error", "email inválido"));
        verifyNoInteractions(dao);
    }

    @Test
    @DisplayName("register devuelve 400 si password es muy corta")
    void register_passwordCorta() throws Exception {
        Context ctx = mock(Context.class);
        UsuarioDAO dao = mock(UsuarioDAO.class);
        AuthController controller = new AuthController(dao);

        RegisterRequest body = new RegisterRequest(
                "test@x.com",
                "123", 
                "Test",
                "User"
        );

        when(ctx.bodyAsClass(RegisterRequest.class)).thenReturn(body);
        when(ctx.status(anyInt())).thenReturn(ctx);

        controller.register(ctx);

        verify(ctx).status(400);
        verify(ctx).json(Map.of("error", "password mínimo 8"));
        verifyNoInteractions(dao);
    }

    @Test
    @DisplayName("register devuelve 400 si nombres está vacío")
    void register_nombresRequeridos() throws Exception {
        Context ctx = mock(Context.class);
        UsuarioDAO dao = mock(UsuarioDAO.class);
        AuthController controller = new AuthController(dao);

        RegisterRequest body = new RegisterRequest(
            "test@x.com",
            "12345678",
            "   ", 
            "User"
        );

        when(ctx.bodyAsClass(RegisterRequest.class)).thenReturn(body);
        when(ctx.status(anyInt())).thenReturn(ctx);

        controller.register(ctx);

        verify(ctx).status(400);
        verify(ctx).json(Map.of("error", "nombres requeridos"));
        verifyNoInteractions(dao);
    }

    @Test
    @DisplayName("register devuelve 400 si apellidos está vacío")
    void register_apellidosRequeridos() throws Exception {
        Context ctx = mock(Context.class);
        UsuarioDAO dao = mock(UsuarioDAO.class);
        AuthController controller = new AuthController(dao);

        RegisterRequest body = new RegisterRequest(
            "test@x.com",
            "12345678",
            "Test",
            "   " 
        );

        when(ctx.bodyAsClass(RegisterRequest.class)).thenReturn(body);
        when(ctx.status(anyInt())).thenReturn(ctx);

        controller.register(ctx);

        verify(ctx).status(400);
        verify(ctx).json(Map.of("error", "apellidos requeridos"));
        verifyNoInteractions(dao);
    }

    @Test
    @DisplayName("login OK devuelve LoginResponse con 200")
    void login_ok() throws Exception {
        Context ctx = mock(Context.class);
        UsuarioDAO dao = mock(UsuarioDAO.class);
        AuthController controller = new AuthController(dao);

        LoginRequest body = new LoginRequest(
                "test@x.com",
                "12345678"
        );

        when(ctx.bodyAsClass(LoginRequest.class)).thenReturn(body);
        when(ctx.status(anyInt())).thenReturn(ctx);

        Usuario u = buildUser("12345678");
        when(dao.findByEmail("test@x.com")).thenReturn(u);

        controller.login(ctx);

        verify(dao).findByEmail("test@x.com");
        verify(ctx, never()).status(401);
        verify(ctx).json(any(LoginResponse.class));
    }

    @Test
    @DisplayName("login devuelve 401 si credenciales no son válidas")
    void login_credencialesInvalidas() throws Exception {
        Context ctx = mock(Context.class);
        UsuarioDAO dao = mock(UsuarioDAO.class);
        AuthController controller = new AuthController(dao);

        LoginRequest body = new LoginRequest(
                "test@x.com",
                "12345678"
        );

        when(ctx.bodyAsClass(LoginRequest.class)).thenReturn(body);
        when(ctx.status(anyInt())).thenReturn(ctx);

        when(dao.findByEmail("test@x.com")).thenReturn(null);

        controller.login(ctx);

        verify(dao).findByEmail("test@x.com");
        verify(ctx).status(401);
        verify(ctx).json(Map.of("error", "credenciales no válidas"));
    }

    @Test
    @DisplayName("login devuelve 400 si email es inválido")
    void login_emailInvalido() throws Exception {
        Context ctx = mock(Context.class);
        UsuarioDAO dao = mock(UsuarioDAO.class);
        AuthController controller = new AuthController(dao);

        LoginRequest body = new LoginRequest(
                "sin-arroba",
                "12345678"
        );

        when(ctx.bodyAsClass(LoginRequest.class)).thenReturn(body);
        when(ctx.status(anyInt())).thenReturn(ctx);

        controller.login(ctx);

        verify(ctx).status(400);
        verify(ctx).json(Map.of("error", "email inválido"));
        verifyNoInteractions(dao);
    }

    @Test
    @DisplayName("login devuelve 400 si password está vacío")
    void login_passwordRequerido() throws Exception {
        Context ctx = mock(Context.class);
        UsuarioDAO dao = mock(UsuarioDAO.class);
        AuthController controller = new AuthController(dao);

        LoginRequest body = new LoginRequest(
                "test@x.com",
                "   "
        );

        when(ctx.bodyAsClass(LoginRequest.class)).thenReturn(body);
        when(ctx.status(anyInt())).thenReturn(ctx);

        controller.login(ctx);

        verify(ctx).status(400);
        verify(ctx).json(Map.of("error", "password requerido"));
        verifyNoInteractions(dao);
    }    

    @Test
    @DisplayName("me devuelve 401 si no hay claims")
    void me_sinClaims() {
        Context ctx = mock(Context.class);
        AuthController controller = new AuthController(mock(UsuarioDAO.class));

        when(ctx.attribute("claims")).thenReturn(null);
        when(ctx.status(anyInt())).thenReturn(ctx);

        controller.me(ctx);

        verify(ctx).status(401);
        verify(ctx).json(Map.of("error", "no autenticado"));
    }

    @Test
    @DisplayName("me devuelve claims cuando existen")
    void me_conClaims() {
        Context ctx = mock(Context.class);
        AuthController controller = new AuthController(mock(UsuarioDAO.class));

        Map<String, Object> claims = Map.of("sub", "1", "email", "test@x.com");
        when(ctx.<Map<String, Object>>attribute("claims")).thenReturn(claims);

        controller.me(ctx);

        verify(ctx).json(claims);
        verify(ctx, never()).status(anyInt());
    }

    @Test
    @DisplayName("normEmail recorta y pone en minúsculas, y soporta null")
    void normEmail_branches() throws Exception {
        Method m = AuthController.class.getDeclaredMethod("normEmail", String.class);
        m.setAccessible(true);

        String res1 = (String) m.invoke(null, "  TEST@EXAMPLE.COM ");
        assertEquals("test@example.com", res1);

        String res2 = (String) m.invoke(null, new Object[]{null});
        assertNull(res2);
    }

    @Test
    void defaultConstructor_noRevienta() {
        AuthController controller = new AuthController();
        assertNotNull(controller);
    }

}
