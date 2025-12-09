package com.arolineas.controller;

import com.aerolineas.controller.PaisController;
import com.aerolineas.dao.PaisDAO;
import com.aerolineas.dto.PaisDTOs;
import com.aerolineas.middleware.Auth;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PaisControllerTest {

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

    @Test
    void routes_registra_get_before_y_post() {
        PaisDAO dao = mock(PaisDAO.class);
        Javalin app = mock(Javalin.class);
        Handler authHandler = mock(Handler.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            PaisController controller = new PaisController(dao);
            controller.routes(app);

            verify(app).get(eq("/api/public/paises"), any());
            verify(app).before("/api/v1/paises", authHandler);
            verify(app).before("/api/v1/paises/*", authHandler);
            verify(app).post(eq("/api/v1/paises"), any());
        }
    }

    @Test
    void get_public_paises_devuelve_lista_json() throws Exception {
        PaisDAO dao = mock(PaisDAO.class);
        Javalin app = mock(Javalin.class);

        PaisController controller = new PaisController(dao);
        controller.routes(app);

        Handler getHandler = captureGet(app, "/api/public/paises");

        Context ctx = mock(Context.class);
        List<PaisDTOs.View> lista = List.of(
                new PaisDTOs.View(1L, "Guatemala"),
                new PaisDTOs.View(2L, "El Salvador")
        );
        when(dao.listAll()).thenReturn(lista);

        getHandler.handle(ctx);

        verify(dao).listAll();
        verify(ctx).json(lista);
    }

    @Test
    void post_admin_crea_pais_y_devuelve_201() throws Exception {
        PaisDAO dao = mock(PaisDAO.class);
        Javalin app = mock(Javalin.class);
        Handler authHandler = mock(Handler.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            PaisController controller = new PaisController(dao);
            controller.routes(app);

            Handler postHandler = capturePost(app, "/api/v1/paises");

            Context ctx = mock(Context.class);
            PaisDTOs.Create dto = new PaisDTOs.Create("Guatemala");
            when(ctx.bodyAsClass(PaisDTOs.Create.class)).thenReturn(dto);
            when(dao.create(dto)).thenReturn(5L);
            when(ctx.status(201)).thenReturn(ctx);

            postHandler.handle(ctx);

            verify(dao).create(dto);
            verify(ctx).status(201);
            verify(ctx).json(Map.of("idPais", 5L));
        }
    }

    @Test
    void constructor_por_defecto_se_ejecuta_sin_explotar() {
        new PaisController();
    }
}
