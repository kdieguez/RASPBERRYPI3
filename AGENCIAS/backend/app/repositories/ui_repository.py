from typing import Any, Dict

COLL = "ui_settings"


class UIRepository:
    def __init__(self, db):
        self.col = db[COLL]

    async def get_by_id(self, _id: str) -> Dict[str, Any]:
        doc = await self.col.find_one({"_id": _id}) or {}
        if not doc:
          return {}
        doc = dict(doc)
        doc.pop("_id", None)
        return doc

    async def upsert(self, _id: str, doc: Dict[str, Any]) -> None:
        await self.col.update_one(
            {"_id": _id},
            {"$set": doc},
            upsert=True,
        )

    async def get_ui_bundle(self) -> Dict[str, Any]:
        """
        Devuelve todo lo necesario para la UI pública:
        - header
        - footer
        - home (nuevo)
        """
        header = await self.get_by_id("header")
        footer = await self.get_by_id("footer")
        home = await self.get_by_id("home")
        return {
            "header": header,
            "footer": footer,
            "home": home,
        }