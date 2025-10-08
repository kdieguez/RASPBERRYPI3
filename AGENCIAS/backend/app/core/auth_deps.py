from fastapi import Header, HTTPException, status, Depends
from bson import ObjectId
import jwt

from app.core.jwt_utils import decode_token

try:
    from app.core.database import get_db
except Exception:
    from app.db.mongo import get_db


async def get_current_user(authorization: str = Header(None)):
    if not authorization or not authorization.lower().startswith("bearer "):
        raise HTTPException(status_code=401, detail="No autorizado")

    token = authorization.split(" ", 1)[1]
    try:
        payload = decode_token(token)
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Token expirado")
    except jwt.InvalidTokenError:
        raise HTTPException(status_code=401, detail="Token inválido")

    uid = payload.get("sub")
    if not uid:
        raise HTTPException(status_code=401, detail="Token inválido")

    d = get_db()
    doc = await d["usuarios"].find_one({"_id": ObjectId(uid)}, {"password_hash": 0})
    if not doc:
        raise HTTPException(status_code=401, detail="Usuario no encontrado")

    doc["id"] = str(doc.pop("_id"))
    return doc


def require_roles(*roles):
    allowed = {r.lower() for r in roles}

    async def _dep(user=Depends(get_current_user)):
        rol = (user.get("rol") or "").lower()
        if rol not in allowed:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Permisos insuficientes",
            )
        return user

    return _dep