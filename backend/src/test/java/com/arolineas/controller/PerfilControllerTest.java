package com.arolineas.controller;

import com.aerolineas.controller.PerfilController;
import com.aerolineas.dao.PaisDAO;
import com.aerolineas.dao.PasajeroDAO;
import com.aerolineas.dao.UsuarioDAO;
import com.aerolineas.dto.UsuarioAdminDTOs;
import com.aerolineas.dto.PaisDTOs;
import com.aerolineas.model.Pasajero;
import com.aerolineas.model.Usuario;
import io.javalin.http.Context;
import io.javalin.validation.BodyValidator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class PerfilControllerTest {

    private Context buildContextWithUserId(long idUsuario) {
        Context ctx = mock(Context.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> claims = (Map<String, Object>)(Map<?, ?>) Map.of("sub", String.valueOf(idUsuario));
        when(ctx.attribute("claims")).thenReturn(claims);
        return ctx;
    }

    @Test
    void getPerfil_ok_conPasajero_devuelvePerfilJson() throws Exception {
        UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);
        PasajeroDAO pasajeroDAO = mock(PasajeroDAO.class);
        PaisDAO paisDAO = mock(PaisDAO.class);

        PerfilController controller = new PerfilController(usuarioDAO, pasajeroDAO, paisDAO);

        Context ctx = buildContextWithUserId(10L);

        Usuario u = mock(Usuario.class);
        when(u.getIdUsuario()).thenReturn(10L);
        when(u.getEmail()).thenReturn("user@test.com");
        when(u.getNombres()).thenReturn("Ana");
        when(u.getApellidos()).thenReturn("López");
        when(u.getIdRol()).thenReturn(3);

        Pasajero p = mock(Pasajero.class);
        when(p.getIdPasajero()).thenReturn(7L);
        LocalDate fn = LocalDate.of(2000, 1, 1);
        when(p.getFechaNacimiento()).thenReturn(fn);
        when(p.getIdPaisDocumento()).thenReturn(1L);
        when(p.getPasaporte()).thenReturn("P123");

        when(usuarioDAO.findById(10L)).thenReturn(u);
        when(pasajeroDAO.findByUsuario(10L)).thenReturn(p);

        controller.getPerfil(ctx);

        verify(usuarioDAO).findById(10L);
        verify(pasajeroDAO).findByUsuario(10L);
        verify(ctx).json(argThat(obj -> {
            if (!(obj instanceof PerfilController.PerfilView view)) return false;
            return view.idUsuario() == 10L
                    && "user@test.com".equals(view.email())
                    && "Ana".equals(view.nombres())
                    && "López".equals(view.apellidos())
                    && view.pasajero() != null
                    && view.pasajero().idPasajero().equals(7L)
                    && "P123".equals(view.pasajero().pasaporte());
        }));
        verify(ctx, never()).status(500);
    }

    @Test
    void getPerfil_sinPasajero_devuelvePerfilConPasajeroNull() throws Exception {
        UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);
        PasajeroDAO pasajeroDAO = mock(PasajeroDAO.class);
        PaisDAO paisDAO = mock(PaisDAO.class);

        PerfilController controller = new PerfilController(usuarioDAO, pasajeroDAO, paisDAO);

        Context ctx = buildContextWithUserId(11L);

        Usuario u = mock(Usuario.class);
        when(u.getIdUsuario()).thenReturn(11L);
        when(u.getEmail()).thenReturn("otra@test.com");
        when(u.getNombres()).thenReturn("Luis");
        when(u.getApellidos()).thenReturn("Pérez");
        when(u.getIdRol()).thenReturn(3);

        when(usuarioDAO.findById(11L)).thenReturn(u);
        when(pasajeroDAO.findByUsuario(11L)).thenReturn(null);

        controller.getPerfil(ctx);

        verify(ctx).json(argThat(obj -> {
            if (!(obj instanceof PerfilController.PerfilView view)) return false;
            return view.idUsuario() == 11L && view.pasajero() == null;
        }));
    }

    @Test
    void getPerfil_errorEnDao_responde500() throws Exception {
        UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);
        PasajeroDAO pasajeroDAO = mock(PasajeroDAO.class);
        PaisDAO paisDAO = mock(PaisDAO.class);

        PerfilController controller = new PerfilController(usuarioDAO, pasajeroDAO, paisDAO);

        Context ctx = buildContextWithUserId(15L);
        when(usuarioDAO.findById(15L)).thenThrow(new RuntimeException("fail"));

        when(ctx.status(500)).thenReturn(ctx);

        controller.getPerfil(ctx);

        verify(ctx).status(500);
        verify(ctx).json(argThat(body ->
                body instanceof Map &&
                        "No se pudo obtener el perfil".equals(((Map<?, ?>) body).get("error"))
        ));
    }

    @Test
    void updatePerfil_ok_actualizaYDevuelveOk() throws Exception {
        UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);
        PasajeroDAO pasajeroDAO = mock(PasajeroDAO.class);
        PaisDAO paisDAO = mock(PaisDAO.class);

        PerfilController controller = new PerfilController(usuarioDAO, pasajeroDAO, paisDAO);

        Context ctx = buildContextWithUserId(20L);

        @SuppressWarnings("unchecked")
        BodyValidator<PerfilController.UpdPerfilReq> validator =
                (BodyValidator<PerfilController.UpdPerfilReq>) mock(BodyValidator.class);

        when(ctx.bodyValidator(PerfilController.UpdPerfilReq.class)).thenReturn(validator);
        when(validator.check(any(), anyString())).thenReturn(validator);

        PerfilController.UpdPerfilReq req = new PerfilController.UpdPerfilReq(
                "  Ana  ",
                "  López ",
                "newpass",
                "2000-01-01",
                1L,
                "  P123  "
        );
        when(validator.get()).thenReturn(req);

        controller.updatePerfil(ctx);

        verify(usuarioDAO).selfUpdate(eq(20L), any(UsuarioAdminDTOs.UpdateSelf.class));
        verify(ctx).json(Map.of("ok", true));
        verify(ctx, never()).status(400);
        verify(ctx, never()).status(500);
    }

    @Test
    void updatePerfil_fechaNacimientoFutura_devuelve400YNoActualiza() throws Exception {
        UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);
        PasajeroDAO pasajeroDAO = mock(PasajeroDAO.class);
        PaisDAO paisDAO = mock(PaisDAO.class);

        PerfilController controller = new PerfilController(usuarioDAO, pasajeroDAO, paisDAO);

        Context ctx = buildContextWithUserId(21L);

        @SuppressWarnings("unchecked")
        BodyValidator<PerfilController.UpdPerfilReq> validator =
                (BodyValidator<PerfilController.UpdPerfilReq>) mock(BodyValidator.class);

        when(ctx.bodyValidator(PerfilController.UpdPerfilReq.class)).thenReturn(validator);
        when(validator.check(any(), anyString())).thenReturn(validator);

        String futureDate = LocalDate.now().plusDays(1).toString();

        PerfilController.UpdPerfilReq req = new PerfilController.UpdPerfilReq(
                "Ana",
                "López",
                null,
                futureDate,
                1L,
                "P123"
        );
        when(validator.get()).thenReturn(req);
        when(ctx.status(400)).thenReturn(ctx);

        controller.updatePerfil(ctx);

        verify(usuarioDAO, never()).selfUpdate(anyLong(), any());
        verify(ctx).status(400);
        verify(ctx).json(argThat(body ->
                body instanceof Map &&
                        "fecha de nacimiento inválida".equals(((Map<?, ?>) body).get("error"))
        ));
    }

    @Test
    void updatePerfil_paisInvalido_devuelve400YNoActualiza() throws Exception {
        UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);
        PasajeroDAO pasajeroDAO = mock(PasajeroDAO.class);
        PaisDAO paisDAO = mock(PaisDAO.class);

        PerfilController controller = new PerfilController(usuarioDAO, pasajeroDAO, paisDAO);

        Context ctx = buildContextWithUserId(22L);

        @SuppressWarnings("unchecked")
        BodyValidator<PerfilController.UpdPerfilReq> validator =
                (BodyValidator<PerfilController.UpdPerfilReq>) mock(BodyValidator.class);

        when(ctx.bodyValidator(PerfilController.UpdPerfilReq.class)).thenReturn(validator);
        when(validator.check(any(), anyString())).thenReturn(validator);

        PerfilController.UpdPerfilReq req = new PerfilController.UpdPerfilReq(
                "Ana",
                "López",
                null,
                null,
                0L,
                "P123"
        );
        when(validator.get()).thenReturn(req);
        when(ctx.status(400)).thenReturn(ctx);

        controller.updatePerfil(ctx);

        verify(usuarioDAO, never()).selfUpdate(anyLong(), any());
        verify(ctx).status(400);
        verify(ctx).json(argThat(body ->
                body instanceof Map &&
                        "país inválido".equals(((Map<?, ?>) body).get("error"))
        ));
    }

    @Test
    void updatePerfil_errorEnDao_responde500() throws Exception {
        UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);
        PasajeroDAO pasajeroDAO = mock(PasajeroDAO.class);
        PaisDAO paisDAO = mock(PaisDAO.class);

        PerfilController controller = new PerfilController(usuarioDAO, pasajeroDAO, paisDAO);

        Context ctx = buildContextWithUserId(23L);

        @SuppressWarnings("unchecked")
        BodyValidator<PerfilController.UpdPerfilReq> validator =
                (BodyValidator<PerfilController.UpdPerfilReq>) mock(BodyValidator.class);

        when(ctx.bodyValidator(PerfilController.UpdPerfilReq.class)).thenReturn(validator);
        when(validator.check(any(), anyString())).thenReturn(validator);

        PerfilController.UpdPerfilReq req = new PerfilController.UpdPerfilReq(
                "Ana",
                "López",
                null,
                null,
                null,
                null
        );
        when(validator.get()).thenReturn(req);

        doThrow(new RuntimeException("boom"))
                .when(usuarioDAO).selfUpdate(anyLong(), any(UsuarioAdminDTOs.UpdateSelf.class));

        when(ctx.status(500)).thenReturn(ctx);

        controller.updatePerfil(ctx);

        verify(ctx).status(500);
        verify(ctx).json(argThat(body ->
                body instanceof Map &&
                        "No se pudo guardar el perfil".equals(((Map<?, ?>) body).get("error"))
        ));
    }

    @Test
    void listPaises_ok_devuelveJson() throws Exception {
        UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);
        PasajeroDAO pasajeroDAO = mock(PasajeroDAO.class);
        PaisDAO paisDAO = mock(PaisDAO.class);

        PerfilController controller = new PerfilController(usuarioDAO, pasajeroDAO, paisDAO);

        Context ctx = mock(Context.class);

        var lista = List.of(
                new PaisDTOs.View(1L, "Guatemala"),
                new PaisDTOs.View(2L, "El Salvador")
        );
        when(paisDAO.listAll()).thenReturn(lista);

        controller.listPaises(ctx);

        verify(paisDAO).listAll();
        verify(ctx).json(lista);
    }

    @Test
    void listPaises_error_responde500() throws Exception {
        UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);
        PasajeroDAO pasajeroDAO = mock(PasajeroDAO.class);
        PaisDAO paisDAO = mock(PaisDAO.class);

        PerfilController controller = new PerfilController(usuarioDAO, pasajeroDAO, paisDAO);

        Context ctx = mock(Context.class);
        when(paisDAO.listAll()).thenThrow(new RuntimeException("fail"));
        when(ctx.status(500)).thenReturn(ctx);

        controller.listPaises(ctx);

        verify(ctx).status(500);
        verify(ctx).json(argThat(body ->
                body instanceof Map &&
                        "No se pudo listar países".equals(((Map<?, ?>) body).get("error"))
        ));
    }

    @Test
    void getPerfil_conPasajero_sinFechaNacimiento_edadNull() throws Exception {
        UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);
        PasajeroDAO pasajeroDAO = mock(PasajeroDAO.class);
        PaisDAO paisDAO = mock(PaisDAO.class);

        PerfilController controller = new PerfilController(usuarioDAO, pasajeroDAO, paisDAO);

        Context ctx = buildContextWithUserId(30L);

        Usuario u = mock(Usuario.class);
        when(u.getIdUsuario()).thenReturn(30L);
        when(u.getEmail()).thenReturn("nf@test.com");
        when(u.getNombres()).thenReturn("NoFecha");
        when(u.getApellidos()).thenReturn("User");
        when(u.getIdRol()).thenReturn(3);

        Pasajero p = mock(Pasajero.class);
        when(p.getIdPasajero()).thenReturn(99L);
        when(p.getFechaNacimiento()).thenReturn(null);
        when(p.getIdPaisDocumento()).thenReturn(1L);
        when(p.getPasaporte()).thenReturn("PX");

        when(usuarioDAO.findById(30L)).thenReturn(u);
        when(pasajeroDAO.findByUsuario(30L)).thenReturn(p);

        controller.getPerfil(ctx);

        verify(ctx).json(argThat(obj -> {
            if (!(obj instanceof PerfilController.PerfilView view)) return false;
            var pv = view.pasajero();
            return pv != null && pv.fechaNacimiento() == null && pv.edad() == null;
        }));
    }

    @Test
    void getPerfil_sinClaims_devuelve500() throws Exception {
        UsuarioDAO usuarioDAO = mock(UsuarioDAO.class);
        PasajeroDAO pasajeroDAO = mock(PasajeroDAO.class);
        PaisDAO paisDAO = mock(PaisDAO.class);

        PerfilController controller = new PerfilController(usuarioDAO, pasajeroDAO, paisDAO);

        Context ctx = mock(Context.class);

        when(ctx.attribute("claims")).thenReturn(null);
        when(ctx.status(500)).thenReturn(ctx);

        controller.getPerfil(ctx);

        verify(ctx).status(500);
        verify(ctx).json(argThat(body ->
                body instanceof Map &&
                        "No se pudo obtener el perfil".equals(((Map<?, ?>) body).get("error"))
        ));
    }
}
