from typing import Dict, Any
from app.repositories.ui_repository import UIRepository

class PortalService:
    def __init__(self, db):
        self.repo = UIRepository(db)

    async def get_ui(self) -> Dict[str, Any]:
        return await self.repo.get_ui_bundle()

    async def save_header(self, data: Dict[str, Any]) -> None:
        payload = {k: v for k, v in data.items() if v is not None}
        payload.update({"_id": "header", "type": "header"})
        await self.repo.upsert("header", payload)

    async def save_footer(self, data: Dict[str, Any]) -> None:
        payload = {k: v for k, v in data.items() if v is not None}
        payload.update({"_id": "footer", "type": "footer"})
        await self.repo.upsert("footer", payload)