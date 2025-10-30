from pydantic import BaseModel, Field, HttpUrl
from typing import Optional, Literal

SystemType = Literal["aerolinea","agencia"]

class SystemIn(BaseModel):
    id: str
    name: str
    type: str                  
    base_url: HttpUrl
    port: Optional[int] = None
    enabled: bool = True

    frontend_enabled: bool = True
    frontend_base: Optional[HttpUrl] = None
    frontend_port: Optional[int] = None

class SystemOut(SystemIn):
    pass

class PartnershipIn(BaseModel):
    id: str
    from_id: str 
    to_id: str   
    active: bool = True

class PartnershipOut(PartnershipIn):
    pass
