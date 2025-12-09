package com.arolineas.controller;

import com.aerolineas.config.DB;
import com.aerolineas.controller.ConfigController;
import com.aerolineas.dao.ConfigDAO;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ConfigControllerTest {

    @Test
    void getAll_ok_devuelveJsonConConfig() throws Exception {
        ConfigDAO dao = mock(ConfigDAO.class);
        ConfigController controller = new ConfigController(dao);

        Context ctx = mock(Context.class);

        Map<String, Map<String, String>> data = Map.of(
                "header", Map.of(
                        "title", "Aerolíneas",
                        "logoUrl", "https://example.com/logo.png"
                ),
                "footer", Map.of(
                        "phone", "12345678",
                        "email", "info@aerolineas.com"
                )
        );

        when(dao.getAll()).thenReturn(data);

        controller.getAll(ctx);

        verify(dao).getAll();
        verify(ctx).json(data);
        verify(ctx, never()).status(anyInt());
    }

    @Test
    void getAll_errorEnDao_devuelve500ConError() throws Exception {
        ConfigDAO dao = mock(ConfigDAO.class);
        ConfigController controller = new ConfigController(dao);

        Context ctx = mock(Context.class);
        when(dao.getAll()).thenThrow(new RuntimeException("DB down"));
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);

        controller.getAll(ctx);

        verify(dao).getAll();
        verify(ctx).status(HttpStatus.INTERNAL_SERVER_ERROR);
        verify(ctx).json(argThat(body ->
                body instanceof Map &&
                        "DB down".equals(((Map<?, ?>) body).get("error"))
        ));
    }

    @Test
    void getBySection_header_ok() throws Exception {
        ConfigDAO dao = mock(ConfigDAO.class);
        ConfigController controller = new ConfigController(dao);

        Context ctx = mock(Context.class);
        when(ctx.pathParam("section")).thenReturn("header");

        Map<String, String> headerConfig = Map.of(
                "title", "Aerolíneas",
                "logoUrl", "https://example.com/logo.png"
        );
        when(dao.getSection("header")).thenReturn(headerConfig);

        controller.getBySection(ctx);

        verify(dao).getSection("header");
        verify(ctx).json(headerConfig);
        verify(ctx, never()).status(anyInt());
    }

    @Test
    void getBySection_footer_ok() throws Exception {
        ConfigDAO dao = mock(ConfigDAO.class);
        ConfigController controller = new ConfigController(dao);

        Context ctx = mock(Context.class);
        when(ctx.pathParam("section")).thenReturn("footer");

        Map<String, String> footerConfig = Map.of(
                "phone", "12345678",
                "email", "info@aerolineas.com"
        );
        when(dao.getSection("footer")).thenReturn(footerConfig);

        controller.getBySection(ctx);

        verify(dao).getSection("footer");
        verify(ctx).json(footerConfig);
        verify(ctx, never()).status(anyInt());
    }

    @Test
    void getBySection_sectionInvalida_devuelve400YNoLlamaDao() throws Exception {
        ConfigDAO dao = mock(ConfigDAO.class);
        ConfigController controller = new ConfigController(dao);

        Context ctx = mock(Context.class);
        when(ctx.pathParam("section")).thenReturn("sidebar");
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);

        controller.getBySection(ctx);

        verify(ctx).status(HttpStatus.BAD_REQUEST);
        verify(ctx).json(argThat(body ->
                body instanceof Map &&
                        "section inválida".equals(((Map<?, ?>) body).get("error"))
        ));
        verify(dao, never()).getSection(anyString());
    }

    @Test
    void getBySection_errorEnDao_devuelve500() throws Exception {
        ConfigDAO dao = mock(ConfigDAO.class);
        ConfigController controller = new ConfigController(dao);

        Context ctx = mock(Context.class);
        when(ctx.pathParam("section")).thenReturn("header");
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);

        when(dao.getSection("header")).thenThrow(new RuntimeException("falló"));

        controller.getBySection(ctx);

        verify(dao).getSection("header");
        verify(ctx).status(HttpStatus.INTERNAL_SERVER_ERROR);
        verify(ctx).json(argThat(body ->
                body instanceof Map &&
                        "falló".equals(((Map<?, ?>) body).get("error"))
        ));
    }

    @Test
    void upsertSection_header_ok_devuelve204() throws Exception {
        ConfigDAO dao = mock(ConfigDAO.class);
        ConfigController controller = new ConfigController(dao);

        Context ctx = mock(Context.class);
        when(ctx.pathParam("section")).thenReturn("header");

        Map<String, String> body = Map.of(
                "title", "Nueva Aerolínea",
                "logoUrl", "https://example.com/nuevo-logo.png"
        );
        when(ctx.bodyAsClass(Map.class)).thenReturn(body);
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);

        controller.upsertSection(ctx);

        verify(dao).upsertSection("header", body);
        verify(ctx).status(HttpStatus.NO_CONTENT);
    }

    @Test
    void upsertSection_footer_ok_devuelve204() throws Exception {
        ConfigDAO dao = mock(ConfigDAO.class);
        ConfigController controller = new ConfigController(dao);

        Context ctx = mock(Context.class);
        when(ctx.pathParam("section")).thenReturn("footer");

        Map<String, String> body = Map.of(
                "phone", "5555-5555",
                "email", "contacto@aerolineas.com"
        );
        when(ctx.bodyAsClass(Map.class)).thenReturn(body);
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);

        controller.upsertSection(ctx);

        verify(dao).upsertSection("footer", body);
        verify(ctx).status(HttpStatus.NO_CONTENT);
    }

    @Test
    void upsertSection_sectionInvalida_devuelve400YNoLlamaDao() throws Exception {
        ConfigDAO dao = mock(ConfigDAO.class);
        ConfigController controller = new ConfigController(dao);

        Context ctx = mock(Context.class);
        when(ctx.pathParam("section")).thenReturn("sidebar");
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);

        controller.upsertSection(ctx);

        verify(ctx).status(HttpStatus.BAD_REQUEST);
        verify(ctx).json(argThat(body ->
                body instanceof Map &&
                        "section inválida".equals(((Map<?, ?>) body).get("error"))
        ));
        verify(dao, never()).upsertSection(anyString(), anyMap());
    }

    @Test
    void upsertSection_errorEnDao_devuelve500() throws Exception {
        ConfigDAO dao = mock(ConfigDAO.class);
        ConfigController controller = new ConfigController(dao);

        Context ctx = mock(Context.class);
        when(ctx.pathParam("section")).thenReturn("header");
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);

        Map<String, String> body = Map.of("title", "X");
        when(ctx.bodyAsClass(Map.class)).thenReturn(body);

        doThrow(new RuntimeException("falló upsert"))
                .when(dao).upsertSection("header", body);

        controller.upsertSection(ctx);

        verify(dao).upsertSection("header", body);
        verify(ctx).status(HttpStatus.INTERNAL_SERVER_ERROR);
        verify(ctx).json(argThat(resp ->
                resp instanceof Map &&
                        "falló upsert".equals(((Map<?, ?>) resp).get("error"))
        ));
    }    

    @Test
    void getAll_sinDaoInyectado_usaDBYConfigDAO() throws Exception {
        ConfigController controller = new ConfigController(); 
        Context ctx = mock(Context.class);

        Map<String, Map<String, String>> data = Map.of(
                "header", Map.of("title", "Aerolíneas"),
                "footer", Map.of("phone", "12345678")
        );

        Connection mockConn = mock(Connection.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<ConfigDAO> daoConstruction =
                     mockConstruction(ConfigDAO.class, (mockDao, context) -> {
                         when(mockDao.getAll()).thenReturn(data);
                     })) {

            dbMock.when(DB::getConnection).thenReturn(mockConn);

            controller.getAll(ctx);

            verify(ctx).json(data);
        }
    }

    @Test
    void getBySection_sinDaoInyectado_header_ok() throws Exception {
        ConfigController controller = new ConfigController(); 
        Context ctx = mock(Context.class);
        when(ctx.pathParam("section")).thenReturn("header");

        Map<String, String> headerConfig = Map.of(
                "title", "Aerolíneas",
                "logoUrl", "https://example.com/logo.png"
        );

        Connection mockConn = mock(Connection.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<ConfigDAO> daoConstruction =
                     mockConstruction(ConfigDAO.class, (mockDao, context) -> {
                         when(mockDao.getSection("header")).thenReturn(headerConfig);
                     })) {

            dbMock.when(DB::getConnection).thenReturn(mockConn);

            controller.getBySection(ctx);

            verify(ctx).json(headerConfig);
        }
    }

    @Test
    void upsertSection_sinDaoInyectado_header_ok() throws Exception {
        ConfigController controller = new ConfigController(); 
        Context ctx = mock(Context.class);
        when(ctx.pathParam("section")).thenReturn("header");

        Map<String, String> body = Map.of(
                "title", "Nueva Aerolínea",
                "logoUrl", "https://example.com/nuevo-logo.png"
        );
        when(ctx.bodyAsClass(Map.class)).thenReturn(body);
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);

        Connection mockConn = mock(Connection.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<ConfigDAO> daoConstruction =
                     mockConstruction(ConfigDAO.class, (mockDao, context) -> {
                         
                     })) {

            dbMock.when(DB::getConnection).thenReturn(mockConn);

            controller.upsertSection(ctx);

            verify(ctx).status(HttpStatus.NO_CONTENT);
        }
    }

    

    @Test
    void getAll_sinDaoInyectado_errorEnDao_devuelve500() throws Exception {
        ConfigController controller = new ConfigController(); 
        Context ctx = mock(Context.class);
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);

        Connection mockConn = mock(Connection.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<ConfigDAO> daoConstruction =
                     mockConstruction(ConfigDAO.class, (mockDao, context) -> {
                         when(mockDao.getAll()).thenThrow(new RuntimeException("falló all"));
                     })) {

            dbMock.when(DB::getConnection).thenReturn(mockConn);

            controller.getAll(ctx);

            verify(ctx).status(HttpStatus.INTERNAL_SERVER_ERROR);
            verify(ctx).json(argThat(body ->
                    body instanceof Map &&
                            "falló all".equals(((Map<?, ?>) body).get("error"))
            ));
        }
    }

    @Test
    void getBySection_sinDaoInyectado_errorEnDao_devuelve500() throws Exception {
        ConfigController controller = new ConfigController(); 
        Context ctx = mock(Context.class);
        when(ctx.pathParam("section")).thenReturn("header");
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);

        Connection mockConn = mock(Connection.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<ConfigDAO> daoConstruction =
                     mockConstruction(ConfigDAO.class, (mockDao, context) -> {
                         when(mockDao.getSection("header"))
                                 .thenThrow(new RuntimeException("falló header"));
                     })) {

            dbMock.when(DB::getConnection).thenReturn(mockConn);

            controller.getBySection(ctx);

            verify(ctx).status(HttpStatus.INTERNAL_SERVER_ERROR);
            verify(ctx).json(argThat(body ->
                    body instanceof Map &&
                            "falló header".equals(((Map<?, ?>) body).get("error"))
            ));
        }
    }

    @Test
    void upsertSection_sinDaoInyectado_errorEnDao_devuelve500() throws Exception {
        ConfigController controller = new ConfigController(); 
        Context ctx = mock(Context.class);
        when(ctx.pathParam("section")).thenReturn("header");
        when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);

        Map<String, String> body = Map.of("title", "X");
        when(ctx.bodyAsClass(Map.class)).thenReturn(body);

        Connection mockConn = mock(Connection.class);

        try (MockedStatic<DB> dbMock = mockStatic(DB.class);
             MockedConstruction<ConfigDAO> daoConstruction =
                     mockConstruction(ConfigDAO.class, (mockDao, context) -> {
                         doThrow(new RuntimeException("falló upsert db"))
                                 .when(mockDao).upsertSection("header", body);
                     })) {

            dbMock.when(DB::getConnection).thenReturn(mockConn);

            controller.upsertSection(ctx);

            verify(ctx).status(HttpStatus.INTERNAL_SERVER_ERROR);
            verify(ctx).json(argThat(resp ->
                    resp instanceof Map &&
                            "falló upsert db".equals(((Map<?, ?>) resp).get("error"))
            ));
        }
    }
}
