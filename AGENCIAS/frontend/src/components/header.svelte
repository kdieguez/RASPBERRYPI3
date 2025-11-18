<script>
  import { onMount } from "svelte";
  import { isLoggedIn, user, logout } from "@/lib/auth";
  import { fetchUI } from "@/lib/portalApi";
  import { ProveedoresAPI } from "@/lib/api";
  import { navigate, link } from "@/lib/router";

  export let current = "home";

  let ui = { header: {} };

  let q = "";
  let priceMin = "";
  let priceMax = "";
  let proveedor = "";
  let tipoVuelo = "";  
  let ratingMin = "";  

  let proveedores = [];

  let showProfileMenu = false;

  onMount(async () => {
    try {
      ui = await fetchUI();
    } catch {}

    try {
      proveedores = await ProveedoresAPI.listPublic();
    } catch (e) {
      console.error("No se pudieron cargar proveedores", e);
    }
  });

  $: isStaff = ["admin", "empleado"].includes(
    ($user?.rol || "").toLowerCase()
  );
  $: isAdmin = ($user?.rol || "").toLowerCase() === "admin";

  function goSearch(e) {
    e.preventDefault();

    const params = new URLSearchParams();

    if (q && q.trim()) params.set("q", q.trim());
    if (priceMin) params.set("priceMin", String(priceMin));
    if (priceMax) params.set("priceMax", String(priceMax));
    if (proveedor) params.set("proveedor", String(proveedor));
    if (tipoVuelo) params.set("tipoVuelo", tipoVuelo);
    if (ratingMin) params.set("ratingMin", String(ratingMin));

    const qs = params.toString();
    const url = "/vuelos" + (qs ? `?${qs}` : "");

    navigate(url);
  }

  function toggleProfileMenu() {
    if (!$isLoggedIn) return;
    showProfileMenu = !showProfileMenu;
  }

  function goPerfil() {
    showProfileMenu = false;
    navigate("/perfil");
  }

  function goReservas() {
    showProfileMenu = false;
    navigate("/reservas");
  }

  function doLogout() {
    showProfileMenu = false;
    logout();
  }

  function initials(u) {
    if (!u) return "U";
    const name = (u.nombres || u.email || "U").toString().trim();
    return name.charAt(0).toUpperCase();
  }
</script>

