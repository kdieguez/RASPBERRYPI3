package com.aerolineas.dto;

public class RutaDTOs {

    public record Create(long idCiudadOrigen, long idCiudadDestino) {}

    public record View(long idRuta, long idCiudadOrigen, String ciudadOrigen, long idCiudadDestino, String ciudadDestino, boolean activa) {}
}
