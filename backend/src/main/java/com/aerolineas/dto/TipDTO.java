package com.aerolineas.dto;

public class TipDTO {
  public long idTip;
  public String titulo;
  public String descripcion;
  public int orden;

  public static class Upsert {
    public String titulo;
    public String descripcion;
    public Integer orden; 
  }
}
