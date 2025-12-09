package com.aerolineas.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class CaptchaUtil {
  private static HttpClient client = HttpClient.newHttpClient();
  private static final ObjectMapper mapper = new ObjectMapper();
  public static boolean verify(String responseToken) {
    try {
      String secret = getenv("RECAPTCHA_SECRET");
      if (secret == null || secret.isBlank()) {
        System.out.println("[CAPTCHA] RECAPTCHA_SECRET no configurado -> modo dev (allow)");
        return true;
      }
      if (responseToken == null || responseToken.isBlank()) return false;

      String form = "secret=" + URLEncoder.encode(secret, StandardCharsets.UTF_8) +
                    "&response=" + URLEncoder.encode(responseToken, StandardCharsets.UTF_8);

      HttpRequest req = HttpRequest.newBuilder(URI.create("https://www.google.com/recaptcha/api/siteverify"))
          .timeout(Duration.ofSeconds(6))
          .header("Content-Type", "application/x-www-form-urlencoded")
          .POST(HttpRequest.BodyPublishers.ofString(form))
          .build();

      HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
      if (res.statusCode() != 200) {
        System.out.println("[CAPTCHA] HTTP " + res.statusCode() + " -> fail");
        return false;
      }
      JsonNode json = mapper.readTree(res.body());
      boolean ok = json.path("success").asBoolean(false);
      if (!ok) System.out.println("[CAPTCHA] respuesta no exitosa: " + res.body());
      return ok;
    } catch (Exception e) {
      System.out.println("[CAPTCHA] error: " + e.getMessage());
      return false;
    }
  }

  private static String getenv(String k) {
    String v = System.getenv(k);
    if (v == null || v.isBlank()) {
      try { v = io.github.cdimascio.dotenv.Dotenv.configure().ignoreIfMissing().load().get(k); }
      catch (Exception ignored) {}
    }
    return v;
  }
}
