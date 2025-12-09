package com.aerolineas.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class EstadosVueloTest {

  @Test
  void constantes_tienenLosValoresEsperados_ySetValidLosContiene() {
    assertEquals(1, EstadosVuelo.PROGRAMADO);
    assertEquals(2, EstadosVuelo.CANCELADO);

    assertTrue(EstadosVuelo.VALID.contains(EstadosVuelo.PROGRAMADO));
    assertTrue(EstadosVuelo.VALID.contains(EstadosVuelo.CANCELADO));
    assertEquals(2, EstadosVuelo.VALID.size());
  }

  @Test
  void esFinal_soloEsTrueParaCancelado() {
    assertTrue(EstadosVuelo.esFinal(EstadosVuelo.CANCELADO));
    assertFalse(EstadosVuelo.esFinal(EstadosVuelo.PROGRAMADO));
    assertFalse(EstadosVuelo.esFinal(999));
  }

  @Test
  void constructorEsPrivado_peroSePuedeInvocarPorReflexion_paraCobertura() throws Exception {
    Constructor<EstadosVuelo> ctor = EstadosVuelo.class.getDeclaredConstructor();
    assertTrue(Modifier.isPrivate(ctor.getModifiers()));

    ctor.setAccessible(true);
    EstadosVuelo instancia = ctor.newInstance();
    assertNotNull(instancia);
  }
}
