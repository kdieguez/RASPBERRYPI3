package com.arolineas.controller;

import com.aerolineas.controller.ContenidoHomeController;
import com.aerolineas.dao.NoticiaDAO;
import com.aerolineas.dao.TipDAO;
import com.aerolineas.dto.NoticiaDTO;
import com.aerolineas.dto.TipDTO;
import com.aerolineas.middleware.Auth;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.validation.Validator;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ContenidoHomeControllerTest {

    private Handler captureGetHandler(Javalin app, String path) {
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        verify(app).get(eq(path), captor.capture());
        return captor.getValue();
    }

    private Handler capturePostHandler(Javalin app, String path) {
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        verify(app).post(eq(path), captor.capture());
        return captor.getValue();
    }

    private Handler capturePutHandler(Javalin app, String path) {
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        verify(app).put(eq(path), captor.capture());
        return captor.getValue();
    }

    private Handler captureDeleteHandler(Javalin app, String path) {
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        verify(app).delete(eq(path), captor.capture());
        return captor.getValue();
    }

    @Test
    void public_tips_devuelveListaDeTips() throws Exception {
        TipDAO tipDAO = mock(TipDAO.class);
        NoticiaDAO noticiaDAO = mock(NoticiaDAO.class);
        Javalin app = mock(Javalin.class);

        ContenidoHomeController controller = new ContenidoHomeController(tipDAO, noticiaDAO);
        controller.routes(app);

        Handler handler = captureGetHandler(app, "/api/public/tips");

        Context ctx = mock(Context.class);
        List<TipDTO> tips = List.of(new TipDTO());
        when(tipDAO.listar()).thenReturn(tips);

        handler.handle(ctx);

        verify(tipDAO).listar();
        verify(ctx).json(tips);
    }

    @Test
    void public_noticas_devuelveListaDeNoticias() throws Exception {
        TipDAO tipDAO = mock(TipDAO.class);
        NoticiaDAO noticiaDAO = mock(NoticiaDAO.class);
        Javalin app = mock(Javalin.class);

        ContenidoHomeController controller = new ContenidoHomeController(tipDAO, noticiaDAO);
        controller.routes(app);

        Handler handler = captureGetHandler(app, "/api/public/noticias");

        Context ctx = mock(Context.class);
        List<NoticiaDTO> noticias = List.of(new NoticiaDTO());
        when(noticiaDAO.listar()).thenReturn(noticias);

        handler.handle(ctx);

        verify(noticiaDAO).listar();
        verify(ctx).json(noticias);
    }

    @Test
    void admin_listarTips_llamaAuthYDaoYDevuelveJson() throws Exception {
        TipDAO tipDAO = mock(TipDAO.class);
        NoticiaDAO noticiaDAO = mock(NoticiaDAO.class);
        Javalin app = mock(Javalin.class);

        Handler authHandler = mock(Handler.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            ContenidoHomeController controller = new ContenidoHomeController(tipDAO, noticiaDAO);
            controller.routes(app);

            Handler handler = captureGetHandler(app, "/api/v1/admin/tips");

            Context ctx = mock(Context.class);
            List<TipDTO> tips = List.of(new TipDTO());
            when(tipDAO.listar()).thenReturn(tips);

            handler.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(tipDAO).listar();
            verify(ctx).json(tips);
        }
    }

    @Test
    void admin_crearTip_llamaAuthDaoYDevuelve201ConId() throws Exception {
        TipDAO tipDAO = mock(TipDAO.class);
        NoticiaDAO noticiaDAO = mock(NoticiaDAO.class);
        Javalin app = mock(Javalin.class);

        Handler authHandler = mock(Handler.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            ContenidoHomeController controller = new ContenidoHomeController(tipDAO, noticiaDAO);
            controller.routes(app);

            Handler handler = capturePostHandler(app, "/api/v1/admin/tips");

            Context ctx = mock(Context.class);
            TipDTO.Upsert dto = new TipDTO.Upsert();
            when(ctx.bodyAsClass(TipDTO.Upsert.class)).thenReturn(dto);
            when(tipDAO.crear(dto)).thenReturn(42L);
            when(ctx.status(201)).thenReturn(ctx);

            handler.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(tipDAO).crear(dto);
            verify(ctx).status(201);
            verify(ctx).json(Map.of("idTip", 42L));
        }
    }

    @Test
    void admin_actualizarTip_llamaAuthDaoYDevuelve204() throws Exception {
        TipDAO tipDAO = mock(TipDAO.class);
        NoticiaDAO noticiaDAO = mock(NoticiaDAO.class);
        Javalin app = mock(Javalin.class);

        Handler authHandler = mock(Handler.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            ContenidoHomeController controller = new ContenidoHomeController(tipDAO, noticiaDAO);
            controller.routes(app);

            Handler handler = capturePutHandler(app, "/api/v1/admin/tips/{id}");

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> validator = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(validator);
            when(validator.get()).thenReturn(10L);

            TipDTO.Upsert dto = new TipDTO.Upsert();
            when(ctx.bodyAsClass(TipDTO.Upsert.class)).thenReturn(dto);

            handler.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(tipDAO).actualizar(10L, dto);
            verify(ctx).status(204);
        }
    }

    @Test
    void admin_eliminarTip_llamaAuthDaoYDevuelve204() throws Exception {
        TipDAO tipDAO = mock(TipDAO.class);
        NoticiaDAO noticiaDAO = mock(NoticiaDAO.class);
        Javalin app = mock(Javalin.class);

        Handler authHandler = mock(Handler.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            ContenidoHomeController controller = new ContenidoHomeController(tipDAO, noticiaDAO);
            controller.routes(app);

            Handler handler = captureDeleteHandler(app, "/api/v1/admin/tips/{id}");

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> validator = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(validator);
            when(validator.get()).thenReturn(15L);

            handler.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(tipDAO).eliminar(15L);
            verify(ctx).status(204);
        }
    }

    @Test
    void admin_listarNoticias_llamaAuthYDaoYDevuelveJson() throws Exception {
        TipDAO tipDAO = mock(TipDAO.class);
        NoticiaDAO noticiaDAO = mock(NoticiaDAO.class);
        Javalin app = mock(Javalin.class);

        Handler authHandler = mock(Handler.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            ContenidoHomeController controller = new ContenidoHomeController(tipDAO, noticiaDAO);
            controller.routes(app);

            Handler handler = captureGetHandler(app, "/api/v1/admin/noticias");

            Context ctx = mock(Context.class);
            List<NoticiaDTO> noticias = List.of(new NoticiaDTO());
            when(noticiaDAO.listar()).thenReturn(noticias);

            handler.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(noticiaDAO).listar();
            verify(ctx).json(noticias);
        }
    }

    @Test
    void admin_crearNoticia_llamaAuthDaoYDevuelve201ConId() throws Exception {
        TipDAO tipDAO = mock(TipDAO.class);
        NoticiaDAO noticiaDAO = mock(NoticiaDAO.class);
        Javalin app = mock(Javalin.class);

        Handler authHandler = mock(Handler.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            ContenidoHomeController controller = new ContenidoHomeController(tipDAO, noticiaDAO);
            controller.routes(app);

            Handler handler = capturePostHandler(app, "/api/v1/admin/noticias");

            Context ctx = mock(Context.class);
            NoticiaDTO.Upsert dto = new NoticiaDTO.Upsert();
            when(ctx.bodyAsClass(NoticiaDTO.Upsert.class)).thenReturn(dto);
            when(noticiaDAO.crear(dto)).thenReturn(7L);
            when(ctx.status(201)).thenReturn(ctx);

            handler.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(noticiaDAO).crear(dto);
            verify(ctx).status(201);
            verify(ctx).json(Map.of("idNoticia", 7L));
        }
    }

    @Test
    void admin_actualizarNoticia_llamaAuthDaoYDevuelve204() throws Exception {
        TipDAO tipDAO = mock(TipDAO.class);
        NoticiaDAO noticiaDAO = mock(NoticiaDAO.class);
        Javalin app = mock(Javalin.class);

        Handler authHandler = mock(Handler.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            ContenidoHomeController controller = new ContenidoHomeController(tipDAO, noticiaDAO);
            controller.routes(app);

            Handler handler = capturePutHandler(app, "/api/v1/admin/noticias/{id}");

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> validator = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(validator);
            when(validator.get()).thenReturn(20L);

            NoticiaDTO.Upsert dto = new NoticiaDTO.Upsert();
            when(ctx.bodyAsClass(NoticiaDTO.Upsert.class)).thenReturn(dto);

            handler.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(noticiaDAO).actualizar(20L, dto);
            verify(ctx).status(204);
        }
    }

    @Test
    void admin_eliminarNoticia_llamaAuthDaoYDevuelve204() throws Exception {
        TipDAO tipDAO = mock(TipDAO.class);
        NoticiaDAO noticiaDAO = mock(NoticiaDAO.class);
        Javalin app = mock(Javalin.class);

        Handler authHandler = mock(Handler.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            ContenidoHomeController controller = new ContenidoHomeController(tipDAO, noticiaDAO);
            controller.routes(app);

            Handler handler = captureDeleteHandler(app, "/api/v1/admin/noticias/{id}");

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> validator = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(validator);
            when(validator.get()).thenReturn(30L);

            handler.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(noticiaDAO).eliminar(30L);
            verify(ctx).status(204);
        }
    }

    @Test
    void routes_registraExceptionHandler() {
        TipDAO tipDAO = mock(TipDAO.class);
        NoticiaDAO noticiaDAO = mock(NoticiaDAO.class);
        Javalin app = mock(Javalin.class);

        ContenidoHomeController controller = new ContenidoHomeController(tipDAO, noticiaDAO);
        controller.routes(app);

        verify(app).exception(eq(Exception.class), any());
    }
}
