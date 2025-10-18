<script>
  import { ComprasAPI } from '@/lib/api';
  import { match } from '@/lib/router';

  let id = '';
  let det = null;
  let loading = true;
  let error = '';

  function extractId() {
    const m = match('/reservas/:id');
    id = m.ok ? m.params.id : (location.pathname.split('/').pop() || '');
  }

  async function load() {
    extractId();
    if (!id) { error = 'ID inválido'; return; }
    loading = true; error = '';
    try { det = await ComprasAPI.detail(id); }
    catch(e){ error = e.message || 'Error'; }
    finally { loading = false; }
  }

  async function descargarPdf() {
    try {
      const blob = await ComprasAPI.boletoPdf(id);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = `boleto-${id}.pdf`; a.click();
      URL.revokeObjectURL(url);
    } catch (e) { alert(e.message); }
  }

  load();
</script>

<div class="container">
  <h2>Reserva #{id}</h2>
  {#if loading}<p>Cargando…</p>{/if}
  {#if error}<p style="color:red">{error}</p>{/if}
  {#if det}
    <div class="card">
      <div style="display:flex; justify-content:space-between; align-items:center;">
        <div>Total: <strong>{det.total}</strong></div>
        <button class="btn" on:click={descargarPdf}>Descargar PDF</button>
      </div>
      <div class="hr"></div>
      {#each det.items || [] as it}
        <div style="display:flex; justify-content:space-between; gap:10px;">
          <div>
            <div><strong>{it.codigoVuelo || it.idVuelo}</strong> ({it.clase})</div>
            <small>{it.fechaSalida} → {it.fechaLlegada}</small>
            <div><small>{(it.ciudadOrigen || it.paisOrigen) ?? '—'} → {(it.ciudadDestino || it.paisDestino) ?? '—'}</small></div>
          </div>
          <div style="text-align:right">
            <div>Cant: {it.cantidad}</div>
            <div>Subtotal: {it.subtotal ?? '—'}</div>
          </div>
        </div>
        <div class="hr"></div>
      {/each}
    </div>
  {/if}
</div>