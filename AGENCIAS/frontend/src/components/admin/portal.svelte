<script>
  import { onMount } from "svelte";
  import { fetchUI, saveHeader, saveFooter } from "../../lib/portalApi";
  import { isAdmin } from "../../lib/auth";
  import { navigate } from "../../lib/router";

  let ui = { header: {}, footer: {} };

  function guard() {
    const ok = isAdmin();
    if (!ok) {
      alert("Solo administradores");
      navigate("/");
    }
    return ok;
  }

  onMount(async () => {
    if (!guard()) return;
    try {
      ui = await fetchUI();
      ui.header ||= {};
      ui.footer ||= {};
    } catch (e) {
      alert(`No se pudo cargar UI: ${e.message}`);
    }
  });

  async function save(type) {
    try {
      if (type === "header") await saveHeader(ui.header);
      if (type === "footer") await saveFooter(ui.footer);
      alert("Guardado correctamente");
    } catch (e) {
      alert(e.message);
    }
  }
</script>

<svelte:window on:hashchange={guard} on:load={guard} />

<h1>Administración del Portal</h1>

<section class="card">
  <h2>Header</h2>
  <div class="grid">
    <label>Logo URL
      <input bind:value={ui.header.logo_url} placeholder="https://..." />
    </label>
    <label>Título
      <input bind:value={ui.header.title} placeholder="Nombre de la agencia" />
    </label>
    <label class="check">
      <input type="checkbox" bind:checked={ui.header.show_search} />
      Mostrar búsqueda
    </label>
    <label class="check">
      <input type="checkbox" bind:checked={ui.header.show_cart} />
      Mostrar carrito
    </label>
  </div>
  <button on:click={() => save("header")}>Guardar Header</button>
</section>

<section class="card">
  <h2>Footer</h2>
  <div class="grid">
    <label>Logo URL
      <input bind:value={ui.footer.logo_url} placeholder="https://..." />
    </label>
    <label>Teléfono
      <input bind:value={ui.footer.phone} />
    </label>
    <label>Dirección
      <input bind:value={ui.footer.address} />
    </label>
    <label>Copyright
      <input bind:value={ui.footer.copyright} />
    </label>
  </div>

  <h3>Links legales</h3>
  {#each (ui.footer.legal_links || (ui.footer.legal_links = [])) as link, i}
    <div class="row">
      <input placeholder="Etiqueta" bind:value={link.label} />
      <input placeholder="URL" bind:value={link.href} />
      <button type="button" on:click={() => ui.footer.legal_links.splice(i, 1)}>
        Eliminar
      </button>
    </div>
  {/each}
  <button
    type="button"
    on:click={() => ui.footer.legal_links.push({ label: "", href: "" })}
  >
    + Agregar link
  </button>

  <div style="margin-top:12px">
    <button on:click={() => save("footer")}>Guardar Footer</button>
  </div>
</section>

<style>
  h1 {
    margin: 12px 0 18px;
  }
  .card {
    background: #fff;
    border: 1px solid #eee;
    border-radius: 12px;
    padding: 16px;
    margin: 12px 0;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
  }
  .grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }
  .row {
    display: flex;
    gap: 8px;
    margin: 6px 0;
  }
  label {
    display: flex;
    flex-direction: column;
    font-size: 0.9rem;
    gap: 6px;
  }
  label.check {
    flex-direction: row;
    align-items: center;
    gap: 8px;
  }
  input {
    padding: 8px;
    border: 1px solid #ddd;
    border-radius: 8px;
  }
  button {
    padding: 8px 12px;
    border: 0;
    border-radius: 8px;
    background: #e62727;
    color: #fff;
    cursor: pointer;
  }
</style>