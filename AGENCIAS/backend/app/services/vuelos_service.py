import datetime as dt
import re
from typing import Any, Dict, List, Optional, Tuple

import httpx

from app.core.config import (
    ESTADO_CANCELADO_ID,
    ESTADO_CANCELADO_TXT,
)
from app.repositories.compras_repository import list_proveedores, get_proveedor
from app.services.proveedores.proveedor_service import obtener_headers_para_proveedor

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
      - Arreglos [yyyy, mm, dd, HH, MM] o [yyyy, mm, dd, HH, MM, SS]
    """
    if s is None:
        return None

    if isinstance(s, dt.datetime):
        return s.replace(microsecond=0).isoformat()

    try:
        if isinstance(s, (list, tuple)):
            parts = [int(x) for x in s]
            if len(parts) >= 5:
                year, month, day = parts[0], parts[1], parts[2]
                hour, minute = parts[3], parts[4]
                second = parts[5] if len(parts) >= 6 else 0
                dt_obj = dt.datetime(year, month, day, hour, minute, second)
                return dt_obj.replace(microsecond=0).isoformat()
    except Exception:
        pass

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

async def _get_json(url: str, *, headers: Optional[Dict[str, str]] = None, timeout: Optional[float] = None):
    async with httpx.AsyncClient(timeout=timeout or TIMEOUT, headers=headers or {"Accept": "application/json"}) as cli:
        r = await cli.get(url)
        r.raise_for_status()
        return r.json()

async def _providers() -> List[Dict[str, Any]]:
    """
    Proveedores habilitados ordenados por nombre.
    """
    try:
        return await list_proveedores(habilitado_only=True)
    except Exception:
        return []

async def _provider_base(prov: Dict[str, Any]) -> Optional[Tuple[str, float, Dict[str, str]]]:
    api = (prov.get("apiUrl") or "").rstrip("/")
    if not api:
        return None
    timeout = float(prov.get("timeout") or TIMEOUT)
    headers = await obtener_headers_para_proveedor(prov.get("_id") or prov.get("id") or "")
    return api, timeout, headers

async def fetch_all() -> List[Dict[str, Any]]:
    """
    Consulta /api/public/vuelos de TODOS los proveedores habilitados en Mongo
    y agrega en cada vuelo el identificador de proveedor.
    Si no hay proveedores, retorna lista vacía.
    """
    provs = await _providers()
    out: List[Dict[str, Any]] = []
    for prov in provs:
        try:
            meta = await _provider_base(prov)
            if not meta:
                continue
            api, timeout, headers = meta
            data = await _get_json(f"{api}/api/public/vuelos", headers=headers, timeout=timeout)
            pid = prov.get("_id") or prov.get("id") or "AEROLINEAS"
            nombre_prov = prov.get("nombre") or prov.get("name") or pid
            for v in data or []:
                v.setdefault("proveedor", pid)
                v.setdefault("nombreProveedor", nombre_prov)
                out.append(v)
        except Exception:
            # ignorar proveedor que falle
            continue
    return out

async def fetch_one(id_vuelo: int, proveedor_id: Optional[str] = None) -> Optional[Dict[str, Any]]:
    """
    Busca el vuelo por id en los proveedores habilitados.
    Si se proporciona proveedor_id, busca solo en ese proveedor.
    Si no se proporciona, busca en todos los proveedores (retorna el primero que encuentre).
    """
    provs = await _providers()
    
    if proveedor_id:
        provs = [p for p in provs if (p.get("_id") or p.get("id")) == proveedor_id]
        if not provs:
            return None
    
    for prov in provs:
        try:
            meta = await _provider_base(prov)
            if not meta:
                continue
            api, timeout, headers = meta
            v = await _get_json(f"{api}/api/public/vuelos/{id_vuelo}", headers=headers, timeout=timeout)
            if v:
                pid = prov.get("_id") or prov.get("id") or "AEROLINEAS"
                nombre_prov = prov.get("nombre") or prov.get("name") or pid
                v.setdefault("proveedor", pid)
                v.setdefault("nombreProveedor", nombre_prov)
                # Fallback: si fechas vienen nulas, intentar buscarlas en el listado público
                if not v.get("fechaSalida") or not v.get("fechaLlegada"):
                    try:
                        lista = await _get_json(f"{api}/api/public/vuelos", headers=headers, timeout=timeout)
                        match = next((x for x in (lista or []) if str(x.get("idVuelo")) == str(id_vuelo)), None)
                        if match:
                            v["fechaSalida"] = v.get("fechaSalida") or match.get("fechaSalida")
                            v["fechaLlegada"] = v.get("fechaLlegada") or match.get("fechaLlegada")
                            # Completar país de origen/destino si faltan
                            v["origenPais"] = v.get("origenPais") or match.get("origenPais")
                            v["destinoPais"] = v.get("destinoPais") or match.get("destinoPais")
                    except Exception:
                        pass
                if not v.get("fechaSalida") or not v.get("fechaLlegada"):
                    try:
                        v1 = await _get_json(f"{api}/api/v1/vuelos/{id_vuelo}", headers=headers, timeout=timeout)
                        if v1:
                            v["fechaSalida"] = v.get("fechaSalida") or v1.get("fechaSalida")
                            v["fechaLlegada"] = v.get("fechaLlegada") or v1.get("fechaLlegada")
                            v["origenPais"] = v.get("origenPais") or v1.get("origenPais")
                            v["destinoPais"] = v.get("destinoPais") or v1.get("destinoPais")
                    except Exception:
                        pass
                if not v.get("fechaSalida") or not v.get("fechaLlegada"):
                    try:
                        lista_v1 = await _get_json(f"{api}/api/v1/vuelos", headers=headers, timeout=timeout)
                        match_v1 = next((x for x in (lista_v1 or []) if str(x.get("idVuelo")) == str(id_vuelo)), None)
                        if match_v1:
                            v["fechaSalida"] = v.get("fechaSalida") or match_v1.get("fechaSalida")
                            v["fechaLlegada"] = v.get("fechaLlegada") or match_v1.get("fechaLlegada")
                            v["origenPais"] = v.get("origenPais") or match_v1.get("origenPais")
                            v["destinoPais"] = v.get("destinoPais") or match_v1.get("destinoPais")
                    except Exception:
                        pass
                # Tercer intento: si hay vuelo pareja, usar sus fechas como referencia
                if (not v.get("fechaSalida") or not v.get("fechaLlegada")) and v.get("idVueloPareja"):
                    try:
                        pareja_id = v.get("idVueloPareja")
                        vp = await _get_json(f"{api}/api/public/vuelos/{pareja_id}", headers=headers, timeout=timeout)
                        if vp:
                            v["fechaSalida"] = v.get("fechaSalida") or vp.get("fechaSalida")
                            v["fechaLlegada"] = v.get("fechaLlegada") or vp.get("fechaLlegada")
                        if not v.get("fechaSalida") or not v.get("fechaLlegada"):
                            vp1 = await _get_json(f"{api}/api/v1/vuelos/{pareja_id}", headers=headers, timeout=timeout)
                            if vp1:
                                v["fechaSalida"] = v.get("fechaSalida") or vp1.get("fechaSalida")
                                v["fechaLlegada"] = v.get("fechaLlegada") or vp1.get("fechaLlegada")
                    except Exception:
                        pass
                # Enriquecer nombres de clases si el proveedor los expone en /api/public/clases
                try:
                    clases_resp = await _get_json(f"{api}/api/public/clases", headers=headers, timeout=timeout)
                    id_to_nombre = {int(c.get("idClase")): c.get("nombre") for c in (clases_resp or []) if c.get("idClase") is not None}
                    for c in v.get("clases") or []:
                        if "clase" not in c or not c.get("clase"):
                            name = id_to_nombre.get(int(c.get("idClase"))) if c.get("idClase") is not None else None
                            if name:
                                c["clase"] = name
                except Exception:
                    pass
                return v
        except httpx.HTTPStatusError as e:
            if e.response.status_code == 404:
                continue
        except Exception:
            continue
    return None

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
        "proveedor": v.get("proveedor") or "AEROLINEAS",
        "nombreProveedor": v.get("nombreProveedor") or v.get("proveedor") or "AEROLINEAS",
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

async def detail(id_vuelo: int, proveedor_id: Optional[str] = None) -> Optional[Dict[str, Any]]:
    v = await fetch_one(id_vuelo, proveedor_id=proveedor_id)
    if not v:
        return None
    # Normalizar posibles variantes de campos de fecha según proveedor
    def _by_substring(d: Dict[str, Any], tokens: List[str]) -> Optional[Any]:
        try:
            low = {str(k).lower(): k for k in d.keys()}
            for t in tokens:
                t = t.lower()
                for lk, orig in low.items():
                    if t in lk and d.get(orig):
                        return d.get(orig)
        except Exception:
            pass
        return None

    fs_raw = (
        v.get("fechaSalida")
        or v.get("salida")
        or v.get("fecha_salida")
        or v.get("horaSalida")
        or v.get("salidaFecha")
        or v.get("salida_hora")
        or v.get("inicio")  # fallback genérico
        or _by_substring(v, ["fecha salida", "salida", "start"])
    )
    fl_raw = (
        v.get("fechaLlegada")
        or v.get("llegada")
        or v.get("fecha_llegada")
        or v.get("horaLlegada")
        or v.get("llegadaFecha")
        or v.get("llegada_hora")
        or v.get("fin")  # fallback genérico
        or _by_substring(v, ["fecha llegada", "llegada", "end", "arribo"])
    )
    v["fechaSalida"] = _iso(fs_raw)
    v["fechaLlegada"] = _iso(fl_raw)
    for e in v.get("escalas") or []:
        eraw_llegada = e.get("llegada") or e.get("fechaLlegada") or e.get("arribo") or e.get("horaLlegada")
        eraw_salida = e.get("salida") or e.get("fechaSalida") or e.get("partida") or e.get("horaSalida")
        e["llegada"] = _iso(eraw_llegada)
        e["salida"] = _iso(eraw_salida)
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
