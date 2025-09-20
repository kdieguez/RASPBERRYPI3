<script>
  import { onMount } from 'svelte';
  import { AuthAPI } from '../lib/api';
  import { setAuth, isLoggedIn } from '../lib/auth';
  import { navigate, path } from '../lib/router';

  let email = '';
  let password = '';
  let loading = false;
  let error = '';
  let showPwd = false;

  function nextFromLocation() {
    let q = '';
    if (location.hash.includes('?')) {
      q = location.hash.split('?')[1];
    } else if (location.search && location.pathname.endsWith('/login')) {
      q = location.search.slice(1);
    }
    const params = new URLSearchParams(q);
    const n = params.get('next');
    return n && n.startsWith('/') ? n : '/';
  }

  onMount(() => {
    if ($isLoggedIn) navigate('/');
  });

  $: if ($isLoggedIn && $path === '/login') {
    navigate('/');
  }

  async function submit(e) {
    e.preventDefault();
    error = '';
    loading = true;
    try {
      const res = await AuthAPI.login({ email: email.trim().toLowerCase(), password });
      setAuth({ token: res.access_token, user: res.user });
      navigate(nextFromLocation());
    } catch (e) {
      error = e?.message || 'Error al iniciar sesión';
    } finally {
      loading = false;
    }
  }
</script>

<div class="container" style="display:grid;place-items:center;min-height:calc(100vh - 64px);">
  <form class="card" style="width: 420px;" on:submit|preventDefault={submit} autocomplete="on">
    <h2 style="margin:0 0 10px 0;">Inicia sesión</h2>
    <p style="margin:0;color:#9ca3af">Usa tu correo y contraseña registrados.</p>

    <div style="margin-top:12px;">
      <label class="label" for="email">Correo</label>
      <input
        id="email"
        class="input"
        type="email"
        bind:value={email}
        placeholder="tucorreo@dominio.com"
        required
        autocomplete="email"
      />
    </div>

    <div style="margin-top:12px;">
      <label class="label" for="password">Contraseña</label>
      <div style="display:flex;gap:8px;align-items:center;">
        <input
          id="password"
          class="input"
          type={showPwd ? 'text' : 'password'}
          bind:value={password}
          placeholder="••••••••"
          required
          autocomplete="current-password"
        />
        <button
          type="button"
          class="btn"
          on:click={() => (showPwd = !showPwd)}
          aria-pressed={showPwd}
          aria-label={showPwd ? 'Ocultar contraseña' : 'Mostrar contraseña'}
        >
          {showPwd ? 'Ocultar' : 'Ver'}
        </button>
      </div>
    </div>

    {#if error}
      <div class="error" role="alert" style="margin-top:8px;">{error}</div>
    {/if}

    <div style="margin-top:16px; display:flex; gap:10px;">
      <button class="btn primary" type="submit" disabled={loading || !email || !password}>
        {loading ? 'Entrando…' : 'Entrar'}
      </button>
      <button class="btn" type="reset" on:click={() => { email=''; password=''; error=''; }}>
        Limpiar
      </button>
    </div>
  </form>
</div>