import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import CajaCompras from "./CajaCompras.jsx";
import ClimaHome from "./ClimaHome.jsx";
import { tipsApi, noticiasApi } from "../api/contenidoHome";
import { comprasApi } from "../api/compras";
import { vuelosApi } from "../api/adminCatalogos";

const toDate = (val) => {
  if (val === null || val === undefined) return null;
  if (Array.isArray(val)) {
    const [Y, M, D, h = 0, m = 0, s = 0] = val;
    const d = new Date(Y, (M ?? 1) - 1, D ?? 1, h, m, s);
    return Number.isNaN(d.getTime()) ? null : d;
  }
  if (typeof val === "number") {
    const d = new Date(val);
    return Number.isNaN(d.getTime()) ? null : d;
  }
  if (typeof val === "string") {
    const s = val.trim();
    let d = new Date(s);
    if (!Number.isNaN(d.getTime())) return d;
    const m = s.match(
      /^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})(?::(\d{2})(?:\.\d{1,9})?)?(?:Z|[+-]\d{2}:?\d{2})?$/i
    );
    if (m) {
      const [, Y, Mo, D, hh, mi, se] = m;
      d = new Date(+Y, +Mo - 1, +D, +hh, +mi, se ? +se : 0);
      if (!Number.isNaN(d.getTime())) return d;
    }
    const mm = s.match(/^(\d{4}-\d{2}-\d{2})[ T](\d{2}:\d{2})$/);
    if (mm) {
      const d2 = new Date(`${mm[1]}T${mm[2]}:00`);
      return Number.isNaN(d2.getTime()) ? null : d2;
    }
  }
  return null;
};

const isSameDay = (a, b) => {
  if (!a || !b) return false;
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
};

const fmtHora = (val) => {
  const d = toDate(val);
  if (!d) return "—";
  return d.toLocaleTimeString("es-GT", {
    hour: "2-digit",
    minute: "2-digit",
  });
};

// Calcula estado + porcentaje de avance del vuelo
const getProgresoVuelo = (vuelo) => {
  const ahora = new Date();
  const salida = toDate(vuelo.fechaSalida);
  const llegada = toDate(vuelo.fechaLlegada);

  if (!salida || !llegada) {
    return {
      estado: "Sin horario",
      percent: 0,
      label: "Horario no disponible",
    };
  }

  // Aún no despega
  if (ahora < salida) {
    return {
      estado: "No ha despegado",
      percent: 0,
    };
  }

  // Ya aterrizó
  if (ahora >= llegada) {
    return {
      estado: "Aterrizado",
      percent: 100,
    };
  }

  // En vuelo
  const total = llegada.getTime() - salida.getTime();
  const transcurrido = ahora.getTime() - salida.getTime();
  let percent = Math.round((transcurrido / total) * 100);

  if (!Number.isFinite(percent) || percent < 0) percent = 0;
  if (percent > 100) percent = 100;
  if (percent > 0 && percent < 8) percent = 8; // que se vea la barrita

  return {
    estado: "En vuelo",
    percent,
    label: `En ruta`,
  };
};

