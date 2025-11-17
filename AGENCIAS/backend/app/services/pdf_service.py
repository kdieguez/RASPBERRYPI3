from typing import Dict, Any
from datetime import datetime
from io import BytesIO
import traceback


async def build_ticket_pdf(detalle: Dict[str, Any]) -> bytes:
    """
    Genera un PDF del boleto usando los datos de MongoDB (agencia).
    Muestra información de todos los proveedores si hay múltiples.
    """
    try:
        return await _build_pdf_with_reportlab(detalle)
    except ImportError:
        print("[PDF] reportlab no está instalado, usando generador básico")
        return _build_basic_pdf_no_libs(detalle)
    except Exception as e:
        print(f"[PDF] Error generando PDF con reportlab: {e}")
        print(traceback.format_exc())
        try:
            return _build_basic_pdf_no_libs(detalle)
        except Exception as e2:
            print(f"[PDF] Error generando PDF básico: {e2}")
            print(traceback.format_exc())
            raise ValueError(f"No se pudo generar el PDF: {str(e2)}")


async def _build_pdf_with_reportlab(detalle: Dict[str, Any]) -> bytes:
    """Genera PDF usando reportlab"""
    from reportlab.lib.pagesizes import A4
    from reportlab.lib import colors
    from reportlab.lib.units import inch
    from reportlab.platypus import SimpleDocTemplate, Table, TableStyle, Paragraph, Spacer
    from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
    from reportlab.lib.enums import TA_CENTER, TA_LEFT
    
    buffer = BytesIO()
    doc = SimpleDocTemplate(
        buffer,
        pagesize=A4,
        topMargin=0.5*inch,
        bottomMargin=0.5*inch,
        leftMargin=0.5*inch,
        rightMargin=0.5*inch
    )
    story = []
    styles = getSampleStyleSheet()
    
    # Estilos
    title_style = ParagraphStyle(
        'CustomTitle',
        parent=styles['Heading1'],
        fontSize=18,
        textColor=colors.HexColor('#E62727'),
        spaceAfter=12,
        alignment=TA_CENTER,
    )
    
    heading_style = ParagraphStyle(
        'CustomHeading',
        parent=styles['Heading2'],
        fontSize=14,
        textColor=colors.HexColor('#1E93AB'),
        spaceAfter=8,
    )
    
    # Título
    story.append(Paragraph("AGENCIA DE VIAJES", title_style))
    story.append(Spacer(1, 0.2*inch))
    
    # Información de la compra
    compra_id = str(detalle.get('id') or detalle.get('_id') or 'N/A')
    codigo = str(detalle.get('codigo') or compra_id)
    total = float(detalle.get('total', 0))
    fecha_creacion = detalle.get('creadaEn')
    estado = int(detalle.get('idEstado', 1))
    
    # Formatear fecha
    fecha_str = "N/A"
    if fecha_creacion:
        try:
            if isinstance(fecha_creacion, str):
                fecha_creacion = fecha_creacion.replace('Z', '+00:00')
                fecha_obj = datetime.fromisoformat(fecha_creacion)
            elif isinstance(fecha_creacion, datetime):
                fecha_obj = fecha_creacion
            else:
                fecha_obj = datetime.now()
            fecha_str = fecha_obj.strftime("%d/%m/%Y %H:%M:%S")
        except:
            fecha_str = str(fecha_creacion) if fecha_creacion else "N/A"
    
    # Tabla de información general
    info_data = [
        ['Número de Compra:', f'#{compra_id}'],
        ['Código de Reserva:', codigo],
        ['Fecha de Compra:', fecha_str],
        ['Estado:', _estado_texto(estado)],
        ['Total:', f'Q {total:,.2f}'],
    ]
    
    info_table = Table(info_data, colWidths=[2.5*inch, 4*inch])
    info_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (0, -1), colors.HexColor('#F5F5F5')),
        ('TEXTCOLOR', (0, 0), (-1, -1), colors.black),
        ('ALIGN', (0, 0), (0, -1), 'LEFT'),
        ('ALIGN', (1, 0), (1, -1), 'LEFT'),
        ('FONTNAME', (0, 0), (0, -1), 'Helvetica-Bold'),
        ('FONTNAME', (1, 0), (1, -1), 'Helvetica'),
        ('FONTSIZE', (0, 0), (-1, -1), 10),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 8),
        ('TOPPADDING', (0, 0), (-1, -1), 8),
        ('GRID', (0, 0), (-1, -1), 1, colors.grey),
    ]))
    story.append(info_table)
    story.append(Spacer(1, 0.3*inch))
    
    # Información de proveedores
    proveedores = detalle.get('proveedores') or []
    detalles_vuelos = detalle.get('detalles_vuelos') or []
    if not detalles_vuelos and detalle.get('detalle_vuelo'):
        detalles_vuelos = [detalle['detalle_vuelo']]
    
    if proveedores:
        story.append(Paragraph("AEROLÍNEAS", heading_style))
        
        for idx, prov in enumerate(proveedores):
            proveedor_id = str(prov.get('id', 'N/A'))
            proveedor_nombre = str(prov.get('nombre', proveedor_id))
            reserva_prov = str(prov.get('idReservaProveedor', 'N/A'))
            codigo_prov = str(prov.get('codigo', 'N/A'))
            total_prov = float(prov.get('total', 0))
            
            story.append(Paragraph(f"<b>{proveedor_nombre}</b>", styles['Heading3']))
            
            prov_data = [
                ['Reserva:', reserva_prov],
                ['Código:', codigo_prov],
                ['Total:', f'Q {total_prov:,.2f}'],
            ]
            
            prov_table = Table(prov_data, colWidths=[1.5*inch, 5*inch])
            prov_table.setStyle(TableStyle([
                ('BACKGROUND', (0, 0), (0, -1), colors.HexColor('#E7F5FF')),
                ('TEXTCOLOR', (0, 0), (-1, -1), colors.black),
                ('ALIGN', (0, 0), (-1, -1), 'LEFT'),
                ('FONTNAME', (0, 0), (0, -1), 'Helvetica-Bold'),
                ('FONTSIZE', (0, 0), (-1, -1), 9),
                ('BOTTOMPADDING', (0, 0), (-1, -1), 6),
                ('TOPPADDING', (0, 0), (-1, -1), 6),
            ]))
            story.append(prov_table)
            
            # Detalles de vuelos de este proveedor
            if idx < len(detalles_vuelos):
                det_vuelo = detalles_vuelos[idx]
                items = det_vuelo.get('items', []) if isinstance(det_vuelo, dict) else []
                
                if items:
                    story.append(Spacer(1, 0.1*inch))
                    story.append(Paragraph("<b>Vuelos:</b>", styles['Normal']))
                    
                    vuelos_data = [['Vuelo', 'Origen', 'Destino', 'Salida', 'Llegada', 'Clase', 'Cant.', 'Precio']]
                    
                    for item in items:
                        if not isinstance(item, dict):
                            continue
                        vuelo = str(item.get('codigoVuelo') or f"#{item.get('idVuelo', 'N/A')}")
                        origen_ciudad = str(item.get('ciudadOrigen', 'N/A'))
                        origen_pais = str(item.get('paisOrigen', ''))
                        origen = f"{origen_ciudad}" + (f", {origen_pais}" if origen_pais else "")
                        destino_ciudad = str(item.get('ciudadDestino', 'N/A'))
                        destino_pais = str(item.get('paisDestino', ''))
                        destino = f"{destino_ciudad}" + (f", {destino_pais}" if destino_pais else "")
                        salida = _formatear_fecha(item.get('fechaSalida'))
                        llegada = _formatear_fecha(item.get('fechaLlegada'))
                        clase = str(item.get('clase') or f"Clase #{item.get('idClase', 'N/A')}")
                        cantidad = str(item.get('cantidad', 1))
                        precio_val = float(item.get('subtotal') or item.get('precioUnitario') or 0)
                        precio = f"Q {precio_val:,.2f}"
                        
                        vuelos_data.append([vuelo, origen, destino, salida, llegada, clase, cantidad, precio])
                    
                    if len(vuelos_data) > 1:
                        vuelos_table = Table(vuelos_data, colWidths=[0.7*inch, 1.1*inch, 1.1*inch, 0.9*inch, 0.9*inch, 0.7*inch, 0.4*inch, 0.7*inch])
                        vuelos_table.setStyle(TableStyle([
                            ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#1E93AB')),
                            ('TEXTCOLOR', (0, 0), (-1, 0), colors.whitesmoke),
                            ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
                            ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
                            ('FONTSIZE', (0, 0), (-1, 0), 7),
                            ('FONTSIZE', (0, 1), (-1, -1), 6),
                            ('BOTTOMPADDING', (0, 0), (-1, -1), 3),
                            ('TOPPADDING', (0, 0), (-1, -1), 3),
                            ('GRID', (0, 0), (-1, -1), 1, colors.grey),
                            ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, colors.HexColor('#F9F9F9')]),
                        ]))
                        story.append(vuelos_table)
            
            if idx < len(proveedores) - 1:
                story.append(Spacer(1, 0.2*inch))
    
    # Pie de página
    story.append(Spacer(1, 0.3*inch))
    story.append(Paragraph("Gracias por su compra", styles['Normal']))
    story.append(Paragraph("Agencia de Viajes", styles['Normal']))
    
    # Construir PDF
    doc.build(story)
    buffer.seek(0)
    pdf_bytes = buffer.getvalue()
    buffer.close()
    
    if len(pdf_bytes) < 100:
        raise ValueError("PDF generado está vacío o corrupto")
    
    return pdf_bytes


