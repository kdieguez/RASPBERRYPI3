from motor.motor_asyncio import AsyncIOMotorClient, AsyncIOMotorDatabase
from app.core.config import MONGO_URI, MONGO_DB

client: AsyncIOMotorClient | None = None
db: AsyncIOMotorDatabase | None = None

async def connect_to_mongo() -> None:
    global client, db
    client = AsyncIOMotorClient(
        MONGO_URI,
        serverSelectionTimeoutMS=8000,
        uuidRepresentation="standard"
    )
    db = client[MONGO_DB]

async def close_mongo_connection() -> None:
    global client, db
    if client is not None:
        client.close()
    client = None
    db = None

def get_db() -> AsyncIOMotorDatabase:
    if db is None:
        raise RuntimeError("Mongo no está inicializado. Llama connect_to_mongo() en el startup.")
    return db

async def ensure_indexes() -> None:
    d = get_db()
    await d["usuarios"].create_index("email", unique=True, name="uq_email")