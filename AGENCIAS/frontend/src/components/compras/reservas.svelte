<script>
  import { ComprasAPI } from '@/lib/api';
  import { link } from '@/lib/router';

  let list = [];
  let loading = true;
  let error = '';

  async function load() {
    loading = true; error = '';
    try { list = await ComprasAPI.list(); }
    catch(e) { error = e.message || 'Error'; }
    finally { loading = false; }
  }
  load();
</script>

<div class="container">
  <h2>Mis reservas</h2>
  {#if loading}<p>Cargando…</p>{/if}
  {#if error}<p style="color:red">{error}</p>{/if}

  <div class="grid">
    {#each list as r}
      <a class="card" href={`/reservas/${r.idReserva}`} use:link style="text-decoration:none;color:inherit;">
        <div style="display:flex; justify-content:space-between;">
          <strong>#{r.idReserva}</strong>
          <span class="badge">Estado {r.idEstado}</span>
        </div>
        <div>Total: <strong>{r.total ?? '—'}</strong></div>
        <small>{r.creadaEn}</small>
      </a>
    {/each}
  </div>
</div>