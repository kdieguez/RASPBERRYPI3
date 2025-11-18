from datetime import datetime
from typing import List, Optional

from fastapi import APIRouter, Depends
from pydantic import BaseModel

from app.core.database import get_db

router = APIRouter(
    prefix="/api/public/proveedores",
    tags=["proveedores-publicos"],
)


class ProveedorPublico(BaseModel):
    id: str
    nombre: str
    apiUrl: str

    pais: Optional[str] = None
    tipo: Optional[str] = None
    descripcion: Optional[str] = None
    logoUrl: Optional[str] = None

    creadoEn: Optional[datetime] = None


@router.get("", response_model=List[ProveedorPublico])
async def listar_proveedores_publicos(db=Depends(get_db)):

    coll = db["proveedores"]

    cursor = coll.find({"habilitado": True})
    results: list[ProveedorPublico] = []

    async for doc in cursor:
        markup = doc.get("markup") or {}

        proveedor = ProveedorPublico(
            id=str(doc.get("_id") or markup.get("id") or ""),
            nombre=doc.get("nombre")
            or markup.get("nombre")
            or str(doc.get("_id")),
            apiUrl=doc.get("apiUrl") or markup.get("apiUrl") or "",
            pais=doc.get("pais") or markup.get("pais"),
            tipo=doc.get("tipo") or markup.get("tipo"),
            descripcion=doc.get("descripcion") or markup.get("descripcion"),
            logoUrl=doc.get("logoUrl") or markup.get("logoUrl"),
            creadoEn=doc.get("creadoEn"),
        )
        results.append(proveedor)

    return results