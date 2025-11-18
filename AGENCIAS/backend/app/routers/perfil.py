from typing import Any, Dict

from fastapi import APIRouter, Depends, HTTPException, status

from app.core.auth_deps import get_current_user
from app.repositories.usuarios_repository import (
    get_user_by_id,
    update_user_profile,
)

router = APIRouter(
    prefix="/api/v1/perfil",
    tags=["perfil"],
)


def _get_user_id(current_user: Dict[str, Any]) -> str:

    for key in ("id", "id_usuario", "_id", "user_id", "sub"):
        v = current_user.get(key)
        if v:
            return str(v)
    raise HTTPException(
        status_code=status.HTTP_400_BAD_REQUEST,
        detail="Usuario sin id válido en el token",
    )


@router.get("/me")
async def read_my_profile(current_user=Depends(get_current_user)):

    user_id = _get_user_id(current_user)
    doc = await get_user_by_id(user_id)
    if not doc:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")

    return {
        "id": doc.get("id"),
        "email": doc.get("email", ""),
        "nombres": doc.get("nombres", ""),
        "apellidos": doc.get("apellidos", ""),
        "telefono": doc.get("telefono", ""),
        "pais": doc.get("pais", ""),
        "documento": doc.get("documento", ""),
    }


@router.put("/me")
async def update_my_profile(
    payload: Dict[str, Any],
    current_user=Depends(get_current_user),
):

    user_id = _get_user_id(current_user)

    allowed = {"nombres", "apellidos", "telefono", "pais", "documento"}
    updates = {k: v for k, v in payload.items() if k in allowed}

    if not updates:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No hay campos válidos para actualizar",
        )

    doc = await update_user_profile(user_id, updates)
    if not doc:
        raise HTTPException(status_code=404, detail="Usuario no encontrado")

    return {
        "id": doc.get("id"),
        "email": doc.get("email", ""),
        "nombres": doc.get("nombres", ""),
        "apellidos": doc.get("apellidos", ""),
        "telefono": doc.get("telefono", ""),
        "pais": doc.get("pais", ""),
        "documento": doc.get("documento", ""),
    }