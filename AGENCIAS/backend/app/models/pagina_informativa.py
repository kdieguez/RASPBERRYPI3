from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel, Field


class Imagen(BaseModel):
    url: str
    alt: str


class Seccion(BaseModel):
    id: str
    titulo: str
    contenido_html: str
    orden: int = 1
    imagenes: List[Imagen] = []


class PaginaBase(BaseModel):
    slug: str
    titulo: str
    descripcion: str
    tipo: str = "informativa"
    habilitado: bool = True
    secciones: List[Seccion] = []


class PaginaCreate(PaginaBase):
    """Datos para crear una página."""
    pass


class PaginaUpdate(BaseModel):
    """Datos que se pueden editar de una página."""
    titulo: Optional[str] = None
    descripcion: Optional[str] = None
    tipo: Optional[str] = None
    habilitado: Optional[bool] = None
    secciones: Optional[List[Seccion]] = None


class PaginaDB(PaginaBase):
    """Cómo viene el documento desde Mongo."""
    id: str = Field(alias="_id")
    ultima_actualizacion: datetime

    class Config:
        populate_by_name = True
        from_attributes = True