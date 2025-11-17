from fastapi import APIRouter, HTTPException, status, Depends, Query
from typing import List, Optional
from datetime import datetime, timezone

from app.models.proveedor import ProveedorIn, ProveedorOut, ProveedorUpdateIn
from app.repositories.compras_repository import (
    get_proveedor,
    list_proveedores,
    upsert_proveedor,
    delete_proveedor
)
from app.core.auth_deps import require_roles
import httpx

router = APIRouter(prefix="/api/admin/proveedores", tags=["proveedores"])

def _proveedor_to_out(doc: dict, incluir_password: bool = False) -> ProveedorOut:
    """Convierte un documento de MongoDB a ProveedorOut"""
    if not doc:
        return None
    # No exponer password por defecto
    if not incluir_password and "password" in doc:
        doc = {**doc, "password": None}
    # Usar 'email' si está disponible, sino 'usuarioEmpresarial' (compatibilidad)
    email = doc.get("email") or doc.get("usuarioEmpresarial")
    return ProveedorOut(
        id=doc.get("_id") or doc.get("id"),
        nombre=doc.get("nombre", ""),
        apiUrl=doc.get("apiUrl", ""),
        email=email,
        usuarioEmpresarial=doc.get("usuarioEmpresarial"),  # Mantener para compatibilidad
        password=doc.get("password") if incluir_password else None,
        timeout=doc.get("timeout", 20.0),
        markup=doc.get("markup", {"porcentaje": 0}),
        habilitado=doc.get("habilitado", True),
        creadoEn=doc.get("creadoEn"),
        actualizadoEn=doc.get("actualizadoEn")
    )

@router.get("", response_model=List[ProveedorOut], summary="Listar todos los proveedores (aerolíneas)")
async def listar_proveedores(
    soloHabilitados: bool = Query(False, alias="soloHabilitados"),
    user = Depends(require_roles("ADMIN"))
):
    """
    Lista todos los proveedores (aerolíneas) configurados.
    Solo usuarios ADMIN pueden acceder.
    """
    try:
        docs = await list_proveedores(habilitado_only=soloHabilitados)
        return [_proveedor_to_out(doc) for doc in docs]
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error al listar proveedores: {str(e)}"
        )

@router.get("/{id}", response_model=ProveedorOut, summary="Obtener un proveedor por ID")
async def obtener_proveedor(
    id: str,
    incluirPassword: bool = Query(False, alias="incluirPassword"),
    user = Depends(require_roles("ADMIN"))
):
    """
    Obtiene un proveedor específico por su ID.
    Solo usuarios ADMIN pueden acceder.
    """
    doc = await get_proveedor(id)
    if not doc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Proveedor '{id}' no encontrado"
        )
    return _proveedor_to_out(doc, incluir_password=incluirPassword)

@router.post("", response_model=ProveedorOut, status_code=status.HTTP_201_CREATED, summary="Crear un nuevo proveedor")
async def crear_proveedor(
    payload: ProveedorIn,
    user = Depends(require_roles("ADMIN"))
):
    """
    Crea un nuevo proveedor (aerolínea).
    Solo usuarios ADMIN pueden acceder.
    """
    try:
        # Verificar si ya existe
        existente = await get_proveedor(payload.id)
        if existente:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail=f"Ya existe un proveedor con ID '{payload.id}'"
            )
        
        now = datetime.now(timezone.utc).isoformat()
        email_val = payload.email or payload.usuarioEmpresarial
        doc = {
            "_id": payload.id,  # MongoDB usa _id, pero el modelo usa id
            "nombre": payload.nombre,
            "apiUrl": payload.apiUrl.rstrip("/"),
            "email": email_val,  
            "usuarioEmpresarial": email_val,  
            "password": payload.password,
            "timeout": payload.timeout,
            "markup": payload.markup,
            "habilitado": payload.habilitado,
            "creadoEn": now,
            "actualizadoEn": now
        }
        
        await upsert_proveedor(doc)
        
        # Retornar el documento creado
        creado = await get_proveedor(payload.id)
        return _proveedor_to_out(creado)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error al crear proveedor: {str(e)}"
        )

