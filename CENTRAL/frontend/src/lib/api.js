import axios from "axios";

const API_BASE  = import.meta.env.VITE_API_BASE || "http://localhost:8000";
const ADMIN_KEY = import.meta.env.VITE_ADMIN_KEY || "";

const api = axios.create({
  baseURL: API_BASE,
  headers: {
    "Content-Type": "application/json",
    "x-admin-key": ADMIN_KEY,
  },
});

export async function listSystems(params = {}) {
  const res = await api.get("/central/systems", { params });
  return res.data;
}

export async function createSystem(body) {
  const res = await api.post("/central/systems", body);
  return res.data;
}

export async function deleteSystem(id) {
  const res = await api.delete(`/central/systems/${id}`);
  return res.data;
}

const LAUNCHER     = import.meta.env.VITE_LAUNCHER_BASE || "http://127.0.0.1:7777";
const LAUNCHER_KEY = import.meta.env.VITE_LAUNCHER_KEY || "";

function stripSlash(u = "") {
  const s = String(u || "").trim();
  return s.replace(/\/+$/, "");
}

function portFromUrl(u = "") {
  try {
    const x = new URL(u);
    return x.port ? parseInt(x.port, 10) : (x.protocol === "https:" ? 443 : 80);
  } catch {
    return null;
  }
}

export async function listLaunched() {
  const res = await axios.get(`${LAUNCHER}/list`, {
    headers: { "x-launcher-key": LAUNCHER_KEY },
  });
  return res.data; 
}

export async function spawnAirline(sys) {
  const payload = {
    airline_id: sys.id,
    airline_name: sys.name,
    
    jar_path: import.meta.env.VITE_AIRLINE_JAR_PATH || undefined,
    central_base: API_BASE,
    central_admin_key: ADMIN_KEY,
    port: sys.port || undefined, 
  };
  const res = await axios.post(`${LAUNCHER}/spawn/airline`, payload, {
    headers: { "x-launcher-key": LAUNCHER_KEY },
  });
  return res.data;
}

export async function stopAirline(id) {
  const res = await axios.post(
    `${LAUNCHER}/stop/${id}`,
    {},
    { headers: { "x-launcher-key": LAUNCHER_KEY } }
  );
  return res.data;
}

export async function listFrontends() {
  const res = await axios.get(`${LAUNCHER}/list_frontends`, {
    headers: { "x-launcher-key": LAUNCHER_KEY },
  });
  return res.data; 
}

export async function spawnFrontend(sys, opts = {}) {
  const appId = `${sys.id}-ui`;
  const key = `front_projdir:${appId}`;

  
  let backendPort;
  try {
    const launched = await listLaunched();
    backendPort = launched?.[sys.id]?.port;
  } catch {
    backendPort = undefined;
  }
  if (!backendPort) {
    
    backendPort = sys.port || portFromUrl(sys.base_url) || 8080;
  }

  
  let projectDir = opts.projectDir || localStorage.getItem(key);
  if (!projectDir) {
    projectDir = prompt(
      `Carpeta del proyecto FRONT de "${sys.name}" (donde está package.json):`,
      ""
    );
    if (!projectDir) throw new Error("Proyecto no especificado");
    localStorage.setItem(key, projectDir);
  }

  
  const apiForThisFront = `http://localhost:${backendPort}`;
  const payload = {
    app_id: appId,
    app_name: sys.name,
    project_dir: projectDir,
    package_manager: (import.meta.env.VITE_FRONT_PM || "npm"),   
    script: (import.meta.env.VITE_FRONT_SCRIPT || "dev"),        
    is_vite: (import.meta.env.VITE_FRONT_IS_VITE ?? "true") !== "false",
    port: sys.frontend_port || undefined, 
    env: {
      VITE_API_URL:  stripSlash(apiForThisFront),
      VITE_API_BASE: stripSlash(apiForThisFront),
    },
  };

  const res = await axios.post(`${LAUNCHER}/spawn/frontend`, payload, {
    headers: { "x-launcher-key": LAUNCHER_KEY },
  });
  return res.data; 
}

export async function stopFrontend(sys) {
  const appId = `${sys.id}-ui`;
  const res = await axios.post(
    `${LAUNCHER}/stop/frontend/${appId}`,
    {},
    { headers: { "x-launcher-key": LAUNCHER_KEY } }
  );
  return res.data;
}
