<script>
  import { onMount } from "svelte";
  import { AuthAPI } from "@/lib/api";
  import { navigate } from "@/lib/router";

  let form = {
    email:"", password:"",
    nombres:"", apellidos:"",
    edad:18, pais_origen:"", numero_pasaporte:"",
    captcha_token:""
  };

  let submitting=false, ok="", err="";
  let fieldErr = { email:"", password:"", nombres:"", apellidos:"", edad:"", pais_origen:"", numero_pasaporte:"" };

  const SITE_KEY = import.meta.env.VITE_RECAPTCHA_SITE_KEY;
  let widgetId = null;

  async function ensureRecaptcha(){
    if (window.grecaptcha?.render) return;
    if (!document.querySelector('script[src*="recaptcha/api.js"]')) {
      const s = document.createElement('script');
      s.src = 'https://www.google.com/recaptcha/api.js?render=explicit';
      s.async = true; s.defer = true;
      document.head.appendChild(s);
    }
    await new Promise((resolve, reject) => {
      let tries = 0;
      const iv = setInterval(() => {
        if (window.grecaptcha?.render) { clearInterval(iv); resolve(); }
        if (++tries > 200) { clearInterval(iv); reject(new Error('reCAPTCHA no cargó')); }
      }, 50);
    });
  }

  function renderWidget(){
    if (widgetId !== null || !window.grecaptcha?.render) return;
    widgetId = window.grecaptcha.render('recaptcha-container', {
      sitekey: SITE_KEY,
      callback: (token) => { form.captcha_token = token; },
      'expired-callback': () => { form.captcha_token = ""; }
    });
  }

  onMount(async () => {
    try { await ensureRecaptcha(); renderWidget(); }
    catch { err = 'No se pudo cargar reCAPTCHA'; }
  });

  function validate() {
    fieldErr = { email:"", password:"", nombres:"", apellidos:"", edad:"", pais_origen:"", numero_pasaporte:"" };
    const email = form.email.trim().toLowerCase();
    if (!email) fieldErr.email = "Correo requerido";
    if (form.password.trim().length < 8) fieldErr.password = "Mínimo 8 caracteres";
    if (!form.nombres.trim()) fieldErr.nombres = "Nombres requeridos";
    if (!form.apellidos.trim()) fieldErr.apellidos = "Apellidos requeridos";
    const edadNum = Number(form.edad);
    if (!Number.isInteger(edadNum) || edadNum < 0 || edadNum > 120) fieldErr.edad = "Edad inválida";
    if (!form.pais_origen.trim()) fieldErr.pais_origen = "País requerido";
    if (!form.numero_pasaporte.trim()) fieldErr.numero_pasaporte = "Pasaporte requerido";
    return Object.values(fieldErr).every(v => !v);
  }

  async function submit(e) {
    e?.preventDefault?.();
    ok = ""; err = "";

    if (!validate()) { err = "Corrige los campos marcados (*)"; return; }
    if (!form.captcha_token) { err = "Por favor, marca el reCAPTCHA."; return; }

    submitting = true;
    try {
      const payload = {
        email: form.email.trim().toLowerCase(),
        password: form.password,
        nombres: form.nombres.trim(),
        apellidos: form.apellidos.trim(),
        edad: Number(form.edad),
        pais_origen: form.pais_origen.trim(),
        numero_pasaporte: form.numero_pasaporte.trim(),
        captcha_token: form.captcha_token
      };
      await AuthAPI.register(payload);
      navigate("/login?next=/admin/portal");
    } catch (e) {
      err = e?.message || "Ocurrió un error";
    } finally {
      submitting=false;
    }
  }
</script>

<section class="container">
  <form class="card" on:submit|preventDefault={submit} novalidate>
    <h2 style="margin-top:0">Crear cuenta</h2>

    <div class="grid">
      <div class="col-6">
        <input class="input" placeholder="Correo *" bind:value={form.email} type="email" autocomplete="email" aria-invalid={fieldErr.email ? "true" : "false"} />
        {#if fieldErr.email}<div class="status-err" style="font-size:.9rem;margin-top:4px">{fieldErr.email}</div>{/if}
      </div>
      <div class="col-6">
        <input class="input" placeholder="Contraseña *" bind:value={form.password} type="password" minlength="8" autocomplete="new-password" aria-invalid={fieldErr.password ? "true" : "false"} />
        {#if fieldErr.password}<div class="status-err" style="font-size:.9rem;margin-top:4px">{fieldErr.password}</div>{/if}
      </div>
      <div class="col-6">
        <input class="input" placeholder="Nombres *" bind:value={form.nombres} autocomplete="given-name" aria-invalid={fieldErr.nombres ? "true" : "false"} />
        {#if fieldErr.nombres}<div class="status-err" style="font-size:.9rem;margin-top:4px">{fieldErr.nombres}</div>{/if}
      </div>
      <div class="col-6">
        <input class="input" placeholder="Apellidos *" bind:value={form.apellidos} autocomplete="family-name" aria-invalid={fieldErr.apellidos ? "true" : "false"} />
        {#if fieldErr.apellidos}<div class="status-err" style="font-size:.9rem;margin-top:4px">{fieldErr.apellidos}</div>{/if}
      </div>
      <div class="col-6">
        <input class="input" placeholder="País de origen *" bind:value={form.pais_origen} aria-invalid={fieldErr.pais_origen ? "true" : "false"} />
        {#if fieldErr.pais_origen}<div class="status-err" style="font-size:.9rem;margin-top:4px">{fieldErr.pais_origen}</div>{/if}
      </div>
      <div class="col-3">
        <input class="input" placeholder="Edad *" bind:value={form.edad} type="number" min="0" max="120" inputmode="numeric" aria-invalid={fieldErr.edad ? "true" : "false"} />
        {#if fieldErr.edad}<div class="status-err" style="font-size:.9rem;margin-top:4px">{fieldErr.edad}</div>{/if}
      </div>
      <div class="col-3">
        <input class="input" placeholder="No. Pasaporte *" bind:value={form.numero_pasaporte} aria-invalid={fieldErr.numero_pasaporte ? "true" : "false"} />
        {#if fieldErr.numero_pasaporte}<div class="status-err" style="font-size:.9rem;margin-top:4px">{fieldErr.numero_pasaporte}</div>{/if}
      </div>

      <div class="col-12">
        <div id="recaptcha-container"></div>
        <div class="help">Marca el reCAPTCHA para continuar.</div>
      </div>
    </div>

    <div style="display:flex; gap:10px; margin-top:12px">
      <button class="btn primary" type="submit" disabled={submitting}
              on:click|preventDefault={submit}>
        {submitting ? "Creando..." : "Crear cuenta"}
      </button>
    </div>

    {#if ok}<p class="status-ok" style="margin-top:10px">{ok}</p>{/if}
    {#if err}<p class="status-err" style="margin-top:10px">{err}</p>{/if}
  </form>
</section>