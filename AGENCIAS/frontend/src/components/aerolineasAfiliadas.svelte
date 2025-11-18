<script>
  import { onMount } from "svelte";
  import { ProveedoresAPI, fmtDate, toDate } from "@/lib/api";

  const secondary = "#1E93AB";

  let loading = true;
  let error = "";
  let proveedores = [];

  onMount(async () => {
    loading = true;
    error = "";
    try {
      proveedores = await ProveedoresAPI.listPublic();
    } catch (e) {
      console.error(e);
      error = e.message ?? "Error al cargar las aerolíneas afiliadas.";
    } finally {
      loading = false;
    }
  });
</script>

<style>
  .page-container {
    max-width: 1000px;
    margin: 0 auto;
    padding: 1.5rem;
  }

  .page-header {
    margin-bottom: 1.5rem;
  }

  .badge {
    display: inline-flex;
    align-items: center;
    gap: 0.4rem;
    font-size: 0.8rem;
    padding: 0.25rem 0.6rem;
    border-radius: 999px;
    background: #ffecec;
    color: #b00020;
  }

  .page-title {
    font-size: 1.9rem;
    font-weight: 700;
    margin: 0.5rem 0 0.25rem 0;
    color: #222;
  }

  .page-description {
    margin-top: 0.25rem;
    color: #555;
  }

  .grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
    gap: 1rem;
    margin-top: 1.5rem;
  }

  .card {
    background: #ffffff;
    border-radius: 12px;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
    padding: 1rem 1.2rem;
    display: flex;
    flex-direction: column;
    gap: 0.4rem;
    border-top: 4px solid #f2f2f2;
  }

  .card-main {
    display: flex;
    gap: 1rem;
    align-items: flex-start;
  }

  .card-text {
    flex: 1;
  }

  .card-logo {
    width: 90px;
    min-width: 90px;
    height: 90px;
    border-radius: 50%;
    overflow: hidden;
    border: 2px solid #e5e7eb;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f9fafb;
  }

  .card-logo img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    gap: 0.5rem;
  }

  .card-title {
    font-size: 1.1rem;
    font-weight: 600;
  }

  .tag {
    font-size: 0.75rem;
    padding: 0.2rem 0.5rem;
    border-radius: 999px;
    background: #e3f2fd;
    color: #0d47a1;
    white-space: nowrap;
  }

  .card-description {
    font-size: 0.9rem;
    color: #555;
    margin-top: 0.25rem;
  }

  .meta {
    font-size: 0.8rem;
    color: #777;
    margin-top: 0.15rem;
  }

  .meta strong {
    font-weight: 600;
  }

  .api-url {
    font-size: 0.8rem;
    color: #1e93ab;
    word-break: break-all;
    margin-top: 0.15rem;
  }

  .loading,
  .error {
    text-align: center;
    padding: 2rem 1rem;
  }

  .error {
    color: #b00020;
  }
</style>

<div class="page-container">
  {#if loading}
    <div class="loading">
      Cargando aerolíneas afiliadas…
    </div>
  {:else if error}
    <div class="error">{error}</div>
  {:else}
    <header class="page-header">
      <div class="badge">
        <span
          style={`display:inline-block;width:10px;height:10px;border-radius:50%;background:${secondary};`}
        ></span>
        <span>Aerolíneas aliadas</span>
      </div>
      <h1 class="page-title">Aerolíneas que trabajan con nosotros</h1>
      <p class="page-description">
        Estas son las aerolíneas proveedoras que actualmente están integradas
        con nuestra agencia. Sus sistemas se conectan a través de APIs
        empresariales para que puedas ver y reservar sus vuelos desde un solo lugar.
      </p>
    </header>

    {#if proveedores.length === 0}
      <div class="error">
        Por el momento no hay aerolíneas afiliadas habilitadas.
      </div>
    {:else}
      <section class="grid">
        {#each proveedores as p (p.id)}
          <article class="card">
            <div class="card-main">
              <div class="card-text">
                <div class="card-header">
                  <h2 class="card-title">{p.nombre}</h2>
                  {#if p.tipo}
                    <span class="tag">{p.tipo}</span>
                  {/if}
                </div>

                {#if p.descripcion}
                  <p class="card-description">
                    {p.descripcion}
                  </p>
                {/if}

                {#if p.pais}
                  <p class="meta">
                    <strong>País objetivo:</strong> {p.pais}
                  </p>
                {/if}

                {#if p.apiUrl}
                  <p class="api-url">
                    <strong>API:</strong> {p.apiUrl}
                  </p>
                {/if}

                {#if p.creadoEn}
                  <p class="meta">
                    <strong>Integrado desde:</strong>
                    {" "}
                    {fmtDate(toDate(p.creadoEn), {
                      year: "numeric",
                      month: "short",
                      day: "2-digit",
                    })}
                  </p>
                {/if}
              </div>

              {#if p.logoUrl}
                <div class="card-logo">
                  <img src={p.logoUrl} alt={`Logo de ${p.nombre}`} />
                </div>
              {/if}
            </div>
          </article>
        {/each}
      </section>
    {/if}
  {/if}
</div>