@router.put("/{id}", response_model=ProveedorOut, summary="Actualizar un proveedor")
async def actualizar_proveedor(
    id: str,
    payload: ProveedorUpdateIn,
    user = Depends(require_roles("ADMIN"))
):
    """
    Actualiza un proveedor existente.
    Solo usuarios ADMIN pueden acceder.
    Solo se actualizan los campos proporcionados.
    """
    try:
        # Verificar que existe
        existente = await get_proveedor(id)
        if not existente:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Proveedor '{id}' no encontrado"
            )
        
        # Construir documento de actualización solo con campos proporcionados
        update_data = {}
        if payload.nombre is not None:
            update_data["nombre"] = payload.nombre
        if payload.apiUrl is not None:
            update_data["apiUrl"] = payload.apiUrl.rstrip("/")
        if payload.email is not None:
            update_data["email"] = payload.email
            update_data["usuarioEmpresarial"] = payload.email 
        elif payload.usuarioEmpresarial is not None:
            update_data["email"] = payload.usuarioEmpresarial
            update_data["usuarioEmpresarial"] = payload.usuarioEmpresarial
        if payload.password is not None:
            update_data["password"] = payload.password  # TODO: Hashear o encriptar
        if payload.timeout is not None:
            update_data["timeout"] = payload.timeout
        if payload.markup is not None:
            update_data["markup"] = payload.markup
        if payload.habilitado is not None:
            update_data["habilitado"] = payload.habilitado
        
        if not update_data:
            # No hay cambios
            return _proveedor_to_out(existente)
        
        update_data["actualizadoEn"] = datetime.now(timezone.utc).isoformat()
        
        # Actualizar
        doc = {**existente, **update_data}
        doc["_id"] = id  # Asegurar que el _id esté presente
        await upsert_proveedor(doc)
        
        # Retornar el documento actualizado
        actualizado = await get_proveedor(id)
        return _proveedor_to_out(actualizado)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error al actualizar proveedor: {str(e)}"
        )

@router.delete("/{id}", status_code=status.HTTP_204_NO_CONTENT, summary="Eliminar un proveedor")
async def eliminar_proveedor(
    id: str,
    user = Depends(require_roles("ADMIN"))
):
    """
    Elimina un proveedor.
    Solo usuarios ADMIN pueden acceder.
    """
    try:
        eliminado = await delete_proveedor(id)
        if not eliminado:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Proveedor '{id}' no encontrado"
            )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error al eliminar proveedor: {str(e)}"
        )

@router.post("/{id}/test", summary="Probar conexión con un proveedor")
async def probar_conexion(
    id: str,
    user = Depends(require_roles("ADMIN"))
):
    """
    Prueba la conexión con el proveedor haciendo un request al endpoint de health.
    Solo usuarios ADMIN pueden acceder.
    """
    try:
        doc = await get_proveedor(id)
        if not doc:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Proveedor '{id}' no encontrado"
            )
        
        api_url = doc.get("apiUrl", "").rstrip("/")
        if not api_url:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="El proveedor no tiene API_URL configurado"
            )
        
        # Intentar conectar al endpoint de health
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.get(f"{api_url}/health")
                response.raise_for_status()
                return {
                    "ok": True,
                    "mensaje": "Conexión exitosa",
                    "status_code": response.status_code,
                    "url": api_url
                }
        except httpx.TimeoutException:
            return {
                "ok": False,
                "mensaje": "Timeout al conectar con el proveedor",
                "url": api_url
            }
        except httpx.HTTPError as e:
            return {
                "ok": False,
                "mensaje": f"Error HTTP: {str(e)}",
                "url": api_url
            }
        except Exception as e:
            return {
                "ok": False,
                "mensaje": f"Error al conectar: {str(e)}",
                "url": api_url
            }
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Error al probar conexión: {str(e)}"
        )
