package com.aerolineas.controller;

import com.aerolineas.dao.ClaseDAO;
import io.javalin.Javalin;

public class ClaseController {
  private final ClaseDAO dao = new ClaseDAO();

  public void routes(Javalin app) {
    app.get("/api/public/clases", ctx -> ctx.json(dao.listAll()));
  }
}
