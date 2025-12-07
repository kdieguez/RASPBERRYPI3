package com.aerolineas.controller;

import com.aerolineas.dao.UsuarioDAO;
import com.aerolineas.dto.UsuarioAdminDTOs;
import com.aerolineas.model.Usuario;
import io.javalin.http.Context;
import io.javalin.validation.BodyValidator;
import kotlin.jvm.functions.Function1;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminUsuarioControllerTest {

    private void stubChecksMap(BodyValidator<Map<String, Object>> validator,
                               Map<String, Object> body) {
        when(validator.check(any(), anyString())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Function1<Map<String, Object>, Boolean> fn =
                    (Function1<Map<String, Object>, Boolean>) inv.getArgument(0);
            fn.invoke(body);          
            return validator;         
        });
    }

    private void stubChecksUpdate(BodyValidator<UsuarioAdminDTOs.UpdateAdmin> validator,
                                  UsuarioAdminDTOs.UpdateAdmin body) {
        when(validator.check(any(), anyString())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Function1<UsuarioAdminDTOs.UpdateAdmin, Boolean> fn =
                    (Function1<UsuarioAdminDTOs.UpdateAdmin, Boolean>) inv.getArgument(0);
            fn.invoke(body);          
            return validator;
        });
    }

    

    @Test
    @DisplayName("createWs crea usuario WS cuando email no existe y usa rol por defecto 2")
    void createWs_creaUsuarioWs_ok() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.status(anyInt())).thenReturn(ctx);

        @SuppressWarnings("unchecked")
        BodyValidator<Map<String, Object>> validator = mock(BodyValidator.class);

        Map<String, Object> body = Map.of(
                "email", "  TEST@MAIL.com ",
                "password", "supersecreto",
                "nombres", "Kat",
                "apellidos", "Diemora"
        );

        when(ctx.bodyValidator(Map.class)).thenReturn((BodyValidator) validator);
        stubChecksMap(validator, body);
        when(validator.get()).thenReturn(body);

        try (MockedConstruction<UsuarioDAO> mocked = mockConstruction(
                UsuarioDAO.class,
                (daoMock, context) -> {
                    when(daoMock.findByEmail("test@mail.com")).thenReturn(null);

                    Usuario u = new Usuario();
                    u.setIdUsuario(123L);
                    u.setEmail("test@mail.com");
                    u.setNombres("Kat");
                    u.setApellidos("Diemora");
                    u.setIdRol(2);
                    when(daoMock.createWithRole(
                            eq("test@mail.com"),
                            anyString(),
                            eq("Kat"),
                            eq("Diemora"),
                            eq(2)
                    )).thenReturn(u);
                }
        )) {
            AdminUsuarioController controller = new AdminUsuarioController();
            UsuarioDAO daoMock = mocked.constructed().get(0);

            controller.createWs(ctx);

            verify(daoMock).findByEmail("test@mail.com");
            verify(daoMock).createWithRole(
                    eq("test@mail.com"),
                    anyString(),
                    eq("Kat"),
                    eq("Diemora"),
                    eq(2)
            );

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> jsonCap = ArgumentCaptor.forClass(Map.class);
            verify(ctx).status(201);
            verify(ctx).json(jsonCap.capture());

            Map<String, Object> json = jsonCap.getValue();
            assertEquals(123L, json.get("idUsuario"));
            assertEquals("test@mail.com", json.get("email"));
            assertEquals("Kat", json.get("nombres"));
            assertEquals("Diemora", json.get("apellidos"));
            assertEquals(2, json.get("idRol"));
        }
    }

    @Test
    @DisplayName("createWs usa idRol del body cuando es válido")
    void createWs_conIdRolPersonalizado_usaEseRol() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.status(anyInt())).thenReturn(ctx);

        @SuppressWarnings("unchecked")
        BodyValidator<Map<String, Object>> validator = mock(BodyValidator.class);

        Map<String, Object> body = Map.of(
                "email", "user@ws.com",
                "password", "supersecreto",
                "nombres", "Agencia",
                "apellidos", "WS",
                "idRol", 5
        );

        when(ctx.bodyValidator(Map.class)).thenReturn((BodyValidator) validator);
        stubChecksMap(validator, body);
        when(validator.get()).thenReturn(body);

        try (MockedConstruction<UsuarioDAO> mocked = mockConstruction(
                UsuarioDAO.class,
                (daoMock, context) -> {
                    when(daoMock.findByEmail("user@ws.com")).thenReturn(null);

                    Usuario u = new Usuario();
                    u.setIdUsuario(50L);
                    u.setEmail("user@ws.com");
                    u.setNombres("Agencia");
                    u.setApellidos("WS");
                    u.setIdRol(5);
                    when(daoMock.createWithRole(
                            eq("user@ws.com"),
                            anyString(),
                            eq("Agencia"),
                            eq("WS"),
                            eq(5)
                    )).thenReturn(u);
                }
        )) {
            AdminUsuarioController controller = new AdminUsuarioController();
            UsuarioDAO daoMock = mocked.constructed().get(0);

            controller.createWs(ctx);

            verify(daoMock).createWithRole(
                    eq("user@ws.com"),
                    anyString(),
                    eq("Agencia"),
                    eq("WS"),
                    eq(5)
            );

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> jsonCap = ArgumentCaptor.forClass(Map.class);
            verify(ctx).status(201);
            verify(ctx).json(jsonCap.capture());

            Map<String, Object> json = jsonCap.getValue();
            assertEquals(5, json.get("idRol"));
        }
    }

        @Test
    @DisplayName("createWs ejecuta branch FALSE de validación cuando falta email")
    void createWs_validacionEmailBranchFalse() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.status(anyInt())).thenReturn(ctx);

        @SuppressWarnings("unchecked")
        BodyValidator<Map<String, Object>> validator = mock(BodyValidator.class);

        Map<String, Object> body = Map.of(
                
                "password", "supersecreto",
                "nombres", "SinMail",
                "apellidos", "Test"
        );

        when(ctx.bodyValidator(Map.class)).thenReturn((BodyValidator) validator);

        when(validator.check(any(), anyString())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Function1<Map<String, Object>, Boolean> fn =
                    (Function1<Map<String, Object>, Boolean>) inv.getArgument(0);
            
            fn.invoke(body);
            return validator;
        });

        when(validator.get()).thenReturn(body);

        try (MockedConstruction<UsuarioDAO> mocked = mockConstruction(
                UsuarioDAO.class,
                (daoMock, mc) -> {
                    
                    when(daoMock.findByEmail("null")).thenReturn(null);

                    
                    Usuario u = new Usuario();
                    u.setIdUsuario(999L);
                    u.setEmail("null");
                    u.setNombres("SinMail");
                    u.setApellidos("Test");
                    u.setIdRol(2);

                    when(daoMock.createWithRole(
                            anyString(), anyString(), anyString(), anyString(), anyInt()
                    )).thenReturn(u);
                }
        )) {
            AdminUsuarioController controller = new AdminUsuarioController();
            controller.createWs(ctx);

            
            
        }
    }


    @Test
    @DisplayName("createWs con idRol inválido mantiene rol por defecto 2")
    void createWs_idRolInvalido_usaRolPorDefecto2() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.status(anyInt())).thenReturn(ctx);

        @SuppressWarnings("unchecked")
        BodyValidator<Map<String, Object>> validator = mock(BodyValidator.class);

        Map<String, Object> body = Map.of(
                "email", "inv@ws.com",
                "password", "supersecreto",
                "nombres", "Inv",
                "apellidos", "Rol",
                "idRol", "noNumero"
        );

        when(ctx.bodyValidator(Map.class)).thenReturn((BodyValidator) validator);
        stubChecksMap(validator, body);
        when(validator.get()).thenReturn(body);

        try (MockedConstruction<UsuarioDAO> mocked = mockConstruction(
                UsuarioDAO.class,
                (daoMock, context) -> {
                    when(daoMock.findByEmail("inv@ws.com")).thenReturn(null);

                    Usuario u = new Usuario();
                    u.setIdUsuario(60L);
                    u.setEmail("inv@ws.com");
                    u.setNombres("Inv");
                    u.setApellidos("Rol");
                    u.setIdRol(2);
                    when(daoMock.createWithRole(
                            eq("inv@ws.com"),
                            anyString(),
                            eq("Inv"),
                            eq("Rol"),
                            eq(2)
                    )).thenReturn(u);
                }
        )) {
            AdminUsuarioController controller = new AdminUsuarioController();
            UsuarioDAO daoMock = mocked.constructed().get(0);

            controller.createWs(ctx);

            verify(daoMock).createWithRole(
                    eq("inv@ws.com"),
                    anyString(),
                    eq("Inv"),
                    eq("Rol"),
                    eq(2)
            );
        }
    }

    @Test
    @DisplayName("createWs responde 409 cuando email ya está registrado")
    void createWs_emailDuplicado_409() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.status(anyInt())).thenReturn(ctx);

        @SuppressWarnings("unchecked")
        BodyValidator<Map<String, Object>> validator = mock(BodyValidator.class);

        Map<String, Object> body = Map.of(
                "email", "ya@existe.com",
                "password", "supersecreto",
                "nombres", "Nombre",
                "apellidos", "Apellido"
        );

        when(ctx.bodyValidator(Map.class)).thenReturn((BodyValidator) validator);
        stubChecksMap(validator, body);
        when(validator.get()).thenReturn(body);

        try (MockedConstruction<UsuarioDAO> mocked = mockConstruction(
                UsuarioDAO.class,
                (daoMock, context) -> {
                    Usuario u = new Usuario();
                    u.setIdUsuario(1L);
                    u.setEmail("ya@existe.com");
                    when(daoMock.findByEmail("ya@existe.com")).thenReturn(u);
                }
        )) {
            AdminUsuarioController controller = new AdminUsuarioController();
            controller.createWs(ctx);

            UsuarioDAO daoMock = mocked.constructed().get(0);

            verify(daoMock).findByEmail("ya@existe.com");
            verify(daoMock, never()).createWithRole(anyString(), anyString(), anyString(), anyString(), anyInt());

            verify(ctx).status(409);
            verify(ctx).json(Map.of("error", "email ya registrado"));
        }
    }

    

    @Test
    @DisplayName("list devuelve items con offset y limit")
    void list_ok() throws Exception {
        Context ctx = mock(Context.class);

        when(ctx.queryParam("q")).thenReturn("kat");
        when(ctx.queryParam("offset")).thenReturn("10");
        when(ctx.queryParam("limit")).thenReturn("5");

        try (MockedConstruction<UsuarioDAO> mocked = mockConstruction(
                UsuarioDAO.class,
                (daoMock, context) -> {
                    UsuarioAdminDTOs.Row fila = new UsuarioAdminDTOs.Row(
                            1L, "a@b.com", "N", "A", 2, "WS", 1
                    );
                    when(daoMock.adminList("kat", 10, 5)).thenReturn(List.of(fila));
                }
        )) {
            AdminUsuarioController controller = new AdminUsuarioController();
            controller.list(ctx);

            UsuarioDAO daoMock = mocked.constructed().get(0);
            verify(daoMock).adminList("kat", 10, 5);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> jsonCap = ArgumentCaptor.forClass(Map.class);
            verify(ctx).json(jsonCap.capture());

            Map<String, Object> json = jsonCap.getValue();
            assertEquals(10, json.get("offset"));
            assertEquals(5, json.get("limit"));
            assertTrue(json.get("items") instanceof List<?>);
            assertEquals(1, ((List<?>) json.get("items")).size());
        }
    }

    @Test
    @DisplayName("list usa valores por defecto cuando no hay offset/limit")
    void list_sinOffsetLimit_usaDefault() throws Exception {
        Context ctx = mock(Context.class);

        when(ctx.queryParam("q")).thenReturn(null);
        when(ctx.queryParam("offset")).thenReturn(null);
        when(ctx.queryParam("limit")).thenReturn(null);

        try (MockedConstruction<UsuarioDAO> mocked = mockConstruction(
                UsuarioDAO.class,
                (daoMock, context) -> {
                    UsuarioAdminDTOs.Row fila = new UsuarioAdminDTOs.Row(
                            1L, "x@y.com", "N", "A", 2, "WS", 1
                    );
                    when(daoMock.adminList(null, 0, 25)).thenReturn(List.of(fila));
                }
        )) {
            AdminUsuarioController controller = new AdminUsuarioController();
            controller.list(ctx);

            UsuarioDAO daoMock = mocked.constructed().get(0);
            verify(daoMock).adminList(null, 0, 25);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> jsonCap = ArgumentCaptor.forClass(Map.class);
            verify(ctx).json(jsonCap.capture());

            Map<String, Object> json = jsonCap.getValue();
            assertEquals(0, json.get("offset"));
            assertEquals(25, json.get("limit"));
        }
    }

    @Test
    @DisplayName("list usa offset por defecto cuando parámetro es inválido")
    void list_offsetInvalido_usaDefault() throws Exception {
        Context ctx = mock(Context.class);

        when(ctx.queryParam("q")).thenReturn("kat");
        when(ctx.queryParam("offset")).thenReturn("noNumero");
        when(ctx.queryParam("limit")).thenReturn("5");

        try (MockedConstruction<UsuarioDAO> mocked = mockConstruction(
                UsuarioDAO.class,
                (daoMock, context) -> {
                    UsuarioAdminDTOs.Row fila = new UsuarioAdminDTOs.Row(
                            1L, "a@b.com", "N", "A", 2, "WS", 1
                    );
                    when(daoMock.adminList("kat", 0, 5)).thenReturn(List.of(fila));
                }
        )) {
            AdminUsuarioController controller = new AdminUsuarioController();
            controller.list(ctx);

            UsuarioDAO daoMock = mocked.constructed().get(0);
            verify(daoMock).adminList("kat", 0, 5);
        }
    }

    

    @Test
    @DisplayName("get devuelve usuario cuando existe")
    void get_encontrado() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.pathParam("id")).thenReturn("42");

        try (MockedConstruction<UsuarioDAO> mocked = mockConstruction(
                UsuarioDAO.class,
                (daoMock, context) -> {
                    UsuarioAdminDTOs.View v = new UsuarioAdminDTOs.View(
                            42L, "a@b.com", "N", "A", 2, "WS", 1,
                            "2000-01-01", 1L, "P123"
                    );
                    when(daoMock.adminGet(42L)).thenReturn(v);
                }
        )) {
            AdminUsuarioController controller = new AdminUsuarioController();
            controller.get(ctx);

            UsuarioDAO daoMock = mocked.constructed().get(0);
            verify(daoMock).adminGet(42L);
            verify(ctx).json(any(UsuarioAdminDTOs.View.class));
            verify(ctx, never()).status(404);
        }
    }

    @Test
    @DisplayName("get responde 404 cuando usuario no existe")
    void get_noEncontrado() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.pathParam("id")).thenReturn("99");
        when(ctx.status(anyInt())).thenReturn(ctx);

        try (MockedConstruction<UsuarioDAO> mocked = mockConstruction(
                UsuarioDAO.class,
                (daoMock, context) -> when(daoMock.adminGet(99L)).thenReturn(null)
        )) {
            AdminUsuarioController controller = new AdminUsuarioController();
            controller.get(ctx);

            UsuarioDAO daoMock = mocked.constructed().get(0);
            verify(daoMock).adminGet(99L);
            verify(ctx).status(404);
            verify(ctx).json(Map.of("error", "no encontrado"));
        }
    }

    

    @Test
    @DisplayName("update valida y delega a adminUpdate, respondiendo ok=true")
    void update_ok() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.pathParam("id")).thenReturn("7");

        @SuppressWarnings("unchecked")
        BodyValidator<UsuarioAdminDTOs.UpdateAdmin> validator = mock(BodyValidator.class);
        UsuarioAdminDTOs.UpdateAdmin body = new UsuarioAdminDTOs.UpdateAdmin(
                "Nombre", "Apellido", "nuevaPass", 2, 1,
                "2000-01-01", 1L, "P123"
        );

        when(ctx.bodyValidator(UsuarioAdminDTOs.UpdateAdmin.class)).thenReturn(validator);
        stubChecksUpdate(validator, body);
        when(validator.get()).thenReturn(body);

        try (MockedConstruction<UsuarioDAO> mocked = mockConstruction(
                UsuarioDAO.class,
                (daoMock, context) -> doNothing().when(daoMock).adminUpdate(7L, body)
        )) {
            AdminUsuarioController controller = new AdminUsuarioController();
            controller.update(ctx);

            UsuarioDAO daoMock = mocked.constructed().get(0);
            verify(daoMock).adminUpdate(7L, body);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> jsonCap = ArgumentCaptor.forClass(Map.class);
            verify(ctx).json(jsonCap.capture());

            Map<String, Object> json = jsonCap.getValue();
            assertEquals(Boolean.TRUE, json.get("ok"));
        }
    }

    @Test
    @DisplayName("update ejecuta branch FALSE de validación cuando datos son inválidos")
    void update_validacionesBranchFalse() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.pathParam("id")).thenReturn("8");

        @SuppressWarnings("unchecked")
        BodyValidator<UsuarioAdminDTOs.UpdateAdmin> validator = mock(BodyValidator.class);

        UsuarioAdminDTOs.UpdateAdmin bodyInvalido = new UsuarioAdminDTOs.UpdateAdmin(
                null,                        
                "Apellido",
                null,
                2,
                1,
                "2000-01-01",
                1L,
                "P123456789012345678901"     
        );

        when(ctx.bodyValidator(UsuarioAdminDTOs.UpdateAdmin.class)).thenReturn(validator);
        stubChecksUpdate(validator, bodyInvalido);
        when(validator.get()).thenReturn(bodyInvalido);

        try (MockedConstruction<UsuarioDAO> mocked = mockConstruction(
                UsuarioDAO.class,
                (daoMock, context) -> doNothing().when(daoMock).adminUpdate(8L, bodyInvalido)
        )) {
            AdminUsuarioController controller = new AdminUsuarioController();
            controller.update(ctx);
            
        }
    }
}
