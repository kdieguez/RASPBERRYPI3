package com.aerolineas.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

  private static int cost() {
    String raw = System.getenv("BCRYPT_COST");
    if (raw == null || raw.isBlank()) {
      raw = System.getProperty("BCRYPT_COST", "10");
    }
    try {
      return Integer.parseInt(raw);
    } catch (Exception e) {
      return 10;
    }
  }

  public static String hash(String plain) {
    return BCrypt.hashpw(plain, BCrypt.gensalt(cost()));
  }

  public static boolean verify(String plain, String hash) {
    if (hash == null || hash.isBlank()) return false;
    return BCrypt.checkpw(plain, hash);
  }
}
