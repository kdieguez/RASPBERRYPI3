<script>
  import { onMount } from "svelte";
  import Header from "./components/header.svelte";
  import Home from "./components/home.svelte";
  import Register from "./components/register.svelte";
  import Login from "./components/login.svelte";
  import Footer from "./components/footer.svelte";
  import AdminUsers from "./components/admin/users.svelte";
  import AdminPortal from "./components/admin/portal.svelte";

  import Catalog from "./components/vuelos/catalog.svelte";
  import Detail from "./components/vuelos/detail.svelte";

  import { isLoggedIn } from "./lib/auth";

  // === estado de ruta local (sin store) ===
  let current = "/";

  function normalize(p) {
    p = (p || "/").toString().replace(/^#/, "");
    if (!p.startsWith("/")) p = "/" + p;
    if (p === "/registro") p = "/register";
    if (p === "/ingresar" || p === "/iniciar-sesion") p = "/login";
    return p.replace(/\/{2,}/g, "/");
  }

  function setFromHash() {
    current = normalize((location.hash || "").slice(1) || "/");
  }

  onMount(setFromHash);
  $: if ($isLoggedIn && current === "/login") location.hash = "/";

  // === helpers de matching mínimos ===
  function match(pattern, p) {
    const A = normalize(pattern).split("/").filter(Boolean);
    const B = normalize(p).split("/").filter(Boolean);
    if (A.length !== B.length) return { ok: false, params: {} };
    const params = {};
    for (let i = 0; i < A.length; i++) {
      if (A[i].startsWith(":")) params[A[i].slice(1)] = B[i];
      else if (A[i] !== B[i]) return { ok: false, params: {} };
    }
    return { ok: true, params };
  }

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

<!-- escucha el hash real del navegador -->
<svelte:window on:hashchange={setFromHash} />

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
      <a class="btn" href="#/">Ir al inicio</a>
    </div>
  </section>
{/if}

<Footer />