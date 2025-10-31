<script>
  import { onMount } from "svelte";

  const API_BASE    = "http://127.0.0.1:8001";
  const API_VUELOS  = `${API_BASE}/api/v1/vuelos`;
  const API_COMPRAS = `${API_BASE}/compras`;

  export let id = null;
  let vuelo = null;
  let loading = true;
  let error = "";
  let okMsg = "";

  let adding = {};

  function idFromPath() {
    const h = location.hash || location.pathname;
    const m = h.match(/\/vuelos\/(\d+)/);
    return m ? Number(m[1]) : null;
  }

  async function getJSON(url) {
    const r = await fetch(url);
    if (!r.ok) throw new Error(await r.text());
    return await r.json();
  }

  function fmtDT(s) {
    if (!s) return "-";
    try { return new Date(s).toLocaleString(); } catch { return s; }
  }
  function money(n) {
    return `Q ${Number(n).toLocaleString("es-GT", { maximumFractionDigits: 2 })}`;
  }

  function getToken() {
    const raw = localStorage.getItem("auth") || localStorage.getItem("token");
    if (!raw) return null;
    try {
      const obj = JSON.parse(raw);
      return obj?.token ?? obj?.access_token ?? raw;
    } catch {
      return raw;
    }
  }

  async function addToCart(c) {
    error = "";
    okMsg = "";
    adding[c.idClase] = true;

    try {
      const token = getToken();
      if (!token) throw new Error("Debes iniciar sesión para añadir al carrito.");

      const body = {
        idVuelo: vuelo.idVuelo,
        idClase: c.idClase,
        cantidad: 1
      };

      const r = await fetch(`${API_COMPRAS}/items?pair=false`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${token}`
        },
        body: JSON.stringify(body)
      });

      if (!r.ok) {
        const txt = await r.text();
        throw new Error(txt || `Error ${r.status}`);
      }

      okMsg = "Añadido al carrito ✅";
      setTimeout(() => (okMsg = ""), 2500);
    } catch (e) {
      error = typeof e === "string" ? e : e.message || "Error al añadir al carrito";
    } finally {
      adding[c.idClase] = false;
    }
  }

  onMount(async () => {
    try {
      id = id ?? idFromPath();
      if (!id) throw new Error("ID inválido");
      vuelo = await getJSON(`${API_VUELOS}/${id}`);
    } catch (e) {
      error = "No se pudo cargar el vuelo";
    } finally {
      loading = false;
    }
  });
</script>

<div class="wrap">
  {#if loading}
    <p>Cargando…</p>
  {:else}
    {#if error}<p class="banner error">{error}</p>{/if}
    {#if okMsg}<p class="banner ok">{okMsg}</p>{/if}

    {#if vuelo}
      <h2>{vuelo.origen} → {vuelo.destino}</h2>
      <hr />

      <div class="info">
        <section>
          <h3>Ruta</h3>
          <p><b>Origen:</b> {vuelo.origen} ({vuelo.origenPais})</p>
          <p><b>Destino:</b> {vuelo.destino} ({vuelo.destinoPais})</p>
        </section>

        <section>
          <h3>Fechas</h3>
          <p><b>Salida:</b> {fmtDT(vuelo.fechaSalida)}</p>
          <p><b>Llegada:</b> {fmtDT(vuelo.fechaLlegada)}</p>
        </section>
      </div>

      <h3>Clases y precios</h3>
      <div class="classes">
        {#each (vuelo.clases || []) as c}
          <div class="class-card">
            <div class="price">{money(c.precio)}</div>
            <div>Clase #{c.idClase}</div>
            <div>Cupo total: {c.cupoTotal}</div>
            <button
              class="btn"
              disabled={adding[c.idClase]}
              on:click={() => addToCart(c)}
            >
              {adding[c.idClase] ? "Añadiendo…" : "Añadir al carrito"}
            </button>
          </div>
        {/each}
        {#if !(vuelo.clases && vuelo.clases.length)}
          <p>No hay clases disponibles.</p>
        {/if}
      </div>

      {#if vuelo.escalas && vuelo.escalas.length}
        <h3>Escalas</h3>
        <div class="scales">
          {#each vuelo.escalas as e}
            <div class="scale">
              <div><b>{e.ciudad}</b> ({e.pais})</div>
              <div>Arribo: {fmtDT(e.llegada)}</div>
              <div>Salida: {fmtDT(e.salida)}</div>
            </div>
          {/each}
        </div>
      {/if}
    {/if}
  {/if}
</div>

<style>
  .wrap { padding: 16px; }
  h2 { color:#e51c23; text-align:center; }
  hr { margin: 10px 0 18px; border: none; border-top: 1px solid #eee; }
  .info { display:grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
  section { background:#fafafa; border:1px solid #f0f0f0; border-radius:12px; padding:14px; }
  .classes { display:grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; }
  .class-card { border:1px solid #eee; border-radius:14px; padding:14px; background:#fff; }
  .price { font-size:1.2rem; font-weight:700; color:#0a7; margin-bottom:6px; }
  .btn { padding:8px 12px; border-radius:10px; border:1px solid #e51c23; background:#e51c23; color:#fff; cursor:pointer; }
  .btn[disabled] { opacity:.6; cursor:not-allowed; }
  .scales { display:grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap:10px; }
  .scale { background:#fff; border:1px solid #eee; border-radius:12px; padding:10px; }
  .banner { padding:10px; border-radius:8px; margin:8px 0; }
  .banner.error { color:#900; background:#fee; border:1px solid #f6c; }
  .banner.ok { color:#065; background:#e9fff5; border:1px solid #bff0dc; }
  @media (max-width: 700px){ .info { grid-template-columns: 1fr; } }
</style>