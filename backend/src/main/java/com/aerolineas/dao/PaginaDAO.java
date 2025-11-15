package com.aerolineas.dao;

import com.aerolineas.config.DB;
import com.aerolineas.dto.PaginaDTO;
import com.aerolineas.dto.SeccionDTO;

import java.sql.*;
import java.util.*;

public class PaginaDAO {

  public long crear(PaginaDTO.Upsert dto) throws SQLException {
    String paginaTable = DB.table("PAGINA_INFORMATIVA");
    String sql = "INSERT INTO " + paginaTable + " (NOMBRE_PAGINA, TITULO, DESCRIPCION) VALUES (?,?,?)";
    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql, new String[]{"ID_PAGINA"})) {
      ps.setString(1, dto.nombrePagina);
      ps.setString(2, dto.titulo);
      ps.setString(3, dto.descripcion);
      ps.executeUpdate();
      try (ResultSet rs = ps.getGeneratedKeys()) {
        if (rs.next()) return rs.getLong(1);
      }
    }
    throw new SQLException("No se generó ID_PAGINA");
  }

  public void actualizar(long idPagina, PaginaDTO.Upsert dto) throws SQLException {
    String paginaTable = DB.table("PAGINA_INFORMATIVA");
    String sql = "UPDATE " + paginaTable + " SET NOMBRE_PAGINA=?, TITULO=?, DESCRIPCION=? WHERE ID_PAGINA=?";
    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, dto.nombrePagina);
      ps.setString(2, dto.titulo);
      ps.setString(3, dto.descripcion);
      ps.setLong(4, idPagina);
      if (ps.executeUpdate() == 0) throw new SQLException("Página no encontrada");
    }
  }

  public void eliminar(long idPagina) throws SQLException {
    String paginaTable = DB.table("PAGINA_INFORMATIVA");
    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement("DELETE FROM " + paginaTable + " WHERE ID_PAGINA=?")) {
      ps.setLong(1, idPagina);
      ps.executeUpdate();
    }
  }

  public List<PaginaDTO> listar() throws SQLException {
    String paginaTable = DB.table("PAGINA_INFORMATIVA");
    String sql = "SELECT ID_PAGINA, NOMBRE_PAGINA, TITULO, DBMS_LOB.SUBSTR(DESCRIPCION, 4000, 1) AS DESCRIPCION FROM " + paginaTable + " ORDER BY ID_PAGINA";
    List<PaginaDTO> out = new ArrayList<>();
    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        PaginaDTO p = new PaginaDTO();
        p.idPagina = rs.getLong("ID_PAGINA");
        p.nombrePagina = rs.getString("NOMBRE_PAGINA");
        p.titulo = rs.getString("TITULO");
        p.descripcion = rs.getString("DESCRIPCION");
        out.add(p);
      }
    }
    return out;
  }

  public PaginaDTO obtenerConContenido(long idPagina) throws SQLException {
    PaginaDTO p = obtenerSimplePorId(idPagina);
    if (p == null) return null;
    p.secciones = new SeccionDAO().listarPorPagina(idPagina);
    return p;
  }

  public PaginaDTO obtenerPorNombreConContenido(String nombre) throws SQLException {
    PaginaDTO p = obtenerSimplePorNombre(nombre);
    if (p == null) return null;
    p.secciones = new SeccionDAO().listarPorPagina(p.idPagina);
    return p;
  }

  private PaginaDTO obtenerSimplePorId(long id) throws SQLException {
    String paginaTable = DB.table("PAGINA_INFORMATIVA");
    String sql = "SELECT ID_PAGINA, NOMBRE_PAGINA, TITULO, DBMS_LOB.SUBSTR(DESCRIPCION, 4000, 1) AS DESCRIPCION FROM " + paginaTable + " WHERE ID_PAGINA=?";
    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) return null;
        PaginaDTO p = new PaginaDTO();
        p.idPagina = rs.getLong(1);
        p.nombrePagina = rs.getString(2);
        p.titulo = rs.getString(3);
        p.descripcion = rs.getString(4);
        return p;
      }
    }
  }

  private PaginaDTO obtenerSimplePorNombre(String nombre) throws SQLException {
    String paginaTable = DB.table("PAGINA_INFORMATIVA");
    String sql = "SELECT ID_PAGINA, NOMBRE_PAGINA, TITULO, DBMS_LOB.SUBSTR(DESCRIPCION, 4000, 1) AS DESCRIPCION FROM " + paginaTable + " WHERE NOMBRE_PAGINA=?";
    try (Connection cn = DB.getConnection();
         PreparedStatement ps = cn.prepareStatement(sql)) {
      ps.setString(1, nombre);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) return null;
        PaginaDTO p = new PaginaDTO();
        p.idPagina = rs.getLong(1);
        p.nombrePagina = rs.getString(2);
        p.titulo = rs.getString(3);
        p.descripcion = rs.getString(4);
        return p;
      }
    }
  }
}
