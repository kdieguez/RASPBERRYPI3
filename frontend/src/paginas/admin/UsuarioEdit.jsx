import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import axios from "../../lib/axios";
import "../../styles/perfil.css";

const ROLES = [
  { id: 1, label: "administrador" },
  { id: 2, label: "webservice" },
  { id: 3, label: "visitante" },
];

export default function UsuarioEdit() {
  const { id } = useParams();
  const nav = useNavigate();

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error,  setError] = useState("");

  const [paises, setPaises] = useState([]);

  const [email, setEmail] = useState("");
  const [nombres, setNombres] = useState("");
  const [apellidos, setApellidos] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [idRol, setIdRol] = useState(3);
  const [habilitado, setHabilitado] = useState(1);

  const [fechaNacimiento, setFechaNacimiento] = useState("");
  const [idPais, setIdPais] = useState("");
  const [pasaporte, setPasaporte] = useState("");

  const titulo = useMemo(() => `Editar usuario #${id}`, [id]);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError("");

      try {
        const { data } = await axios.get(`/api/admin/usuarios/${id}`);
        if (cancelled) return;

        setEmail(data.email || "");
        setNombres(data.nombres || "");
        setApellidos(data.apellidos || "");
        setIdRol(Number(data.idRol || 3));
        setHabilitado(Number(data.habilitado || 0));

        setFechaNacimiento(data.fechaNacimiento || "");
        setIdPais(data.idPais || "");
        setPasaporte(data.pasaporte || "");
      } catch (e) {
        if (!cancelled) setError(e?.response?.data?.error || "No se pudo cargar el usuario");
      }

      try {
        const resPaises = await axios.get("/api/public/paises");
        if (!cancelled) setPaises(resPaises.data || []);
      } catch {
      }

      if (!cancelled) setLoading(false);
    }

    load();
    return () => { cancelled = true; };
  }, [id]);

  const guardar = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError("");

    try {
      await axios.put(`/api/admin/usuarios/${id}`, {
        nombres: (nombres || "").trim(),
        apellidos: (apellidos || "").trim(),
        newPassword: newPassword || null,
        idRol: Number(idRol),
        habilitado: Number(habilitado),
        fechaNacimiento: fechaNacimiento || null,
        idPais: idPais ? Number(idPais) : null,
        pasaporte: pasaporte || null,
      });
      nav("/admin/usuarios", { replace: true });
    } catch (e2) {
      setError(e2?.response?.data?.error || "No se pudo guardar");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="container pag-perfil">
        <div className="card">
          <h2>{titulo}</h2>
          <div className="skl row" />
          <div className="skl row" />
          <div className="skl row" />
        </div>
      </div>
    );
  }

  return (
    <div className="container pag-perfil">
      <div className="card">
        <div className="perfil__head">
          <div>
            <h2>{titulo}</h2>
            <p className="subtitle">
              <Link to="/admin/usuarios">← Volver</Link>
            </p>
          </div>
        </div>

        <form onSubmit={guardar} className="form">
          <div className="grid-1">
            <label className="label">Correo</label>
            <input className="input" value={email} readOnly />
          </div>

          <div className="grid-2">
            <div>
              <label className="label">Nombres</label>
              <input
                className="input"
                value={nombres}
                onChange={(e)=>setNombres(e.target.value)}
              />
            </div>
            <div>
              <label className="label">Apellidos</label>
              <input
                className="input"
                value={apellidos}
                onChange={(e)=>setApellidos(e.target.value)}
              />
            </div>
          </div>

          <div className="grid-1">
            <label className="label">Nueva contraseña (opcional)</label>
            <input
              className="input"
              type="password"
              value={newPassword}
              onChange={(e)=>setNewPassword(e.target.value)}
              placeholder="Dejar en blanco para no cambiar"
            />
          </div>

          <div className="grid-2">
            <div>
              <label className="label">Rol</label>
              <select
                className="input"
                value={idRol}
                onChange={(e)=>setIdRol(Number(e.target.value))}
              >
                {ROLES.map(r => <option key={r.id} value={r.id}>{r.label}</option>)}
              </select>
            </div>
            <div>
              <label className="label">Habilitado</label>
              <select
                className="input"
                value={habilitado}
                onChange={(e)=>setHabilitado(Number(e.target.value))}
              >
                <option value={1}>Sí</option>
                <option value={0}>No</option>
              </select>
            </div>
          </div>

          <hr className="sep" />

          <div className="grid-3">
            <div>
              <label className="label">Fecha de nacimiento</label>
              <input
                className="input"
                type="date"
                value={fechaNacimiento || ""}
                onChange={(e)=>setFechaNacimiento(e.target.value)}
              />
            </div>
            <div>
              <label className="label">País del documento</label>
              <select
                className="input"
                value={idPais || ""}
                onChange={(e)=>setIdPais(e.target.value)}
              >
                <option value="">-- Selecciona --</option>
                {paises.map(p => (
                  <option key={p.idPais} value={p.idPais}>{p.nombre}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="label">Pasaporte</label>
              <input
                className="input"
                value={pasaporte || ""}
                onChange={(e)=>setPasaporte(e.target.value)}
                maxLength={20}
              />
            </div>
          </div>

          {error && <div className="error" role="alert">{error}</div>}

          <div className="actions">
            <button className="btn btn-secondary" disabled={saving}>
              {saving ? "Guardando..." : "Guardar cambios"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
