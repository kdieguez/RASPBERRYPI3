<script>
  import {
    listSystems,
    createSystem,
    deleteSystem,
    spawnAirline,
    stopAirline,
    listLaunched,
    listFrontends,
    spawnFrontend,
    stopFrontend,
  } from "./lib/api";
  import { onMount } from "svelte";

  let systems = [];
  let loading = false;

  let launched = {};   
  let fronts = {};     

  let filterType = "";     
  let filterEnabled = "";  

  let form = {
    id: "",
    name: "",
    type: "aerolinea",
    base_url: "http://localhost:8080/",
    port: 8080,
    enabled: true,
    notify_url: "",
    
    frontend_enabled: true,
    frontend_base: "http://localhost:5174/",
    frontend_port: 5174,
  };

  async function refreshStatus() {
    try { launched = await listLaunched(); } catch {}
    try { fronts   = await listFrontends(); } catch {}
  }

  async function load() {
    loading = true;
    try {
      const params = {};
      if (filterType) params.type = filterType;
      if (filterEnabled) params.enabled = filterEnabled === "true";
      systems = await listSystems(params);
      await refreshStatus();
    } catch (e) {
      console.error(e);
      alert("Error cargando sistemas");
    } finally {
      loading = false;
    }
  }

  function isValidId(id) {
    return /^[a-z0-9]([-a-z0-9_]*[a-z0-9])?$/.test(id || "");
  }

  function normUrl(u) {
    if (!u) return "";
    const s = String(u).trim();
    return s.endsWith("/") ? s : s + "/";
  }
  
  function frontUrl(sys) {
    const appId = `${sys.id}-ui`;
    const running = fronts[appId];
    if (running?.port) return `http://localhost:${running.port}`;
    if (sys.frontend_base) return String(sys.frontend_base).replace(/\/+$/, "");
    if (sys.frontend_port) return `http://localhost:${sys.frontend_port}`;
    return null;
  }

  async function onCreate() {
    if (!isValidId(form.id)) {
      alert("ID inválido: usa minúsculas, números, guiones o _");
      return;
    }
    try {
      const body = {
        ...form,
        base_url: normUrl(form.base_url),
        frontend_base: form.frontend_base ? normUrl(form.frontend_base) : null,
        frontend_port: form.frontend_port || null,
        notify_url: form.notify_url || null,
      };
      await createSystem(body);
      await load();
      alert("Sistema creado");
    } catch (e) {
      console.error(e);
      const msg = e?.response?.data?.detail || e?.message || "Error";
      alert("No se pudo crear: " + msg);
    }
  }

  async function onDelete(id) {
    if (!confirm(`Eliminar ${id}?`)) return;
    try {
      await deleteSystem(id);
      await load();
    } catch (e) {
      console.error(e);
      alert("Error eliminando");
    }
  }

  async function onSpawnBackend(s) {
    try {
      const r = await spawnAirline(s);
      await refreshStatus();
      alert(`Backend OK → PID ${r.pid} puerto ${r.port}`);
    } catch (e) {
      console.error(e);
      const msg = e?.response?.data?.detail || e?.message || "Error";
      alert("Spawn backend falló: " + msg);
    }
  }

  async function onStopBackend(id) {
    try {
      await stopAirline(id);
      await refreshStatus();
    } catch (e) {
      console.error(e);
      alert("Stop backend falló");
    }
  }

  async function onDevFrontend(s) {
    try {
      const r = await spawnFrontend(s);   
      await refreshStatus();
      const url = frontUrl({ ...s, frontend_port: r.port }) || `http://localhost:${r.port}`;
      window.open(url, "_blank");
      alert(`Frontend OK → PID ${r.pid} puerto ${r.port}`);
    } catch (e) {
      console.error(e);
      const msg = e?.response?.data?.detail || e?.message || "Error";
      alert("Dev frontend falló: " + msg);
    }
  }

  async function onStopFrontend(s) {
    try {
      await stopFrontend(s);
      await refreshStatus();
    } catch (e) {
      console.error(e);
      alert("Stop frontend falló");
    }
  }

  onMount(load);
