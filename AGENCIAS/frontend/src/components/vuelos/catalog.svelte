<script>
  import { onMount } from "svelte";

  const API = "http://127.0.0.1:8001/api/v1/vuelos";

  let origin = "";
  let destination = "";
  let date = "";
  let pax = 1;

  let origins = [];
  let destinations = [];
  let items = [];
  let loading = false;
  let error = "";

  async function getJSON(url) {
    const r = await fetch(url);
    if (!r.ok) {
      let msg = "";
      try {
        const j = await r.json();
        msg = j.detail || j.message || j.error || "";
      } catch {
        msg = await r.text();
      }
      throw new Error(msg || `HTTP ${r.status}`);
    }
    return await r.json();
  }

  onMount(async () => {
    try {
      origins = await getJSON(`${API}/origins`);
    } catch (e) {
      error = e.message || "No se pudieron cargar los orígenes";
    }
  });

  async function onOriginChange() {
    destination = "";
    destinations = [];
    if (!origin) return;
    try {
      destinations = await getJSON(`${API}/destinations/${encodeURIComponent(origin)}`);
    } catch (e) {
      error = e.message || "No se pudieron cargar los destinos";
    }
  }

  async function buscar() {
    loading = true;
    error = "";
    try {
      const q = new URLSearchParams();
      if (origin) q.set("origin", origin);
      if (destination) q.set("destination", destination);
      if (date) q.set("date", date);
      if (pax) q.set("pax", String(pax));
      items = await getJSON(`${API}?${q.toString()}`);
    } catch (e) {
      error = e.message || "Error al buscar vuelos";
      items = [];
    } finally {
      loading = false;
    }
  }

  function fmtMoney(n) {
    if (n == null) return "-";
    return `Q ${Number(n).toLocaleString("es-GT", { maximumFractionDigits: 2 })}`;
  }

  function viewDetail(id) {
    location.hash = `#/vuelos/${id}`;
  }
</script>

<div class="container">
  <h2>Catálogo de vuelos</h2>

  {#if error}
    <p class="error">{error}</p>
  {/if}

  <div class="filters">
    <select bind:value={origin} on:change={onOriginChange}>
      <option value="">Origen</option>
      {#each origins as o}
        <option value={o}>{o}</option>
      {/each}
    </select>

    <select bind:value={destination} disabled={!origin}>
      <option value="">Destino</option>
      {#each destinations as d}
        <option value={d}>{d}</option>
      {/each}
    </select>

    <input type="date" bind:value={date} />
    <input type="number" min="1" max="9" bind:value={pax} />

    <button class="btn" on:click={buscar} disabled={loading}>
      {loading ? "Buscando..." : "Buscar"}
    </button>
  </div>

  <div class="grid">
    {#each items as it}
      <div class="card">
        <div class="price">Desde {fmtMoney(it.precioDesde)}</div>
        <div class="title">{it.origen} → {it.destino}</div>
        <div class="meta">
          <span>Proveedor: {it.proveedor}</span>
          {#if it.tieneEscala}
            <span class="pill">Con escala</span>
          {/if}
        </div>
        <div class="actions">
          <button class="btn primary" on:click={() => viewDetail(it.idVuelo)}>Ver detalle</button>
        </div>
      </div>
    {/each}
  </div>

  {#if !loading && items.length === 0}
    <p class="hint">Usa los filtros y presiona <b>Buscar</b> para ver resultados.</p>
  {/if}
</div>

<style>
  .container {
    padding: 16px;
  }

  h2 {
    font-size: 1.6rem;
    color: #1a1a1a;
    margin-bottom: 10px;
  }

  .filters {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
    align-items: center;
    margin-bottom: 16px;
  }

  select,
  input {
    padding: 8px;
    border: 1px solid #ddd;
    border-radius: 8px;
  }

  .btn {
    padding: 10px 14px;
    border-radius: 999px;
    border: 1px solid #ddd;
    background: #fff;
    cursor: pointer;
    transition: all 0.2s ease-in-out;
  }

  .btn:hover {
    background: #f6f6f6;
  }

  .btn.primary {
    background: #e51c23;
    color: #fff;
    border-color: #e51c23;
  }

  .btn.primary:hover {
    background: #c5131a;
  }

  .grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 16px;
  }

  .card {
    background: #fff;
    border: 1px solid #eee;
    border-radius: 16px;
    padding: 16px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    transition: transform 0.15s ease;
  }

  .card:hover {
    transform: translateY(-2px);
  }

  .price {
    color: #0a7;
    font-weight: 700;
    font-size: 1.1rem;
    margin-bottom: 4px;
  }

  .title {
    font-size: 1.1rem;
    font-weight: 700;
    margin-bottom: 6px;
  }

  .meta {
    display: flex;
    gap: 8px;
    align-items: center;
    color: #666;
    font-size: 0.9rem;
  }

  .pill {
    background: #e7f5ff;
    color: #0a5;
    border-radius: 999px;
    padding: 2px 8px;
    font-size: 0.8rem;
  }

  .actions {
    display: flex;
    gap: 8px;
    margin-top: 12px;
  }

  .error {
    color: #c00;
    font-weight: bold;
    margin-bottom: 10px;
  }

  .hint {
    color: #555;
    margin-top: 20px;
  }
</style>