<script>
  import { onMount } from "svelte";
  import { VuelosAPI, ComprasAPI } from "../../lib/api";
  import { path, match, getQuery, onNavigate, navigate } from "../../lib/router";

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

  function toDate(any) {
    if (any === null || any === undefined) return null;
    if (any instanceof Date) return isNaN(any) ? null : any;
    if (typeof any === "number") {
      const ms = any > 1e12 ? any : any * 1000;
      return new Date(ms);
    }
    if (typeof any === "string") {
      const s = any.trim().replace(" ", "T");
      const d = new Date(s);
      if (!isNaN(d)) return d;
    }
    return null;
  }

  function fmtDate(d) {
    if (!d) return "";
    return new Intl.DateTimeFormat("es-GT", {
      year: "numeric",
      month: "short",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit"
    }).format(d);
  }

  async function load(p) {
    loading = true; error = "";
    const id = parseFromPath(p);
    if (!id) { error = "Ruta inválida"; loading = false; return; }
    try {
      const v = await VuelosAPI.detail(id, pax);
      const salida = toDate(v.salida ?? v.fechaSalida);
      const llegada = toDate(v.llegada ?? v.fechaLlegada);
      flight = {
        ...v,
        salida,
        llegada,
        salidaTxt: fmtDate(salida),
        llegadaTxt: fmtDate(llegada),
        escalas: v.escalas?.map(e => ({
          ciudad: e.ciudad ?? e.city ?? e.aeropuerto ?? "—",
          pais: e.pais ?? e.country ?? "",
          llegadaTxt: fmtDate(toDate(e.llegada ?? e.arrival)),
          salidaTxt: fmtDate(toDate(e.salida ?? e.departure))
        })) ?? [],
        clases: v.clases?.map(c => ({
          idClase: c.idClase ?? c.id,
          nombre: c.nombre ?? `Clase #${c.idClase ?? c.id}`,
          precio: c.precio ?? c.precioBase ?? c.tarifa ?? 0,
          disponible: c.disponible ?? c.cupo ?? c.asientosDisponibles ?? 0
        })) ?? []
      };
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

  async function continuar(c) {
    try {
      await ComprasAPI.addItem({
        idVuelo: flight.id,
        idClase: c.idClase,
        cantidad: pax,
        pair: false
      });
      navigate("/carrito");
    } catch (e) {
      alert(e?.message || "No se pudo agregar al carrito");
    }
  }
</script>

{#if loading}
  <div class="loading">Cargando vuelo...</div>
{:else if error}
  <p class="error">{error}</p>
{:else if flight}
  <div class="vuelo-detail">
    <div class="header">
      <h1>{flight.origen} → {flight.destino}</h1>
      <p class="fecha">{flight.salidaTxt} — {flight.llegadaTxt}</p>
    </div>

    <div class="section-grid">
      <div class="info-box">
        <h3>Ruta</h3>
        <p><b>Origen:</b> {flight.origen}</p>
        <p><b>Destino:</b> {flight.destino}</p>
      </div>

      <div class="info-box">
        <h3>Fechas</h3>
        <p><b>Salida:</b> {flight.salidaTxt}</p>
        <p><b>Llegada:</b> {flight.llegadaTxt}</p>
      </div>
    </div>

    {#if flight.escalas?.length}
      <div class="section">
        <h3>Escalas</h3>
        <ul class="escalas">
          {#each flight.escalas as e}
            <li>
              ✈️ <b>{e.ciudad}</b>{e.pais ? ` (${e.pais})` : ''} —
              {e.llegadaTxt} → {e.salidaTxt}
            </li>
          {/each}
        </ul>
      </div>
    {/if}

    <div class="section">
      <h3>Clases y precios</h3>
      <div class="clases-grid">
        {#each flight.clases as c}
          <div class="clase">
            <div class="precio">Q {c.precio.toLocaleString()}</div>
            <div class="nombre">{c.nombre}</div>
            <div class="disp">Disponibles: {c.disponible}</div>
            <button on:click={() => continuar(c)}>Añadir al carrito</button>
          </div>
        {/each}
      </div>
    </div>
  </div>
{/if}

<style>
  .vuelo-detail {
    background: #fff;
    border: 1px solid #eee;
    border-left: 4px solid #E62727;
    padding: 1.5rem;
    border-radius: 16px;
    max-width: 1100px;
    margin: 1rem auto 2rem;
    box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  }

  .header {
    text-align: center;
    margin-bottom: 1rem;
  }
  .header h1 {
    color: #E62727;
    margin-bottom: 0.25rem;
  }
  .fecha {
    color: #666;
    font-size: 0.95rem;
  }

  .section-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit,minmax(260px,1fr));
    gap: 1rem;
    margin-top: 1rem;
  }

  .info-box {
    border: 1px solid #eee;
    border-radius: 10px;
    padding: 1rem;
    background: #fafafa;
  }

  .section {
    margin-top: 1.5rem;
  }
  .section h3 {
    border-bottom: 2px solid #1E93AB;
    padding-bottom: 0.25rem;
    color: #1E93AB;
  }

  .escalas {
    list-style: none;
    padding: 0.5rem 1rem;
  }
  .escalas li {
    margin: 0.25rem 0;
    color: #444;
  }

  .clases-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 1rem;
    margin-top: 0.75rem;
  }

  .clase {
    border: 1px solid #eee;
    border-radius: 12px;
    padding: 1rem;
    text-align: center;
    background: #fff;
    transition: transform 0.15s ease, box-shadow 0.15s ease;
  }
  .clase:hover {
    transform: translateY(-3px);
    box-shadow: 0 3px 10px rgba(0,0,0,0.07);
  }
  .precio {
    font-weight: 700;
    font-size: 1.3rem;
    color: #E62727;
  }
  .nombre {
    margin: 0.2rem 0;
    color: #333;
  }
  .disp {
    color: #666;
    font-size: 0.85rem;
    margin-bottom: 0.4rem;
  }
  button {
    background: #1E93AB;
    color: #fff;
    border: none;
    padding: 0.5rem 1rem;
    border-radius: 10px;
    cursor: pointer;
    font-weight: 600;
  }
  button:hover {
    background: #157a8b;
  }

  .loading {
    text-align: center;
    padding: 2rem;
  }

  .error {
    color: #E62727;
    text-align: center;
    font-weight: 500;
  }
</style>