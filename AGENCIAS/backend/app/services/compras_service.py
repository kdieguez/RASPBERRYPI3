from __future__ import annotations
from typing import Any, Dict, List, Optional, Tuple
from decimal import Decimal
from uuid import uuid4
import httpx
import re

from app.models.compras import (
    AddItemReq, UpdateQtyReq, PaymentReq, CarritoResp, CarritoItem,
    CheckoutResp, ReservaListItem, ReservaDetalle, ReservaItem,
)
from app.repositories import compras_repository as repo
from app.services.proveedores import aerolinea_client as aer
from app.services.mailer import send_email
from app.services.pdf_service import build_ticket_pdf
from app.services.vuelos_service import get_flight_detail_filtered

def _dec(v: Any) -> Decimal:
    try:
        return Decimal(str(v))
    except Exception:
        return Decimal("0.00")


def _apply_markup(precio_base: Decimal, markup_pct: Optional[float]) -> Decimal:
    if not markup_pct:
        return precio_base.quantize(Decimal("0.01"))
    return (precio_base * (Decimal("1.0") + Decimal(str(markup_pct)) / Decimal("100"))).quantize(Decimal("0.01"))


async def _recalculate_totals(cart: Dict[str, Any]) -> None:
    """
    Recalcula subtotales y total.
    Persistimos como float en Mongo para evitar problemas de serialización,
    pero al responder convertimos a Decimal.
    """
    total = Decimal("0.00")
    for it in cart.get("items", []):
        precio_final = _dec(it.get("precioFinal") if it.get("precioFinal") is not None else it.get("precioBase"))
        qty = int(it.get("cantidad", 1))
        sub = (precio_final * qty).quantize(Decimal("0.01"))
        it["subtotal"] = float(sub)
        total += sub
    cart["total"] = float(total)

async def get_cart(user: Dict[str, Any]) -> CarritoResp:
    c = await repo.get_cart(user["id"])
    await _recalculate_totals(c)

    items = [
        CarritoItem(**{
            "idItem": it.get("idItem"),
            "proveedor": it.get("proveedor", "AEROLINEAS"),
            "idVuelo": it.get("idVuelo"),
            "codigoVuelo": it.get("codigoVuelo"),
            "fechaSalida": it.get("fechaSalida"),
            "fechaLlegada": it.get("fechaLlegada"),
            "idClase": it.get("idClase"),
            "clase": it.get("clase"),
            "cantidad": it.get("cantidad", 1),
            "precioBase": _dec(it.get("precioBase")),
            "precioFinal": _dec(it.get("precioFinal")),
            "subtotal": _dec(it.get("subtotal")),
            "ciudadOrigen": it.get("ciudadOrigen"),
            "paisOrigen": it.get("paisOrigen"),
            "ciudadDestino": it.get("ciudadDestino"),
            "paisDestino": it.get("paisDestino"),
            "parejaDe": it.get("parejaDe"),
        })
        for it in c.get("items", [])
    ]

    return CarritoResp(
        idCarrito=c["id"],
        idUsuario=c["idUsuario"],
        fechaCreacion=c.get("fechaCreacion"),
        total=_dec(c.get("total")),
        items=items
    )


