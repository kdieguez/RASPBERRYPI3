package com.aerolineas.dto;

public class PaisDTOs {
    public record Create(String nombre) {}
    public record View(long idPais, String nombre) {}
}
