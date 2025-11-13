package com.aerolineas.dto;

import java.util.List;

public class SeccionDTO {
  public long idSeccion;
  public long idPagina;
  public String nombreSeccion;
  public String descripcion; 
  public int orden;
  public List<MediaDTO> media;

  public static class Upsert {
    public String nombreSeccion;
    public String descripcion;
    public Integer orden;     
  }

  public static class Reordenar {
    public long idSeccion;
    public int orden;
  }
}
