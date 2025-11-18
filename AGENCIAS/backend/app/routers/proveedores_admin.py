from datetime import datetime, timezone
from typing import Any, Optional, List

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel

from app.core.database import get_db

router = APIRouter(
    prefix="/api/admin/proveedores",
    tags=["Proveedores admin"],
)


class ProveedorAdminBase(BaseModel):
    nombre: str
    apiUrl: str
    habilitado: bool = True

    pais: Optional[str] = None
    tipo: Optional[str] = None
    descripcion: Optional[str] = None
    logoUrl: Optional[str] = None


class ProveedorAdminUpdate(ProveedorAdminBase):
    """Payload que recibe el PUT."""
    pass


class ProveedorAdminOut(ProveedorAdminBase):
    """Lo que le devolvemos al frontend admin."""
    id: str

    creadoEn: Optional[datetime] = None
    actualizadoEn: Optional[datetime] = None

    usuarioEmpresarial: Optional[str] = None
    timeout: Optional[float] = None
    markup: Optional[dict[str, Any]] = None

def map_doc(doc: dict) -> ProveedorAdminOut:
    """
    Mapea el documento de Mongo a ProveedorAdminOut.
    Usa `markup.nombre` como fallback si aún no tienes `nombre` en raíz.
    """
    markup = doc.get("markup") or {}

    nombre = doc.get("nombre") or markup.get("nombre") or doc.get("_id")
    api_url = doc.get("apiUrl") or markup.get("apiUrl") or ""

    return ProveedorAdminOut(
        id=str(doc.get("_id") or doc.get("id")),
        nombre=nombre,
        apiUrl=api_url,
        pais=doc.get("pais"),
        tipo=doc.get("tipo"),
        descripcion=doc.get("descripcion"),
        logoUrl=doc.get("logoUrl"),
        habilitado=bool(doc.get("habilitado", True)),
        creadoEn=doc.get("creadoEn"),
        actualizadoEn=doc.get("actualizadoEn"),
        usuarioEmpresarial=doc.get("usuarioEmpresarial"),
        timeout=doc.get("timeout"),
        markup=markup or None,
    )

@router.get("", response_model=List[ProveedorAdminOut])
async def list_proveedores(db=Depends(get_db)):
    coll = db["proveedores"]
    res: list[ProveedorAdminOut] = []
    async for doc in coll.find({}):
        res.append(map_doc(doc))
    return res


@router.get("/{proveedor_id}", response_model=ProveedorAdminOut)
async def get_proveedor(proveedor_id: str, db=Depends(get_db)):
    coll = db["proveedores"]
    doc = await coll.find_one({"_id": proveedor_id})
    if not doc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Proveedor no encontrado",
        )
    return map_doc(doc)


@router.put("/{proveedor_id}", response_model=ProveedorAdminOut)
async def update_proveedor(
    proveedor_id: str,
    data: ProveedorAdminUpdate,
    db=Depends(get_db),
):
    coll = db["proveedores"]

    print("\n=== UPDATE PROVEEDOR ===")
    print("proveedor_id (path):", repr(proveedor_id))

    filtro = {"_id": proveedor_id}
    print("filtro que se usará:", filtro)

    now = datetime.now(timezone.utc)

    set_fields = {
        "nombre": data.nombre,
        "apiUrl": data.apiUrl,
        "pais": data.pais,
        "tipo": data.tipo,
        "descripcion": data.descripcion,
        "logoUrl": data.logoUrl,
        "habilitado": data.habilitado,
        "actualizadoEn": now,
    }
    print("set_fields:", set_fields)

    result = await coll.update_one(filtro, {"$set": set_fields})

    print(
        "matched_count:", result.matched_count,
        "modified_count:", result.modified_count,
    )

    if result.matched_count == 0:
        print(">>> NO se encontró proveedor con ese _id para actualizar")
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Proveedor no encontrado al actualizar",
        )

    doc_actualizado = await coll.find_one(filtro)
    print("doc_actualizado:", doc_actualizado)
    print("=== FIN UPDATE PROVEEDOR ===\n")

    return map_doc(doc_actualizado)