package com.aerolineas.controller;

import com.aerolineas.dao.ClaseDAO;
import com.aerolineas.middleware.WebServiceAuth;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;

public class ClaseController {

  private final ClaseDAO dao;
  private final Handler wsValidator;

  public ClaseController() {
    this(new ClaseDAO(), WebServiceAuth.validate());
  }

  public ClaseController(ClaseDAO dao, Handler wsValidator) {
    this.dao = dao;
    this.wsValidator = wsValidator;
  }

  void validateOptionalWebService(Context ctx) throws Exception {
    String wsEmail = ctx.header("X-WebService-Email");
    String wsPassword = ctx.header("X-WebService-Password");

    if (wsEmail != null && !wsEmail.isBlank()
        && wsPassword != null && !wsPassword.isBlank()) {
      wsValidator.handle(ctx);
    }
  }

  public void routes(Javalin app) {
    app.get("/api/public/clases", ctx -> {
      validateOptionalWebService(ctx);
      ctx.json(dao.listAll());
    });
  }
}
