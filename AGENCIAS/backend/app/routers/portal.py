from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field, HttpUrl
from typing import List, Optional

from app.core.database import get_db
from app.core.auth_deps import get_current_user
from app.services.portal_service import PortalService

router = APIRouter(prefix="/portal", tags=["portal"])


class LegalLink(BaseModel):
    label: str = Field(min_length=1)
    href: str = Field(min_length=1)


class HeaderIn(BaseModel):
    logo_url: Optional[HttpUrl] = None
    title: Optional[str] = None
    show_search: Optional[bool] = True
    show_cart: Optional[bool] = True


class FooterIn(BaseModel):
    logo_url: Optional[HttpUrl] = None
    phone: Optional[str] = None
    address: Optional[str] = None
    legal_links: Optional[List[LegalLink]] = None
    copyright: Optional[str] = None


class StepItem(BaseModel):
    title: str = Field(min_length=1)
    text: str = Field(min_length=1)


class BenefitItem(BaseModel):
    icon: str = Field(min_length=1)
    title: str = Field(min_length=1)
    text: str = Field(min_length=1)


class TouristDestinationItem(BaseModel):
    name: str = Field(min_length=1)        
    imageUrl: str = Field(min_length=1)  


class HomeIn(BaseModel):
    heroTitle: Optional[str] = None
    heroSubtitle: Optional[str] = None
    heroHighlight: Optional[str] = None

    ctaPrimaryLabel: Optional[str] = None
    ctaPrimaryHref: Optional[str] = None

    ctaSecondaryLabel: Optional[str] = None
    ctaSecondaryHref: Optional[str] = None

    steps: Optional[List[StepItem]] = None
    benefits: Optional[List[BenefitItem]] = None

    touristDestinations: Optional[List[TouristDestinationItem]] = None


def require_admin(user):
    def read(field, default=None):
        if isinstance(user, dict):
            return user.get(field, default)
        return getattr(user, field, default)

    rol = read("rol") or read("role")
    is_admin_flag = bool(read("is_admin", False))

    is_admin = False
    if isinstance(rol, str):
        is_admin = rol.strip().lower() == "admin"
    elif isinstance(rol, int):
        from app.core.roles import ADMIN
        is_admin = (rol == ADMIN)

    if not (is_admin or is_admin_flag):
        raise HTTPException(status_code=403, detail="Solo administradores")


@router.get("/ui")
async def get_ui(db=Depends(get_db)):
    svc = PortalService(db)
    return await svc.get_ui()


@router.put("/header")
async def upsert_header(
    data: HeaderIn,
    db=Depends(get_db),
    user=Depends(get_current_user),
):
    require_admin(user)
    svc = PortalService(db)
    await svc.save_header(data.dict())
    return {"ok": True}


@router.put("/footer")
async def upsert_footer(
    data: FooterIn,
    db=Depends(get_db),
    user=Depends(get_current_user),
):
    require_admin(user)
    svc = PortalService(db)
    await svc.save_footer(data.dict())
    return {"ok": True}


@router.put("/home")
async def upsert_home(
    data: HomeIn,
    db=Depends(get_db),
    user=Depends(get_current_user),
):
    require_admin(user)
    svc = PortalService(db)
    await svc.save_home(data.dict())
    return {"ok": True}