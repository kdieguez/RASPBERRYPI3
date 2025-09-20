<script>
  import { user as currentUser, isLoggedIn } from '../lib/auth';
</script>

<div class="container">
  <div class="card">
    <h2 style="margin-top:0">
      Bienvenido, {$currentUser?.nombres || $currentUser?.email || 'Visitante'}
    </h2>

    {#if $isLoggedIn}
      <p style="opacity:.85">Tu sesión está activa. Esta vista está protegida si así lo decides.</p>
      <ul>
        <li>Desde aquí podemos construir el flujo de búsqueda de vuelos/hoteles/paquetes.</li>
        <li>También conectar el carrito y el checkout cuando estén listos.</li>
      </ul>
    {:else}
      <p style="opacity:.85">Aún no has iniciado sesión.</p>
    {/if}
  </div>
</div>