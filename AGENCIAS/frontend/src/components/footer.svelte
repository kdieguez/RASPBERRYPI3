<script>
  import { onMount } from 'svelte';
  import { navigate } from '../lib/router';
  import { fetchUI } from '../lib/portalApi';

  let ui = { footer: {} };
  let el;
  const year = new Date().getFullYear();

  function adjustPadding() {
    if (!el) return;
    const h = el.getBoundingClientRect().height || 0;
    document.body.style.paddingBottom = `${h}px`;
  }

  onMount(() => {
    (async () => {
      try { ui = await fetchUI(); } catch {}
      adjustPadding();
    })();

    const ro = new ResizeObserver(adjustPadding);
    ro.observe(el);

    window.addEventListener('resize', adjustPadding);
    window.addEventListener('orientationchange', adjustPadding);

    return () => {
      ro.disconnect();
      window.removeEventListener('resize', adjustPadding);
      window.removeEventListener('orientationchange', adjustPadding);
      document.body.style.paddingBottom = '';
    };
  });
</script>

<footer bind:this={el} class="footer">
  <div class="wrap">
    <div class="col brand">
      {#if ui.footer?.logo_url}
        <img src={ui.footer.logo_url} alt="logo" />
      {/if}
      <div class="muted">
        {ui.footer?.copyright || `© ${year} Agencia · Hecho con ✈️`}
      </div>
    </div>

    <nav class="col nav">
      <a href="#/" class="link" on:click|preventDefault={() => navigate('/')}>Inicio</a>
      <a href="#/register" class="link" on:click|preventDefault={() => navigate('/register')}>Registrarme</a>

      {#each (ui.footer?.legal_links || []) as l}
        <a class="link" href={l.href}>{l.label}</a>
      {/each}
    </nav>

    <div class="col info">
      <div><strong>Teléfono:</strong> {ui.footer?.phone || '-'}</div>
      <div><strong>Dirección:</strong> {ui.footer?.address || '-'}</div>
    </div>
  </div>
</footer>

<style>
  .footer{
    position: fixed;
    left: 0; right: 0; bottom: 0;
    z-index: 5;
    background: #0b1020;
    color: #e5e7eb;
    border-top: 1px solid #1f2937;
  }
  .wrap{
    max-width: 960px;
    margin: 0 auto;
    padding: 14px 16px;
    display: grid;
    grid-template-columns: 2fr 1fr 1fr;
    gap: 16px;
    align-items: center;
  }
  .col{min-width: 0}
  .brand{display:flex; align-items:center; gap:10px}
  .brand img{height: 36px; border-radius: 8px}
  .muted{opacity:.85; font-size:.92rem}

  .nav{display:flex; gap:12px; justify-content:flex-start; flex-wrap:wrap}
  .link{
    color:#9ed6ff; text-decoration:none; font-size:.95rem;
    border: 1px solid transparent; padding: 4px 8px; border-radius: 8px;
  }
  .link:hover{ border-color:#1E93AB; }

  .info{font-size:.95rem; text-align:right}
  .info strong{font-weight:600}

  @media (max-width: 720px){
    .wrap{grid-template-columns: 1fr; gap:10px}
    .info{text-align:left}
  }
</style>