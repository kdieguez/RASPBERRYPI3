package com.aerolineas.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

  @Test
  void generate_y_parse_debenMantenerLosClaimsBasicos() {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "user@example.com");
    claims.put("role", "ADMIN");

    String token = JwtUtil.generate(claims);

    assertNotNull(token);
    assertFalse(token.isBlank());

    Claims parsed = JwtUtil.parse(token);

    assertEquals("user@example.com", parsed.get("sub", String.class));
    assertEquals("ADMIN", parsed.get("role", String.class));

    Date iat = parsed.getIssuedAt();
    Date exp = parsed.getExpiration();
    assertNotNull(iat);
    assertNotNull(exp);
    assertTrue(exp.after(iat), "La expiración debe ser posterior al issuedAt");
  }

  @Test
  void expMinutes_debeSerMayorQueCero() throws Exception {
    Method m = JwtUtil.class.getDeclaredMethod("expMinutes");
    m.setAccessible(true);
    long minutes = (long) m.invoke(null);

    assertTrue(minutes > 0, "expMinutes debe ser mayor que cero");
  }

  @Test
  void key_debeRetornarArregloNoVacio() throws Exception {
    Method m = JwtUtil.class.getDeclaredMethod("key");
    m.setAccessible(true);
    byte[] key = (byte[]) m.invoke(null);

    assertNotNull(key);
    assertTrue(key.length > 0, "La llave no debe estar vacía");
  }
}