async def add_or_increment_item(user: Dict[str, Any], req: AddItemReq, incluir_pareja: bool) -> None:
    cart = await repo.get_cart(user["id"])

    proveedor_id = "AEROLINEAS"
    prov = await repo.get_proveedor(proveedor_id) or {"_id": "AEROLINEAS", "markup": {"porcentaje": 0}}
    markup_pct = (prov.get("markup") or {}).get("porcentaje") or 0

    item = next((x for x in cart["items"]
                 if x.get("proveedor") == proveedor_id
                 and int(x.get("idVuelo")) == int(req.idVuelo)
                 and int(x.get("idClase")) == int(req.idClase)), None)

    if item:
        item["cantidad"] = int(item.get("cantidad", 1)) + max(1, req.cantidad)
    else:
        detalle = await get_flight_detail_filtered(req.idVuelo, pax=max(1, req.cantidad))

        precio_base = None
        clase_txt = None
        codigo_vuelo = None
        salida = None
        llegada = None
        ori = None
        dest = None
        ori_pais = None
        dest_pais = None

        if detalle:
            codigo_vuelo = detalle.get("codigo")
            salida = detalle.get("fechaSalida")
            llegada = detalle.get("fechaLlegada")
            ori = detalle.get("origen")
            dest = detalle.get("destino")
            ori_pais = detalle.get("origenPais")
            dest_pais = detalle.get("destinoPais")
            for c in (detalle.get("clases") or []):
                if int(c.get("idClase")) == int(req.idClase):
                    precio_base = c.get("precio")
                    clase_txt = c.get("nombre")
                    break

        precio_base_dec = _dec(precio_base) if precio_base is not None else Decimal("0.00")
        precio_final_dec = _apply_markup(precio_base_dec, markup_pct) if precio_base is not None else Decimal("0.00")

        item = {
            "idItem": str(uuid4()),
            "proveedor": proveedor_id,
            "idVuelo": int(req.idVuelo),
            "idClase": int(req.idClase),
            "clase": clase_txt,
            "codigoVuelo": codigo_vuelo,
            "fechaSalida": salida,
            "fechaLlegada": llegada,
            "ciudadOrigen": ori,
            "paisOrigen": ori_pais,
            "ciudadDestino": dest,
            "paisDestino": dest_pais,
            "cantidad": max(1, req.cantidad),
            "precioBase": float(precio_base_dec) if precio_base is not None else None,
            "precioFinal": float(precio_final_dec) if precio_base is not None else None,
        }
        if incluir_pareja:
            item["parejaDe"] = int(req.idVuelo)
        cart["items"].append(item)

    await _recalculate_totals(cart)
    await repo.save_cart(cart)


async def update_quantity(user: Dict[str, Any], id_item: str, cantidad: int, sync_pareja: bool) -> None:
    cart = await repo.get_cart(user["id"])
    item = next((x for x in cart["items"] if x.get("idItem") == id_item), None)
    if not item:
        raise ValueError("Item no encontrado")
    item["cantidad"] = int(cantidad)

    await _recalculate_totals(cart)
    await repo.save_cart(cart)


async def remove_item(user: Dict[str, Any], id_item: str, sync_pareja: bool) -> None:
    cart = await repo.get_cart(user["id"])
    idx = next((i for i, x in enumerate(cart["items"]) if x.get("idItem") == id_item), None)
    if idx is None:
        return
    del cart["items"][idx]

    await _recalculate_totals(cart)
    await repo.save_cart(cart)

