from datetime import datetime
from passlib.context import CryptContext
from pymongo.errors import DuplicateKeyError

from app.models.usuario import RegistroIn, ROLE_REGISTRADO
from app.repositories.usuarios_repository import insert_usuario, find_by_email


_pwd = CryptContext(schemes=["bcrypt"], deprecated="auto")

def _norm(s: str) -> str:
    return s.strip() if isinstance(s, str) else s

async def crear_usuario(payload: RegistroIn) -> dict:
    """Crea un usuario (password hasheada) y devuelve el documento público sin password_hash."""
    email_norm = _norm(payload.email).lower()

    if await find_by_email(email_norm):
        raise ValueError("El correo ya se encuentra registrado.")

    doc = {
        "email": email_norm,
        "password_hash": _pwd.hash(payload.password),
        "nombres": _norm(payload.nombres),
        "apellidos": _norm(payload.apellidos),
        "edad": payload.edad,
        "pais_origen": _norm(payload.pais_origen),
        "numero_pasaporte": _norm(payload.numero_pasaporte),
        "rol": ROLE_REGISTRADO,
        "activo": True,
        "creado_en": datetime.utcnow(),
        "ultimo_login": None,
        "metadatos": {"fuente": "web", "captcha_ok": True},
    }

    try:
        saved = await insert_usuario(doc)
        return saved
    except DuplicateKeyError:
        raise ValueError("El correo ya se encuentra registrado.")