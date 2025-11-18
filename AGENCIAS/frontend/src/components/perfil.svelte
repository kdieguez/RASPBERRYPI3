<script>
  import { onMount } from "svelte";
  import { fetchProfile, updateProfile } from "@/lib/profileApi";
  import { navigate } from "@/lib/router";

  let loading = true;
  let saving = false;
  let error = "";
  let success = "";

  let form = {
    email: "",
    nombres: "",
    apellidos: "",
    telefono: "",
    pais: "",
    documento: "",
  };

  onMount(async () => {
    try {
      const data = await fetchProfile();
      form.email = data.email ?? "";
      form.nombres = data.nombres ?? "";
      form.apellidos = data.apellidos ?? "";
      form.telefono = data.telefono ?? "";
      form.pais = data.pais ?? "";
      form.documento = data.documento ?? "";
    } catch (e) {
      error = e.message;
    } finally {
      loading = false;
    }
  });

  async function handleSubmit(e) {
    e.preventDefault();
    error = "";
    success = "";
    saving = true;

    try {
      const payload = {
        nombres: form.nombres || null,
        apellidos: form.apellidos || null,
        telefono: form.telefono || null,
        pais: form.pais || null,
        documento: form.documento || null,
      };

      const updated = await updateProfile(payload);
      success = "Perfil actualizado correctamente";

      form.nombres = updated.nombres ?? "";
      form.apellidos = updated.apellidos ?? "";
      form.telefono = updated.telefono ?? "";
      form.pais = updated.pais ?? "";
      form.documento = updated.documento ?? "";
    } catch (e) {
      error = e.message;
    } finally {
      saving = false;
    }
  }

  function goBack() {
    navigate("/");
  }
</script>

{#if loading}
  <div class="perfil-container">
    <p>Cargando perfil...</p>
  </div>
{:else}
  <div class="perfil-container">
    <h1>Mi perfil</h1>

    {#if error}
      <div class="perfil-alert error">{error}</div>
    {/if}

    {#if success}
      <div class="perfil-alert success">{success}</div>
    {/if}

    <form class="perfil-form" on:submit|preventDefault={handleSubmit}>
      <div class="perfil-row">
        <label for="perfil-email">Email</label>
        <input
          id="perfil-email"
          type="email"
          bind:value={form.email}
          disabled
        />
      </div>

      <div class="perfil-row">
        <label for="perfil-nombres">Nombres</label>
        <input
          id="perfil-nombres"
          type="text"
          bind:value={form.nombres}
          required
        />
      </div>

      <div class="perfil-row">
        <label for="perfil-apellidos">Apellidos</label>
        <input
          id="perfil-apellidos"
          type="text"
          bind:value={form.apellidos}
          required
        />
      </div>

      <div class="perfil-row">
        <label for="perfil-telefono">Teléfono</label>
        <input
          id="perfil-telefono"
          type="text"
          bind:value={form.telefono}
        />
      </div>

      <div class="perfil-row">
        <label for="perfil-pais">País</label>
        <input
          id="perfil-pais"
          type="text"
          bind:value={form.pais}
        />
      </div>

      <div class="perfil-row">
        <label for="perfil-documento">Documento (pasaporte / DPI)</label>
        <input
          id="perfil-documento"
          type="text"
          bind:value={form.documento}
        />
      </div>

      <div class="perfil-actions">
        <button type="button" on:click={goBack}>Cancelar</button>
        <button type="submit" disabled={saving}>
          {#if saving}Guardando...{/if}
          {#if !saving}Guardar cambios{/if}
        </button>
      </div>
    </form>
  </div>
{/if}

<style>
  .perfil-container {
    max-width: 600px;
    margin: 2rem auto;
    padding: 1.5rem;
    border-radius: 12px;
    background: #ffffff;
    box-shadow: 0 0 12px rgba(0,0,0,0.06);
  }

  h1 {
    margin-bottom: 1rem;
  }

  .perfil-form {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }

  .perfil-row {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
  }

  label {
    font-size: 0.9rem;
    font-weight: 600;
  }

  input {
    padding: 0.5rem 0.75rem;
    border-radius: 8px;
    border: 1px solid #ddd;
  }

  .perfil-actions {
    margin-top: 1rem;
    display: flex;
    justify-content: flex-end;
    gap: 0.75rem;
  }

  .perfil-actions button {
    padding: 0.5rem 1rem;
    border-radius: 8px;
    border: none;
    cursor: pointer;
  }

  .perfil-actions button[type="button"] {
    background: #eee;
  }

  .perfil-actions button[type="submit"] {
    background: #1E93AB;
    color: white;
    font-weight: 600;
  }

  .perfil-alert {
    padding: 0.5rem 0.75rem;
    border-radius: 8px;
    margin-bottom: 0.5rem;
    font-size: 0.9rem;
  }

  .perfil-alert.error {
    background: #ffe5e5;
    color: #b71c1c;
  }

  .perfil-alert.success {
    background: #e6ffed;
    color: #1b5e20;
  }
</style>