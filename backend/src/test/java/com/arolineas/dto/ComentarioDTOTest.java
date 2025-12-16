package com.arolineas.dto;

import com.aerolineas.dto.ComentarioDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ComentarioDTOTest {

  @Test
  void create_debeGuardarCamposCorrectamente() {
    ComentarioDTO.Create create = new ComentarioDTO.Create("Un comentario", 10L);

    assertEquals("Un comentario", create.comentario());
    assertEquals(17L, create.idPadre());
  }

  @Test
  void view_debeGuardarCamposBasicos() {
    LocalDateTime ahora = LocalDateTime.now();

    ComentarioDTO.View view = new ComentarioDTO.View(
        1L,
        100L,
        200L,
        "Katherine",
        "Me encantó el vuelo",
        ahora,
        null,
        5
    );

    assertEquals(1L, view.getIdComentario());
    assertEquals(100L, view.getIdVuelo());
    assertEquals(200L, view.getIdUsuario());
    assertEquals("Katherine", view.getAutor());
    assertEquals("Me encantó el vuelo", view.getComentario());
    assertEquals(ahora, view.getCreadaEn());
    assertNull(view.getIdPadre());
    assertEquals(5, view.getRatingAutor());
    assertNotNull(view.getRespuestas());
    assertTrue(view.getRespuestas().isEmpty());
  }

  @Test
  void view_permiteAgregarRespuestasHijas() {
    LocalDateTime ahora = LocalDateTime.now();

    ComentarioDTO.View padre = new ComentarioDTO.View(
        1L,
        100L,
        200L,
        "Autora Principal",
        "Comentario principal",
        ahora,
        null,
        4
    );

    ComentarioDTO.View respuesta1 = new ComentarioDTO.View(
        2L,
        100L,
        201L,
        "Otra Persona",
        "Respuesta 1",
        ahora.plusMinutes(5),
        1L,
        3
    );

    ComentarioDTO.View respuesta2 = new ComentarioDTO.View(
        3L,
        100L,
        202L,
        "Tercera Persona",
        "Respuesta 2",
        ahora.plusMinutes(10),
        1L,
        null
    );

    padre.getRespuestas().add(respuesta1);
    padre.getRespuestas().add(respuesta2);

    assertEquals(2, padre.getRespuestas().size());
    assertSame(respuesta1, padre.getRespuestas().get(0));
    assertSame(respuesta2, padre.getRespuestas().get(1));

    assertEquals(2L, padre.getRespuestas().get(0).getIdComentario());
    assertEquals(1L, padre.getRespuestas().get(0).getIdPadre());
    assertEquals("Respuesta 1", padre.getRespuestas().get(0).getComentario());

    assertEquals(3L, padre.getRespuestas().get(1).getIdComentario());
    assertEquals("Respuesta 2", padre.getRespuestas().get(1).getComentario());
  }
}
