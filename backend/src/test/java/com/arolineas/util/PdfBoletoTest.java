package com.aerolineas.util;

import com.aerolineas.dto.CompraDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfBoletoTest {


  private static void setField(Object bean, String name, Object value) throws Exception {
    Field f = bean.getClass().getDeclaredField(name);
    f.setAccessible(true);
    f.set(bean, value);
  }

  private static String callDt(String val) throws Exception {
    Method m = PdfBoleto.class.getDeclaredMethod("dt", String.class);
    m.setAccessible(true);
    return (String) m.invoke(null, val);
  }

  private static String callSanitize(String val) throws Exception {
    Method m = PdfBoleto.class.getDeclaredMethod("sanitize", String.class);
    m.setAccessible(true);
    return (String) m.invoke(null, val);
  }


  @Test
  void build_conDetalleCompletoGeneraPdfNoVacio_yTextoClave() throws Exception {
    CompraDTO.ReservaDetalle det = new CompraDTO.ReservaDetalle();
    setField(det, "creadaEn", Instant.parse("2025-01-01T12:00:00Z").toString());
    setField(det, "total", new BigDecimal("1234.56"));

    List<CompraDTO.ReservaItem> items = new ArrayList<>();

    CompraDTO.ReservaItem it1 = new CompraDTO.ReservaItem();
    setField(it1, "codigoVuelo", "VUELO-001");
    setField(it1, "clase", "Económica");
    setField(it1, "paisOrigen", "Guatemala");
    setField(it1, "ciudadOrigen", "Ciudad de Guatemala");
    setField(it1, "paisDestino", "México");
    setField(it1, "ciudadDestino", "Ciudad de México");
    setField(it1, "fechaSalida", "2025-01-10T14:00:00Z");
    setField(it1, "fechaLlegada", "2025-01-10T18:00:00Z");
    setField(it1, "cantidad", 2);
    setField(it1, "precioUnitario", new BigDecimal("500.00"));
    setField(it1, "subtotal", new BigDecimal("1000.00"));

    setField(it1, "escalaCiudad", "San Salvador");
    setField(it1, "escalaPais", "El Salvador");
    setField(it1, "escalaLlegada", "2025-01-10T15:00:00Z");
    setField(it1, "escalaSalida", "2025-01-10T16:00:00Z");

    setField(it1, "regresoCodigo", "VUELO-001");
    setField(it1, "regresoCiudadOrigen", "Ciudad de México");
    setField(it1, "regresoPaisOrigen", "México");
    setField(it1, "regresoCiudadDestino", "Guatemala");
    setField(it1, "regresoPaisDestino", "Guatemala");
    setField(it1, "regresoFechaSalida", "2025-01-20T10:00:00Z");
    setField(it1, "regresoFechaLlegada", "2025-01-20T14:00:00Z");

    items.add(it1);

    CompraDTO.ReservaItem it2 = new CompraDTO.ReservaItem();
    setField(it2, "codigoVuelo", "VUELO-002");
    setField(it2, "clase", "Business");
    setField(it2, "paisOrigen", "Guatemala");
    setField(it2, "ciudadOrigen", "Ciudad de Guatemala");
    setField(it2, "paisDestino", "Costa Rica");
    setField(it2, "ciudadDestino", "San José");
    setField(it2, "fechaSalida", "2025-02-01T08:00:00Z");
    setField(it2, "fechaLlegada", "2025-02-01T10:00:00Z");
    setField(it2, "cantidad", 1);
    setField(it2, "precioUnitario", new BigDecimal("234.56"));
    setField(it2, "subtotal", new BigDecimal("234.56"));

    setField(it2, "regresoCodigo", "RET-002");
    setField(it2, "regresoCiudadOrigen", "San José");
    setField(it2, "regresoPaisOrigen", "Costa Rica");
    setField(it2, "regresoCiudadDestino", "Guatemala");
    setField(it2, "regresoPaisDestino", "Guatemala");
    setField(it2, "regresoFechaSalida", "2025-02-10T14:00:00Z");
    setField(it2, "regresoFechaLlegada", "2025-02-10T16:00:00Z");

    items.add(it2);

    setField(det, "items", items);

    byte[] pdf = PdfBoleto.build(det, "ABC123", "Katherine", "kat@example.com");

    assertNotNull(pdf);
    assertTrue(pdf.length > 0);

    try (PDDocument doc = PDDocument.load(new ByteArrayInputStream(pdf))) {
      assertTrue(doc.getNumberOfPages() >= 1);

      PDFTextStripper stripper = new PDFTextStripper();
      String txt = stripper.getText(doc);

      assertTrue(txt.contains("ABC123"));             
      assertTrue(txt.contains("Katherine"));          
      assertTrue(txt.toLowerCase().contains("guatemala"));
      assertTrue(txt.toLowerCase().contains("méxico"));
    }
  }

  @Test
  void build_sinEmailUsaSobrecargaDeTresParametros() throws Exception {
    CompraDTO.ReservaDetalle det = new CompraDTO.ReservaDetalle();
    setField(det, "creadaEn", null);
    setField(det, "total", BigDecimal.ZERO);
    setField(det, "items", new ArrayList<CompraDTO.ReservaItem>());

    byte[] pdf = PdfBoleto.build(det, "XYZ999", "Comprador");
    assertNotNull(pdf);
    assertTrue(pdf.length > 0);
  }

  @Test
  void dt_conFechaValidaDevuelveFormatoLegible_yConInvalidaDevuelveOriginalONDash() throws Exception {
    String formatted = callDt("2025-01-01T12:00:00Z");
    assertNotNull(formatted);
    assertNotEquals("2025-01-01T12:00:00Z", formatted);  

    String same = callDt("no-es-fecha");
    assertEquals("no-es-fecha", same);

    String dash = callDt(null);
    assertEquals("-", dash);
  }

  @Test
  void sanitize_reemplazaCaracteresEspeciales() throws Exception {
    String in = "Hola\u2014mundo\u2192\n\"comillas\" \u2022 bullet";
    String out = callSanitize(in);

    assertFalse(out.contains("\n"));     
    assertTrue(out.contains("->"));      
    assertTrue(out.contains("-"));       
    assertTrue(out.contains("*"));       
  }
}
