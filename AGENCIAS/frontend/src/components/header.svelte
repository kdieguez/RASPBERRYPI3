<script>
  import { createEventDispatcher } from 'svelte';
  import { isLoggedIn, user, logout } from '../lib/auth';

  export let current = 'home';

  const dispatch = createEventDispatcher();
  const gotoHome = () => dispatch('gotoHome');
  const gotoRegister = () => dispatch('gotoRegister');
  const gotoLogin = () => dispatch('gotoLogin');
  const gotoUsers = () => dispatch('gotoUsers');

  $: isStaff = ['admin', 'empleado'].includes(($user?.rol || '').toLowerCase());
</script>

<header class="header">
  <div class="wrap">
    <button class="brand" on:click={gotoHome} aria-label="Ir al inicio">Agencias</button>

    <nav class="nav" aria-label="Navegación principal" style="display:flex;gap:8px;align-items:center;">
      <button
        type="button"
        class="btn {current === 'home' ? 'primary' : 'ghost'}"
        aria-current={current === 'home' ? 'page' : undefined}
        on:click={gotoHome}
      >
        Inicio
      </button>

      {#if $isLoggedIn}
        {#if isStaff}
          <button
            type="button"
            class="btn {current === 'users' ? 'primary' : ''}" 
            aria-current={current === 'users' ? 'page' : undefined}
            on:click={gotoUsers}
          >
            Usuarios
          </button>
        {/if}

        <span style="opacity:.85">Hola, {$user?.nombres || $user?.email}</span>
        <button type="button" class="btn danger" on:click={logout}>Salir</button>
      {:else}
        <button
          type="button"
          class="btn {current === 'register' ? 'primary' : ''}"
          aria-current={current === 'register' ? 'page' : undefined}
          on:click={gotoRegister}
        >
          Registrarme
        </button>

        <button
          type="button"
          class="btn {current === 'login' ? 'primary' : ''}"
          aria-current={current === 'login' ? 'page' : undefined}
          on:click={gotoLogin}
        >
          Login
        </button>
      {/if}
    </nav>
  </div>
</header>

<style>
  .header { display:flex; border-bottom: 1px solid #1f2937; background: rgba(17,24,39,.6); backdrop-filter: blur(6px); position: sticky; top: 0; z-index:10; }
  .wrap { max-width: 960px; margin: 0 auto; width: 100%; padding: 12px 16px; display:flex; align-items:center; justify-content:space-between; }
  .brand { font-weight: 600; letter-spacing: .4px; cursor:pointer; background:none; border:none; color:inherit; padding:0; }
  .btn { border: 1px solid #1f2937; background: #0b1020; color: #e5e7eb; padding: 8px 12px; border-radius: 10px; cursor: pointer; }
  .btn.primary { border-color: #0b3c46; background: #0b2a32; }
  .btn.ghost { background: transparent; }
  .btn.danger { border-color: #4b0b0b; background: #2a0b0b; color: #ffd6d6; }
</style>