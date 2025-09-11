import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import axios from "../lib/axios";             
import "../styles/login.css";
import { saveAuth } from "../lib/auth";

export default function Login() {
  const nav = useNavigate();
  const [form, setForm] = useState({ email: "", password: "" });
  const [showPass, setShowPass] = useState(false);
  const [err, setErr] = useState("");
  const [loading, setLoading] = useState(false);

  const onChange = (k, v) => setForm(f => ({ ...f, [k]: v }));
  const disabled = !form.email || !form.password;

  const submit = async (e) => {
    e.preventDefault();
    setErr("");
    setLoading(true);
    try {
      const { data } = await axios.post("/api/auth/login", form);
      saveAuth({ token: data.token, user: data.user, expiresInSec: data.expires_in });
      nav("/");
    } catch (e2) {
      const msg = e2?.response?.status === 401
        ? "Credenciales incorrectas"
        : (e2?.response?.data?.error || "No se pudo iniciar sesión");
      setErr(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container pag-login">
      <div className="card container-inner">
        <h2>Iniciar sesión</h2>
        <p className="subtitle">Ingresa con tu correo y contraseña</p>

        <form onSubmit={submit} className="form">
          <label className="label">Correo</label>
          <input
            className="input"
            type="email"
            autoComplete="email"
            placeholder="ej: nombre@correo.com"
            value={form.email}
            onChange={(e)=>onChange("email", e.target.value)}
          />

          <label className="label">Contraseña</label>
          <div className="input-wrap">
            <input
              className="input"
              type={showPass ? "text" : "password"}
              autoComplete="current-password"
              placeholder="tu contraseña"
              value={form.password}
              onChange={(e)=>onChange("password", e.target.value)}
            />
            <button type="button" className="toggle" onClick={()=>setShowPass(s=>!s)}>
              {showPass ? "Ocultar" : "Mostrar"}
            </button>
          </div>

          {err && <div className="error">{err}</div>}

          <button className="btn btn-secondary" disabled={loading || disabled}>
            {loading ? "Ingresando..." : "Entrar"}
          </button>
        </form>

        <p className="muted">
          ¿No tienes cuenta? <Link to="/registro">Crear cuenta</Link>
        </p>
      </div>
    </div>
  );
}
