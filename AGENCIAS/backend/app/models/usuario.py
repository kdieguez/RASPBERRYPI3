from datetime import datetime
from typing import Literal, Optional

from pydantic import BaseModel, Field, EmailStr, field_validator
from app.core.config import CAPTCHA_DISABLED

ROLE_REGISTRADO = "VISITANTE_REGISTRADO"
ROLE_ADMIN      = "ADMIN"
ROLE_EMPLEADO   = "EMPLEADO"
ROLE_WEBSERVICE = "WEBSERVICE"

ROLES_VALIDOS = {
    ROLE_REGISTRADO,
    ROLE_ADMIN,
    ROLE_EMPLEADO,
    ROLE_WEBSERVICE,
}

class RegistroIn(BaseModel):
    email: EmailStr
    password: str = Field(min_length=8, max_length=128)
    nombres: str = Field(min_length=2, max_length=80)
    apellidos: str = Field(min_length=2, max_length=80)
    edad: int = Field(ge=0, le=120)
    pais_origen: str = Field(min_length=2, max_length=60)
    numero_pasaporte: str = Field(min_length=4, max_length=30)
    captcha_token: Optional[str] = None

    @field_validator("captcha_token")
    @classmethod
    def validar_captcha(cls, v: Optional[str]) -> str:

        if CAPTCHA_DISABLED:
            return "DEV"
        if not v:
            raise ValueError("Captcha inválido o ausente")
        return v

class UsuarioOut(BaseModel):
    id: str
    email: EmailStr
    nombres: str
    apellidos: str
    edad: int
    pais_origen: str
    numero_pasaporte: str
    rol: Literal["VISITANTE_REGISTRADO", "ADMIN", "EMPLEADO", "WEBSERVICE"] = ROLE_REGISTRADO
    activo: bool = True
    creado_en: datetime