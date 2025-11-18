<script>
  import { onMount } from "svelte";
  import { user as currentUser, isLoggedIn } from "../lib/auth";
  import { request } from "@/lib/api";

  let loading = true;
  let error = "";
  let home = {};

  let topRoutes = [];
  let topError = "";
  let loadingTop = true;

  const defaultHome = {
    heroTitle: "Tu viaje, en un solo lugar",
    heroSubtitle:
      "Compara y reserva vuelos de nuestras aerolíneas aliadas desde una misma plataforma.",
    heroHighlight: "Integración aerolíneas.",

    ctaPrimaryLabel: "Buscar vuelos",
    ctaPrimaryHref: "/vuelos",
    ctaSecondaryLabel: "Ver aerolíneas afiliadas",
    ctaSecondaryHref: "/aerolineas-afiliadas",
    touristDestinations: [
      {
        name: "Guatemala",
        imageUrl:
          "https://images.pexels.com/photos/4603766/pexels-photo-4603766.jpeg?auto=compress&cs=tinysrgb&w=800"
      },
      {
        name: "México",
        imageUrl:
          "https://images.pexels.com/photos/672630/pexels-photo-672630.jpeg?auto=compress&cs=tinysrgb&w=800"
      },
      {
        name: "Madrid",
        imageUrl:
          "https://images.pexels.com/photos/4606722/pexels-photo-4606722.jpeg?auto=compress&cs=tinysrgb&w=800"
      }
    ]
  };

  function mergeHome(data) {
    const src = data || {};
    return {
      ...defaultHome,
      ...src,
      steps: Array.isArray(src.steps) && src.steps.length > 0
        ? src.steps
        : defaultHome.steps,
      benefits: Array.isArray(src.benefits) && src.benefits.length > 0
        ? src.benefits
        : defaultHome.benefits,
      touristDestinations:
        Array.isArray(src.touristDestinations) &&
        src.touristDestinations.length > 0
          ? src.touristDestinations
          : defaultHome.touristDestinations
    };
  }

  onMount(async () => {
    loading = true;
    error = "";
    try {
      const ui = await request("/portal/ui");
      home = mergeHome(ui?.home);
    } catch (e) {
      console.error(e);
      error = e.message ?? "Error al cargar la página de inicio.";
      home = defaultHome;
    } finally {
      loading = false;
    }

    loadingTop = true;
    topError = "";
    try {
      const data = await request("/compras/stats/top-rutas");
      topRoutes = Array.isArray(data) ? data : [];
    } catch (e) {
      console.error(e);
      topError =
        e?.message ?? "No se pudieron cargar las rutas más reservadas.";
    } finally {
      loadingTop = false;
    }
  });

  $: displayName =
    $currentUser?.nombres || $currentUser?.email || "Visitante";

  $: maxCount =
    topRoutes && topRoutes.length
      ? topRoutes.reduce((m, r) => Math.max(m, r.count ?? 0), 0)
      : 1;
</script>

