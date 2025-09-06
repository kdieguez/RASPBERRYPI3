import { useState } from "react";
import "./header.css";

export default function Header(){
  const [open, setOpen] = useState(false);

  const nav = [
    { label: "Vuelos", href: "#" },
    { label: "Rutas", href: "#" },
    { label: "Flota", href: "#" },
    { label: "Ofertas", href: "#" },
    { label: "Admin", href: "#" },
  ];

  return (
    <header className="hdr">
      <div className="container hdr__row">
        <a className="hdr__brand" href="#">
          <svg width="34" height="34" viewBox="0 0 24 24" aria-hidden="true">
            <path fill="currentColor" d="M2 13c7-1 10-4 12-9l2 1-2 6 7 1 1 2-7 1 2 6-2 1c-2-5-5-8-12-9v-1Z"/>
          </svg>
          <span>Aerolíneas</span>
        </a>

        <nav className={`hdr__nav ${open ? "is-open" : ""}`} aria-label="Principal">
          {nav.map((item) => (
            <a key={item.label} href={item.href} className="hdr__link">
              {item.label}
            </a>
          ))}
          <a href="#" className="hdr__cta">Reservar</a>
        </nav>

        <button className="hdr__burger" aria-label="Abrir menú" onClick={() => setOpen(v=>!v)}>
          <span/>
          <span/>
          <span/>
        </button>
      </div>
    </header>
  );
}
