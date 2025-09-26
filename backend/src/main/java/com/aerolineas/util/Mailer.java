package com.aerolineas.util;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public final class Mailer {

  private static String env(String k, String def) {
    String v = System.getenv(k);
    return (v == null || v.isBlank()) ? def : v.trim();
  }

  private static final String HOST = env("MAIL_HOST", "smtp.gmail.com");
  private static final int    PORT = Integer.parseInt(env("MAIL_PORT", "587"));
  private static final String USER = env("MAIL_USER", "");
  private static final String PASS = env("MAIL_PASS", "");
  private static final String FROM = env("MAIL_FROM", USER);
  private static final boolean AUTH = Boolean.parseBoolean(env("MAIL_AUTH", "true"));
  private static final boolean TLS  = Boolean.parseBoolean(env("MAIL_TLS", "true"));
  private static final boolean SSL  = Boolean.parseBoolean(env("MAIL_SSL", "false"));
  private static final boolean DEBUG= Boolean.parseBoolean(env("MAIL_DEBUG", "false"));

  private static Session session() {
    Properties p = new Properties();
    p.put("mail.smtp.host", HOST);
    p.put("mail.smtp.port", String.valueOf(PORT));
    p.put("mail.smtp.auth", String.valueOf(AUTH));
    if (TLS) p.put("mail.smtp.starttls.enable", "true");
    if (SSL) p.put("mail.smtp.ssl.enable", "true");
    p.put("mail.smtp.ssl.protocols", "TLSv1.2");
    p.put("mail.smtp.connectiontimeout", "10000");
    p.put("mail.smtp.timeout", "10000");

    Authenticator a = AUTH
        ? new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
              return new PasswordAuthentication(USER, PASS);
            }
          }
        : null;

    Session s = Session.getInstance(p, a);
    s.setDebug(DEBUG);
    return s;
  }

  public static void send(String to, String subject, String html) throws MessagingException {
    if (to == null || to.isBlank())
      throw new MessagingException("Destinatario vacío (to).");

    Session s = session();
    MimeMessage m = new MimeMessage(s);

    m.setFrom(new InternetAddress(FROM));
    m.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
    m.setSubject(subject, StandardCharsets.UTF_8.name());
    m.setContent(html, "text/html; charset=UTF-8");

    Transport.send(m);
  }

  public static void main(String[] args) throws Exception {
    String to = env("TEST_TO", "");
    if (to.isBlank()) {
      System.err.println("TEST_TO no definido, saliendo.");
      return;
    }
    send(to, "Prueba Aerolíneas", "<h3>Hola 👋</h3><p>Este es un correo de prueba.</p>");
    System.out.println("Correo de prueba enviado a " + to);
  }

  private Mailer() {}
}
