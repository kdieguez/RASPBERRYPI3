package com.aerolineas.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PasajeroTest {

  @Test
  void constructorVacio_y_setters_debenAsignarValores() {
    Pasajero p = new Pasajero();

    LocalDate fecha = LocalDate.of(1995, 5, 10);

    p.setIdPasajero(1L);
    p.setFechaNacimiento(fecha);
    p.setIdPaisDocumento(502L);
    p.setPasaporte("P1234567");
    p.setIdUsuario(99L);

    assertEquals(1L, p.getIdPasajero());
    assertEquals(fecha, p.getFechaNacimiento());
    assertEquals(502L, p.getIdPaisDocumento());
    assertEquals("P1234567", p.getPasaporte());
    assertEquals(99L, p.getIdUsuario());
  }

  @Test
  void constructorCompleto_debeInicializarTodosLosCampos() {
    LocalDate fecha = LocalDate.of(2000, 1, 1);

    Pasajero p = new Pasajero(
        10L,
        fecha,
        1L,
        "X890123",
        50L
    );

    assertEquals(10L, p.getIdPasajero());
    assertEquals(fecha, p.getFechaNacimiento());
    assertEquals(1L, p.getIdPaisDocumento());
    assertEquals("X890123", p.getPasaporte());
    assertEquals(50L, p.getIdUsuario());
  }
}
