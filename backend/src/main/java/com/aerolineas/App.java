package com.aerolineas;

import com.aerolineas.config.DB;
import com.aerolineas.controller.*;
import com.aerolineas.http.JsonErrorHandler;
import com.aerolineas.middleware.Auth;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;

import java.util.Map;

public class App {

  private static void requireAuth(Context ctx, Handler next) throws Exception {
    Auth.jwt().handle(ctx);
    if (ctx.attribute("claims") == null) return;
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
    if (ctx.attribute("claims") == null) return;
    if (!isAdmin(ctx)) { ctx.status(403).json(Map.of("error","solo administradores")); return; }
    next.handle(ctx);
  }

  private static int resolvePort(String[] args) {
    String envPort = System.getenv("PORT");
    if (envPort != null && envPort.matches("\\d+")) {
      return Integer.parseInt(envPort);
    }
    String sysPort = System.getProperty("port");
    if (sysPort != null && sysPort.matches("\\d+")) {
      return Integer.parseInt(sysPort);
    }
    if (args != null) {
      for (String a : args) {
        if (a != null && a.startsWith("--port=")) {
          String v = a.substring("--port=".length());
          if (v.matches("\\d+")) return Integer.parseInt(v);
        }
      }
    }
    return 8080;
  }

  private static void parseArgs(String[] args) {
    if (args == null) return;
    for (String arg : args) {
      if (arg == null) continue;
      // Parse --schema=SCHEMA_NAME
      if (arg.startsWith("--schema=")) {
        String schemaValue = arg.substring("--schema=".length());
        if (!schemaValue.isBlank()) {
          System.setProperty("schema", schemaValue);
        }
      }
      // Parse --user=USER_NAME (usuario de Oracle)
      if (arg.startsWith("--user=")) {
        String userValue = arg.substring("--user=".length());
        if (!userValue.isBlank()) {
          System.setProperty("oracle.user", userValue);
        }
      }
      // Parse --password=PASSWORD (contraseña de Oracle)
      if (arg.startsWith("--password=")) {
        String passValue = arg.substring("--password=".length());
        if (!passValue.isBlank()) {
          System.setProperty("oracle.password", passValue);
        }
      }
      // Parse --port=PORT (ya manejado en resolvePort, pero lo dejamos para consistencia)
      // Ya está manejado arriba
    }
  }

  public static void main(String[] args) {
    // Parse argumentos de línea de comandos (--schema=, --port=)
    parseArgs(args);
    
    int port = resolvePort(args);

    var app = Javalin.create(cfg -> cfg.http.defaultContentType = "application/json").start(port);

    app.exception(IllegalArgumentException.class, (e, ctx) -> {
      ctx.status(400).json(Map.of("error", e.getMessage()));
    });
    app.exception(Exception.class, (e, ctx) -> {
      ctx.status(500).json(Map.of("error", e.getMessage() == null ? "Error interno" : e.getMessage()));
    });

    DB.init();

    app.before(ctx -> {
      ctx.header("Access-Control-Allow-Origin", "*");
      ctx.header("Vary", "Origin");
      ctx.header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,PATCH");

      String reqHeaders = ctx.header("Access-Control-Request-Headers");
      String allowHeaders = (reqHeaders != null && !reqHeaders.isBlank())
          ? reqHeaders
          : "Authorization, Content-Type, X-User-Id, X-WebService-Email, X-WebService-Password";
      ctx.header("Access-Control-Allow-Headers", allowHeaders);

      ctx.header("Access-Control-Max-Age", "86400");
    });

    app.options("/*", ctx -> {
      ctx.header("Access-Control-Allow-Origin", "*");
      ctx.header("Vary", "Origin");
      ctx.header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS,PATCH");

      String reqHeaders = ctx.header("Access-Control-Request-Headers");
      String allowHeaders = (reqHeaders != null && !reqHeaders.isBlank())
          ? reqHeaders
          : "Authorization, Content-Type, X-User-Id, X-WebService-Email, X-WebService-Password";
      ctx.header("Access-Control-Allow-Headers", allowHeaders);

      ctx.header("Access-Control-Max-Age", "86400");
      ctx.status(204);
    });

    var authCtrl   = new AuthController();
    var perfilCtrl = new PerfilController();
    var adminUsr   = new AdminUsuarioController();
    var configCtrl = new ConfigController();
    var agenciasCtrl = new AgenciasConfigController();

    app.get("/health", ctx -> ctx.result("OK"));
    app.get("/api/db/ping", ctx -> ctx.json(DB.ping() ? "DB OK" : "DB FAIL"));

    app.post("/api/auth/register", authCtrl::register);
    app.post("/api/auth/login",    authCtrl::login);
    app.get ("/api/auth/me",       ctx -> requireAuth(ctx, authCtrl::me));

    app.get("/api/perfil", ctx -> requireAuth(ctx, perfilCtrl::getPerfil));
    app.put("/api/perfil", ctx -> requireAuth(ctx, perfilCtrl::updatePerfil));

    app.get("/api/admin/usuarios",            ctx -> requireAdmin(ctx, adminUsr::list));
    app.post("/api/admin/usuarios/create-ws", ctx -> requireAdmin(ctx, adminUsr::createWs));
    app.get("/api/admin/usuarios/{id}",       ctx -> requireAdmin(ctx, adminUsr::get));
    app.put("/api/admin/usuarios/{id}",       ctx -> requireAdmin(ctx, adminUsr::update));

    app.get("/api/config",                 configCtrl::getAll);
    app.get("/api/config/{section}",       configCtrl::getBySection);
    app.put("/api/admin/config/{section}", ctx -> requireAdmin(ctx, configCtrl::upsertSection));

    // Configuración de agencias (requiere admin)
    app.get("/api/admin/agencias",                    ctx -> requireAdmin(ctx, agenciasCtrl::list));
    app.get("/api/admin/agencias/{id}",               ctx -> requireAdmin(ctx, agenciasCtrl::get));
    app.post("/api/admin/agencias",                   ctx -> requireAdmin(ctx, agenciasCtrl::create));
    app.put("/api/admin/agencias/{id}",               ctx -> requireAdmin(ctx, agenciasCtrl::update));
    app.delete("/api/admin/agencias/{id}",            ctx -> requireAdmin(ctx, agenciasCtrl::delete));

    // Controladores de dominio
    new PaisController().routes(app);
    new CiudadController().routes(app);
    new RutaController().routes(app);
    new VueloController().routes(app);
    new ClaseController().routes(app);
    new ComprasController().register(app);
    new ComentarioController().routes(app);
    new RatingController().routes(app);
    new PaginasController().routes(app);
    new ContenidoHomeController().routes(app);

    app.exception(Exception.class, JsonErrorHandler.of(500));
  }
}
