package com.aerolineas.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class ComentarioDTO {

  public static record Create(String comentario, Long idPadre) {}

  public static final class View {
    private final Long idComentario;
    private final Long idVuelo;
    private final Long idUsuario;
    private final String autor;
    private final String comentario;
    private final LocalDateTime creadaEn;
    private final Long idPadre;             
    private final Integer ratingAutor;      
    private final List<View> respuestas = new ArrayList<>();

    public View(Long idComentario, Long idVuelo, Long idUsuario, String autor,
                String comentario, LocalDateTime creadaEn, Long idPadre,
                Integer ratingAutor) {
      this.idComentario = idComentario;
      this.idVuelo = idVuelo;
      this.idUsuario = idUsuario;
      this.autor = autor;
      this.comentario = comentario;
      this.creadaEn = creadaEn;
      this.idPadre = idPadre;
      this.ratingAutor = ratingAutor;
    }

    public Long getIdComentario() { return idComentario; }
    public Long getIdVuelo() { return idVuelo; }
    public Long getIdUsuario() { return idUsuario; }
    public String getAutor() { return autor; }
    public String getComentario() { return comentario; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
    public Long getIdPadre() { return idPadre; }
    public Integer getRatingAutor() { return ratingAutor; }
    public List<View> getRespuestas() { return respuestas; }
  }
}
