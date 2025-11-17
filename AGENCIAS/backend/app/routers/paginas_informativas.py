from __future__ import annotations

from datetime import datetime
from typing import List

from fastapi import APIRouter, Depends, HTTPException, status
from motor.motor_asyncio import AsyncIOMotorDatabase

from app.core.database import get_db
from app.core.auth_deps import require_roles
from app.models.pagina_informativa import PaginaCreate, PaginaUpdate, PaginaDB

router = APIRouter(
    prefix="/api",
    tags=["paginas_informativas"],
)

COL_NAME = "paginas_informativas"

@router.get("/public/paginas/{slug}", response_model=PaginaDB)
async def get_pagina_publica(
    slug: str,
    db: AsyncIOMotorDatabase = Depends(get_db),
):
    """
    Devuelve una página informativa publicada/habilitada por su slug.
    No requiere autenticación.
    """
    doc = await db[COL_NAME].find_one({"_id": slug, "habilitado": True})

    if not doc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Página no encontrada o deshabilitada",
        )

    return doc

@router.get("/admin/paginas", response_model=List[PaginaDB])
async def listar_paginas_admin(
    db: AsyncIOMotorDatabase = Depends(get_db),
    user=Depends(require_roles("admin")),
):
    """
    Lista todas las páginas informativas (solo admin).
    """
    cursor = db[COL_NAME].find().sort("slug", 1)
    return [doc async for doc in cursor]


@router.get("/admin/paginas/{slug}", response_model=PaginaDB)
async def obtener_pagina_admin(
    slug: str,
    db: AsyncIOMotorDatabase = Depends(get_db),
    user=Depends(require_roles("admin")),
):
    """
    Devuelve una página informativa por su slug (solo admin).
    """
    doc = await db[COL_NAME].find_one({"_id": slug})

    if not doc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Página no encontrada",
        )

    return doc


@router.post(
    "/admin/paginas",
    response_model=PaginaDB,
    status_code=status.HTTP_201_CREATED,
)
async def crear_pagina(
    data: PaginaCreate,
    db: AsyncIOMotorDatabase = Depends(get_db),
    user=Depends(require_roles("admin")),
):
    """
    Crea una nueva página informativa (solo admin).
    El slug se usa como _id en Mongo.
    """
    slug = data.slug

    existing = await db[COL_NAME].find_one({"_id": slug})
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Ya existe una página con ese slug",
        )

    doc = data.model_dump()
    doc["_id"] = slug
    doc["ultima_actualizacion"] = datetime.utcnow()

    await db[COL_NAME].insert_one(doc)
    return doc


@router.put("/admin/paginas/{slug}", response_model=PaginaDB)
async def actualizar_pagina(
    slug: str,
    data: PaginaUpdate,
    db: AsyncIOMotorDatabase = Depends(get_db),
    user=Depends(require_roles("admin")),
):
    """
    Actualiza una página informativa existente (solo admin).
    """
    update_data = {
      k: v
      for k, v in data.model_dump(exclude_unset=True).items()
    }

    if not update_data:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No se recibió ningún dato para actualizar",
        )

    update_data["ultima_actualizacion"] = datetime.utcnow()

    result = await db[COL_NAME].update_one(
        {"_id": slug},
        {"$set": update_data},
    )

    if result.matched_count == 0:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Página no encontrada",
        )

    doc = await db[COL_NAME].find_one({"_id": slug})
    return doc


@router.delete("/admin/paginas/{slug}", status_code=status.HTTP_204_NO_CONTENT)
async def eliminar_pagina(
    slug: str,
    db: AsyncIOMotorDatabase = Depends(get_db),
    user=Depends(require_roles("admin")),
):
    """
    Elimina una página informativa por su slug (solo admin).
    """
    res = await db[COL_NAME].delete_one({"_id": slug})
    if res.deleted_count == 0:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Página no encontrada",
        )
    return None