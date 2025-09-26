package com.aerolineas.controller;

import io.javalin.Javalin;
import io.javalin.http.Context;

import com.aerolineas.dao.ComprasDAO;
import com.aerolineas.dto.CompraDTO.AddItemReq;
import com.aerolineas.dto.CompraDTO.UpdateQtyReq;
import com.aerolineas.dto.CompraDTO.CheckoutResp;
import com.aerolineas.dto.CompraDTO.CarritoResp;
import com.aerolineas.dto.CompraDTO.PaymentReq;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

public class ComprasController {

  private final ComprasDAO dao = new ComprasDAO();

  private long getUserId(Context ctx) {
    @SuppressWarnings("unchecked")
    Map<String, Object> claims = ctx.attribute("claims");
    if (claims != null) {
      Object id = claims.get("idUsuario");
      if (id == null) id = claims.get("id");
      if (id == null) id = claims.get("userId");
      if (id == null) id = claims.get("sub");
      if (id != null) try { return Long.parseLong(String.valueOf(id)); } catch (Exception ignore) {}
    }
    String h = ctx.header("X-User-Id"); 
    if (h != null && !h.isBlank()) return Long.parseLong(h);
    throw new IllegalStateException("Usuario no autenticado");
  }

  private static final Locale LOCALE_GT = Locale.forLanguageTag("es-GT");
  private static String money(BigDecimal n) {
    if (n == null) n = BigDecimal.ZERO;
    return NumberFormat.getCurrencyInstance(LOCALE_GT).format(n);
  }
  private static String dt(String s) {
    try {
      Instant ins = Instant.parse(s);
      LocalDateTime ldt = LocalDateTime.ofInstant(ins, ZoneId.systemDefault());
      return ldt.format(DateTimeFormatter.ofPattern("d 'de' MMM yyyy, h:mm a", new Locale("es", "MX")));
    } catch (Exception e) {
      return s != null ? s : "—";
    }
  }
  private static String getOpt(Object bean, String field) {
    if (bean == null) return "";
    try {
      var f = bean.getClass().getDeclaredField(field);
      f.setAccessible(true);
      Object v = f.get(bean);
      return v == null ? "" : String.valueOf(v);
    } catch (Exception ignore) { return ""; }
  }

  public void register(Javalin app) {

    app.get("/api/compras/carrito", ctx -> {
      try {
        long userId = getUserId(ctx);
        CarritoResp resp = dao.getCart(userId);
        ctx.json(resp);
      } catch (Exception e) {
        ctx.status(400).json(Map.of("error", e.getMessage()));
      }
    });

    app.post("/api/compras/items", ctx -> {
      try {
        long userId = getUserId(ctx);
        AddItemReq req = ctx.bodyAsClass(AddItemReq.class);
        if (req == null) throw new IllegalArgumentException("Solicitud inválida");
        dao.addOrIncrementItem(userId, req.idVuelo, req.idClase, req.cantidad <= 0 ? 1 : req.cantidad);
        ctx.status(201);
      } catch (Exception e) {
        ctx.status(400).json(Map.of("error", e.getMessage()));
      }
    });

    app.put("/api/compras/items/{idItem}", ctx -> {
      try {
        long userId = getUserId(ctx);
        long idItem = Long.parseLong(ctx.pathParam("idItem"));
        UpdateQtyReq req = ctx.bodyAsClass(UpdateQtyReq.class);
        dao.updateQuantity(userId, idItem, req.cantidad);
        ctx.status(204);
      } catch (Exception e) {
        ctx.status(400).json(Map.of("error", e.getMessage()));
      }
    });

    app.delete("/api/compras/items/{idItem}", ctx -> {
      try {
        long userId = getUserId(ctx);
        long idItem = Long.parseLong(ctx.pathParam("idItem"));
        dao.removeItem(userId, idItem);
        ctx.status(204);
      } catch (Exception e) {
        ctx.status(400).json(Map.of("error", e.getMessage()));
      }
    });

    app.post("/api/compras/checkout", ctx -> {
      try {
        long userId = getUserId(ctx);
        PaymentReq req = ctx.bodyAsClass(PaymentReq.class);

        if (req == null || req.tarjeta == null || req.facturacion == null)
          throw new IllegalArgumentException("Datos de pago incompletos.");
        if (req.tarjeta.numero == null || req.tarjeta.numero.trim().length() < 12)
          throw new IllegalArgumentException("Número de tarjeta inválido.");
        if (req.tarjeta.cvv == null || req.tarjeta.cvv.trim().length() < 3)
          throw new IllegalArgumentException("CVV inválido.");

        CarritoResp resumen = dao.getCart(userId);
        if (resumen.items == null || resumen.items.isEmpty())
          throw new IllegalArgumentException("El carrito está vacío.");

        long idReserva = dao.checkout(userId);
        ctx.json(new CheckoutResp(idReserva));

        try { sendEmail(ctx, resumen, idReserva); } catch (Exception ignore) {}
      } catch (Exception e) {
        ctx.status(400).json(Map.of("error", e.getMessage()));
      }
    });

    app.get("/api/compras/reservas", ctx -> {
      try {
        long userId = getUserId(ctx);
        var list = dao.listReservasByUser(userId);
        ctx.json(list);
      } catch (Exception e) {
        ctx.status(400).json(Map.of("error", e.getMessage()));
      }
    });

    app.get("/api/compras/reservas/{id}", ctx -> {
      try {
        long userId = getUserId(ctx);
        long id = Long.parseLong(ctx.pathParam("id"));
        var det = dao.getReservaDetalle(userId, id);
        ctx.json(det);
      } catch (Exception e) {
        ctx.status(400).json(Map.of("error", e.getMessage()));
      }
    });
  }

