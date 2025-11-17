from __future__ import annotations
from typing import Any, Dict, List, Optional, Tuple
from decimal import Decimal
import re

from app.models.compras import (
    AddItemReq,
    UpdateQtyReq,
    PaymentReq,
    CarritoResp,
    CarritoItem,
    CheckoutResp,
    ReservaListItem,
    ReservaDetalle,
    ReservaItem,
)
from app.repositories import compras_repository as repo
from app.services.proveedores import aerolinea_client as aer
from app.services.mailer import send_email
from app.services.pdf_service import build_ticket_pdf


def _dec(v: Any) -> Decimal:
    try:
        return Decimal(str(v))
    except Exception:
        return Decimal("0.00")

async def get_cart(user: Dict[str, Any]) -> CarritoResp:
    """
    Obtiene el carrito del usuario consultando TODOS los proveedores habilitados
    y combinando los resultados. Cada item se etiqueta con su proveedor correspondiente.
    """
    from app.repositories.compras_repository import list_proveedores
    
    all_items: List[CarritoItem] = []
    total = Decimal("0.00")
    id_carrito = None
    fecha_creacion = None
    
    try:
        proveedores = await list_proveedores(habilitado_only=True)
        for prov in proveedores:
            proveedor_id = prov.get("_id") or prov.get("id")
            if not proveedor_id:
                continue
            
            try:
                raw = await aer.get_cart(str(user["id"]), proveedor_id=proveedor_id)    
            
                if not id_carrito and raw.get("idCarrito") or raw.get("id"):
                    id_carrito = str(raw.get("idCarrito") or raw.get("id"))
                if not fecha_creacion and raw.get("fechaCreacion"):
                    fecha_creacion = raw.get("fechaCreacion")
                
                for it in raw.get("items", []):
                    all_items.append(
                        CarritoItem(
                            idItem=str(it.get("idItem")),
                            proveedor=proveedor_id,
                            idVuelo=int(it.get("idVuelo")),
                            codigoVuelo=it.get("codigoVuelo"),
                            fechaSalida=it.get("fechaSalida"),
                            fechaLlegada=it.get("fechaLlegada"),
                            idClase=int(it.get("idClase")),
                            clase=it.get("clase"),
                            cantidad=int(it.get("cantidad", 1)),
                            precioBase=_dec(it.get("precioBase") or it.get("precioUnitario")),
                            precioFinal=_dec(it.get("precioFinal") or it.get("precioUnitario")),
                            subtotal=_dec(it.get("subtotal")),
                            ciudadOrigen=it.get("ciudadOrigen"),
                            paisOrigen=it.get("paisOrigen"),
                            ciudadDestino=it.get("ciudadDestino"),
                            paisDestino=it.get("paisDestino"),
                            parejaDe=it.get("parejaDe"),
                        )
                    )
                    total += _dec(it.get("subtotal") or it.get("precioFinal") or it.get("precioUnitario") or 0)
            except Exception:
                continue
    except Exception:
        pass

    return CarritoResp(
        idCarrito=id_carrito or str(user["id"]),
        idUsuario=str(user["id"]),
        fechaCreacion=fecha_creacion,
        total=total,
        items=all_items,
    )


async def add_or_increment_item(
    user: Dict[str, Any],
    req: AddItemReq,
    incluir_pareja: bool,
) -> None:
    proveedor_id = req.proveedor if req.proveedor else None
    await aer.add_item(
        user_id=str(user["id"]),
        id_vuelo=int(req.idVuelo),
        id_clase=int(req.idClase),
        cantidad=int(req.cantidad),
        incluir_pareja=incluir_pareja,
        proveedor_id=proveedor_id,
    )


async def update_quantity(
    user: Dict[str, Any],
    id_item: str,
    cantidad: int,
    sync_pareja: bool,
    proveedor_id: Optional[str] = None,
) -> None:
    """
    Actualiza la cantidad de un item del carrito.
    Si no se proporciona proveedor_id, busca el item en todos los proveedores.
    """
    if not proveedor_id:
        cart = await get_cart(user)
        item = next((it for it in cart.items if it.idItem == id_item), None)
        if item and item.proveedor:
            proveedor_id = item.proveedor
        else:
            raise ValueError(f"Item {id_item} no encontrado en el carrito")
    
    await aer.update_item(
        user_id=str(user["id"]),
        id_item=int(id_item),
        cantidad=int(cantidad),
        sync_pareja=sync_pareja,
        proveedor_id=proveedor_id,
    )


