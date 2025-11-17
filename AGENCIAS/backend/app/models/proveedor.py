from pydantic import BaseModel, Field, HttpUrl
from typing import Optional

class ProveedorIn(BaseModel):
    """Modelo para crear/actualizar un proveedor (aerolínea)"""
    id: str = Field(..., description="ID único del proveedor (ej: AEROLINEA_PRINCIPAL)")
    nombre: str = Field(..., min_length=1, description="Nombre descriptivo de la aerolínea")
    apiUrl: str = Field(..., description="URL completa del backend (ej: http://localhost:8080)")
    email: Optional[str] = Field(None, description="Email del usuario webservice en esa aerolínea (requerido para autenticación)")
    usuarioEmpresarial: Optional[str] = Field(None, description="[DEPRECATED] Usar 'email' en su lugar. Email del usuario webservice")
    password: Optional[str] = Field(None, description="Password del usuario webservice (requerido para autenticación)")
    timeout: float = Field(20.0, ge=1.0, le=300.0, description="Timeout en segundos para requests")
    markup: dict = Field(default_factory=lambda: {"porcentaje": 0}, description="Configuración de markup")
    habilitado: bool = Field(True, description="Si está habilitado para usar")
    
    class Config:
        populate_by_name = True
        json_schema_extra = {
            "example": {
                "id": "AEROLINEA_PRINCIPAL",
                "nombre": "Aerolínea Principal",
                "apiUrl": "http://localhost:8080",
                "email": "webservice@aerolinea.com",
                "password": "miPassword123",
                "timeout": 20.0,
                "markup": {"porcentaje": 5},
                "habilitado": True
            }
        }

class ProveedorOut(BaseModel):
    """Modelo de salida de un proveedor"""
    id: str
    nombre: str
    apiUrl: str
    email: Optional[str] = None
    usuarioEmpresarial: Optional[str] = None 
    password: Optional[str] = None  
    timeout: float
    markup: dict
    habilitado: bool
    creadoEn: Optional[str] = None
    actualizadoEn: Optional[str] = None
    
    class Config:
        populate_by_name = True

class ProveedorUpdateIn(BaseModel):
    """Modelo para actualizar un proveedor (todos los campos opcionales)"""
    nombre: Optional[str] = None
    apiUrl: Optional[str] = None
    email: Optional[str] = None
    usuarioEmpresarial: Optional[str] = None  
    password: Optional[str] = None
    timeout: Optional[float] = Field(None, ge=1.0, le=300.0)
    markup: Optional[dict] = None
    habilitado: Optional[bool] = None


