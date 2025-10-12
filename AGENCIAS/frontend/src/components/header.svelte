<script>
  import { onMount } from 'svelte';
  import { isLoggedIn, user, logout } from '@/lib/auth';
  import { fetchUI } from '@/lib/portalApi';
  import { navigate, link } from '@/lib/router';  // <- usa navigate para el buscador y link como acción

  export let current = 'home';

  let ui = { header: {} };
  let q = '';

  onMount(async () => { try { ui = await fetchUI(); } catch {} });

  $: isStaff = ['admin', 'empleado'].includes(($user?.rol || '').toLowerCase());
  $: isAdmin = (($user?.rol || '').toLowerCase() === 'admin');

  function goSearch(e) {
    e.preventDefault();
    navigate(`/vuelos?q=${encodeURIComponent(q)}`);
  }
</script>

<header class="header">
  <div class="wrap">
    <a class="brand" href="/" use:link aria-label="Ir al inicio">
      {#if ui.header?.logo_url}<img src={ui.header.logo_url} alt="logo" />{/if}
      <span>{ui.header?.title || 'Agencias'}</span>
    </a>

    <nav class="nav" style="display:flex;gap:8px;align-items:center;">
      {#if ui.header?.show_search}
        <form class="search" on:submit={goSearch}>
          <input placeholder="Buscar vuelos…" bind:value={q} />
          <button type="submit" class="btn">Buscar</button>
        </form>
      {/if}

      {#if ui.header?.show_cart}
        <a class="btn ghost" href="/carrito" use:link>Carrito</a>
      {/if}

      <a class="btn {current === 'home' ? 'primary' : 'ghost'}"
         aria-current={current === 'home' ? 'page' : undefined}
         href="/" use:link>Inicio</a>

      <a class="btn {current === 'vuelos' ? 'primary' : 'ghost'}"
         aria-current={current === 'vuelos' ? 'page' : undefined}
         href="/vuelos" use:link>Vuelos</a>

      {#if $isLoggedIn}
        {#if isStaff}
          <a class="btn {current === 'users' ? 'primary' : 'ghost'}"
             aria-current={current === 'users' ? 'page' : undefined}
             href="/admin/users" use:link>Usuarios</a>
        {/if}
        {#if isAdmin}
          <a class="btn {current === 'portal' ? 'primary' : 'ghost'}"
             aria-current={current === 'portal' ? 'page' : undefined}
             href="/admin/portal" use:link>Portal</a>
        {/if}
        <span style="opacity:.85">Hola, {$user?.nombres || $user?.email}</span>
        <button type="button" class="btn danger" on:click={logout}>Salir</button>
      {:else}
        <a class="btn {current === 'register' ? 'primary' : 'ghost'}"
           aria-current={current === 'register' ? 'page' : undefined}
           href="/register" use:link>Registrarme</a>

        <a class="btn {current === 'login' ? 'primary' : 'ghost'}"
           aria-current={current === 'login' ? 'page' : undefined}
           href="/login" use:link>Login</a>
      {/if}
    </nav>
  </div>
</header>

<style>
  .header { display:flex; border-bottom: 1px solid #b04c6f; background: #d26e91; position: sticky; top: 0; z-index:10; }
  .wrap { max-width: 960px; margin: 0 auto; width: 100%; padding: 12px 16px; display:flex; align-items:center; justify-content:space-between; gap: 12px; }
  .brand { display:flex; align-items:center; gap:8px; font-weight: 600; color:inherit; text-decoration: none; }
  .brand img { height: 28px; border-radius: 6px; }

  .btn { border: 1px solid #1f2937; background: #0b1020; color: #e5e7eb; padding: 8px 12px; border-radius: 10px; text-decoration: none; display:inline-block; }
  .btn.ghost { background: transparent; }
  .btn.primary { background: #277b29; color:#fff; }
  .btn.danger { background: #2a0b0b; color:#ffd6d6; }
  .search { display:flex; gap:6px; align-items:center; margin-right:6px; }
  .search input { padding: 7px 10px; border: 1px solid #c7c7c7; border-radius: 10px; min-width: 200px; }
</style>
