from fastapi import APIRouter, Query, HTTPException, status, Depends
from typing import Optional, Any
from pydantic import BaseModel, EmailStr, Field
from passlib.context import CryptContext
from bson import ObjectId
from datetime import datetime, timezone

try:
    from app.core.database import get_db
except Exception:
    from app.db.mongo import get_db

from app.core.auth_deps import require_roles

router = APIRouter(prefix="/api/v1/users", tags=["users"])
_pwd = CryptContext(schemes=["bcrypt"], deprecated="auto")

ROLE_CANON = {
    "admin": "ADMIN",
    "empleado": "EMPLEADO",
    "visitante_registrado": "VISITANTE_REGISTRADO",
    "webservice": "WEBSERVICE",
    "ADMIN": "ADMIN",
    "EMPLEADO": "EMPLEADO",
    "VISITANTE_REGISTRADO": "VISITANTE_REGISTRADO",
    "WEBSERVICE": "WEBSERVICE",
}
def canon_role(value: Optional[str]) -> str:
    key = (value or "").strip()
    return ROLE_CANON.get(key.lower(), ROLE_CANON.get(key, "VISITANTE_REGISTRADO"))

class UserOut(BaseModel):
    id: str
    email: EmailStr
    nombres: str | None = ""
    apellidos: str | None = ""
    edad: int | None = 0
    pais_origen: str | None = ""
    numero_pasaporte: str | None = ""
    rol: str
    activo: bool = True
    creado_en: Any | None = None

class UserCreateIn(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8)
    nombres: str = ""
    apellidos: str = ""
    edad: int | None = 0
    pais_origen: str = ""
    numero_pasaporte: str = ""
    rol: str = "EMPLEADO"
    activo: bool = True

class UserUpdateIn(BaseModel):
    nombres: Optional[str] = None
    apellidos: Optional[str] = None
    edad: Optional[int] = None
    pais_origen: Optional[str] = None
    numero_pasaporte: Optional[str] = None
    rol: Optional[str] = None
    activo: Optional[bool] = None
    password: Optional[str] = Field(default=None, min_length=8)

def _public(u: dict) -> dict:
    return {
        "id": str(u["_id"]),
        "email": u.get("email") or u.get("correo"),
        "nombres": u.get("nombres", ""),
        "apellidos": u.get("apellidos", ""),
        "edad": u.get("edad", 0),
        "pais_origen": u.get("pais_origen") or u.get("pais", ""),
        "numero_pasaporte": u.get("numero_pasaporte", ""),
        "rol": canon_role(u.get("rol")),
        "activo": u.get("activo", True),
        "creado_en": u.get("creado_en"),
    }

@router.get("", response_model=dict, dependencies=[Depends(require_roles("admin","empleado"))])
async def list_users(
    q: str | None = Query(default=None, description="texto libre"),
    role: str | None = Query(default=None),
    activo: bool | None = Query(default=None),
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=10, ge=1, le=100),
):
    d = get_db()
    filt: dict = {}

    if q:
        rx = {"$regex": q, "$options": "i"}
        filt["$or"] = [{"email": rx}, {"correo": rx}, {"nombres": rx}, {"apellidos": rx}]
    if role:
        filt["rol"] = canon_role(role)
    if activo is not None:
        filt["activo"] = bool(activo)

    total = await d["usuarios"].count_documents(filt)
    cursor = (
        d["usuarios"]
        .find(filt, {"password_hash": 0})
        .sort([("_id", -1)])
        .skip((page - 1) * page_size)
        .limit(page_size)
    )
    items = [_public(u) async for u in cursor]
    return {"items": items, "total": total, "page": page, "page_size": page_size}

@router.get("/{user_id}", response_model=UserOut, dependencies=[Depends(require_roles("admin","empleado"))])
async def get_user(user_id: str):
    d = get_db()
    u = await d["usuarios"].find_one({"_id": ObjectId(user_id)}, {"password_hash": 0})
    if not u:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")
    return UserOut(**_public(u))

@router.post("", response_model=UserOut, status_code=201, dependencies=[Depends(require_roles("admin"))])
async def create_user(payload: UserCreateIn):
    d = get_db()
    email_norm = payload.email.strip().lower()
    exists = await d["usuarios"].find_one({"$or":[{"email": email_norm}, {"correo": email_norm}]})
    if exists:
        raise HTTPException(status_code=409, detail="El correo ya existe")

    doc = {
        "email": email_norm,
        "password_hash": _pwd.hash(payload.password),
        "nombres": payload.nombres.strip(),
        "apellidos": payload.apellidos.strip(),
        "edad": int(payload.edad or 0),
        "pais_origen": payload.pais_origen.strip(),
        "numero_pasaporte": payload.numero_pasaporte.strip(),
        "rol": canon_role(payload.rol),
        "activo": bool(payload.activo),
        "creado_en": datetime.now(timezone.utc).isoformat(),
    }
    res = await d["usuarios"].insert_one(doc)
    u = await d["usuarios"].find_one({"_id": res.inserted_id})
    return UserOut(**_public(u))

@router.patch("/{user_id}", response_model=UserOut, dependencies=[Depends(require_roles("admin","empleado"))])
async def update_user(user_id: str, payload: UserUpdateIn):
    d = get_db()
    updates: dict = {}
    for k in ("nombres","apellidos","edad","pais_origen","numero_pasaporte","rol","activo"):
        v = getattr(payload, k)
        if v is not None:
            if k == "rol":
                updates[k] = canon_role(v)
            else:
                updates[k] = v
    if payload.password:
        updates["password_hash"] = _pwd.hash(payload.password)

    if not updates:
        raise HTTPException(status_code=400, detail="Nada que actualizar")

    await d["usuarios"].update_one({"_id": ObjectId(user_id)}, {"$set": updates})
    u = await d["usuarios"].find_one({"_id": ObjectId(user_id)}, {"password_hash":0})
    if not u:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")
    return UserOut(**_public(u))

@router.delete("/{user_id}", status_code=204, dependencies=[Depends(require_roles("admin"))])
async def delete_user(user_id: str):
    d = get_db()
    await d["usuarios"].delete_one({"_id": ObjectId(user_id)})
    return None