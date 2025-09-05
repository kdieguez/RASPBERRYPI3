package com.aerolineas;

import com.aerolineas.config.DB;
import io.javalin.Javalin;
import com.aerolineas.controller.*;

public class App {
  public static void main(String[] args) {
    var app = Javalin.create(cfg -> cfg.http.defaultContentType = "application/json")
                     .start(8080);

    DB.init();

    app.before(ctx -> {
      ctx.header("Access-Control-Allow-Origin", "*");
      ctx.header("Access-Control-Allow-Headers", "Authorization, Content-Type");
      ctx.header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
    });
    app.options("/*", ctx -> ctx.status(204));

    app.get("/health", ctx -> ctx.result("OK"));
    app.get("/api/db/ping", ctx -> ctx.json(DB.ping() ? "DB OK" : "DB FAIL"));

    new PaisController().routes(app);
    new CiudadController().routes(app);
    new RutaController().routes(app);
    new VueloController().routes(app);
  }
}
