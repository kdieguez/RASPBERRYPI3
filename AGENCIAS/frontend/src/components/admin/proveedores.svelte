<script>
  import { onMount } from "svelte";
  import { ProveedoresAdminAPI } from "@/lib/api";

  let loading = true;
  let saving = false;
  let error = "";

  let proveedores = [];
  let seleccionadoId = "";

  let form = {
    nombre: "",
    apiUrl: "",
    habilitado: true,
    pais: "",
    tipo: "",
    descripcion: "",
    logoUrl: "",
  };

  onMount(async () => {
    await cargarLista();
  });

  async function cargarLista() {
    loading = true;
    error = "";
    try {
      proveedores = await ProveedoresAdminAPI.list();
      console.log("[ADMIN PROV] lista:", proveedores);

      if (proveedores.length > 0) {
        await seleccionar(proveedores[0].id);
      }
    } catch (e) {
      console.error(e);
      error = e.message ?? "Error al cargar proveedores.";
    } finally {
      loading = false;
    }
  }

  async function seleccionar(id) {
    seleccionadoId = id;
    loading = true;
    error = "";
    try {
      const p = await ProveedoresAdminAPI.get(id);
      console.log("[ADMIN PROV] seleccionado:", p);

      form = {
        nombre: p.nombre || "",
        apiUrl: p.apiUrl || "",
        habilitado: p.habilitado ?? true,
        pais: p.pais || "",
        tipo: p.tipo || "",
        descripcion: p.descripcion || "",
        logoUrl: p.logoUrl || "",
      };
    } catch (e) {
      console.error(e);
      error = e.message ?? "Error al cargar proveedor.";
    } finally {
      loading = false;
    }
  }

  async function guardar() {
    if (!seleccionadoId) return;
    saving = true;
    error = "";
    try {
      console.log("[ADMIN PROV] guardando:", seleccionadoId, form);

      const actualizado = await ProveedoresAdminAPI.update(seleccionadoId, form);
      console.log("[ADMIN PROV] actualizado:", actualizado);

      const idx = proveedores.findIndex((p) => p.id === actualizado.id);
      if (idx >= 0) {
        proveedores[idx] = actualizado;
      }

      alert("Proveedor actualizado correctamente.");
    } catch (e) {
      console.error(e);
      error = e.message ?? "Error al guardar cambios.";
    } finally {
      saving = false;
    }
  }
</script>

<style>
  .page {
    max-width: 960px;
    margin: 0 auto;
    padding: 1.5rem;
  }

  .row {
    display: flex;
    gap: 1rem;
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .sidebar {
    min-width: 220px;
  }

  .form {
    flex: 1;
    min-width: 260px;
    background: #fff;
    border-radius: 12px;
    padding: 1rem 1.2rem;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
  }

  h1 {
    margin-bottom: 0.4rem;
  }

  p.description {
    margin-bottom: 1rem;
    color: #4b5563;
    font-size: 0.9rem;
  }

  label {
    display: block;
    font-size: 0.85rem;
    font-weight: 600;
    margin-top: 0.6rem;
  }

  input,
  textarea,
  select {
    width: 100%;
    padding: 0.4rem 0.5rem;
    border-radius: 6px;
    border: 1px solid #d1d5db;
    font-size: 0.9rem;
  }

  textarea {
    min-height: 80px;
  }

  .checkbox-row {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    margin-top: 0.5rem;
  }

  .btn-primary {
    margin-top: 1rem;
    padding: 0.6rem 1rem;
    border-radius: 8px;
    border: none;
    background: #277b29;
    color: #fff;
    cursor: pointer;
  }

  .btn-primary[disabled] {
    opacity: 0.7;
    cursor: default;
  }

  .error {
    margin-top: 0.5rem;
    color: #b00020;
  }
</style>

<div class="page">
  <h1>Aerolíneas afiliadas (admin)</h1>
  <p class="description">
    Administra la información que se muestra en la página pública de aerolíneas:
    nombre, país, tipo, descripción y logo de cada proveedor.
  </p>

  {#if loading}
    <p>Cargando…</p>
  {:else}
    <div class="row">
      <aside class="sidebar">
        <label for="sel-proveedor">Selecciona aerolínea</label>
        <select
          id="sel-proveedor"
          bind:value={seleccionadoId}
          on:change={(e) => seleccionar(e.target.value)}
        >
          {#each proveedores as p}
            <option value={p.id}>{p.nombre}</option>
          {/each}
        </select>
      </aside>

      <section class="form">
        <label for="nombre">Nombre público</label>
        <input id="nombre" bind:value={form.nombre} />

        <label for="apiUrl">Link</label>
        <input id="apiUrl" bind:value={form.apiUrl} />

        <label for="pais">País objetivo</label>
        <input id="pais" bind:value={form.pais} />

        <label for="tipo">Tipo (ej. Socio estratégico, Aliado, etc.)</label>
        <input id="tipo" bind:value={form.tipo} />

        <label for="descripcion">Descripción</label>
        <textarea id="descripcion" bind:value={form.descripcion}></textarea>

        <label for="logoUrl">Logo (URL)</label>
        <input id="logoUrl" bind:value={form.logoUrl} />

        <div class="checkbox-row">
          <input id="chk-hab" type="checkbox" bind:checked={form.habilitado} />
          <label for="chk-hab">Proveedor habilitado</label>
        </div>

        <button
          class="btn-primary"
          on:click|preventDefault={guardar}
          disabled={saving}
        >
          {saving ? "Guardando…" : "Guardar cambios"}
        </button>

        {#if error}
          <div class="error">{error}</div>
        {/if}
      </section>
    </div>
  {/if}
</div>