def _build_basic_pdf_no_libs(detalle: Dict[str, Any]) -> bytes:
    """
    Genera un PDF válido básico sin usar librerías externas.
    Usa la estructura básica de PDF (formato binario).
    """
    compra_id = str(detalle.get('id') or detalle.get('_id') or 'N/A')
    codigo = str(detalle.get('codigo') or compra_id)
    total = float(detalle.get('total', 0))
    fecha_creacion = detalle.get('creadaEn', 'N/A')
    estado = int(detalle.get('idEstado', 1))
    
    # Construir contenido de texto
    lines = []
    lines.append("=" * 70)
    lines.append("AGENCIA DE VIAJES")
    lines.append("=" * 70)
    lines.append("")
    lines.append(f"Numero de Compra: #{compra_id}")
    lines.append(f"Codigo de Reserva: {codigo}")
    lines.append(f"Fecha de Compra: {fecha_creacion}")
    lines.append(f"Estado: {_estado_texto(estado)}")
    lines.append(f"Total: Q {total:,.2f}")
    lines.append("")
    lines.append("-" * 70)
    lines.append("AEROLINEAS")
    lines.append("-" * 70)
    
    proveedores = detalle.get('proveedores') or []
    detalles_vuelos = detalle.get('detalles_vuelos') or []
    if not detalles_vuelos and detalle.get('detalle_vuelo'):
        detalles_vuelos = [detalle['detalle_vuelo']]
    
    for idx, prov in enumerate(proveedores):
        proveedor_id = str(prov.get('id', 'N/A'))
        proveedor_nombre = str(prov.get('nombre', proveedor_id))
        lines.append("")
        lines.append(f"Aerolinea: {proveedor_nombre}")
        lines.append(f"  Reserva: {prov.get('idReservaProveedor', 'N/A')}")
        lines.append(f"  Codigo: {prov.get('codigo', 'N/A')}")
        lines.append(f"  Total: Q {float(prov.get('total', 0)):,.2f}")
        
        if idx < len(detalles_vuelos):
            det_vuelo = detalles_vuelos[idx]
            items = det_vuelo.get('items', []) if isinstance(det_vuelo, dict) else []
            if items:
                lines.append("  Vuelos:")
                for item in items:
                    if not isinstance(item, dict):
                        continue
                    vuelo = str(item.get('codigoVuelo') or f"#{item.get('idVuelo', 'N/A')}")
                    origen = f"{item.get('ciudadOrigen', 'N/A')}, {item.get('paisOrigen', '')}"
                    destino = f"{item.get('ciudadDestino', 'N/A')}, {item.get('paisDestino', '')}"
                    salida = _formatear_fecha(item.get('fechaSalida'))
                    llegada = _formatear_fecha(item.get('fechaLlegada'))
                    clase = str(item.get('clase') or f"Clase #{item.get('idClase', 'N/A')}")
                    cantidad = item.get('cantidad', 1)
                    precio = float(item.get('subtotal') or item.get('precioUnitario') or 0)
                    
                    lines.append(f"    - {vuelo}: {origen} -> {destino}")
                    lines.append(f"      Salida: {salida} | Llegada: {llegada}")
                    lines.append(f"      {clase} | Cantidad: {cantidad} | Precio: Q {precio:,.2f}")
    
    lines.append("")
    lines.append("=" * 70)
    lines.append("Gracias por su compra")
    lines.append("Agencia de Viajes")
    lines.append("=" * 70)
    
    content = "\n".join(lines)
    
    # Generar PDF básico válido usando estructura mínima de PDF
    # Esto es un PDF 1.4 básico con solo texto
    pdf_content = _create_minimal_pdf(content)
    return pdf_content.encode('latin-1') if isinstance(pdf_content, str) else pdf_content


