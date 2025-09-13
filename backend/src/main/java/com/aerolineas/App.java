package com.aerolineas;

import com.aerolineas.config.DB;
import com.aerolineas.controller.*;
import com.aerolineas.middleware.Auth;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;

import java.util.Map;

public class App {

  private static void requireAuth(Context ctx, Handler next) throws Exception {
    Auth.jwt().handle(ctx);
    if (ctx.attribute("claims") == null) return; // no autenticado
    next.handle(ctx);
  }

  private static boolean isAdmin(Context ctx) {
    @SuppressWarnings("unchecked")
    Map<String,Object> claims = ctx.attribute("claims");
    if (claims == null) return false;
    Object rol = claims.get("rol");
    try { return Integer.parseInt(String.valueOf(rol)) == 1; }
    catch (Exception e) { return false; }
  }

  private static void requireAdmin(Context ctx, Handler next) throws Exception {
    Auth.jwt().handle(ctx);
    if (ctx.attribute("claims") == null) return; // no autenticado
    if (!isAdmin(ctx)) { ctx.status(403).json(Map.of("error","solo administradores")); return; }
    next.handle(ctx);
  }

  public static void main(String[] args) {
    var app = Javalin.create(cfg -> cfg.http.defaultContentType = "application/json").start(8080);
    app.exception(IllegalArgumentException.class, (e, ctx) -> {
      ctx.status(400).json(Map.of("error", e.getMessage()));
    });
    app.exception(Exception.class, (e, ctx) -> {
      ctx.status(500).json(Map.of("error", e.getMessage() == null ? "Error interno" : e.getMessage()));
    });

    DB.init();

    app.before(ctx -> {
      ctx.header("Access-Control-Allow-Origin", "*");
      ctx.header("Access-Control-Allow-Headers", "Authorization, Content-Type");
      ctx.header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
    });
    app.options("/*", ctx -> ctx.status(204));

    var authCtrl   = new AuthController();
    var perfilCtrl = new PerfilController();
    var adminUsr   = new AdminUsuarioController(); 
    app.get("/health", ctx -> ctx.result("OK"));
    app.get("/api/db/ping", ctx -> ctx.json(DB.ping() ? "DB OK" : "DB FAIL"));

    app.post("/api/auth/register", authCtrl::register);
    app.post("/api/auth/login",    authCtrl::login);
    app.get ("/api/auth/me",       ctx -> requireAuth(ctx, authCtrl::me));

    app.get("/api/perfil", ctx -> requireAuth(ctx, perfilCtrl::getPerfil));
    app.put("/api/perfil", ctx -> requireAuth(ctx, perfilCtrl::updatePerfil));

    app.get("/api/admin/usuarios",            ctx -> requireAdmin(ctx, adminUsr::list));
    app.get("/api/admin/usuarios/{id}",       ctx -> requireAdmin(ctx, adminUsr::get));
    app.put("/api/admin/usuarios/{id}",       ctx -> requireAdmin(ctx, adminUsr::update));

    new PaisController().routes(app);
    new CiudadController().routes(app);
    new RutaController().routes(app);
    new VueloController().routes(app);
    new ClaseController().routes(app);
  }
}
