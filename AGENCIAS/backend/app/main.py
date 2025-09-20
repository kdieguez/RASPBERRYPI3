from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.routers import users_admin
from app.core.config import ALLOWED_ORIGINS, APP_NAME
from app.core.database import (
    connect_to_mongo,
    close_mongo_connection,
    ensure_indexes,
    get_db,
)
from app.routers.auth import router as auth_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    await connect_to_mongo()
    await ensure_indexes()
    yield
    await close_mongo_connection()


app = FastAPI(title=APP_NAME, version="1.0.0", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=ALLOWED_ORIGINS, 
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/health")
async def health():
    await get_db()["usuarios"].count_documents({})
    return {"status": "ok"}

app.include_router(auth_router)
app.include_router(users_admin.router)