</script>

<style>
  :root {
    --primary: #E62727;
    --secondary: #1E93AB;
    --bg: #fafafa;
    --card: #fff;
    --text: #222;
    --muted: #777;
    --border: #e8e8e8;
  }
  * { box-sizing: border-box; }
  body { margin:0; font-family: system-ui,-apple-system,Segoe UI,Roboto,sans-serif; background: var(--bg); color: var(--text); }
  header {
    display:flex; align-items:center; justify-content:space-between;
    padding: 16px 24px; background: var(--card); border-bottom: 1px solid var(--border);
    position: sticky; top:0; z-index: 10;
  }
  .brand { font-weight: 700; letter-spacing: 0.3px; }
  .dot { width:10px; height:10px; border-radius: 999px; display:inline-block; margin-right:8px; background: var(--primary); }
  .container { max-width: 1100px; margin: 24px auto; padding: 0 16px; }

  .grid { display:grid; grid-template-columns: 420px 1fr; gap: 24px; }
  @media (max-width: 900px) { .grid { grid-template-columns: 1fr; } }

  .card { background: var(--card); border:1px solid var(--border); border-radius: 14px; padding:16px; }
  h2 { margin: 0 0 12px; font-size: 18px; }
  label { font-size: 12px; color: var(--muted); display:block; margin-bottom:6px; }
  input, select, a.btn-ghost { width: 100%; padding:10px 12px; border:1px solid var(--border); border-radius:10px; outline: none; text-decoration:none; display:inline-block; text-align:center; }
  .row { display:grid; grid-template-columns: 1fr 1fr; gap: 12px; }
  .actions { display:flex; gap:10px; margin-top: 12px; }
  button { border:0; border-radius: 10px; padding:10px 14px; cursor:pointer; font-weight:600; }
  .btn-primary { background: var(--primary); color: #fff; }
  .btn-secondary { background: var(--secondary); color: #fff; }
  .btn-ghost { background: #fff; border:1px solid var(--border); }
  .tag { background:#f2f2f2; padding:2px 8px; border-radius: 999px; font-size: 12px; }
  table { width:100%; border-collapse: collapse; }
  th, td { padding: 10px 8px; border-bottom:1px solid var(--border); vertical-align: middle; }
  th { text-align:left; color: var(--muted); font-weight:600; font-size: 12px; }
  small.mono { font-family: ui-monospace,SFMono-Regular,Menlo,Consolas,monospace; color:#555; }
  td .btn-ghost { width:auto; padding:6px 10px; border-radius:8px; }
  .pill { background:#f5f5f5; border:1px solid var(--border); padding:3px 8px; border-radius:999px; font-size:12px; }
</style>

<header>
  <div class="brand"><span class="dot"></span>Central · Sistemas</div>
  <div class="tag">{loading ? 'Cargando…' : `Total: ${systems.length}`}</div>
</header>

<div class="container grid">
  <!-- Crear sistema -->
  <div class="card">
    <h2>Nuevo sistema</h2>

    <div class="row">
      <div>
        <label>ID (slug único)</label>
        <input bind:value={form.id} placeholder="guatefly" />
      </div>
      <div>
        <label>Nombre</label>
        <input bind:value={form.name} placeholder="GuateFly" />
      </div>
    </div>

    <div class="row" style="margin-top:10px">
      <div>
        <label>Tipo</label>
        <select bind:value={form.type}>
          <option value="aerolinea">aerolínea</option>
          <option value="agencia">agencia</option>
        </select>
      </div>
      <div>
        <label>Habilitado</label>
        <select bind:value={form.enabled}>
          <option value={true}>sí</option>
          <option value={false}>no</option>
        </select>
      </div>
    </div>

    <div class="row" style="margin-top:10px">
      <div>
        <label>Base URL</label>
        <input bind:value={form.base_url} placeholder="http://localhost:8080/" />
      </div>
      <div>
        <label>Puerto (opcional)</label>
        <input type="number" bind:value={form.port} />
      </div>
    </div>

    <div class="row" style="margin-top:10px">
      <div>
        <label>Frontend habilitado</label>
        <select bind:value={form.frontend_enabled}>
          <option value={true}>sí</option>
          <option value={false}>no</option>
        </select>
      </div>
      <div>
        <label>Front URL (opcional)</label>
        <input bind:value={form.frontend_base} placeholder="http://localhost:5174/" />
      </div>
    </div>

    <div class="row" style="margin-top:10px">
      <div>
        <label>Front puerto (opcional)</label>
        <input type="number" bind:value={form.frontend_port} />
      </div>
      <div></div>
    </div>

    <div style="margin-top:10px">
      <label>Notify URL (opcional, agencias)</label>
      <input bind:value={form.notify_url} placeholder="http://localhost:9000/notify" />
    </div>

    <div class="actions">
      <button class="btn-primary" on:click|preventDefault={onCreate}>Guardar</button>
      <button class="btn-ghost" on:click={load}>Refrescar</button>
    </div>
  </div>

  <!-- Listado -->
  <div class="card">
    <h2>Listado</h2>

    <div class="row" style="margin-bottom: 10px">
      <div>
        <label>Filtrar por tipo</label>
        <select bind:value={filterType} on:change={load}>
          <option value="">(todos)</option>
          <option value="aerolinea">aerolínea</option>
          <option value="agencia">agencia</option>
        </select>
      </div>
      <div>
        <label>Habilitado</label>
        <select bind:value={filterEnabled} on:change={load}>
          <option value="">(todos)</option>
          <option value="true">sí</option>
          <option value="false">no</option>
        </select>
      </div>
    </div>

    <table>
      <thead>
        <tr>
          <th>Id</th><th>Nombre</th><th>Tipo</th><th>Base</th><th>Puerto</th>
          <th>Enabled</th><th>Backend</th><th>Front</th><th></th>
        </tr>
      </thead>
      <tbody>
        {#each systems as s}
          {#key s.id}
          <tr>
            <td>{s.id}</td>
            <td>{s.name}</td>
            <td>{s.type}</td>
            <td>{s.base_url}</td>
            <td>{s.port ?? "-"}</td>
            <td>{s.enabled ? "sí" : "no"}</td>

            <!-- Estado backend -->
            <td>
              {#if launched[s.id]}
                <span class="pill">PID {launched[s.id].pid} · {launched[s.id].port}</span>
              {:else}
                <small class="mono" style="color:#999">apagado</small>
              {/if}
            </td>

            <!-- Frontend -->
            <td>
              {#if s.frontend_enabled}
                <div style="display:flex; gap:6px; align-items:center; flex-wrap:wrap">
                  <button class="btn-ghost" on:click={() => onDevFrontend(s)}>Dev</button>
                  <button class="btn-ghost" on:click={() => onStopFrontend(s)}>Stop</button>
                  {#if frontUrl(s)}
                    <a class="btn-ghost" href={frontUrl(s)} target="_blank" rel="noreferrer">Abrir</a>
                  {:else}
                    <small class="mono" style="color:#999">sin URL</small>
                  {/if}
                </div>
              {:else}
                <small class="mono" style="color:#999">deshabilitado</small>
              {/if}
            </td>

            <td style="display:flex; gap:6px">
              {#if s.type === "aerolinea"}
                <button class="btn-secondary" on:click={() => onSpawnBackend(s)}>Spawn</button>
                <button class="btn-ghost" on:click={() => onStopBackend(s.id)}>Stop</button>
              {/if}
              <button class="btn-ghost" on:click={() => onDelete(s.id)}>Eliminar</button>
            </td>
          </tr>
          {/key}
        {/each}
      </tbody>
    </table>
  </div>
</div>
