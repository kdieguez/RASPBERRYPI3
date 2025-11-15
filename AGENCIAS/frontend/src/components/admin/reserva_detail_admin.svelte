<script>
  import { ComprasAPI, UsersAPI } from '@/lib/api';
  import { match } from '@/lib/router';

  let id = '';
  let det = null;
  let loading = true;
  let error = '';

  let cancelLoading = false;
  let cancelError = '';
  let cancelSuccess = '';

  // info del usuario dueño de la reserva
  let userInfo = null;
  let userLoading = false;

  function extractId() {
    const m = match('/admin/reservas/:id');
    id = m.ok ? m.params.id : (location.pathname.split('/').pop() || '');
  }

  async function loadUser() {
    userInfo = null;
    if (!det?.idUsuario) return;

    userLoading = true;
    try {
      userInfo = await UsersAPI.get(det.idUsuario);
    } catch (e) {
      console.error('No se pudo cargar info de usuario', e);
    } finally {
      userLoading = false;
    }
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
      det = await ComprasAPI.detailAdmin(id);
      await loadUser();
    } catch (e) {
      error = e.message || 'Error cargando detalle';
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

  // 👇 nombre correcto y el mismo que usamos en el template
  async function cancelarReserva() {
    if (!det || !id) return;
    cancelLoading = true;
    cancelError = '';
    cancelSuccess = '';
    try {
      await ComprasAPI.cancelAdmin(id);
      det = { ...det, idEstado: 2 };
      cancelSuccess = 'La reserva se canceló correctamente (admin).';
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
  <h2>Reserva #{id} (admin)</h2>
  {#if loading}<p>Cargando…</p>{/if}
  {#if error}<p style="color:red">{error}</p>{/if}

  {#if det}
    <div class="card">
      <div style="display:flex; justify-content:space-between; align-items:center; gap: 1rem;">
        <div>
          <div>
            <strong>Usuario:</strong>
            {#if userLoading}
              <span> Cargando…</span>
            {:else if userInfo}
              <span>{userInfo.nombres || userInfo.email || det.idUsuario}</span>
            {:else}
              <span>{det.idUsuario}</span>
            {/if}
          </div>

          {#if userInfo?.email}
            <div><small>Email: {userInfo.email}</small></div>
          {/if}

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
              {cancelLoading ? 'Cancelando…' : 'Cancelar reserva (admin)'}
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

<style>
  .badge {
    padding: 3px 8px;
    border-radius: 8px;
    font-size: 12px;
    font-weight: 600;
  }
</style>