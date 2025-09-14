package com.aerolineas.dto;

import java.time.LocalDateTime;
import java.util.List;

public class VueloDTO {

    public record ClaseConfig(int idClase, int cupoTotal, double precio) {}

    public record Create( String codigo, long idRuta, LocalDateTime fechaSalida, LocalDateTime fechaLlegada, List<ClaseConfig> clases, List<EscalaCreate> escalas, Boolean activo) {}

    public record Update( long idVuelo, LocalDateTime fechaSalida, LocalDateTime fechaLlegada, boolean activo) {}

    public record View( long idVuelo, String codigo, long idRuta, String origen, String destino, LocalDateTime fechaSalida, LocalDateTime fechaLlegada, boolean activo, Integer idEstado, String estado, List<ClaseConfig> clases, List<EscalaView> escalas, Long idVueloPareja
    ) {
        public View( long idVuelo, String codigo, long idRuta, LocalDateTime fechaSalida, LocalDateTime fechaLlegada, boolean activo, List<ClaseConfig> clases, List<EscalaView> escalas
        ) {
            this(idVuelo, codigo, idRuta, null, null, fechaSalida, fechaLlegada, activo, null, null, clases, escalas, null);
        }
        public View( long idVuelo, String codigo, long idRuta, String origen, String destino, LocalDateTime fechaSalida, LocalDateTime fechaLlegada, boolean activo, List<ClaseConfig> clases, List<EscalaView> escalas
        ) {
            this(idVuelo, codigo, idRuta, origen, destino, fechaSalida, fechaLlegada, activo, null, null, clases, escalas, null);
        }
    }

    public record ViewAdmin( long idVuelo, String codigo, long idRuta, String origen, String destino, LocalDateTime fechaSalida, LocalDateTime fechaLlegada, boolean activo, Integer idEstado, String estado, List<ClaseConfig> clases, List<EscalaView> escalas, Long idVueloPareja, String codigoPareja ) {}

    public static record EstadoUpdate(Integer idEstado, String motivo) {}

    public record EscalaCreate(Long idCiudad, LocalDateTime llegada, LocalDateTime salida) {}

    public record EscalaView(Long idCiudad, String ciudad, String pais, LocalDateTime llegada, LocalDateTime salida) {}

    public record CreatePair(Create ida, Create regreso) {}

    public record CreatedPair(Long idIda, Long idRegreso) {}

    public record UpdateAdmin( String codigo, long idRuta, LocalDateTime fechaSalida, LocalDateTime fechaLlegada, Boolean activo, List<ClaseConfig> clases, List<EscalaCreate> escalas, String motivoCambio ) {}
}
