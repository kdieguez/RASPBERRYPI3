<script>
  import { onMount } from "svelte";
  import { PaginasAPI } from "@/lib/api";

  const slug = "cancelacion-compras";

  let loading = true;
  let saving = false;
  let error = "";
  let success = "";
  let pagina = null;

  async function cargar() {
    loading = true;
    error = "";
    success = "";

    try {
      pagina = await PaginasAPI.getAdmin(slug);

      if (!pagina) {
        pagina = {
          slug,
          titulo: "Cancelación de compras",
          descripcion: "",
          habilitado: true,
          secciones: []
        };
      }

      if (!Array.isArray(pagina.secciones)) {
        pagina.secciones = [];
      }
    } catch (e) {
      error = e?.message || "Error al cargar la página.";
    } finally {
      loading = false;
    }
  }

  onMount(cargar);

  function addSeccion() {
    const nextOrden = (pagina.secciones?.length || 0) + 1;
    pagina.secciones = [
      ...pagina.secciones,
      {
        id: `sec-${Date.now()}`,
        titulo: "Nueva sección",
        contenido_html: "",
        orden: nextOrden,
        imagenes: []
      }
    ];
  }

  function removeSeccion(idx) {
    if (!confirm("¿Eliminar esta sección?")) return;
    pagina.secciones = pagina.secciones.filter((_, i) => i !== idx);
  }

  function addImagen(idx) {
    pagina.secciones[idx].imagenes = [
      ...(pagina.secciones[idx].imagenes || []),
      { url: "", alt: "" }
    ];
  }

  function removeImagen(idxSec, idxImg) {
    pagina.secciones[idxSec].imagenes =
      pagina.secciones[idxSec].imagenes.filter((_, i) => i !== idxImg);
  }

  async function guardar() {
    saving = true;
    error = "";
    success = "";

    try {
      const payload = {
        titulo: pagina.titulo,
        descripcion: pagina.descripcion,
        habilitado: pagina.habilitado,
        secciones: pagina.secciones.map((s, i) => ({
          id: s.id || `sec-${i + 1}`,
          titulo: s.titulo,
          contenido_html: s.contenido_html,
          orden: Number(s.orden) || i + 1,
          imagenes: (s.imagenes || [])
            .filter((img) => img.url.trim() && img.alt.trim())
        }))
      };

      await PaginasAPI.updateAdmin(slug, payload);
      success = "Cambios guardados correctamente.";
    } catch (e) {
      error = e?.message || "Error al guardar los cambios.";
    } finally {
      saving = false;
    }
  }
</script>

<style>
  .page-admin {
    max-width: 960px;
    margin: 1.5rem auto;
    padding: 1.5rem;
  }
  h1 {
    font-size: 1.8rem;
    margin-bottom: 0.25rem;
  }
  .subtitle {
    color: #555;
    margin-bottom: 1rem;
  }
  label {
    font-weight: 600;
  }
  input[type="text"],
  textarea,
  input[type="number"] {
    padding: 0.4rem 0.6rem;
    border-radius: 6px;
    border: 1px solid #ccc;
    font: inherit;
  }
  textarea {
    min-height: 100px;
  }
  .switch-row {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }
  .sections-admin {
    margin-top: 1rem;
  }
  .sec-card {
    border: 1px solid #ddd;
    border-radius: 10px;
    padding: 1rem;
    background: #fff;
    margin-bottom: 1rem;
  }
  .sec-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 0.5rem;
  }
  .btn {
    border-radius: 10px;
    padding: 6px 12px;
    border: 1px solid #1f2937;
    background: #0b1020;
    color: #fff;
    cursor: pointer;
  }
  .btn.ghost {
    background: transparent;
    color: #111;
  }
  .btn.danger {
    background: #b91c1c;
  }
  .toolbar {
    margin-top: 1rem;
    display: flex;
    justify-content: flex-end;
    gap: 0.6rem;
  }
  .msg.error {
    color: #b00020;
  }
  .msg.success {
    color: #176b34;
  }
</style>

<div class="page-admin">
  {#if loading}
    <p>Cargando configuración…</p>
  {:else if error}
    <p class="msg error">{error}</p>
  {:else}
    <h1>Editar: Cancelación de compras</h1>
    <p class="subtitle">Página informativa editable solo por administradores.</p>

    <!-- TÍTULO -->
    <div>
      <label>Título</label>
      <input type="text" bind:value={pagina.titulo} />
    </div>

    <!-- DESCRIPCIÓN -->
    <div style="margin-top: 0.7rem;">
      <label>Descripción</label>
      <textarea bind:value={pagina.descripcion}></textarea>
    </div>

    <!-- HABILITADO -->
    <div class="switch-row" style="margin:0.7rem 0;">
      <input type="checkbox" bind:checked={pagina.habilitado} />
      <label>Página habilitada</label>
    </div>

    <!-- SECCIONES -->
    <h2 style="margin-top:1.5rem;">Secciones</h2>

    <button class="btn ghost" on:click={addSeccion}>+ Agregar sección</button>

    <div class="sections-admin">
      {#each pagina.secciones as sec, idx}
        <div class="sec-card">
          <div class="sec-header">
            <strong>Sección {idx + 1}</strong>
            <button class="btn danger" on:click={() => removeSeccion(idx)}>
              Eliminar
            </button>
          </div>

          <label>Título</label>
          <input type="text" bind:value={sec.titulo} />

          <label style="margin-top:0.5rem;">Contenido HTML</label>
          <textarea bind:value={sec.contenido_html}></textarea>

          <label style="margin-top:0.5rem;">Orden</label>
          <input type="number" bind:value={sec.orden} min="1" />

          <h4 style="margin-top:1rem;">Imágenes</h4>
          {#each sec.imagenes as img, idxImg}
            <div style="display:flex;gap:0.4rem;margin-bottom:0.4rem;">
              <input type="text" placeholder="URL" bind:value={img.url} />
              <input type="text" placeholder="ALT" bind:value={img.alt} />
              <button class="btn ghost" on:click={() => removeImagen(idx, idxImg)}>X</button>
            </div>
          {/each}

          <button class="btn ghost" on:click={() => addImagen(idx)}>
            + Agregar imagen
          </button>
        </div>
      {/each}
    </div>

    <!-- GUARDAR -->
    <div class="toolbar">
      {#if error}
        <span class="msg error">{error}</span>
      {/if}
      {#if success}
        <span class="msg success">{success}</span>
      {/if}
      <button class="btn" on:click={guardar} disabled={saving}>
        {saving ? "Guardando…" : "Guardar cambios"}
      </button>
    </div>
  {/if}
</div>