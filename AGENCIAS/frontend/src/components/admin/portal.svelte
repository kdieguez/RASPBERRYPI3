<script>
  import { onMount } from "svelte";
  import {
    fetchUI,
    saveHeader,
    saveFooter,
    saveHome
  } from "../../lib/portalApi";
  import { isAdmin } from "../../lib/auth";
  import { navigate } from "../../lib/router";

  let ui = { header: {}, footer: {}, home: {} };

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
      ui.home ||= {};

      ui.home.steps ||= [];
      ui.home.benefits ||= [];
      ui.home.touristDestinations ||= [];
    } catch (e) {
      alert(`No se pudo cargar UI: ${e.message}`);
    }
  });

  async function save(type) {
    try {
      if (type === "header") {
        await saveHeader(ui.header);
      }
      if (type === "footer") {
        await saveFooter(ui.footer);
      }
      if (type === "home") {
        await saveHome(ui.home);
      }

      alert("Guardado correctamente");

      if (type === "header" || type === "footer") {
        window.location.reload();
      }
    } catch (e) {
      alert(e.message);
    }
  }

  function addStep() {
    ui.home.steps ||= [];
    ui.home.steps = [
      ...ui.home.steps,
      { title: "", text: "" }
    ];
  }

  function removeStep(i) {
    ui.home.steps = ui.home.steps.filter((_, idx) => idx !== i);
  }

  function addBenefit() {
    ui.home.benefits ||= [];
    ui.home.benefits = [
      ...ui.home.benefits,
      { icon: "⭐", title: "", text: "" }
    ];
  }

  function removeBenefit(i) {
    ui.home.benefits = ui.home.benefits.filter((_, idx) => idx !== i);
  }

  function addDestination() {
    ui.home.touristDestinations ||= [];
    ui.home.touristDestinations = [
      ...ui.home.touristDestinations,
      { name: "", imageUrl: "" }
    ];
  }

  function removeDestination(i) {
    ui.home.touristDestinations =
      ui.home.touristDestinations.filter((_, idx) => idx !== i);
  }

  function addLegalLink() {
    ui.footer.legal_links ||= [];
    ui.footer.legal_links = [
      ...ui.footer.legal_links,
      { label: "", href: "" }
    ];
  }

  function removeLegalLink(i) {
    ui.footer.legal_links =
      ui.footer.legal_links.filter((_, idx) => idx !== i);
  }
</script>

<svelte:window on:hashchange={guard} on:load={guard} />

<h1>Administración del Portal</h1>

