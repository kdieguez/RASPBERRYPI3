# app/db.py
import os
import certifi
from dotenv import load_dotenv
from motor.motor_asyncio import AsyncIOMotorClient, AsyncIOMotorDatabase
from fastapi import HTTPException

load_dotenv()

MONGO_URI = os.getenv("MONGO_URI")
MONGO_DB  = os.getenv("MONGO_DB", "central")

_client: AsyncIOMotorClient | None = None
_db: AsyncIOMotorDatabase | None = None

async def connect_to_mongo() -> None:
    """Abrir conexión y guardar DB global."""
    global _client, _db
    if _client is not None and _db is not None:
        return
    if not MONGO_URI:
        raise RuntimeError("Falta MONGO_URI en .env")
    _client = AsyncIOMotorClient(
        MONGO_URI,
        tlsCAFile=certifi.where(),
        serverSelectionTimeoutMS=30000,
    )
    # ‘ping’ para validar conexión
    await _client.admin.command("ping")
    _db = _client[MONGO_DB]

async def close_mongo_connection() -> None:
    global _client, _db
    if _client:
        _client.close()
    _client = None
    _db = None

def get_db() -> AsyncIOMotorDatabase:
    if _db is None:
        # Se llama antes de connect_to_mongo()
        raise HTTPException(500, "MongoDB no inicializado (startup)")
    return _db

# Factories de colecciones (no variables globales)
def systems():
    return get_db()["systems"]

def partnerships():
    return get_db()["partnerships"]

async def ensure_indexes() -> None:
    db = get_db()
    await db["systems"].create_index(
        [("type", 1), ("enabled", 1)],
        name="idx_type_enabled",
    )
    await db["systems"].create_index("name", name="idx_name")
    await db["systems"].create_index("port", name="idx_port")
    await db["systems"].create_index("frontend_port", name="idx_frontend_port")

    await db["partnerships"].create_index(
        [("from_id", 1), ("to_id", 1)],
        unique=True,
        name="uniq_from_to",
    )
