<script>
  import Header from "@/components/header.svelte";
  import Home from "@/components/home.svelte";
  import Register from "@/components/register.svelte";
  import Login from "@/components/login.svelte";
  import Footer from "@/components/footer.svelte";
  import AdminUsers from "@/components/admin/users.svelte";
  import AdminPortal from "@/components/admin/portal.svelte";
  import Catalog from "@/components/vuelos/catalog.svelte";
  import Detail from "@/components/vuelos/detail.svelte";
  import { isLoggedIn } from "@/lib/auth";
  import { path, navigate, match, link } from "@/lib/router";

  import Cart from "@/components/compras/cart.svelte";
  import Checkout from "@/components/compras/checkout.svelte";
  import Reservas from "@/components/compras/reservas.svelte";
  import ReservaDetail from "@/components/compras/reserva_detail.svelte";

  import AdminReservas from "@/components/admin/reservas_admin.svelte";
  import AdminReservaDetail from "@/components/admin/reserva_detail_admin.svelte";

  import CancelacionCompras from "@/components/compras/cancelacionCompras.svelte";

  import PaginasCancelacion from "@/components/admin/paginas_cancelacion.svelte";

  let current = "/";
  const unsub = path.subscribe((p) => (current = p));

  $: if ($isLoggedIn && current === "/login") navigate("/", { replace: true });

  function headerCurrent(p) {
    if (p === "/") return "home";
    if (p === "/register") return "register";
    if (p === "/login") return "login";
    if (p === "/admin/users") return "users";
    if (p === "/admin/portal") return "portal";
    if (match("/vuelos", p).ok || match("/vuelos/:id", p).ok) return "vuelos";
    if (match("/carrito", p).ok) return "carrito";
    if (match("/reservas", p).ok || match("/reservas/:id", p).ok) return "reservas";
    if (p === "/admin/reservas" || match("/admin/reservas/:id", p).ok)
      return "admin-reservas";
    if (p === "/informacion/cancelacion-compras") return "info-cancelacion";
    if (p === "/admin/paginas/cancelacion-compras") return "admin-paginas";
    return "";
  }

  $: isVuelosCatalog        = match("/vuelos", current).ok;
  $: isVuelosDetail         = match("/vuelos/:id", current).ok;

  $: isCart                 = match("/carrito", current).ok;
  $: isCheckout             = match("/checkout", current).ok;
  $: isReservas             = match("/reservas", current).ok;
  $: isReservaDetail        = match("/reservas/:id", current).ok;

  $: isAdminReservas        = match("/admin/reservas", current).ok;
  $: isAdminReservaDetail   = match("/admin/reservas/:id", current).ok;

  $: isCancelacionCompras   = match("/informacion/cancelacion-compras", current).ok;
  $: isAdminPagCancelacion  = match("/admin/paginas/cancelacion-compras", current).ok;
</script>

<div class="app-layout">
  <Header current={headerCurrent(current)} />

  <main class="page-main">
    {#key current}
      {#if current === '/'}
        <Home />

      {:else if isVuelosCatalog}
        <Catalog />
      {:else if isVuelosDetail}
        <Detail />

      {:else if isCart}
        <Cart />
      {:else if isCheckout}
        <Checkout />
      {:else if isReservas}
        <Reservas />
      {:else if isReservaDetail}
        <ReservaDetail />

      {:else if isCancelacionCompras}
        <CancelacionCompras />

      {:else if isAdminReservas}
        <AdminReservas />
      {:else if isAdminReservaDetail}
        <AdminReservaDetail />

      {:else if isAdminPagCancelacion}
        <PaginasCancelacion />

      {:else if current === '/admin/users'}
        <AdminUsers />
      {:else if current === '/admin/portal'}
        <AdminPortal />
      {:else if current === '/register'}
        <Register />
      {:else if current === '/login'}
        {#if $isLoggedIn}
          <section class="container">
            <div class="card">Ya has iniciado sesión. Redirigiendo…</div>
          </section>
        {:else}
          <Login />
        {/if}
      {:else}
        <section class="container">
          <div class="card">
            <h2>Página no encontrada</h2>
            <a class="btn" href="/" use:link>Ir al inicio</a>
          </div>
        </section>
      {/if}
    {/key}
  </main>

  <Footer />
</div>

<style>
  .app-layout {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
  }

  .page-main {
    flex: 1 0 auto;
    padding-bottom: 90px; 
  }
</style>