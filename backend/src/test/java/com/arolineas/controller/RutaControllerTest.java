package com.arolineas.controller;

import com.aerolineas.controller.RutaController;
import com.aerolineas.dao.RutaDAO;
import com.aerolineas.dto.RutaDTOs;
import com.aerolineas.middleware.Auth;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class RutaControllerTest {

    // Helpers para capturar handlers registrados en Javalin
    private Handler captureGet(Javalin app, String path) {
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        verify(app).get(eq(path), captor.capture());
        return captor.getValue();
    }

    private Handler capturePost(Javalin app, String path) {
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        verify(app).post(eq(path), captor.capture());
        return captor.getValue();
    }

    private Handler capturePut(Javalin app, String path) {
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        verify(app).put(eq(path), captor.capture());
        return captor.getValue();
    }

    @Test
    void routes_configuraBeforeConAuthAdminEmpleado() {
        RutaDAO dao = mock(RutaDAO.class);
        Javalin app = mock(Javalin.class);
        Handler authHandler = mock(Handler.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            RutaController controller = new RutaController(dao);
            controller.routes(app);

            verify(app).before("/api/v1/rutas", authHandler);
            verify(app).before("/api/v1/rutas/*", authHandler);
        }
    }

    @Test
    void listAll_devuelveJsonConListaDeRutas() throws Exception {
        RutaDAO dao = mock(RutaDAO.class);
        Javalin app = mock(Javalin.class);

        RutaController controller = new RutaController(dao);
        controller.routes(app);

        Handler handler = captureGet(app, "/api/v1/rutas");

        Context ctx = mock(Context.class);

        @SuppressWarnings("unchecked")
        List<RutaDTOs.View> rutas =
                (List<RutaDTOs.View>)(List<?>) List.of(new Object());

        when(dao.listAll()).thenReturn(rutas);

        handler.handle(ctx);

        verify(dao).listAll();
        verify(ctx).json(rutas);
    }

    @Test
    void post_creaRutaYDevuelve201ConId() throws Exception {
        RutaDAO dao = mock(RutaDAO.class);
        Javalin app = mock(Javalin.class);

        RutaController controller = new RutaController(dao);
        controller.routes(app);

        Handler handler = capturePost(app, "/api/v1/rutas");

        Context ctx = mock(Context.class);
        RutaDTOs.Create dto = mock(RutaDTOs.Create.class);

        when(ctx.bodyAsClass(RutaDTOs.Create.class)).thenReturn(dto);
        when(dao.create(dto)).thenReturn(42L);
        when(ctx.status(201)).thenReturn(ctx);

        handler.handle(ctx);

        verify(dao).create(dto);
        verify(ctx).status(201);
        verify(ctx).json(Map.of("idRuta", 42L));
    }

    @Test
    void toggle_putLlamaDaoYDevuelve204() throws Exception {
        RutaDAO dao = mock(RutaDAO.class);
        Javalin app = mock(Javalin.class);

        RutaController controller = new RutaController(dao);
        controller.routes(app);

        Handler handler = capturePut(app, "/api/v1/rutas/{id}/toggle");

        Context ctx = mock(Context.class);
        when(ctx.pathParam("id")).thenReturn("7");
        when(ctx.status(204)).thenReturn(ctx);

        handler.handle(ctx);

        verify(dao).toggleActiva(7L);
        verify(ctx).status(204);
    }

    @Test
    void constructorPorDefecto_noLanzaExcepcion() {
        new com.aerolineas.controller.RutaController();
    }

}
