from __future__ import annotations

from typing import List, Optional
from decimal import Decimal
from pydantic import BaseModel, Field, field_validator

def _normalize_dt(value):
    """
    Acepta:
      - str: se regresa igual
      - list/tuple: [YYYY, M, D, HH?, mm?, ss?] → 'YYYY-MM-DDTHH:mm:ss'
      - None: None
      - otro: str(value)
    """
    if value is None:
        return None
    if isinstance(value, str):
        return value
    if isinstance(value, (list, tuple)) and len(value) >= 3:
        y, m, d, *rest = value
        hh = rest[0] if len(rest) > 0 else 0
        mm = rest[1] if len(rest) > 1 else 0
        ss = rest[2] if len(rest) > 2 else 0
        return f"{int(y):04d}-{int(m):02d}-{int(d):02d}T{int(hh):02d}:{int(mm):02d}:{int(ss):02d}"
    return str(value)

class AddItemReq(BaseModel):
    idVuelo: int = Field(..., ge=1)
    idClase: int = Field(..., ge=1)
    cantidad: int = Field(1, ge=1)
    proveedor: Optional[str] = Field(None, description="ID del proveedor (aerolínea) del vuelo")


class UpdateQtyReq(BaseModel):
    cantidad: int = Field(..., ge=1)


class PaymentReq(BaseModel):
    class Tarjeta(BaseModel):
        nombre: Optional[str] = None
        numero: str
        expMes: Optional[int] = None
        expAnio: Optional[int] = None
        cvv: str

    class Facturacion(BaseModel):
        direccion: Optional[str] = None
        ciudad: Optional[str] = None
        pais: Optional[str] = None
        zip: Optional[str] = None

    tarjeta: Tarjeta
    facturacion: Facturacion


class CheckoutResp(BaseModel):
    idReserva: str

class CarritoItem(BaseModel):
    idItem: str
    proveedor: str = "AEROLINEAS"
    idVuelo: int
    codigoVuelo: Optional[str] = None
    fechaSalida: Optional[str] = None
    fechaLlegada: Optional[str] = None
    idClase: int
    clase: Optional[str] = None
    cantidad: int
    precioBase: Optional[Decimal] = None
    precioFinal: Optional[Decimal] = None
    subtotal: Optional[Decimal] = None
    ciudadOrigen: Optional[str] = None
    paisOrigen: Optional[str] = None
    ciudadDestino: Optional[str] = None
    paisDestino: Optional[str] = None
    parejaDe: Optional[int] = None

    @field_validator("fechaSalida", "fechaLlegada", mode="before")
    @classmethod
    def _norm_dates(cls, v):
        return _normalize_dt(v)


class CarritoResp(BaseModel):
    idCarrito: str | int
    idUsuario: str | int
    fechaCreacion: Optional[str] = None
    total: Decimal = Decimal("0.00")
    items: List[CarritoItem] = []


class ReservaItem(BaseModel):
    idItem: str
    proveedor: str = "AEROLINEAS"
    idVuelo: int
    codigoVuelo: Optional[str] = None
    fechaSalida: Optional[str] = None
    fechaLlegada: Optional[str] = None
    idClase: int
    clase: Optional[str] = None
    cantidad: int
    precioUnitario: Optional[Decimal] = None
    subtotal: Optional[Decimal] = None
    ciudadOrigen: Optional[str] = None
    paisOrigen: Optional[str] = None
    ciudadDestino: Optional[str] = None
    paisDestino: Optional[str] = None
    regresoCodigo: Optional[str] = None
    regresoFechaSalida: Optional[str] = None
    regresoFechaLlegada: Optional[str] = None
    regresoCiudadOrigen: Optional[str] = None
    regresoPaisOrigen: Optional[str] = None
    regresoCiudadDestino: Optional[str] = None
    regresoPaisDestino: Optional[str] = None

    @field_validator(
        "fechaSalida",
        "fechaLlegada",
        "regresoFechaSalida",
        "regresoFechaLlegada",
        mode="before",
    )
    @classmethod
    def _norm_reserva_dates(cls, v):
        return _normalize_dt(v)


class ReservaListItem(BaseModel):
    idReserva: str
    idUsuario: str | int
    idEstado: int
    total: Decimal
    creadaEn: Optional[str] = None
    codigo: Optional[str] = None


class ReservaDetalle(BaseModel):
    idReserva: str
    idUsuario: str | int
    idEstado: int
    total: Decimal
    creadaEn: Optional[str] = None
    codigo: Optional[str] = None
    items: List[ReservaItem] = []
    compradorNombre: Optional[str] = None
    compradorEmail: Optional[str] = None