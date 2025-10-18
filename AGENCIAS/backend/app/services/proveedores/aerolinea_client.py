from __future__ import annotations
import httpx
from typing import Any, Dict, List, Optional
from app.core.config import AEROLINEAS_API_URL, AEROLINEAS_TIMEOUT

import os
WS_TOKEN = os.getenv("AEROLINEAS_WS_TOKEN", "").strip()

def _headers_for(user_id: str, email: Optional[str] = None, name: Optional[str] = None) -> Dict[str, str]:
    h = {
        "Accept": "application/json",
        "X-User-Id": user_id,
    }
    if email:
        h["X-User-Email"] = email
    if name:
        h["X-User-Name"] = name
    if WS_TOKEN:
        h["Authorization"] = f"Bearer {WS_TOKEN}"
    return h

async def add_item(user_id: str, id_vuelo: int, id_clase: int, cantidad: int, incluir_pareja: bool=False) -> None:
    url = f"{AEROLINEAS_API_URL}/api/compras/items"
    payload = {"idVuelo": id_vuelo, "idClase": id_clase, "cantidad": cantidad}
    qp = "?pair=true" if incluir_pareja else ""
    async with httpx.AsyncClient(timeout=AEROLINEAS_TIMEOUT, headers=_headers_for(user_id)) as client:
        r = await client.post(url + qp, json=payload)
        r.raise_for_status()

async def update_item(user_id: str, id_item: int, cantidad: int, sync_pareja: bool=False) -> None:
    url = f"{AEROLINEAS_API_URL}/api/compras/items/{id_item}"
    qp = "?syncPareja=true" if sync_pareja else ""
    payload = {"cantidad": cantidad}
    async with httpx.AsyncClient(timeout=AEROLINEAS_TIMEOUT, headers=_headers_for(user_id)) as client:
        r = await client.put(url + qp, json=payload)
        r.raise_for_status()

async def remove_item(user_id: str, id_item: int, sync_pareja: bool=False) -> None:
    url = f"{AEROLINEAS_API_URL}/api/compras/items/{id_item}"
    qp = "?syncPareja=true" if sync_pareja else ""
    async with httpx.AsyncClient(timeout=AEROLINEAS_TIMEOUT, headers=_headers_for(user_id)) as client:
        r = await client.delete(url + qp)
        r.raise_for_status()

async def checkout(user_id: str, payment: Dict[str, Any], email: Optional[str], name: Optional[str]) -> Dict[str, Any]:
    url = f"{AEROLINEAS_API_URL}/api/compras/checkout"
    async with httpx.AsyncClient(timeout=AEROLINEAS_TIMEOUT, headers=_headers_for(user_id, email, name)) as client:
        r = await client.post(url, json=payment)
        r.raise_for_status()
        return r.json()

async def get_reserva_detalle(user_id: str, id_reserva: int, email: Optional[str], name: Optional[str]) -> Dict[str, Any]:
    url = f"{AEROLINEAS_API_URL}/api/compras/reservas/{id_reserva}"
    async with httpx.AsyncClient(timeout=AEROLINEAS_TIMEOUT, headers=_headers_for(user_id, email, name)) as client:
        r = await client.get(url)
        r.raise_for_status()
        return r.json()