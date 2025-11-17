import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { agenciasApi } from "../../api/agencias";
import { adminUsuariosApi } from "../../api/usuariosAdmin";

export default function AgenciaEdit() {
  const { id } = useParams();
  const nav = useNavigate();
  const isNew = id === "nueva";

  const [form, setForm] = useState({ idAgencia: "", nombre: "", apiUrl: "", idUsuarioWs: "", habilitado: true });
  const [wsUser, setWsUser] = useState({ idUsuario: "", email: "", nombres: "", apellidos: "", password: "" });
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        setLoading(true);
        setErr("");
        const agencia = !isNew ? (await agenciasApi.get(id)).data : null;
        if (agencia) {
          setForm({
            idAgencia: agencia.idAgencia,
            nombre: agencia.nombre,
            apiUrl: agencia.apiUrl,
            idUsuarioWs: agencia.idUsuarioWs ?? "",
            habilitado: !!agencia.habilitado,
          });
          if (agencia.idUsuarioWs) {
            try {
              const resU = await adminUsuariosApi.get(agencia.idUsuarioWs);
              const u = resU.data;
              setWsUser({
                idUsuario: u.idUsuario,
                email: u.email,
                nombres: u.nombres || "",
                apellidos: u.apellidos || "",
                password: "",
              });
            } catch {
              setWsUser({ idUsuario: agencia.idUsuarioWs, email: "", nombres: "", apellidos: "", password: "" });
            }
          }
        }
      } catch (e) {
        setErr(e?.response?.data?.error || "No se pudo cargar la agencia");
      } finally {
        setLoading(false);
      }
    })();
  }, [id]);

  const onChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm(f => ({ ...f, [name]: type === "checkbox" ? checked : value }));
  };

  const onSave = async (e) => {
    e.preventDefault();
    try {
      setSaving(true);
      setErr("");

      let idUsuarioWs = form.idUsuarioWs ? Number(form.idUsuarioWs) : null;

      if (!idUsuarioWs) {
        if (!wsUser.email || !wsUser.password || !wsUser.nombres || !wsUser.apellidos) {
          throw new Error("Completa los datos del usuario WebService (email, password, nombres y apellidos).");
        }
        const payload = {
          email: wsUser.email,
          password: wsUser.password,
          nombres: wsUser.nombres,
          apellidos: wsUser.apellidos,
          idRol: 2, 
        };
        const res = await adminUsuariosApi.createWS(payload);
        idUsuarioWs = res.data?.idUsuario ?? idUsuarioWs;

        setWsUser(u => ({ ...u, idUsuario: idUsuarioWs }));
      } else {

        const payloadUpd = {
          nombres: wsUser.nombres,
          apellidos: wsUser.apellidos,
          newPassword: wsUser.password ? wsUser.password : undefined,
          idRol: 2,
          habilitado: 1,
        };
        await adminUsuariosApi.update(idUsuarioWs, payloadUpd);
      }

      const body = {
        idAgencia: form.idAgencia,
        nombre: form.nombre,
        apiUrl: form.apiUrl,
        idUsuarioWs: idUsuarioWs,
        habilitado: !!form.habilitado,
      };
      if (isNew) await agenciasApi.create(body);
      else await agenciasApi.update(form.idAgencia, body);

      nav("/admin/agencias");
    } catch (e) {
      setErr(e?.response?.data?.error || e.message || "No se pudo guardar");
    } finally {
      setSaving(false);
    }
  };

  const onDelete = async () => {
    if (isNew) return;
    if (!confirm("¿Eliminar esta agencia?")) return;
    try {
      await agenciasApi.remove(form.idAgencia);
      nav("/admin/agencias");
    } catch (e) {
      setErr(e?.response?.data?.error || "No se pudo eliminar");
    }
  };

  if (loading) return <div className="container"><p>Cargando…</p></div>;

  return (
    <div className="container">
      <h2>{isNew ? "Nueva agencia" : `Agencia ${form.idAgencia}`}</h2>
      {err && <p className="error">{err}</p>}
      <form onSubmit={onSave} className="form">
        <div className="field">
          <label>ID agencia</label>
          <input name="idAgencia" value={form.idAgencia} onChange={onChange} disabled={!isNew} required />
        </div>
        <div className="field">
          <label>Nombre</label>
          <input name="nombre" value={form.nombre} onChange={onChange} required />
        </div>
        <div className="field">
          <label>API URL</label>
          <input name="apiUrl" value={form.apiUrl} onChange={onChange} required />
        </div>
        <div className="field">
          <label>
            <input type="checkbox" name="habilitado" checked={!!form.habilitado} onChange={onChange} />
            Habilitado
          </label>
        </div>

        <div className="field">
          <fieldset style={{border:"1px solid #eee", borderRadius:12, padding:14}}>
            <legend style={{padding:"0 8px"}}>Usuario WebService</legend>
            <div className="field">
              <label>Email {form.idUsuarioWs ? "(solo lectura)" : ""}</label>
              <input type="email" value={wsUser.email} onChange={e=>setWsUser({...wsUser, email:e.target.value})} required={!form.idUsuarioWs} disabled={!!form.idUsuarioWs} />
            </div>
            <div className="field">
              <label>Nombres</label>
              <input value={wsUser.nombres} onChange={e=>setWsUser({...wsUser, nombres:e.target.value})} required />
            </div>
            <div className="field">
              <label>Apellidos</label>
              <input value={wsUser.apellidos} onChange={e=>setWsUser({...wsUser, apellidos:e.target.value})} required />
            </div>
            <div className="field">
              <label>{form.idUsuarioWs ? "Nuevo password (opcional)" : "Password"}</label>
              <input type="password" value={wsUser.password} onChange={e=>setWsUser({...wsUser, password:e.target.value})} required={!form.idUsuarioWs} minLength={8} placeholder={form.idUsuarioWs ? "Dejar vacío para no cambiar" : ""} />
            </div>
          </fieldset>
        </div>

        <div style={{display:"flex", gap:8}}>
          <button className="btn primary" disabled={saving}>{saving ? "Guardando…" : "Guardar"}</button>
          {!isNew && <button type="button" className="btn" onClick={onDelete}>Eliminar</button>}
          <button type="button" className="btn" onClick={()=>nav("/admin/agencias")}>Cancelar</button>
        </div>
      </form>
    </div>
  );
}


