<script>
  import { ComprasAPI } from '@/lib/api';
  import { navigate } from '@/lib/router';

  let loading = true;
  let error = '';
  let cart = null;

  async function load() {
    loading = true; error = '';
    try {
      cart = await ComprasAPI.getCart();
    } catch (e) {
      error = e?.message || 'No se pudo cargar el carrito';
    } finally {
      loading = false;
    }
  }

  async function updateQty(it, qty) {
    const cantidad = Math.max(1, Number(qty || 1));
    try {
      await ComprasAPI.updateQty(it.idItem, cantidad, false);
      await load();
    } catch (e) {
      alert(e?.message || 'No se pudo actualizar la cantidad');
    }
  }

  async function removeItem(it) {
    if (!confirm('¿Eliminar este ítem?')) return;
    try {
      await ComprasAPI.removeItem(it.idItem, false);
      await load();
    } catch (e) {
      alert(e?.message || 'No se pudo eliminar el ítem');
    }
  }

  function goCheckout() {
    navigate('/checkout');
  }

  load();
</script>

<div class="container">
  <h2>Carrito</h2>
  {#if loading}<p>Cargando…</p>{/if}
  {#if error}<p class="error">{error}</p>{/if}

  {#if cart && cart.items && cart.items.length}
    {#each cart.items as it}
      <div class="card" style="margin-bottom:10px;">
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <div>
            <strong>{it.codigoVuelo || it.idVuelo}</strong> ({it.clase || `Clase ${it.idClase}`})
            <div><small>{it.fechaSalida} → {it.fechaLlegada}</small></div>
            <div><small>{(it.ciudadOrigen || it.paisOrigen) ?? '—'} → {(it.ciudadDestino || it.paisDestino) ?? '—'}</small></div>
          </div>
          <button class="btn" on:click={() => removeItem(it)}>Eliminar</button>
        </div>

        <div style="display:flex; gap:8px; align-items:center; margin-top:8px;">
          <label>Cantidad</label>
          <input class="input" type="number" min="1" value={it.cantidad}
                 on:change={(e)=>updateQty(it, e.target.value)} />
          <div style="margin-left:auto">Subtotal: <strong>{it.subtotal ?? '—'}</strong></div>
        </div>
      </div>
    {/each}

    <div style="display:flex; justify-content:flex-end; margin-top:12px;">
      <div class="card">
        Total: <strong>{cart.total ?? '—'}</strong>
        <div style="margin-top:8px">
          <button class="btn" on:click={goCheckout}>Continuar a pago</button>
        </div>
      </div>
    </div>
  {:else if !loading}
    <div class="card">Tu carrito está vacío.</div>
  {/if}
</div>

<style>
  .error { color:#E62727; }
</style>