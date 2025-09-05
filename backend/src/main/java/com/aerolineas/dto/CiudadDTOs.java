package com.aerolineas.dto;

public class CiudadDTOs {
    public record Create(long idPais, String nombre) {}
    public record View(long idCiudad, long idPais, String pais, String nombre, boolean activo) {}
}
