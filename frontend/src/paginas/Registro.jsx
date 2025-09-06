import { useState } from "react";
import ReCAPTCHA from "react-google-recaptcha";
import axios from "axios";
import { useNavigate, Link } from "react-router-dom";
import "../styles/registro.css";

const API = import.meta.env.VITE_API_URL || "http://localhost:8080";
const SITE_KEY = import.meta.env.VITE_RECAPTCHA_SITE_KEY;

export default function Register() {
  const nav = useNavigate();
  const [form, setForm] = useState({ email:"", password:"", nombres:"", apellidos:"" });
  const [captchaToken, setCaptchaToken] = useState("");
  const [err, setErr] = useState("");
  const [loading, setLoading] = useState(false);

  const onChange = (k, v) => setForm(f => ({ ...f, [k]: v }));
  const disabled = !SITE_KEY || !captchaToken || !form.email || !form.password || !form.nombres || !form.apellidos;

  const submit = async (e) => {
    e.preventDefault(); setErr(""); setLoading(true);
    try {
      const { data } = await axios.post(`${API}/api/auth/register`, { ...form, captchaToken });

      localStorage.setItem("auth.token", data.token);
      localStorage.setItem("auth.user", JSON.stringify(data.user));
      localStorage.setItem("auth.expiresAt", String(Date.now() + data.expires_in * 1000));
      nav("/panel");
    } catch (e2) {
      setErr(e2?.response?.data?.error || "No se pudo crear la cuenta");
    } finally {
      setLoading(false);
    }
  };

    return (
        <div className="container pag-reg">
            <div className="card">
                <h2>Crear cuenta</h2>
                <p className="subtitle">Regístrate para empezar a volar ✈️</p>

                <form className="form" onSubmit={submit} noValidate>
                    <div className="form-row">
                        <div className="form-col">
                            <label className="label" htmlFor="nombres">Nombres</label>
                            <input
                                id="nombres"
                                className="input"
                                value={form.nombres}
                                onChange={(e) => onChange("nombres", e.target.value)}
                                autoComplete="given-name"
                            />
                        </div>

                        <div className="form-col">
                            <label className="label" htmlFor="apellidos">Apellidos</label>
                            <input
                                id="apellidos"
                                className="input"
                                value={form.apellidos}
                                onChange={(e) => onChange("apellidos", e.target.value)}
                                autoComplete="family-name"
                            />
                        </div>
                    </div>

                    <div>
                        <label className="label" htmlFor="email">Correo</label>
                        <input
                            id="email"
                            className="input"
                            type="email"
                            placeholder="ej: nombre@correo.com"
                            value={form.email}
                            onChange={(e) => onChange("email", e.target.value)}
                            autoComplete="email"
                        />
                    </div>

                    <div>
                        <label className="label" htmlFor="password">Contraseña</label>
                        <input
                            id="password"
                            className="input"
                            type="password"
                            placeholder="mínimo 8 caracteres"
                            minLength={8}
                            value={form.password}
                            onChange={(e) => onChange("password", e.target.value)}
                            autoComplete="new-password"
                        />
                    </div>

                    <div>
                        {SITE_KEY ? (
                        <ReCAPTCHA
                        sitekey={SITE_KEY}
                        onChange={(t) => setCaptchaToken(t || "")}
                    />
                ) : (
                    <div className="error">Falta VITE_RECAPTCHA_SITE_KEY en el .env</div>
                )}
            </div>

            {err && <div className="error" role="alert">{err}</div>}

            <div className="actions">
                <button
                    className="btn btn-secondary"
                    type="submit"
                    disabled={loading || disabled}
                >
                    {loading ? "Creando..." : "Crear cuenta"}
                </button>

                <span>
                    ¿Ya tienes cuenta? <Link to="/login">Iniciar sesión</Link>
                </span>
            </div>
        </form>
    </div>
  </div>
    );

}
