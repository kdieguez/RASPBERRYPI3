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
    # Usar proveedor por defecto (se resuelve automáticamente en aer.get_cart)
    raw = await aer.get_cart(str(user["id"]))

    items: List[CarritoItem] = []
    for it in raw.get("items", []):
        # Extraer proveedor del item si está disponible, sino usar "AEROLINEAS" por defecto
        proveedor = it.get("proveedor") or "AEROLINEAS"
        items.append(
            CarritoItem(
                idItem=str(it.get("idItem")),
                proveedor=proveedor,
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

    return CarritoResp(
        idCarrito=str(raw.get("idCarrito") or raw.get("id")),
        idUsuario=str(raw.get("idUsuario") or user["id"]),
        fechaCreacion=raw.get("fechaCreacion"),
        total=_dec(raw.get("total")),
        items=items,
    )


async def add_or_increment_item(
    user: Dict[str, Any],
    req: AddItemReq,
    incluir_pareja: bool,
) -> None:
    await aer.add_item(
        user_id=str(user["id"]),
        id_vuelo=int(req.idVuelo),
        id_clase=int(req.idClase),
        cantidad=int(req.cantidad),
        incluir_pareja=incluir_pareja,
    )


async def update_quantity(
    user: Dict[str, Any],
    id_item: str,
    cantidad: int,
    sync_pareja: bool,
) -> None:
    await aer.update_item(
        user_id=str(user["id"]),
        id_item=int(id_item),
        cantidad=int(cantidad),
        sync_pareja=sync_pareja,
    )


async def remove_item(
    user: Dict[str, Any],
    id_item: str,
    sync_pareja: bool,
) -> None:
    await aer.remove_item(
        user_id=str(user["id"]),
        id_item=int(id_item),
        sync_pareja=sync_pareja,
    )

async def checkout(user: Dict[str, Any], payment: PaymentReq) -> CheckoutResp:
    carrito = await aer.get_cart(str(user["id"]))
    if not carrito.get("items"):
        raise ValueError("El carrito está vacío")

    vuelos_items = carrito.get("items") or []
    if not vuelos_items:
        raise ValueError("No hay items de vuelo en el carrito")

    # Obtener proveedor_id del primer item del carrito si está disponible
    # Si no, se usará el proveedor por defecto en aer.checkout
    proveedor_id = None
    primer_item = vuelos_items[0]
    if primer_item.get("proveedor"):
        proveedor_id = primer_item["proveedor"]

    try:
        checkout_resp = await aer.checkout(
            user_id=str(user["id"]),
            payment=payment.dict(),
            email=user.get("email"),
            name=user.get("nombres") or user.get("name"),
            proveedor_id=proveedor_id,
        )
        aero_id_reserva = str(
            checkout_resp.get("idReserva") or checkout_resp.get("id")
        )

        det = await aer.get_reserva_detalle(
            user_id=str(user["id"]),
            id_reserva=int(aero_id_reserva),
            email=user.get("email"),
            name=user.get("nombres") or user.get("name"),
            proveedor_id=proveedor_id,
        )
    except RuntimeError as e:
        raise ValueError(str(e))

    total = _dec(det.get("total"))

    # Usar el proveedor_id obtenido o "AEROLINEAS" por defecto
    proveedor_id_final = proveedor_id or "AEROLINEAS"

    compra_doc: Dict[str, Any] = {
        "idUsuario": user["id"],
        "tipo": "vuelo",
        "idEstado": int(det.get("idEstado", 1)),
        "total": float(total),
        "codigo": det.get("codigo"),
        "proveedores": [
            {
                "id": proveedor_id_final,
                "idReservaProveedor": aero_id_reserva,
                "codigo": det.get("codigo"),
            }
        ],
        "detalle_vuelo": det,
    }
    saved = await repo.insert_compra(compra_doc)

    # 5. PDF + correo
    pdf_bytes = await build_ticket_pdf(det)
    if user.get("email"):
        html = f"""
        <div style="font-family:Inter,Arial,sans-serif">
          <h2 style="color:#E62727;margin:0">
            Compra confirmada #{saved['id']} · Reserva {aero_id_reserva}
          </h2>
          <p>¡Gracias por tu compra! Adjuntamos tu boleto en PDF.</p>
          <p><strong>Total:</strong> {total}</p>
          <p style="color:#1E93AB">Aerolíneas · Agencia</p>
        </div>
        """
        try:
            await send_email(
                to=user["email"],
                subject=f"Compra confirmada #{saved['id']} · Reserva {aero_id_reserva}",
                html=html,
                attachments=[("boleto.pdf", pdf_bytes, "application/pdf")],
            )
        except Exception:
            pass

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
    # Buscar cualquier proveedor (no solo "AEROLINEAS")
    aer_prov = proveedores[0] if proveedores else None
    if aer_prov and aer_prov.get("idReservaProveedor"):
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
            if remote_estado is not None and int(remote_estado) != int(det.get("idEstado", 1)):
                det["idEstado"] = int(remote_estado)
                await repo.update_estado_compra(compra_id, int(remote_estado))
        except Exception:
            pass

    items: List[ReservaItem] = []
    if det.get("detalle_vuelo"):
        dv = det["detalle_vuelo"]
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
    if det.get("detalle_vuelo"):
        dv = det["detalle_vuelo"]
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
    # Buscar cualquier proveedor (no solo "AEROLINEAS")
    aer_prov = proveedores[0] if proveedores else None

    if aer_prov and aer_prov.get("idReservaProveedor"):
        proveedor_id = aer_prov.get("id")
        try:
            await aer.cancelar_reserva(
                user_id=str(user["id"]),
                id_reserva=int(aer_prov["idReservaProveedor"]),
                email=user.get("email"),
                name=user.get("nombres") or user.get("name"),
                proveedor_id=proveedor_id,
            )
        except RuntimeError as e:
            raise ValueError(str(e))

    await repo.update_estado_compra(compra_id, 2)
    return True


def _safe_filename(s: str) -> str:
    return re.sub(r"[^A-Za-z0-9._-]", "_", s or "boleto")


async def get_compra_pdf(user: Dict[str, Any], compra_id: str) -> Tuple[bytes, str]:
    """
    Devuelve (pdf_bytes, nombre_archivo_base) para la compra indicada del usuario.

    - Si tenemos idReservaProveedor, intenta descargar el boleto real desde Aerolíneas.
    - Si falla, usa el PDF simple generado localmente (build_ticket_pdf).
    """
    det = await repo.find_compra_detail(user["id"], compra_id)
    if not det:
        raise ValueError("Compra no encontrada")

    codigo = det.get("codigo") or compra_id

    proveedores = det.get("proveedores") or []
    # Buscar cualquier proveedor (no solo "AEROLINEAS")
    aer_prov = proveedores[0] if proveedores else None

    if aer_prov and aer_prov.get("idReservaProveedor"):
        proveedor_id = aer_prov.get("id")
        try:
            pdf_bytes, fname_remote = await aer.get_boleto_pdf(
                user_id=str(user["id"]),
                id_reserva=int(aer_prov["idReservaProveedor"]),
                email=user.get("email"),
                name=user.get("nombres") or user.get("name"),
                proveedor_id=proveedor_id,
            )
            return pdf_bytes, _safe_filename(fname_remote or f"boleto-{codigo}")
        except Exception:
            pass

    pdf_bytes = await build_ticket_pdf(det)
    return pdf_bytes, _safe_filename(f"boleto-{codigo}")