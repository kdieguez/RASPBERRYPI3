package com.aerolineas.service;

import com.aerolineas.config.DB;
import com.aerolineas.dao.VueloDAO;
import com.aerolineas.dto.VueloDTO;
import com.aerolineas.util.Mailer;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class NotificacionesService {

  private final VueloDAO vueloDAO = new VueloDAO();
  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("d 'de' MMM yyyy, h:mm a", new Locale("es","MX"));

  public void notificarCambio(long idVuelo, String motivo) {
    try {
      VueloDTO.View v = vueloDAO.obtenerVuelo(idVuelo);
      if (v == null) return;

      String subject = "Actualización de tu vuelo " + safe(v.codigo());
      String html = htmlCambio(v, motivo);

      for (Destinatario d : listarDestinatarios(idVuelo)) {
        String personalizado = html.replace("{{NOMBRE}}", buildNombre(d.nombres, d.apellidos));
        Mailer.send(d.email, subject, personalizado);
      }
    } catch (Exception ignore) {}
  }

  public void notificarCancelacion(long idVuelo, String motivo) {
    try {
      VueloDTO.View v = vueloDAO.obtenerVuelo(idVuelo);
      if (v == null) return;

      String subject = "Cancelación de tu vuelo " + safe(v.codigo());
      String html = htmlCancelacion(v, motivo);

      for (Destinatario d : listarDestinatarios(idVuelo)) {
        String personalizado = html.replace("{{NOMBRE}}", buildNombre(d.nombres, d.apellidos));
        Mailer.send(d.email, subject, personalizado);
      }
    } catch (Exception ignore) { }
  }

  private List<Destinatario> listarDestinatarios(long idVuelo) throws SQLException {
    String reservaItemTable = DB.table("RESERVA_ITEM");
    String reservaTable = DB.table("RESERVA");
    String usuarioTable = DB.table("USUARIO");
    final String sql = "SELECT DISTINCT r.ID_RESERVA, u.ID_USUARIO, u.EMAIL, u.NOMBRES, u.APELLIDOS FROM " + reservaItemTable + " ri JOIN " + reservaTable + " r ON r.ID_RESERVA = ri.ID_RESERVA JOIN " + usuarioTable + " u ON u.ID_USUARIO = r.ID_USUARIO WHERE ri.ID_VUELO = ? AND r.ID_ESTADO IN (2,3) AND NVL(u.HABILITADO,1) = 1";

    List<Destinatario> list = new ArrayList<>();
    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setLong(1, idVuelo);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          list.add(new Destinatario(
              rs.getLong("ID_RESERVA"),
              rs.getLong("ID_USUARIO"),
              safe(rs.getString("EMAIL")),
              safe(rs.getString("NOMBRES")),
              safe(rs.getString("APELLIDOS"))
          ));
        }
      }
    }
    return list;
  }

  private String htmlCambio(VueloDTO.View v, String motivo) {
    String salida  = dt(v.fechaSalida());
    String llegada = dt(v.fechaLlegada());
    String origen  = ruta(v.origen(), v.origenPais());
    String destino = ruta(v.destino(), v.destinoPais());

    return new StringBuilder()
        .append("<h2>Hola {{NOMBRE}},</h2>")
        .append("<p>Queremos informarte que tu vuelo <strong>").append(safe(v.codigo())).append("</strong> ha sido <strong>actualizado</strong>.</p>")
        .append("<p><strong>Motivo:</strong> ").append(safe(motivo)).append("</p>")
        .append("<ul style='line-height:1.5'>")
        .append("<li><strong>Ruta:</strong> ").append(origen).append(" &rarr; ").append(destino).append("</li>")
        .append("<li><strong>Salida:</strong> ").append(salida).append("</li>")
        .append("<li><strong>Llegada:</strong> ").append(llegada).append("</li>")
        .append("</ul>")
        .append("<p>Si estos cambios no te funcionan, contáctanos para ayudarte con opciones.</p>")
        .append("<p>Gracias por volar con nosotros,<br/>Aerolíneas</p>")
        .toString();
  }

  private String htmlCancelacion(VueloDTO.View v, String motivo) {
    String salida  = dt(v.fechaSalida());
    String origen  = ruta(v.origen(), v.origenPais());
    String destino = ruta(v.destino(), v.destinoPais());

    return new StringBuilder()
        .append("<h2>Hola {{NOMBRE}},</h2>")
        .append("<p>Lamentamos informarte que tu vuelo <strong>").append(safe(v.codigo())).append("</strong> ha sido <strong>cancelado</strong>.</p>")
        .append("<p><strong>Motivo:</strong> ").append(safe(motivo)).append("</p>")
        .append("<ul style='line-height:1.5'>")
        .append("<li><strong>Ruta:</strong> ").append(origen).append(" &rarr; ").append(destino).append("</li>")
        .append("<li><strong>Salida prevista:</strong> ").append(salida).append("</li>")
        .append("</ul>")
        .append("<p>Nuestro equipo puede ayudarte a reprogramar o gestionar alternativas. Responde a este correo o visita tu historial para más opciones.</p>")
        .append("<p>Disculpa los inconvenientes,<br/>Aerolíneas</p>")
        .toString();
  }

  private String ruta(String ciudad, String pais) {
    ciudad = safe(ciudad);
    pais   = safe(pais);
    if (ciudad.isBlank() && pais.isBlank()) return "—";
    if (ciudad.isBlank()) return pais;
    if (pais.isBlank())   return ciudad;
    return ciudad + ", " + pais;
  }

  private String dt(LocalDateTime ldt) {
    if (ldt == null) return "—";
    return FMT.format(ldt);
  }

  private String buildNombre(String nombres, String apellidos) {
    nombres  = safe(nombres);
    apellidos = safe(apellidos);
    String full = (nombres + " " + apellidos).trim();
    return full.isBlank() ? "cliente" : full;
  }

  private String safe(String s) { return s == null ? "" : s; }

  private record Destinatario(long idReserva, long idUsuario, String email, String nombres, String apellidos) {}
}
