# app/main.py
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

# Conexión Mongo y utilidades
from app.db import connect_to_mongo, ensure_indexes, close_mongo_connection

# Routers
from app.routers.systems import router as systems_router
from app.routers.partnerships import router as partnerships_router
# Si tienes el router de bootstrap, descomenta la siguiente línea:
# from app.routers.bootstrap import router as bootstrap_router

@asynccontextmanager
async def lifespan(app: FastAPI):
    # 1) Conectar a Mongo
    await connect_to_mongo()
    # 2) Crear índices
    await ensure_indexes()
    yield
    # 3) Cerrar conexión
    await close_mongo_connection()

app = FastAPI(title="Central API", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Routers
app.include_router(systems_router,      prefix="/central", tags=["systems"])
app.include_router(partnerships_router, prefix="/central", tags=["partnerships"])
# app.include_router(bootstrap_router,  prefix="/central", tags=["bootstrap"])

# Health
@app.get("/")
def root():
    return {"ok": True, "msg": "Central API"}

@app.get("/health")
async def health():
    return {"ok": True}
