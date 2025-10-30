import os
import certifi
from dotenv import load_dotenv
from motor.motor_asyncio import AsyncIOMotorClient

load_dotenv()  

MONGO_URI = os.getenv("MONGO_URI")           
MONGO_DB  = os.getenv("MONGO_DB", "central")

_client: AsyncIOMotorClient | None = None
_db = None

def get_client() -> AsyncIOMotorClient:
    global _client, _db
    if _client is None:
        if not MONGO_URI:
            raise RuntimeError("Falta MONGO_URI en .env")
        _client = AsyncIOMotorClient(
            MONGO_URI,
            tlsCAFile=certifi.where(),
            serverSelectionTimeoutMS=30000,
        )
        _db = _client[MONGO_DB]
    return _client

def get_db():
    global _db
    if _db is None:
        get_client()
    return _db

db = get_db()
systems = db["systems"]
partnerships = db["partnerships"]  

async def ensure_indexes():
    db = get_db()

    await db["systems"].create_index([("type", 1), ("enabled", 1)], name="idx_type_enabled")
    await db["systems"].create_index("name", name="idx_name")
    await db["systems"].create_index("port", name="idx_port")
    await db["systems"].create_index("frontend_port", name="idx_frontend_port")

    await db["partnerships"].create_index(
        [("from_id", 1), ("to_id", 1)],
        unique=True,
        name="uniq_from_to"
    )

