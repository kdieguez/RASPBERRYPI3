import { useEffect, useState } from "react";
import CajaCompras from "./CajaCompras.jsx";
import { tipsApi, noticiasApi } from "../api/contenidoHome";
import ClimaHome from "./ClimaHome.jsx";

export default function Home() {
  const [tips, setTips] = useState([]);
  const [noticias, setNoticias] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const cargar = async () => {
      try {
        setLoading(true);
        setError("");
        const [tipsRes, noticiasRes] = await Promise.all([
          tipsApi.listPublic(),
          noticiasApi.listPublic(),
        ]);
        setTips(Array.isArray(tipsRes.data) ? tipsRes.data : []);
        setNoticias(Array.isArray(noticiasRes.data) ? noticiasRes.data : []);
      } catch (e) {
        console.error(e);
        setError("No se pudo cargar el contenido del home.");
      } finally {
        setLoading(false);
      }
    };
    cargar();
  }, []);

  return (
    <div className="home">
            <div className="container" style={{ padding: "20px 0 40px" }}>
        <CajaCompras />
      </div>

            <div className="container" style={{ marginBottom: 32 }}>
        <ClimaHome />
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
                      {String(n.fechaPublicacion).replace("T", " ").slice(0, 16)}
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
