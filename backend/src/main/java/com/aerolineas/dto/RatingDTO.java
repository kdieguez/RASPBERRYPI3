package com.aerolineas.dto;

public final class RatingDTO {

  public static record Create(Integer calificacion, String comentario) {}

  public static record Resumen(double promedio, int total, Integer miRating) {}
}
