from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import httpx

from app.routers import users_admin
from app.core.config import ALLOWED_ORIGINS, APP_NAME, AEROLINEAS_API_URL
from app.core.database import (
    connect_to_mongo,
    close_mongo_connection,
    ensure_indexes,
    get_db,
)
from app.routers.auth import router as auth_router
from app.routers.vuelos import router as vuelos_router
from app.routers.portal import router as portal_router

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
    max_age=600,
)

@app.get("/health")
async def health():
    await get_db()["usuarios"].count_documents({})
    return {"status": "ok"}

@app.get("/health/full")
async def health_full():

    users_count = await get_db()["usuarios"].count_documents({})

    url = f"{AEROLINEAS_API_URL}/api/public/vuelos"
    try:
        async with httpx.AsyncClient(timeout=10, headers={"Accept": "application/json"}) as client:
            r = await client.get(url)
            r.raise_for_status()
            aerolineas_ok = True
    except httpx.TimeoutException:
        raise HTTPException(status_code=502, detail="Aerolíneas no responde (timeout).")
    except httpx.HTTPError as e:
        raise HTTPException(status_code=502, detail=f"Error al consultar Aerolíneas: {str(e)}")

    return {
        "status": "ok",
        "mongo": {"usuarios_count": users_count},
        "aerolineas": {"reachable": aerolineas_ok, "url": url},
    }

# Routers
app.include_router(auth_router)
app.include_router(users_admin.router)
app.include_router(vuelos_router)
app.include_router(portal_router)