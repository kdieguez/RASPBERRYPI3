import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import "./header.css";
import { isLoggedIn, getUser, clearAuth } from "../lib/auth";

export default function Header(){
  const [open, setOpen] = useState(false);
  const [adminOpen, setAdminOpen] = useState(false);
  const [user, setUser] = useState(getUser());
  const nav = useNavigate();

  useEffect(() => {
    const onChange = () => setUser(getUser());
    window.addEventListener("auth:changed", onChange);
    return () => window.removeEventListener("auth:changed", onChange);
  }, []);

  const links = [
    { label: "Vuelos", href: "#" },
    { label: "Rutas", href: "#" },
    { label: "Flota", href: "#" },
    { label: "Ofertas", href: "#" },
  ];

  const isAdmin = !!user && Number(user.idRol) === 1;

  const logout = () => {
    clearAuth();
    nav("/login");
  };

  const firstLetter = (user?.nombres || "").trim().charAt(0).toUpperCase();

  const closeAdminOnBlur = (e) => {
    if (!e.currentTarget.contains(e.relatedTarget)) setAdminOpen(false);
  };

  return (
    <header className="hdr">
      <div className="container hdr__row">
        <Link className="hdr__brand" to="/">
          <svg width="34" height="34" viewBox="0 0 24 24" aria-hidden="true">
            <path fill="currentColor" d="M2 13c7-1 10-4 12-9l2 1-2 6 7 1 1 2-7 1 2 6-2 1c-2-5-5-8-12-9v-1Z"/>
          </svg>
          <span>Aerolíneas</span>
        </Link>

        <nav className={`hdr__nav ${open ? "is-open" : ""}`} aria-label="Principal">
          {links.map((item) => (
            <a key={item.label} href={item.href} className="hdr__link">
              {item.label}
            </a>
          ))}

          {isAdmin && (
            <div
              className="hdr__menu"
              tabIndex={-1}
              onBlur={closeAdminOnBlur}
            >
              <button
                type="button"
                className="hdr__link hdr__menu-btn"
                aria-haspopup="menu"
                aria-expanded={adminOpen}
                onClick={() => setAdminOpen(o => !o)}
              >
                Admin ▾
              </button>
              <div className={`hdr__dropdown ${adminOpen ? "is-open" : ""}`} role="menu">
                <Link to="/admin/usuarios" className="hdr__drop-item" role="menuitem" onClick={() => setAdminOpen(false)}>
                  Usuarios
                </Link>
                <Link to="/admin/vuelos/nuevo" className="hdr__drop-item" role="menuitem" onClick={() => setAdminOpen(false)}>
                  Crear vuelo
                </Link>
              </div>
            </div>
          )}

          <a href="#" className="hdr__cta">Reservar</a>

          {/* Sesión */}
          {isLoggedIn() ? (
            <div className="hdr__session">
              <Link to="/perfil" className="hdr__avatar" title="Mi perfil">
                {firstLetter || (
                  <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true">
                    <path fill="currentColor" d="M12 12a5 5 0 1 0-5-5a5 5 0 0 0 5 5Zm0 2c-4.418 0-8 2.239-8 5v1h16v-1c0-2.761-3.582-5-8-5Z"/>
                  </svg>
                )}
              </Link>
              <button className="hdr__logout" onClick={logout}>Salir</button>
            </div>
          ) : (
            <div className="hdr__session">
              <Link className="hdr__link" to="/login">Iniciar sesión</Link>
              <Link className="hdr__cta hdr__cta--ghost" to="/registro">Crear cuenta</Link>
            </div>
          )}
        </nav>

        <button
          className="hdr__burger"
          aria-label="Abrir menú"
          onClick={() => setOpen(v=>!v)}
        >
          <span/>
          <span/>
          <span/>
        </button>
      </div>
    </header>
  );
}
