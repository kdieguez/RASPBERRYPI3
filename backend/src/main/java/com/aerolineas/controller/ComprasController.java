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

import com.aerolineas.util.Mailer;
import com.aerolineas.util.PdfBoleto;

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

    app.get("/api/dev/test-mail", ctx -> {
      String to = ctx.queryParam("to");
      if (to == null || to.isBlank()) to = ctx.header("X-User-Email");
      if (to == null || to.isBlank()) {
        ctx.status(400).result("Falta ?to=destinatario o header X-User-Email");
        return;
      }
      Mailer.send(to, "Prueba SMTP", "<p>Esto es una prueba desde el backend.</p>");
      ctx.result("OK enviado a " + to);
    });

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

    app.get("/api/compras/reservas/{id}/boleto.pdf", ctx -> {
      try {
        long userId = getUserId(ctx);
        long id = Long.parseLong(ctx.pathParam("id"));

        var det = dao.getReservaDetalle(userId, id);
        if (det == null) { ctx.status(404).json(Map.of("error","Reserva no encontrada")); return; }

        String codigo = null;
        try {
          var f = det.getClass().getDeclaredField("codigo");
          f.setAccessible(true);
          Object v = f.get(det);
          codigo = (v == null) ? null : String.valueOf(v);
        } catch (NoSuchFieldException ignore) {}
        if (codigo == null || codigo.isBlank()) {
          try (var cn = com.aerolineas.config.DB.getConnection();
               var ps = cn.prepareStatement(
                   "SELECT CODIGO FROM AEROLINEA.RESERVA WHERE ID_RESERVA = ? AND ID_USUARIO = ?")) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            try (var rs = ps.executeQuery()) {
              if (rs.next()) codigo = rs.getString(1);
            }
          }
        }
        if (codigo == null || codigo.isBlank()) codigo = String.valueOf(id);

        @SuppressWarnings("unchecked")
        Map<String, Object> claims = ctx.attribute("claims");

        String email = null;
        String nombre = null;

        if (claims != null) {
          Object em = claims.get("email");
          if (em != null) email = String.valueOf(em);

          Object nm = claims.get("nombre");
          if (nm == null) nm = claims.get("name");
          if (nm == null) nm = claims.get("fullName");
          if (nm == null) nm = claims.get("usuario");
          if (nm == null) nm = claims.get("username");
          if (nm != null) nombre = String.valueOf(nm);
        }

        if (email == null || email.isBlank()) email  = ctx.header("X-User-Email");
        if (nombre == null || nombre.isBlank()) nombre = ctx.header("X-User-Name");

        if ((nombre == null || nombre.isBlank()) && email != null && email.contains("@")) {
          nombre = email.substring(0, email.indexOf('@'));
        }
        if (nombre == null || nombre.isBlank()) nombre = "Cliente";

        byte[] pdf = PdfBoleto.build(det, codigo, nombre, email);

        String safe = codigo.replaceAll("[^A-Za-z0-9._-]", "_");
        ctx.header("Content-Type", "application/pdf");
        ctx.header("Content-Disposition", "attachment; filename=\"boleto-" + safe + ".pdf\"");
        ctx.header("Cache-Control", "no-store");
        ctx.result(pdf);
      } catch (Exception e) {
    e.printStackTrace();
    ctx.status(400).json(Map.of("error", e.getClass().getSimpleName() + ": " + (e.getMessage()==null? "Error generando PDF" : e.getMessage())));
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
          String salida = dt(String.valueOf(it.fechaSalida));
          String llegada = dt(String.valueOf(it.fechaLlegada));
          String paisO = getOpt(it, "paisOrigen");
          String paisD = getOpt(it, "paisDestino");
          String ciuO  = getOpt(it, "ciudadOrigen");
          String ciuD  = getOpt(it, "ciudadDestino");

          StringBuilder extra = new StringBuilder();
          if (!(ciuO.isBlank() && paisO.isBlank()) || !(ciuD.isBlank() && paisD.isBlank())) {
            extra.append("<br/><small>");
            if (!(ciuO.isBlank() && paisO.isBlank())) {
              extra.append("Origen: ").append(ciuO.isBlank() ? paisO : (ciuO + ", " + paisO));
            }
            if (!(ciuD.isBlank() && paisD.isBlank())) {
              if (extra.length() > 13) extra.append(" • ");
              extra.append("Destino: ").append(ciuD.isBlank() ? paisD : (ciuD + ", " + paisD));
            }
            extra.append("</small>");
          }

          html.append("<li style='margin:6px 0'>")
              .append("<strong>").append(String.valueOf(it.codigoVuelo)).append("</strong>")
              .append(" (").append(String.valueOf(it.clase)).append(")")
              .append("<br/><small>Salida: ").append(salida).append(" • Llegada: ").append(llegada).append("</small>")
              .append(extra)
              .append("<br/><strong>").append(money(it.subtotal)).append("</strong>")
              .append("</li>");
        }
      }
      html.append("</ul>");
      html.append("<p>Total: <strong>").append(money(resumen.total)).append("</strong></p>");
      html.append("<p>¡Buen viaje!<br/>Aerolíneas</p>");

      Mailer.send(to, "Confirmación de reserva #" + idReserva, html.toString());
    } catch (Exception ex) {
      if (Boolean.parseBoolean(System.getenv("MAIL_DEBUG"))) {
        System.out.println("[MAIL] Error enviando correo: " + ex.getMessage());
      }
    }
  }
}
