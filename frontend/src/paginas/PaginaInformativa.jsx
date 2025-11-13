import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import paginasInfoApi from "../api/paginasInfo";

export default function PaginaInformativa() {
  const { slug } = useParams();
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  // slug = "checkin", "abordaje", "equipaje", etc.
  useEffect(() => {
    (async () => {
      setLoading(true);
      setError("");
      try {
        const { data } = await paginasInfoApi.list();
        const paginas = Array.isArray(data) ? data : [];
        const encontrada =
          paginas.find(
            (p) =>
              (p.nombrePagina && p.nombrePagina.toLowerCase() === slug) ||
              (p.titulo && p.titulo.toLowerCase().replace(/\s+/g, "") === slug)
          ) || null;

        if (!encontrada) {
          setError("No se encontró la información de esta página.");
          setData(null);
          return;
        }

        // cargamos el detalle por ID para traer secciones + media
        const detalle = await paginasInfoApi.getById(encontrada.idPagina);
        setData(detalle.data);
      } catch (e) {
        console.error(e);
        setError("No se pudo cargar la información de esta página.");
      } finally {
        setLoading(false);
      }
    })();
  }, [slug]);

  if (loading) {
    return (
      <div className="container" style={{ padding: "32px 0 40px" }}>
        <h1>Información</h1>
        <p>Cargando información…</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container" style={{ padding: "32px 0 40px" }}>
        <h1>Información</h1>
        <p style={{ color: "red" }}>{error}</p>
      </div>
    );
  }

  if (!data) {
    return (
      <div className="container" style={{ padding: "32px 0 40px" }}>
        <h1>Información</h1>
        <p>No se pudo cargar la información de esta página.</p>
      </div>
    );
  }

  return (
    <div className="container" style={{ padding: "32px 0 40px" }}>
      <h1>{data.titulo || "Información"}</h1>

      {data.descripcion && (
        <p style={{ maxWidth: 800, marginTop: 8 }}>{data.descripcion}</p>
      )}

      <div style={{ marginTop: 24 }}>
        {(data.secciones || []).map((sec, idx) => (
          <section
            key={sec.idSeccion || idx}
            style={{ marginBottom: 32, maxWidth: 900 }}
          >
            <h2
              style={{
                fontSize: "1.25rem",
                marginBottom: 8,
                fontWeight: 600,
              }}
            >
              {sec.nombreSeccion || `Punto ${idx + 1}`}
            </h2>
            {sec.descripcion && (
              <p style={{ marginBottom: 12 }}>{sec.descripcion}</p>
            )}

            {/* IMÁGENES / MEDIA: normal vs infografía */}
            {(sec.media || []).length > 0 && (
              <div className="info-media-list">
                {sec.media.map((m) => {
                  const isInfografia = m.tipoMedia === "INFOGRAFIA";
                  return (
                    <figure
                      key={m.idMedia || m.url}
                      className={
                        "info-media-item " +
                        (isInfografia
                          ? "info-media-item--infografia"
                          : "info-media-item--imagen")
                      }
                    >
                      {m.url && (
                        <img
                          className={
                            "info-media-img " +
                            (isInfografia
                              ? "info-media-img--infografia"
                              : "info-media-img--imagen")
                          }
                          src={m.url}
                          alt={
                            isInfografia
                              ? "Infografía informativa"
                              : "Imagen informativa"
                          }
                        />
                      )}
                    </figure>
                  );
                })}
              </div>
            )}
          </section>
        ))}
      </div>
    </div>
  );
}
