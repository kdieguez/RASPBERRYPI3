package com.aerolineas.controller;

import com.aerolineas.dao.ComentarioDAO;
import com.aerolineas.dto.ComentarioDTO;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ComentarioControllerTest {

    @Test
    @DisplayName("Constructor por defecto no revienta")
    void defaultConstructor_ok() {
        ComentarioController controller = new ComentarioController();
        assertNotNull(controller);
    }

    @Test
    @DisplayName("GET /api/public/vuelos/{id}/comentarios lista comentarios del vuelo")
    void listComentarios_ok() throws Exception {
        Javalin app = mock(Javalin.class);
        Context ctx = mock(Context.class);
        ComentarioDAO dao = mock(ComentarioDAO.class);
        Handler jwtHandler = mock(Handler.class);

        final Handler[] handlerHolder = new Handler[1];
        when(app.get(eq("/api/public/vuelos/{id}/comentarios"), any())).thenAnswer(inv -> {
            handlerHolder[0] = inv.getArgument(1);
            return app;
        });

        @SuppressWarnings("unchecked")
        Validator<Long> val = mock(Validator.class);
        when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
        when(val.get()).thenReturn(5L);

        List<ComentarioDTO.View> lista = List.of(
                new ComentarioDTO.View(
                        1L,
                        5L,
                        2L,
                        "Autor",
                        "Comentario",
                        LocalDateTime.now(),
                        null,
                        null
                )
        );
        when(dao.listarPublic(5L)).thenReturn(lista);

        ComentarioController controller = new ComentarioController(dao, jwtHandler);
        controller.routes(app);

        assertNotNull(handlerHolder[0], "Handler GET no registrado");
        handlerHolder[0].handle(ctx);

        verify(dao).listarPublic(5L);
        verify(ctx).json(lista);
    }

    @Test
    @DisplayName("POST /api/v1/vuelos/{id}/comentarios crea comentario correctamente")
    void crearComentario_ok() throws Exception {
        Javalin app = mock(Javalin.class);
        Context ctx = mock(Context.class);
        ComentarioDAO dao = mock(ComentarioDAO.class);
        Handler jwtHandler = mock(Handler.class);

        final Handler[] handlerHolder = new Handler[1];
        when(app.post(eq("/api/v1/vuelos/{id}/comentarios"), any())).thenAnswer(inv -> {
            handlerHolder[0] = inv.getArgument(1);
            return app;
        });

        doAnswer(inv -> null).when(jwtHandler).handle(any());

        Map<String, Object> claims = Map.of("idUsuario", 7L);
        when(ctx.attribute("claims")).thenReturn(claims);

        @SuppressWarnings("unchecked")
        Validator<Long> val = mock(Validator.class);
        when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
        when(val.get()).thenReturn(10L);

        ComentarioDTO.Create body = new ComentarioDTO.Create("Hola mundo", null);
        when(ctx.bodyAsClass(ComentarioDTO.Create.class)).thenReturn(body);

        when(ctx.status(anyInt())).thenReturn(ctx);
        when(dao.crear(10L, 7L, "Hola mundo", null)).thenReturn(99L);

        ComentarioController controller = new ComentarioController(dao, jwtHandler);
        controller.routes(app);

        assertNotNull(handlerHolder[0], "Handler POST no registrado");
        handlerHolder[0].handle(ctx);

        verify(jwtHandler).handle(ctx);
        verify(dao).crear(10L, 7L, "Hola mundo", null);
        verify(ctx).status(201);
        verify(ctx).json(Map.of("idComentario", 99L));
    }

    @Test
    @DisplayName("POST comentario en blanco devuelve 400 y no llama al DAO")
    void crearComentario_bodyInvalido() throws Exception {
        Javalin app = mock(Javalin.class);
        Context ctx = mock(Context.class);
        ComentarioDAO dao = mock(ComentarioDAO.class);
        Handler jwtHandler = mock(Handler.class);

        final Handler[] handlerHolder = new Handler[1];
        when(app.post(eq("/api/v1/vuelos/{id}/comentarios"), any())).thenAnswer(inv -> {
            handlerHolder[0] = inv.getArgument(1);
            return app;
        });

        doAnswer(inv -> null).when(jwtHandler).handle(any());

        Map<String, Object> claims = Map.of("idUsuario", 7L);
        when(ctx.attribute("claims")).thenReturn(claims);

        @SuppressWarnings("unchecked")
        Validator<Long> val = mock(Validator.class);
        when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
        when(val.get()).thenReturn(10L);

        ComentarioDTO.Create body = new ComentarioDTO.Create("   ", null);
        when(ctx.bodyAsClass(ComentarioDTO.Create.class)).thenReturn(body);
        when(ctx.status(anyInt())).thenReturn(ctx);

        ComentarioController controller = new ComentarioController(dao, jwtHandler);
        controller.routes(app);

        assertNotNull(handlerHolder[0]);
        handlerHolder[0].handle(ctx);

        verify(dao, never()).crear(anyLong(), anyLong(), anyString(), any());
        verify(ctx).status(400);
        verify(ctx).json(Map.of("error", "comentario es requerido"));
    }

    @Test
    @DisplayName("POST con claims inválidos devuelve 401 No autenticado")
    void crearComentario_userIdInvalido() throws Exception {
        Javalin app = mock(Javalin.class);
        Context ctx = mock(Context.class);
        ComentarioDAO dao = mock(ComentarioDAO.class);
        Handler jwtHandler = mock(Handler.class);

        final Handler[] handlerHolder = new Handler[1];
        when(app.post(eq("/api/v1/vuelos/{id}/comentarios"), any())).thenAnswer(inv -> {
            handlerHolder[0] = inv.getArgument(1);
            return app;
        });

        doAnswer(inv -> null).when(jwtHandler).handle(any());

        Map<String, Object> claims = Map.of("idUsuario", "xxx");
        when(ctx.attribute("claims")).thenReturn(claims);

        @SuppressWarnings("unchecked")
        Validator<Long> val = mock(Validator.class);
        when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
        when(val.get()).thenReturn(10L);

        ComentarioDTO.Create body = new ComentarioDTO.Create("Comentario válido", null);
        when(ctx.bodyAsClass(ComentarioDTO.Create.class)).thenReturn(body);
        when(ctx.status(anyInt())).thenReturn(ctx);

        ComentarioController controller = new ComentarioController(dao, jwtHandler);
        controller.routes(app);

        assertNotNull(handlerHolder[0]);
        handlerHolder[0].handle(ctx);

        verify(dao, never()).crear(anyLong(), anyLong(), anyString(), any());
        verify(ctx).status(401);
        verify(ctx).json(Map.of("error", "No autenticado"));
    }

    @Test
    @DisplayName("POST si DAO lanza excepción devuelve 400 con mensaje de error")
    void crearComentario_daoLanzaExcepcion() throws Exception {
        Javalin app = mock(Javalin.class);
        Context ctx = mock(Context.class);
        ComentarioDAO dao = mock(ComentarioDAO.class);
        Handler jwtHandler = mock(Handler.class);

        final Handler[] handlerHolder = new Handler[1];
        when(app.post(eq("/api/v1/vuelos/{id}/comentarios"), any())).thenAnswer(inv -> {
            handlerHolder[0] = inv.getArgument(1);
            return app;
        });

        doAnswer(inv -> null).when(jwtHandler).handle(any());

        Map<String, Object> claims = Map.of("idUsuario", 7L);
        when(ctx.attribute("claims")).thenReturn(claims);

        @SuppressWarnings("unchecked")
        Validator<Long> val = mock(Validator.class);
        when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
        when(val.get()).thenReturn(10L);

        ComentarioDTO.Create body = new ComentarioDTO.Create("Comentario válido", null);
        when(ctx.bodyAsClass(ComentarioDTO.Create.class)).thenReturn(body);
        when(ctx.status(anyInt())).thenReturn(ctx);

        when(dao.crear(10L, 7L, "Comentario válido", null))
                .thenThrow(new RuntimeException("boom"));

        ComentarioController controller = new ComentarioController(dao, jwtHandler);
        controller.routes(app);

        assertNotNull(handlerHolder[0]);
        handlerHolder[0].handle(ctx);

        verify(dao).crear(10L, 7L, "Comentario válido", null);
        verify(ctx).status(400);
        verify(ctx).json(Map.of("error", "boom"));
    }
}
