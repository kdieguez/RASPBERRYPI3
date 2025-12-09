package com.aerolineas.util;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class Mailer {
  private Mailer() {}

  private static String env(String k, String def) {
    String v = System.getenv(k);
    if (v == null || v.isBlank()) {
      v = System.getProperty(k);
    }
    return (v == null || v.isBlank()) ? def : v.trim();
  }

  private static boolean bool(String k, boolean def) {
    String v = System.getenv(k);
    if (v == null || v.isBlank()) {
      v = System.getProperty(k);
    }
    if (v == null) return def;
    v = v.trim().toLowerCase();
    return v.equals("1") || v.equals("true") || v.equals("yes") || v.equals("y");
  }

  private static Session buildSession() {
    String host = env("MAIL_HOST", "smtp.gmail.com");
    String port = env("MAIL_PORT", "587");
    boolean auth = bool("MAIL_AUTH", true);
    boolean tls  = bool("MAIL_TLS",  true);
    boolean debug = bool("MAIL_DEBUG", false);

    final String user = env("MAIL_USER", "");
    final String pass = env("MAIL_PASS", "");

    if (user.isBlank() || pass.isBlank()) {
      throw new IllegalStateException("MAIL_USER/MAIL_PASS no configurados");
    }

    Properties p = new Properties();
    p.put("mail.smtp.host", host);
    p.put("mail.smtp.port", port);
    p.put("mail.smtp.auth", String.valueOf(auth));
    p.put("mail.smtp.connectiontimeout", "15000");
    p.put("mail.smtp.timeout", "30000");
    p.put("mail.smtp.writetimeout", "30000");

    if (tls) {
      p.put("mail.smtp.starttls.enable", "true");
      p.put("mail.smtp.starttls.required", "true");
    }

    if ("465".equals(port)) {
      p.put("mail.smtp.ssl.enable", "true");
    }

    Authenticator a = new Authenticator() {
      @Override protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(user, pass);
      }
    };

    Session s = Session.getInstance(p, a);
    s.setDebug(debug);
    return s;
  }

  public static void send(String to, String subject, String html) throws Exception {
    if (to == null || to.isBlank()) throw new IllegalArgumentException("Destinatario vacío");

    Session session = buildSession();
    String from = env("MAIL_FROM", env("MAIL_USER", ""));
    if (from.isBlank()) from = env("MAIL_USER", "");

    MimeMessage msg = new MimeMessage(session);
    msg.setFrom(new InternetAddress(from));
    msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
    msg.setSubject(subject, StandardCharsets.UTF_8.name());

    String text = html.replaceAll("<[^>]+>", " ")
                      .replace("&nbsp;", " ")
                      .replaceAll("\\s+", " ")
                      .trim();

    MimeBodyPart textPart = new MimeBodyPart();
    textPart.setText(text, StandardCharsets.UTF_8.name());

    MimeBodyPart htmlPart = new MimeBodyPart();
    htmlPart.setContent(html, "text/html; charset=UTF-8");

    MimeMultipart mp = new MimeMultipart("alternative");
    mp.addBodyPart(textPart);
    mp.addBodyPart(htmlPart);

    msg.setContent(mp);
    Transport.send(msg);
  }
}
