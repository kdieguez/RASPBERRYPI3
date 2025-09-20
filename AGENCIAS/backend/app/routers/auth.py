from fastapi import APIRouter, HTTPException, status, Header, Depends
from pydantic import BaseModel, EmailStr
from passlib.context import CryptContext
from bson import ObjectId
import httpx, jwt

from app.models.usuario import RegistroIn, UsuarioOut
from app.services.usuarios_service import crear_usuario
from app.core.config import CAPTCHA_DISABLED, CAPTCHA_SECRET
from app.core.database import get_db
from app.core.jwt_utils import create_access_token, decode_token

router = APIRouter(prefix="/api/v1/auth", tags=["auth"])
_pwd = CryptContext(schemes=["bcrypt"], deprecated="auto")


ROLE_CANON = {
    "admin": "ADMIN",
    "empleado": "EMPLEADO",
    "visitante_registrado": "VISITANTE_REGISTRADO",
    "webservice": "WEBSERVICE",
    "ADMIN": "ADMIN",
    "EMPLEADO": "EMPLEADO",
    "VISITANTE_REGISTRADO": "VISITANTE_REGISTRADO",
    "WEBSERVICE": "WEBSERVICE",
}
def canon_role(value: str | None) -> str:
    key = (value or "").strip()
    return ROLE_CANON.get(key.lower(), ROLE_CANON.get(key, "VISITANTE_REGISTRADO"))


async def _verify_captcha(token: str | None) -> bool:
    if CAPTCHA_DISABLED:
        return True
    if not token:
        return False
    try:
        async with httpx.AsyncClient(timeout=7) as client:
            r = await client.post(
                "https://www.google.com/recaptcha/api/siteverify",
                data={"secret": CAPTCHA_SECRET, "response": token},
                headers={"Content-Type": "application/x-www-form-urlencoded"},
            )
            data = r.json()
            return bool(data.get("success"))
    except Exception:
        return False


@router.post("/register", response_model=UsuarioOut, status_code=status.HTTP_201_CREATED)
async def register(payload: RegistroIn):
    if not await _verify_captcha(payload.captcha_token):
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Captcha inválido")
    try:
        created = await crear_usuario(payload)
        data = dict(created) if isinstance(created, dict) else created.dict()
        data["rol"] = canon_role(data.get("rol"))
        return UsuarioOut(**data)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    except Exception:
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="No se pudo crear el usuario.")


class LoginIn(BaseModel):
    email: EmailStr
    password: str

class TokenOut(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UsuarioOut

@router.post("/login", response_model=TokenOut)
async def login(payload: LoginIn):
    d = get_db()
    email_norm = payload.email.strip().lower()

    user = await d["usuarios"].find_one(
        {"email": email_norm},
        {
            "password_hash": 1,
            "_id": 1, "email": 1, "nombres": 1, "apellidos": 1,
            "edad": 1, "pais_origen": 1, "numero_pasaporte": 1,
            "rol": 1, "activo": 1, "creado_en": 1
        }
    )
    if not user or not _pwd.verify(payload.password, user.get("password_hash", "")):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Credenciales inválidas")

    if not user.get("activo", True):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Usuario deshabilitado")

    role_out = canon_role(user.get("rol"))

    public = {
        "id": str(user["_id"]),
        "email": user["email"],
        "nombres": user.get("nombres", ""),
        "apellidos": user.get("apellidos", ""),
        "edad": user.get("edad", 0),
        "pais_origen": user.get("pais_origen", ""),
        "numero_pasaporte": user.get("numero_pasaporte", ""),
        "rol": role_out,          
        "activo": user.get("activo", True),
        "creado_en": user.get("creado_en"),
    }

    token = create_access_token(
        sub=str(user["_id"]),
        extra={"email": user["email"], "rol": role_out} 
    )
    return {"access_token": token, "token_type": "bearer", "user": public}


async def get_current_user(authorization: str = Header(None)):
    if not authorization or not authorization.lower().startswith("bearer "):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="No autorizado")
    token = authorization.split(" ", 1)[1]
    try:
        payload = decode_token(token)
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Token expirado")
    except jwt.InvalidTokenError:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Token inválido")

    uid = payload.get("sub")
    if not uid:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Token inválido")

    d = get_db()
    doc = await d["usuarios"].find_one(
        {"_id": ObjectId(uid)},
        {"password_hash": 0}
    )
    if not doc:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Usuario no encontrado")

    doc["id"] = str(doc.pop("_id"))
    doc["rol"] = canon_role(doc.get("rol"))
    return UsuarioOut(**doc)

@router.get("/me", response_model=UsuarioOut)
async def me(user = Depends(get_current_user)):
    return user