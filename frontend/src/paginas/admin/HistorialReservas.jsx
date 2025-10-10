import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { comprasApi } from "../../api/compras";
import { getUser } from "../../lib/auth";
import "../../styles/historial.css";

const money = (n) =>
  Number(n || 0).toLocaleString("es-GT", { style: "currency", currency: "GTQ" });
const dt = (s) =>
  s ? new Date(s).toLocaleString("es-GT", { dateStyle: "medium", timeStyle: "short" }) : "—";
const estadoName = (id) =>
  ({ 1: "Pagada", 2: "Cancelada" }[Number(id)] || `Estado ${id}`);

export default function HistorialReservas() {
  const nav = useNavigate();
  const u = getUser();
  const isAdmin = !!u && Number(u.idRol) === 1;

  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  
  const [q, setQ] = useState("");
  const [codigo, setCodigo] = useState("");
  const [vuelo, setVuelo] = useState("");
  const [desde, setDesde] = useState("");
  const [hasta, setHasta] = useState("");
  const [estado, setEstado] = useState("");

  const fetch = async () => {
    try {
      setLoading(true);
      setErr("");
      const params = {};
      if (q) params.q = q;               
      if (codigo) params.codigo = codigo;
      if (vuelo) params.vuelo = vuelo;
      if (desde) params.desde = desde;
      if (hasta) params.hasta = hasta;
      if (estado) params.estado = estado;
      const { data } = await comprasApi.adminListReservas(params);
      setList(Array.isArray(data) ? data : []);
    } catch (e) {
      setErr(e?.response?.data?.error || e.message || "No se pudo cargar");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!u) { nav("/login"); return; }
    if (!isAdmin) { nav("/"); return; }
    fetch();
  }, []);

  if (!isAdmin) return null;

  return (
    <div className="container historial">
      <div className="h-head">
        <h1>Reservas (admin)</h1>
      </div>

      <div className="card" style={{ marginBottom: 12 }}>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(6,1fr)", gap: 8 }}>
          <input
            className="input"
            placeholder="Buscar (email, nombre, apellido o ID de usuario)"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
          <input
            className="input"
            placeholder="Código reserva"
            value={codigo}
            onChange={(e) => setCodigo(e.target.value)}
          />
          <input
            className="input"
            placeholder="Vuelo (ID o código)"
            value={vuelo}
            onChange={(e) => setVuelo(e.target.value)}
          />
          <input className="input" type="date" value={desde} onChange={(e) => setDesde(e.target.value)} />
          <input className="input" type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} />
          <select className="input" value={estado} onChange={(e) => setEstado(e.target.value)}>
            <option value="">Todos</option>
            <option value="1">Pagada</option>
            <option value="2">Cancelada</option>
          </select>
        </div>
        <div style={{ marginTop: 8, display: "flex", gap: 8 }}>
          <button className="btn btn-secondary" onClick={fetch}>Aplicar filtros</button>
          <button
            className="btn"
            onClick={() => {
              setQ(""); setCodigo(""); setVuelo("");
              setDesde(""); setHasta(""); setEstado("");
            }}
          >
            Limpiar
          </button>
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
        ) : list.length === 0 ? (
          <div className="empty">No hay resultados.</div>
        ) : (
          <table className="h-table">
            <thead>
              <tr>
                <th>Código</th>
                <th>ID</th>
                <th>Usuario</th>
                <th>Fecha</th>
                <th>Estado</th>
                <th>Total</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {list.map((r) => (
                <tr key={r.idReserva}>
                  <td>{r.codigo ? <span className="pill">{r.codigo}</span> : "—"}</td>
                  <td>#{r.idReserva}</td>
                  <td>#{r.idUsuario}</td>
                  <td>{dt(r.creadaEn)}</td>
                  <td>
                    <span className={`pill ${Number(r.idEstado) === 1 ? "pill--ok" : "pill--muted"}`}>
                      {estadoName(r.idEstado)}
                    </span>
                  </td>
                  <td>{money(r.total)}</td>
                  <td>
                    <Link className="btn" to={`/admin/reservas/${r.idReserva}`}>Ver</Link>
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
