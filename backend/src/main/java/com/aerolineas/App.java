package com.aerolineas;

import com.aerolineas.config.DB;
import io.javalin.Javalin;

public class App {
  public static void main(String[] args) {
    var app = Javalin.create(/*cfg -> {}*/).start(8080);

    app.before(ctx -> {
      ctx.header("Access-Control-Allow-Origin", "*");
      ctx.header("Access-Control-Allow-Headers", "Authorization, Content-Type");
      ctx.header("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
    });
    app.options("/*", ctx -> ctx.status(204));

    app.get("/health", ctx -> ctx.result("OK"));
    app.get("/api/db/ping", ctx -> ctx.json(DB.ping() ? "DB OK" : "DB FAIL"));
  }
}
