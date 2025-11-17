from fastapi import APIRouter, Query, HTTPException
from typing import List, Optional, Dict, Any
import httpx

from app.services import vuelos_service as svc

router = APIRouter(prefix="/api/v1/vuelos", tags=["vuelos"])

def _norm(s: Optional[str]) -> Optional[str]:
    if s is None: 
        return None
    s2 = s.strip()
    return s2 if s2 else None

def _validate_iso_date(date_str: Optional[str]) -> Optional[str]:
    if date_str is None:
        return None
    try:
        import datetime as dt
        dt.date.fromisoformat(date_str)
        return date_str
    except Exception:
        raise HTTPException(status_code=400, detail="Parámetro 'date' debe ser YYYY-MM-DD")

@router.get("", summary="Buscar vuelos (catálogo)", response_model=List[Dict[str, Any]])
async def buscar_vuelos(
    origin: Optional[str] = Query(None),
    destination: Optional[str] = Query(None),
    date: Optional[str] = Query(None, description="YYYY-MM-DD"),
    pax: int = Query(1, ge=1, le=9),
):
    origin_n = _norm(origin)
    destination_n = _norm(destination)
    date_valid = _validate_iso_date(date)
    try:
        return await svc.search(origin_n, destination_n, date_valid, pax)
    except httpx.TimeoutException:
        raise HTTPException(status_code=502, detail="Aerolíneas no responde (timeout).")
    except httpx.HTTPError as e:
        raise HTTPException(status_code=502, detail=f"Error al consultar Aerolíneas: {str(e)}")

@router.get("/origins", response_model=List[str])
async def origins():
    try:
        return await svc.list_origins()
    except httpx.TimeoutException:
        raise HTTPException(status_code=502, detail="Aerolíneas no responde (timeout).")
    except httpx.HTTPError as e:
        raise HTTPException(status_code=502, detail=f"Error al consultar Aerolíneas: {str(e)}")

@router.get("/destinations/{origin}", response_model=List[str])
async def destinations(origin: str):
    try:
        return await svc.list_destinations(origin)
    except httpx.TimeoutException:
        raise HTTPException(status_code=502, detail="Aerolíneas no responde (timeout).")
    except httpx.HTTPError as e:
        raise HTTPException(status_code=502, detail=f"Error al consultar Aerolíneas: {str(e)}")

@router.get("/{id_vuelo}", response_model=Dict[str, Any])
async def detalle(
    id_vuelo: int,
    proveedor: Optional[str] = Query(None, description="ID del proveedor (aerolínea) del vuelo")
):
    try:
        v = await svc.detail(id_vuelo, proveedor_id=proveedor)
    except httpx.TimeoutException:
        raise HTTPException(status_code=502, detail="Aerolíneas no responde (timeout).")
    except httpx.HTTPError as e:
        raise HTTPException(status_code=502, detail=f"Error al consultar Aerolíneas: {str(e)}")

    if not v:
        raise HTTPException(status_code=404, detail="Vuelo no disponible")
    return v