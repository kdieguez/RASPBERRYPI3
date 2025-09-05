package com.aerolineas.util;

import java.util.Set;

public final class EstadosVuelo {
  private EstadosVuelo() {}

  public static final int PROGRAMADO = 1;
  public static final int CANCELADO  = 2;

  public static final Set<Integer> VALID = Set.of(PROGRAMADO, CANCELADO);

  public static boolean esFinal(int estado) {
    return estado == CANCELADO;
  }
}
