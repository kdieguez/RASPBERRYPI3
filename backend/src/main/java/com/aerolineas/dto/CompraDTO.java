package com.aerolineas.dto;

import java.math.BigDecimal;
import java.util.List;

public class CompraDTO {

  public static class AddItemReq {
    public long idVuelo;
    public int idClase;
    public int cantidad;
  }
  public static class UpdateQtyReq {
    public int cantidad;
  }

  public static class PaymentReq {
    public Tarjeta tarjeta;
    public Facturacion facturacion;

    public static class Tarjeta {
      public String nombre;
      public String numero;
      public Integer expMes;
      public Integer expAnio;
      public String cvv;
    }
    public static class Facturacion {
      public String direccion;
      public String ciudad;
      public String pais;
      public String zip;
    }
  }

  public static class CheckoutResp {
    public long idReserva;
    public CheckoutResp(long id) { this.idReserva = id; }
  }

  public static class CarritoResp {
    public long idCarrito;
    public long idUsuario;
    public String fechaCreacion;
    public BigDecimal total;
    public List<CarritoItem> items;
  }

  public static class CarritoItem {
    public long idItem;
    public long idVuelo;
    public String codigoVuelo;
    public String fechaSalida;
    public String fechaLlegada;
    public int idClase;
    public String clase;
    public int cantidad;
    public BigDecimal precioUnitario;
    public BigDecimal subtotal;
    public String ciudadOrigen;
    public String paisOrigen;
    public String ciudadDestino;
    public String paisDestino;
  }

  public static class ReservaListItem {
    public long idReserva;
    public long idUsuario;
    public int idEstado;
    public BigDecimal total;
    public String creadaEn;
    public String codigo;        
  }

  public static class ReservaDetalle {
    public long idReserva;
    public long idUsuario;
    public int idEstado;
    public BigDecimal total;
    public String creadaEn;
    public String codigo;         
    public List<ReservaItem> items;
  }

  public static class ReservaItem {
    public long idItem;
    public long idVuelo;
    public String codigoVuelo;
    public String fechaSalida;
    public String fechaLlegada;
    public int idClase;
    public String clase;
    public int cantidad;
    public BigDecimal precioUnitario;
    public BigDecimal subtotal;
    public String ciudadOrigen;
    public String paisOrigen;
    public String ciudadDestino;
    public String paisDestino;
    public String escalaCiudad;
    public String escalaPais;
    public String escalaLlegada;   // ISO-8601
    public String escalaSalida;    // ISO-8601
    public String regresoCodigo;
    public String regresoFechaSalida;   // ISO-8601
    public String regresoFechaLlegada;  // ISO-8601
    public String regresoCiudadOrigen;
    public String regresoPaisOrigen;
    public String regresoCiudadDestino;
    public String regresoPaisDestino;
  }
}
