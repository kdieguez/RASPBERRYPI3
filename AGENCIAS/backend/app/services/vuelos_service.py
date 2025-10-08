import datetime as dt
from typing import Any, Dict, List, Optional
import httpx

from app.core.config import (
    AEROLINEAS_API_URL,
    ESTADO_CANCELADO_ID,
    ESTADO_CANCELADO_TXT,
    AEROLINEAS_TIMEOUT,
)

def _parse_date(date_str: Optional[str]) -> Optional[dt.date]:
    if not date_str:
        return None
    try:
        return dt.date.fromisoformat(date_str)
    except Exception:
        return None

def _parse_datetime_to_date(dt_str: str) -> Optional[dt.date]:
    if not dt_str:
        return None
    s = str(dt_str).strip()
    if s.endswith("Z"):
        s = s[:-1]
    try:
        return dt.datetime.fromisoformat(s).date()
    except Exception:
        return None

def _is_cancelled(v: Dict[str, Any]) -> bool:
    id_estado = v.get("idEstado")
    estado_txt = (v.get("estado") or "").strip().lower()
    if ESTADO_CANCELADO_ID and id_estado == ESTADO_CANCELADO_ID:
        return True
    return estado_txt == ESTADO_CANCELADO_TXT

def _min_price(v: Dict[str, Any]) -> Optional[float]:
    clases = v.get("clases") or []
    prices = [c.get("precio") for c in clases if isinstance(c.get("precio"), (int, float))]
    return min(prices) if prices else None

def _class_has_capacity(c: Dict[str, Any], pax: int) -> bool:
    dispo = c.get("disponible")
    if isinstance(dispo, int):
        return dispo >= pax

    if pax <= 1:
        return True

    return False

def _has_capacity(v: Dict[str, Any], pax: int) -> bool:
    clases = v.get("clases") or []
    return any(_class_has_capacity(c, pax) for c in clases)

async def _fetch_all_flights() -> List[Dict[str, Any]]:
    url = f"{AEROLINEAS_API_URL}/api/public/vuelos"
    async with httpx.AsyncClient(
        timeout=AEROLINEAS_TIMEOUT,
        headers={"Accept": "application/json"}
    ) as client:
        r = await client.get(url)
        r.raise_for_status()
        data = r.json()
        return data if isinstance(data, list) else []

async def _fetch_flight_detail(flight_id: int) -> Optional[Dict[str, Any]]:
    url = f"{AEROLINEAS_API_URL}/api/public/vuelos/{flight_id}"
    async with httpx.AsyncClient(
        timeout=AEROLINEAS_TIMEOUT,
        headers={"Accept": "application/json"}
    ) as client:
        r = await client.get(url)
        if r.status_code == 404:
            return None
        r.raise_for_status()
        return r.json()

def _valid_flight(v: Dict[str, Any]) -> bool:
    if not v.get("activo", True):
        return False
    if _is_cancelled(v):
        return False
    if _min_price(v) is None:
        return False
    return True

def _match_filters(
    v: Dict[str, Any],
    origin: Optional[str],
    destination: Optional[str],
    dep_date: Optional[dt.date],
    pax: int,
) -> bool:
    def norm(x: Optional[str]) -> str:
        return (x or "").strip().lower()

    if origin and norm(v.get("origen")) != norm(origin):
        return False
    if destination and norm(v.get("destino")) != norm(destination):
        return False
    if dep_date:
        fs = v.get("fechaSalida")
        d = _parse_datetime_to_date(str(fs))
        if not d or d != dep_date:
            return False
    if pax and not _has_capacity(v, pax):
        return False
    return True

def _to_catalog_item(v: Dict[str, Any]) -> Dict[str, Any]:
    return {
        "idVuelo": v.get("idVuelo"),
        "codigo": v.get("codigo"),
        "origen": v.get("origen"),
        "destino": v.get("destino"),
        "origenPais": v.get("origenPais"),
        "destinoPais": v.get("destinoPais"),
        "fechaSalida": v.get("fechaSalida"),
        "fechaLlegada": v.get("fechaLlegada"),
        "precioDesde": _min_price(v),
        "tieneEscala": bool(v.get("escalas") or []),
        "proveedor": "AEROLINEAS",
    }

async def search_flights(
    origin: Optional[str],
    destination: Optional[str],
    date_str: Optional[str],
    pax: int,
) -> List[Dict[str, Any]]:
    dep_date = _parse_date(date_str)
    vuelos = await _fetch_all_flights()

    filtered = []
    for v in vuelos:
        if not _valid_flight(v):
            continue
        if not _match_filters(v, origin, destination, dep_date, pax):
            continue
        filtered.append(v)

    filtered.sort(key=lambda x: (_min_price(x) or float("inf")))
    return [_to_catalog_item(v) for v in filtered]

async def get_flight_detail_filtered(flight_id: int, pax: int) -> Optional[Dict[str, Any]]:
    v = await _fetch_flight_detail(flight_id)
    if not v:
        return None
    if not _valid_flight(v):
        return None
    if pax and not _has_capacity(v, pax):
        return None
    return v

async def list_origins() -> List[str]:
    vuelos = await _fetch_all_flights()
    origins = sorted({(v.get("origen") or "").strip() for v in vuelos if _valid_flight(v)})
    return [o for o in origins if o]

async def list_destinations(origin: str) -> List[str]:
    def norm(x: Optional[str]) -> str:
        return (x or "").strip().lower()

    vuelos = await _fetch_all_flights()
    dests = sorted({
        (v.get("destino") or "").strip()
        for v in vuelos
        if _valid_flight(v) and norm(v.get("origen")) == norm(origin)
    })
    return [d for d in dests if d]