async def remove_item(
    user: Dict[str, Any],
    id_item: str,
    sync_pareja: bool,
    proveedor_id: Optional[str] = None,
) -> None:
    """
    Elimina un item del carrito.
    Si no se proporciona proveedor_id, busca el item en todos los proveedores.
    """
    if not proveedor_id:
        cart = await get_cart(user)
        item = next((it for it in cart.items if it.idItem == id_item), None)
        if item and item.proveedor:
            proveedor_id = item.proveedor
        else:
            raise ValueError(f"Item {id_item} no encontrado en el carrito")
    
    await aer.remove_item(
        user_id=str(user["id"]),
        id_item=int(id_item),
        sync_pareja=sync_pareja,
        proveedor_id=proveedor_id,
    )

async def checkout(user: Dict[str, Any], payment: PaymentReq) -> CheckoutResp:
    """
    Realiza el checkout. El carrito puede tener items de múltiples proveedores.
    Se procesa cada proveedor por separado y se combinan los resultados en una sola compra.
    """
    cart = await get_cart(user)
    if not cart.items:
        raise ValueError("El carrito está vacío")

    items_por_proveedor: Dict[str, List[CarritoItem]] = {}
    for item in cart.items:
        if not item.proveedor:
            raise ValueError(f"El item {item.idItem} no tiene proveedor asignado")
        if item.proveedor not in items_por_proveedor:
            items_por_proveedor[item.proveedor] = []
        items_por_proveedor[item.proveedor].append(item)

    if not items_por_proveedor:
        raise ValueError("No hay items válidos en el carrito")

    proveedores_info: List[Dict[str, Any]] = []
    total_general = Decimal("0.00")
    detalles_vuelos: List[Dict[str, Any]] = []
    pdfs_adjuntos: List[Tuple[str, bytes, str]] = []
    codigos_reserva: List[str] = []

    for proveedor_id, items in items_por_proveedor.items():
        try:
            carrito_raw = await aer.get_cart(str(user["id"]), proveedor_id=proveedor_id)
            if not carrito_raw.get("items"):
                raise ValueError(f"El carrito del proveedor {proveedor_id} está vacío")

            cliente_final = {
                "email": user.get("email"),
                "nombres": user.get("nombres") or user.get("name") or "",
                "apellidos": user.get("apellidos") or "",
            }
            
            checkout_resp = await aer.checkout(
                user_id=str(user["id"]),
                payment=payment.dict(),
                email=user.get("email"),
                name=user.get("nombres") or user.get("name"),
                proveedor_id=proveedor_id,
                cliente_final=cliente_final,
            )
            aero_id_reserva = str(
                checkout_resp.get("idReserva") or checkout_resp.get("id")
            )

            try:
                det = await aer.get_reserva_detalle(
                    user_id=str(user["id"]), 
                    id_reserva=int(aero_id_reserva),
                    email=user.get("email"),
                    name=user.get("nombres") or user.get("name"),
                    proveedor_id=proveedor_id,
                )
            except Exception as det_error:
                print(f"[Checkout] Advertencia: No se pudo obtener detalle de reserva {aero_id_reserva}: {det_error}")
                det = {
                    "id": int(aero_id_reserva),
                    "idReserva": int(aero_id_reserva),
                    "codigo": f"RES-{aero_id_reserva}",
                    "total": 0.0,
                    "idEstado": 1,
                    "items": [],
                }

            total_proveedor = _dec(det.get("total"))
            total_general += total_proveedor

            nombre_proveedor = proveedor_id
            try:
                from app.repositories.compras_repository import get_proveedor
                prov_info = await get_proveedor(proveedor_id)
                if prov_info:
                    nombre_proveedor = prov_info.get("nombre") or prov_info.get("name") or proveedor_id
            except Exception:
                pass

            proveedores_info.append({
                "id": proveedor_id,
                "nombre": nombre_proveedor,
                "idReservaProveedor": aero_id_reserva,
                "codigo": det.get("codigo"),
                "total": float(total_proveedor),
            })

            detalles_vuelos.append(det)
            codigos_reserva.append(aero_id_reserva)

        except Exception as e:
            raise ValueError(f"Error procesando checkout del proveedor {proveedor_id}: {str(e)}")

    compra_doc: Dict[str, Any] = {
        "idUsuario": user["id"],
        "tipo": "vuelo",
        "idEstado": int(detalles_vuelos[0].get("idEstado", 1)) if detalles_vuelos else 1,
        "total": float(total_general),
        "codigo": "-".join(codigos_reserva) if len(codigos_reserva) > 1 else (codigos_reserva[0] if codigos_reserva else None),
        "proveedores": proveedores_info,
        "detalle_vuelo": detalles_vuelos[0] if len(detalles_vuelos) == 1 else None,
        "detalles_vuelos": detalles_vuelos if len(detalles_vuelos) > 1 else None,
        "creadaEn": None, 
    }
    saved = await repo.insert_compra(compra_doc)

    pdf_bytes = None
    try:
        compra_completa = await repo.find_compra_detail(user["id"], saved["id"])
        if compra_completa:
            print(f"[Checkout] Generando PDF para compra {saved['id']}")
            print(f"[Checkout] Datos compra: id={compra_completa.get('id')}, codigo={compra_completa.get('codigo')}, total={compra_completa.get('total')}")
            pdf_bytes = await build_ticket_pdf(compra_completa)
            print(f"[Checkout] PDF generado: {len(pdf_bytes)} bytes")
            if not pdf_bytes or len(pdf_bytes) < 100:
                print(f"[Checkout] ERROR: PDF generado está vacío o muy pequeño")
                pdf_bytes = None
        else:
            print(f"[Checkout] ERROR: No se encontró la compra {saved['id']} en MongoDB")
    except Exception as e:
        import traceback
        print(f"[Checkout] ERROR generando PDF desde MongoDB: {e}")
        print(traceback.format_exc())
        pdf_bytes = None

    if user.get("email"):
        codigos_str = ", ".join(codigos_reserva)
        html = f"""
        <div style="font-family:Inter,Arial,sans-serif">
          <h2 style="color:#E62727;margin:0">
            Compra confirmada #{saved['id']} · Reservas: {codigos_str}
          </h2>
          <p>¡Gracias por tu compra! Se han procesado {len(proveedores_info)} reserva(s) de diferentes aerolíneas.</p>
          <p><strong>Total:</strong> Q {total_general:,.2f}</p>
          <p><strong>Aerolíneas:</strong> {', '.join([p.get('nombre', p.get('id', 'N/A')) for p in proveedores_info])}</p>
          <p style="color:#1E93AB">Agencia de Viajes</p>
        </div>
        """
        try:
            attachments = []
            if pdf_bytes:
                nombre_archivo = f"boleto_agencia_{saved['id']}.pdf"
                attachments.append((nombre_archivo, pdf_bytes, "application/pdf"))
            
            await send_email(
                to=user["email"],
                subject=f"Compra confirmada #{saved['id']} · {len(proveedores_info)} reserva(s)",
                html=html,
                attachments=attachments if attachments else None,
            )
        except Exception as e:
            print(f"Error enviando email: {e}")

    return CheckoutResp(idReserva=saved["id"])


