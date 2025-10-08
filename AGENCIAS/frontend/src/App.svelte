<script>
  import Header from "./components/header.svelte";
  import Home from "./components/home.svelte";
  import Register from "./components/register.svelte";
  import Login from "./components/login.svelte";
  import Footer from "./components/footer.svelte";
  import AdminUsers from "./components/admin/users.svelte";

  import Catalog from "./components/vuelos/catalog.svelte";
  import Detail from "./components/vuelos/detail.svelte";

  import { path, navigate, match } from "./lib/router.js";
  import { isLoggedIn } from "./lib/auth";

  $: current = $path;

  $: if ($isLoggedIn && $path === '/login') navigate('/');

  function headerCurrent(p) {
    if (p === '/') return 'home';
    if (p === '/register') return 'register';
    if (p === '/login') return 'login';
    if (p === '/admin/users') return 'users';
    if (match('/vuelos', p).ok || match('/vuelos/:id', p).ok) return 'vuelos';
    return '';
  }

  $: isVuelosCatalog = match('/vuelos', current).ok;
  $: isVuelosDetail  = match('/vuelos/:id', current).ok;
</script>

<Header
  current={headerCurrent(current)}
  on:gotoHome={() => navigate('/')}
  on:gotoRegister={() => navigate('/register')}
  on:gotoLogin={() => navigate('/login')}
  on:gotoUsers={() => navigate('/admin/users')}
  on:gotoVuelos={() => navigate('/vuelos')}
/>

{#if current === '/'}
  <Home />

{:else if isVuelosCatalog}
  <Catalog />

{:else if isVuelosDetail}
  <Detail />

{:else if current === '/admin/users'}
  <AdminUsers />

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
      <button class="btn" on:click={() => navigate('/')}>Ir al inicio</button>
    </div>
  </section>
{/if}

<Footer />