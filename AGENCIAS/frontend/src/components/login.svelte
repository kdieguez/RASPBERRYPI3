<script>
  import { onMount } from "svelte";
  import { AuthAPI } from "@/lib/api";
  import { setAuth, isLoggedIn } from "@/lib/auth";
  import { navigate } from "@/lib/router";

  let email = "";
  let password = "";
  let loading = false;
  let error = "";
  let showPwd = false;

  function nextFromLocation() {
    const params = new URLSearchParams(location.search || "");
    const n = params.get("next");
    return n && n.startsWith("/") ? n : "/admin/portal";
  }

  onMount(() => {
    if ($isLoggedIn) navigate(nextFromLocation(), { replace: true });
  });

  async function submit() {
    error = "";
    loading = true;
    console.log("[LOGIN] submit →", { email });

    try {
      const res = await AuthAPI.login({
        email: email.trim().toLowerCase(),
        password
      });

      const token =
        res?.access_token || res?.accessToken || res?.token || null;
      let user = res?.user || null;

      if (!token) throw new Error("La API no devolvió token de acceso.");

      if (!user) {
        try { user = await AuthAPI.me(); } catch {}
      }

      setAuth({ token, user });
      console.log("[LOGIN] ok → navegando a", nextFromLocation());
      navigate(nextFromLocation());
    } catch (e) {
      console.error("[LOGIN] error:", e);
      error = e?.message || "Error al iniciar sesión";
    } finally {
      loading = false;
    }
  }
</script>

<div class="container" style="display:grid;place-items:center;min-height:calc(100vh - 64px);">
  <form
    class="card"
    style="width: 420px;"
    autocomplete="on"
    on:submit={(e) => { e.preventDefault(); if (!loading) submit(); }}
  >
    <h2 style="margin:0 0 10px 0;">Inicia sesión</h2>
    <p style="margin:0;color:#9ca3af">Usa tu correo y contraseña registrados.</p>

    <div style="margin-top:12px;">
      <label class="label" for="email">Correo</label>
      <input
        id="email"
        class="input"
        type="email"
        bind:value={email}
        placeholder="tucorreo@dominio.com"
        required
        autocomplete="email"
      />
    </div>

    <div style="margin-top:12px;">
      <label class="label" for="password">Contraseña</label>
      <div style="display:flex;gap:8px;align-items:center;">
        <input
          id="password"
          class="input"
          type={showPwd ? "text" : "password"}
          bind:value={password}
          placeholder="••••••••"
          required
          autocomplete="current-password"
        />
        <button
          type="button"
          class="btn"
          on:click={() => (showPwd = !showPwd)}
          aria-pressed={showPwd}
          aria-label={showPwd ? "Ocultar contraseña" : "Mostrar contraseña"}
        >
          {showPwd ? "Ocultar" : "Ver"}
        </button>
      </div>
    </div>

    {#if error}
      <div class="error" role="alert" style="margin-top:8px;">{error}</div>
    {/if}

    <div style="margin-top:16px; display:flex; gap:10px;">
      <button class="btn primary" type="submit" disabled={loading || !email || !password}>
        {loading ? "Entrando…" : "Entrar"}
      </button>
      <button
        class="btn"
        type="button"
        on:click={() => { email=''; password=''; error=''; }}
        disabled={loading}
      >
        Limpiar
      </button>
    </div>
  </form>
</div>