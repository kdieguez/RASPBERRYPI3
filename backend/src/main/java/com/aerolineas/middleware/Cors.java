package com.aerolineas.middleware;

import io.javalin.Javalin;
import io.javalin.http.Context;

public final class Cors {

  private Cors() {}

  private static void setHeaders(Context ctx) {
    ctx.header("Access-Control-Allow-Origin", "*");
    ctx.header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
    ctx.header("Access-Control-Allow-Headers", "Authorization, Content-Type");
  }

  public static void install(Javalin app) {
    app.before(Cors::setHeaders);
    
    app.options("/*", ctx -> {
      setHeaders(ctx);
      ctx.status(204);
    });
  }
}