async def list_compras(user: Dict[str, Any]) -> List[ReservaListItem]:
    """
    Lista compras guardadas en la Agencia (Mongo) para el usuario.
    El detalle "vivo" siempre está en Aerolíneas.
    """
    rows = await repo.list_compras_by_user(user["id"])
    out: List[ReservaListItem] = []
    for r in rows:
        out.append(
            ReservaListItem(
                idReserva=r["id"],
                idUsuario=r["idUsuario"],
                idEstado=int(r.get("idEstado", 1)),
                total=_dec(r.get("total")),
                creadaEn=r.get("creadaEn"),
                codigo=r.get("codigo"),
            )
        )
    return out


async def get_compra_detalle(user: Dict[str, Any], compra_id: str) -> ReservaDetalle:
    """
    Detalle de compra desde la Agencia, sincronizando estado con Aerolíneas
    si tenemos el idReservaProveedor.
    """
    det = await repo.find_compra_detail(user["id"], compra_id)
    if not det:
        raise ValueError("Compra no encontrada")

    proveedores = det.get("proveedores") or []
    estados_proveedores = []
    for aer_prov in proveedores:
        if not aer_prov.get("idReservaProveedor"):
            continue
        proveedor_id = aer_prov.get("id")
        try:
            remote = await aer.get_reserva_detalle(
                user_id=str(user["id"]),
                id_reserva=int(aer_prov["idReservaProveedor"]),
                email=user.get("email"),
                name=user.get("nombres") or user.get("name"),
                proveedor_id=proveedor_id,
            )
            remote_estado = remote.get("idEstado")
            if remote_estado is not None:
                estados_proveedores.append(int(remote_estado))
        except Exception:
            pass
    
    if estados_proveedores:
        nuevo_estado = max(estados_proveedores)
        if nuevo_estado != int(det.get("idEstado", 1)):
            det["idEstado"] = nuevo_estado
            await repo.update_estado_compra(compra_id, nuevo_estado)

    items: List[ReservaItem] = []
    
    detalles_vuelos = det.get("detalles_vuelos") or []
    if not detalles_vuelos and det.get("detalle_vuelo"):
        detalles_vuelos = [det["detalle_vuelo"]]
    
    for dv in detalles_vuelos:
        for it in dv.get("items", []):
            # Extraer proveedor del item si está disponible
            proveedor = it.get("proveedor") or "AEROLINEAS"
            items.append(
                ReservaItem(
                    idItem=str(it.get("idItem")),
                    proveedor=proveedor,
                    idVuelo=int(it.get("idVuelo")),
                    codigoVuelo=it.get("codigoVuelo"),
                    fechaSalida=it.get("fechaSalida"),
                    fechaLlegada=it.get("fechaLlegada"),
                    idClase=int(it.get("idClase")),
                    clase=it.get("clase"),
                    cantidad=int(it.get("cantidad", 1)),
                    precioUnitario=_dec(it.get("precioUnitario")),
                    subtotal=_dec(it.get("subtotal")),
                    ciudadOrigen=it.get("ciudadOrigen"),
                    paisOrigen=it.get("paisOrigen"),
                    ciudadDestino=it.get("ciudadDestino"),
                    paisDestino=it.get("paisDestino"),
                    regresoCodigo=it.get("regresoCodigo"),
                )
            )

    return ReservaDetalle(
        idReserva=det["id"],
        idUsuario=det["idUsuario"],
        idEstado=int(det.get("idEstado", 1)),
        total=_dec(det.get("total")),
        creadaEn=det.get("creadaEn"),
        codigo=det.get("codigo"),
        items=items,
        compradorNombre=None,
        compradorEmail=None,
    )


