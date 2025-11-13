package com.aerolineas.dto;

public class MediaDTO {
  public long idMedia;
  public long idSeccion;
  public String tipoMedia; 
  public String url;
  public int orden;

  public static class Upsert {
    public String tipoMedia; 
    public String url;
    public Integer orden; 
  }

  public static class Reordenar {
    public long idMedia;
    public int orden;
  }
}
