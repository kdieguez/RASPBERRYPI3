package com.aerolineas.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {
  private static int cost() {
    try { return Integer.parseInt(System.getenv().getOrDefault("BCRYPT_COST", "10")); }
    catch (Exception e) { return 10; }
  }

  public static String hash(String plain) {
    return BCrypt.hashpw(plain, BCrypt.gensalt(cost()));
  }

  public static boolean verify(String plain, String hash) {
    if (hash == null || hash.isBlank()) return false;
    return BCrypt.checkpw(plain, hash);
  }
}
