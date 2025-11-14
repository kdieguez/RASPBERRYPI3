package com.aerolineas.dto;

public class CiudadDTOs {

    public record Create(long idPais, String nombre, String weatherQuery) {}

    public record View(long idCiudad, long idPais, String pais, String nombre, boolean activo) {}

    public record WeatherCity(long idCiudad, String ciudad, String pais, String weatherQuery) {}
}
