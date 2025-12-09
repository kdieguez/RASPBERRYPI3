package com.aerolineas.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VueloDTOTest {

  @Test
  void claseConfig_y_create_debenGuardarValores() {
    VueloDTO.ClaseConfig clase = new VueloDTO.ClaseConfig(1, 100, 599.99);
    LocalDateTime salida = LocalDateTime.of(2025, 1, 10, 8, 30);
    LocalDateTime llegada = LocalDateTime.of(2025, 1, 10, 12, 15);
    VueloDTO.EscalaCreate escala = new VueloDTO.EscalaCreate(10L, salida.plusHours(1), salida.plusHours(2));

    VueloDTO.Create create = new VueloDTO.Create(
        "AV123",
        50L,
        salida,
        llegada,
        List.of(clase),
        List.of(escala),
        true
    );

    assertEquals("AV123", create.codigo());
    assertEquals(50L, create.idRuta());
    assertEquals(salida, create.fechaSalida());
    assertEquals(llegada, create.fechaLlegada());
    assertTrue(create.activo());
    assertEquals(1, create.clases().size());
    assertEquals(1, create.escalas().size());
    assertEquals(1, create.clases().get(0).idClase());
    assertEquals(100, create.clases().get(0).cupoTotal());
    assertEquals(10L, create.escalas().get(0).idCiudad());
  }

  @Test
  void view_constructorCanonico_debeGuardarTodosLosCampos() {
    LocalDateTime salida = LocalDateTime.of(2025, 2, 1, 9, 0);
    LocalDateTime llegada = LocalDateTime.of(2025, 2, 1, 11, 30);
    VueloDTO.ClaseConfig clase = new VueloDTO.ClaseConfig(2, 50, 799.0);
    VueloDTO.EscalaView escala = new VueloDTO.EscalaView(
        20L, "Ciudad Escala", "País Escala", salida.plusHours(1), salida.plusHours(2)
    );

    VueloDTO.View view = new VueloDTO.View(
        99L,
        "AV999",
        77L,
        "Guatemala",
        "El Salvador",
        salida,
        llegada,
        true,
        3,
        "Programado",
        List.of(clase),
        List.of(escala),
        88L,
        "Guatemala",
        "El Salvador"
    );

    assertEquals(99L, view.idVuelo());
    assertEquals("AV999", view.codigo());
    assertEquals(77L, view.idRuta());
    assertEquals("Guatemala", view.origen());
    assertEquals("El Salvador", view.destino());
    assertEquals(salida, view.fechaSalida());
    assertEquals(llegada, view.fechaLlegada());
    assertTrue(view.activo());
    assertEquals(3, view.idEstado());
    assertEquals("Programado", view.estado());
    assertEquals(1, view.clases().size());
    assertEquals(1, view.escalas().size());
    assertEquals(88L, view.idVueloPareja());
    assertEquals("Guatemala", view.origenPais());
    assertEquals("El Salvador", view.destinoPais());
  }

  @Test
  void view_constructorSinOrigenDestino_debeDejarlosNull() {
    LocalDateTime salida = LocalDateTime.of(2025, 3, 5, 7, 0);
    LocalDateTime llegada = LocalDateTime.of(2025, 3, 5, 10, 0);
    VueloDTO.ClaseConfig clase = new VueloDTO.ClaseConfig(3, 120, 450.0);
    VueloDTO.EscalaView escala = new VueloDTO.EscalaView(
        30L, "Escala City", "Escala País", salida.plusHours(1), salida.plusHours(2)
    );

    VueloDTO.View view = new VueloDTO.View(
        10L,
        "AV010",
        5L,
        salida,
        llegada,
        true,
        List.of(clase),
        List.of(escala)
    );

    assertEquals(10L, view.idVuelo());
    assertEquals("AV010", view.codigo());
    assertEquals(5L, view.idRuta());
    assertNull(view.origen());
    assertNull(view.destino());
    assertTrue(view.activo());
    assertNull(view.idEstado());
    assertNull(view.estado());
    assertNull(view.idVueloPareja());
    assertNull(view.origenPais());
    assertNull(view.destinoPais());
  }

  @Test
  void view_constructorConOrigenDestino_peroSinEstado_niPareja() {
    LocalDateTime salida = LocalDateTime.of(2025, 4, 1, 6, 0);
    LocalDateTime llegada = LocalDateTime.of(2025, 4, 1, 9, 0);
    VueloDTO.ClaseConfig clase = new VueloDTO.ClaseConfig(4, 80, 650.0);
    VueloDTO.EscalaView escala = new VueloDTO.EscalaView(
        40L, "Ciudad X", "País X", salida.plusHours(1), salida.plusHours(2)
    );

    VueloDTO.View view = new VueloDTO.View(
        20L,
        "AV020",
        15L,
        "Ciudad Origen",
        "Ciudad Destino",
        salida,
        llegada,
        true,
        List.of(clase),
        List.of(escala)
    );

    assertEquals(20L, view.idVuelo());
    assertEquals("Ciudad Origen", view.origen());
    assertEquals("Ciudad Destino", view.destino());
    assertTrue(view.activo());
    assertNull(view.idEstado());
    assertNull(view.estado());
    assertNull(view.idVueloPareja());
    assertNull(view.origenPais());
    assertNull(view.destinoPais());
  }

  @Test
  void viewAdmin_constructor_debeGuardarCampos() {
    LocalDateTime salida = LocalDateTime.now();
    LocalDateTime llegada = salida.plusHours(2);
    VueloDTO.ClaseConfig clase = new VueloDTO.ClaseConfig(1, 100, 500.0);
    VueloDTO.EscalaView escala = new VueloDTO.EscalaView(
        1L, "Escala Admin", "País Admin", salida.plusHours(1), salida.plusHours(1).plusMinutes(30)
    );

    VueloDTO.ViewAdmin va = new VueloDTO.ViewAdmin(
        1L,
        "CODE1",
        2L,
        "OrigenA",
        "DestinoB",
        salida,
        llegada,
        true,
        1,
        "Programado",
        List.of(clase),
        List.of(escala),
        9L,
        "CODE-PAIR",
        "GT",
        "SV"
    );

    assertEquals(1L, va.idVuelo());
    assertEquals("CODE1", va.codigo());
    assertEquals("OrigenA", va.origen());
    assertEquals("DestinoB", va.destino());
    assertTrue(va.activo());
    assertEquals(1, va.idEstado());
    assertEquals("Programado", va.estado());
    assertEquals(9L, va.idVueloPareja());
    assertEquals("CODE-PAIR", va.codigoPareja());
    assertEquals("GT", va.origenPais());
    assertEquals("SV", va.destinoPais());
    assertEquals(1, va.clases().size());
    assertEquals(1, va.escalas().size());
  }

  @Test
  void estadoUpdate_createPair_y_createdPair_debenFuncionar() {
    VueloDTO.EstadoUpdate eu = new VueloDTO.EstadoUpdate(2, "Motivo X");
    assertEquals(2, eu.idEstado());
    assertEquals("Motivo X", eu.motivo());

    VueloDTO.Create c1 = new VueloDTO.Create(
        "IDA", 1L, LocalDateTime.now(), LocalDateTime.now().plusHours(1),
        List.of(), List.of(), true
    );
    VueloDTO.Create c2 = new VueloDTO.Create(
        "REG", 2L, LocalDateTime.now(), LocalDateTime.now().plusHours(2),
        List.of(), List.of(), true
    );

    VueloDTO.CreatePair cp = new VueloDTO.CreatePair(c1, c2);
    assertSame(c1, cp.ida());
    assertSame(c2, cp.regreso());

    VueloDTO.CreatedPair crp = new VueloDTO.CreatedPair(100L, 200L);
    assertEquals(100L, crp.idIda());
    assertEquals(200L, crp.idRegreso());
  }

  @Test
  void updateAdmin_y_vueloConEscalaCreate_yView_debenGuardarValores() {
    LocalDateTime salida = LocalDateTime.of(2025, 5, 1, 10, 0);
    LocalDateTime llegada = salida.plusHours(3);

    VueloDTO.ClaseConfig clase = new VueloDTO.ClaseConfig(5, 60, 700.0);
    VueloDTO.EscalaCreate escalaCreate = new VueloDTO.EscalaCreate(
        1L, salida.plusHours(1), salida.plusHours(2)
    );

    VueloDTO.UpdateAdmin ua = new VueloDTO.UpdateAdmin(
        "NEWCODE",
        77L,
        salida,
        llegada,
        true,
        List.of(clase),
        List.of(escalaCreate),
        "Cambio de horario"
    );

    assertEquals("NEWCODE", ua.codigo());
    assertEquals(77L, ua.idRuta());
    assertEquals(salida, ua.fechaSalida());
    assertEquals(llegada, ua.fechaLlegada());
    assertTrue(ua.activo());
    assertEquals(1, ua.clases().size());
    assertEquals(1, ua.escalas().size());
    assertEquals("Cambio de horario", ua.motivoCambio());

    VueloDTO.VueloConEscalaCreate vceCreate = new VueloDTO.VueloConEscalaCreate(
        "ESCALA123",
        10L,
        20L,
        List.of(clase),
        true
    );

    assertEquals("ESCALA123", vceCreate.codigo());
    assertEquals(10L, vceCreate.idVueloPrimerTramo());
    assertEquals(20L, vceCreate.idVueloSegundoTramo());
    assertTrue(vceCreate.activo());
    assertEquals(1, vceCreate.clases().size());

    VueloDTO.View tramo1 = new VueloDTO.View(
        1L,
        "T1",
        1L,
        "Ciudad T1",
        "Ciudad Escala",
        salida,
        salida.plusHours(1),
        true,
        List.of(clase),
        List.of()
    );

    VueloDTO.View tramo2 = new VueloDTO.View(
        2L,
        "T2",
        2L,
        "Ciudad Escala",
        "Ciudad Final",
        salida.plusHours(1),
        llegada,
        true,
        List.of(clase),
        List.of()
    );

    VueloDTO.VueloConEscalaView vceView = new VueloDTO.VueloConEscalaView(
        999L,
        "ESCALA123",
        tramo1,
        tramo2,
        "Ciudad T1",
        "Ciudad Final",
        "GT",
        "SV",
        salida,
        llegada,
        true,
        1,
        "Programado",
        List.of(clase),
        List.of()
    );

    assertEquals(999L, vceView.idVueloConEscala());
    assertEquals("ESCALA123", vceView.codigo());
    assertSame(tramo1, vceView.primerTramo());
    assertSame(tramo2, vceView.segundoTramo());
    assertEquals("Ciudad T1", vceView.origen());
    assertEquals("Ciudad Final", vceView.destino());
    assertEquals("GT", vceView.origenPais());
    assertEquals("SV", vceView.destinoPais());
    assertEquals(salida, vceView.fechaSalida());
    assertEquals(llegada, vceView.fechaLlegada());
    assertTrue(vceView.activo());
    assertEquals(1, vceView.idEstado());
    assertEquals("Programado", vceView.estado());
    assertEquals(1, vceView.clases().size());
    assertTrue(vceView.escalas().isEmpty());
  }
}
