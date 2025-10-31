import datetime as dt
import re
from typing import Any, Dict, List, Optional

import httpx

from app.core.config import (
    AEROLINEAS_API_URL,
    ESTADO_CANCELADO_ID,
    ESTADO_CANCELADO_TXT,
)

TIMEOUT = 15.0

_MONTH = {
    "JAN": 1, "FEB": 2, "MAR": 3, "APR": 4, "MAY": 5, "JUN": 6,
    "JUL": 7, "AUG": 8, "SEP": 9, "OCT": 10, "NOV": 11, "DEC": 12,
}

_ORACLE_RE = re.compile(
    r"""^\s*
        (?P<d>\d{1,2})-(?P<mon>[A-Za-z]{3})-(?P<yy>\d{2})
        \s+
        (?P<h>\d{1,2})[.:](?P<m>\d{2})[.:](?P<s>\d{2})
        (?:\.\d+)?                   
        (?:\s*(?P<ap>AM|PM))?             
        \s*$""",
    re.X,
)

def _iso(s: Any) -> Optional[str]:
    """
    Normaliza a ISO8601 (YYYY-MM-DDTHH:MM:SS) sin microsegundos.
    Acepta:
      - ISO nativo (con 'T' o con espacio)
      - Formato Oracle 'DD-MON-YY HH.MM.SS[.fffffffff] [AM|PM]'
    """
    if s is None:
        return None

    if isinstance(s, dt.datetime):
        return s.replace(microsecond=0).isoformat()

    txt = str(s).strip()
    if not txt:
        return None

    try:
        fixed = txt.replace(" ", "T", 1) if " " in txt and "T" not in txt[:11] else txt
        return dt.datetime.fromisoformat(fixed).replace(microsecond=0).isoformat()
    except Exception:
        pass

    m = _ORACLE_RE.match(txt)
    if m:
        d = int(m.group("d"))
        mon_abbr = m.group("mon").upper()
        yy = int(m.group("yy"))
        h = int(m.group("h"))
        mi = int(m.group("m"))
        se = int(m.group("s"))
        ap = m.group("ap")

        year = 2000 + yy
        month = _MONTH.get(mon_abbr)
        if not month:
            return None

        if ap:
            h = h % 12
            if ap.upper() == "PM":
                h += 12

        try:
            dt_obj = dt.datetime(year, month, d, h, mi, se)
            return dt_obj.replace(microsecond=0).isoformat()
        except Exception:
            return None

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
    return bool(cancel_txt and estado == cancel_txt)

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

def _match(v: Dict[str, Any], origin: Optional[str], dest: Optional[str],
           date: Optional[str], pax: int) -> bool:
    def n(x): return (x or "").strip().lower()
    if origin and n(v.get("origen")) != n(origin):
        return False
    if dest and n(v.get("destino")) != n(dest):
        return False
    if date:
        try:
            goal = dt.date.fromisoformat(date)
            fs = _iso(v.get("fechaSalida"))
            if not fs:
                return False
            if dt.datetime.fromisoformat(fs).date() != goal:
                return False
        except Exception:
            return False
    return pax >= 1

async def search(origin: Optional[str], destination: Optional[str],
                 date: Optional[str], pax: int) -> List[Dict[str, Any]]:
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
        e["salida"] = _iso(e.get("salida"))
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
