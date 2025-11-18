from __future__ import annotations
from typing import Any, Mapping, Union
from pydantic import BaseModel, AnyUrl
from app.repositories.ui_repository import UIRepository


def _convert_value(v: Any) -> Any:
    if isinstance(v, AnyUrl):
        return str(v)

    if isinstance(v, (list, tuple)):
        return [_convert_value(x) for x in v]

    if isinstance(v, dict):
        return {k: _convert_value(x) for k, x in v.items()}

    return v


def _clean(data: Union[dict, BaseModel]) -> dict:
    if isinstance(data, BaseModel):
        raw = data.model_dump()
    elif isinstance(data, Mapping):
        raw = dict(data)
    else:
        return {}

    cleaned: dict[str, Any] = {}

    for k, v in raw.items():
        if v is None:
            continue 
        cleaned[k] = _convert_value(v)

    return cleaned


class PortalService:

    def __init__(self, db):
        self.repo = UIRepository(db)

    async def get_ui(self) -> dict:
        return await self.repo.get_ui_bundle()

    async def save_header(self, data: Union[dict, BaseModel]) -> None:
        await self.repo.upsert("header", _clean(data))

    async def save_footer(self, data: Union[dict, BaseModel]) -> None:
        await self.repo.upsert("footer", _clean(data))

    async def save_home(self, data: Union[dict, BaseModel]) -> None:
        await self.repo.upsert("home", _clean(data))