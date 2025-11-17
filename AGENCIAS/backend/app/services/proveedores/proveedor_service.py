"""
Servicio para gestionar autenticación con proveedores (aerolíneas).
Envía credenciales (email + password) en headers para autenticación WebService.
"""
from __future__ import annotations
from typing import Dict, Optional
from app.repositories.compras_repository import get_proveedor

def _sanitize_user_id(user_id: Optional[str]) -> str:
    """
    Aerolíneas requiere X-User-Id numérico (long).
    Si recibimos un ObjectId de Mongo u otra cosa, intentamos usar el fallback.
    """
    import os
    if user_id and user_id.isdigit():
        return user_id
    fallback = os.getenv("AEROLINEAS_FALLBACK_USER_ID", "").strip()
    if fallback and fallback.isdigit():
        return fallback
    raise ValueError(
        "Aerolíneas requiere un X-User-Id numérico. "
        "Define AEROLINEAS_FALLBACK_USER_ID en .env o guarda aeroUserId numérico por usuario."
    )

async def obtener_headers_para_proveedor(
    proveedor_id: str,
    user_id: Optional[str] = None,
    email: Optional[str] = None,
    name: Optional[str] = None
) -> Dict[str, str]:
    """
    Obtiene los headers necesarios para hacer requests a un proveedor.
    Incluye credenciales WebService (email + password) en headers.
    """
    headers = {"Accept": "application/json"}
    
    prov = await get_proveedor(proveedor_id)
    if not prov:
        raise ValueError(f"Proveedor '{proveedor_id}' no encontrado")
    
    if not prov.get("habilitado", True):
        raise ValueError(f"Proveedor '{proveedor_id}' no está habilitado")
    
    ws_email = prov.get("email") or prov.get("usuarioEmpresarial")
    ws_password = prov.get("password")
    
    if ws_email and ws_password:
        headers["X-WebService-Email"] = ws_email
        headers["X-WebService-Password"] = ws_password
    else:
        raise ValueError(
            f"Proveedor '{proveedor_id}' no tiene credenciales WebService configuradas "
            "(email y password requeridos)"
        )
    
    # Agregar headers de usuario si están disponibles
    if user_id:
        headers["X-User-Id"] = _sanitize_user_id(user_id)
    if email:
        headers["X-User-Email"] = email
    if name:
        headers["X-User-Name"] = name
    
    return headers


