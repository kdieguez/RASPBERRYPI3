import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import axios from "../lib/axios";
import "../styles/perfil.css";

export default function Perfil(){
  const [loading, setLoading] = useState(true);
  const [saving, setSaving]  = useState(false);
  const [error, setError] = useState("");
  const [ok, setOk] = useState(false);

  const [paises, setPaises] = useState([]);
  const [perfil, setPerfil] = useState(null);
  const [nombres,   setNombres]   = useState("");
  const [apellidos, setApellidos] = useState("");
  const [fechaNacimiento, setFechaNacimiento] = useState("");
  const [idPais, setIdPais] = useState("");
  const [pasaporte, setPasaporte] = useState("");

  const edad = useMemo(() => {
    if (!fechaNacimiento) return null;
    const fn = new Date(fechaNacimiento);
    const today = new Date();
    let e = today.getFullYear() - fn.getFullYear();
    const m = today.getMonth() - fn.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < fn.getDate())) e--;
    return e;
  }, [fechaNacimiento]);

  useEffect(() => {
    (async () => {
      try {
        setLoading(true);
        const [paisesRes, perfilRes] = await Promise.all([
          axios.get("/api/public/paises"),
          axios.get("/api/perfil"),
        ]);

        setPaises(paisesRes.data || []);
        const p = perfilRes.data || null;
        setPerfil(p);

        setNombres(p?.nombres || "");
        setApellidos(p?.apellidos || "");

        const pas = p?.pasajero || {};
        setFechaNacimiento(pas.fechaNacimiento || "");
        setIdPais(pas.idPais || "");
        setPasaporte(pas.pasaporte || "");
      } catch (e) {
        setError(e?.response?.data?.error || "No se pudo cargar el perfil");
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const guardar = async (e) => {
    e.preventDefault();
    setError(""); setOk(false); setSaving(true);

    if (!nombres.trim() || !apellidos.trim()) {
      setError("Nombres y apellidos son requeridos.");
      setSaving(false);
      return;
    }

    try {
      const payload = {
        nombres:   nombres.trim(),
        apellidos: apellidos.trim(),

        fechaNacimiento: fechaNacimiento || null,
        idPais: idPais ? Number(idPais) : null,
        pasaporte: pasaporte || null,
      };

      await axios.put("/api/perfil", payload);

      setPerfil((prev) => prev ? { ...prev, nombres: nombres.trim(), apellidos: apellidos.trim() } : prev);

      try {
        const US = "auth.user";
        const u = JSON.parse(localStorage.getItem(US) || "null");
        if (u) {
          u.nombres = nombres.trim();
          u.apellidos = apellidos.trim();
          localStorage.setItem(US, JSON.stringify(u));
          window.dispatchEvent(new Event("auth:changed"));
        }
      } catch { }

      setOk(true);
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
          <div className="skl title" />
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
          <div className="avatar">
            <svg width="40" height="40" viewBox="0 0 24 24" aria-hidden="true">
              <path fill="currentColor" d="M12 12a5 5 0 1 0-5-5a5 5 0 0 0 5 5m0 2c-4.42 0-8 2.24-8 5v1h16v-1c0-2.76-3.58-5-8-5"/>
            </svg>
          </div>
          <div>
            <h2>Mi perfil</h2>
            <p className="subtitle">
              Administra tu información personal. <Link to="/">Volver al inicio</Link>
            </p>
          </div>
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
          <div>
            <label className="label">Correo</label>
            <input className="input" value={perfil?.email || ""} readOnly />
          </div>
          <div>
            <label className="label">Rol</label>
            <input
              className="input"
              value={perfil?.idRol === 1 ? "Administrador"
                    : perfil?.idRol === 2 ? "Webservice"
                    : "Visitante"}
              readOnly
            />
          </div>
        </div>

        <hr className="sep" />

        <form onSubmit={guardar} className="form">
          <div className="grid-3">
            <div>
              <label className="label">Fecha de nacimiento</label>
              <input
                className="input"
                type="date"
                value={fechaNacimiento || ""}
                onChange={(e)=>setFechaNacimiento(e.target.value)}
              />
              <div className="hint">{edad != null ? `Edad: ${edad} años` : "Opcional"}</div>
            </div>

            <div>
              <label className="label">País de documento</label>
              <select
                className="input"
                value={idPais || ""}
                onChange={(e)=>setIdPais(e.target.value)}
              >
                <option value="">-- Selecciona un país --</option>
                {paises.map(p => (
                  <option key={p.idPais} value={p.idPais}>{p.nombre}</option>
                ))}
              </select>
              <div className="hint">Opcional</div>
            </div>

            <div>
              <label className="label">Pasaporte</label>
              <input
                className="input"
                maxLength={20}
                value={pasaporte || ""}
                onChange={(e)=>setPasaporte(e.target.value)}
                placeholder="ej: X12345678"
              />
              <div className="hint">Máx. 20 caracteres (opcional)</div>
            </div>
          </div>

          {error && <div className="error" role="alert">{error}</div>}
          {ok && <div className="ok">¡Guardado!</div>}

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
