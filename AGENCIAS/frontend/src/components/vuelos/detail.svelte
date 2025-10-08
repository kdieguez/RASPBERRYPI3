<script>
  import { onMount } from "svelte";
  import { VuelosAPI } from "../../lib/api";
  import { path, match, getQuery, onNavigate } from "../../lib/router";

  let flight = null;
  let loading = true;
  let error = "";
  let pax = 1;

  function parseFromPath(p) {
    const m = match("/vuelos/:id", p);
    const id = Number(m.ok ? m.params.id : 0);
    const qs = getQuery(p);
    pax = Number(qs.get("pax") || "1");
    return id;
  }

  async function load(p) {
    loading = true; error = "";
    const id = parseFromPath(p);
    if (!id) { error = "Ruta inválida"; loading = false; return; }
    try {
      flight = await VuelosAPI.detail(id, pax);
    } catch (e) {
      error = e.message || "No se pudo cargar el vuelo";
      flight = null;
    } finally {
      loading = false;
    }
  }

  onMount(() => {
    let current;
    const unsubPath = path.subscribe((p) => { current = p; });
    load(current);
    const off = onNavigate(load);
    return () => { off(); unsubPath(); };
  });

  function canReserve(c) {
    if (typeof c?.disponible === "number") return c.disponible >= pax;
    return pax <= 1;
  }
</script>

{#if loading}<p>Cargando...</p>{/if}
{#if error}<p class="error">{error}</p>{/if}

{#if flight}
  <div class="card">
    <h2>{flight.origen} → {flight.destino}</h2>
    <p class="when">{new Date(flight.fechaSalida).toLocaleString()} — {new Date(flight.fechaLlegada).toLocaleString()}</p>

    {#if flight.escalas?.length}
      <h3>Escalas</h3>
      <ul class="escalas">
        {#each flight.escalas as e}
          <li>{e.ciudad}, {e.pais} · {new Date(e.llegada).toLocaleString()} → {new Date(e.salida).toLocaleString()}</li>
        {/each}
      </ul>
    {/if}

    <h3>Clases disponibles</h3>
    <div class="classes">
      {#each flight.clases as c}
        <div class="clase {canReserve(c) ? '' : 'disabled'}">
          <div class="price">{c.precio?.toLocaleString?.() ?? c.precio}</div>
          <div>Clase #{c.idClase}</div>
          {#if typeof c.disponible === "number"}<div>Disponibles: {c.disponible}</div>{/if}
          <button disabled={!canReserve(c)}>Continuar</button>
        </div>
      {/each}
    </div>
  </div>
{/if}

<style>
  .card { background:#fff; border:1px solid #eee; border-left:4px solid #E62727; padding:1rem; border-radius:12px; max-width: 1000px; margin:0 auto; }
  .error { color:#E62727; }
  .when { color:#555; margin:.25rem 0 .75rem; }
  .escalas { margin:.25rem 0 1rem; padding-left:1rem; }
  .classes { display:grid; grid-template-columns: repeat(auto-fill,minmax(220px,1fr)); gap:.75rem; margin-top:.75rem; }
  .clase { border:1px solid #eee; border-radius:12px; padding:.8rem; }
  .clase.disabled { opacity:.55; }
  button { background:#1E93AB; color:#fff; border:0; padding:.5rem .8rem; border-radius:10px; cursor:pointer; }
</style>