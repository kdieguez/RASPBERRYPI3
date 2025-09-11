package com.aerolineas.middleware;

import com.aerolineas.util.JwtUtil;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.UnauthorizedResponse;

import java.util.Map;

public final class Auth {
  private Auth() {}

  public static Handler jwt() {
    return ctx -> parseAndAttach(ctx); 
  }

  public static Handler adminOrEmpleado() {
    return ctx -> {
      Map<String,Object> cl = parseAndAttach(ctx);
      int rol = toInt(cl.get("rol"));
      if (!(rol == 1)) {
        throw new UnauthorizedResponse("requiere rol administrador/empleado");
      }
    };
  }

  @SuppressWarnings("unchecked")
  public static Map<String,Object> claims(Context ctx) {
    Object raw = ctx.attribute("claims");
    if (raw instanceof Map) return (Map<String,Object>) raw;
    return null;
  }

  private static Map<String,Object> parseAndAttach(Context ctx) {
    String auth = ctx.header("Authorization");
    if (auth == null || !auth.startsWith("Bearer ")) {
      throw new UnauthorizedResponse("token requerido");
    }
    var token = auth.substring(7);
    var c = JwtUtil.parse(token);
    Map<String,Object> claims = Map.of(
        "sub",   c.get("sub"),
        "email", c.get("email"),
        "rol",   c.get("rol"),
        "name",  c.get("name")
    );
    ctx.attribute("claims", claims);
    return claims;
  }

  private static int toInt(Object v) {
    if (v instanceof Number n) return n.intValue();
    try { return Integer.parseInt(String.valueOf(v)); } catch (Exception e) { return -1; }
  }
}
