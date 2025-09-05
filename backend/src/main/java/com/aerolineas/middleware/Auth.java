package com.aerolineas.middleware;

import io.javalin.http.Handler;

public class Auth {
    public static Handler adminOrEmpleado() {
        return ctx -> {
            String role = ctx.header("X-Role");
            boolean ok = role != null && (role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("empleado"));
            if (!ok) {
                ctx.status(401).result("No autorizado");
                ctx.skipRemainingHandlers(); 
            }
        };
    }
}