async def checkout(user: Dict[str, Any], payment: PaymentReq) -> CheckoutResp:
    """
    Orquesta el checkout:
    - Reenvía ítems del carrito local a Aerolíneas.
    - Ejecuta checkout real en Aerolíneas.
    - Trae el detalle de reserva, persiste la compra en Mongo y limpia el carrito.
    - Envía correo con PDF adjunto.
    """
    cart = await repo.get_cart(user["id"])
    if not cart.get("items"):
        raise ValueError("El carrito está vacío")

    vuelos_items = [it for it in cart["items"] if it.get("proveedor") == "AEROLINEAS"]
    if not vuelos_items:
        raise ValueError("No hay items de vuelo en el carrito")

    try:
        for it in vuelos_items:
            await aer.add_item(
                user_id=user["id"],
                id_vuelo=int(it["idVuelo"]),
                id_clase=int(it["idClase"]),
                cantidad=int(it.get("cantidad", 1)),
                incluir_pareja=bool(it.get("parejaDe"))
            )

        checkout_resp = await aer.checkout(
            user["id"],
            payment.dict(),
            user.get("email"),
            user.get("nombres") or user.get("name")
        )
        aero_id_reserva = str(checkout_resp.get("idReserva"))

        det = await aer.get_reserva_detalle(
            user["id"],
            int(aero_id_reserva),
            user.get("email"),
            user.get("nombres") or user.get("name")
        )
    except httpx.HTTPStatusError as e:
        detail = (e.response.text or "").strip() or f"Error del proveedor Aerolíneas ({e.response.status_code})"
        raise ValueError(detail)
    except httpx.HTTPError as e:
        raise ValueError(f"No se pudo contactar a Aerolíneas: {str(e)}")

    total = _dec(det.get("total"))

    compra_doc = {
        "idUsuario": user["id"],
        "tipo": "vuelo",
        "idEstado": 1,
        "total": float(total),
        "codigo": det.get("codigo"),
        "proveedores": [{
            "id": "AEROLINEAS",
            "idReservaProveedor": aero_id_reserva,
            "codigo": det.get("codigo")
        }],
        "detalle_vuelo": det,
    }
    saved = await repo.insert_compra(compra_doc)

    cart["items"] = []
    cart["total"] = 0.0
    await repo.save_cart(cart)

    pdf_bytes = await build_ticket_pdf(saved)
    if user.get("email"):
        html = f"""
        <div style="font-family:Inter,Arial,sans-serif">
          <h2 style="color:#E62727;margin:0">Compra confirmada #{saved['id']}</h2>
          <p>¡Gracias por tu compra! Adjuntamos tu boleto en PDF.</p>
          <p><strong>Total:</strong> {total}</p>
          <p style="color:#1E93AB">Aerolíneas · Agencia</p>
        </div>
        """
        try:
            await send_email(
                to=user["email"],
                subject=f"Compra confirmada #{saved['id']}",
                html=html,
                attachments=[("boleto.pdf", pdf_bytes, "application/pdf")]
            )
        except Exception:
            pass

    return CheckoutResp(idReserva=saved["id"])

async def list_compras(user: Dict[str, Any]) -> List[ReservaListItem]:
    rows = await repo.list_compras_by_user(user["id"])
    out: List[ReservaListItem] = []
    for r in rows:
        out.append(ReservaListItem(
            idReserva=r["id"],
            idUsuario=r["idUsuario"],
            idEstado=int(r.get("idEstado", 1)),
            total=_dec(r.get("total")),
            creadaEn=r.get("creadaEn"),
            codigo=r.get("codigo")
        ))
    return out


async def get_compra_detalle(user: Dict[str, Any], compra_id: str) -> ReservaDetalle:
    det = await repo.find_compra_detail(user["id"], compra_id)
    if not det:
        raise ValueError("Compra no encontrada")

    items: List[ReservaItem] = []
    if det.get("detalle_vuelo"):
        dv = det["detalle_vuelo"]
        for it in dv.get("items", []):
            items.append(ReservaItem(
                idItem=str(it.get("idItem")),
                proveedor="AEROLINEAS",
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
                regresoCodigo=it.get("regresoCodigo")
            ))

    return ReservaDetalle(
        idReserva=det["id"],
        idUsuario=det["idUsuario"],
        idEstado=int(det.get("idEstado", 1)),
        total=_dec(det.get("total")),
        creadaEn=det.get("creadaEn"),
        codigo=det.get("codigo"),
        items=items,
        compradorNombre=None,
        compradorEmail=None
    )

async def cancelar_compra(user: Dict[str, Any], compra_id: str, is_admin: bool) -> bool:
    """
    Marca la compra como cancelada en Agencia (idEstado=2).
    Si en el futuro quieres propagar al proveedor, aquí llamarías al endpoint de cancelación
    de Aerolíneas y, si es exitoso, actualizas Mongo.
    """
    await repo.update_estado_compra(compra_id, 2)
    return True

def _safe_filename(s: str) -> str:
    return re.sub(r'[^A-Za-z0-9._-]', '_', s or 'boleto')


async def get_compra_pdf(user: Dict[str, Any], compra_id: str) -> Tuple[bytes, str]:
    """
    Devuelve (pdf_bytes, nombre_archivo_base) para la compra indicada del usuario.
    """
    det = await repo.find_compra_detail(user["id"], compra_id)
    if not det:
        raise ValueError("Compra no encontrada")

    codigo = det.get("codigo") or compra_id
    pdf_bytes = await build_ticket_pdf(det)
    return pdf_bytes, _safe_filename(f"boleto-{codigo}")