<script>
  import { ComprasAPI } from '@/lib/api';
  import { navigate } from '@/lib/router';

  let tarjeta = { numero: '', cvv: '' };
  let facturacion = { direccion: '', ciudad: '', pais: '', zip: '' };
  let loading = false;
  let error = '';

  function luhnOk(num) {
    const s = (num || '').replace(/\s+/g, '');
    if (!/^\d{12,19}$/.test(s)) return false;
    let sum = 0, alt = false;
    for (let i = s.length - 1; i >= 0; i--) {
      let n = parseInt(s[i], 10);
      if (alt) { n *= 2; if (n > 9) n -= 9; }
      sum += n; alt = !alt;
    }
    return sum % 10 === 0;
  }

  function normalize() {
    tarjeta.numero = (tarjeta.numero || '').replace(/\s+/g, '');
    tarjeta.cvv = (tarjeta.cvv || '').trim();
    facturacion.direccion = (facturacion.direccion || '').trim();
    facturacion.ciudad = (facturacion.ciudad || '').trim();
    facturacion.pais = (facturacion.pais || '').trim();
    facturacion.zip = (facturacion.zip || '').trim();
  }

  function showErrorFrom(e) {
    try {
      if (e?.response?.data) {
        const d = e.response.data;
        if (typeof d === 'string') return d;
        if (d.detail) return typeof d.detail === 'string' ? d.detail : JSON.stringify(d.detail);
        if (d.error) return d.error;
      }
    } catch(_) {}
    if (e?.message) return e.message;
    return 'Error procesando el pago';
  }

  async function pagar() {
    error = '';
    normalize();

    if (!luhnOk(tarjeta.numero)) {
      error = 'Número de tarjeta inválido.'; return;
    }
    if (!/^\d{3,4}$/.test(tarjeta.cvv)) {
      error = 'CVV inválido.'; return;
    }

    loading = true;
    try {
      const r = await ComprasAPI.checkout({ tarjeta, facturacion });
      alert(`Compra exitosa. Reserva #${r.idReserva}`);
      navigate(`/reservas/${r.idReserva}`);
    } catch (e) {
      error = showErrorFrom(e);
    } finally {
      loading = false;
    }
  }

  function onEnter(e) {
    if (e.key === 'Enter' && !loading) pagar();
  }
</script>

<div class="container" role="form" on:keydown={onEnter}>
  <h2>Checkout</h2>
  {#if error}<p style="color:#E62727; margin:8px 0">{error}</p>{/if}

  <div class="card" style="display:grid; gap:12px; max-width:700px;">
    <div>
      <label class="label" for="numTarjeta">Número de tarjeta</label>
      <input
        id="numTarjeta"
        class="input"
        bind:value={tarjeta.numero}
        placeholder="4111 1111 1111 1111"
        inputmode="numeric"
        maxlength="23"
        on:input={(e)=> tarjeta.numero = e.target.value.replace(/[^\d ]/g,'')}
      />
      <small style="opacity:.7">No se guarda en el servidor.</small>
    </div>

    <div>
      <label class="label" for="cvv">CVV</label>
      <input
        id="cvv"
        class="input"
        bind:value={tarjeta.cvv}
        placeholder="123"
        inputmode="numeric"
        maxlength="4"
        on:input={(e)=> tarjeta.cvv = e.target.value.replace(/\D/g,'')}
      />
    </div>

    <div class="hr"></div>

    <div>
      <label class="label" for="direccion">Dirección</label>
      <input id="direccion" class="input" bind:value={facturacion.direccion} />
    </div>

    <div class="grid grid-2">
      <div>
        <label class="label" for="ciudad">Ciudad</label>
        <input id="ciudad" class="input" bind:value={facturacion.ciudad} />
      </div>
      <div>
        <label class="label" for="pais">País</label>
        <input id="pais" class="input" bind:value={facturacion.pais} />
      </div>
    </div>

    <div>
      <label class="label" for="zip">ZIP</label>
      <input id="zip" class="input" bind:value={facturacion.zip} />
    </div>

    <div>
      <button class="btn" disabled={loading} on:click={pagar}>
        {loading ? 'Procesando…' : 'Pagar'}
      </button>
    </div>
  </div>
</div>

<style>
  .btn[disabled]{ opacity:.7; cursor:not-allowed;}
</style>