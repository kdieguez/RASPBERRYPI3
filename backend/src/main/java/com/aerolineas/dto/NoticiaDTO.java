package com.aerolineas.dto;

import java.time.LocalDateTime;

public class NoticiaDTO {
  public long idNoticia;
  public String titulo;
  public String contenido;
  public LocalDateTime fechaPublicacion;
  public Integer orden;
  public String urlImagen;

  public static class Upsert {
    public String titulo;
    public String contenido;
    public LocalDateTime fechaPublicacion; 
    public Integer orden;                  
    public String urlImagen;               
  }
}
