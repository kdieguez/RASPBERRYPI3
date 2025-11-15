<script>
  import { ComprasAPI, fmtDate, toDate } from '@/lib/api';
  import { link } from '@/lib/router';

  let list = [];
  let loading = true;
  let error = '';

  function estadoTexto(idEstado) {
    const n = Number(idEstado);
    if (n === 1) return 'Activa';
    if (n === 2) return 'Cancelada';
    if (n === 3) return 'Finalizada';
    return `Estado ${idEstado ?? '—'}`;
  }

  function estadoColor(idEstado) {
    const n = Number(idEstado);
    if (n === 1) return '#2e7d32';  
    if (n === 2) return '#c62828';  
    if (n === 3) return '#1565c0';  
    return '#555';
  }

  async function load() {
    loading = true;
    error = '';
    try {
      list = await ComprasAPI.list();
    } catch (e) {
      error = e.message || 'Error cargando reservas';
    } finally {
      loading = false;
    }
  }

  load();
</script>

<style>
  .reservas-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
    gap: 1rem;
    margin-top: 1rem;
  }

  .card {
    border-radius: 10px;
    padding: 1rem;
    background: white;
    border: 1px solid #eee;
    transition: box-shadow 0.15s;
  }
  .card:hover {
    box-shadow: 0px 4px 12px rgba(0,0,0,0.10);
  }

  .badge {
    padding: 3px 8px;
    border-radius: 8px;
    font-size: 12px;
    font-weight: 600;
  }
</style>

<div class="container">
  <h2>Mis reservas</h2>

  {#if loading}
    <p>Cargando…</p>
  {/if}

  {#if error}
    <p style="color:red">{error}</p>
  {/if}

  {#if !loading && list.length === 0}
    <p>No tienes reservas todavía.</p>
  {/if}

  <div class="reservas-grid">
    {#each list as r}
      <a
        class="card"
        style="text-decoration:none;color:inherit;"
        href={`/reservas/${r.idReserva}`}
        use:link
      >
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <strong>Reserva #{r.idReserva}</strong>
          <span
            class="badge"
            style={`background:${estadoColor(r.idEstado)}; color:white;`}
          >
            {estadoTexto(r.idEstado)}
          </span>
        </div>

        <div style="margin-top:0.5rem;">
          Total:
          <strong>
            {r.total != null ? Number(r.total).toFixed(2) : '—'}
          </strong>
        </div>

        <small style="color:#666">
          {fmtDate(toDate(r.creadaEn))}
        </small>
      </a>
    {/each}
  </div>
</div>