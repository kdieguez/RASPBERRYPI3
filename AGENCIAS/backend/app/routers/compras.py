from __future__ import annotations
from fastapi import APIRouter, Depends, HTTPException, Query, Response
from typing import List, Optional
from pydantic import BaseModel

from app.core.auth_deps import get_current_user, require_roles
from app.core.database import get_db
from app.models.compras import (
    AddItemReq,
    UpdateQtyReq,
    PaymentReq,
    CheckoutResp,
    CarritoResp,
    ReservaListItem,
    ReservaDetalle,
)
from app.services import compras_service as svc

router = APIRouter(prefix="/compras", tags=["compras"])


class TopRuta(BaseModel):
    origin: str
    destination: str
    count: int


@router.get("/carrito", response_model=CarritoResp)
async def get_carrito(user=Depends(get_current_user)):
    try:
        return await svc.get_cart(user)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/items", status_code=201)
async def add_item(
    req: AddItemReq,
    pair: bool = Query(False),
    user=Depends(get_current_user),
):
    try:
        return await svc.add_or_increment_item(user, req, incluir_pareja=pair)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.put("/items/{id_item}", status_code=204)
async def update_item(
    id_item: str,
    req: UpdateQtyReq,
    syncPareja: bool = Query(False),
    proveedor: Optional[str] = Query(
        None, description="ID del proveedor (aerolínea) del item"
    ),
    user=Depends(get_current_user),
):
    try:
        await svc.update_quantity(
            user,
            id_item,
            req.cantidad,
            sync_pareja=syncPareja,
            proveedor_id=proveedor,
        )
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.delete("/items/{id_item}", status_code=204)
async def delete_item(
    id_item: str,
    syncPareja: bool = Query(False),
    proveedor: Optional[str] = Query(
        None, description="ID del proveedor (aerolínea) del item"
    ),
    user=Depends(get_current_user),
):
    try:
        await svc.remove_item(
            user,
            id_item,
            sync_pareja=syncPareja,
            proveedor_id=proveedor,
        )
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/checkout", response_model=CheckoutResp)
async def checkout(req: PaymentReq, user=Depends(get_current_user)):
    try:
        return await svc.checkout(user, req)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/reservas", response_model=List[ReservaListItem])
async def list_reservas(user=Depends(get_current_user)):
    try:
        return await svc.list_compras(user)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/reservas/{id_compra}", response_model=ReservaDetalle)
async def get_reserva_detalle(id_compra: str, user=Depends(get_current_user)):
    try:
        return await svc.get_compra_detalle(user, id_compra)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/reservas/{id_compra}/cancelar")
async def cancelar(id_compra: str, user=Depends(get_current_user)):
    try:
        ok = await svc.cancelar_compra(user, id_compra, is_admin=False)
        if not ok:
            raise HTTPException(
                status_code=409,
                detail="La compra no está en estado cancelable",
            )
        return {"status": "ok"}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/reservas/{id_compra}/boleto.pdf")
async def descargar_boleto_pdf(id_compra: str, user=Depends(get_current_user)):
    try:
        pdf_bytes, fname = await svc.get_compra_pdf(user, id_compra)
        return Response(
            content=pdf_bytes,
            media_type="application/pdf",
            headers={
                "Content-Disposition": f'attachment; filename="{fname}.pdf"',
                "Cache-Control": "no-store",
            },
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get("/stats/top-rutas", response_model=List[TopRuta])
async def top_rutas():
    db = get_db()
    coll = db["compras"]

    pipeline = [
        {"$match": {"tipo": "vuelo"}},
        {"$unwind": "$detalle_vuelo.items"},
        {
            "$group": {
                "_id": {
                    "origin": "$detalle_vuelo.items.ciudadOrigen",
                    "destination": "$detalle_vuelo.items.ciudadDestino",
                },
                "count": {"$sum": 1},
            }
        },
        {"$sort": {"count": -1}},
        {"$limit": 5},
        {
            "$project": {
                "_id": 0,
                "origin": {"$ifNull": ["$_id.origin", "—"]},
                "destination": {"$ifNull": ["$_id.destination", "—"]},
                "count": 1,
            }
        },
    ]

    rows = await coll.aggregate(pipeline).to_list(length=5)
    return [
        TopRuta(
            origin=r.get("origin") or "—",
            destination=r.get("destination") or "—",
            count=int(r.get("count", 0)),
        )
        for r in rows
    ]


@router.get(
    "/admin/reservas",
    response_model=List[ReservaListItem],
)
async def list_reservas_admin(user=Depends(require_roles("admin"))):
    try:
        return await svc.list_compras_admin()
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.get(
    "/admin/reservas/{id_compra}",
    response_model=ReservaDetalle,
)
async def get_reserva_detalle_admin(
    id_compra: str,
    user=Depends(require_roles("admin")),
):
    try:
        return await svc.get_compra_detalle_admin(id_compra)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@router.post("/admin/reservas/{id_compra}/cancelar")
async def cancelar_admin(
    id_compra: str,
    user=Depends(require_roles("admin")),
):
    """
    Cancela una compra como administrador.
    """
    try:
        ok = await svc.cancelar_compra(user, id_compra, is_admin=True)
        if not ok:
            raise HTTPException(
                status_code=409,
                detail="La compra no está en estado cancelable",
            )
        return {"status": "ok"}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))