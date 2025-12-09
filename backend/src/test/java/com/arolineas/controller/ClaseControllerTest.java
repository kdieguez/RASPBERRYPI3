package com.aerolineas.controller;

import com.aerolineas.dao.ClaseDAO;
import com.aerolineas.dto.ClaseDTOs;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ClaseControllerTest {

    @Test
    @DisplayName("Constructor por defecto no revienta")
    void defaultConstructor_ok() {
        ClaseController controller = new ClaseController();
        assertNotNull(controller);
    }

    @Test
    @DisplayName("GET /api/public/clases sin headers de WS no llama al validador")
    void listClases_sinHeaders_noValidaWs() throws Exception {
        Javalin app = mock(Javalin.class);
        Context ctx = mock(Context.class);
        ClaseDAO dao = mock(ClaseDAO.class);
        Handler wsHandler = mock(Handler.class);

        final Handler[] handlerHolder = new Handler[1];
        when(app.get(eq("/api/public/clases"), any())).thenAnswer(inv -> {
            handlerHolder[0] = inv.getArgument(1);
            return app;
        });

        when(ctx.header("X-WebService-Email")).thenReturn(null);
        when(ctx.header("X-WebService-Password")).thenReturn(null);

        List<ClaseDTOs.View> lista = List.of(
                new ClaseDTOs.View(1, "Económica")
        );
        when(dao.listAll()).thenReturn(lista);

        ClaseController controller = new ClaseController(dao, wsHandler);
        controller.routes(app);

        assertNotNull(handlerHolder[0], "Handler de /api/public/clases no registrado");
        handlerHolder[0].handle(ctx);

        verify(wsHandler, never()).handle(any());
        verify(dao).listAll();
        verify(ctx).json(lista);
    }

    @Test
    @DisplayName("GET /api/public/clases con headers de WS llama al validador")
    void listClases_conHeaders_validaWs() throws Exception {
        Javalin app = mock(Javalin.class);
        Context ctx = mock(Context.class);
        ClaseDAO dao = mock(ClaseDAO.class);
        Handler wsHandler = mock(Handler.class);

        final Handler[] handlerHolder = new Handler[1];
        when(app.get(eq("/api/public/clases"), any())).thenAnswer(inv -> {
            handlerHolder[0] = inv.getArgument(1);
            return app;
        });

        when(ctx.header("X-WebService-Email")).thenReturn("ws@test.com");
        when(ctx.header("X-WebService-Password")).thenReturn("secret");

        List<ClaseDTOs.View> lista = List.of(
                new ClaseDTOs.View(2, "Ejecutiva")
        );
        when(dao.listAll()).thenReturn(lista);

        ClaseController controller = new ClaseController(dao, wsHandler);
        controller.routes(app);

        assertNotNull(handlerHolder[0]);
        handlerHolder[0].handle(ctx);

        verify(wsHandler).handle(ctx);
        verify(dao).listAll();
        verify(ctx).json(lista);
    }
}
