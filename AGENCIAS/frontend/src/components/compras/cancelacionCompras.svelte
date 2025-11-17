<script>
  import { onMount } from "svelte";
  import { PaginasAPI } from "@/lib/api";

  let pagina = null;
  let loading = true;
  let error = "";

  const primary = "#E62727";
  const secondary = "#1E93AB";

  async function cargarPagina() {
    loading = true;
    error = "";
    try {
      pagina = await PaginasAPI.getPublic("cancelacion-compras");
    } catch (e) {
      console.error(e);
      error = e.message ?? "Error desconocido al cargar la página.";
    } finally {
      loading = false;
    }
  }

  onMount(cargarPagina);
</script>

<style>
  .page-container {
    max-width: 900px;
    margin: 0 auto;
    padding: 1.5rem;
  }

  .page-header {
    border-bottom: 3px solid #eee;
    margin-bottom: 1.5rem;
    padding-bottom: 0.75rem;
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
    font-size: 1.8rem;
    font-weight: 700;
    margin: 0.5rem 0 0.25rem 0;
    color: #222;
  }

  .page-description {
    margin-top: 0.25rem;
    color: #555;
  }

  .sections {
    display: flex;
    flex-direction: column;
    gap: 1.3rem;
  }

  .section-card {
    border-radius: 12px;
    padding: 1.1rem 1.3rem;
    box-shadow: 0 2px 6px rgba(0,0,0,0.08);
    background: #ffffff;
    border-left: 4px solid #f2f2f2;
  }

  .section-card:nth-child(odd) {
    border-left-color: #E62727;
  }

  .section-card:nth-child(even) {
    border-left-color: #1E93AB;
  }

  .section-title {
    font-size: 1.2rem;
    font-weight: 600;
    margin: 0 0 0.5rem 0;
  }

  .section-content :global(p) {
    margin: 0.3rem 0;
    line-height: 1.5;
  }

  .section-content :global(ul),
  .section-content :global(ol) {
    padding-left: 1.5rem;
    margin: 0.4rem 0;
  }

  .section-images {
    display: flex;
    flex-wrap: wrap;
    gap: 0.75rem;
    margin-top: 0.8rem;
  }

  .section-images img {
    max-width: 180px;
    border-radius: 8px;
    object-fit: cover;
  }

  .loading,
  .error {
    text-align: center;
    padding: 2rem 1rem;
  }

  .error {
    color: #b00020;
  }

  @media (max-width: 640px) {
    .page-container {
      padding: 1rem;
    }

    .section-images {
      justify-content: center;
    }

    .section-images img {
      max-width: 100%;
    }
  }
</style>

<div class="page-container">
  {#if loading}
    <div class="loading">
      <p>Cargando información sobre cancelación de compras…</p>
    </div>
  {:else if error}
    <div class="error">
      <p>{error}</p>
    </div>
  {:else if pagina}
    <header class="page-header">
      <div class="badge">
        <span
          style={`display:inline-block;width:10px;height:10px;border-radius:50%;background:${primary};`}
        ></span>
        <span>Guía de ayuda</span>
      </div>
      <h1 class="page-title">{pagina.titulo}</h1>
      <p class="page-description">{pagina.descripcion}</p>
    </header>

    <main class="sections">
      {#each pagina.secciones
        .slice()
        .sort((a, b) => a.orden - b.orden) as seccion}
        <article class="section-card">
          <h2 class="section-title">{seccion.titulo}</h2>

          <div class="section-content">
            {@html seccion.contenido_html}
          </div>

          {#if seccion.imagenes && seccion.imagenes.length > 0}
            <div class="section-images">
              {#each seccion.imagenes as img}
                <img src={img.url} alt={img.alt} loading="lazy" />
              {/each}
            </div>
          {/if}
        </article>
      {/each}
    </main>
  {:else}
    <div class="error">
      <p>No se encontró contenido para esta página.</p>
    </div>
  {/if}
</div>