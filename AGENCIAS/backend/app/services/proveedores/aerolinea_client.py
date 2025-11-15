from __future__ import annotations

import os
from typing import Any, Dict, List, Optional, Tuple

import httpx

from app.core.config import AEROLINEAS_API_URL, AEROLINEAS_TIMEOUT

WS_TOKEN = os.getenv("AEROLINEAS_WS_TOKEN", "").strip()
AER_FALLBACK_USER_ID = (os.getenv("AEROLINEAS_FALLBACK_USER_ID") or "").strip()


def _sanitize_user_id(user_id: Optional[str]) -> str:
    if user_id and str(user_id).isdigit():
        return str(user_id)

    if AER_FALLBACK_USER_ID and AER_FALLBACK_USER_ID.isdigit():
        return AER_FALLBACK_USER_ID

    raise ValueError(
        "Aerolíneas requiere un X-User-Id numérico. "
        "Define AEROLINEAS_FALLBACK_USER_ID en .env o guarda aeroUserId numérico por usuario."
    )


def _headers_for(
    user_id: Optional[str],
    email: Optional[str] = None,
    name: Optional[str] = None,
) -> Dict[str, str]:
    uid = _sanitize_user_id(user_id)
    h: Dict[str, str] = {
        "Accept": "application/json",
        "X-User-Id": uid,
    }
    if email:
        h["X-User-Email"] = email
    if name:
        h["X-User-Name"] = name
    if WS_TOKEN:
        h["Authorization"] = f"Bearer {WS_TOKEN}"
    return h


async def _request(
    method: str,
    path: str,
    *,
    user_id: Optional[str],
    email: Optional[str] = None,
    name: Optional[str] = None,
    **kwargs: Any,
) -> httpx.Response:
    headers = _headers_for(user_id, email, name)

    async with httpx.AsyncClient(
        base_url=AEROLINEAS_API_URL,
        timeout=AEROLINEAS_TIMEOUT,
        headers=headers,
    ) as client:
        try:
            r = await client.request(method, path, **kwargs)
            r.raise_for_status()
            return r
        except httpx.HTTPStatusError as e:
            msg = ""
            try:
                data = e.response.json()
                msg = data.get("error") or data.get("detail") or ""
            except Exception:
                pass
            if not msg:
                msg = f"Error {e.response.status_code} llamando a Aerolíneas"
            raise RuntimeError(msg) from e
        except httpx.RequestError as e:
            raise RuntimeError(f"Error de conexión con Aerolíneas: {e}") from e

async def get_cart(user_id: str) -> Dict[str, Any]:
    r = await _request("GET", "/api/compras/carrito", user_id=user_id)
    return r.json()


async def add_item(
    user_id: str,
    id_vuelo: int,
    id_clase: int,
    cantidad: int,
    incluir_pareja: bool = False,
) -> None:
    payload = {"idVuelo": id_vuelo, "idClase": id_clase, "cantidad": cantidad}
    qp = "?pair=true" if incluir_pareja else ""
    await _request(
        "POST",
        f"/api/compras/items{qp}",
        user_id=user_id,
        json=payload,
    )


async def update_item(
    user_id: str,
    id_item: int,
    cantidad: int,
    sync_pareja: bool = False,
) -> None:
    qp = "?syncPareja=true" if sync_pareja else ""
    payload = {"cantidad": cantidad}
    await _request(
        "PUT",
        f"/api/compras/items/{id_item}{qp}",
        user_id=user_id,
        json=payload,
    )


async def remove_item(
    user_id: str,
    id_item: int,
    sync_pareja: bool = False,
) -> None:
    qp = "?syncPareja=true" if sync_pareja else ""
    await _request(
        "DELETE",
        f"/api/compras/items/{id_item}{qp}",
        user_id=user_id,
    )

async def checkout(
    user_id: str,
    payment: Dict[str, Any],
    email: Optional[str],
    name: Optional[str],
) -> Dict[str, Any]:
    r = await _request(
        "POST",
        "/api/compras/checkout",
        user_id=user_id,
        email=email,
        name=name,
        json=payment,
    )
    return r.json()


async def list_reservas(
    user_id: str,
    email: Optional[str],
    name: Optional[str],
) -> List[Dict[str, Any]]:
    r = await _request(
        "GET",
        "/api/compras/reservas",
        user_id=user_id,
        email=email,
        name=name,
    )
    return r.json()


async def get_reserva_detalle(
    user_id: str,
    id_reserva: int,
    email: Optional[str],
    name: Optional[str],
) -> Dict[str, Any]:
    r = await _request(
        "GET",
        f"/api/compras/reservas/{id_reserva}",
        user_id=user_id,
        email=email,
        name=name,
    )
    return r.json()


async def cancelar_reserva(
    user_id: str,
    id_reserva: int,
    email: Optional[str],
    name: Optional[str],
) -> Dict[str, Any]:
    r = await _request(
        "POST",
        f"/api/compras/reservas/{id_reserva}/cancelar",
        user_id=user_id,
        email=email,
        name=name,
    )
    return r.json()


async def get_boleto_pdf(
    user_id: str,
    id_reserva: int,
    email: Optional[str],
    name: Optional[str],
) -> Tuple[bytes, str]:
    async with httpx.AsyncClient(
        base_url=AEROLINEAS_API_URL,
        timeout=AEROLINEAS_TIMEOUT,
        headers=_headers_for(user_id, email, name),
    ) as client:
        try:
            r = await client.get(f"/api/compras/reservas/{id_reserva}/boleto.pdf")
            r.raise_for_status()
        except httpx.HTTPStatusError as e:
            msg = ""
            try:
                data = e.response.json()
                msg = data.get("error") or data.get("detail") or ""
            except Exception:
                pass
            if not msg:
                msg = f"Error {e.response.status_code} descargando boleto"
            raise RuntimeError(msg) from e
        except httpx.RequestError as e:
            raise RuntimeError(f"Error de conexión con Aerolíneas: {e}") from e

        pdf_bytes = r.content
        dispo = r.headers.get("Content-Disposition", "")
        fname = "boleto"
        if "filename=" in dispo:
            try:
                raw = dispo.split("filename=", 1)[1].strip().strip('"')
                if raw.lower().endswith(".pdf"):
                    raw = raw[:-4]
                if raw:
                    fname = raw
            except Exception:
                pass
        return pdf_bytes, fname