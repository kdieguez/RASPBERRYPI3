package com.aerolineas.dto;

import java.time.LocalDateTime;
import java.util.List;

public class VueloDTO {

    public record ClaseConfig(int idClase, int cupoTotal, double precio) {}

    public record Create(String codigo, long idRuta, LocalDateTime fechaSalida, LocalDateTime fechaLlegada, List<ClaseConfig> clases, List<EscalaCreate> escalas) {}

    public record Update(long idVuelo, LocalDateTime fechaSalida, LocalDateTime fechaLlegada, boolean activo) {}

    public record View(long idVuelo, String codigo, long idRuta, LocalDateTime fechaSalida, LocalDateTime fechaLlegada, boolean activo, List<ClaseConfig> clases, List<EscalaView> escalas) {}

    public static record EstadoUpdate( Integer idEstado) {}

    public record EscalaCreate(Long idCiudad, LocalDateTime llegada, LocalDateTime salida) {}
    
    public record EscalaView(Long idCiudad, String ciudad, String pais, LocalDateTime llegada, LocalDateTime salida) {}

    
}
