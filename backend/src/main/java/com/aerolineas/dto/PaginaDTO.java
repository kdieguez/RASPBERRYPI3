package com.aerolineas.dto;

import java.util.List;

public class PaginaDTO {
  public long idPagina;
  public String nombrePagina; 
  public String titulo;
  public String descripcion;
  public List<SeccionDTO> secciones;

    public static class Upsert {
    public String nombrePagina;
    public String titulo;
    public String descripcion;
  }
}
