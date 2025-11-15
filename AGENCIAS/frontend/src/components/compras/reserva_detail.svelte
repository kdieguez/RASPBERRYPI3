<script>
  import { ComprasAPI } from '@/lib/api';
  import { match } from '@/lib/router';

  let id = '';
  let det = null;
  let loading = true;
  let error = '';

  let cancelLoading = false;
  let cancelError = '';
  let cancelSuccess = '';

  function extractId() {
    const m = match('/reservas/:id');
    id = m.ok ? m.params.id : (location.pathname.split('/').pop() || '');
  }

  async function load() {
    extractId();
    if (!id) {
      error = 'ID inválido';
      return;
    }
    loading = true;
    error = '';
    cancelError = '';
    cancelSuccess = '';
    try {
      det = await ComprasAPI.detail(id);
    } catch (e) {
      error = e.message || 'Error';
    } finally {
      loading = false;
    }
  }

  async function descargarPdf() {
    try {
      const blob = await ComprasAPI.boletoPdf(id);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `boleto-${id}.pdf`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      alert(e.message);
    }
  }

  async function cancelarReserva() {
    if (!det || !id) return;
    cancelLoading = true;
    cancelError = '';
    cancelSuccess = '';
    try {
      await ComprasAPI.cancel(id);
      det = { ...det, idEstado: 2 };
      cancelSuccess = 'La reserva se canceló correctamente.';
    } catch (e) {
      cancelError = e.message || 'No se pudo cancelar la reserva.';
    } finally {
      cancelLoading = false;
    }
  }

  function estadoTexto(idEstado) {
    const v = Number(idEstado);
    if (v === 1) return 'Activa';
    if (v === 2) return 'Cancelada';
    if (v === 3) return 'Finalizada';
    return `Estado ${idEstado ?? '—'}`;
  }

  load();
</script>

<div class="container">
  <h2>Reserva #{id}</h2>
  {#if loading}<p>Cargando…</p>{/if}
  {#if error}<p style="color:red">{error}</p>{/if}

  {#if det}
    <div class="card">
      <div style="display:flex; justify-content:space-between; align-items:center; gap: 1rem;">
        <div>
          <div>
            Total:
            <strong>
              {det.total != null ? Number(det.total).toFixed(2) : '—'}
            </strong>
          </div>
          <div>
            <small>
              Estado:
              <strong>{estadoTexto(det.idEstado)}</strong>
            </small>
          </div>
          {#if det.creadaEn}
            <div><small>Creada: {det.creadaEn}</small></div>
          {/if}
        </div>

        <div style="display:flex; flex-direction:column; gap:0.5rem; align-items:flex-end;">
          <button class="btn" on:click={descargarPdf}>
            Descargar PDF
          </button>

          {#if det.idEstado === 1}
            <button
              class="btn"
              style="background:#c62828;color:white"
              disabled={cancelLoading}
              on:click={cancelarReserva}
            >
              {cancelLoading ? 'Cancelando…' : 'Cancelar reserva'}
            </button>
          {:else}
            <span class="badge">Esta reserva ya no se puede cancelar</span>
          {/if}
        </div>
      </div>

      {#if cancelError}
        <p style="color:red; margin-top:0.5rem;">{cancelError}</p>
      {/if}
      {#if cancelSuccess}
        <p style="color:green; margin-top:0.5rem;">{cancelSuccess}</p>
      {/if}

      <div class="hr"></div>

      {#each det.items || [] as it}
        <div style="display:flex; justify-content:space-between; gap:10px;">
          <div>
            <div>
              <strong>{it.codigoVuelo || it.idVuelo}</strong> ({it.clase})
            </div>
            <small>{it.fechaSalida} → {it.fechaLlegada}</small>
            <div>
              <small>
                {(it.ciudadOrigen || it.paisOrigen) ?? '—'}
                →
                {(it.ciudadDestino || it.paisDestino) ?? '—'}
              </small>
            </div>
          </div>
          <div style="text-align:right">
            <div>Cant: {it.cantidad}</div>
            <div>
              Subtotal:
              {it.subtotal != null ? Number(it.subtotal).toFixed(2) : '—'}
            </div>
          </div>
        </div>
        <div class="hr"></div>
      {/each}
    </div>
  {/if}
</div>