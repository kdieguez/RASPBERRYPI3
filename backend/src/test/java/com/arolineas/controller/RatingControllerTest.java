package com.arolineas.controller;

import com.aerolineas.controller.RatingController;
import com.aerolineas.dao.RatingDAO;
import com.aerolineas.middleware.Auth;
import com.aerolineas.util.JwtUtil;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.validation.Validator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import io.jsonwebtoken.Claims;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class RatingControllerTest {

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
    void resumen_conUsuarioDevuelvePayload() throws Exception {
        RatingDAO dao = mock(RatingDAO.class);
        Javalin app = mock(Javalin.class);
        RatingController controller = new RatingController(dao);
        controller.routes(app);

        Handler handler = captureGet(app, "/api/public/vuelos/{id}/ratings/resumen");

        Context ctx = mock(Context.class);
        @SuppressWarnings("unchecked")
        Validator<Long> val = (Validator<Long>) mock(Validator.class);
        when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
        when(val.get()).thenReturn(5L);
        when(ctx.header("Authorization")).thenReturn("Bearer token");

        RatingDAO.Resumen resumen = mock(RatingDAO.Resumen.class);
        when(resumen.promedio()).thenReturn(4.5);
        when(resumen.total()).thenReturn(10L);
        when(resumen.miRating()).thenReturn(3);
        when(dao.resumen(5L, 7L)).thenReturn(resumen);

                try (MockedStatic<JwtUtil> jwtMock = mockStatic(JwtUtil.class)) {
            Claims claims = mock(Claims.class);
            when(claims.get("sub")).thenReturn(7L);

            jwtMock.when(() -> JwtUtil.parse("token"))
                   .thenReturn(claims);

            handler.handle(ctx);
        }

        verify(dao).resumen(5L, 7L);
        verify(ctx).json(argThat(body -> {
            if (!(body instanceof Map<?, ?> m)) return false;
            return Double.valueOf(4.5).equals(m.get("promedio"))
                    && Long.valueOf(10L).equals(m.get("total"))
                    && Integer.valueOf(3).equals(m.get("miRating"));
        }));
    }

    @Test
    void resumen_sinHeader_usaUsuarioNull() throws Exception {
        RatingDAO dao = mock(RatingDAO.class);
        Javalin app = mock(Javalin.class);
        RatingController controller = new RatingController(dao);
        controller.routes(app);

        Handler handler = captureGet(app, "/api/public/vuelos/{id}/ratings/resumen");

        Context ctx = mock(Context.class);
        @SuppressWarnings("unchecked")
        Validator<Long> val = (Validator<Long>) mock(Validator.class);
        when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
        when(val.get()).thenReturn(9L);
        when(ctx.header("Authorization")).thenReturn(null);

        RatingDAO.Resumen resumen = mock(RatingDAO.Resumen.class);
        when(resumen.promedio()).thenReturn(5.0);
        when(resumen.total()).thenReturn(1L);
        when(resumen.miRating()).thenReturn(null);
        when(dao.resumen(eq(9L), isNull())).thenReturn(resumen);

        handler.handle(ctx);

        verify(dao).resumen(9L, null);
        verify(ctx).json(any());
    }

    @Test
    void crearRating_ok_llamaAuthYDao() throws Exception {
        RatingDAO dao = mock(RatingDAO.class);
        Javalin app = mock(Javalin.class);
        RatingController controller = new RatingController(dao);
        Handler authHandler = mock(Handler.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            authMock.when(Auth::jwt).thenReturn(authHandler);
            authMock.when(() -> Auth.claims(any())).thenReturn(Map.of("sub", 3L));

            controller.routes(app);

            Handler handler = capturePost(app, "/api/v1/vuelos/{id}/ratings");

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(15L);

            RatingController.CreateReq body = new RatingController.CreateReq(4);
            when(ctx.bodyAsClass(RatingController.CreateReq.class)).thenReturn(body);
            when(ctx.status(201)).thenReturn(ctx);

            handler.handle(ctx);

            verify(authHandler).handle(ctx);
            verify(dao).upsertRating(15L, 3L, 4);
            verify(ctx).status(201);
        }
    }

    @Test
    void crearRating_sinCalificacion_devuelve400() throws Exception {
        RatingDAO dao = mock(RatingDAO.class);
        Javalin app = mock(Javalin.class);
        RatingController controller = new RatingController(dao);
        Handler authHandler = mock(Handler.class);

        try (MockedStatic<Auth> authMock = mockStatic(Auth.class)) {
            authMock.when(Auth::jwt).thenReturn(authHandler);
            authMock.when(() -> Auth.claims(any())).thenReturn(Map.of("sub", 3L));

            controller.routes(app);

            Handler handler = capturePost(app, "/api/v1/vuelos/{id}/ratings");

            Context ctx = mock(Context.class);
            @SuppressWarnings("unchecked")
            Validator<Long> val = (Validator<Long>) mock(Validator.class);
            when(ctx.pathParamAsClass("id", Long.class)).thenReturn(val);
            when(val.get()).thenReturn(15L);

            RatingController.CreateReq body = new RatingController.CreateReq(null);
            when(ctx.bodyAsClass(RatingController.CreateReq.class)).thenReturn(body);
            when(ctx.status(400)).thenReturn(ctx);

            handler.handle(ctx);

            verify(dao, never()).upsertRating(anyLong(), anyLong(), anyInt());
            verify(ctx).status(400);
            verify(ctx).json(argThat(bodyMap ->
                    bodyMap instanceof Map &&
                            "calificacion requerida".equals(((Map<?, ?>) bodyMap).get("error"))
            ));
        }
    }
}
