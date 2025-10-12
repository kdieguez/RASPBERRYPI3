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
    return "";
  }

  $: isVuelosCatalog = match("/vuelos", current).ok;
  $: isVuelosDetail  = match("/vuelos/:id", current).ok;
</script>

<Header current={headerCurrent(current)} />

{#if current === '/'}
  <Home />
{:else if isVuelosCatalog}
  <Catalog />
{:else if isVuelosDetail}
  <Detail />
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

<Footer />