package com.aerolineas.middleware;

import com.aerolineas.dao.UsuarioDAO;
import com.aerolineas.model.Usuario;
import com.aerolineas.util.PasswordUtil;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.UnauthorizedResponse;

import java.util.Map;

public final class WebServiceAuth {
    private WebServiceAuth() {}

    private static String normEmail(String e) {
        return e == null ? null : e.trim().toLowerCase();
    }

    public static Handler validate() {
        return ctx -> {

            if ("OPTIONS".equalsIgnoreCase(String.valueOf(ctx.method()))) {
                return;
            }

            String email = ctx.header("X-WebService-Email");
            String password = ctx.header("X-WebService-Password");

            if (email == null || email.isBlank() || password == null || password.isBlank()) {
                throw new UnauthorizedResponse("Credenciales WebService requeridas: X-WebService-Email y X-WebService-Password");
            }

            email = normEmail(email);

            UsuarioDAO usuarioDAO = new UsuarioDAO();
            Usuario usuario = usuarioDAO.findByEmail(email);

            if (usuario == null) {
                throw new UnauthorizedResponse("Credenciales WebService inválidas");
            }

            if (!usuario.isHabilitado()) {
                throw new UnauthorizedResponse("Usuario WebService deshabilitado");
            }

            if (usuario.getIdRol() != 2) {
                throw new UnauthorizedResponse("Usuario no es de tipo WebService");
            }

            if (!PasswordUtil.verify(password, usuario.getContrasenaHash())) {
                throw new UnauthorizedResponse("Credenciales WebService inválidas");
            }

            Map<String, Object> claims = Map.of(
                "sub", String.valueOf(usuario.getIdUsuario()),
                "idUsuario", String.valueOf(usuario.getIdUsuario()),
                "email", usuario.getEmail(),
                "rol", usuario.getIdRol(),
                "name", usuario.getNombres() + " " + usuario.getApellidos()
            );
            ctx.attribute("claims", claims);
        };
    }
}

