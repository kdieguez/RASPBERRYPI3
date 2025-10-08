from typing import Optional, List, Dict, Any
from fastapi import APIRouter, HTTPException, Query
import httpx
import datetime as dt

from app.services.vuelos_service import (
    search_flights,
    get_flight_detail_filtered,
    list_origins,
    list_destinations,
)

router = APIRouter(prefix="/vuelos", tags=["vuelos"])

def _norm(s: Optional[str]) -> Optional[str]:
    if s is None:
        return None
    s2 = s.strip()
    return s2 if s2 else None

def _validate_iso_date(date_str: Optional[str]) -> Optional[str]:

    if date_str is None:
        return None
    try:
        dt.date.fromisoformat(date_str)
        return date_str
    except Exception:
        raise HTTPException(status_code=400, detail="Parámetro 'date' debe ser YYYY-MM-DD")

@router.get("/origins", response_model=List[str])
async def get_origins():
    try:
        return await list_origins()
    except httpx.TimeoutException:
        raise HTTPException(status_code=502, detail="Aerolíneas no responde (timeout).")
    except httpx.HTTPError as e:
        raise HTTPException(status_code=502, detail=f"Error al consultar Aerolíneas: {str(e)}")

@router.get("/destinations", response_model=List[str])
async def get_destinations(
    origin: str = Query(..., description="Nombre exacto del origen")
):
    origin_n = _norm(origin)
    if not origin_n:
        raise HTTPException(status_code=400, detail="El parámetro 'origin' es requerido")

    try:
        return await list_destinations(origin_n)
    except httpx.TimeoutException:
        raise HTTPException(status_code=502, detail="Aerolíneas no responde (timeout).")
    except httpx.HTTPError as e:
        raise HTTPException(status_code=502, detail=f"Error al consultar Aerolíneas: {str(e)}")

@router.get("", response_model=List[Dict[str, Any]])
async def get_catalog(
    origin: Optional[str] = Query(None, description="Filtro por origen"),
    destination: Optional[str] = Query(None, description="Filtro por destino"),
    date: Optional[str] = Query(None, description="Fecha de salida YYYY-MM-DD"),
    pax: int = Query(1, ge=1, le=9, description="Número de pasajeros"),
):
    origin_n = _norm(origin)
    destination_n = _norm(destination)
    date_valid = _validate_iso_date(date)

    try:
        return await search_flights(origin_n, destination_n, date_valid, pax)
    except httpx.TimeoutException:
        raise HTTPException(status_code=502, detail="Aerolíneas no responde (timeout).")
    except httpx.HTTPError as e:
        raise HTTPException(status_code=502, detail=f"Error al consultar Aerolíneas: {str(e)}")

@router.get("/{flight_id}", response_model=Dict[str, Any])
async def get_detail(
    flight_id: int,
    pax: int = Query(1, ge=1, le=9, description="Pasajeros para validar cupo"),
):
    try:
        v = await get_flight_detail_filtered(flight_id, pax)
    except httpx.TimeoutException:
        raise HTTPException(status_code=502, detail="Aerolíneas no responde (timeout).")
    except httpx.HTTPError as e:
        raise HTTPException(status_code=502, detail=f"Error al consultar Aerolíneas: {str(e)}")

    if not v:
        raise HTTPException(status_code=404, detail="Vuelo no disponible")
    return v