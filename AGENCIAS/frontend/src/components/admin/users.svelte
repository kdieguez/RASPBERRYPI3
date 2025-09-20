<script>
  import { onMount } from 'svelte';
  import Swal from 'sweetalert2';
  import { UsersAPI } from '../../lib/api';
  import { isLoggedIn, user as currentUser } from '../../lib/auth';
  import { navigate } from '../../lib/router';

  let loading = true, error = '';
  let items = [], total = 0, page = 1, page_size = 10;
  let q = '', role = '', activo = '';

  async function load() {
    loading = true; error = '';
    try {
      const res = await UsersAPI.list({ q, role, activo, page, page_size });
      items = (res.items || []).map(u => ({
        ...u,
        _origRol: u.rol,
        _saving: false
      }));
      total = res.total || items.length;
    } catch (e) {
      error = e?.message || 'No se pudo cargar';
    } finally {
      loading = false;
    }
  }

  function canEdit() {
    const r = ($currentUser?.rol || '').toLowerCase();
    return r === 'admin' || r === 'empleado';
  }
  function isAdmin() {
    return ($currentUser?.rol || '').toLowerCase() === 'admin';
  }

  function toastOK(title) {
    Swal.fire({
      toast: true, icon: 'success', title,
      position: 'top-end', showConfirmButton: false,
      timer: 1400, timerProgressBar: true
    });
  }
  function toastErr(title, text) {
    Swal.fire({
      icon: 'error', title: title || 'Error',
      text: text || 'Ocurrió un problema', confirmButtonText: 'Entendido'
    });
  }

  async function saveRole(u) {
    if (u.rol === u._origRol) return;
    items = items.map(it => it.id === u.id ? { ...it, _saving: true } : it);
    try {
      const updated = await UsersAPI.update(u.id, { rol: u.rol });
      const finalRol = updated?.rol ?? u.rol;
      items = items.map(it =>
        it.id === u.id ? { ...it, rol: finalRol, _origRol: finalRol, _saving: false } : it
      );
      toastOK(`Rol actualizado a ${finalRol}`);
    } catch (e) {
      items = items.map(it =>
        it.id === u.id ? { ...it, _saving: false, rol: it._origRol } : it
      );
      toastErr('No se pudo guardar el rol,', e?.message);
    }
  }

  async function toggleActive(u) {
    items = items.map(it => it.id === u.id ? { ...it, _saving: true } : it);
    try {
      const updated = await UsersAPI.update(u.id, { activo: !u.activo });
      items = items.map(it =>
        it.id === u.id
          ? { ...it, activo: updated.activo, _origRol: updated.rol ?? it._origRol, _saving: false }
          : it
      );
      toastOK(updated.activo ? 'Usuario activado' : 'Usuario desactivado');
    } catch (e) {
      items = items.map(it => it.id === u.id ? { ...it, _saving: false } : it);
      toastErr('No se pudo cambiar el estado', e?.message);
    }
  }

  async function removeUser(u) {
    if (!confirm(`¿Eliminar a ${u.email}?`)) return;
    try {
      await UsersAPI.remove(u.id);
      await load();
      toastOK('Usuario eliminado');
    } catch (e) {
      toastErr('No se pudo eliminar', e?.message);
    }
  }

  onMount(() => {
    if (!$isLoggedIn || !canEdit()) { navigate('/'); return; }
    load();
  });
</script>

<div class="container">
  <div class="card">
    <h2 style="margin-top:0">Administración de usuarios</h2>

    <div class="row two" style="margin:10px 0;">
      <input
        class="input"
        placeholder="Buscar (correo, nombres, apellidos)"
        bind:value={q}
        on:keyup={(e)=>{ if(e.key==='Enter') load(); }} />
      <div style="display:flex; gap:8px;">
        <select class="input" bind:value={role} on:change={load}>
          <option value="">Todos los roles</option>
          <option>ADMIN</option>
          <option>EMPLEADO</option>
          <option>VISITANTE_REGISTRADO</option>
          <option>WEBSERVICE</option>
        </select>
        <select class="input" bind:value={activo} on:change={load}>
          <option value="">Todos</option>
          <option value="true">Activos</option>
          <option value="false">Inactivos</option>
        </select>
        <button class="btn" on:click={load}>Buscar</button>
      </div>
    </div>

    {#if error}
      <div class="error">{error}</div>
    {/if}

    {#if loading}
      <div class="card">Cargando…</div>
    {:else}
      <div style="overflow:auto;">
        <table style="width:100%; border-collapse:collapse;">
          <thead>
            <tr>
              <th style="text-align:left; padding:8px; border-bottom:1px solid #1f2937;">Correo</th>
              <th style="text-align:left; padding:8px; border-bottom:1px solid #1f2937;">Nombre</th>
              <th style="text-align:left; padding:8px; border-bottom:1px solid #1f2937;">Rol</th>
              <th style="text-align:left; padding:8px; border-bottom:1px solid #1f2937;">Estado</th>
              <th style="padding:8px; border-bottom:1px solid #1f2937;">Acciones</th>
            </tr>
          </thead>
          <tbody>
            {#each items as u}
              <tr>
                <td style="padding:8px;">{u.email}</td>
                <td style="padding:8px;">{u.nombres} {u.apellidos}</td>
                <td style="padding:8px;">
                  {#if isAdmin()}
                    <div style="display:flex; gap:8px; align-items:center;">
                      <select class="input" bind:value={u.rol}>
                        <option value="ADMIN">ADMIN</option>
                        <option value="EMPLEADO">EMPLEADO</option>
                        <option value="VISITANTE_REGISTRADO">VISITANTE_REGISTRADO</option>
                        <option value="WEBSERVICE">WEBSERVICE</option>
                      </select>

                      {#if u.rol !== u._origRol}
                        <button class="btn primary" disabled={u._saving} on:click={() => saveRole(u)}>
                          {u._saving ? 'Guardando…' : 'Guardar'}
                        </button>
                      {/if}
                    </div>
                  {:else}
                    {u.rol}
                  {/if}
                </td>
                <td style="padding:8px;">
                  <button class="btn {u.activo ? 'primary' : ''}" disabled={u._saving} on:click={() => toggleActive(u)}>
                    {u.activo ? 'Activo' : 'Inactivo'}
                  </button>
                </td>
                <td style="padding:8px; text-align:center;">
                  {#if isAdmin()}
                    <button class="btn danger" disabled={u._saving} on:click={() => removeUser(u)}>Eliminar</button>
                  {/if}
                </td>
              </tr>
            {/each}
          </tbody>
        </table>
      </div>

      <div style="margin-top:12px; display:flex; gap:8px; align-items:center;">
        <button class="btn" disabled={page<=1} on:click={() => { page--; load(); }}>‹ Anterior</button>
        <div style="opacity:.8">Página {page}</div>
        <button class="btn" disabled={(page*page_size)>=total} on:click={() => { page++; load(); }}>Siguiente ›</button>
      </div>
    {/if}
  </div>
</div>