export default function Home() {
  const [tips, setTips] = useState([]);
  const [noticias, setNoticias] = useState([]);
  const [topDestinos, setTopDestinos] = useState([]);
  const [vuelosHoy, setVuelosHoy] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const navigate = useNavigate();

  useEffect(() => {
    const cargar = async () => {
      try {
        setLoading(true);
        setError("");

        const hoy = new Date();
        const y = hoy.getFullYear();
        const m = hoy.getMonth(); 
        const first = new Date(y, m, 1);
        const last = new Date(y, m + 1, 0);
        const fmtDate = (d) => d.toISOString().slice(0, 10);

        const [tipsRes, noticiasRes, destinosRes, vuelosRes] = await Promise.all([
          tipsApi.listPublic(),
          noticiasApi.listPublic(),
          comprasApi.topDestinos({
            desde: fmtDate(first),
            hasta: fmtDate(last),
            limit: 5,
          }),
          vuelosApi.listPublic(),
        ]);

        // Tips & noticias
        setTips(Array.isArray(tipsRes.data) ? tipsRes.data : []);
        setNoticias(Array.isArray(noticiasRes.data) ? noticiasRes.data : []);

        // Top destinos 
        setTopDestinos(
          Array.isArray(destinosRes.data) ? destinosRes.data : []
        );

        // Vuelos de hoy
        const dataVuelos = Array.isArray(vuelosRes.data) ? vuelosRes.data : [];
        const ahora = new Date();

        const hoyList = dataVuelos
          .filter(
            (v) =>
              v.activo !== false &&
              !String(v.estado || "").toLowerCase().includes("cancel")
          )
          .filter((v) => {
            const fs = toDate(v.fechaSalida);
            return fs && isSameDay(fs, ahora);
          })
          .sort((a, b) => {
            const da = toDate(a.fechaSalida)?.getTime() || 0;
            const db = toDate(b.fechaSalida)?.getTime() || 0;
            return da - db;
          });

        setVuelosHoy(hoyList);
      } catch (e) {
        console.error(e);
        setError("No se pudo cargar el contenido del home.");
      } finally {
        setLoading(false);
      }
    };
    cargar();
  }, []);

  const irAVueloDetalle = (idVuelo) => {
    if (!idVuelo) return;
    navigate(`/vuelos/${idVuelo}`);
  };

  const irAVuelosPorDestino = (dest) => {
    const ciudad = dest.ciudadDestino || "";
    if (!ciudad) return;
    navigate(`/vuelos?destino=${encodeURIComponent(ciudad)}`);
  };

  return (

    <div className="home">
      <h1 style={{justifyContent: "center", alignContent: "center", textAlign: "center"}}>¡Bienvenido a nuestra Aerolínea!</h1>
      {/* Caja de búsqueda / compras */}
      <div className="container" style={{ padding: "20px 0 40px" }}>
        <CajaCompras />
      </div>

      {/* Clima */}
      <div className="container" style={{ marginBottom: 32 }}>
        <ClimaHome />
      </div>

      {/* Vuelos de hoy con barrita de progreso */}
      <div className="container" style={{ marginBottom: 32 }}>
        <section className="home-block home-block--accent">
          <div className="home-block__header">
            <h2 className="home-block__title">Vuelos de hoy</h2>
            {!loading && vuelosHoy.length > 0 && (
              <span className="home-block__subtitle">
                Estado en tiempo casi real de los vuelos programados para hoy
              </span>
            )}
          </div>

          {loading && vuelosHoy.length === 0 && <p>Cargando vuelos…</p>}

          {!loading && vuelosHoy.length === 0 && (
            <p className="hint">
              Hoy no hay vuelos programados o ya no hay vuelos activos.
            </p>
          )}

          {vuelosHoy.length > 0 && (
            <ul className="home-today">
              {vuelosHoy.map((v) => {
                const prog = getProgresoVuelo(v);
                return (
                  <li key={v.idVuelo} className="home-today__item">
                    <div className="home-today__left">
                      <div className="home-today__route">
                        <span className="badge">{v.codigo}</span>
                        <div>
                          <strong>
                            {v.origen} → {v.destino}
                          </strong>
                          {(v.origenPais || v.destinoPais) && (
                            <div className="home-today__countries">
                              {v.origenPais || "—"} → {v.destinoPais || "—"}
                            </div>
                          )}
                        </div>
                      </div>

                      <div className="home-today__times">
                        <span>
                          <span className="label">Salida</span>{" "}
                          {fmtHora(v.fechaSalida)}
                        </span>
                        <span>
                          <span className="label">Llegada</span>{" "}
                          {fmtHora(v.fechaLlegada)}
                        </span>
                      </div>
                    </div>

                    <div className="home-today__right">
                      <div className="home-today__status">{prog.estado}</div>
                      <div className="home-today__bar-track">
                        <div
                          className="home-today__bar-fill"
                          style={{ width: `${prog.percent}%` }}
                        />
                      </div>
                      <div className="home-today__label">{prog.label}</div>
                      <button
                        type="button"
                        className="btn btn-small btn-secondary"
                        onClick={() => irAVueloDetalle(v.idVuelo)}
                      >
                        Ver vuelo
                      </button>
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </section>
      </div>

      {/* Top destinos del mes*/}
      <div className="container" style={{ marginBottom: 32 }}>
        <section className="home-block">
          <h2 className="home-block__title">Destinos más reservados este mes</h2>

          {loading && topDestinos.length === 0 && <p>Cargando destinos…</p>}

          {!loading && topDestinos.length === 0 && (
            <p className="hint">
              Aún no tenemos reservas registradas este mes.
            </p>
          )}

          {topDestinos.length > 0 && (
            <ul className="home-top-list">
              {topDestinos.map((d, idx) => (
                <li key={d.idCiudadDestino || idx} className="home-top-list__item">
                  <span className="home-top-list__rank">#{idx + 1}</span>
                  <span className="home-top-list__city">
                    {d.ciudadDestino}, {d.paisDestino}
                  </span>
                  <button
                    type="button"
                    className="btn btn-small"
                    onClick={() => irAVuelosPorDestino(d)}
                  >
                    Ver vuelos
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      {/* Tips + Noticias */}
      <section className="home-sections container">
        {error && (
          <div className="error" style={{ marginBottom: 16 }} role="alert">
            {error}
          </div>
        )}

        {/* Tips */}
        <div className="home-block">
          <h2 className="home-block__title">Tips de viaje</h2>

          {loading && tips.length === 0 && <p>Cargando tips…</p>}

          {!loading && tips.length === 0 && (
            <p className="hint">Pronto tendremos tips de viaje para ti ✈️</p>
          )}

          {tips.length > 0 && (
            <div className="home-tips">
              {tips.map((t) => (
                <details key={t.idTip} className="home-tip">
                  <summary className="home-tip__summary">
                    <span className="home-tip__bullet">●</span>
                    <span>{t.titulo}</span>
                  </summary>
                  {t.descripcion && (
                    <p className="home-tip__body">{t.descripcion}</p>
                  )}
                </details>
              ))}
            </div>
          )}
        </div>

        {/* Noticias */}
        <div className="home-block">
          <h2 className="home-block__title">Noticias</h2>

          {loading && noticias.length === 0 && <p>Cargando noticias…</p>}

          {!loading && noticias.length === 0 && (
            <p className="hint">
              Aún no hay noticias publicadas. El equipo de GuateFly estará
              compartiendo novedades muy pronto.
            </p>
          )}

          <div className="home-news">
            {noticias.map((n) => (
              <article key={n.idNoticia} className="home-news__card">
                {n.urlImagen && (
                  <div className="home-news__image">
                    <img src={n.urlImagen} alt={n.titulo} />
                  </div>
                )}
                <div className="home-news__content">
                  <h3 className="home-news__title">{n.titulo}</h3>
                  {n.fechaPublicacion && (
                    <div className="home-news__date">
                      {String(n.fechaPublicacion)
                        .replace("T", " ")
                        .slice(0, 16)}
                    </div>
                  )}
                  {n.contenido && (
                    <p className="home-news__text">{n.contenido}</p>
                  )}
                </div>
              </article>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
