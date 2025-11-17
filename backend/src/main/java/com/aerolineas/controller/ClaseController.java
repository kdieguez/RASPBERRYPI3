package com.aerolineas.controller;

import com.aerolineas.dao.ClaseDAO;
import com.aerolineas.middleware.WebServiceAuth;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class ClaseController {
  private final ClaseDAO dao = new ClaseDAO();

  private void validateOptionalWebService(Context ctx) throws Exception {
    String wsEmail = ctx.header("X-WebService-Email");
    String wsPassword = ctx.header("X-WebService-Password");
    
    if (wsEmail != null && !wsEmail.isBlank() && wsPassword != null && !wsPassword.isBlank()) {
      WebServiceAuth.validate().handle(ctx);
    }
  }

  public void routes(Javalin app) {
    app.get("/api/public/clases", ctx -> {
      validateOptionalWebService(ctx);
      ctx.json(dao.listAll());
    });
  }
}
