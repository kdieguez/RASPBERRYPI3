<script>
  import { onMount } from 'svelte';
  import { auth } from '../lib/auth';
  import { AuthAPI } from '../lib/api';

  let ready = false;

  onMount(async () => {
    try {
      let parsed = null;
      try {
        const raw = localStorage.getItem('auth');
        parsed = raw ? JSON.parse(raw) : null;
      } catch {}

      if (parsed?.token && !parsed?.user) {
        try {
          const me = await AuthAPI.me();
          const next = { token: parsed.token, user: me };
          localStorage.setItem('auth', JSON.stringify(next));
          auth.set(next);
        } catch {
          localStorage.removeItem('auth');
          auth.set(null);
        }
      }
    } finally {
      ready = true;
    }
  });
</script>

{#if !ready}
  <div class="container" style="min-height:30vh;display:grid;place-items:center;">
    <div class="card">Cargando…</div>
  </div>
{:else}
  {#if $auth?.token && $auth?.user}
    <slot />
  {:else}
    <slot name="guest" />
  {/if}
{/if}