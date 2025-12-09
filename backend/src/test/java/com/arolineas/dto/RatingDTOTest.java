package com.arolineas.dto;

import com.aerolineas.dto.RatingDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RatingDTOTest {

  @Test
  void create_debeGuardarCamposCorrectamente() {
    RatingDTO.Create create = new RatingDTO.Create(5, "Excelente servicio");

    assertEquals(5, create.calificacion());
    assertEquals("Excelente servicio", create.comentario());
  }

  @Test
  void resumen_debeGuardarCamposCorrectamente_conMiRating() {
    RatingDTO.Resumen resumen = new RatingDTO.Resumen(4.3, 12, 5);

    assertEquals(4.3, resumen.promedio());
    assertEquals(12, resumen.total());
    assertEquals(5, resumen.miRating());
  }

  @Test
  void resumen_debePermitirMiRatingNull() {
    RatingDTO.Resumen resumen = new RatingDTO.Resumen(0.0, 0, null);

    assertEquals(0.0, resumen.promedio());
    assertEquals(0, resumen.total());
    assertNull(resumen.miRating());
  }
}