<style>
  .home-page {
    max-width: 1100px;
    margin: 0 auto;
    padding: 1.5rem;
  }

  .hero {
    display: grid;
    grid-template-columns: minmax(0, 2fr) minmax(0, 1.2fr);
    gap: 2rem;
    align-items: center;
    margin-bottom: 2.5rem;
  }

  @media (max-width: 860px) {
    .hero {
      grid-template-columns: 1fr;
    }
  }

  .hero-greeting {
    font-size: 0.95rem;
    opacity: 0.85;
    margin-bottom: 0.4rem;
  }

  .hero-title {
    font-size: 2rem;
    font-weight: 700;
    margin: 0.2rem 0 0.6rem 0;
    color: #111827;
  }

  .hero-highlight {
    display: inline-block;
    background: rgba(30, 147, 171, 0.08);
    border-radius: 999px;
    padding: 0.25rem 0.9rem;
    font-size: 0.8rem;
    font-weight: 600;
    color: #1e93ab;
    border: 1px solid rgba(30, 147, 171, 0.35);
    margin-bottom: 0.4rem;
  }

  .hero-subtitle {
    font-size: 0.98rem;
    color: #4b5563;
    margin-bottom: 0.7rem;
  }

  .hero-cta {
    display: flex;
    flex-wrap: wrap;
    gap: 0.6rem;
  }

  .btn-primary {
    background: #e62727;
    color: white;
    padding: 0.55rem 1.1rem;
    border-radius: 999px;
    border: none;
    font-size: 0.9rem;
    cursor: pointer;
    text-decoration: none;
    transition: transform 0.08s ease-out,
      box-shadow 0.08s ease-out,
      background 0.1s ease-out;
  }

  .btn-primary:hover {
    background: #c51f1f;
    box-shadow: 0 4px 10px rgba(230, 39, 39, 0.35);
    transform: translateY(-1px);
  }

  .btn-secondary {
    background: white;
    color: #1f2933;
    padding: 0.55rem 1.1rem;
    border-radius: 999px;
    border: 1px solid rgba(30, 147, 171, 0.5);
    font-size: 0.9rem;
    cursor: pointer;
    text-decoration: none;
    transition: background 0.08s ease-out, color 0.08s ease-out;
  }

  .btn-secondary:hover {
    background: rgba(30, 147, 171, 0.06);
  }

  .section-title {
    font-size: 1.2rem;
    font-weight: 600;
    margin: 0 0 0.4rem 0;
  }

  .section-sub {
    font-size: 0.9rem;
    color: #6b7280;
    margin-bottom: 0.9rem;
  }

  .steps-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 1rem;
    margin-bottom: 2rem;
  }

  .step-card {
    background: #ffffff;
    border-radius: 12px;
    padding: 0.9rem 1rem;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
    border-top: 3px solid #1e93ab;
    font-size: 0.88rem;
  }

  .step-card strong {
    display: block;
    margin-bottom: 0.25rem;
  }

  .benefits-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 1rem;
    margin-bottom: 1.5rem;
  }

  .benefit-card {
    background: #f9fafb;
    border-radius: 12px;
    padding: 0.9rem 1rem;
    font-size: 0.88rem;
    display: flex;
    gap: 0.6rem;
    border: 1px solid #e5e7eb;
  }

  .benefit-icon {
    font-size: 1.6rem;
    line-height: 1;
  }

  .benefit-title {
    font-weight: 600;
    margin-bottom: 0.2rem;
  }

  .destinos-section {
    margin-top: 2.2rem;
    margin-bottom: 2rem;
  }

  .destinos-track-wrapper {
    overflow-x: auto;
    padding-bottom: 0.4rem;
  }

  .destinos-track {
    display: flex;
    gap: 0.9rem;
    min-width: 100%;
    scroll-snap-type: x mandatory;
  }

  .destino-card {
    scroll-snap-align: start;
    flex: 0 0 220px;
    background: #ffffff;
    border-radius: 14px;
    overflow: hidden;
    box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
    border: 1px solid #e5e7eb;
    display: flex;
    flex-direction: column;
  }

  .destino-img-wrapper {
    position: relative;
    width: 100%;
    padding-top: 62%;
    overflow: hidden;
  }

  .destino-img-wrapper img {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.25s ease-out;
  }

  .destino-card:hover .destino-img-wrapper img {
    transform: scale(1.04);
  }

  .destino-body {
    padding: 0.6rem 0.75rem 0.8rem;
  }

  .destino-name {
    font-size: 0.95rem;
    font-weight: 600;
    color: #111827;
  }

  .destino-tag {
    font-size: 0.8rem;
    color: #1e93ab;
    margin-top: 0.15rem;
  }

  .top-routes-section {
    margin-top: 2.2rem;
    margin-bottom: 2rem;
  }

  .top-routes-card {
    background: #ffffff;
    border-radius: 14px;
    padding: 1rem 1.1rem;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    border: 1px solid #e5e7eb;
  }

  .top-routes-list {
    display: flex;
    flex-direction: column;
    gap: 0.6rem;
    margin-top: 0.4rem;
  }

  .route-row {
    display: grid;
    grid-template-columns: auto 1fr auto;
    gap: 0.6rem;
    align-items: center;
  }

  @media (max-width: 640px) {
    .route-row {
      grid-template-columns: auto 1fr;
      grid-template-rows: auto auto;
    }
    .route-count {
      justify-self: flex-start;
      font-size: 0.8rem;
      margin-top: 0.15rem;
    }
  }

  .route-rank {
    font-weight: 700;
    color: #e62727;
    font-size: 0.9rem;
    min-width: 2rem;
  }

  .route-main {
    display: flex;
    flex-direction: column;
    gap: 0.2rem;
  }

  .route-title {
    font-size: 0.9rem;
    font-weight: 600;
    color: #111827;
  }

  .route-bar {
    position: relative;
    height: 6px;
    border-radius: 999px;
    background: #f3f4f6;
    overflow: hidden;
  }

  .route-bar-fill {
    position: absolute;
    inset: 0;
    width: 0;
    background: linear-gradient(90deg, #e62727, #1e93ab);
    border-radius: 999px;
    transition: width 0.25s ease-out;
  }

  .route-count {
    font-size: 0.85rem;
    color: #374151;
    font-weight: 500;
    white-space: nowrap;
  }

  .top-error {
    font-size: 0.85rem;
    color: #b00020;
    margin-top: 0.4rem;
  }

  .no-routes {
    font-size: 0.85rem;
    color: #6b7280;
    margin-top: 0.4rem;
  }

  .loading,
  .error {
    text-align: center;
    margin-top: 3rem;
  }

  .error {
    color: #b00020;
  }
</style>

<div class="home-page">
  {#if loading}
    <p class="loading">Cargando experiencia de inicio…</p>
  {:else if error}
    <p class="error">{error}</p>
  {:else}
    <section class="hero">
      <div>
        <p class="hero-greeting">
          Hola, {displayName}
          {#if $isLoggedIn}
            — tu sesión está activa.
          {:else}
            — puedes iniciar sesión para ver tus reservas.
          {/if}
        </p>

        {#if home.heroHighlight}
          <div class="hero-highlight">{home.heroHighlight}</div>
        {/if}

        <h1 class="hero-title">{home.heroTitle}</h1>

        {#if home.heroSubtitle}
          <p class="hero-subtitle">{home.heroSubtitle}</p>
        {/if}

        <div class="hero-cta">
          {#if home.ctaPrimaryLabel && home.ctaPrimaryHref}
            <a class="btn-primary" href={home.ctaPrimaryHref}>
              {home.ctaPrimaryLabel}
            </a>
          {/if}

          {#if home.ctaSecondaryLabel && home.ctaSecondaryHref}
            <a class="btn-secondary" href={home.ctaSecondaryHref}>
              {home.ctaSecondaryLabel}
            </a>
          {/if}
        </div>
      </div>
    </section>

    <section>
      <h2 class="section-title">Pasos del flujo</h2>

      <div class="steps-grid">
        {#each home.steps as step, i}
          <article class="step-card">
            <strong>{step.title || `Paso ${i + 1}`}</strong>
            <p>{step.text}</p>
          </article>
        {/each}
      </div>
    </section>

    <section>
      <h2 class="section-title">Beneficios</h2>

      <div class="benefits-grid">
        {#each home.benefits as b}
          <article class="benefit-card">
            <div class="benefit-icon">{b.icon}</div>
            <div>
              <div class="benefit-title">{b.title}</div>
              <div>{b.text}</div>
            </div>
          </article>
        {/each}
      </div>
    </section>

    {#if home.touristDestinations && home.touristDestinations.length}
      <section class="destinos-section">
        <h2 class="section-title">Algunos destinos turísticos</h2>

        <div class="destinos-track-wrapper">
          <div class="destinos-track">
            {#each home.touristDestinations as d}
              <article class="destino-card">
                <div class="destino-img-wrapper">
                  <img src={d.imageUrl} alt={`Destino turístico: ${d.name}`} loading="lazy" />
                </div>
                <div class="destino-body">
                  <div class="destino-name">{d.name}</div>
                  <div class="destino-tag">Destino destacado</div>
                </div>
              </article>
            {/each}
          </div>
        </div>
      </section>
    {/if}

    {#if !loadingTop}
      <section class="top-routes-section">
        <h2 class="section-title">Rutas más reservadas en agencias</h2>
        <p class="section-sub">
          Este ranking se basa en las compras de vuelos registradas dentro de la
          plataforma de agencias.
        </p>

        <div class="top-routes-card">
          {#if topError}
            <p class="top-error">{topError}</p>
          {:else if !topRoutes || topRoutes.length === 0}
            <p class="no-routes">
              Aún no hay reservas suficientes para mostrar un ranking de rutas.
            </p>
          {:else}
            <div class="top-routes-list">
              {#each topRoutes as r, i}
                <div class="route-row">
                  <div class="route-rank">#{i + 1}</div>
                  <div class="route-main">
                    <div class="route-title">
                      {r.origin} → {r.destination}
                    </div>
                    <div class="route-bar">
                      <div
                        class="route-bar-fill"
                        style={`width: ${Math.max(
                          8,
                          (r.count / maxCount) * 100
                        )}%`}
                      ></div>
                    </div>
                  </div>
                  <div class="route-count">
                    {r.count} {r.count === 1 ? "reserva" : "reservas"}
                  </div>
                </div>
              {/each}
            </div>
          {/if}
        </div>
      </section>
    {/if}
  {/if}
</div>