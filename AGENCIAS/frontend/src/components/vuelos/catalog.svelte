<script>
  import { onMount } from "svelte";
  import { VuelosAPI, ComprasAPI } from "@/lib/api";
  import { link } from "@/lib/router";

  console.log('[CAT] MONTANDO Catalog.svelte __build=2');

  let origin = "";
  let destination = "";
  let date = "";
  let pax = 1;

  let origins = [];
  let destinations = [];
  let items = [];            // ← ÚNICA fuente de verdad
  let loading = false;
  let error = "";
  let showDebug = false;

  onMount(async () => {
    try { origins = await VuelosAPI.origins(); } catch (e) { error = e?.message || "No se pudieron cargar los orígenes"; }
  });

  async function onOriginChange() {
    destination = "";
    destinations = [];
    if (!origin) return;
    try { destinations = await VuelosAPI.destinations(origin); }
    catch (e) { error = e?.message || "No se pudieron cargar los destinos"; }
  }

  async function search() {
    loading = true; error = "";
    try {
      const res = await VuelosAPI.search({ origin, destination, date, pax });
      console.log('[CAT] resultados len:', Array.isArray(res) ? res.length : 'N/A', res);
      // fuerza nueva referencia para reactividad
      items = (Array.isArray(res) ? res : []).map(x => ({ ...x }));
      // ordena de manera segura
      items.sort((a,b) =>
        (a?.precioDesde ?? Number.MAX_SAFE_INTEGER) - (b?.precioDesde ?? Number.MAX_SAFE_INTEGER)
      );
    } catch (e) {
      console.error('[CAT] error search', e);
      error = e?.message || "Error al buscar vuelos";
      items = [];
    } finally {
      loading = false;
    }
  }

  async function addToCart(v) {
    try {
      await ComprasAPI.addItem({ idVuelo: v.idVuelo, idClase: 1, cantidad: 1, pair: false });
      alert("Agregado al carrito");
    } catch (e) {
      alert(e?.message || "Error agregando al carrito");
    }
  }

  const idOrigin = "fld-origin";
  const idDestination = "fld-destination";
  const idDate = "fld-date";
  const idPax = "fld-pax";
</script>

<div class="wrap">
  <h1>Vuelos</h1>

  <form class="card" on:submit|preventDefault={search}>
    <div class="row">
      <div>
        <label for={idOrigin}>Origen</label>
        <select id={idOrigin} bind:value={origin} on:change={onOriginChange}>
          <option value="">— Selecciona —</option>
          {#each origins as o}<option value={o}>{o}</option>{/each}
        </select>
      </div>

      <div>
        <label for={idDestination}>Destino</label>
        <select id={idDestination} bind:value={destination} disabled={!origin}>
          <option value="">— Selecciona —</option>
          {#each destinations as d}<option value={d}>{d}</option>{/each}
        </select>
      </div>

      <div>
        <label for={idDate}>Fecha salida</label>
        <input id={idDate} type="date" bind:value={date} />
      </div>

      <div>
        <label for={idPax}>Pasajeros</label>
        <input id={idPax} type="number" min="1" max="9" bind:value={pax} />
      </div>

      <div class="actions">
        <button class="btn" type="submit">Buscar</button>
      </div>
    </div>
  </form>

  {#if error}<p class="error">{error}</p>{/if}
  {#if loading}<p>Cargando...</p>{/if}

  <p class="muted">Vuelos encontrados: {items.length}</p>

  <details class="muted" bind:open={showDebug} style="margin:.5rem 0">
    <summary>Ver datos crudos (debug)</summary>
    <pre style="white-space:pre-wrap;overflow:auto;max-height:240px;">{JSON.stringify(items, null, 2)}</pre>
  </details>

  <div class="grid">
    {#each items as v (v.idVuelo ?? v.codigo ?? v)}
      <div class="item">
        <a
          href={`/vuelos/${v.idVuelo}?pax=${pax}`}
          use:link
          style="text-decoration:none; color:inherit;"
        >
          <div class="price">Desde {v.precioDesde?.toLocaleString?.() ?? v.precioDesde ?? '—'}</div>
          <div class="route">{v.origen} → {v.destino}</div>
          <div class="meta">
            <span>{v.fechaSalida ? new Date(v.fechaSalida).toLocaleString() : '—'}</span>
            <span>Proveedor: {v.proveedor ?? '—'}</span>
            {#if v.tieneEscala}<span class="escala">Con escala</span>{/if}
          </div>
        </a>

        <div class="item-actions">
          <button class="btn" on:click={() => addToCart(v)}>Agregar al carrito</button>
          <a class="btn ghost" href="/carrito" use:link>Ver carrito</a>
        </div>
      </div>
    {/each}
  </div>
</div>

<style>
  .wrap { max-width: 1000px; margin: 0 auto; padding: 1rem; }
  h1 { color: #E62727; }
  .muted { opacity:.75 }
  .card { background:#fff; border:1px solid #eee; border-left:4px solid #1E93AB; padding:1rem; border-radius:12px; box-shadow:0 4px 12px rgba(0,0,0,.04);}
  .row { display:grid; grid-template-columns: repeat(5, 1fr); gap:.75rem; align-items:end; }
  label { font-size:.85rem; color:#555; }
  input, select { width:100%; padding:.55rem .6rem; border:1px solid #ddd; border-radius:10px; }
  .actions { display:flex; justify-content:flex-end; }
  .btn { background:#E62727; color:#fff; border:0; padding:.6rem 1rem; border-radius:999px; cursor:pointer; }
  .btn:hover { opacity:.9; }
  .btn.ghost { background: transparent; color:#E62727; border:1px solid #E62727; }
  .error { color:#E62727; margin-top:.5rem; }
  .grid { display:grid; grid-template-columns: repeat(auto-fill, minmax(280px,1fr)); gap:1rem; margin-top:1rem; }
  .item { border:1px solid #eee; border-radius:14px; padding:.9rem; transition: transform .12s ease; background:#fff; }
  .item:hover { transform: translateY(-2px); box-shadow: 0 6px 16px rgba(0,0,0,.06); }
  .price { font-weight:700; color:#1E93AB; }
  .route { font-size:1.15rem; margin:.25rem 0; }
  .meta { display:flex; gap:.6rem; flex-wrap:wrap; font-size:.85rem; color:#666; }
  .escala { background:#1E93AB10; padding:.1rem .4rem; border-radius:6px; color:#1E93AB; }
  .item-actions { margin-top:.6rem; display:flex; gap:.5rem; }
</style>