<header class="header">
  <div class="wrap">
    <a class="brand" href="/" use:link aria-label="Ir al inicio">
      {#if ui.header?.logo_url}
        <img src={ui.header.logo_url} alt="logo" />
      {/if}
      <span>{ui.header?.title || "Agencias"}</span>
    </a>

    <nav class="nav">
      {#if ui.header?.show_search}
        <form class="search" on:submit={goSearch}>
          <input
            class="search-text"
            placeholder="Origen, destino o código..."
            bind:value={q}
          />

          <input
            class="search-number"
            type="number"
            min="0"
            placeholder="Precio mín."
            bind:value={priceMin}
          />

          <input
            class="search-number"
            type="number"
            min="0"
            placeholder="Precio máx."
            bind:value={priceMax}
          />

          <select class="search-select" bind:value={proveedor}>
            <option value="">Cualquier aerolínea</option>
            {#each proveedores as p}
              <option value={p.id}>{p.nombre}</option>
            {/each}
          </select>

          <select class="search-select" bind:value={tipoVuelo}>
            <option value="">Tipo de vuelo</option>
            <option value="directo">Solo directos</option>
            <option value="escala">Con escala</option>
          </select>

          <select class="search-select" bind:value={ratingMin}>
            <option value="">Rating mínimo</option>
            <option value="1">★ 1+</option>
            <option value="2">★ 2+</option>
            <option value="3">★ 3+</option>
            <option value="4">★ 4+</option>
            <option value="5">★ 5</option>
          </select>

          <button type="submit" class="btn btn-small primary">
            Buscar
          </button>
        </form>
      {/if}

      <div class="nav-group nav-main">
        <a
          class="btn {current === 'home' ? 'primary' : 'ghost'}"
          aria-current={current === 'home' ? 'page' : undefined}
          href="/"
          use:link
        >
          Inicio
        </a>

        <a
          class="btn {current === 'vuelos' ? 'primary' : 'ghost'}"
          aria-current={current === 'vuelos' ? 'page' : undefined}
          href="/vuelos"
          use:link
        >
          Vuelos
        </a>

        {#if $isLoggedIn}
          <a
            class="btn {current === 'reservas' ? 'primary' : 'ghost'}"
            aria-current={current === 'reservas' ? 'page' : undefined}
            href="/reservas"
            use:link
          >
            Mis reservas
          </a>
        {/if}

        <a
          class="btn {current === 'info-cancelacion' ? 'primary' : 'ghost'}"
          aria-current={current === 'info-cancelacion' ? 'page' : undefined}
          href="/informacion/cancelacion-compras"
          use:link
        >
          Cancelación de compras
        </a>

        <a
          class="btn {current === 'info-aerolineas' ? 'primary' : 'ghost'}"
          aria-current={current === 'info-aerolineas' ? 'page' : undefined}
          href="/informacion/aerolineas-afiliadas"
          use:link
        >
          Aerolíneas afiliadas
        </a>
      </div>

      {#if $isLoggedIn && isStaff}
        <div class="nav-group nav-admin">
          <details class="admin-menu">
            <summary class="btn ghost">
              Admin ▾
            </summary>
            <div class="admin-menu-items">
              <a
                class:active={current === 'users'}
                href="/admin/users"
                use:link
              >
                Usuarios
              </a>

              {#if isAdmin}
                <a
                  class:active={current === 'admin-reservas'}
                  href="/admin/reservas"
                  use:link
                >
                  Reservas
                </a>

                <a
                  class:active={current === 'admin-paginas'}
                  href="/admin/paginas/cancelacion-compras"
                  use:link
                >
                  Páginas informativas
                </a>

                <a
                  class:active={current === 'admin-proveedores'}
                  href="/admin/proveedores"
                  use:link
                >
                  Aerolíneas afiliadas
                </a>

                <a
                  class:active={current === 'portal'}
                  href="/admin/portal"
                  use:link
                >
                  Portal (UI)
                </a>
              {/if}
            </div>
          </details>
        </div>
      {/if}

      <div class="nav-group nav-user">
        {#if ui.header?.show_cart}
          <a class="btn ghost" href="/carrito" use:link>
            Carrito
          </a>
        {/if}

        {#if $isLoggedIn}
          <div class="profile-wrapper">
            <button
              type="button"
              class="profile-icon"
              on:click={toggleProfileMenu}
              aria-label="Menú de usuario"
            >
              <span>{initials($user)}</span>
            </button>

            {#if showProfileMenu}
              <div class="profile-menu">
                <div class="profile-menu-header">
                  <div class="profile-name">
                    {$user?.nombres || $user?.email}
                  </div>
                  {#if $user?.email}
                    <div class="profile-email">{$user.email}</div>
                  {/if}
                </div>

                <button type="button" on:click={goPerfil}>
                  Editar perfil
                </button>
                <button type="button" on:click={goReservas}>
                  Mis reservaciones
                </button>
                <hr />
                <button type="button" on:click={doLogout}>
                  Cerrar sesión
                </button>
              </div>
            {/if}
          </div>
        {:else}
          <a
            class="btn {current === 'register' ? 'primary' : 'ghost'}"
            aria-current={current === 'register' ? 'page' : undefined}
            href="/register"
            use:link
          >
            Registrarme
          </a>

          <a
            class="btn {current === 'login' ? 'primary' : 'ghost'}"
            aria-current={current === 'login' ? 'page' : undefined}
            href="/login"
            use:link
          >
            Login
          </a>
        {/if}
      </div>
    </nav>
  </div>
</header>

<style>
  .header {
    display: flex;
    border-bottom: 1px solid #b04c6f;
    background: #d26e91;
    position: sticky;
    top: 0;
    z-index: 10;
  }

  .wrap {
    max-width: 960px;
    margin: 0 auto;
    width: 100%;
    padding: 12px 16px;
    display: grid;
    grid-template-columns: auto 1fr auto;
    align-items: center;
    gap: 12px;
  }

  .brand {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
    color: inherit;
    text-decoration: none;
    white-space: nowrap;
  }
  .brand img {
    height: 28px;
    border-radius: 6px;
  }

  .nav {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
    justify-content: flex-end;
  }

  .search {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    align-items: center;
    justify-content: flex-start;
    max-width: 100%;
  }
  .search-text {
    min-width: 200px;
    flex: 1 1 220px;
  }
  .search-number,
  .search-select {
    flex: 0 0 auto;
    min-width: 110px;
  }

  .search input,
  .search select {
    padding: 7px 10px;
    border: 1px solid #c7c7c7;
    border-radius: 10px;
    font-size: 0.85rem;
  }

  .nav-group {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }

  .nav-admin {
    border-left: 1px solid rgba(255, 255, 255, 0.4);
    padding-left: 8px;
  }

  .nav-user {
    gap: 8px;
  }

  .btn {
    border: 1px solid #1f2937;
    background: #0b1020;
    color: #e5e7eb;
    padding: 8px 12px;
    border-radius: 10px;
    text-decoration: none;
    display: inline-block;
    font-size: 0.9rem;
    white-space: nowrap;
  }
  .btn.btn-small {
    padding: 6px 10px;
    font-size: 0.8rem;
  }
  .btn.ghost {
    background: transparent;
  }
  .btn.primary {
    background: #277b29;
    color: #fff;
  }

  .admin-menu {
    position: relative;
  }
  .admin-menu summary {
    list-style: none;
    cursor: pointer;
  }
  .admin-menu summary::-webkit-details-marker {
    display: none;
  }
  .admin-menu-items {
    position: absolute;
    right: 0;
    top: 110%;
    background: #fff;
    color: #111827;
    border-radius: 10px;
    box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
    padding: 0.4rem 0.6rem;
    min-width: 220px;
    display: flex;
    flex-direction: column;
    gap: 0.2rem;
    z-index: 20;
  }
  .admin-menu-items a {
    padding: 0.25rem 0.4rem;
    border-radius: 6px;
    text-decoration: none;
    color: inherit;
    font-size: 0.85rem;
  }
  .admin-menu-items a:hover {
    background: #f3f4f6;
  }
  .admin-menu-items a.active {
    background: #d1fae5;
    font-weight: 600;
  }

  .profile-wrapper {
    position: relative;
  }

  .profile-icon {
    width: 36px;
    height: 36px;
    border-radius: 999px;
    border: none;
    background: #1E93AB;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #ffffff;
    font-weight: 700;
    cursor: pointer;
    padding: 0;
  }

  .profile-icon span {
    font-size: 0.95rem;
  }

  .profile-menu {
    position: absolute;
    right: 0;
    top: 120%;
    background: #ffffff;
    color: #111827;
    border-radius: 10px;
    box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
    padding: 0.5rem 0.75rem;
    min-width: 220px;
    z-index: 30;
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
  }

  .profile-menu-header {
    padding-bottom: 0.4rem;
    margin-bottom: 0.4rem;
    border-bottom: 1px solid #e5e7eb;
  }

  .profile-name {
    font-weight: 600;
    font-size: 0.9rem;
  }

  .profile-email {
    font-size: 0.8rem;
    color: #6b7280;
  }

  .profile-menu button {
    background: transparent;
    border: none;
    text-align: left;
    padding: 0.25rem 0.2rem;
    font-size: 0.86rem;
    cursor: pointer;
    border-radius: 6px;
  }

  .profile-menu button:hover {
    background: #f3f4f6;
  }

  @media (max-width: 768px) {
    .wrap {
      grid-template-columns: 1fr;
      align-items: flex-start;
    }
    .nav {
      width: 100%;
      justify-content: flex-start;
    }
  }
</style>