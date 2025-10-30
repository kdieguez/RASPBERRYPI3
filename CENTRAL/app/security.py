import os
from fastapi import Header, HTTPException

ADMIN_KEY = os.getenv("ADMIN_KEY", "dev-key")

def require_admin_key(x_admin_key: str | None = Header(default=None)):
    """
    Valida la cabecera x-admin-key contra ADMIN_KEY.
    Úsalo con Depends(require_admin_key) en rutas que requieran clave.
    """
    if not x_admin_key or x_admin_key != ADMIN_KEY:
        raise HTTPException(status_code=403, detail="Forbidden")
