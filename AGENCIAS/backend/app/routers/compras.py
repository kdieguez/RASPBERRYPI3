from __future__ import annotations
from fastapi import APIRouter, Depends, HTTPException, Query, Response
from typing import List

from app.core.auth_deps import get_current_user, require_roles
from app.models.compras import (
    AddItemReq, UpdateQtyReq, PaymentReq,
    CheckoutResp, CarritoResp, ReservaListItem, ReservaDetalle
)
from app.services import compras_service as svc

router = APIRouter(prefix="/compras", tags=["compras"])

@router.get("/carrito", response_model=CarritoResp)
async def get_carrito(user=Depends(get_current_user)):
    try:
        return await svc.get_cart(user)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.post("/items", status_code=201)
async def add_item(req: AddItemReq, pair: bool = Query(False), user=Depends(get_current_user)):
    try:
        return await svc.add_or_increment_item(user, req, incluir_pareja=pair)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.put("/items/{id_item}", status_code=204)
async def update_item(id_item: str, req: UpdateQtyReq, syncPareja: bool = Query(False), user=Depends(get_current_user)):
    try:
        await svc.update_quantity(user, id_item, req.cantidad, sync_pareja=syncPareja)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@router.delete("/items/{id_item}", status_code=204)
async def delete_item(id_item: str, syncPareja: bool = Query(False), user=Depends(get_current_user)):
    try:
        await svc.remove_item(user, id_item, sync_pareja=syncPareja)
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
            raise HTTPException(status_code=409, detail="La compra no está en estado cancelable")
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