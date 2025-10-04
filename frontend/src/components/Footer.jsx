import { useEffect, useState } from "react";
import "./footer.css";
import { configApi } from "../api/config";

export default function Footer() {
  const [brand, setBrand] = useState({ nombre: "Aerolíneas", logo: "" });

  const [cfg, setCfg] = useState({
    telefono: "",
    direccion: "",
    copyRight: "",
    email: "",
    tagline: "",
  });

  const [company, setCompany] = useState([]);         
  const [contactoExtra, setContactoExtra] = useState([]); 

  useEffect(() => {
    (async () => {
      try {
        const headerCfg = await configApi.getHeader();
        if (headerCfg) {
          setBrand({
            nombre: headerCfg.nombre || "Aerolíneas",
            logo: headerCfg.logo || "",
          });
        }
      } catch (e) {
        console.warn("No se pudo cargar /api/config/header", e);
      }

      try {
        const f = await configApi.getFooter();
        const norm = (obj) =>
          Object.fromEntries(
            Object.entries(obj || {}).map(([k, v]) => [String(k).toLowerCase(), v])
          );
        const fx = norm(f);

        setCfg((prev) => ({
          ...prev,
          telefono:  fx.telefono  ?? prev.telefono,
          direccion: fx.direccion ?? prev.direccion,
          copyRight: fx.copyright ?? prev.copyRight, 
          email:     fx.email     ?? prev.email,
          tagline:   fx.tagline   ?? prev.tagline,
        }));

        setCompany(Array.isArray(f?.company) ? f.company : []);
        setContactoExtra(
          Array.isArray(f?.contactoextra)
            ? f.contactoextra
            : Array.isArray(f?.contactoExtra)
            ? f.contactoExtra
            : []
        );
      } catch (e) {
        console.warn("No se pudo cargar /api/config/footer", e);
      }
    })();
  }, []);

  const currentYear = new Date().getFullYear();
  const copy =
    cfg.copyRight && String(cfg.copyRight).trim()
      ? cfg.copyRight
      : `© ${currentYear} ${brand.nombre}. Todos los derechos reservados.`;

  return (
    <footer className="ftr">
      <div className="container ftr__grid">
        <div className="ftr__brand">
          {brand.logo ? (
            <img
              src={brand.logo}
              alt={brand.nombre}
              className="ftr__brand-logo"
              onError={(e) => {
                e.currentTarget.style.display = "none";
              }}
            />
          ) : (
            <div className="ftr__logo" aria-hidden="true">
              ✈
            </div>
          )}
          <h3>{brand.nombre}</h3>
          {cfg.tagline && <p>{cfg.tagline}</p>}
        </div>

        <nav className="ftr__col" aria-label="Compañía">
          <h4>Compañía</h4>
          {company?.length ? (
            company.map((it, i) => (
              <a key={i} href={it.href || "#"}>
                {it.label || "Enlace"}
              </a>
            ))
          ) : (
            <>
              <a href="#">Nosotros</a>
              <a href="#">Trabaja con nosotros</a>
              <a href="#">Prensa</a>
            </>
          )}
        </nav>

        <div className="ftr__col">
          <h4>Contacto</h4>
          {cfg.telefono && <p>{cfg.telefono}</p>}
          {cfg.email && (
            <p>
              <a href={`mailto:${cfg.email}`} className="ftr__contact-link">
                {cfg.email}
              </a>
            </p>
          )}
          {cfg.direccion && <p>{cfg.direccion}</p>}

          {contactoExtra?.map((it, i) => (
            <p key={i}>
              {it?.href ? (
                <a href={it.href} target="_blank" rel="noreferrer">
                  {it.label || it.value}
                </a>
              ) : (
                <>
                  {it?.label ? `${it.label}: ` : ""}
                  {it?.value}
                </>
              )}
            </p>
          ))}
        </div>
      </div>

      <div className="ftr__bar">
        <div className="container ftr__bar__row">
          <small>{copy}</small>
          <div className="ftr__links">
            <a href="#">Privacidad</a>
            <span>•</span>
            <a href="#">Términos</a>
          </div>
        </div>
      </div>
    </footer>
  );
}
