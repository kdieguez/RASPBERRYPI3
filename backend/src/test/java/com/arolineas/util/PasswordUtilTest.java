package com.aerolineas.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

  private static int callCost() throws Exception {
    Method m = PasswordUtil.class.getDeclaredMethod("cost");
    m.setAccessible(true);
    return (int) m.invoke(null);
  }

  @AfterEach
  void clearProps() {
    System.clearProperty("BCRYPT_COST");
  }

  @Test
  void cost_sinEnvNiProperty_devuelve10PorDefecto() throws Exception {
    System.clearProperty("BCRYPT_COST");
    int c = callCost();
    assertEquals(10, c);
  }

  @Test
  void cost_conPropertyValida_devuelveEseValor() throws Exception {
    System.setProperty("BCRYPT_COST", "12");
    int c = callCost();
    assertEquals(12, c);
  }

  @Test
  void cost_conPropertyInvalida_devuelve10() throws Exception {
    System.setProperty("BCRYPT_COST", "no-num");
    int c = callCost();
    assertEquals(10, c);
  }

  @Test
  void hash_y_verify_contraseñaCorrectaDevuelveTrue() {
    String plain = "MiSuperClave123!";
    String hash = PasswordUtil.hash(plain);

    assertNotNull(hash);
    assertNotEquals(plain, hash);          
    assertTrue(PasswordUtil.verify(plain, hash));
  }

  @Test
  void verify_contraseñaIncorrectaDevuelveFalse() {
    String plain = "correcta";
    String hash = PasswordUtil.hash(plain);

    assertFalse(PasswordUtil.verify("incorrecta", hash));
  }

  @Test
  void verify_conHashNullOBlancoDevuelveFalse() {
    assertFalse(PasswordUtil.verify("algo", null));
    assertFalse(PasswordUtil.verify("algo", ""));
    assertFalse(PasswordUtil.verify("algo", "   "));
  }
}
