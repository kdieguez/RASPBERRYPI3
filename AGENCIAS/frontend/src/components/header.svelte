<script>
  import { onMount } from "svelte";
  import { isLoggedIn, user, logout } from "@/lib/auth";
  import { fetchUI } from "@/lib/portalApi";
  import { navigate, link } from "@/lib/router";

  export let current = "home";

  let ui = { header: {} };
  let q = "";

  onMount(async () => {
    try {
      ui = await fetchUI();
    } catch {}
  });

  $: isStaff = ["admin", "empleado"].includes(
    ($user?.rol || "").toLowerCase()
  );
  $: isAdmin = ($user?.rol || "").toLowerCase() === "admin";

  function goSearch(e) {
    e.preventDefault();
    navigate(`/vuelos?q=${encodeURIComponent(q)}`);
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
          <input placeholder="Buscar vuelos…" bind:value={q} />
          <button type="submit" class="btn btn-small">Buscar</button>
        </form>
      {/if}

      <!-- Navegación principal -->
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
      </div>

      <!-- Zona admin / staff -->
      {#if $isLoggedIn && isStaff}
        <div class="nav-group nav-admin">
          <a
            class="btn {current === 'users' ? 'primary' : 'ghost'}"
            aria-current={current === 'users' ? 'page' : undefined}
            href="/admin/users"
            use:link
          >
            Usuarios
          </a>

          {#if isAdmin}
            <a
              class="btn {current === 'admin-reservas' ? 'primary' : 'ghost'}"
              aria-current={current === 'admin-reservas' ? 'page' : undefined}
              href="/admin/reservas"
              use:link
            >
              Reservas (admin)
            </a>

            <a
              class="btn {current === 'admin-paginas' ? 'primary' : 'ghost'}"
              aria-current={current === 'admin-paginas' ? 'page' : undefined}
              href="/admin/paginas/cancelacion-compras"
              use:link
            >
              Páginas
            </a>

            <a
              class="btn {current === 'portal' ? 'primary' : 'ghost'}"
              aria-current={current === 'portal' ? 'page' : undefined}
              href="/admin/portal"
              use:link
            >
              Portal
            </a>
          {/if}
        </div>
      {/if}

      <!-- Zona usuario / login -->
      <div class="nav-group nav-user">
        {#if ui.header?.show_cart}
          <a class="btn ghost" href="/carrito" use:link>
            Carrito
          </a>
        {/if}

        {#if $isLoggedIn}
          <span class="user-label">
            Hola, {$user?.nombres || $user?.email}
          </span>
          <button type="button" class="btn danger" on:click={logout}>
            Salir
          </button>
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
    display: flex;
    align-items: center;
    justify-content: space-between;
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
    flex: 1;
    justify-content: flex-end;
    flex-wrap: wrap;
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
    margin-left: auto;
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
  .btn.danger {
    background: #2a0b0b;
    color: #ffd6d6;
    border-color: #2a0b0b;
  }

  .search {
    display: flex;
    gap: 6px;
    align-items: center;
    margin-right: 4px;
  }
  .search input {
    padding: 7px 10px;
    border: 1px solid #c7c7c7;
    border-radius: 10px;
    min-width: 200px;
  }

  .user-label {
    opacity: 0.9;
    font-size: 0.88rem;
  }

  @media (max-width: 768px) {
    .wrap {
      flex-direction: column;
      align-items: flex-start;
    }
    .nav {
      width: 100%;
      justify-content: flex-start;
    }
    .nav-user {
      margin-left: 0;
    }
  }
</style>