def _create_minimal_pdf(text_content: str) -> bytes:
    """
    Crea un PDF mínimo válido sin librerías externas.
    Usa la estructura básica de PDF 1.4.
    """
    # Escapar caracteres especiales para PDF
    def escape_pdf(s):
        s = s.replace('\\', '\\\\')
        s = s.replace('(', '\\(')
        s = s.replace(')', '\\)')
        return s
    
    text_escaped = escape_pdf(text_content)
    lines = text_escaped.split('\n')
    
    # Crear objetos PDF básicos
    pdf_parts = []
    
    # Header
    pdf_parts.append(b'%PDF-1.4\n')
    
    # Objeto 1: Catalog
    catalog_obj = b'''1 0 obj
<<
/Type /Catalog
/Pages 2 0 R
>>
endobj
'''
    pdf_parts.append(catalog_obj)
    
    # Objeto 2: Pages
    pages_obj = b'''2 0 obj
<<
/Type /Pages
/Kids [3 0 R]
/Count 1
>>
endobj
'''
    pdf_parts.append(pages_obj)
    
    # Objeto 3: Page
    page_content_obj_num = 4
    page_obj = f'''3 0 obj
<<
/Type /Page
/Parent 2 0 R
/MediaBox [0 0 612 792]
/Contents {page_content_obj_num} 0 R
/Resources <<
/Font <<
/F1 <<
/Type /Font
/Subtype /Type1
/BaseFont /Courier
>>
>>
>>
>>
endobj
'''.encode('latin-1')
    pdf_parts.append(page_obj)
    
    # Objeto 4: Content stream
    # Posicionar texto (PDF usa coordenadas desde abajo)
    y_start = 750
    line_height = 12
    content_lines = []
    y = y_start
    
    for line in lines[:60]:  # Limitar a ~60 líneas por página
        if y < 50:  # Nueva página si se acaba el espacio
            break
        content_lines.append(f"BT\n/F1 10 Tf\n50 {y} Td\n({line}) Tj\nET")
        y -= line_height
    
    content_text = '\n'.join(content_lines)
    content_obj = f'''{page_content_obj_num} 0 obj
<<
/Length {len(content_text)}
>>
stream
{content_text}
endstream
endobj
'''.encode('latin-1')
    pdf_parts.append(content_obj)
    
    # Cross-reference table
    xref_offset = sum(len(p) for p in pdf_parts)
    xref = b'''xref
0 5
0000000000 65535 f 
0000000009 00000 n 
0000000058 00000 n 
0000000115 00000 n 
0000000250 00000 n 
'''
    pdf_parts.append(xref)
    
    # Trailer
    trailer = f'''trailer
<<
/Size 5
/Root 1 0 R
>>
startxref
{xref_offset}
%%EOF
'''.encode('latin-1')
    pdf_parts.append(trailer)
    
    return b''.join(pdf_parts)


def _estado_texto(estado: int) -> str:
    """Convierte el ID de estado a texto"""
    estados = {
        1: "Confirmado",
        2: "Pendiente",
        3: "Cancelado",
    }
    return estados.get(estado, f"Estado {estado}")


def _formatear_fecha(fecha: Any) -> str:
    """Formatea una fecha a string legible"""
    if not fecha:
        return "N/A"
    try:
        if isinstance(fecha, str):
            fecha = fecha.replace('Z', '+00:00')
            fecha_obj = datetime.fromisoformat(fecha)
        elif isinstance(fecha, datetime):
            fecha_obj = fecha
        else:
            return str(fecha)
        return fecha_obj.strftime("%d/%m/%Y %H:%M")
    except:
        return str(fecha)[:16] if fecha else "N/A"
