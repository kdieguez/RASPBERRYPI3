import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import "./header.css";
import { isLoggedIn, getUser, clearAuth } from "../lib/auth";
import { clasesApi } from "../api/adminCatalogos";
import { configApi } from "../api/config";

export default function Header(){
  const [open, setOpen] = useState(false);
  const [adminOpen, setAdminOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);

  const [clases, setClases] = useState([]);
  const [claseSel, setClaseSel] = useState("");

  const [origen, setOrigen] = useState("");
  const [destino, setDestino] = useState("");
  const [fsd, setFsd] = useState("");
  const [fsh, setFsh] = useState("");
  const [frd, setFrd] = useState("");
  const [frh, setFrh] = useState("");
  const [pmin, setPmin] = useState("");
  const [pmax, setPmax] = useState("");
  const [direct, setDirect] = useState(false);

  const [user, setUser] = useState(getUser());
  const [brand, setBrand] = useState({ nombre: "Aerolíneas", logo: "" });
  const nav = useNavigate();

  useEffect(() => {
    const onChange = () => setUser(getUser());
    window.addEventListener("auth:changed", onChange);
    return () => window.removeEventListener("auth:changed", onChange);
  }, []);

  useEffect(() => {
    (async () => {
      try {
        const { data } = await clasesApi.list();
        setClases(Array.isArray(data) ? data : []);
      } catch {
        setClases([]);
      }
    })();
  }, []);

  useEffect(() => {
    (async () => {
      try {
        const cfg = await configApi.getHeader();
        setBrand(cfg);
      } catch (e) {
        console.warn("No se pudo cargar /api/config/header", e);
      }
    })();
  }, []);

  const isAdmin = !!user && Number(user.idRol) === 1;

  const logout = () => {
    clearAuth();
    nav("/login");
  };

  const firstLetter = (user?.nombres || "").trim().charAt(0).toUpperCase();

  const closeAdminOnBlur = (e) => {
    if (!e.currentTarget.contains(e.relatedTarget)) setAdminOpen(false);
  };

  const closeMenus = () => { setOpen(false); setAdminOpen(false); };

  const toggleSearch = () => {
    setSearchOpen(v => !v);
    setOpen(false);
    setAdminOpen(false);
  };

  const submitSearch = (e) => {
    e.preventDefault();
    const params = new URLSearchParams();
    const add = (k,v) => { if (v && String(v).trim() !== "") params.set(k, String(v).trim()); };
    add("origen", origen);
    add("destino", destino);
    add("fsd", fsd);
    add("fsh", fsh);
    add("frd", frd);
    add("frh", frh);
    add("pmin", pmin);
    add("pmax", pmax);
    if (direct) params.set("direct","1");
    if (claseSel) params.set("clase", claseSel);

    nav("/vuelos" + (params.toString() ? `?${params.toString()}` : ""));
    setSearchOpen(false);
  };

  const clearSearch = () => {
    setOrigen(""); setDestino("");
    setFsd(""); setFsh(""); setFrd(""); setFrh("");
    setPmin(""); setPmax("");
    setDirect(false);
    setClaseSel("");
  };

  return (
    <header className="hdr">
      <div className="container hdr__row">
        <Link className="hdr__brand" to="/" onClick={closeMenus}>
          {brand.logo ? (
            <img
              src={brand.logo}
              alt={brand.nombre}
              className="hdr__brand-logo"
              onError={(e)=>{ e.currentTarget.style.display = "none"; }}
            />
          ) : (
            <svg width="34" height="34" viewBox="0 0 24 24" aria-hidden="true">
              <path fill="currentColor" d="M2 13c7-1 10-4 12-9l2 1-2 6 7 1 1 2-7 1 2 6-2 1c-2-5-5-8-12-9v-1Z"/>
            </svg>
          )}
          <span>{brand.nombre}</span>
        </Link>

        <button
          className="hdr__iconbtn"
          aria-label="Buscar vuelos"
          title="Buscar vuelos"
          onClick={toggleSearch}
        >
          <svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true">
            <g fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="11" cy="11" r="7"/>
              <path d="m21 21l-3.5-3.5"/>
            </g>
          </svg>
        </button>

        <nav className={`hdr__nav ${open ? "is-open" : ""}`} aria-label="Principal">
          <Link to="/vuelos" className="hdr__link" onClick={closeMenus}>
            Vuelos
          </Link>

          {isLoggedIn() && (
            <>
              <Link to="/compras/historial" className="hdr__link" onClick={closeMenus}>
                Historial
              </Link>
              <Link to="/compras/carrito" className="hdr__link" onClick={closeMenus}>
                Carrito
              </Link>
            </>
          )}

          {isAdmin && (
            <div className="hdr__menu" tabIndex={-1} onBlur={closeAdminOnBlur}>
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
                <Link to="/admin/usuarios" className="hdr__drop-item" role="menuitem" onClick={closeMenus}>Usuarios</Link>
                <Link to="/admin/vuelos/nuevo" className="hdr__drop-item" role="menuitem" onClick={closeMenus}>Crear vuelo</Link>
                <Link to="/admin/paises" className="hdr__drop-item" role="menuitem" onClick={closeMenus}>Países</Link>
                <Link to="/admin/ciudades" className="hdr__drop-item" role="menuitem" onClick={closeMenus}>Ciudades</Link>
                <Link to="/admin/rutas" className="hdr__drop-item" role="menuitem" onClick={closeMenus}>Rutas</Link>
                <Link to="/admin/config" className="hdr__drop-item" role="menuitem" onClick={closeMenus}> Header & Footer </Link>
                <Link to="/admin/reservas" className="hdr__drop-item" role="menuitem" onClick={closeMenus}> Historial de reservas</Link>
              </div>
            </div>
          )}

          {isLoggedIn() ? (
            <div className="hdr__session">
              <Link to="/perfil" className="hdr__avatar" title="Mi perfil" onClick={closeMenus}>
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
              <Link className="hdr__cta hdr__cta--ghost" to="/login" onClick={closeMenus}>Iniciar Sesión</Link>
            </div>
          )}
        </nav>

        <button
          className="hdr__burger"
          aria-label="Abrir menú"
          onClick={() => setOpen(v=>!v)}
        >
          <span/><span/><span/>
        </button>
      </div>

      <div className={`hdr__search ${searchOpen ? "is-open" : ""}`}>
        <form className="hdr__sform container" onSubmit={submitSearch}>
          <div className="hdr__srow">
            <div className="hdr__scol">
              <label className="label">Origen</label>
              <input
                className="input"
                placeholder="Ciudad o país"
                value={origen}
                onChange={(e)=>setOrigen(e.target.value)}
              />
            </div>
            <div className="hdr__scol">
              <label className="label">Destino</label>
              <input
                className="input"
                placeholder="Ciudad o país"
                value={destino}
                onChange={(e)=>setDestino(e.target.value)}
              />
            </div>
            <div className="hdr__scol">
              <label className="label">Salida (de)</label>
              <input className="input" type="date" value={fsd} onChange={(e)=>setFsd(e.target.value)} />
            </div>
            <div className="hdr__scol">
              <label className="label">Salida (a)</label>
              <input className="input" type="date" value={fsh} onChange={(e)=>setFsh(e.target.value)} />
            </div>
          </div>

          <div className="hdr__srow">
            <div className="hdr__scol">
              <label className="label">Regreso (de)</label>
              <input className="input" type="date" value={frd} onChange={(e)=>setFrd(e.target.value)} />
            </div>
            <div className="hdr__scol">
              <label className="label">Regreso (a)</label>
              <input className="input" type="date" value={frh} onChange={(e)=>setFrh(e.target.value)} />
            </div>

            <div className="hdr__scol">
              <label className="label">Clase</label>
              <select className="input" value={claseSel} onChange={(e)=>setClaseSel(e.target.value)}>
                <option value="">Todas</option>
                {clases.map((c) => (
                  <option key={c.idClase} value={c.idClase}>{c.nombre}</option>
                ))}
              </select>
            </div>

            <div className="hdr__scol">
              <label className="label">Precio mínimo</label>
              <input className="input" type="number" min="0" inputMode="numeric" placeholder="0" value={pmin} onChange={(e)=>setPmin(e.target.value)} />
            </div>
            <div className="hdr__scol">
              <label className="label">Precio máximo</label>
              <input className="input" type="number" min="0" inputMode="numeric" placeholder="5000" value={pmax} onChange={(e)=>setPmax(e.target.value)} />
            </div>
          </div>

          <div className="hdr__srow hdr__srow--end">
            <label className="check">
              <input type="checkbox" checked={direct} onChange={(e)=>setDirect(e.target.checked)} />
              <span>Solo directos</span>
            </label>
            <div className="actions">
              <button type="button" className="btn" onClick={clearSearch}>Limpiar</button>
              <button className="btn btn-secondary" type="submit">Buscar</button>
            </div>
          </div>
        </form>
      </div>
    </header>
  );
}
