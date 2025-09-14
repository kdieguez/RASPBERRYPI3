import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { vuelosApi } from "../../api/adminCatalogos";

const fmtDt = (s) => new Date(s).toLocaleString("es-MX", { dateStyle:"medium", timeStyle:"short" });

export default function AdminVuelosList() {
  const [items, setItems] = useState([]);
  const [err, setErr] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        setLoading(true); setErr("");
        const { data } = await vuelosApi.listAdmin();
        setItems(data || []);
      } catch (e) {
        setErr(e?.response?.data?.error || "No se pudieron cargar los vuelos");
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  return (
    <div className="container pag-vuelos-list">
      <div className="vlist__head">
        <h1>Vuelos (admin)</h1>
        <Link to="/admin/vuelos/nuevo" className="btn btn-secondary">Crear vuelo</Link>
      </div>

      <div className="card">
        {loading ? (
          <>
            <div className="skl title" />
            <div className="skl row" /><div className="skl row" />
          </>
        ) : err ? (
          <div className="error">{err}</div>
        ) : !items.length ? (
          <div className="empty">No hay vuelos.</div>
        ) : (
          <ul className="vlist">
            {items.map(v => (
              <li key={v.idVuelo} className="vitem">
                <div className="vitem__main">
                  <div className="vitem__route">
                    <div className="vitem__code">
                      <span className="badge">{v.codigo}</span>
                      {v.activo ? <span className="pill pill--ok">Activo</span> : <span className="pill">Inactivo</span>}
                    </div>
                    <div className="vitem__city">
                      <strong>{v.origen && v.destino ? `${v.origen} → ${v.destino}` : `Ruta #${v.idRuta}`}</strong>
                    </div>
                    <div className="vitem__times">
                      <div><span className="label">Salida</span><span>{fmtDt(v.fechaSalida)}</span></div>
                      <div><span className="label">Llegada</span><span>{fmtDt(v.fechaLlegada)}</span></div>
                    </div>
                  </div>
                </div>

                <div className="vitem__actions">
                  <Link className="btn btn-secondary" to={`/admin/vuelos/${v.idVuelo}`}>Editar</Link>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
