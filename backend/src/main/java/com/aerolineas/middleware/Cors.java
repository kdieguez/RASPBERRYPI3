package com.aerolineas.middleware;

import io.javalin.Javalin;
import io.javalin.http.Context;

public final class Cors {

  private Cors() {}

  private static final String ALLOWED_METHODS = "GET,POST,PUT,DELETE,OPTIONS,PATCH";
  private static final String DEFAULT_ALLOWED_HEADERS = "Authorization, Content-Type, X-User-Id, x-user-id";

  private static void setHeaders(Context ctx) {
    ctx.header("Access-Control-Allow-Origin", "*");
    ctx.header("Vary", "Origin");
    ctx.header("Access-Control-Allow-Methods", ALLOWED_METHODS);
    ctx.header("Access-Control-Max-Age", "86400");

    String reqHeaders = ctx.header("Access-Control-Request-Headers");
    if (reqHeaders != null && !reqHeaders.isBlank()) {
      ctx.header("Access-Control-Allow-Headers", reqHeaders);
    } else {
      ctx.header("Access-Control-Allow-Headers", DEFAULT_ALLOWED_HEADERS);
    }
  }

  public static void install(Javalin app) {
    app.before(Cors::setHeaders);
    app.options("/*", ctx -> {
      setHeaders(ctx);
      ctx.status(204);
    });
  }
}
