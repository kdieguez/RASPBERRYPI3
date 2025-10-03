package com.aerolineas.util;

import com.aerolineas.dto.CompraDTO;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class PdfBoleto {

  private static final Locale LOCALE_GT = Locale.forLanguageTag("es-GT");
  private static final NumberFormat NF = NumberFormat.getCurrencyInstance(LOCALE_GT);
  private static final DateTimeFormatter DTF =
      DateTimeFormatter.ofPattern("d 'de' MMM yyyy, h:mm a", new Locale("es", "MX"));

  private static final float[] PRIMARY = hex("#E62727");
  private static final float[] MUTED   = hex("#777777");
  private static final float[] LIGHT   = hex("#F5F5F7");
  private static final float[] BORDER  = hex("#E1E1E6");
  private static final float[] BLACK   = new float[]{0,0,0};
  private static final float[] WHITE   = new float[]{1,1,1};

  private PdfBoleto() {}

  private static String money(BigDecimal n) { if (n == null) n = BigDecimal.ZERO; return NF.format(n); }
  private static String dt(String s) {
    try {
      Instant ins = Instant.parse(s);
      LocalDateTime ldt = LocalDateTime.ofInstant(ins, ZoneId.systemDefault());
      return ldt.format(DTF);
    } catch (Exception e) { return s != null ? s : "-"; }
  }
  private static String getOpt(Object bean, String field) {
    if (bean == null) return "";
    try {
      var f = bean.getClass().getDeclaredField(field);
      f.setAccessible(true);
      Object v = f.get(bean);
      return v == null ? "" : String.valueOf(v);
    } catch (Exception ignore) { return ""; }
  }

  private static String sanitize(String s) {
    if (s == null) return "-";
    StringBuilder out = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++) {
      char ch = s.charAt(i);
      if (ch == '\n' || ch == '\r' || ch == '\t') { out.append(' '); continue; }
      if (ch == 0x2013 || ch == 0x2014) { out.append('-'); continue; }     
      if (ch == 0x2018 || ch == 0x2019) { out.append('\''); continue; }    
      if (ch == 0x201C || ch == 0x201D) { out.append('"'); continue; }     
      if (ch == 0x2022) { out.append('*'); continue; }                     
      if (ch >= 0x2500 && ch <= 0x25FF) { out.append('-'); continue; }     
      if (ch >= 32 && ch <= 255) { out.append(ch); } else { out.append('?'); }
    }
    return out.toString();
  }

  private static float[] hex(String hex) {
    String h = hex.replace("#","").trim();
    int r = Integer.parseInt(h.substring(0,2),16);
    int g = Integer.parseInt(h.substring(2,4),16);
    int b = Integer.parseInt(h.substring(4,6),16);
    return new float[]{ r/255f, g/255f, b/255f };
  }
  private static void fillRect(PDPageContentStream cs, float x, float y, float w, float h, float[] c) throws Exception {
    cs.setNonStrokingColor(c[0], c[1], c[2]); cs.addRect(x,y,w,h); cs.fill();
  }
  private static void strokeRect(PDPageContentStream cs, float x, float y, float w, float h, float[] c, float lw) throws Exception {
    cs.setStrokingColor(c[0], c[1], c[2]); cs.setLineWidth(lw); cs.addRect(x,y,w,h); cs.stroke();
  }
  private static void line(PDPageContentStream cs, float x1, float y, float x2, float[] c, float lw) throws Exception {
    cs.setStrokingColor(c[0], c[1], c[2]); cs.setLineWidth(lw); cs.moveTo(x1,y); cs.lineTo(x2,y); cs.stroke();
  }

  private static void text(PDPageContentStream cs, float x, float y,
                           PDType1Font font, float size, float[] color, String s) throws Exception {
    if (color == null) color = BLACK;
    s = sanitize(s);
    cs.beginText();
    cs.setFont(font, size);
    cs.setNonStrokingColor(color[0], color[1], color[2]);
    cs.newLineAtOffset(x, y);
    cs.showText(s);
    cs.endText();
  }
  private static void text(PDPageContentStream cs, float x, float y,
                           PDType1Font font, float size, String s) throws Exception {
    text(cs, x, y, font, size, null, s);
  }

  private static float textW(PDType1Font f, float size, String s) throws Exception {
    return f.getStringWidth(sanitize(s)) / 1000f * size;
  }
  private static void rightText(PDPageContentStream cs, float rightX, float y, PDType1Font f, float size, float[] c, String s) throws Exception {
    float w = textW(f, size, s); text(cs, rightX - w, y, f, size, c, s);
  }
  private static float ensureSpace(PDDocument doc, PDPage[] pageRef, PDPageContentStream[] csRef, float y, float needed, float margin) throws Exception {
    PDPage page = pageRef[0];
    if (y - needed < margin) {
      csRef[0].close();
      PDPage newPage = new PDPage(PDRectangle.LETTER);
      doc.addPage(newPage);
      pageRef[0] = newPage;
      csRef[0] = new PDPageContentStream(doc, newPage);
      return newPage.getMediaBox().getHeight() - margin;
    }
    return y;
  }

  private static void header(PDPageContentStream cs, PDPage page, float margin, String codigo) throws Exception {
    float W = page.getMediaBox().getWidth();
    float barH = 48;
    fillRect(cs, 0, page.getMediaBox().getHeight() - barH, W, barH, PRIMARY);
    text(cs, margin, page.getMediaBox().getHeight() - 31, PDType1Font.HELVETICA_BOLD, 16, WHITE, "Aerolíneas - Boleto");
    rightText(cs, W - margin, page.getMediaBox().getHeight() - 31, PDType1Font.HELVETICA_BOLD, 14, WHITE, "CÓDIGO: " + codigo);
  }
  private static void sectionTitle(PDPageContentStream cs, float x, float y, String title) throws Exception {
    text(cs, x, y, PDType1Font.HELVETICA_BOLD, 12.5f, BLACK, title.toUpperCase());
  }
  private static void keyVal(PDPageContentStream cs, float x, float y, String k, String v) throws Exception {
    text(cs, x, y, PDType1Font.HELVETICA, 11, MUTED, k + ": ");
    float kw = textW(PDType1Font.HELVETICA, 11, k + ": ");
    text(cs, x + kw, y, PDType1Font.HELVETICA_BOLD, 11, BLACK, (v == null || v.isBlank()) ? "-" : v);
  }
  private static void badge(PDPageContentStream cs, float x, float y, String label) throws Exception {
    float padX = 6, h = 16;
    float w = textW(PDType1Font.HELVETICA_BOLD, 9.5f, label) + padX * 2;
    fillRect(cs, x, y - h + 2, w, h, hex("#F0F1F3"));
    strokeRect(cs, x, y - h + 2, w, h, BORDER, 0.6f);
    text(cs, x + padX, y - 10, PDType1Font.HELVETICA_BOLD, 9.5f, label);
  }

  private static void itineraryCard(PDDocument doc, PDPage[] pageRef, PDPageContentStream[] csRef,
                                    float margin, float contentW, float y, CompraDTO.ReservaItem it) throws Exception {
    PDPageContentStream cs = csRef[0];
    float cardH = 102;
    y = ensureSpace(doc, pageRef, csRef, y, cardH + 16, margin);
    cs = csRef[0];

    float cardY = y - cardH;
    fillRect(cs, margin, cardY, contentW, cardH, WHITE);
    strokeRect(cs, margin, cardY, contentW, cardH, BORDER, 0.8f);
    fillRect(cs, margin, cardY + cardH - 24, contentW, 24, LIGHT);
    line(cs, margin, cardY + cardH - 24, margin + contentW, hex("#EDEDEF"), 0.8f);

    float badgesY = cardY + cardH - 9;
    String cod = (it.codigoVuelo != null ? it.codigoVuelo : "Vuelo");
    badge(cs, margin + 10,  badgesY, cod);
    float b1w = textW(PDType1Font.HELVETICA_BOLD, 9.5f, cod) + 12;
    badge(cs, margin + 16 + b1w, badgesY, it.clase != null ? it.clase : "Clase");

    String paisO = getOpt(it, "paisOrigen");
    String paisD = getOpt(it, "paisDestino");
    String ciuO  = getOpt(it, "ciudadOrigen");
    String ciuD  = getOpt(it, "ciudadDestino");
    String origen  = (!ciuO.isBlank() || !paisO.isBlank()) ? (ciuO.isBlank()? paisO : (ciuO + ", " + paisO)) : "-";
    String destino = (!ciuD.isBlank() || !paisD.isBlank()) ? (ciuD.isBlank()? paisD : (ciuD + ", " + paisD)) : "-";

    float leftX = margin + 14;
    float rightX = margin + contentW - 14;

    text(cs, leftX,               cardY + cardH - 40, PDType1Font.HELVETICA_BOLD, 12, BLACK, origen);
    rightText(cs, rightX,         cardY + cardH - 40, PDType1Font.HELVETICA_BOLD, 12, BLACK, destino);

    String arrow = "-->";
    float arrowW = textW(PDType1Font.HELVETICA, 11, arrow);
    float centerX = margin + (contentW / 2f);
    text(cs, centerX - (arrowW / 2f), cardY + cardH - 41, PDType1Font.HELVETICA, 11, MUTED, arrow);

    keyVal(cs, leftX,          cardY + cardH - 58, "Salida",  dt(String.valueOf(it.fechaSalida)));
    keyVal(cs, leftX,          cardY + cardH - 74, "Llegada", dt(String.valueOf(it.fechaLlegada)));

    keyVal(cs, rightX - 220,   cardY + cardH - 58, "Cantidad",   String.valueOf(it.cantidad));
    keyVal(cs, rightX - 220,   cardY + cardH - 74, "P. unitario", money(it.precioUnitario));
    keyVal(cs, rightX - 220,   cardY + cardH - 90, "Subtotal",    money(it.subtotal));

    line(cs, margin, cardY - 6, margin + contentW, BORDER, 0.6f);
  }

  public static byte[] build(CompraDTO.ReservaDetalle det, String codigo, String comprador, String email) throws Exception {
    try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      PDPage page = new PDPage(PDRectangle.LETTER);
      doc.addPage(page);
      PDPage[] pageRef = new PDPage[]{ page };
      PDPageContentStream cs = new PDPageContentStream(doc, page);
      PDPageContentStream[] csRef = new PDPageContentStream[]{ cs };

      float margin = 50, W = page.getMediaBox().getWidth();
      float contentW = W - margin*2;
      float y = page.getMediaBox().getHeight() - margin;

      header(cs, page, margin, codigo);

      y -= 70;
      sectionTitle(cs, margin, y, "Resumen");
      y -= 16; keyVal(cs, margin, y, "Comprador", (comprador == null || comprador.isBlank()) ? "-" : comprador);
      if (email != null && !email.isBlank()) {
        y -= 13; text(cs, margin + 74, y, PDType1Font.HELVETICA, 9.5f, MUTED, email);
      }
      y -= 16; keyVal(cs, margin, y, "Fecha de compra", det.creadaEn == null ? "-" : dt(det.creadaEn));
      y -= 16; keyVal(cs, margin, y, "Total", money(det.total));

      y -= 12; line(cs, margin, y, margin + contentW, BORDER, 1f); y -= 18;

      sectionTitle(cs, margin, y, "Itinerario");
      y -= 12;

      if (det.items != null && !det.items.isEmpty()) {
        for (var it : det.items) {
          y = ensureSpace(doc, pageRef, csRef, y, 130, margin);
          itineraryCard(doc, pageRef, csRef, margin, contentW, y, it);
          y -= 130;
        }
      } else {
        text(cs, margin, y, PDType1Font.HELVETICA, 11, MUTED, "No hay vuelos en esta reserva.");
        y -= 16;
      }

      cs = csRef[0];

      y = Math.max(y, 90);
      y -= 6; line(cs, margin, y, margin + contentW, BORDER, 1f); y -= 14;
      text(cs, margin, y, PDType1Font.HELVETICA, 9.5f, MUTED,
          "Este boleto es válido con identificación oficial del comprador. Consulta políticas de cambios y equipaje.");
      y -= 12;
      text(cs, margin, y, PDType1Font.HELVETICA, 9.5f, MUTED, "Contacto: soporte@aerolineas.com");

      cs.close();
      doc.save(out);
      return out.toByteArray();
    }
  }

  public static byte[] build(CompraDTO.ReservaDetalle det, String codigo, String comprador) throws Exception {
    return build(det, codigo, comprador, null);
  }
}
