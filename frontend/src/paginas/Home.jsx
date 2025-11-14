import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import CajaCompras from "./CajaCompras.jsx";
import { tipsApi, noticiasApi } from "../api/contenidoHome";
import ClimaHome from "./ClimaHome.jsx";
import { comprasApi } from "../api/compras";

export default function Home() {
  const [tips, setTips] = useState([]);
  const [noticias, setNoticias] = useState([]);
  const [topDestinos, setTopDestinos] = useState([]);
  const [periodoLabel, setPeriodoLabel] = useState("");
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
        const fmt = (d) => d.toISOString().slice(0, 10);

        const label = hoy.toLocaleDateString("es-GT", {
          month: "long",
          year: "numeric",
        });
        setPeriodoLabel(label.charAt(0).toUpperCase() + label.slice(1));

        const [tipsRes, noticiasRes, destinosRes] = await Promise.all([
          tipsApi.listPublic(),
          noticiasApi.listPublic(),
          comprasApi.topDestinos({
            desde: fmt(first),
            hasta: fmt(last),
            limit: 5,
          }),
        ]);

        setTips(Array.isArray(tipsRes.data) ? tipsRes.data : []);
        setNoticias(Array.isArray(noticiasRes.data) ? noticiasRes.data : []);
        setTopDestinos(
          Array.isArray(destinosRes.data) ? destinosRes.data : []
        );
      } catch (e) {
        console.error(e);
        setError("No se pudo cargar el contenido del home.");
      } finally {
        setLoading(false);
      }
    };
    cargar();
  }, []);

  const irAVuelos = (dest) => {
    const ciudad = dest.ciudadDestino || "";
    navigate(`/vuelos?destino=${encodeURIComponent(ciudad)}`);
  };

  return (
    <div className="home">
      <div className="container" style={{ padding: "20px 0 40px" }}>
        <CajaCompras />
      </div>

      <div className="container" style={{ marginBottom: 32 }}>
        <ClimaHome />
      </div>

      <div className="container" style={{ marginBottom: 32 }}>
        <section className="home-block home-top-destinos-block">
          <div className="home-block__header">
            <div>
              <h2 className="home-block__title">
                Destinos más reservados este mes
              </h2>
              <p className="home-block__subtitle">
                Basado en las reservas confirmadas de {periodoLabel}.
              </p>
            </div>
          </div>

          {loading && topDestinos.length === 0 && (
            <p className="hint">Cargando destinos…</p>
          )}

          {!loading && topDestinos.length === 0 && (
            <p className="hint">
              Aún no tenemos reservas registradas este mes. ¡Sé la primera en
              reservar tu próximo viaje! ✈️
            </p>
          )}

          {topDestinos.length > 0 && (
            <ul className="home-top-destinos">
              {topDestinos.map((d, idx) => (
                <li
                  key={d.idCiudadDestino || `${d.ciudadDestino}-${idx}`}
                  className="home-top-destinos__item"
                >
                  <div className="home-top-destinos__left">
                    <span className="home-top-destinos__rank">
                      #{idx + 1}
                    </span>
                    <div className="home-top-destinos__info">
                      <span className="home-top-destinos__city">
                        {d.ciudadDestino}, {d.paisDestino}
                      </span>
                      {idx === 0 && (
                        <span className="home-top-destinos__badge">
                          Más popular
                        </span>
                      )}
                    </div>
                  </div>

                  <button
                    type="button"
                    className="btn-link home-top-destinos__cta"
                    onClick={() => irAVuelos(d)}
                  >
                    Ver vuelos
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>

      <section className="home-sections container">
        {error && (
          <div className="error" style={{ marginBottom: 16 }} role="alert">
            {error}
          </div>
        )}

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
