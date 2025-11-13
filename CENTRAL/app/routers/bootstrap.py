# app/routers/bootstrap.py
from fastapi import APIRouter, HTTPException, Depends
from ..db import systems
from ..security import require_admin_key

router = APIRouter(prefix="/bootstrap", tags=["bootstrap"])

@router.get("/{sid}", dependencies=[Depends(require_admin_key)])
async def get_bootstrap_once(sid: str):
    """
    Devuelve las credenciales del admin inicial (si existen)
    y elimina la contraseña temporal después de la primera lectura.
    """
    doc = await systems.find_one({"_id": sid})
    if not doc:
        raise HTTPException(404, "Sistema no encontrado")

    boot = (doc.get("bootstrap") or {})
    admin_email = boot.get("admin_email")
    admin_temp_pass = boot.get("admin_temp_pass")

    if not admin_email:
        return {
            "ok": True,
            "admin_email": None,
            "admin_temp_pass": None
        }

    # Limpia la contraseña temporal (solo se muestra una vez)
    await systems.update_one(
        {"_id": sid},
        {"$unset": {"bootstrap.admin_temp_pass": ""}, "$set": {"bootstrap.shown": True}}
    )

    return {
        "ok": True,
        "admin_email": admin_email,
        "admin_temp_pass": admin_temp_pass
    }
