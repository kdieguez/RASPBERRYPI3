import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { comprasApi } from "../api/compras";
import { getUser } from "../lib/auth";
import RouteLine from "../components/RouteLine";
import "../styles/historial.css";

const money = (n) => Number(n || 0).toLocaleString("es-GT", { style: "currency", currency: "GTQ" });
const dt = (s) => s ? new Date(s).toLocaleString("es-GT", { dateStyle: "medium", timeStyle: "short" }) : "—";
const estadoName = (id) => ({ 1:"Confirmada", 2:"Cancelada", 3:"Reembolsada", 4:"Expirada" }[Number(id)] || `Estado ${id}`);

export default function ReservaDetalle() {
  const { id } = useParams();
  const nav = useNavigate();
  const u = getUser();

  const [det, setDet] = useState(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  useEffect(() => {
    if (!u) { nav("/login"); return; }
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

  if (!u) return null;

  return (
    <div className="container historial">
      <div className="h-head">
        <h1>Reserva #{id}</h1>
        <Link className="btn" to="/compras/historial">Volver al historial</Link>
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
              <span><strong>Fecha:</strong> {dt(det.creadaEn)}</span>
              <span><strong>Estado:</strong> {estadoName(det.idEstado)}</span>
              <span><strong>Total:</strong> {money(det.total)}</span>
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
                      <div><span className="label">Salida</span> {dt(it.fechaSalida)}</div>
                      <div><span className="label">Llegada</span> {dt(it.fechaLlegada)}</div>
                    </div>
                  </div>
                  <div className="rd-money">
                    <div><span className="label">Cantidad</span> {it.cantidad}</div>
                    <div><span className="label">P. unitario</span> {money(it.precioUnitario)}</div>
                    <div><span className="label">Subtotal</span> <strong>{money(it.subtotal)}</strong></div>
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
