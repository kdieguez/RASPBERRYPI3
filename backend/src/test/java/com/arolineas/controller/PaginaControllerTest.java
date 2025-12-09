package com.arolineas.controller;

import com.aerolineas.controller.PaginasController;
import com.aerolineas.dao.MediaDAO;
import com.aerolineas.dao.PaginaDAO;
import com.aerolineas.dao.SeccionDAO;
import com.aerolineas.dto.MediaDTO;
import com.aerolineas.dto.PaginaDTO;
import com.aerolineas.dto.SeccionDTO;
import com.aerolineas.middleware.Auth;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.validation.Validator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PaginaControllerTest {   

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

    private Handler captureDelete(Javalin app, String path) {
        ArgumentCaptor<Handler> captor = ArgumentCaptor.forClass(Handler.class);
        verify(app).delete(eq(path), captor.capture());
        return captor.getValue();
    }

    @Test
    void listarPaginas_public_devuelveJson() throws Exception {
        PaginaDAO paginaDAO = mock(PaginaDAO.class);
        SeccionDAO seccionDAO = mock(SeccionDAO.class);
        MediaDAO mediaDAO = mock(MediaDAO.class);
        Javalin app = mock(Javalin.class);

        PaginasController controller = new PaginasController(paginaDAO, seccionDAO, mediaDAO);
        controller.routes(app);

        Handler handler = captureGet(app, "/api/public/paginas");

        Context ctx = mock(Context.class);
        
        List<PaginaDTO> paginas = List.of(new PaginaDTO());
        when(paginaDAO.listar()).thenReturn(paginas);

        handler.handle(ctx);

        verify(paginaDAO).listar();
        verify(ctx).json(paginas);
    }

    @Test
    void obtenerPaginaPorId_encontrada_devuelveJson() throws Exception {
        PaginaDAO paginaDAO = mock(PaginaDAO.class);
        SeccionDAO seccionDAO = mock(SeccionDAO.class);
        MediaDAO mediaDAO = mock(MediaDAO.class);
        Javalin app = mock(Javalin.class);

        PaginasController controller = new PaginasController(paginaDAO, seccionDAO, mediaDAO);
        controller.routes(app);

        Handler handler = captureGet(app, "/api/public/paginas/{id}");

        Context ctx = mock(Context.class);
        @SuppressWarnings("unchecked")
        Validator<Long> val = (Validator<Long>) mock(Validator.class);
        when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
        when(val.get()).thenReturn(5L);

        
        PaginaDTO pagina = new PaginaDTO();
        when(paginaDAO.obtenerConContenido(5L)).thenReturn(pagina);

        handler.handle(ctx);

        verify(paginaDAO).obtenerConContenido(5L);
        verify(ctx).json(pagina);
        verify(ctx, never()).status(404);
    }

    @Test
    void obtenerPaginaPorId_noEncontrada_devuelve404() throws Exception {
        PaginaDAO paginaDAO = mock(PaginaDAO.class);
        SeccionDAO seccionDAO = mock(SeccionDAO.class);
        MediaDAO mediaDAO = mock(MediaDAO.class);
        Javalin app = mock(Javalin.class);

        PaginasController controller = new PaginasController(paginaDAO, seccionDAO, mediaDAO);
        controller.routes(app);

        Handler handler = captureGet(app, "/api/public/paginas/{id}");

        Context ctx = mock(Context.class);
        @SuppressWarnings("unchecked")
        Validator<Long> val = (Validator<Long>) mock(Validator.class);
        when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
        when(val.get()).thenReturn(5L);
        when(ctx.status(404)).thenReturn(ctx);

        when(paginaDAO.obtenerConContenido(5L)).thenReturn(null);

        handler.handle(ctx);

        verify(paginaDAO).obtenerConContenido(5L);
        verify(ctx).status(404);
        verify(ctx).json(argThat(body ->
                body instanceof Map &&
                        "Página no encontrada".equals(((Map<?, ?>) body).get("error"))
        ));
    }

    @Test
    void obtenerPaginaPorNombre_encontrada_devuelveJson() throws Exception {
        PaginaDAO paginaDAO = mock(PaginaDAO.class);
        SeccionDAO seccionDAO = mock(SeccionDAO.class);
        MediaDAO mediaDAO = mock(MediaDAO.class);
        Javalin app = mock(Javalin.class);

        PaginasController controller = new PaginasController(paginaDAO, seccionDAO, mediaDAO);
        controller.routes(app);

        Handler handler = captureGet(app, "/api/public/paginas/by-name/{nombre}");

        Context ctx = mock(Context.class);
        when(ctx.pathParam("nombre")).thenReturn("home");

        
        PaginaDTO pagina = new PaginaDTO();
        when(paginaDAO.obtenerPorNombreConContenido("home")).thenReturn(pagina);

        handler.handle(ctx);

        verify(paginaDAO).obtenerPorNombreConContenido("home");
        verify(ctx).json(pagina);
    }

    @Test
    void obtenerPaginaPorNombre_noEncontrada_404() throws Exception {
        PaginaDAO paginaDAO = mock(PaginaDAO.class);
        SeccionDAO seccionDAO = mock(SeccionDAO.class);
        MediaDAO mediaDAO = mock(MediaDAO.class);
        Javalin app = mock(Javalin.class);

        PaginasController controller = new PaginasController(paginaDAO, seccionDAO, mediaDAO);
        controller.routes(app);

        Handler handler = captureGet(app, "/api/public/paginas/by-name/{nombre}");

        Context ctx = mock(Context.class);
        when(ctx.pathParam("nombre")).thenReturn("home");
        when(ctx.status(404)).thenReturn(ctx);

        when(paginaDAO.obtenerPorNombreConContenido("home")).thenReturn(null);

        handler.handle(ctx);

        verify(paginaDAO).obtenerPorNombreConContenido("home");
        verify(ctx).status(404);
        verify(ctx).json(argThat(body ->
                body instanceof Map &&
                        "Página no encontrada".equals(((Map<?, ?>) body).get("error"))
        ));
    }

    @Test
    void crearPagina_admin_llamaAuthDaoYDevuelve201() throws Exception {
        PaginaDAO paginaDAO = mock(PaginaDAO.class);
        SeccionDAO seccionDAO = mock(SeccionDAO.class);
        MediaDAO mediaDAO = mock(MediaDAO.class);
        Javalin app = mock(Javalin.class);
        Handler authHandler = mock(Handler.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            PaginasController controller = new PaginasController(paginaDAO, seccionDAO, mediaDAO);
            controller.routes(app);

            Handler handler = capturePost(app, "/api/v1/admin/paginas");

            Context ctx = mock(Context.class);
            PaginaDTO.Upsert dto = new PaginaDTO.Upsert();
            when(ctx.bodyAsClass(PaginaDTO.Upsert.class)).thenReturn(dto);
            when(paginaDAO.crear(dto)).thenReturn(10L);
            when(ctx.status(201)).thenReturn(ctx);

            handler.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(paginaDAO).crear(dto);
            verify(ctx).status(201);
            verify(ctx).json(Map.of("idPagina", 10L));
        }
    }

    @Test
    void actualizarPagina_admin_llamaAuthYDevuelve204() throws Exception {
        PaginaDAO paginaDAO = mock(PaginaDAO.class);
        SeccionDAO seccionDAO = mock(SeccionDAO.class);
        MediaDAO mediaDAO = mock(MediaDAO.class);
        Javalin app = mock(Javalin.class);
        Handler authHandler = mock(Handler.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            PaginasController controller = new PaginasController(paginaDAO, seccionDAO, mediaDAO);
            controller.routes(app);

            Handler handler = capturePut(app, "/api/v1/admin/paginas/{id}");

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(3L);

            PaginaDTO.Upsert dto = new PaginaDTO.Upsert();
            when(ctx.bodyAsClass(PaginaDTO.Upsert.class)).thenReturn(dto);

            handler.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(paginaDAO).actualizar(3L, dto);
            verify(ctx).status(204);
        }
    }

    @Test
    void eliminarPagina_admin_llamaAuthYDevuelve204() throws Exception {
        PaginaDAO paginaDAO = mock(PaginaDAO.class);
        SeccionDAO seccionDAO = mock(SeccionDAO.class);
        MediaDAO mediaDAO = mock(MediaDAO.class);
        Javalin app = mock(Javalin.class);
        Handler authHandler = mock(Handler.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            PaginasController controller = new PaginasController(paginaDAO, seccionDAO, mediaDAO);
            controller.routes(app);

            Handler handler = captureDelete(app, "/api/v1/admin/paginas/{id}");

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(4L);

            handler.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(paginaDAO).eliminar(4L);
            verify(ctx).status(204);
        }
    }

    @Test
    void crearSeccion_admin_devuelve201() throws Exception {
        PaginaDAO paginaDAO = mock(PaginaDAO.class);
        SeccionDAO seccionDAO = mock(SeccionDAO.class);
        MediaDAO mediaDAO = mock(MediaDAO.class);
        Javalin app = mock(Javalin.class);
        Handler authHandler = mock(Handler.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            PaginasController controller = new PaginasController(paginaDAO, seccionDAO, mediaDAO);
            controller.routes(app);

            Handler handler = capturePost(app, "/api/v1/admin/paginas/{idPagina}/secciones");

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("idPagina", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(8L);

            SeccionDTO.Upsert dto = new SeccionDTO.Upsert();
            when(ctx.bodyAsClass(SeccionDTO.Upsert.class)).thenReturn(dto);
            when(seccionDAO.crear(8L, dto)).thenReturn(99L);
            when(ctx.status(201)).thenReturn(ctx);

            handler.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(seccionDAO).crear(8L, dto);
            verify(ctx).status(201);
            verify(ctx).json(Map.of("idSeccion", 99L));
        }
    }

    @Test
    void actualizarSeccion_admin_devuelve204() throws Exception {
        PaginaDAO paginaDAO = mock(PaginaDAO.class);
        SeccionDAO seccionDAO = mock(SeccionDAO.class);
        MediaDAO mediaDAO = mock(MediaDAO.class);
        Javalin app = mock(Javalin.class);
        Handler authHandler = mock(Handler.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            PaginasController controller = new PaginasController(paginaDAO, seccionDAO, mediaDAO);
            controller.routes(app);

            Handler handler = capturePut(app, "/api/v1/admin/secciones/{idSeccion}");

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("idSeccion", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(12L);

            SeccionDTO.Upsert dto = new SeccionDTO.Upsert();
            when(ctx.bodyAsClass(SeccionDTO.Upsert.class)).thenReturn(dto);

            handler.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(seccionDAO).actualizar(12L, dto);
            verify(ctx).status(204);
        }
    }

    @Test
    void eliminarSeccion_admin_devuelve204() throws Exception {
        PaginaDAO paginaDAO = mock(PaginaDAO.class);
        SeccionDAO seccionDAO = mock(SeccionDAO.class);
        MediaDAO mediaDAO = mock(MediaDAO.class);
        Javalin app = mock(Javalin.class);
        Handler authHandler = mock(Handler.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            PaginasController controller = new PaginasController(paginaDAO, seccionDAO, mediaDAO);
            controller.routes(app);

            Handler handler = captureDelete(app, "/api/v1/admin/secciones/{idSeccion}");

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("idSeccion", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(13L);

            handler.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(seccionDAO).eliminar(13L);
            verify(ctx).status(204);
        }
    }

    @Test
    void reordenarSecciones_admin_devuelve204() throws Exception {
        PaginaDAO paginaDAO = mock(PaginaDAO.class);
        SeccionDAO seccionDAO = mock(SeccionDAO.class);
        MediaDAO mediaDAO = mock(MediaDAO.class);
        Javalin app = mock(Javalin.class);
        Handler authHandler = mock(Handler.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            PaginasController controller = new PaginasController(paginaDAO, seccionDAO, mediaDAO);
            controller.routes(app);

            Handler handler = capturePut(app, "/api/v1/admin/paginas/{idPagina}/secciones/reordenar");

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("idPagina", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(20L);

            SeccionDTO.Reordenar[] arr = new SeccionDTO.Reordenar[]{ new SeccionDTO.Reordenar() };
            when(ctx.bodyAsClass(SeccionDTO.Reordenar[].class)).thenReturn(arr);

            handler.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(seccionDAO).reordenar(eq(20L), anyList());
            verify(ctx).status(204);
        }
    }

    @Test
    void crearMedia_admin_devuelve201() throws Exception {
        PaginaDAO paginaDAO = mock(PaginaDAO.class);
        SeccionDAO seccionDAO = mock(SeccionDAO.class);
        MediaDAO mediaDAO = mock(MediaDAO.class);
        Javalin app = mock(Javalin.class);
        Handler authHandler = mock(Handler.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            PaginasController controller = new PaginasController(paginaDAO, seccionDAO, mediaDAO);
            controller.routes(app);

            Handler handler = capturePost(app, "/api/v1/admin/secciones/{idSeccion}/media");

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("idSeccion", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(30L);

            MediaDTO.Upsert dto = new MediaDTO.Upsert();
            when(ctx.bodyAsClass(MediaDTO.Upsert.class)).thenReturn(dto);
            when(mediaDAO.crear(30L, dto)).thenReturn(77L);
            when(ctx.status(201)).thenReturn(ctx);

            handler.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(mediaDAO).crear(30L, dto);
            verify(ctx).status(201);
            verify(ctx).json(Map.of("idMedia", 77L));
        }
    }

    @Test
    void eliminarMedia_admin_devuelve204() throws Exception {
        PaginaDAO paginaDAO = mock(PaginaDAO.class);
        SeccionDAO seccionDAO = mock(SeccionDAO.class);
        MediaDAO mediaDAO = mock(MediaDAO.class);
        Javalin app = mock(Javalin.class);
        Handler authHandler = mock(Handler.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            PaginasController controller = new PaginasController(paginaDAO, seccionDAO, mediaDAO);
            controller.routes(app);

            Handler handler = captureDelete(app, "/api/v1/admin/media/{idMedia}");

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("idMedia", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(40L);

            handler.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(mediaDAO).eliminar(40L);
            verify(ctx).status(204);
        }
    }

    @Test
    void reordenarMedia_admin_devuelve204() throws Exception {
        PaginaDAO paginaDAO = mock(PaginaDAO.class);
        SeccionDAO seccionDAO = mock(SeccionDAO.class);
        MediaDAO mediaDAO = mock(MediaDAO.class);
        Javalin app = mock(Javalin.class);
        Handler authHandler = mock(Handler.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            authMock.when(Auth::adminOrEmpleado).thenReturn(authHandler);

            PaginasController controller = new PaginasController(paginaDAO, seccionDAO, mediaDAO);
            controller.routes(app);

            Handler handler = capturePut(app, "/api/v1/admin/secciones/{idSeccion}/media/reordenar");

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("idSeccion", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(50L);

            MediaDTO.Reordenar[] arr = new MediaDTO.Reordenar[]{ new MediaDTO.Reordenar() };
            when(ctx.bodyAsClass(MediaDTO.Reordenar[].class)).thenReturn(arr);

            handler.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(mediaDAO).reordenar(eq(50L), anyList());
            verify(ctx).status(204);
        }
    }

    @Test
    void routes_registraExceptionHandler() {
        PaginaDAO paginaDAO = mock(PaginaDAO.class);
        SeccionDAO seccionDAO = mock(SeccionDAO.class);
        MediaDAO mediaDAO = mock(MediaDAO.class);
        Javalin app = mock(Javalin.class);

        PaginasController controller = new PaginasController(paginaDAO, seccionDAO, mediaDAO);
        controller.routes(app);

        verify(app).exception(eq(Exception.class), any());
    }
}