  private void sendEmail(Context ctx, CarritoResp resumen, long idReserva) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> claims = ctx.attribute("claims");
      String to = claims != null ? String.valueOf(claims.getOrDefault("email", "")) : "";
      if (to == null || to.isBlank()) {
        to = ctx.header("X-User-Email"); 
        if (to == null || to.isBlank()) return;
      }

      StringBuilder html = new StringBuilder();
      html.append("<h2>Confirmación de reserva #").append(idReserva).append("</h2>");
      html.append("<p>Gracias por tu compra. Este es el detalle:</p>");
      html.append("<ul style='padding-left:16px'>");
      if (resumen.items != null) {
        for (var it : resumen.items) {
          String cod = String.valueOf(it.codigoVuelo);
          String clase = String.valueOf(it.clase);
          String salida = dt(String.valueOf(it.fechaSalida));
          String llegada = dt(String.valueOf(it.fechaLlegada));
          String paisO = getOpt(it, "paisOrigen");
          String paisD = getOpt(it, "paisDestino");
          String ciuO  = getOpt(it, "ciudadOrigen");
          String ciuD  = getOpt(it, "ciudadDestino");

          String origen = (ciuO.isBlank() && paisO.isBlank()) ? "" : (" — Origen: " + (ciuO.isBlank()?paisO:(ciuO+", "+paisO)));
          String destino= (ciuD.isBlank() && paisD.isBlank()) ? "" : (" — Destino: " + (ciuD.isBlank()?paisD:(ciuD+", "+paisD)));

          html.append("<li style='margin:6px 0'>")
              .append("<strong>").append(cod).append("</strong> (").append(clase).append(")")
              .append("<br/><small>Salida: ").append(salida)
              .append(" • Llegada: ").append(llegada).append("</small>")
              .append("<br/><small>").append(origen).append(destino).append("</small>")
              .append("<br/><strong>").append(money(it.subtotal)).append("</strong>")
              .append("</li>");
        }
      }
      html.append("</ul>");
      html.append("<p>Total: <strong>").append(money(resumen.total)).append("</strong></p>");
      html.append("<p>¡Buen viaje!<br/>Aerolíneas</p>");

      Class<?> mailer = Class.forName("com.aerolineas.util.Mailer");
      var m = mailer.getMethod("send", String.class, String.class, String.class);
      m.invoke(null, to, "Confirmación de reserva #" + idReserva, html.toString());
    } catch (ClassNotFoundException cnf) {
    } catch (Exception ex) {
    }
  }
}
