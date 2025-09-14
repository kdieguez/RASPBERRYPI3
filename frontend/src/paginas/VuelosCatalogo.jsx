import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { vuelosApi, clasesApi } from "../api/adminCatalogos";
import { getUser } from "../lib/auth";
import "../styles/vuelosCatalogo.css";

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
      const [, Y, Mo, D, h, mi, se] = m;
      d = new Date(+Y, +Mo - 1, +D, +h, +mi, se ? +se : 0);
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

const fmtDt = (val) => {
  const d = toDate(val);
  return d ? d.toLocaleString("es-MX", { dateStyle: "medium", timeStyle: "short" }) : "—";
};

const fmtMoney = (n) =>
  Number(n).toLocaleString("es-MX", {
    style: "currency",
    currency: "MXN",
    maximumFractionDigits: 2,
  });

const EstadoPill = ({ estado }) => {
  const s = (estado || "").toLowerCase();
  const isCancel = s.includes("cancel"); 
  return (
    <span className={`pill ${isCancel ? "pill--danger" : "pill--ok"}`}>
      {estado || "Programado"}
    </span>
  );
};

export default function VuelosCatalogo() {
  const [vuelos, setVuelos] = useState([]);
  const [clases, setClases] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  const [q, setQ] = useState("");
  const [desde, setDesde] = useState(""); 
  const [hasta, setHasta] = useState("");

  const user = getUser();
  const isAdmin = !!user && Number(user.idRol) === 1;

  useEffect(() => {
    (async () => {
      try {
        setLoading(true);
        setErr("");

        const [vuelosRes, clasesRes] = await Promise.all([
          (async () => {
            try {
              return (await (isAdmin ? vuelosApi.listAdmin() : vuelosApi.listPublic())).data;
            } catch {
              return (await vuelosApi.listPublic()).data;
            }
          })(),
          (await clasesApi.list()).data,
        ]);

        const folded = foldPairsFromList(Array.isArray(vuelosRes) ? vuelosRes : []);
        setVuelos(folded);
        setClases(clasesRes || []);
      } catch (e) {
        setErr(e?.response?.data?.error || "No se pudieron cargar los vuelos");
      } finally {
        setLoading(false);
      }
    })();
  }, [isAdmin]);

  const foldPairsFromList = (list) => {
    const byId = new Map(list.map((v) => [v.idVuelo, { ...v }]));
    const hidden = new Set();

    for (const v of list) {
      if (!v.idVueloPareja || hidden.has(v.idVuelo) || hidden.has(v.idVueloPareja)) continue;
      const pair = byId.get(v.idVueloPareja);
      if (!pair) continue;

      const fs1 = toDate(v.fechaSalida);
      const fs2 = toDate(pair.fechaSalida);
      const ida = fs1 && fs2 && fs1 <= fs2 ? v : pair;
      const regreso = ida === v ? pair : v;

      const host = byId.get(ida.idVuelo);
      if (host) {
        host.pareja = {
          idVuelo: regreso.idVuelo,
          codigo: regreso.codigo,
          origen: regreso.origen,
          destino: regreso.destino,
          fechaSalida: regreso.fechaSalida,
          fechaLlegada: regreso.fechaLlegada,
        };
        byId.set(ida.idVuelo, host);
      }
      hidden.add(regreso.idVuelo);
    }

    return Array.from(byId.values()).filter((v) => !hidden.has(v.idVuelo));
  };

  const claseName = (idClase) =>
    clases.find((c) => Number(c.idClase) === Number(idClase))?.nombre || `Clase ${idClase}`;

  const filtered = useMemo(() => {
    const qx = q.trim().toLowerCase();
    const dFrom = desde ? new Date(`${desde}T00:00:00`) : null;
    const dTo = hasta ? new Date(`${hasta}T23:59:59`) : null;

    return (vuelos || []).filter((v) => {
      const txt = [v.codigo, v.origen, v.destino, v.estado, v.idRuta && `ruta ${v.idRuta}`]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();
      if (qx && !txt.includes(qx)) return false;

      const fs = toDate(v.fechaSalida);
      if (dFrom && fs && fs < dFrom) return false;
      if (dTo && fs && fs > dTo) return false;

      return true;
    });
  }, [vuelos, q, desde, hasta]);

  return (
    <div className="container pag-vuelos-list">
      <div className="vlist__head">
        <h1>
          Vuelos {isAdmin && <span className="pill" style={{ marginLeft: 8 }}>Admin: todos</span>}
        </h1>
        <div className="vlist__filters">
          <input
            className="input"
            placeholder="Buscar por código, ruta…"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
          <input
            className="input"
            type="date"
            value={desde}
            onChange={(e) => setDesde(e.target.value)}
            title="Desde"
          />
          <input
            className="input"
            type="date"
            value={hasta}
            onChange={(e) => setHasta(e.target.value)}
            title="Hasta"
          />
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
        ) : filtered.length === 0 ? (
          <div className="empty">No hay vuelos que coincidan con el filtro.</div>
        ) : (
          <ul className="vlist">
            {filtered.map((v) => (
              <li key={v.idVuelo} className="vitem">
                <div className="vitem__main">
                  <div className="vitem__route">
                    <div className="vitem__code">
                      <span className="badge">{v.codigo}</span>

                      <EstadoPill estado={v.estado} />

                      {isAdmin && (
                        v.activo === false
                          ? <span className="pill">Inactivo</span>
                          : <span className="pill pill--ok">Activo</span>
                      )}
                    </div>

                    <div className="vitem__city">
                      <strong>
                        {v.origen && v.destino
                          ? `${v.origen} → ${v.destino}`
                          : `Ruta #${v.idRuta}`}
                      </strong>
                    </div>

                    <div className="vitem__times">
                      <div>
                        <span className="label">Salida</span>
                        <span>{fmtDt(v.fechaSalida)}</span>
                      </div>
                      <div>
                        <span className="label">Llegada</span>
                        <span>{fmtDt(v.fechaLlegada)}</span>
                      </div>
                    </div>
                  </div>

                  {Array.isArray(v.escalas) && v.escalas.length > 0 && (
                    <div className="vitem__escalas">
                      {v.escalas.map((e, i) => (
                        <div key={i} className="esc">
                          <span className="label">Escala</span>
                          <span>
                            {e.ciudad} ({e.pais}) — {fmtDt(e.llegada)} → {fmtDt(e.salida)}
                          </span>
                        </div>
                      ))}
                    </div>
                  )}

                  {v.pareja && (
                    <div className="vitem__escalas">
                      <div className="esc">
                        <span className="label">Regreso</span>
                        <span>
                          <strong>{v.pareja.codigo}</strong>{" "}
                          {v.pareja.origen && v.pareja.destino
                            ? `${v.pareja.origen} → ${v.pareja.destino}`
                            : ""}
                          {" — "}
                          {fmtDt(v.pareja.fechaSalida)} → {fmtDt(v.pareja.fechaLlegada)}
                        </span>
                      </div>
                    </div>
                  )}
                </div>

                <div className="vitem__classes">
                  {v.clases.map((c, i) => (
                    <div key={i} className="cl">
                      <div className="cl__name">{claseName(c.idClase)}</div>
                      <div className="cl__meta">
                        <span className="label">Cupo</span>
                        <span>{c.cupoTotal}</span>
                        <span className="label">Precio</span>
                        <span>{fmtMoney(c.precio)}</span>
                      </div>
                    </div>
                  ))}
                </div>

                {isAdmin && (
                  <div className="vitem__actions">
                    <Link className="btn btn-secondary" to={`/admin/vuelos/${v.idVuelo}`}>
                      Editar
                    </Link>
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
