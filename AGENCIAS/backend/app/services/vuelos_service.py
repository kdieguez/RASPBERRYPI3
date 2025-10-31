import datetime as dt
from typing import Any, Dict, List, Optional
import httpx

from app.core.config import (
    AEROLINEAS_API_URL,
    ESTADO_CANCELADO_ID,
    ESTADO_CANCELADO_TXT,
)

TIMEOUT = 15.0

def _iso(s: Any) -> Optional[str]:
    """
    Normaliza a ISO-8601 sin microsegundos.
    Acepta:
      - datetime
      - string ISO o con espacio ("2025-09-15 08:00:00")
      - arreglo estilo Jackson [yyyy, m, d, H, M, S, (ms)]
      - dict con year, month, day, (hour, minute, second opcionales)
    """
    if s is None:
        return None

    if isinstance(s, dt.datetime):
        return s.replace(microsecond=0).isoformat()

    if isinstance(s, (list, tuple)) and len(s) >= 3:
        y = int(s[0]); m = int(s[1]); d = int(s[2])
        H = int(s[3]) if len(s) > 3 else 0
        M = int(s[4]) if len(s) > 4 else 0
        S = int(s[5]) if len(s) > 5 else 0
        us = (int(s[6]) * 1000) if len(s) > 6 else 0
        return dt.datetime(y, m, d, H, M, S, us).replace(microsecond=0).isoformat()

    if isinstance(s, dict) and {"year", "month", "day"} <= set(s.keys()):
        y = int(s["year"]); m = int(s["month"]); d = int(s["day"])
        H = int(s.get("hour", 0)); M = int(s.get("minute", 0)); S = int(s.get("second", 0))
        return dt.datetime(y, m, d, H, M, S).replace(microsecond=0).isoformat()

    txt = str(s).strip()
    if not txt:
        return None
    if " " in txt and "T" not in txt[:11]:
        txt = txt.replace(" ", "T", 1)
    if txt.endswith("Z"):
        txt = txt[:-1] + "+00:00"
    try:
        return dt.datetime.fromisoformat(txt).replace(microsecond=0).isoformat()
    except Exception:
        return None


def _min_price(v: Dict[str, Any]) -> Optional[float]:
    clases = v.get("clases") or []
    prices = [c.get("precio") for c in clases if isinstance(c.get("precio"), (int, float))]
    return min(prices) if prices else None


def _is_cancelled(v: Dict[str, Any]) -> bool:
    if v.get("idEstado") is not None and ESTADO_CANCELADO_ID is not None:
        if v.get("idEstado") == ESTADO_CANCELADO_ID:
            return True
    estado = (v.get("estado") or "").strip().lower()
    cancel_txt = (ESTADO_CANCELADO_TXT or "").strip().lower()
    return bool(cancel_txt) and estado == cancel_txt


async def _get_json(url: str):
    async with httpx.AsyncClient(timeout=TIMEOUT, headers={"Accept": "application/json"}) as cli:
        r = await cli.get(url)
        r.raise_for_status()
        return r.json()

async def fetch_all() -> List[Dict[str, Any]]:
    return await _get_json(f"{AEROLINEAS_API_URL}/api/public/vuelos")

async def fetch_one(id_vuelo: int) -> Optional[Dict[str, Any]]:
    try:
        return await _get_json(f"{AEROLINEAS_API_URL}/api/public/vuelos/{id_vuelo}")
    except httpx.HTTPStatusError as e:
        if e.response.status_code == 404:
            return None
        raise

def map_catalog_item(v: Dict[str, Any]) -> Dict[str, Any]:
    return {
        "idVuelo": v.get("idVuelo"),
        "codigo": v.get("codigo"),
        "origen": v.get("origen"),
        "destino": v.get("destino"),
        "origenPais": v.get("origenPais"),
        "destinoPais": v.get("destinoPais"),
        "fechaSalida": _iso(v.get("fechaSalida")),
        "fechaLlegada": _iso(v.get("fechaLlegada")),
        "precioDesde": _min_price(v),
        "tieneEscala": bool(v.get("escalas") or []),
        "proveedor": "AEROLINEAS",
    }


def _match(v: Dict[str, Any], origin: Optional[str], dest: Optional[str], date: Optional[str], pax: int) -> bool:
    def n(x): return (x or "").strip().lower()
    if origin and n(v.get("origen")) != n(origin): return False
    if dest   and n(v.get("destino")) != n(dest):  return False
    if date:
        try:
            goal = dt.date.fromisoformat(date)
            fs = _iso(v.get("fechaSalida"))
            if not fs: return False
            if dt.datetime.fromisoformat(fs).date() != goal: return False
        except Exception:
            return False
    return pax >= 1


async def search(origin: Optional[str], destination: Optional[str], date: Optional[str], pax: int) -> List[Dict[str, Any]]:
    vuelos = await fetch_all()
    out: List[Dict[str, Any]] = []
    for v in vuelos:
        if v.get("activo") is False:
            continue
        if _is_cancelled(v):
            continue
        if _min_price(v) is None:
            continue
        if not _match(v, origin, destination, date, pax):
            continue
        out.append(map_catalog_item(v))
    out.sort(key=lambda x: (x["precioDesde"] if x["precioDesde"] is not None else 9e18))
    return out


async def detail(id_vuelo: int) -> Optional[Dict[str, Any]]:
    v = await fetch_one(id_vuelo)
    if not v:
        return None
    v["fechaSalida"] = _iso(v.get("fechaSalida"))
    v["fechaLlegada"] = _iso(v.get("fechaLlegada"))
    for e in v.get("escalas") or []:
        e["llegada"] = _iso(e.get("llegada"))
        e["salida"]  = _iso(e.get("salida"))
    return v


async def list_origins() -> List[str]:
    vuelos = await fetch_all()
    return sorted({(v.get("origen") or "").strip() for v in vuelos if v.get("origen")})


async def list_destinations(origin: str) -> List[str]:
    o = (origin or "").strip().lower()
    vuelos = await fetch_all()
    return sorted({
        (v.get("destino") or "").strip()
        for v in vuelos
        if (v.get("origen") or "").strip().lower() == o
    })