async def list_compras_admin() -> List[ReservaListItem]:
    rows = await repo.list_compras_admin()
    out: List[ReservaListItem] = []
    for r in rows:
        out.append(
            ReservaListItem(
                idReserva=r["id"],
                idUsuario=r["idUsuario"],
                idEstado=int(r.get("idEstado", 1)),
                total=_dec(r.get("total")),
                creadaEn=r.get("creadaEn"),
                codigo=r.get("codigo"),
            )
        )
    return out


async def get_compra_detalle_admin(compra_id: str) -> ReservaDetalle:
    det = await repo.find_compra_detail_admin(compra_id)
    if not det:
        raise ValueError("Compra no encontrada")

    items: List[ReservaItem] = []
    
    detalles_vuelos = det.get("detalles_vuelos") or []
    if not detalles_vuelos and det.get("detalle_vuelo"):
        detalles_vuelos = [det["detalle_vuelo"]]
    
    for dv in detalles_vuelos:
        for it in dv.get("items", []):
            # Extraer proveedor del item si está disponible
            proveedor = it.get("proveedor") or "AEROLINEAS"
            items.append(
                ReservaItem(
                    idItem=str(it.get("idItem")),
                    proveedor=proveedor,
                    idVuelo=int(it.get("idVuelo")),
                    codigoVuelo=it.get("codigoVuelo"),
                    fechaSalida=it.get("fechaSalida"),
                    fechaLlegada=it.get("fechaLlegada"),
                    idClase=int(it.get("idClase")),
                    clase=it.get("clase"),
                    cantidad=int(it.get("cantidad", 1)),
                    precioUnitario=_dec(it.get("precioUnitario")),
                    subtotal=_dec(it.get("subtotal")),
                    ciudadOrigen=it.get("ciudadOrigen"),
                    paisOrigen=it.get("paisOrigen"),
                    ciudadDestino=it.get("ciudadDestino"),
                    paisDestino=it.get("paisDestino"),
                    regresoCodigo=it.get("regresoCodigo"),
                )
            )

    return ReservaDetalle(
        idReserva=det["id"],
        idUsuario=det["idUsuario"],
        idEstado=int(det.get("idEstado", 1)),
        total=_dec(det.get("total")),
        creadaEn=det.get("creadaEn"),
        codigo=det.get("codigo"),
        items=items,
        compradorNombre=None,
        compradorEmail=None,
    )


