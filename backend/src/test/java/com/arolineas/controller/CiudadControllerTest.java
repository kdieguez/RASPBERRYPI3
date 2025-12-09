package com.aerolineas.controller;

import com.aerolineas.dao.CiudadDAO;
import com.aerolineas.dto.CiudadDTOs;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.validation.NullableValidator;
import io.javalin.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CiudadControllerTest {

            
    @Test
    @DisplayName("GET /api/public/ciudades usa idPais cuando viene en query")
    void listPublic_ciudades_conIdPais() throws Exception {
        Javalin app = mock(Javalin.class);
        Context ctx = mock(Context.class);
        CiudadDAO dao = mock(CiudadDAO.class);

        final Handler[] handlerHolder = new Handler[1];
        when(app.get(eq("/api/public/ciudades"), any(Handler.class))).thenAnswer(inv -> {
            handlerHolder[0] = inv.getArgument(1);
            return app;
        });

                @SuppressWarnings("unchecked")
        Validator<Long> validator = mock(Validator.class);
        @SuppressWarnings("unchecked")
        NullableValidator<Long> nullableValidator = mock(NullableValidator.class);

        when(ctx.queryParamAsClass("idPais", Long.class)).thenReturn(validator);
        when(validator.allowNullable()).thenReturn(nullableValidator);
        when(nullableValidator.get()).thenReturn(1L);

        List<CiudadDTOs.View> lista = List.of(
                new CiudadDTOs.View(10L, 1L, "Guatemala", "Ciudad Test", true)
        );
        when(dao.listAll(1L)).thenReturn(lista);

        CiudadController controller = new CiudadController(dao);
        controller.routes(app);

        assertNotNull(handlerHolder[0], "Handler de /api/public/ciudades no registrado");
        handlerHolder[0].handle(ctx);

        verify(dao).listAll(1L);
        verify(ctx).json(lista);
    }

    @Test
    @DisplayName("GET /api/public/ciudades pasa null a listAll cuando idPais es null")
    void listPublic_ciudades_sinIdPais() throws Exception {
        Javalin app = mock(Javalin.class);
        Context ctx = mock(Context.class);
        CiudadDAO dao = mock(CiudadDAO.class);

        final Handler[] handlerHolder = new Handler[1];
        when(app.get(eq("/api/public/ciudades"), any(Handler.class))).thenAnswer(inv -> {
            handlerHolder[0] = inv.getArgument(1);
            return app;
        });

        @SuppressWarnings("unchecked")
        Validator<Long> validator = mock(Validator.class);
        @SuppressWarnings("unchecked")
        NullableValidator<Long> nullableValidator = mock(NullableValidator.class);

        when(ctx.queryParamAsClass("idPais", Long.class)).thenReturn(validator);
        when(validator.allowNullable()).thenReturn(nullableValidator);
        when(nullableValidator.get()).thenReturn(null); 
        List<CiudadDTOs.View> listaVacia = List.of();
        when(dao.listAll(null)).thenReturn(listaVacia);

        CiudadController controller = new CiudadController(dao);
        controller.routes(app);

        assertNotNull(handlerHolder[0]);
        handlerHolder[0].handle(ctx);

        verify(dao).listAll(null);
        verify(ctx).json(listaVacia);
    }

            
    @Test
    @DisplayName("GET /api/public/clima/ciudades devuelve lista de ciudades para clima")
    void listForWeather_ok() throws Exception {
        Javalin app = mock(Javalin.class);
        Context ctx = mock(Context.class);
        CiudadDAO dao = mock(CiudadDAO.class);

        final Handler[] handlerHolder = new Handler[1];
        when(app.get(eq("/api/public/clima/ciudades"), any(Handler.class))).thenAnswer(inv -> {
            handlerHolder[0] = inv.getArgument(1);
            return app;
        });

        List<CiudadDTOs.WeatherCity> lista = List.of(
                new CiudadDTOs.WeatherCity(99L, "Ciudad Clima", "Guatemala", "Guatemala City,GT")
        );
        when(dao.listForWeather()).thenReturn(lista);

        CiudadController controller = new CiudadController(dao);
        controller.routes(app);

        assertNotNull(handlerHolder[0]);
        handlerHolder[0].handle(ctx);

        verify(dao).listForWeather();
        verify(ctx).json(lista);
    }

            
    @Test
    @DisplayName("POST /api/v1/ciudades crea ciudad y devuelve idCiudad")
    void createCiudad_ok() throws Exception {
        Javalin app = mock(Javalin.class);
        Context ctx = mock(Context.class);
        CiudadDAO dao = mock(CiudadDAO.class);

        final Handler[] handlerHolder = new Handler[1];
        when(app.post(eq("/api/v1/ciudades"), any(Handler.class))).thenAnswer(inv -> {
            handlerHolder[0] = inv.getArgument(1);
            return app;
        });

        CiudadDTOs.Create dto = new CiudadDTOs.Create(
                1L,
                "Ciudad Test",
                "Ciudad Test,GT"
        );
        when(ctx.bodyAsClass(CiudadDTOs.Create.class)).thenReturn(dto);
        when(ctx.status(anyInt())).thenReturn(ctx);

        when(dao.create(dto)).thenReturn(42L);

        CiudadController controller = new CiudadController(dao);
        controller.routes(app);

        assertNotNull(handlerHolder[0]);
        handlerHolder[0].handle(ctx);

        verify(dao).create(dto);
        verify(ctx).status(201);
        verify(ctx).json(Map.of("idCiudad", 42L));
    }

            
    @Test
    @DisplayName("PUT /api/v1/ciudades/{id}/toggle llama a toggleActiva y responde 204")
    void toggleCiudad_ok() throws Exception {
        Javalin app = mock(Javalin.class);
        Context ctx = mock(Context.class);
        CiudadDAO dao = mock(CiudadDAO.class);

        final Handler[] handlerHolder = new Handler[1];
        when(app.put(eq("/api/v1/ciudades/{id}/toggle"), any(Handler.class))).thenAnswer(inv -> {
            handlerHolder[0] = inv.getArgument(1);
            return app;
        });

        when(ctx.pathParam("id")).thenReturn("7");
        when(ctx.status(anyInt())).thenReturn(ctx);

        CiudadController controller = new CiudadController(dao);
        controller.routes(app);

        assertNotNull(handlerHolder[0]);
        handlerHolder[0].handle(ctx);

        verify(dao).toggleActiva(7L);
        verify(ctx).status(204);
    }

    @Test
    @DisplayName("Constructor por defecto de CiudadController no truena")
    void defaultConstructor_works() {
        CiudadController controller = new CiudadController();
        assertNotNull(controller);
    }

}
