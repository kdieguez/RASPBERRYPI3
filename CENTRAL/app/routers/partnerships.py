from fastapi import APIRouter, Depends, HTTPException, Query
from ..db import partnerships as partnerships_col
from ..models import PartnershipIn, PartnershipOut
from ..security import require_admin_key

router = APIRouter(prefix="/partnerships", tags=["partnerships"])

@router.get("", response_model=list[PartnershipOut])
async def list_partnerships(
    from_id: str | None = Query(None),
    to_id: str | None = Query(None),
    active: bool | None = Query(True),
):
    q = {}
    if from_id: q["from_id"] = from_id
    if to_id: q["to_id"] = to_id
    if active is not None: q["active"] = active
    cur = partnerships_col().find(q, {"_id": 0})
    return [doc async for doc in cur]

@router.post("", response_model=PartnershipOut, dependencies=[Depends(require_admin_key)])
async def create_partnership(payload: PartnershipIn):
    if await partnerships_col().find_one({"_id": payload.id}):
        raise HTTPException(409, "ID already exists")

    # índice único (idempotente)
    await partnerships_col().create_index([("from_id", 1), ("to_id", 1)], unique=True)

    doc = payload.model_dump()
    doc["_id"] = doc.pop("id")
    await partnerships_col().insert_one(doc)
    return payload

@router.put("/{pid}", response_model=PartnershipOut, dependencies=[Depends(require_admin_key)])
async def update_partnership(pid: str, payload: PartnershipIn):
    if pid != payload.id:
        raise HTTPException(400, "ID mismatch")
    doc = payload.model_dump()
    doc["_id"] = doc.pop("id")
    res = await partnerships_col().replace_one({"_id": pid}, doc)
    if res.matched_count == 0:
        raise HTTPException(404, "Not found")
    return payload

@router.delete("/{pid}", dependencies=[Depends(require_admin_key)])
async def delete_partnership(pid: str):
    res = await partnerships_col().delete_one({"_id": pid})
    if res.deleted_count == 0:
        raise HTTPException(404, "Not found")
    return {"ok": True}
