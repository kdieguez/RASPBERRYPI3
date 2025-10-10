import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { comprasApi } from "../api/compras";
import { getUser } from "../lib/auth";
import "../styles/historial.css";

const money = (n) => Number(n || 0).toLocaleString("es-GT", { style: "currency", currency: "GTQ" });
const dt = (s) => s ? new Date(s).toLocaleString("es-GT", { dateStyle: "medium", timeStyle: "short" }) : "—";
const estadoName = (id) => ({ 1:"Pagada", 2:"Cancelada"}[Number(id)] || `Estado ${id}`);

export default function HistorialCompras() {
  const nav = useNavigate();
  const u = getUser();

  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  useEffect(() => {
    if (!u) { nav("/login"); return; }
    (async () => {
      try {
        setLoading(true);
        setErr("");
        const { data } = await comprasApi.listReservas();
        setList(Array.isArray(data) ? data : []);
      } catch (e) {
        setErr(e?.response?.data?.error || e.message || "No se pudo cargar el historial");
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  if (!u) return null;

  return (
    <div className="container historial">
      <div className="h-head">
        <h1>Mis compras</h1>
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
        ) : list.length === 0 ? (
          <div className="empty">Aún no tienes compras.</div>
        ) : (
          <table className="h-table">
            <thead>
              <tr>
                <th>Código</th>
                <th>ID</th>
                <th>Fecha</th>
                <th>Estado</th>
                <th>Total</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {list.map((r) => (
                <tr key={r.idReserva}>
                  <td>
                    {r.codigo ? <span className="pill">{r.codigo}</span> : "—"}
                  </td>
                  <td>#{r.idReserva}</td>
                  <td>{dt(r.creadaEn)}</td>
                  <td>
                    <span className={`pill ${Number(r.idEstado) === 1 ? "pill--ok" : "pill--muted"}`}>
                      {estadoName(r.idEstado)}
                    </span>
                  </td>
                  <td>{money(r.total)}</td>
                  <td>
                    <Link className="btn" to={`/compras/reservas/${r.idReserva}`}>Ver detalle</Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