<section class="card">
  <h2>Header</h2>
  <p class="help">
    Configura el logo, el título y las acciones visibles en la barra superior.
  </p>

  <div class="grid">
    <label>Logo URL
      <input
        bind:value={ui.header.logo_url}
        placeholder="https://ejemplo.com/logo.png"
      />
    </label>
    <label>Título
      <input
        bind:value={ui.header.title}
        placeholder="Nombre de la agencia"
      />
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
  <h2>Home</h2>
  <p class="help">
    Aquí defines los textos e ítems que se muestran en la página de inicio:
    hero, pasos, beneficios y destinos turísticos.
  </p>

  <div class="grid">
    <label>Título principal (heroTitle)
      <input
        bind:value={ui.home.heroTitle}
        placeholder="Tu viaje, en un solo lugar"
      />
    </label>
    <label>Texto destacado (heroHighlight)
      <input
        bind:value={ui.home.heroHighlight}
        placeholder="Integración B2B entre aerolíneas y agencias"
      />
    </label>
  </div>

  <label>Subtítulo (heroSubtitle)
    <textarea
      bind:value={ui.home.heroSubtitle}
      rows="2"
      placeholder="Texto que explica brevemente qué hace el portal"
    ></textarea>
  </label>

  <div class="grid">
    <label>CTA principal - texto
      <input
        bind:value={ui.home.ctaPrimaryLabel}
        placeholder="Buscar vuelos"
      />
    </label>
    <label>CTA principal - link
      <input
        bind:value={ui.home.ctaPrimaryHref}
        placeholder="/vuelos"
      />
    </label>
    <label>CTA secundaria - texto
      <input
        bind:value={ui.home.ctaSecondaryLabel}
        placeholder="Ver aerolíneas afiliadas"
      />
    </label>
    <label>CTA secundaria - link
      <input
        bind:value={ui.home.ctaSecondaryHref}
        placeholder="/aerolineas-afiliadas"
      />
    </label>
  </div>

  <h3>Pasos del flujo (steps)</h3>
  {#if (ui.home.steps || []).length === 0}
    <p class="help">No hay pasos aún. Agrega al menos uno.</p>
  {/if}
  {#each ui.home.steps || [] as step, i}
    <div class="row">
      <input
        class="grow"
        placeholder={`Título paso ${i + 1}`}
        bind:value={step.title}
      />
      <input
        class="grow"
        placeholder="Descripción breve"
        bind:value={step.text}
      />
      <button type="button" on:click={() => removeStep(i)}>✕</button>
    </div>
  {/each}
  <button type="button" class="secondary" on:click={addStep}>
    + Agregar paso
  </button>

  <h3>Beneficios (benefits)</h3>
  {#if (ui.home.benefits || []).length === 0}
    <p class="help">No hay beneficios aún. Agrega algunos puntos clave.</p>
  {/if}
  {#each ui.home.benefits || [] as b, i}
    <div class="row">
      <input
        style="max-width:60px"
        placeholder="Icono"
        bind:value={b.icon}
      />
      <input
        class="grow"
        placeholder="Título"
        bind:value={b.title}
      />
      <input
        class="grow"
        placeholder="Descripción"
        bind:value={b.text}
      />
      <button type="button" on:click={() => removeBenefit(i)}>✕</button>
    </div>
  {/each}
  <button type="button" class="secondary" on:click={addBenefit}>
    + Agregar beneficio
  </button>

  <h3>Destinos turísticos destacados</h3>
  <p class="help">
    Estos destinos se mostrarán en un carrusel en el Home. Solo defines nombre
    y URL de la imagen; el frontend los maqueta bonito.
  </p>

  {#if (ui.home.touristDestinations || []).length === 0}
    <p class="help">Aún no hay destinos. Agrega algunos.</p>
  {/if}

  {#each ui.home.touristDestinations || [] as d, i}
    <div class="row">
      <input
        class="grow"
        placeholder="Nombre del destino (ej. París, Francia)"
        bind:value={d.name}
      />
      <input
        class="grow"
        placeholder="URL de la imagen"
        bind:value={d.imageUrl}
      />
      <button type="button" on:click={() => removeDestination(i)}>✕</button>
    </div>
  {/each}

  <button type="button" class="secondary" on:click={addDestination}>
    + Agregar destino
  </button>

  <div style="margin-top:12px">
    <button on:click={() => save("home")}>Guardar Home</button>
  </div>
</section>

<section class="card">
  <h2>Footer</h2>
  <p class="help">
    Configura el logo, datos de contacto y links legales del pie de página.
  </p>

  <div class="grid">
    <label>Logo URL
      <input
        bind:value={ui.footer.logo_url}
        placeholder="https://ejemplo.com/logo-footer.png"
      />
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
  {#each ui.footer.legal_links || [] as link, i}
    <div class="row">
      <input
        class="grow"
        placeholder="Etiqueta (ej. Política de privacidad)"
        bind:value={link.label}
      />
      <input
        class="grow"
        placeholder="URL"
        bind:value={link.href}
      />
      <button type="button" on:click={() => removeLegalLink(i)}>✕</button>
    </div>
  {/each}
  <button type="button" class="secondary" on:click={addLegalLink}>
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
  @media (max-width: 800px) {
    .grid {
      grid-template-columns: 1fr;
    }
  }
  .row {
    display: flex;
    gap: 8px;
    margin: 6px 0;
    align-items: center;
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
  input,
  textarea {
    padding: 8px;
    border: 1px solid #ddd;
    border-radius: 8px;
    font-size: 0.9rem;
  }
  textarea {
    resize: vertical;
  }
  button {
    padding: 8px 12px;
    border: 0;
    border-radius: 8px;
    background: #e62727;
    color: #fff;
    cursor: pointer;
  }
  button.secondary {
    background: #f3f4f6;
    color: #111827;
  }
  .grow {
    flex: 1;
  }
  .help {
    font-size: 0.85rem;
    color: #6b7280;
    margin: 4px 0 10px;
  }
</style>