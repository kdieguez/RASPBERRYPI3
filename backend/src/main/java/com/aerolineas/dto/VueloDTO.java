package com.aerolineas.dto;

import java.time.LocalDateTime;
import java.util.List;

public class VueloDTO {

    public record ClaseConfig(int idClase, int cupoTotal, double precio) {}

    public record Create(
        String codigo,
        long idRuta,
        LocalDateTime fechaSalida,
        LocalDateTime fechaLlegada,
        List<ClaseConfig> clases,
        List<EscalaCreate> escalas
    ) {}

    public record Update(
        long idVuelo,
        LocalDateTime fechaSalida,
        LocalDateTime fechaLlegada,
        boolean activo
    ) {}

    // ⬇️ View con origen/destino
    public record View(
        long idVuelo,
        String codigo,
        long idRuta,
        String origen,          // NUEVO
        String destino,         // NUEVO
        LocalDateTime fechaSalida,
        LocalDateTime fechaLlegada,
        boolean activo,
        List<ClaseConfig> clases,
        List<EscalaView> escalas
    ) {
        // ⬇️ Constructor retro-compatible (firma vieja de 8 parámetros)
        public View(
            long idVuelo,
            String codigo,
            long idRuta,
            LocalDateTime fechaSalida,
            LocalDateTime fechaLlegada,
            boolean activo,
            List<ClaseConfig> clases,
            List<EscalaView> escalas
        ) {
            this(idVuelo, codigo, idRuta, null, null, fechaSalida, fechaLlegada, activo, clases, escalas);
        }
    }

    public static record EstadoUpdate(Integer idEstado) {}

    public record EscalaCreate(Long idCiudad, LocalDateTime llegada, LocalDateTime salida) {}

    public record EscalaView(Long idCiudad, String ciudad, String pais, LocalDateTime llegada, LocalDateTime salida) {}

    public record CreatePair(Create ida, Create regreso) {}

    public record CreatedPair(Long idIda, Long idRegreso) {}

    public record UpdateAdmin(
        String codigo,
        long idRuta,
        LocalDateTime fechaSalida,
        LocalDateTime fechaLlegada,
        Boolean activo,
        List<ClaseConfig> clases,
        List<EscalaCreate> escalas
    ) {}
}
