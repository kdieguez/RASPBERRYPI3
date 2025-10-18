<script>
  import { ComprasAPI } from '@/lib/api';
  import { navigate } from '@/lib/router';

  let tarjeta = { numero: '', cvv: '' };
  let facturacion = { direccion: '', ciudad: '', pais: '', zip: '' };
  let loading = false;
  let error = '';

  async function pagar() {
    loading = true; error = '';
    try {
      const r = await ComprasAPI.checkout({ tarjeta, facturacion });
      alert(`Compra exitosa. Reserva #${r.idReserva}`);
      navigate(`/reservas/${r.idReserva}`);
    } catch (e) {
      error = e.message || 'Error';
    } finally {
      loading = false;
    }
  }
</script>

<div class="container">
  <h2>Checkout</h2>
  {#if error}<p style="color:red">{error}</p>{/if}

  <div class="card" style="display:grid; gap:12px; max-width:700px;">
    <div>
      <label class="label">Número de tarjeta</label>
      <input class="input" bind:value={tarjeta.numero} placeholder="4111111111111111" />
    </div>
    <div>
      <label class="label">CVV</label>
      <input class="input" bind:value={tarjeta.cvv} placeholder="123" />
    </div>

    <div class="hr"></div>

    <div><label class="label">Dirección</label><input class="input" bind:value={facturacion.direccion} /></div>
    <div class="grid grid-2">
      <div><label class="label">Ciudad</label><input class="input" bind:value={facturacion.ciudad} /></div>
      <div><label class="label">País</label><input class="input" bind:value={facturacion.pais} /></div>
    </div>
    <div><label class="label">ZIP</label><input class="input" bind:value={facturacion.zip} /></div>

    <div><button class="btn" disabled={loading} on:click={pagar}>{loading ? 'Procesando…' : 'Pagar'}</button></div>
  </div>
</div>