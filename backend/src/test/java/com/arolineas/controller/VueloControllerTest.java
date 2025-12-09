package com.arolineas.controller;

import com.aerolineas.controller.VueloController;
import com.aerolineas.dao.VueloDAO;
import com.aerolineas.dto.VueloDTO;
import com.aerolineas.middleware.Auth;
import com.aerolineas.middleware.WebServiceAuth;
import com.aerolineas.service.NotificacionesService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.validation.Validator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*; 
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.aerolineas.util.EstadosVuelo.CANCELADO;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class VueloControllerTest {

    private Handler captureGetHandler(String path, VueloDAO dao) {
        NotificacionesService notifySvc = mock(NotificacionesService.class);
        VueloController controller = new VueloController(dao, notifySvc);

        Javalin app = mock(Javalin.class);
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);

        when(app.get(eq(path), captor.capture())).thenReturn(app);

        controller.routes(app);
        return captor.getValue();
    }

    @Test
    void constructorPorDefecto_noRevienta() {
        new com.aerolineas.controller.VueloController();
    }

    @Test
    void listarPublic_sinWebService_ok() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        Handler h = captureGetHandler("/api/public/vuelos", dao);

        Context ctx = mock(Context.class);
        when(ctx.header("X-WebService-Email")).thenReturn(null);
        when(ctx.header("X-WebService-Password")).thenReturn(null);

        h.handle(ctx);

        verify(dao).listarVuelosPublic();
        verify(ctx).json(any());
    }

    @Test
    void listarPublic_conWebServiceHeaders_invocaValidadorWS() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);
        VueloController controller = new VueloController(dao, notifySvc);

        Javalin app = mock(Javalin.class);
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        when(app.get(eq("/api/public/vuelos"), captor.capture())).thenReturn(app);

        controller.routes(app);
        Handler h = captor.getValue();

        Context ctx = mock(Context.class);
        when(ctx.header("X-WebService-Email")).thenReturn("ws@test.com");
        when(ctx.header("X-WebService-Password")).thenReturn("secret");

        Handler wsHandler = mock(Handler.class);
        try (MockedStatic<WebServiceAuth> wsMock = mockStatic(WebServiceAuth.class)) {
            wsMock.when(WebServiceAuth::validate).thenReturn(wsHandler);

            h.handle(ctx);

            verify(wsHandler).handle(ctx);
        }
    }

    @Test
    void listarConEscalaPublic_ok() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        Handler h = captureGetHandler("/api/public/vuelos/con-escala", dao);

        Context ctx = mock(Context.class);
        when(ctx.header("X-WebService-Email")).thenReturn(null);
        when(ctx.header("X-WebService-Password")).thenReturn(null);

        h.handle(ctx);

        verify(dao).listarVuelosConEscalaPublic();
        verify(ctx).json(any());
    }

    @Test
    void listarConEscalaPublic_errorDevuelve500() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        Handler h = captureGetHandler("/api/public/vuelos/con-escala", dao);

        Context ctx = mock(Context.class);
        when(ctx.header("X-WebService-Email")).thenReturn(null);
        when(ctx.header("X-WebService-Password")).thenReturn(null);
        when(dao.listarVuelosConEscalaPublic()).thenThrow(new RuntimeException("boom"));

        when(ctx.status(500)).thenReturn(ctx);

        h.handle(ctx);

        verify(ctx).status(500);
        verify(ctx).json(argThat(body ->
                body instanceof Map &&
                        "boom".equals(((Map<?, ?>) body).get("error"))
        ));
    }

    @Test
    void obtenerConEscalaPublic_404_siNoExiste() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);
        VueloController controller = new VueloController(dao, notifySvc);

        Javalin app = mock(Javalin.class);
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        when(app.get(eq("/api/public/vuelos/con-escala/{id}"), captor.capture())).thenReturn(app);

        controller.routes(app);
        Handler h = captor.getValue();

        Context ctx = mock(Context.class);
        when(ctx.header("X-WebService-Email")).thenReturn(null);
        when(ctx.header("X-WebService-Password")).thenReturn(null);

        @SuppressWarnings("unchecked")
        Validator<Long> val = (Validator<Long>) mock(Validator.class);
        when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
        when(val.get()).thenReturn(5L);

        when(dao.obtenerVueloConEscalaPublic(5L)).thenReturn(null);
        when(ctx.status(404)).thenReturn(ctx);

        h.handle(ctx);

        verify(ctx).status(404);
        verify(ctx).json(argThat(body ->
                body instanceof Map &&
                        "Vuelo con escala no encontrado".equals(((Map<?, ?>) body).get("error"))
        ));
    }

    @Test
    void obtenerConEscalaPublic_error500() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);
        VueloController controller = new VueloController(dao, notifySvc);

        Javalin app = mock(Javalin.class);
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        when(app.get(eq("/api/public/vuelos/con-escala/{id}"), captor.capture())).thenReturn(app);

        controller.routes(app);
        Handler h = captor.getValue();

        Context ctx = mock(Context.class);
        when(ctx.header("X-WebService-Email")).thenReturn(null);
        when(ctx.header("X-WebService-Password")).thenReturn(null);

        @SuppressWarnings("unchecked")
        Validator<Long> val = (Validator<Long>) mock(Validator.class);
        when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
        when(val.get()).thenReturn(7L);

        when(dao.obtenerVueloConEscalaPublic(7L)).thenThrow(new RuntimeException("boom-escala"));
        when(ctx.status(500)).thenReturn(ctx);

        h.handle(ctx);

        verify(ctx).status(500);
        verify(ctx).json(argThat(body ->
                body instanceof Map &&
                        "boom-escala".equals(((Map<?, ?>) body).get("error"))
        ));
    }

    @Test
    void obtenerPublic_404_siNoExiste() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);
        VueloController controller = new VueloController(dao, notifySvc);

        Javalin app = mock(Javalin.class);
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        when(app.get(eq("/api/public/vuelos/{id}"), captor.capture())).thenReturn(app);

        controller.routes(app);
        Handler h = captor.getValue();

        Context ctx = mock(Context.class);
        when(ctx.header("X-WebService-Email")).thenReturn(null);
        when(ctx.header("X-WebService-Password")).thenReturn(null);

        @SuppressWarnings("unchecked")
        Validator<Long> val = (Validator<Long>) mock(Validator.class);
        when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
        when(val.get()).thenReturn(9L);

        when(dao.obtenerVueloPublic(9L)).thenReturn(null);
        when(ctx.status(404)).thenReturn(ctx);

        h.handle(ctx);

        verify(ctx).status(404);
        verify(ctx).json(argThat(body ->
                body instanceof Map &&
                        "Vuelo no encontrado".equals(((Map<?, ?>) body).get("error"))
        ));
    }

    @Test
    void obtenerPublic_ok_devuelveJson() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);
        VueloController controller = new VueloController(dao, notifySvc);

        Javalin app = mock(Javalin.class);
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        when(app.get(eq("/api/public/vuelos/{id}"), captor.capture())).thenReturn(app);

        controller.routes(app);
        Handler h = captor.getValue();

        Context ctx = mock(Context.class);
        when(ctx.header("X-WebService-Email")).thenReturn(null);
        when(ctx.header("X-WebService-Password")).thenReturn(null);

        @SuppressWarnings("unchecked")
        Validator<Long> val = (Validator<Long>) mock(Validator.class);
        when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
        when(val.get()).thenReturn(12L);

        VueloDTO.View vuelo = new VueloDTO.View(
                12L,
                "COD-12",
                1L,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(2),
                true,
                List.of(),
                List.of()
        );

        when(dao.obtenerVueloPublic(12L)).thenReturn(vuelo);

        h.handle(ctx);

        verify(ctx).json(vuelo);
    }

    @Test
    void listarV1_sinWebService_ok() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        Handler h = captureGetHandler("/api/v1/vuelos", dao);

        Context ctx = mock(Context.class);
        when(ctx.header("X-WebService-Email")).thenReturn(null);
        when(ctx.header("X-WebService-Password")).thenReturn(null);

        h.handle(ctx);

        verify(dao).listarVuelosPublic();
        verify(ctx).json(any());
    }

    @Test
    void listarV1_conWebServiceHeaders_invocaValidadorWS() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);
        VueloController controller = new VueloController(dao, notifySvc);

        Javalin app = mock(Javalin.class);
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        when(app.get(eq("/api/v1/vuelos"), captor.capture())).thenReturn(app);

        controller.routes(app);
        Handler h = captor.getValue();

        Context ctx = mock(Context.class);
        when(ctx.header("X-WebService-Email")).thenReturn("ws@test.com");
        when(ctx.header("X-WebService-Password")).thenReturn("secret");

        Handler wsHandler = mock(Handler.class);
        try (MockedStatic<WebServiceAuth> wsMock = mockStatic(WebServiceAuth.class)) {
            wsMock.when(WebServiceAuth::validate).thenReturn(wsHandler);

            h.handle(ctx);

            verify(wsHandler).handle(ctx);
        }
    }

    @Test
    void listarConEscalaV1_ok() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        Handler h = captureGetHandler("/api/v1/vuelos/con-escala", dao);

        Context ctx = mock(Context.class);
        when(ctx.header("X-WebService-Email")).thenReturn(null);
        when(ctx.header("X-WebService-Password")).thenReturn(null);

        h.handle(ctx);

        verify(dao).listarVuelosConEscalaPublic();
        verify(ctx).json(any());
    }

    @Test
    void obtenerConEscalaV1_ok_devuelveJson() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);
        VueloController controller = new VueloController(dao, notifySvc);

        Javalin app = mock(Javalin.class);
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        when(app.get(eq("/api/v1/vuelos/con-escala/{id}"), captor.capture())).thenReturn(app);

        controller.routes(app);
        Handler h = captor.getValue();

        Context ctx = mock(Context.class);
        when(ctx.header("X-WebService-Email")).thenReturn(null);
        when(ctx.header("X-WebService-Password")).thenReturn(null);

        @SuppressWarnings("unchecked")
        Validator<Long> val = (Validator<Long>) mock(Validator.class);
        when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
        when(val.get()).thenReturn(50L);

        VueloDTO.VueloConEscalaView vuelo = new VueloDTO.VueloConEscalaView(
                50L,
                "COD-50",
                null,
                null,
                "GUA",
                "SAL",
                "Guatemala",
                "El Salvador",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                true,
                null,
                null,
                List.of(),
                List.of()
        );

        when(dao.obtenerVueloConEscalaPublic(50L)).thenReturn(vuelo);

        h.handle(ctx);

        verify(ctx).json(vuelo);
    }

    @Test
    void obtenerV1_404_siNoExiste() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);
        VueloController controller = new VueloController(dao, notifySvc);

        Javalin app = mock(Javalin.class);
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        when(app.get(eq("/api/v1/vuelos/{id}"), captor.capture())).thenReturn(app);

        controller.routes(app);
        Handler h = captor.getValue();

        Context ctx = mock(Context.class);
        when(ctx.header("X-WebService-Email")).thenReturn(null);
        when(ctx.header("X-WebService-Password")).thenReturn(null);

        @SuppressWarnings("unchecked")
        Validator<Long> val = (Validator<Long>) mock(Validator.class);
        when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
        when(val.get()).thenReturn(30L);

        when(dao.obtenerVueloPublic(30L)).thenReturn(null);
        when(ctx.status(404)).thenReturn(ctx);

        h.handle(ctx);

        verify(ctx).status(404);
        verify(ctx).json(argThat(body ->
                body instanceof Map &&
                        "Vuelo no encontrado".equals(((Map<?, ?>) body).get("error"))
        ));
    }

    @Test
    void obtenerV1_ok_devuelveJson() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);
        VueloController controller = new VueloController(dao, notifySvc);

        Javalin app = mock(Javalin.class);
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        when(app.get(eq("/api/v1/vuelos/{id}"), captor.capture())).thenReturn(app);

        controller.routes(app);
        Handler h = captor.getValue();

        Context ctx = mock(Context.class);
        when(ctx.header("X-WebService-Email")).thenReturn(null);
        when(ctx.header("X-WebService-Password")).thenReturn(null);

        @SuppressWarnings("unchecked")
        Validator<Long> val = (Validator<Long>) mock(Validator.class);
        when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
        when(val.get()).thenReturn(31L);

        VueloDTO.View vuelo = new VueloDTO.View(
                31L,
                "COD-31",
                1L,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(2),
                true,
                List.of(),
                List.of()
        );

        when(dao.obtenerVueloPublic(31L)).thenReturn(vuelo);

        h.handle(ctx);

        verify(ctx).json(vuelo);
    }

    @Test
    void admin_listarVuelos_ok() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.get(eq("/api/v1/admin/vuelos"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);

            VueloDTO.View v = new VueloDTO.View(
                    1L,
                    "COD-1",
                    1L,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusHours(1),
                    true,
                    List.of(),
                    List.of()
            );
            List<VueloDTO.View> lista = List.of(v);

            when(dao.listarVuelos(false)).thenReturn(lista);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(dao).listarVuelos(false);
            verify(ctx).json(lista);
        }
    }

    @Test
    void admin_obtenerVuelo_404() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.get(eq("/api/v1/admin/vuelos/{id}"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(40L);
            when(dao.obtenerVueloAdmin(40L)).thenReturn(null);
            when(ctx.status(404)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(ctx).status(404);
            verify(ctx).json(argThat(body ->
                    body instanceof Map &&
                            "Vuelo no encontrado".equals(((Map<?, ?>) body).get("error"))
            ));
        }
    }

    @Test
    void admin_obtenerVuelo_ok() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.get(eq("/api/v1/admin/vuelos/{id}"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(41L);

            VueloDTO.ViewAdmin vuelo = new VueloDTO.ViewAdmin(
                    41L,
                    "COD-41",
                    1L,
                    "GUA",
                    "SAL",
                    LocalDateTime.now(),
                    LocalDateTime.now().plusHours(1),
                    true,
                    null,
                    null,
                    List.of(),
                    List.of(),
                    null,
                    null,
                    "Guatemala",
                    "El Salvador"
            );

            when(dao.obtenerVueloAdmin(41L)).thenReturn(vuelo);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(ctx).json(vuelo);
        }
    }

    @Test
    void admin_crearVuelo_ok() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.post(eq("/api/v1/admin/vuelos"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);

            VueloDTO.Create dto = new VueloDTO.Create(
                    "COD-99",
                    1L,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusHours(2),
                    List.of(),
                    List.of(),
                    true
            );
            when(ctx.bodyAsClass(VueloDTO.Create.class)).thenReturn(dto);
            when(dao.crearVueloReturnId(dto)).thenReturn(99L);
            when(ctx.status(201)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(dao).crearVueloReturnId(dto);
            verify(ctx).status(201);
            verify(ctx).json(argThat(body ->
                    body instanceof Map &&
                            ((Map<?, ?>) body).get("idVuelo").equals(99L)
            ));
        }
    }

    @Test
    void admin_crearVuelo_errorDevuelve400() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.post(eq("/api/v1/admin/vuelos"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);

            VueloDTO.Create dto = new VueloDTO.Create(
                    "COD-ERR",
                    1L,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusHours(1),
                    List.of(),
                    List.of(),
                    true
            );
            when(ctx.bodyAsClass(VueloDTO.Create.class)).thenReturn(dto);
            when(dao.crearVueloReturnId(dto)).thenThrow(new RuntimeException("fallo"));
            when(ctx.status(400)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(ctx).status(400);
            verify(ctx).json(argThat(body ->
                    body instanceof Map &&
                            "fallo".equals(((Map<?, ?>) body).get("error"))
            ));
        }
    }

    @Test
    void crearSimple_ok() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.post(eq("/api/v1/vuelos"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);

            VueloDTO.Create dto = new VueloDTO.Create(
                    "COD-SIMPLE",
                    2L,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusHours(1),
                    List.of(),
                    List.of(),
                    true
            );
            when(ctx.bodyAsClass(VueloDTO.Create.class)).thenReturn(dto);
            when(ctx.status(201)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(dao).crearVuelo(dto);
            verify(ctx).status(201);
        }
    }

    @Test
    void actualizarEstado_idEstadoNull_400() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.put(eq("/api/v1/vuelos/{id}/estado"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(70L);

            VueloDTO.EstadoUpdate dto = new VueloDTO.EstadoUpdate(null, "motivo");
            when(ctx.bodyAsClass(VueloDTO.EstadoUpdate.class)).thenReturn(dto);
            when(ctx.status(400)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(ctx).status(400);
            verify(ctx).json(argThat(body ->
                    body instanceof Map &&
                            "idEstado es requerido".equals(((Map<?, ?>) body).get("error"))
            ));
        }
    }

    @Test
    void actualizarEstado_cancelarSinMotivo_400() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.put(eq("/api/v1/vuelos/{id}/estado"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(71L);

            VueloDTO.EstadoUpdate dto = new VueloDTO.EstadoUpdate(CANCELADO, "  ");
            when(ctx.bodyAsClass(VueloDTO.EstadoUpdate.class)).thenReturn(dto);
            when(ctx.status(400)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(ctx).status(400);
            verify(ctx).json(argThat(body ->
                    body instanceof Map &&
                            "motivo es requerido para cancelar".equals(((Map<?, ?>) body).get("error"))
            ));
        }
    }

    @Test
    void actualizarEstado_cancelado_ok() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.put(eq("/api/v1/vuelos/{id}/estado"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(72L);

            VueloDTO.EstadoUpdate dto = new VueloDTO.EstadoUpdate(CANCELADO, "motivo-cancel");
            when(ctx.bodyAsClass(VueloDTO.EstadoUpdate.class)).thenReturn(dto);
            when(ctx.status(204)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(dao).actualizarEstado(72L, CANCELADO, "motivo-cancel");
            verify(ctx).status(204);
        }
    }

    @Test
void admin_obtenerVueloConEscala_ok() throws Exception {
    VueloDAO dao = mock(VueloDAO.class);
    NotificacionesService notifySvc = mock(NotificacionesService.class);

    try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
        Handler authHandler = mock(Handler.class);
        authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

        VueloController controller = new VueloController(dao, notifySvc);
        Javalin app = mock(Javalin.class);

        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        when(app.get(eq("/api/v1/admin/vuelos/con-escala/{id}"), captor.capture()))
                .thenReturn(app);

        controller.routes(app);
        Handler h = captor.getValue();

        Context ctx = mock(Context.class);
        @SuppressWarnings("unchecked")
        Validator<Long> val = (Validator<Long>) mock(Validator.class);
        when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
        when(val.get()).thenReturn(80L);

        VueloDTO.VueloConEscalaView vuelo = new VueloDTO.VueloConEscalaView(
                80L,
                "COD-80",
                null,
                null,
                "GUA",
                "SAL",
                "Guatemala",
                "El Salvador",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                true,
                null,
                null,
                List.of(),
                List.of()
        );

        when(dao.obtenerVueloConEscala(80L)).thenReturn(vuelo);

        h.handle(ctx);

        verify(authHandler).handle(ctx);
        verify(ctx).json(vuelo);
    }
}

    @Test
    void admin_listarVuelosConEscala_ok() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.get(eq("/api/v1/admin/vuelos/con-escala"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);

            when(dao.listarVuelosConEscala()).thenReturn(List.of());

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(dao).listarVuelosConEscala();
            verify(ctx).json(any());
        }
    }

    @Test
    void admin_listarVuelosConEscala_error500() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.get(eq("/api/v1/admin/vuelos/con-escala"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);

            when(dao.listarVuelosConEscala()).thenThrow(new RuntimeException("boom-admin-escala"));
            when(ctx.status(500)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(ctx).status(500);
            verify(ctx).json(argThat(body ->
                    body instanceof Map &&
                            "boom-admin-escala".equals(((Map<?, ?>) body).get("error"))
            ));
        }
    }

    @Test
    void admin_obtenerVueloConEscala_404() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.get(eq("/api/v1/admin/vuelos/con-escala/{id}"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(81L);

            when(dao.obtenerVueloConEscala(81L)).thenReturn(null);
            when(ctx.status(404)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(ctx).status(404);
            verify(ctx).json(argThat(body ->
                    body instanceof Map &&
                            "Vuelo con escala no encontrado".equals(((Map<?, ?>) body).get("error"))
            ));
        }
    }

    @Test
    void admin_obtenerVueloConEscala_error500() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.get(eq("/api/v1/admin/vuelos/con-escala/{id}"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(82L);

            when(dao.obtenerVueloConEscala(82L)).thenThrow(new RuntimeException("boom-admin-escala-2"));
            when(ctx.status(500)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(ctx).status(500);
            verify(ctx).json(argThat(body ->
                    body instanceof Map &&
                            "boom-admin-escala-2".equals(((Map<?, ?>) body).get("error"))
            ));
        }
    }

    @Test
    void admin_crearVueloConEscala_ok() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.post(eq("/api/v1/admin/vuelos/con-escala"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);

            when(ctx.bodyAsClass(any(Class.class))).thenReturn(null);
            when(dao.crearVueloConEscala(any())).thenReturn(500L);
            when(ctx.status(201)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(dao).crearVueloConEscala(any());
            verify(ctx).status(201);
            verify(ctx).json(argThat(body ->
                    body instanceof Map &&
                            ((Map<?, ?>) body).get("idVueloConEscala").equals(500L)
            ));
        }
    }

    @Test
    void admin_crearVueloConEscala_error400() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.post(eq("/api/v1/admin/vuelos/con-escala"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);

            when(ctx.bodyAsClass(any(Class.class))).thenReturn(null);
            when(dao.crearVueloConEscala(any())).thenThrow(new RuntimeException("err-escala"));
            when(ctx.status(400)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(ctx).status(400);
            verify(ctx).json(argThat(body ->
                    body instanceof Map &&
                            "err-escala".equals(((Map<?, ?>) body).get("error"))
            ));
        }
    }

    @Test
    void actualizarEstado_sqlNoExiste_404() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.put(eq("/api/v1/vuelos/{id}/estado"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(90L);

            VueloDTO.EstadoUpdate dto = new VueloDTO.EstadoUpdate(2, "motivo");
            when(ctx.bodyAsClass(VueloDTO.EstadoUpdate.class)).thenReturn(dto);

            doThrow(new SQLException("vuelo no existe"))
                .when(dao).actualizarEstado(90L, 2, "motivo");  

            when(ctx.status(404)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(ctx).status(404);
            verify(ctx).json(argThat(body ->
                    body instanceof Map &&
                            "vuelo no existe".equals(((Map<?, ?>) body).get("error"))
            ));
        }
    }

    @Test
    void actualizarEstado_sqlCancelado_409() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.put(eq("/api/v1/vuelos/{id}/estado"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(91L);

            VueloDTO.EstadoUpdate dto = new VueloDTO.EstadoUpdate(CANCELADO, "motivo-ok");
            when(ctx.bodyAsClass(VueloDTO.EstadoUpdate.class)).thenReturn(dto);

            doThrow(new SQLException("ya cancelado"))
            .when(dao).actualizarEstado(91L, CANCELADO, "motivo-ok");
            when(ctx.status(409)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(ctx).status(409);
            verify(ctx).json(argThat(body ->
                    body instanceof Map &&
                            "ya cancelado".equals(((Map<?, ?>) body).get("error"))
            ));
        }
    }

    @Test
    void actualizarEstado_sqlOtro_400() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.put(eq("/api/v1/vuelos/{id}/estado"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(92L);

            VueloDTO.EstadoUpdate dto = new VueloDTO.EstadoUpdate(3, "motivo-x");
            when(ctx.bodyAsClass(VueloDTO.EstadoUpdate.class)).thenReturn(dto);

            doThrow(new SQLException("otro-error"))
            .when(dao).actualizarEstado(92L, 3, "motivo-x");
            when(ctx.status(400)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(ctx).status(400);
            verify(ctx).json(argThat(body ->
                    body instanceof Map &&
                            "otro-error".equals(((Map<?, ?>) body).get("error"))
            ));
        }
    }

    @Test
    void roundtrip_reqNull_400() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.post(eq("/api/v1/vuelos/roundtrip"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);

            when(ctx.bodyAsClass(any(Class.class))).thenReturn(null);
            when(ctx.status(400)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(ctx).status(400);
            verify(ctx).json(argThat(body ->
                    body instanceof Map &&
                            "Se requieren objetos 'ida' y 'regreso'".equals(((Map<?, ?>) body).get("error"))
            ));
        }
    }

    @Test
    void link_reqNull_400() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.post(eq("/api/v1/vuelos/link"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);

            when(ctx.bodyAsClass(any(Class.class))).thenReturn(null);
            when(ctx.status(400)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(ctx).status(400);
            verify(ctx).json(argThat(body ->
                    body instanceof Map &&
                            "idIda e idRegreso son requeridos".equals(((Map<?, ?>) body).get("error"))
            ));
        }
    }

    @Test
    void unlink_ok_204() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.put(eq("/api/v1/vuelos/{id}/unlink"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(100L);
            when(ctx.status(204)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(dao).desvincularPareja(100L);
            verify(ctx).status(204);
        }
    }

    @Test
    void unlink_sqlError_400() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.put(eq("/api/v1/vuelos/{id}/unlink"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(101L);

            doThrow(new SQLException("unlink-error")).when(dao).desvincularPareja(101L);
            when(ctx.status(400)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(ctx).status(400);
            verify(ctx).json(argThat(body ->
                    body instanceof Map &&
                            "unlink-error".equals(((Map<?, ?>) body).get("error"))
            ));
        }
    }

        @Test
    void admin_actualizarVuelo_codigoRequerido_400() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.put(eq("/api/v1/admin/vuelos/{id}"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(200L);

            VueloDTO.UpdateAdmin dto = mock(VueloDTO.UpdateAdmin.class);
            when(ctx.bodyAsClass(VueloDTO.UpdateAdmin.class)).thenReturn(dto);
            when(dto.codigo()).thenReturn(null);
            when(ctx.status(400)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(ctx).status(400);
            verify(ctx).json(argThat(body ->
                    body instanceof Map &&
                            "Código requerido".equals(((Map<?, ?>) body).get("error"))
            ));
        }
    }

    @Test
    void admin_actualizarVuelo_fechaSalidaMayor_400() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.put(eq("/api/v1/admin/vuelos/{id}"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(201L);

            VueloDTO.UpdateAdmin dto = mock(VueloDTO.UpdateAdmin.class);
            when(ctx.bodyAsClass(VueloDTO.UpdateAdmin.class)).thenReturn(dto);

            LocalDateTime salida = LocalDateTime.now().plusHours(3);
            LocalDateTime llegada = LocalDateTime.now();

            when(dto.codigo()).thenReturn("COD-201");
            when(dto.fechaSalida()).thenReturn(salida);
            when(dto.fechaLlegada()).thenReturn(llegada);
            when(ctx.status(400)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(ctx).status(400);
            verify(ctx).json(argThat(body ->
                    body instanceof Map &&
                            "La salida debe ser menor que la llegada".equals(((Map<?, ?>) body).get("error"))
            ));
        }
    }

    @Test
    void admin_actualizarVuelo_sinClases_400() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.put(eq("/api/v1/admin/vuelos/{id}"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(202L);

            VueloDTO.UpdateAdmin dto = mock(VueloDTO.UpdateAdmin.class);
            when(ctx.bodyAsClass(VueloDTO.UpdateAdmin.class)).thenReturn(dto);

            LocalDateTime salida = LocalDateTime.now();
            LocalDateTime llegada = LocalDateTime.now().plusHours(1);

            when(dto.codigo()).thenReturn("COD-202");
            when(dto.fechaSalida()).thenReturn(salida);
            when(dto.fechaLlegada()).thenReturn(llegada);
            when(dto.clases()).thenReturn(null); 
            when(ctx.status(400)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(ctx).status(400);
            verify(ctx).json(argThat(body ->
                    body instanceof Map &&
                            "Debe indicar al menos una clase".equals(((Map<?, ?>) body).get("error"))
            ));
        }
    }

    @Test
    void admin_actualizarVuelo_sinMotivoCambio_400() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.put(eq("/api/v1/admin/vuelos/{id}"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(203L);

            VueloDTO.UpdateAdmin dto = mock(VueloDTO.UpdateAdmin.class);
            when(ctx.bodyAsClass(VueloDTO.UpdateAdmin.class)).thenReturn(dto);

            LocalDateTime salida = LocalDateTime.now();
            LocalDateTime llegada = LocalDateTime.now().plusHours(1);

            when(dto.codigo()).thenReturn("COD-203");
            when(dto.fechaSalida()).thenReturn(salida);
            when(dto.fechaLlegada()).thenReturn(llegada);

            List<VueloDTO.ClaseConfig> clases =
            List.of(new VueloDTO.ClaseConfig(1, 10, 100.0));
            when(dto.clases()).thenReturn(clases);

            when(dto.motivoCambio()).thenReturn("   "); 
            when(ctx.status(400)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(ctx).status(400);
            verify(ctx).json(argThat(body ->
                    body instanceof Map &&
                            "motivoCambio es requerido para modificar".equals(((Map<?, ?>) body).get("error"))
            ));
        }
    }

    @Test
    void admin_actualizarVuelo_ok_204_yNotifica() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.put(eq("/api/v1/admin/vuelos/{id}"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(204L);

            VueloDTO.UpdateAdmin dto = mock(VueloDTO.UpdateAdmin.class);
            when(ctx.bodyAsClass(VueloDTO.UpdateAdmin.class)).thenReturn(dto);

            LocalDateTime salida = LocalDateTime.now();
            LocalDateTime llegada = LocalDateTime.now().plusHours(1);

            when(dto.codigo()).thenReturn("COD-204");
            when(dto.fechaSalida()).thenReturn(salida);
            when(dto.fechaLlegada()).thenReturn(llegada);

            List<VueloDTO.ClaseConfig> clases =
            List.of(new VueloDTO.ClaseConfig(1, 10, 100.0));
            when(dto.clases()).thenReturn(clases);

            when(dto.motivoCambio()).thenReturn("ajuste horario");
            when(ctx.status(204)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(dao).actualizarVueloAdmin(204L, dto);
            verify(ctx).status(204);
            verify(notifySvc, timeout(200)).notificarCambio(204L, "ajuste horario");
        }
    }

    @Test
    void admin_actualizarVuelo_sqlCancelado_409() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.put(eq("/api/v1/admin/vuelos/{id}"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(205L);

            VueloDTO.UpdateAdmin dto = mock(VueloDTO.UpdateAdmin.class);
            when(ctx.bodyAsClass(VueloDTO.UpdateAdmin.class)).thenReturn(dto);

            LocalDateTime salida = LocalDateTime.now();
            LocalDateTime llegada = LocalDateTime.now().plusHours(1);

            when(dto.codigo()).thenReturn("COD-205");
            when(dto.fechaSalida()).thenReturn(salida);
            when(dto.fechaLlegada()).thenReturn(llegada);

            List<VueloDTO.ClaseConfig> clases =
            List.of(new VueloDTO.ClaseConfig(1, 10, 100.0));    
            when(dto.clases()).thenReturn(clases);

            when(dto.motivoCambio()).thenReturn("motivo-cancelado");

            doThrow(new SQLException("ya cancelado anteriormente"))
                    .when(dao).actualizarVueloAdmin(205L, dto);

            when(ctx.status(409)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(ctx).status(409);
            verify(ctx).json(argThat(body ->
                    body instanceof Map &&
                            "ya cancelado anteriormente".equals(((Map<?, ?>) body).get("error"))
            ));
        }
    }

    @Test
    void admin_actualizarVuelo_sqlOtro_400() throws Exception {
        VueloDAO dao = mock(VueloDAO.class);
        NotificacionesService notifySvc = mock(NotificacionesService.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            Handler authHandler = mock(Handler.class);
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            VueloController controller = new VueloController(dao, notifySvc);
            Javalin app = mock(Javalin.class);
            ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
            when(app.put(eq("/api/v1/admin/vuelos/{id}"), captor.capture())).thenReturn(app);

            controller.routes(app);
            Handler h = captor.getValue();

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(206L);

            VueloDTO.UpdateAdmin dto = mock(VueloDTO.UpdateAdmin.class);
            when(ctx.bodyAsClass(VueloDTO.UpdateAdmin.class)).thenReturn(dto);

            LocalDateTime salida = LocalDateTime.now();
            LocalDateTime llegada = LocalDateTime.now().plusHours(1);

            when(dto.codigo()).thenReturn("COD-206");
            when(dto.fechaSalida()).thenReturn(salida);
            when(dto.fechaLlegada()).thenReturn(llegada);

            List<VueloDTO.ClaseConfig> clases =
            List.of(new VueloDTO.ClaseConfig(1, 10, 100.0));
            when(dto.clases()).thenReturn(clases);


            when(dto.motivoCambio()).thenReturn("motivo-x");

            doThrow(new SQLException("error-desconocido"))
                    .when(dao).actualizarVueloAdmin(206L, dto);

            when(ctx.status(400)).thenReturn(ctx);

            h.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(ctx).status(400);
            verify(ctx).json(argThat(body ->
                    body instanceof Map &&
                            "error-desconocido".equals(((Map<?, ?>) body).get("error"))
            ));
        }
    }

    @Test
void listarConEscalaPublic_conWebServiceHeaders_invocaValidadorWS() throws Exception {
    VueloDAO dao = mock(VueloDAO.class);
    NotificacionesService notifySvc = mock(NotificacionesService.class);
    VueloController controller = new VueloController(dao, notifySvc);

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
    when(app.get(eq("/api/public/vuelos/con-escala"), captor.capture())).thenReturn(app);

    controller.routes(app);
    Handler h = captor.getValue();

    Context ctx = mock(Context.class);
    when(ctx.header("X-WebService-Email")).thenReturn("ws@test.com");
    when(ctx.header("X-WebService-Password")).thenReturn("secret");

    Handler wsHandler = mock(Handler.class);

    try (MockedStatic<WebServiceAuth> wsMock = mockStatic(WebServiceAuth.class)) {
        wsMock.when(WebServiceAuth::validate).thenReturn(wsHandler);

        h.handle(ctx);

        verify(wsHandler).handle(ctx);
    }
}

@Test
void obtenerPublic_conWebServiceHeaders_invocaValidadorWS() throws Exception {
    VueloDAO dao = mock(VueloDAO.class);
    NotificacionesService notifySvc = mock(NotificacionesService.class);
    VueloController controller = new VueloController(dao, notifySvc);

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
    when(app.get(eq("/api/public/vuelos/{id}"), captor.capture())).thenReturn(app);

    controller.routes(app);
    Handler h = captor.getValue();

    Context ctx = mock(Context.class);

    when(ctx.header("X-WebService-Email")).thenReturn("ws@test.com");
    when(ctx.header("X-WebService-Password")).thenReturn("secret");

    @SuppressWarnings("unchecked")
    Validator<Long> val = (Validator<Long>) mock(Validator.class);
    when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
    when(val.get()).thenReturn(123L);

    VueloDTO.View vuelo = new VueloDTO.View(
            123L,
            "COD-123",
            1L,
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(2),
            true,
            List.of(),
            List.of()
    );
    when(dao.obtenerVueloPublic(123L)).thenReturn(vuelo);

    Handler wsHandler = mock(Handler.class);

    try (MockedStatic<WebServiceAuth> wsMock = mockStatic(WebServiceAuth.class)) {
        wsMock.when(WebServiceAuth::validate).thenReturn(wsHandler);

        h.handle(ctx);

        verify(wsHandler).handle(ctx);
    }
}

@Test
void roundtrip_ok_201() throws Exception {
    VueloDAO dao = mock(VueloDAO.class);
    NotificacionesService notifySvc = mock(NotificacionesService.class);

    try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
        Handler authHandler = mock(Handler.class);
        authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

        VueloController controller = new VueloController(dao, notifySvc);
        Javalin app = mock(Javalin.class);
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        when(app.post(eq("/api/v1/vuelos/roundtrip"), captor.capture())).thenReturn(app);

        controller.routes(app);
        Handler h = captor.getValue();

        Context ctx = mock(Context.class);

        VueloDTO.Create ida = new VueloDTO.Create(
                "IDA",
                1L,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                List.of(),
                List.of(),
                true
        );
        VueloDTO.Create regreso = new VueloDTO.Create(
                "REG",
                1L,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1),
                List.of(),
                List.of(),
                true
        );

        VueloController.RoundtripReq req = new VueloController.RoundtripReq(ida, regreso);
        when(ctx.bodyAsClass(VueloController.RoundtripReq.class)).thenReturn(req);

        when(dao.crearVueloReturnId(ida)).thenReturn(10L);
        when(dao.crearVueloReturnId(regreso)).thenReturn(20L);

        when(ctx.status(201)).thenReturn(ctx);

        h.handle(ctx);

        verify(authHandler).handle(ctx);
        verify(dao).crearVueloReturnId(ida);
        verify(dao).crearVueloReturnId(regreso);
        verify(dao).vincularPareja(10L, 20L);
        verify(ctx).status(201);
        verify(ctx).json(argThat(body ->
                body instanceof Map &&
                        ((Map<?, ?>) body).get("idIda").equals(10L) &&
                        ((Map<?, ?>) body).get("idRegreso").equals(20L)
        ));
    }
}

@Test
void roundtrip_sqlError_400() throws Exception {
    VueloDAO dao = mock(VueloDAO.class);
    NotificacionesService notifySvc = mock(NotificacionesService.class);

    try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
        Handler authHandler = mock(Handler.class);
        authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

        VueloController controller = new VueloController(dao, notifySvc);
        Javalin app = mock(Javalin.class);
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        when(app.post(eq("/api/v1/vuelos/roundtrip"), captor.capture())).thenReturn(app);

        controller.routes(app);
        Handler h = captor.getValue();

        Context ctx = mock(Context.class);

        VueloDTO.Create ida = new VueloDTO.Create(
                "IDA-ERR",
                1L,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                List.of(),
                List.of(),
                true
        );
        VueloDTO.Create regreso = ida; 

        VueloController.RoundtripReq req = new VueloController.RoundtripReq(ida, regreso);
        when(ctx.bodyAsClass(VueloController.RoundtripReq.class)).thenReturn(req);

        when(dao.crearVueloReturnId(ida)).thenThrow(new SQLException("fallo-roundtrip"));
        when(ctx.status(400)).thenReturn(ctx);

        h.handle(ctx);

        verify(authHandler).handle(ctx);
        verify(ctx).status(400);
        verify(ctx).json(argThat(body ->
                body instanceof Map &&
                        "fallo-roundtrip".equals(((Map<?, ?>) body).get("error"))
        ));
    }
}

@Test
void roundtrip_errorGenerico_500() throws Exception {
    VueloDAO dao = mock(VueloDAO.class);
    NotificacionesService notifySvc = mock(NotificacionesService.class);

    try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
        Handler authHandler = mock(Handler.class);
        authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

        VueloController controller = new VueloController(dao, notifySvc);
        Javalin app = mock(Javalin.class);
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        when(app.post(eq("/api/v1/vuelos/roundtrip"), captor.capture())).thenReturn(app);

        controller.routes(app);
        Handler h = captor.getValue();

        Context ctx = mock(Context.class);

        VueloDTO.Create ida = new VueloDTO.Create(
                "IDA-OK",
                1L,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                List.of(),
                List.of(),
                true
        );
        VueloDTO.Create regreso = ida;

        VueloController.RoundtripReq req = new VueloController.RoundtripReq(ida, regreso);
        when(ctx.bodyAsClass(VueloController.RoundtripReq.class)).thenReturn(req);

        when(dao.crearVueloReturnId(ida)).thenReturn(10L);
        when(dao.crearVueloReturnId(regreso)).thenReturn(20L);
        doThrow(new RuntimeException("boom-link"))
                .when(dao).vincularPareja(10L, 20L);

        when(ctx.status(500)).thenReturn(ctx);

        h.handle(ctx);

        verify(authHandler).handle(ctx);
        verify(ctx).status(500);
        verify(ctx).json(argThat(body ->
                body instanceof Map &&
                        "Error al crear roundtrip".equals(((Map<?, ?>) body).get("error"))
        ));
    }
}


@Test
void link_ok_204() throws Exception {
    VueloDAO dao = mock(VueloDAO.class);
    NotificacionesService notifySvc = mock(NotificacionesService.class);

    try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
        Handler authHandler = mock(Handler.class);
        authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

        VueloController controller = new VueloController(dao, notifySvc);
        Javalin app = mock(Javalin.class);
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        when(app.post(eq("/api/v1/vuelos/link"), captor.capture())).thenReturn(app);

        controller.routes(app);
        Handler h = captor.getValue();

        Context ctx = mock(Context.class);

        VueloController.LinkReq req = new VueloController.LinkReq(10L, 20L);
        when(ctx.bodyAsClass(VueloController.LinkReq.class)).thenReturn(req);
        when(ctx.status(204)).thenReturn(ctx);

        h.handle(ctx);

        verify(authHandler).handle(ctx);
        verify(dao).vincularPareja(10L, 20L);
        verify(ctx).status(204);
    }
}

@Test
void link_sqlError_400() throws Exception {
    VueloDAO dao = mock(VueloDAO.class);
    NotificacionesService notifySvc = mock(NotificacionesService.class);

    try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
        Handler authHandler = mock(Handler.class);
        authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

        VueloController controller = new VueloController(dao, notifySvc);
        Javalin app = mock(Javalin.class);
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        when(app.post(eq("/api/v1/vuelos/link"), captor.capture())).thenReturn(app);

        controller.routes(app);
        Handler h = captor.getValue();

        Context ctx = mock(Context.class);

        VueloController.LinkReq req = new VueloController.LinkReq(30L, 40L);
        when(ctx.bodyAsClass(VueloController.LinkReq.class)).thenReturn(req);

        doThrow(new SQLException("no se pudo"))
                .when(dao).vincularPareja(30L, 40L);

        when(ctx.status(400)).thenReturn(ctx);

        h.handle(ctx);

        verify(authHandler).handle(ctx);
        verify(ctx).status(400);
        verify(ctx).json(argThat(body ->
                body instanceof Map &&
                        "no se pudo".equals(((Map<?, ?>) body).get("error"))
        ));
    }
}

@Test
void listarPublic_webServiceHeaderIncompleto_seIgnoraYLista() throws Exception {
    VueloDAO dao = mock(VueloDAO.class);
    NotificacionesService notifySvc = mock(NotificacionesService.class);
    VueloController controller = new VueloController(dao, notifySvc);

    Javalin app = mock(Javalin.class);
    ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
    when(app.get(eq("/api/public/vuelos"), captor.capture())).thenReturn(app);

    controller.routes(app);
    Handler h = captor.getValue();

    Context ctx = mock(Context.class);

    when(ctx.header("X-WebService-Email")).thenReturn("ws@test.com");
    when(ctx.header("X-WebService-Password")).thenReturn(null);

    h.handle(ctx);

    verify(dao).listarVuelosPublic();
    verify(ctx).json(any());

    verify(ctx, never()).status(anyInt());
}

@Test
void unlink_errorGenerico_sePropagaAlManejadorGlobal() throws Exception {
    VueloDAO dao = mock(VueloDAO.class);
    NotificacionesService notifySvc = mock(NotificacionesService.class);

    try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
        Handler authHandler = mock(Handler.class);
        authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

        VueloController controller = new VueloController(dao, notifySvc);
        Javalin app = mock(Javalin.class);
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        when(app.put(eq("/api/v1/vuelos/{id}/unlink"), captor.capture())).thenReturn(app);

        controller.routes(app);
        Handler h = captor.getValue();

        Context ctx = mock(Context.class);
        @SuppressWarnings("unchecked")
        Validator<Long> val = (Validator<Long>) mock(Validator.class);
        when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
        when(val.get()).thenReturn(150L);

        doThrow(new RuntimeException("boom-unlink"))
                .when(dao).desvincularPareja(150L);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> h.handle(ctx));
        assertEquals("boom-unlink", ex.getMessage());

        verify(authHandler).handle(ctx);

    }
}

@Test
void actualizarEstado_errorGenerico_sePropagaAlManejadorGlobal() throws Exception {
    VueloDAO dao = mock(VueloDAO.class);
    NotificacionesService notifySvc = mock(NotificacionesService.class);

    try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
        Handler authHandler = mock(Handler.class);
        authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

        VueloController controller = new VueloController(dao, notifySvc);
        Javalin app = mock(Javalin.class);
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        when(app.put(eq("/api/v1/vuelos/{id}/estado"), captor.capture())).thenReturn(app);

        controller.routes(app);
        Handler h = captor.getValue();

        Context ctx = mock(Context.class);
        @SuppressWarnings("unchecked")
        Validator<Long> val = (Validator<Long>) mock(Validator.class);
        when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
        when(val.get()).thenReturn(93L);

        VueloDTO.EstadoUpdate dto = new VueloDTO.EstadoUpdate(2, "motivo-x");
        when(ctx.bodyAsClass(VueloDTO.EstadoUpdate.class)).thenReturn(dto);

        doThrow(new RuntimeException("estado-boom"))
                .when(dao).actualizarEstado(93L, 2, "motivo-x");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> h.handle(ctx));
        assertEquals("estado-boom", ex.getMessage());

        verify(authHandler).handle(ctx);
    }
}


}
