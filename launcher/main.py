import os
import socket
import subprocess
import json
import platform
import shutil
from pathlib import Path
from typing import Optional, Literal, Dict, List

from fastapi import FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import requests
import webbrowser

SECRET = os.getenv("LAUNCHER_KEY", "supercalifragilisticoespialidoso")
STATE_FILE = "launcher_state.json"

JAR_DIR_HINT = os.getenv("LAUNCHER_JAR_DIR")      
JAR_NAME_HINT = os.getenv("LAUNCHER_JAR_NAME")    

def free_port(start=8080, tries=200):
    for p in range(start, start + tries):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            try:
                s.bind(("127.0.0.1", p))
                return p
            except OSError:
                pass
    raise RuntimeError("No free port available")

def free_port_front(start=5173, tries=200):
    return free_port(start, tries)

def load_state():
    if os.path.exists(STATE_FILE):
        with open(STATE_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    return {}

def save_state(st):
    with open(STATE_FILE, "w", encoding="utf-8") as f:
        json.dump(st, f, indent=2)

app = FastAPI(title="Local Launcher")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

state = load_state()  
state.setdefault("airlines", {})
state.setdefault("frontends", {})

def _workdir_for_jar(jar_path: Path) -> Path:
    jar_dir = jar_path.parent
    if jar_dir.name in ("target", "build"):
        return jar_dir.parent
    return jar_dir

def _find_jar_candidates() -> List[Path]:
    candidates: List[Path] = []
    
    if JAR_DIR_HINT and JAR_NAME_HINT:
        p = Path(JAR_DIR_HINT).expanduser().resolve() / JAR_NAME_HINT
        if p.exists():
            candidates.append(p)

    if JAR_DIR_HINT and not candidates:
        d = Path(JAR_DIR_HINT).expanduser().resolve()
        for path in d.glob("*.jar"):
            candidates.append(path)
    
    base = Path(__file__).parent.resolve()
    common = [
        base.parent / "backend" / "target",
        base.parent / "aerolineas" / "backend" / "target",
        base.parent / "AEROLINEAS" / "backend" / "target",
    ]
    for d in common:
        if d.exists():
            for path in d.glob("*.jar"):
                candidates.append(path)
    
    unique, seen = [], set()
    for c in candidates:
        s = str(c)
        if c.exists() and s not in seen:
            seen.add(s)
            unique.append(c)
    prioritized = [p for p in unique if "aerolineas" in p.name.lower()]
    rest = [p for p in unique if p not in prioritized]
    return prioritized + rest

def _resolve_jar_path(opt_path: Optional[str]) -> Path:
    if opt_path:
        p = Path(opt_path).expanduser().resolve()
        if not p.exists():
            raise HTTPException(400, f"jar_path no existe: {p}")
        return p
    candidates = _find_jar_candidates()
    if not candidates:
        raise HTTPException(400, "No se encontró ningún .jar. Configura LAUNCHER_JAR_DIR/LAUNCHER_JAR_NAME o envía jar_path.")
    return candidates[0]

class AirlineLaunch(BaseModel):
    airline_id: str
    airline_name: str
    jar_path: Optional[str] = None   
    central_base: str                
    central_admin_key: str
    oracle_url: Optional[str] = None
    oracle_user: Optional[str] = None
    oracle_pass: Optional[str] = None
    port: Optional[int] = None

@app.get("/list")
def list_running(x_launcher_key: str | None = Header(default=None)):
    if x_launcher_key != SECRET:
        raise HTTPException(403, "bad key")
    return state["airlines"]

@app.post("/spawn/airline")
def spawn_airline(payload: AirlineLaunch, x_launcher_key: str | None = Header(default=None)):
    if x_launcher_key != SECRET:
        raise HTTPException(403, "bad key")
    port = payload.port or free_port(8080)
    env = os.environ.copy()
    env["AIRLINE_ID"] = payload.airline_id
    env["AIRLINE_NAME"] = payload.airline_name
    env["CENTRAL_BASE"] = payload.central_base.rstrip("/")
    env["CENTRAL_ADMIN_KEY"] = payload.central_admin_key
    env["PORT"] = str(port)
    if payload.oracle_url:  env["ORACLE_URL"]  = payload.oracle_url
    if payload.oracle_user: env["ORACLE_USER"] = payload.oracle_user
    if payload.oracle_pass: env["ORACLE_PASS"] = payload.oracle_pass

    jar_path = _resolve_jar_path(payload.jar_path)
    workdir = _workdir_for_jar(jar_path)

    proc = subprocess.Popen(
        ["java", "-jar", str(jar_path)],
        env=env,
        cwd=str(workdir),
        creationflags=getattr(subprocess, "CREATE_NEW_PROCESS_GROUP", 0),
    )
    state["airlines"][payload.airline_id] = {"pid": proc.pid, "port": port}
    save_state(state)
    return {"ok": True, "pid": proc.pid, "port": port, "id": payload.airline_id}

@app.post("/stop/{airline_id}")
def stop_airline(airline_id: str, x_launcher_key: str | None = Header(default=None)):
    if x_launcher_key != SECRET:
        raise HTTPException(403, "bad key")
    info = state["airlines"].get(airline_id)
    if not info:
        return {"ok": True, "msg": "not running"}
    try:
        subprocess.run(["taskkill", "/PID", str(info["pid"]), "/F"], check=False)
    finally:
        state["airlines"].pop(airline_id, None)
        save_state(state)
    return {"ok": True}

PkgMgr = Literal["npm", "yarn", "pnpm"]

class FrontendLaunch(BaseModel):
    app_id: str
    app_name: str
    project_dir: str              
    package_manager: PkgMgr = "npm"
    script: str = "dev"           
    is_vite: bool = True
    port: Optional[int] = None
    env: Optional[Dict[str, str]] = None

def _pm_executable(pm: str) -> str:
    """
    Devuelve el ejecutable correcto para el package manager.
    En Windows forzamos *.cmd para evitar el problema con npm.ps1.
    Permite override con variables:
      LAUNCHER_NPM_CMD, LAUNCHER_YARN_CMD, LAUNCHER_PNPM_CMD
    """
    is_win = platform.system().lower().startswith("win")

    if pm == "npm":
        override = os.getenv("LAUNCHER_NPM_CMD")
        if override:
            return override
        if is_win:
            return shutil.which("npm.cmd") or r"C:\Program Files\nodejs\npm.cmd"
        return shutil.which("npm") or "npm"
    if pm == "yarn":
        override = os.getenv("LAUNCHER_YARN_CMD")
        if override:
            return override
        if is_win:
            return shutil.which("yarn.cmd") or "yarn.cmd"
        return shutil.which("yarn") or "yarn"

    if pm == "pnpm":
        override = os.getenv("LAUNCHER_PNPM_CMD")
        if override:
            return override
        if is_win:
            return shutil.which("pnpm.cmd") or "pnpm.cmd"
        return shutil.which("pnpm") or "pnpm"

    raise HTTPException(400, f"package_manager no soportado: {pm}")

def _frontend_cmd(pm_exec: str, script: str, port: int, is_vite: bool) -> List[str]:
    """
    Construye el comando listo para Popen.
    """
    if is_vite:
        
        if pm_exec.endswith(("npm", "npm.cmd")):
            return [pm_exec, "run", script, "--", "--port", str(port)]
        else:
            return [pm_exec, script, "--port", str(port)]
    else:
        
        if pm_exec.endswith(("npm", "npm.cmd")):
            return [pm_exec, "run", script]
        else:
            return [pm_exec, script]

@app.get("/list_frontends")
def list_frontends(x_launcher_key: str | None = Header(default=None)):
    if x_launcher_key != SECRET:
        raise HTTPException(403, "bad key")
    return state["frontends"]

@app.post("/spawn/frontend")
def spawn_frontend(payload: FrontendLaunch, x_launcher_key: str | None = Header(default=None)):
    if x_launcher_key != SECRET:
        raise HTTPException(403, "bad key")

    port = payload.port or free_port_front()
    proj = Path(payload.project_dir).expanduser().resolve()
    pkg = proj / "package.json"
    if not pkg.exists():
        raise HTTPException(400, f"package.json no encontrado en {proj}")

    pm_exec = _pm_executable(payload.package_manager)

    env = os.environ.copy()
    if payload.env:
        env.update({k: str(v) for k, v in payload.env.items()})
    if not payload.is_vite:
        env["PORT"] = str(port)   

    cmd = _frontend_cmd(pm_exec, payload.script, port, payload.is_vite)

    try:
        proc = subprocess.Popen(
            cmd,
            cwd=str(proj),
            env=env,
            creationflags=getattr(subprocess, "CREATE_NEW_PROCESS_GROUP", 0),
        )
    except FileNotFoundError as e:
        raise HTTPException(
            500,
            f"No se pudo ejecutar '{pm_exec}'. Asegúrate de que existe o define "
            f"LAUNCHER_NPM_CMD/LAUNCHER_YARN_CMD/LAUNCHER_PNPM_CMD. Detalle: {e}"
        ) from e
    except Exception as e:
        raise HTTPException(500, f"Fallo al lanzar el frontend: {e}") from e

    state["frontends"][payload.app_id] = {"pid": proc.pid, "port": port}
    save_state(state)
    return {"ok": True, "pid": proc.pid, "port": port, "id": payload.app_id}

@app.post("/stop/frontend/{app_id}")
def stop_frontend(app_id: str, x_launcher_key: str | None = Header(default=None)):
    if x_launcher_key != SECRET:
        raise HTTPException(403, "bad key")
    info = state["frontends"].get(app_id)
    if not info:
        return {"ok": True, "msg": "not running"}
    try:
        subprocess.run(["taskkill", "/PID", str(info["pid"]), "/F"], check=False)
    finally:
        state["frontends"].pop(app_id, None)
        save_state(state)
    return {"ok": True}

@app.post("/spawn/frontend_by_system")
def spawn_frontend_by_system(payload: dict, x_launcher_key: str | None = Header(default=None)):
    if x_launcher_key != SECRET:
        raise HTTPException(403, "bad key")

    system_id = payload.get("system_id")
    central_base = (payload.get("central_base") or "").rstrip("/")
    admin_key = payload.get("central_admin_key") or ""
    if not system_id or not central_base:
        raise HTTPException(400, "Faltan system_id o central_base")

    url = f"{central_base}/central/systems/{system_id}"
    headers = {"x-admin-key": admin_key} if admin_key else {}
    r = requests.get(url, headers=headers, timeout=10)
    if r.status_code != 200:
        raise HTTPException(r.status_code, f"No se pudo leer system {system_id} en Central")
    s = r.json()

    app_id = s["id"]
    frontend_base = s.get("frontend_base")
    frontend_port = s.get("frontend_port")
    frontend_dir = s.get("frontend_dir")
    frontend_pm = (s.get("frontend_pm") or "npm").lower()
    frontend_script = s.get("frontend_script") or "dev"
    extra_env = s.get("frontend_env") or {}
    is_cra = s.get("is_cra") is True
    is_vite = not is_cra

    if frontend_dir:
        proj = Path(frontend_dir).expanduser().resolve()
        if not (proj / "package.json").exists():
            raise HTTPException(400, f"package.json no encontrado en {proj}")
        port = frontend_port or free_port_front()
        env = os.environ.copy()
        env.update({k: str(v) for k, v in extra_env.items()})
        if not is_vite:
            env["PORT"] = str(port)

        pm_exec = _pm_executable(frontend_pm)
        cmd = _frontend_cmd(pm_exec, frontend_script, port, is_vite)

        proc = subprocess.Popen(
            cmd, cwd=str(proj), env=env,
            creationflags=getattr(subprocess, "CREATE_NEW_PROCESS_GROUP", 0),
        )
        state["frontends"][app_id] = {"pid": proc.pid, "port": port}
        save_state(state)
        if frontend_base:
            webbrowser.open(str(frontend_base))
        return {"ok": True, "mode": "devserver", "pid": proc.pid, "port": port, "id": app_id}

    if frontend_base:
        webbrowser.open(str(frontend_base))
        return {"ok": True, "mode": "open-url", "url": frontend_base, "id": app_id}

    raise HTTPException(400, "El sistema no tiene frontend_dir ni frontend_base")
