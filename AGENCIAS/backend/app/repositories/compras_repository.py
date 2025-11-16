from __future__ import annotations
from typing import Any, Dict, List, Optional
from bson import ObjectId
from datetime import datetime, timezone
from app.core.database import get_db

COLL_CARTS = "carritos"
COLL_ORDERS = "compras"
COLL_PROV = "proveedores"


def _oid(id_str: str) -> ObjectId:
    return ObjectId(id_str)


def _id_str(doc: Dict[str, Any] | None) -> Dict[str, Any] | None:
    if not doc:
        return doc
    d = dict(doc)
    if "_id" in d:
        d["id"] = str(d.pop("_id"))
    return d


# -------- Proveedores ----------
async def get_proveedor(proveedor_id: str) -> Optional[Dict[str, Any]]:
    d = get_db()
    doc = await d[COLL_PROV].find_one({"_id": proveedor_id})
    return _id_str(doc)

async def list_proveedores(habilitado_only: bool = False) -> List[Dict[str, Any]]:
    d = get_db()
    query = {"habilitado": True} if habilitado_only else {}
    cur = d[COLL_PROV].find(query).sort([("nombre", 1)])
    out: List[Dict[str, Any]] = []
    async for x in cur:
        out.append(_id_str(x))
    return out

async def upsert_proveedor(doc: Dict[str, Any]) -> None:
    d = get_db()
    _id = doc.get("_id")
    if not _id:
        raise ValueError("Proveedor necesita _id (e.g., 'AEROLINEA_1')")
    await d[COLL_PROV].update_one({"_id": _id}, {"$set": doc}, upsert=True)

async def delete_proveedor(proveedor_id: str) -> bool:
    d = get_db()
    res = await d[COLL_PROV].delete_one({"_id": proveedor_id})
    return res.deleted_count > 0


# -------- Carritos -------------
async def ensure_cart(user_id: str) -> Dict[str, Any]:
    d = get_db()
    c = await d[COLL_CARTS].find_one({"idUsuario": user_id})
    if c:
        return _id_str(c)
    now = datetime.now(timezone.utc).isoformat()
    doc = {
        "idUsuario": user_id,
        "items": [],
        "total": 0.0,
        "fechaCreacion": now,
    }
    res = await d[COLL_CARTS].insert_one(doc)
    saved = await d[COLL_CARTS].find_one({"_id": res.inserted_id})
    return _id_str(saved)


async def get_cart(user_id: str) -> Dict[str, Any]:
    d = get_db()
    c = await d[COLL_CARTS].find_one({"idUsuario": user_id})
    if not c:
        return await ensure_cart(user_id)
    return _id_str(c)


async def save_cart(cart: Dict[str, Any]) -> None:
    d = get_db()
    _id = cart.get("id")
    if not _id:
        raise ValueError("Cart sin id")
    cart_doc = dict(cart)
    cart_doc["_id"] = _oid(_id)
    cart_doc.pop("id", None)
    await d[COLL_CARTS].replace_one({"_id": cart_doc["_id"]}, cart_doc, upsert=True)


# -------- Compras --------------
async def insert_compra(doc: Dict[str, Any]) -> Dict[str, Any]:
    d = get_db()
    now = datetime.now(timezone.utc).isoformat()
    doc = {**doc, "creadaEn": now}
    res = await d[COLL_ORDERS].insert_one(doc)
    saved = await d[COLL_ORDERS].find_one({"_id": res.inserted_id})
    return _id_str(saved)


async def list_compras_by_user(user_id: str) -> List[Dict[str, Any]]:
    """
    Historial de compras del usuario (para la vista del cliente).
    """
    d = get_db()
    cur = d[COLL_ORDERS].find({"idUsuario": user_id}).sort([("_id", -1)])
    out: List[Dict[str, Any]] = []
    async for x in cur:
        out.append(_id_str(x))
    return out


async def list_compras_admin() -> List[Dict[str, Any]]:
    """
    Historial de todas las compras (para la vista del administrador).
    """
    d = get_db()
    cur = d[COLL_ORDERS].find({}).sort([("_id", -1)])
    out: List[Dict[str, Any]] = []
    async for x in cur:
        out.append(_id_str(x))
    return out


async def find_compra_detail(user_id: str, compra_id: str) -> Optional[Dict[str, Any]]:
    """
    Detalle de compra restringido al dueño (cliente).
    """
    d = get_db()
    doc = await d[COLL_ORDERS].find_one(
        {"_id": _oid(compra_id), "idUsuario": user_id}
    )
    return _id_str(doc)


async def find_compra_detail_admin(compra_id: str) -> Optional[Dict[str, Any]]:
    """
    Detalle de compra sin filtrar por usuario (para admin).
    """
    d = get_db()
    doc = await d[COLL_ORDERS].find_one({"_id": _oid(compra_id)})
    return _id_str(doc)


async def update_estado_compra(compra_id: str, nuevo_estado: int) -> None:
    d = get_db()
    await d[COLL_ORDERS].update_one(
        {"_id": _oid(compra_id)},
        {"$set": {"idEstado": nuevo_estado}},
    )