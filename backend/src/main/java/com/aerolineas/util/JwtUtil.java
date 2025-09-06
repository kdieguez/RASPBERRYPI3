package com.aerolineas.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

public class JwtUtil {
  private static byte[] key() {
    String secret = System.getenv().getOrDefault("JWT_SECRET", "dev-secret-change-me-please-1234567890");
    return secret.getBytes(StandardCharsets.UTF_8);
  }
  private static long expMinutes() {
    try { return Long.parseLong(System.getenv().getOrDefault("JWT_EXP_MIN","120")); }
    catch (Exception e) { return 120; }
  }

  public static String generate(Map<String, Object> claims) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(expMinutes() * 60);
    return Jwts.builder()
      .claims(claims)
      .issuedAt(Date.from(now))
      .expiration(Date.from(exp))
      .signWith(Keys.hmacShaKeyFor(key()))
      .compact();
  }

  public static io.jsonwebtoken.Claims parse(String token) {
    return Jwts.parser()
      .verifyWith(Keys.hmacShaKeyFor(key()))
      .build()
      .parseSignedClaims(token)
      .getPayload();
  }
}
