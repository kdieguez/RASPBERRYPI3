import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
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
  return d
    ? d.toLocaleString("es-MX", { dateStyle: "medium", timeStyle: "short" })
    : "—";
};

const fmtMoney = (n) =>
  Number(n).toLocaleString("es-GT", {
    style: "currency",
    currency: "GTQ",
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

const minPrecio = (vuelo) => {
  if (!vuelo?.clases || vuelo.clases.length === 0) return null;
  let m = Infinity;
  for (const c of vuelo.clases) {
    const p = Number(c.precio);
    if (Number.isFinite(p)) m = Math.min(m, p);
  }
  return m === Infinity ? null : m;
};

const precioDeClase = (vuelo, idClase) => {
  if (!vuelo?.clases) return null;
  const c = vuelo.clases.find((x) => Number(x.idClase) === Number(idClase));
  return c ? Number(c.precio) : null;
};

export default function VuelosCatalogo() {
  const [vuelos, setVuelos] = useState([]);
  const [clases, setClases] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  const user = getUser();
  const isAdmin = !!user && Number(user.idRol) === 1;

  const [sp] = useSearchParams(); 
  const nav = useNavigate();

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
          clases: regreso.clases || [],
          escalas: regreso.escalas || [],
        };
        byId.set(ida.idVuelo, host);
      }
      hidden.add(regreso.idVuelo);
    }

    return Array.from(byId.values()).filter((v) => !hidden.has(v.idVuelo));
  };

  const claseName = (idClase) =>
    clases.find((c) => Number(c.idClase) === Number(idClase))?.nombre ||
    `Clase ${idClase}`;

  const activeFilters = useMemo(() => {
    const get = (k) => sp.get(k);
    const af = [];
    if (get("origen")) af.push({ k: "origen", v: get("origen") });
    if (get("destino")) af.push({ k: "destino", v: get("destino") });
    if (get("fsd") || get("fsh"))
      af.push({ k: "salida", v: `${get("fsd") || "—"} → ${get("fsh") || "—"}` });
    if (get("frd") || get("frh"))
      af.push({ k: "regreso", v: `${get("frd") || "—"} → ${get("frh") || "—"}` });
    if (get("clase")) {
      const nm = claseName(get("clase"));
      af.push({ k: "clase", v: nm });
    }
    if (get("pmin") || get("pmax"))
      af.push({ k: "precio", v: `${get("pmin") || "0"} - ${get("pmax") || "∞"}` });
    if (get("direct") === "1") af.push({ k: "directo", v: "Solo directos" });
    return af;
  }, [sp, clases]);

  const clearFilters = () => nav("/vuelos", { replace: true });

  const filtered = useMemo(() => {
    const spGet = (k) => (sp.get(k) || "").trim().toLowerCase();

    const f_origen = spGet("origen");
    const f_dest = spGet("destino");
    const fsd = sp.get("fsd");
    const fsh = sp.get("fsh");
    const frd = sp.get("frd");
    const frh = sp.get("frh");
    const pmin = Number(sp.get("pmin"));
    const pmax = Number(sp.get("pmax"));
    const directOnly = sp.get("direct") === "1";
    const claseSel = sp.get("clase"); 

    const inDate = (d, fromStr, toStr) => {
      const dt = toDate(d);
      const from = fromStr ? new Date(`${fromStr}T00:00:00`) : null;
      const to = toStr ? new Date(`${toStr}T23:59:59`) : null;
      if (!dt) return false;
      if (from && dt < from) return false;
      if (to && dt > to) return false;
      return true;
    };

    const str = (x) => (x || "").toLowerCase();

    return (vuelos || []).filter((v) => {
      const rutaPaises = [str(v.origenPais), str(v.destinoPais)].join(" ");
      const escalaPaises = Array.isArray(v.escalas)
        ? v.escalas.map((e) => str(e.pais)).join(" ")
        : "";

      if (f_origen) {
        const hit =
          str(v.origen).includes(f_origen) ||
          str(v.origenPais).includes(f_origen) ||
          escalaPaises.includes(f_origen);
        if (!hit) return false;
      }
      if (f_dest) {
        const hit =
          str(v.destino).includes(f_dest) ||
          str(v.destinoPais).includes(f_dest) ||
          escalaPaises.includes(f_dest);
        if (!hit) return false;
      }

      if (claseSel) {
        const tiene =
          Array.isArray(v.clases) &&
          v.clases.some((c) => Number(c.idClase) === Number(claseSel));
        if (!tiene) return false;
      }

      if ((fsd || fsh) && !inDate(v.fechaSalida, fsd, fsh)) return false;

      if ((frd || frh) && (!v.pareja || !inDate(v.pareja.fechaSalida, frd, frh)))
        return false;

      const precioBase = claseSel ? precioDeClase(v, claseSel) : minPrecio(v);
      if (Number.isFinite(pmin) && !Number.isNaN(pmin) && pmin > 0 && precioBase !== null && precioBase < pmin)
        return false;
      if (Number.isFinite(pmax) && !Number.isNaN(pmax) && pmax > 0 && precioBase !== null && precioBase > pmax)
        return false;

      if (directOnly && Array.isArray(v.escalas) && v.escalas.length > 0) return false;

      return true;
    });
  }, [vuelos, sp]);

  return (
    <div className="container pag-vuelos-list">
      <div className="vlist__head">
        <h1>
          Vuelos {isAdmin && <span className="pill" style={{ marginLeft: 8 }}>Admin: todos</span>}
        </h1>
      </div>

      {activeFilters.length > 0 && (
        <div className="filter-chips" style={{ marginBottom: 10 }}>
          {activeFilters.map((f, i) => (
            <span key={i} className="pill">
              {f.k}: <strong>{f.v}</strong>
            </span>
          ))}
          <button className="btn btn-small" onClick={clearFilters} style={{ marginLeft: 8 }}>
            Quitar filtros
          </button>
        </div>
      )}

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
                      {isAdmin &&
                        (v.activo === false ? (
                          <span className="pill">Inactivo</span>
                        ) : (
                          <span className="pill pill--ok">Activo</span>
                        ))}
                    </div>

                    <div className="vitem__city">
                      <strong>
                        {v.origen && v.destino
                          ? `${v.origen} → ${v.destino}`
                          : `Ruta #${v.idRuta}`}
                      </strong>
                      {(v.origenPais || v.destinoPais) && (
                        <div className="label" style={{ marginTop: 2 }}>
                          {v.origenPais || "—"} → {v.destinoPais || "—"}
                        </div>
                      )}
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
                          <strong>
                            <Link to={`/vuelos/${v.pareja.idVuelo}`}>{v.pareja.codigo}</Link>
                          </strong>{" "}
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

                <div className="vitem__actions">
                  <Link className="btn" to={`/vuelos/${v.idVuelo}`}>
                    Ver detalles
                  </Link>
                  {isAdmin && (
                    <Link
                      className="btn btn-secondary"
                      to={`/admin/vuelos/${v.idVuelo}`}
                      style={{ marginLeft: 8 }}
                    >
                      Editar
                    </Link>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