async def cancelar_compra(
    user: Dict[str, Any],
    compra_id: str,
    is_admin: bool,
) -> bool:
    """
    Cancela una compra. Si la compra tiene múltiples proveedores (aerolíneas),
    cancela la reserva en cada una de ellas.
    """
    if is_admin:
        det = await repo.find_compra_detail_admin(compra_id)
    else:
        det = await repo.find_compra_detail(user["id"], compra_id)

    if not det:
        raise ValueError("Compra no encontrada")

    estado_actual = int(det.get("idEstado", 1))
    if estado_actual != 1:
        raise ValueError("Solo se pueden cancelar compras activas.")

    proveedores = det.get("proveedores") or []
    if not proveedores:
        raise ValueError("La compra no tiene proveedores asociados")

    errores = []
    cancelaciones_exitosas = 0
    
    for aer_prov in proveedores:
        if not aer_prov.get("idReservaProveedor"):
            continue
        
        proveedor_id = aer_prov.get("id")
        id_reserva_prov = aer_prov.get("idReservaProveedor")
        
        try:
            print(f"[Cancelar] Cancelando reserva {id_reserva_prov} en proveedor {proveedor_id}")
            await aer.cancelar_reserva(
                user_id=str(user["id"]),
                id_reserva=int(id_reserva_prov),
                email=user.get("email"),
                name=user.get("nombres") or user.get("name"),
                proveedor_id=proveedor_id,
            )
            cancelaciones_exitosas += 1
            print(f"[Cancelar] Reserva {id_reserva_prov} cancelada exitosamente en {proveedor_id}")
        except RuntimeError as e:
            error_msg = f"Error cancelando en {proveedor_id} (reserva {id_reserva_prov}): {str(e)}"
            errores.append(error_msg)
            print(f"[Cancelar] {error_msg}")
        except Exception as e:
            error_msg = f"Error inesperado cancelando en {proveedor_id} (reserva {id_reserva_prov}): {str(e)}"
            errores.append(error_msg)
            print(f"[Cancelar] {error_msg}")

    if cancelaciones_exitosas == 0:
        if errores:
            raise ValueError(f"No se pudo cancelar ninguna reserva. Errores: {'; '.join(errores)}")
        else:
            raise ValueError("No se encontraron reservas para cancelar en los proveedores")

    if errores and cancelaciones_exitosas > 0:
        print(f"[Cancelar] Advertencia: Se cancelaron {cancelaciones_exitosas} de {len(proveedores)} reservas. Errores: {'; '.join(errores)}")

    await repo.update_estado_compra(compra_id, 2)
    return True


def _safe_filename(s: str) -> str:
    return re.sub(r"[^A-Za-z0-9._-]", "_", s or "boleto")


async def get_compra_pdf(user: Dict[str, Any], compra_id: str) -> Tuple[bytes, str]:
    """
    Devuelve (pdf_bytes, nombre_archivo_base) para la compra indicada del usuario.
    Genera el PDF desde MongoDB (datos de la agencia).
    """
    det = await repo.find_compra_detail(user["id"], compra_id)
    if not det:
        raise ValueError("Compra no encontrada")

    codigo = det.get("codigo") or compra_id

    try:
        print(f"[PDF] Generando PDF para compra {compra_id}")
        print(f"[PDF] Datos: id={det.get('id')}, codigo={codigo}, total={det.get('total')}")
        pdf_bytes = await build_ticket_pdf(det)
        print(f"[PDF] PDF generado: {len(pdf_bytes)} bytes")
        
        if not pdf_bytes or len(pdf_bytes) < 100:
            raise ValueError(f"PDF generado está vacío o corrupto ({len(pdf_bytes) if pdf_bytes else 0} bytes)")
        
        return pdf_bytes, _safe_filename(f"boleto-agencia-{codigo}")
    except Exception as e:
        import traceback
        print(f"[PDF] ERROR generando PDF: {e}")
        print(traceback.format_exc())
        raise ValueError(f"Error generando PDF: {str(e)}")