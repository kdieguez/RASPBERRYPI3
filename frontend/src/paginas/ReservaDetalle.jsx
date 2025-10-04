import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { comprasApi } from "../api/compras";
import { getUser } from "../lib/auth";
import RouteLine from "../components/RouteLine";
import "../styles/historial.css";

const money = (n) =>
  Number(n || 0).toLocaleString("es-GT", { style: "currency", currency: "GTQ" });

const dt = (s) =>
  s ? new Date(s).toLocaleString("es-GT", { dateStyle: "medium", timeStyle: "short" }) : "—";

const estadoName = (id) =>
  ({ 1: "Confirmada", 2: "Pagada", 3: "Reembolsada", 4: "Expirada" }[Number(id)] ||
    `Estado ${id}`);

const route = (ciudad, pais) => {
  const c = (ciudad || "").trim();
  const p = (pais || "").trim();
  if (!c && !p) return "—";
  if (!c) return p;
  if (!p) return c;
  return `${c}, ${p}`;
};

export default function ReservaDetalle() {
  const { id } = useParams();
  const nav = useNavigate();
  const u = getUser();

  const [det, setDet] = useState(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    if (!u) {
      nav("/login");
      return;
    }
    (async () => {
      try {
        setLoading(true);
        setErr("");
        const { data } = await comprasApi.getReserva(id);
        setDet(data);
      } catch (e) {
        setErr(e?.response?.data?.error || e.message || "No se pudo cargar la reserva");
      } finally {
        setLoading(false);
      }
    })();
  }, [id]);

  const onDownload = async () => {
    try {
      setDownloading(true);
      const { data } = await comprasApi.downloadBoleto(id);
      const blob = new Blob([data], { type: "application/pdf" });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      const codigo = det?.codigo ? String(det.codigo) : `reserva-${id}`;
      a.download = `boleto-${codigo}.pdf`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (e) {
      alert(e?.response?.data?.error || e.message || "No se pudo descargar el boleto");
    } finally {
      setDownloading(false);
    }
  };

  if (!u) return null;

  return (
    <div className="container historial">
      <div className="h-head">
        <h1>
          Reserva {det?.codigo ? det.codigo : `#${id}`}
        </h1>
        <div style={{ display: "flex", gap: 8 }}>
          <button className="btn btn-primary" onClick={onDownload} disabled={downloading || loading}>
            {downloading ? "Generando PDF..." : "Descargar reserva"}
          </button>
          <Link className="btn" to="/compras/historial">
            Volver al historial
          </Link>
        </div>
      </div>

      <div className="card">
        {loading ? (
          <>
            <div className="skl title" />
            <div className="skl row" />
            <div className="skl row" />
          </>
        ) : err ? (
          <div className="error">{err}</div>
        ) : !det ? null : (
          <>
            <div className="h-meta">
              <span>
                <strong>Fecha:</strong> {dt(det.creadaEn)}
              </span>
              <span>
                <strong>Estado:</strong>{" "}
                <span className={`pill ${Number(det.idEstado) === 1 ? "pill--ok" : "pill--muted"}`}>
                  {estadoName(det.idEstado)}
                </span>
              </span>
              <span>
                <strong>Total:</strong> {money(det.total)}
              </span>
            </div>

            <ul className="rd-list">
              {det.items?.map((it) => (
                <li key={it.idItem} className="rd-item">
                  <div className="rd-main">
                    <div className="rd-code">
                      <span className="badge">{it.codigoVuelo}</span>
                      <span className="pill">{it.clase}</span>
                    </div>

                    <RouteLine item={it} />

                    <div className="rd-times">
                      <div>
                        <span className="label">Salida</span> {dt(it.fechaSalida)}
                      </div>
                      <div>
                        <span className="label">Llegada</span> {dt(it.fechaLlegada)}
                      </div>
                    </div>

                    {(it.escalaCiudad || it.escalaPais || it.escalaLlegada || it.escalaSalida) && (
                      <div className="rd-extra">
                        <div className="label" style={{ marginBottom: 4 }}>Escala</div>
                        <div className="text-muted">
                          {route(it.escalaCiudad, it.escalaPais)} — {dt(it.escalaLlegada)} → {dt(it.escalaSalida)}
                        </div>
                      </div>
                    )}

                    {it.regresoCodigo && (
                      <div className="rd-extra">
                        <div className="label" style={{ marginBottom: 4 }}>Regreso</div>
                        <div className="rd-code" style={{ marginBottom: 6 }}>
                          <span className="badge">{it.regresoCodigo}</span>
                        </div>
                        <div className="text-muted">
                          {route(it.regresoCiudadOrigen, it.regresoPaisOrigen)} → {route(it.regresoCiudadDestino, it.regresoPaisDestino)}
                        </div>
                        <div className="text-muted">
                          {dt(it.regresoFechaSalida)} → {dt(it.regresoFechaLlegada)}
                        </div>
                      </div>
                    )}
                  </div>

                  <div className="rd-money">
                    <div>
                      <span className="label">Cantidad</span> {it.cantidad}
                    </div>
                    <div>
                      <span className="label">P. unitario</span> {money(it.precioUnitario)}
                    </div>
                    <div>
                      <span className="label">Subtotal</span>{" "}
                      <strong>{money(it.subtotal)}</strong>
                    </div>
                  </div>
                </li>
              ))}
            </ul>

            <div className="sum-total" style={{ marginTop: 10 }}>
              Total: <strong>{money(det.total)}</strong>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
