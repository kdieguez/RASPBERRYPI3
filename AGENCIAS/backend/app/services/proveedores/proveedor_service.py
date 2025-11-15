"""
Servicio para gestionar autenticación y tokens JWT con proveedores (aerolíneas).
Cachea tokens JWT para evitar re-autenticación en cada request.
"""
from __future__ import annotations
import httpx
from typing import Dict, Optional
from datetime import datetime, timezone, timedelta
from app.repositories.compras_repository import get_proveedor

# Cache simple de tokens JWT por proveedor
# Estructura: {proveedor_id: {"token": str, "expires_at": datetime}}
_token_cache: Dict[str, Dict[str, any]] = {}

async def obtener_token_para_proveedor(proveedor_id: str) -> Optional[str]:
    """
    Obtiene un token JWT válido para el proveedor especificado.
    Si hay un token cacheado y válido, lo retorna.
    Si no, hace login con las credenciales del proveedor.
    """
    # Verificar cache
    if proveedor_id in _token_cache:
        cached = _token_cache[proveedor_id]
        expires_at = cached.get("expires_at")
        if expires_at and datetime.now(timezone.utc) < expires_at:
            return cached.get("token")
    
    # Obtener configuración del proveedor
    prov = await get_proveedor(proveedor_id)
    if not prov:
        raise ValueError(f"Proveedor '{proveedor_id}' no encontrado")
    
    if not prov.get("habilitado", True):
        raise ValueError(f"Proveedor '{proveedor_id}' no está habilitado")
    
    api_url = prov.get("apiUrl", "").rstrip("/")
    usuario = prov.get("usuarioEmpresarial")
    password = prov.get("password")
    
    if not api_url:
        raise ValueError(f"Proveedor '{proveedor_id}' no tiene API_URL configurado")
    
    # Si no hay credenciales, no podemos autenticarnos
    # En este caso, retornamos None (será tratado como endpoint público)
    if not usuario or not password:
        return None
    
    # Hacer login para obtener token JWT
    try:
        login_url = f"{api_url}/api/auth/login"
        timeout = prov.get("timeout", 20.0)
        
        async with httpx.AsyncClient(timeout=timeout) as client:
            response = await client.post(
                login_url,
                json={"email": usuario, "password": password},
                headers={"Content-Type": "application/json", "Accept": "application/json"}
            )
            response.raise_for_status()
            data = response.json()
            
            token = data.get("access_token")
            if not token:
                raise ValueError("No se recibió token en la respuesta de login")
            
            # Cachear el token (asumimos que expira en 120 minutos por defecto)
            # Podríamos leer el exp del JWT, pero por simplicidad usamos 115 minutos
            expires_at = datetime.now(timezone.utc) + timedelta(minutes=115)
            _token_cache[proveedor_id] = {
                "token": token,
                "expires_at": expires_at
            }
            
            return token
    except httpx.HTTPError as e:
        raise ValueError(f"Error al autenticarse con proveedor '{proveedor_id}': {str(e)}")
    except Exception as e:
        raise ValueError(f"Error al obtener token para proveedor '{proveedor_id}': {str(e)}")

def limpiar_cache_proveedor(proveedor_id: Optional[str] = None):
    """
    Limpia el cache de tokens.
    Si se proporciona proveedor_id, limpia solo ese proveedor.
    Si no, limpia todo el cache.
    """
    global _token_cache
    if proveedor_id:
        _token_cache.pop(proveedor_id, None)
    else:
        _token_cache.clear()

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
    Incluye autenticación JWT si está configurada.
    """
    headers = {"Accept": "application/json"}
    
    # Agregar token JWT si está disponible
    try:
        token = await obtener_token_para_proveedor(proveedor_id)
        if token:
            headers["Authorization"] = f"Bearer {token}"
    except Exception:
        # Si no podemos obtener token, continuamos sin él (endpoint público)
        pass
    
    # Agregar headers de usuario si están disponibles
    if user_id:
        headers["X-User-Id"] = _sanitize_user_id(user_id)
    if email:
        headers["X-User-Email"] = email
    if name:
        headers["X-User-Name"] = name
    
    return headers


