<script>
  import { onMount } from "svelte";
  import { getQuery } from "@/lib/router";
  import { setProveedor, getProveedor, clearProveedor } from "@/lib/proveedorStore";

  const API_BASE    = import.meta.env.VITE_API_URL || "http://127.0.0.1:8001";
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
  function safe(n) {
    return n == null || n === "" ? "-" : n;
  }
  function getSalida(v) {
    return v?.fechaSalida || v?.salida || v?.fecha_salida || v?.horaSalida || v?.salidaFecha || null;
  }
  function getLlegada(v) {
    return v?.fechaLlegada || v?.llegada || v?.fecha_llegada || v?.horaLlegada || v?.llegadaFecha || null;
  }
  function classDisplayName(c) {
    return c?.clase || c?.nombre || c?.nombreClase || c?.tipo || `Clase #${c?.idClase}`;
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

      // Incluir proveedor si está disponible en el vuelo
      if (vuelo.proveedor) {
        body.proveedor = vuelo.proveedor;
      }

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
      
      // Obtener proveedor del store/localStorage (esta es la fuente principal)
      let proveedor = getProveedor();
      
      // Si no hay en el store, intentar desde query params como fallback
      if (!proveedor) {
        try {
          const query = getQuery();
          proveedor = query.get("proveedor");
        } catch {
          // Ignorar
        }
      }
      
      console.log("[Detail] ID vuelo:", id, "Proveedor a usar:", proveedor);
      
      let url = `${API_VUELOS}/${id}`;
      if (proveedor) {
        url += `?proveedor=${encodeURIComponent(proveedor)}`;
      }
      
      console.log("[Detail] URL a consultar:", url);
      
      vuelo = await getJSON(url);
      
      console.log("[Detail] Vuelo recibido, proveedor:", vuelo?.proveedor);
      
      // Guardar el proveedor del vuelo en el store para futuras consultas
      // (esto asegura que si el vuelo tiene un proveedor, se guarde)
      if (vuelo && vuelo.proveedor) {
        setProveedor(vuelo.proveedor);
        console.log("[Detail] Proveedor guardado en store:", vuelo.proveedor);
      }
    } catch (e) {
      console.error("[Detail] Error:", e);
      error = "No se pudo cargar el vuelo";
      // Si hay error, limpiar el proveedor guardado
      clearProveedor();
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
          <p><b>Origen:</b> {vuelo.origen} ({safe(vuelo.origenPais)})</p>
          <p><b>Destino:</b> {vuelo.destino} ({safe(vuelo.destinoPais)})</p>
          {#if vuelo.codigo || vuelo.codigoVuelo}
            <p><b>Código:</b> {vuelo.codigo || vuelo.codigoVuelo}</p>
          {/if}
          {#if vuelo.proveedor}
            <p><b>Proveedor:</b> {vuelo.nombreProveedor || vuelo.proveedor}</p>
          {/if}
        </section>

        <section>
          <h3>Fechas</h3>
          <p><b>Salida:</b> {fmtDT(getSalida(vuelo))}</p>
          <p><b>Llegada:</b> {fmtDT(getLlegada(vuelo))}</p>
          {#if vuelo.duracion}
            <p><b>Duración:</b> {safe(vuelo.duracion)}</p>
          {/if}
        </section>
      </div>

      {#if (vuelo.detalles || vuelo.extra)}
        <h3>Detalles</h3>
        <section class="box">
          {#if vuelo.avion}<p><b>Avión:</b> {vuelo.avion}</p>{/if}
          {#if vuelo.aerolinea}<p><b>Aerolínea:</b> {vuelo.aerolinea}</p>{/if}
          {#if vuelo.terminalSalida}<p><b>Terminal salida:</b> {vuelo.terminalSalida}</p>{/if}
          {#if vuelo.terminalLlegada}<p><b>Terminal llegada:</b> {vuelo.terminalLlegada}</p>{/if}
          {#if vuelo.puertaSalida}<p><b>Puerta salida:</b> {vuelo.puertaSalida}</p>{/if}
          {#if vuelo.puertaLlegada}<p><b>Puerta llegada:</b> {vuelo.puertaLlegada}</p>{/if}
        </section>
      {/if}

      <h3>Clases y precios</h3>
      <div class="classes">
        {#each (vuelo.clases || []) as c}
          <div class="class-card">
            <div class="price">{money(c.precio)}</div>
            <div>{classDisplayName(c)}</div>
            <div>Cupo total: {c.cupoTotal}</div>
            {#if c.disponibles != null}
              <div>Disponibles: {c.disponibles}</div>
            {/if}
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
  .box { background:#fafafa; border:1px solid #f0f0f0; border-radius:12px; padding:14px; margin-bottom: 14px; }
  .scales { display:grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap:10px; }
  .scale { background:#fff; border:1px solid #eee; border-radius:12px; padding:10px; }
  .banner { padding:10px; border-radius:8px; margin:8px 0; }
  .banner.error { color:#900; background:#fee; border:1px solid #f6c; }
  .banner.ok { color:#065; background:#e9fff5; border:1px solid #bff0dc; }
  @media (max-width: 700px){ .info { grid-template-columns: 1fr; } }
</style>