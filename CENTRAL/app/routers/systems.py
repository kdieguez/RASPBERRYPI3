from fastapi import APIRouter, Depends, HTTPException, Query
from fastapi.encoders import jsonable_encoder
from typing import Optional

from ..db import systems as systems_col, partnerships as partnerships_col
from ..models import SystemIn, SystemOut
from ..security import require_admin_key
from app.services.provision_service import provision_oracle_schema

router = APIRouter(prefix="/systems", tags=["systems"])

def _norm_url(u: Optional[str]):
    if not u:
        return u
    u = str(u).strip()
    return u if u.endswith("/") else (u + "/")

@router.get("", response_model=list[SystemOut])
async def list_systems(
    type: Optional[str] = Query(None, description='Filtro: "aerolinea" | "agencia"'),
    enabled: Optional[bool] = Query(True, description="Filtro por enabled"),
    frontend_enabled: Optional[bool] = Query(None, description="Filtro por frontend_enabled"),
):
    q: dict = {}
    if type:
        q["type"] = type
    if enabled is not None:
        q["enabled"] = enabled
    if frontend_enabled is not None:
        q["frontend_enabled"] = frontend_enabled

    cur = systems_col().find(q).sort("name", 1)
    out: list[SystemOut] = []
    async for doc in cur:
        doc = jsonable_encoder(doc)
        doc["id"] = doc.pop("_id")
        out.append(SystemOut(**doc))
    return out

@router.get("/{sid}", response_model=SystemOut)
async def get_system(sid: str):
    doc = await systems_col().find_one({"_id": sid})
    if not doc:
        raise HTTPException(404, "Not found")
    doc = jsonable_encoder(doc)
    doc["id"] = doc.pop("_id")
    return SystemOut(**doc)

@router.post("", response_model=SystemOut, dependencies=[Depends(require_admin_key)])
async def create_system(payload: SystemIn):
    # ID único
    if await systems_col().find_one({"_id": payload.id}):
        raise HTTPException(409, "ID already exists")

    # Si es aerolínea, provisiona Oracle ANTES de guardar
    oracle_info = None
    if payload.type == "aerolinea":
        try:
            oracle_info = provision_oracle_schema(
                tenant_id=payload.id,
                db_user=None,
                db_pass=None,
                db_dsn=None,
            )
        except Exception as e:
            msg = getattr(e, "detail", str(e)) or "Error al crear BD"
            raise HTTPException(500, f"No se pudo crear esquema Oracle: {msg}")

    # Construye documento final
    doc = jsonable_encoder(payload)
    doc["base_url"] = _norm_url(doc.get("base_url"))
    doc["frontend_base"] = _norm_url(doc.get("frontend_base"))
    doc["_id"] = doc.pop("id")

    if oracle_info:
        doc.update({
            "db_user":  oracle_info["db_user"],
            "db_pass":  oracle_info["db_pass"],
            "db_dsn":   oracle_info["db_dsn"],
            "jdbc_url": oracle_info["jdbc_url"],
            "bootstrap": {
                "admin_email":     oracle_info.get("admin_email"),
                "admin_temp_pass": oracle_info.get("admin_temp_pass"),
                "shown": False
            }
        })

    await systems_col().insert_one(doc)
    doc_out = {**doc, "id": doc["_id"]}
    return SystemOut(**doc_out)

@router.put("/{sid}", response_model=SystemOut, dependencies=[Depends(require_admin_key)])
async def update_system(sid: str, payload: SystemIn):
    if sid != payload.id:
        raise HTTPException(400, "ID mismatch")

    doc = jsonable_encoder(payload)
    doc["base_url"] = _norm_url(doc.get("base_url"))
    doc["frontend_base"] = _norm_url(doc.get("frontend_base"))
    doc["_id"] = doc.pop("id")

    res = await systems_col().replace_one({"_id": sid}, doc)
    if res.matched_count == 0:
        raise HTTPException(404, "Not found")

    doc_out = {**doc, "id": doc["_id"]}
    return SystemOut(**doc_out)

@router.delete("/{sid}", dependencies=[Depends(require_admin_key)])
async def delete_system(sid: str):
    # No eliminar si tiene partnerships activos
    has_active_link = await partnerships_col().find_one(
        {"$or": [{"from_id": sid}, {"to_id": sid}], "active": True}
    )
    if has_active_link:
        raise HTTPException(409, "No se puede eliminar: tiene partnerships activos")

    res = await systems_col().delete_one({"_id": sid})
    if res.deleted_count == 0:
        raise HTTPException(404, "No encontrado")
    return {"ok": True}
