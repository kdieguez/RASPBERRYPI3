from typing import Optional, Dict, Any
from bson import ObjectId
from app.core.database import get_db

COLL = "usuarios"


def _normalize_email(email: str) -> str:
    return (email or "").strip().lower()


def _id_str(doc: Dict[str, Any] | None) -> Dict[str, Any] | None:
    if not doc:
        return doc
    d = dict(doc)
    if "_id" in d:
        d["id"] = str(d.pop("_id"))
    return d


def _public(doc: Dict[str, Any] | None) -> Dict[str, Any] | None:
    """Oculta campos sensibles al devolver al caller."""
    if not doc:
        return doc
    d = _id_str(doc)
    d.pop("password_hash", None)
    return d


async def insert_usuario(doc: Dict[str, Any]) -> Dict[str, Any]:
    d = get_db()
    res = await d[COLL].insert_one(doc)
    saved = await d[COLL].find_one(
        {"_id": res.inserted_id},
        {"password_hash": 0}
    )
    return _public(saved)


async def find_by_email(email: str) -> Optional[Dict[str, Any]]:
    d = get_db()
    doc = await d[COLL].find_one(
        {"email": _normalize_email(email)},
        {"password_hash": 0}
    )
    return _public(doc)


async def find_by_id(id_str: str) -> Optional[Dict[str, Any]]:
    d = get_db()
    try:
        _id = ObjectId(id_str)
    except Exception:
        return None
    doc = await d[COLL].find_one({"_id": _id}, {"password_hash": 0})
    return _public(doc)


async def count_usuarios() -> int:
    d = get_db()
    return await d[COLL].count_documents({})



async def get_user_by_id(id_str: str) -> Optional[Dict[str, Any]]:

    return await find_by_id(id_str)


async def update_user_profile(
    id_str: str,
    updates: Dict[str, Any]
) -> Optional[Dict[str, Any]]:
    """
    Actualiza campos de perfil del usuario y devuelve el documento público.
    """
    d = get_db()
    try:
        _id = ObjectId(id_str)
    except Exception:
        return None

    updates = {k: v for k, v in updates.items() if v is not None}
    if not updates:
        return None

    res = await d[COLL].update_one({"_id": _id}, {"$set": updates})
    if res.matched_count == 0:
        return None

    doc = await d[COLL].find_one({"_id": _id}, {"password_hash": 0})
    return _public(doc)