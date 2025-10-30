from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.routers.systems import router as systems_router
from app.routers.partnerships import router as partnerships_router  

app = FastAPI(title="Central API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],              
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(systems_router, prefix="/central", tags=["systems"])

app.include_router(partnerships_router, prefix="/central", tags=["partnerships"])

from app.db import ensure_indexes

@app.on_event("startup")
async def _startup():
    await ensure_indexes()

@app.get("/")
def root():
    return {"ok": True, "msg": "Central API"}

@app.get("/health/db")
def health_db():
    return {"ok": True}
