from typing import Any, Dict, Optional

COLL = "ui_settings"

class UIRepository:
    def __init__(self, db):
        self.col = db[COLL]

    async def get_by_id(self, _id: str) -> Dict[str, Any]:
        return await self.col.find_one({"_id": _id}) or {}

    async def upsert(self, _id: str, doc: Dict[str, Any]) -> None:
        await self.col.update_one({"_id": _id}, {"$set": doc}, upsert=True)

    async def get_ui_bundle(self) -> Dict[str, Any]:
        header = await self.get_by_id("header")
        footer = await self.get_by_id("footer")
        return {"header": header, "footer": footer}