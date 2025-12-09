package com.aerolineas.controller;

import com.aerolineas.config.DB;
import com.aerolineas.dao.AgenciasConfigDAO;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AgenciasConfigControllerTest { 

    @Test
    @DisplayName("list devuelve agencias con soloHabilitadas=true")
    void list_soloHabilitadas_true_ok() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.queryParam("soloHabilitadas")).thenReturn("true");

        Connection conn = mock(Connection.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<AgenciasConfigDAO> cons =
                     mockConstruction(AgenciasConfigDAO.class,
                             (daoMock, c) -> when(daoMock.listar(true))
                                     .thenReturn(List.of(Map.of("idAgencia", "A1"))))) {

            dbMock.when(DB::getConnection).thenReturn(conn);

            AgenciasConfigController controller = new AgenciasConfigController();
            controller.list(ctx);

            AgenciasConfigDAO dao = cons.constructed().get(0);
            verify(dao).listar(true);
            verify(ctx).json(any());
        }
    }

    @Test
    @DisplayName("list usa soloHabilitadas=false cuando no viene parámetro")
    void list_soloHabilitadas_paramNull() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.queryParam("soloHabilitadas")).thenReturn(null);

        Connection conn = mock(Connection.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<AgenciasConfigDAO> cons =
                     mockConstruction(AgenciasConfigDAO.class,
                             (daoMock, c) -> when(daoMock.listar(false))
                                     .thenReturn(List.of()))) {

            dbMock.when(DB::getConnection).thenReturn(conn);

            AgenciasConfigController controller = new AgenciasConfigController();
            controller.list(ctx);

            AgenciasConfigDAO dao = cons.constructed().get(0);
            verify(dao).listar(false);
            verify(ctx).json(any());
        }
    }

    @Test
    @DisplayName("list maneja excepción devolviendo 500")
    void list_error_500() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.queryParam("soloHabilitadas")).thenReturn("true");
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);

        Connection conn = mock(Connection.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<AgenciasConfigDAO> cons =
                     mockConstruction(AgenciasConfigDAO.class,
                             (daoMock, c) -> when(daoMock.listar(true))
                                     .thenThrow(new RuntimeException("boom list")))) {

            dbMock.when(DB::getConnection).thenReturn(conn);

            AgenciasConfigController controller = new AgenciasConfigController();
            controller.list(ctx);

            verify(ctx).status(HttpStatus.INTERNAL_SERVER_ERROR);
            verify(ctx).json(Map.of("error", "boom list"));
        }
    }

    @Test
    @DisplayName("get devuelve agencia cuando existe")
    void get_found() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.pathParam("id")).thenReturn("AG1");

        Connection conn = mock(Connection.class);
        Map<String, Object> agencia = Map.of("idAgencia", "AG1", "nombre", "Agencia 1");

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<AgenciasConfigDAO> cons =
                     mockConstruction(AgenciasConfigDAO.class,
                             (daoMock, c) -> when(daoMock.obtener("AG1")).thenReturn(agencia))) {

            dbMock.when(DB::getConnection).thenReturn(conn);

            AgenciasConfigController controller = new AgenciasConfigController();
            controller.get(ctx);

            AgenciasConfigDAO dao = cons.constructed().get(0);
            verify(dao).obtener("AG1");
            verify(ctx).json(agencia);
            verify(ctx, never()).status(HttpStatus.NOT_FOUND);
        }
    }

    @Test
    @DisplayName("get devuelve 404 cuando agencia no existe")
    void get_notFound_404() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.pathParam("id")).thenReturn("NOPE");
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);

        Connection conn = mock(Connection.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<AgenciasConfigDAO> cons =
                     mockConstruction(AgenciasConfigDAO.class,
                             (daoMock, c) -> when(daoMock.obtener("NOPE")).thenReturn(null))) {

            dbMock.when(DB::getConnection).thenReturn(conn);

            AgenciasConfigController controller = new AgenciasConfigController();
            controller.get(ctx);

            verify(ctx).status(HttpStatus.NOT_FOUND);
            verify(ctx).json(Map.of("error", "Agencia no encontrada"));
        }
    }

    @Test
    @DisplayName("get maneja excepción devolviendo 500")
    void get_error_500() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.pathParam("id")).thenReturn("AG1");
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);

        Connection conn = mock(Connection.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<AgenciasConfigDAO> cons =
                     mockConstruction(AgenciasConfigDAO.class,
                             (daoMock, c) -> when(daoMock.obtener("AG1"))
                                     .thenThrow(new RuntimeException("boom get")))) {

            dbMock.when(DB::getConnection).thenReturn(conn);

            AgenciasConfigController controller = new AgenciasConfigController();
            controller.get(ctx);

            verify(ctx).status(HttpStatus.INTERNAL_SERVER_ERROR);
            verify(ctx).json(Map.of("error", "boom get"));
        }
    }    

    @Test
    @DisplayName("create crea agencia correctamente")
    void create_ok() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);

        Map<String, Object> body = Map.of(
                "idAgencia", "AG1",
                "nombre", "Agencia Uno",
                "apiUrl", "https://api.test",
                "idUsuarioWs", 10L
        );
        when(ctx.bodyAsClass(Map.class)).thenReturn(body);

        Connection conn = mock(Connection.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<AgenciasConfigDAO> cons =
                     mockConstruction(AgenciasConfigDAO.class,
                             (daoMock, c) ->
                                     doNothing().when(daoMock)
                                             .crear("AG1", "Agencia Uno", "https://api.test", 10L))) {

            dbMock.when(DB::getConnection).thenReturn(conn);

            AgenciasConfigController controller = new AgenciasConfigController();
            controller.create(ctx);

            AgenciasConfigDAO dao = cons.constructed().get(0);
            verify(dao).crear("AG1", "Agencia Uno", "https://api.test", 10L);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> cap = ArgumentCaptor.forClass(Map.class);
            verify(ctx).status(HttpStatus.CREATED);
            verify(ctx).json(cap.capture());

            Map<String, Object> json = cap.getValue();
            assertEquals(Boolean.TRUE, json.get("ok"));
            assertEquals("AG1", json.get("idAgencia"));
        }
    }

    @Test
    @DisplayName("create valida idAgencia requerido")
    void create_idAgenciaRequerido_400() {
        Context ctx = mock(Context.class);
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);

        Map<String, Object> body = Map.of(
                "nombre", "Agencia",
                "apiUrl", "https://api.test"
        );
        when(ctx.bodyAsClass(Map.class)).thenReturn(body);

        AgenciasConfigController controller = new AgenciasConfigController();
        controller.create(ctx);

        verify(ctx).status(HttpStatus.BAD_REQUEST);
        verify(ctx).json(Map.of("error", "idAgencia es requerido"));
    }

    @Test
    @DisplayName("create valida nombre requerido")
    void create_nombreRequerido_400() {
        Context ctx = mock(Context.class);
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);

        Map<String, Object> body = Map.of(
                "idAgencia", "AG1",
                "apiUrl", "https://api.test"
        );
        when(ctx.bodyAsClass(Map.class)).thenReturn(body);

        AgenciasConfigController controller = new AgenciasConfigController();
        controller.create(ctx);

        verify(ctx).status(HttpStatus.BAD_REQUEST);
        verify(ctx).json(Map.of("error", "nombre es requerido"));
    }

    @Test
    @DisplayName("create valida apiUrl requerido")
    void create_apiUrlRequerido_400() {
        Context ctx = mock(Context.class);
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);

        Map<String, Object> body = Map.of(
                "idAgencia", "AG1",
                "nombre", "Agencia Uno"
        );
        when(ctx.bodyAsClass(Map.class)).thenReturn(body);

        AgenciasConfigController controller = new AgenciasConfigController();
        controller.create(ctx);

        verify(ctx).status(HttpStatus.BAD_REQUEST);
        verify(ctx).json(Map.of("error", "apiUrl es requerido"));
    }

    @Test
    @DisplayName("create maneja conflicto de unique constraint con 409")
    void create_conflict_409() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);

        Map<String, Object> body = Map.of(
                "idAgencia", "AG1",
                "nombre", "Agencia Uno",
                "apiUrl", "https://api.test"
        );
        when(ctx.bodyAsClass(Map.class)).thenReturn(body);

        Connection conn = mock(Connection.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<AgenciasConfigDAO> cons =
                     mockConstruction(AgenciasConfigDAO.class,
                             (daoMock, c) -> doThrow(new RuntimeException("unique constraint"))
                                     .when(daoMock)
                                     .crear("AG1", "Agencia Uno", "https://api.test", null))) {

            dbMock.when(DB::getConnection).thenReturn(conn);

            AgenciasConfigController controller = new AgenciasConfigController();
            controller.create(ctx);

            verify(ctx).status(HttpStatus.CONFLICT);
            verify(ctx).json(Map.of("error", "Ya existe una agencia con ese ID"));
        }
    }

    @Test
    @DisplayName("create maneja error genérico con 500")
    void create_error_500() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);

        Map<String, Object> body = Map.of(
                "idAgencia", "AG1",
                "nombre", "Agencia Uno",
                "apiUrl", "https://api.test"
        );
        when(ctx.bodyAsClass(Map.class)).thenReturn(body);

        Connection conn = mock(Connection.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<AgenciasConfigDAO> cons =
                     mockConstruction(AgenciasConfigDAO.class,
                             (daoMock, c) -> doThrow(new RuntimeException("otro error"))
                                     .when(daoMock)
                                     .crear("AG1", "Agencia Uno", "https://api.test", null))) {

            dbMock.when(DB::getConnection).thenReturn(conn);

            AgenciasConfigController controller = new AgenciasConfigController();
            controller.create(ctx);

            verify(ctx).status(HttpStatus.INTERNAL_SERVER_ERROR);
            verify(ctx).json(Map.of("error", "otro error"));
        }
    }

    @Test
    @DisplayName("update actualiza agencia y devuelve 204")
    void update_ok() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);
        when(ctx.pathParam("id")).thenReturn("AG1");

        Map<String, Object> body = Map.of(
                "nombre", "Nuevo Nombre",
                "apiUrl", "https://api.nueva",
                "idUsuarioWs", 20L,
                "habilitado", true
        );
        when(ctx.bodyAsClass(Map.class)).thenReturn(body);

        Connection conn = mock(Connection.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<AgenciasConfigDAO> cons =
                     mockConstruction(AgenciasConfigDAO.class,
                             (daoMock, c) ->
                                     doNothing().when(daoMock)
                                             .actualizar("AG1", "Nuevo Nombre",
                                                     "https://api.nueva", 20L, true))) {

            dbMock.when(DB::getConnection).thenReturn(conn);

            AgenciasConfigController controller = new AgenciasConfigController();
            controller.update(ctx);

            AgenciasConfigDAO dao = cons.constructed().get(0);
            verify(dao).actualizar("AG1", "Nuevo Nombre",
                    "https://api.nueva", 20L, true);
            verify(ctx).status(HttpStatus.NO_CONTENT);
        }
    }

    @Test
    @DisplayName("update maneja no encontrada con 404")
    void update_notFound_404() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);
        when(ctx.pathParam("id")).thenReturn("AGX");

        Map<String, Object> body = Map.of(
                "nombre", "Nombre",
                "apiUrl", "https://api.test"
        );
        when(ctx.bodyAsClass(Map.class)).thenReturn(body);

        Connection conn = mock(Connection.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<AgenciasConfigDAO> cons =
                     mockConstruction(AgenciasConfigDAO.class,
                             (daoMock, c) -> doThrow(new RuntimeException("no encontrada"))
                                     .when(daoMock)
                                     .actualizar(eq("AGX"), any(), any(), any(), any()))) {

            dbMock.when(DB::getConnection).thenReturn(conn);

            AgenciasConfigController controller = new AgenciasConfigController();
            controller.update(ctx);

            verify(ctx).status(HttpStatus.NOT_FOUND);
            verify(ctx).json(Map.of("error", "no encontrada"));
        }
    }

    @Test
    @DisplayName("update maneja error genérico con 500")
    void update_error_500() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);
        when(ctx.pathParam("id")).thenReturn("AGX");

        Map<String, Object> body = Map.of(
                "nombre", "Nombre",
                "apiUrl", "https://api.test"
        );
        when(ctx.bodyAsClass(Map.class)).thenReturn(body);

        Connection conn = mock(Connection.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<AgenciasConfigDAO> cons =
                     mockConstruction(AgenciasConfigDAO.class,
                             (daoMock, c) -> doThrow(new RuntimeException("otro error"))
                                     .when(daoMock)
                                     .actualizar(eq("AGX"), any(), any(), any(), any()))) {

            dbMock.when(DB::getConnection).thenReturn(conn);

            AgenciasConfigController controller = new AgenciasConfigController();
            controller.update(ctx);

            verify(ctx).status(HttpStatus.INTERNAL_SERVER_ERROR);
            verify(ctx).json(Map.of("error", "otro error"));
        }
    }

    @Test
    @DisplayName("delete elimina agencia y devuelve 204")
    void delete_ok() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);
        when(ctx.pathParam("id")).thenReturn("AG1");

        Connection conn = mock(Connection.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<AgenciasConfigDAO> cons =
                     mockConstruction(AgenciasConfigDAO.class,
                             (daoMock, c) -> doNothing().when(daoMock).eliminar("AG1"))) {

            dbMock.when(DB::getConnection).thenReturn(conn);

            AgenciasConfigController controller = new AgenciasConfigController();
            controller.delete(ctx);

            AgenciasConfigDAO dao = cons.constructed().get(0);
            verify(dao).eliminar("AG1");
            verify(ctx).status(HttpStatus.NO_CONTENT);
        }
    }

    @Test
    @DisplayName("delete maneja no encontrada con 404")
    void delete_notFound_404() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);
        when(ctx.pathParam("id")).thenReturn("AGX");

        Connection conn = mock(Connection.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<AgenciasConfigDAO> cons =
                     mockConstruction(AgenciasConfigDAO.class,
                             (daoMock, c) -> doThrow(new RuntimeException("no encontrada"))
                                     .when(daoMock).eliminar("AGX"))) {

            dbMock.when(DB::getConnection).thenReturn(conn);

            AgenciasConfigController controller = new AgenciasConfigController();
            controller.delete(ctx);

            verify(ctx).status(HttpStatus.NOT_FOUND);
            verify(ctx).json(Map.of("error", "no encontrada"));
        }
    }

    @Test
    @DisplayName("delete maneja error genérico con 500")
    void delete_error_500() throws Exception {
        Context ctx = mock(Context.class);
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);
        when(ctx.pathParam("id")).thenReturn("AGX");

        Connection conn = mock(Connection.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<AgenciasConfigDAO> cons =
                     mockConstruction(AgenciasConfigDAO.class,
                             (daoMock, c) -> doThrow(new RuntimeException("otro error"))
                                     .when(daoMock).eliminar("AGX"))) {

            dbMock.when(DB::getConnection).thenReturn(conn);

            AgenciasConfigController controller = new AgenciasConfigController();
            controller.delete(ctx);

            verify(ctx).status(HttpStatus.INTERNAL_SERVER_ERROR);
            verify(ctx).json(Map.of("error", "otro error"));
        }
    }
}
