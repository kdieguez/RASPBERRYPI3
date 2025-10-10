import { useEffect, useState, useMemo } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { comprasApi } from "../../api/compras";
import "../../styles/historial.css";

const money = (n) =>
  Number(n || 0).toLocaleString("es-GT", { style: "currency", currency: "GTQ" });

const dt = (s) =>
  s ? new Date(s).toLocaleString("es-GT", { dateStyle: "medium", timeStyle: "short" }) : "—";

const estadoName = (id) =>
  ({ 1: "PAGADA", 2: "CONFIRMADA", 3: "REEMBOLSADA", 4: "EXPIRADA" }[Number(id)] || `Estado ${id}`);

const place = (ciudad, pais) => {
  const c = (ciudad || "").trim();
  const p = (pais || "").trim();
  if (!c && !p) return "—";
  if (c && p) return `${c}, ${p}`;
  return c || p;
};

export default function AdminReservaDetalle() {
  const { id } = useParams();
  const nav = useNavigate();

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  const fetch = async () => {
    try {
      setLoading(true); setErr("");
      const { data } = await comprasApi.adminGetReserva(id);
      setData(data);
    } catch (e) {
      setErr(e?.response?.data?.error || e.message || "No se pudo cargar");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { if (!id) { nav("/admin/reservas"); return; } fetch(); }, [id]);

  const code = data?.codigo || `#${id}`;
  const buyer = (data?.compradorNombre && data.compradorNombre.trim())
    ? data.compradorNombre
    : (data?.compradorEmail || "");

  return (
    <div className="container historial">
      <div className="h-head" style={{ alignItems: "center" }}>
        <div>
          <h1 style={{ marginBottom: 4 }}>
            Reserva {code}
            {buyer ? (
              <span style={{ marginLeft: 12, fontWeight: 500, fontSize: 18, opacity: 0.8 }}>
                — {buyer}
              </span>
            ) : null}
          </h1>
        </div>
        <div style={{ marginLeft: "auto", display: "flex", gap: 8 }}>
          <Link className="btn btn-secondary" to="/admin/reservas">Volver</Link>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 12 }}>
        {loading ? (
          <>
            <div className="skl title" />
            <div className="skl row" />
            <div className="skl row" />
          </>
        ) : err ? (
          <div className="error">{err}</div>
        ) : !data ? (
          <div className="empty">No se encontró la reserva.</div>
        ) : (
          <>
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(5, minmax(0,1fr))",
                gap: 12,
                marginBottom: 8,
              }}
            >
              <Info label="ID reserva" value={`#${data.idReserva}`} />
              <Info label="Usuario" value={`#${data.idUsuario}`} />
              <Info
                label="Estado"
                value={
                  <span className={`pill ${Number(data.idEstado) === 1 ? "pill--ok" : "pill--muted"}`}>
                    {estadoName(data.idEstado)}
                  </span>
                }
              />
              <Info label="Creada en" value={dt(data.creadaEn)} />
              <Info label="Total" value={<strong>{money(data.total)}</strong>} />
            </div>

            <div className="table-wrap">
              <table className="h-table">
                <thead>
                  <tr>
                    <th>Vuelo</th>
                    <th>Clase</th>
                    <th>Salida</th>
                    <th>Llegada</th>
                    <th>Origen</th>
                    <th>Destino</th>
                    <th>Cant.</th>
                    <th>P. Unit.</th>
                    <th>Subtotal</th>
                  </tr>
                </thead>
                <tbody>
                  {(data.items || []).map((it) => (
                    <tr key={it.idItem}>
                      <td>
                        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                          <span className="pill">{it.codigoVuelo || `#${it.idVuelo}`}</span>
                          {it.escalaCiudad || it.escalaPais ? (
                            <small className="pill pill--muted">Escala: {place(it.escalaCiudad, it.escalaPais)}</small>
                          ) : null}
                          {it.regresoCodigo ? (
                            <small className="pill pill--muted">Regreso: {it.regresoCodigo}</small>
                          ) : null}
                        </div>
                      </td>
                      <td>{it.clase || `Clase ${it.idClase}`}</td>
                      <td>{dt(it.fechaSalida)}</td>
                      <td>{dt(it.fechaLlegada)}</td>
                      <td>{place(it.ciudadOrigen, it.paisOrigen)}</td>
                      <td>{place(it.ciudadDestino, it.paisDestino)}</td>
                      <td>{it.cantidad}</td>
                      <td>{money(it.precioUnitario)}</td>
                      <td><strong>{money(it.subtotal)}</strong></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div style={{ display: "flex", justifyContent: "flex-end", marginTop: 12 }}>
              <div style={{ textAlign: "right" }}>
                <div style={{ fontSize: 12, opacity: 0.7 }}>Total</div>
                <div style={{ fontSize: 20, fontWeight: 700 }}>{money(data.total)}</div>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function Info({ label, value }) {
  return (
    <div className="info">
      <div style={{ fontSize: 12, opacity: 0.7, marginBottom: 4 }}>{label}</div>
      <div>{value ?? "—"}</div>
    </div>
  );
}
