import { useEffect, useState } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { vuelosApi, clasesApi } from "../api/adminCatalogos";
import { getUser } from "../lib/auth";
import { comprasApi } from "../api/compras";
import "../styles/vueloDetalle.css";

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

const fmtDateTime = (val) => {
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

export default function VueloDetalle() {
  const { id } = useParams();
  const nav = useNavigate();
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");
  const [v, setV] = useState(null);
  const [clasesCat, setClasesCat] = useState([]);
  const [regreso, setRegreso] = useState(null);

  const [idClaseSel, setIdClaseSel] = useState(null);
  const [cant, setCant] = useState(1);

  useEffect(() => {
    (async () => {
      try {
        setLoading(true);
        setErr("");

        const [{ data: vuelo }, { data: catClases }] = await Promise.all([
          vuelosApi.getPublic(id),
          clasesApi.list(),
        ]);

        setV(vuelo);
        setClasesCat(Array.isArray(catClases) ? catClases : []);
        setIdClaseSel(vuelo?.clases?.[0]?.idClase ?? null);

        if (vuelo?.idVueloPareja && Number(vuelo.idVueloPareja) !== Number(id)) {
          try {
            const { data: vp } = await vuelosApi.getPublic(vuelo.idVueloPareja);
            setRegreso(vp);
          } catch {
            setRegreso({ idVuelo: vuelo.idVueloPareja });
          }
        } else {
          setRegreso(null);
        }
      } catch (e) {
        setErr(e?.response?.data?.error || "No se pudo cargar el vuelo");
      } finally {
        setLoading(false);
      }
    })();
  }, [id]);

  const claseName = (idClase) =>
    clasesCat.find((c) => Number(c.idClase) === Number(idClase))?.nombre ||
    `Clase #${idClase}`;

  if (loading)
    return (
      <div className="container pag-vuelo">
        <div className="card">
          <div className="skl title" />
          <div className="skl row" />
          <div className="skl row" />
        </div>
      </div>
    );

  if (err)
    return (
      <div className="container pag-vuelo">
        <div className="card">
          <div className="error">{err}</div>
        </div>
      </div>
    );

  if (!v) return null;

  const esCancelado = (v.estado || "").toLowerCase().includes("cancel");
  const salida = fmtDateTime(v.fechaSalida);
  const llegada = fmtDateTime(v.fechaLlegada);

  const comprar = async () => {
    const u = getUser();
    if (!u) { alert("Primero inicia sesión."); nav("/login"); return; }
    if (esCancelado || v.activo === false) {
      alert("Este vuelo no está disponible para compra.");
      return;
    }
    const idClase = idClaseSel ?? v?.clases?.[0]?.idClase;
    if (!idClase) { alert("Selecciona una clase."); return; }
    try {
      await comprasApi.addItem({
        idVuelo: Number(v.idVuelo),
        idClase: Number(idClase),
        cantidad: Number(cant || 1),
      });
      nav(`/compras/carrito`);
    } catch (e) {
      alert(e?.response?.data?.error || e?.message || "No se pudo agregar al carrito");
    }
  };

  return (
    <div className="container pag-vuelo">
      <div className="card vd">
        <header className="vd__header">
          <div className="vd__avatar">
            <svg width="36" height="36" viewBox="0 0 24 24" aria-hidden="true">
              <path
                fill="currentColor"
                d="M2 13c7-1 10-4 12-9l2 1-2 6 7 1 1 2-7 1 2 6-2 1c-2-5-5-8-12-9v-1Z"
              />
            </svg>
          </div>
          <div className="vd__header__text">
            <h2>
              Vuelo <span className="vd__code">{v.codigo || `#${id}`}</span>
            </h2>
            <p className="subtitle">
              <Link to="/vuelos">← Volver a la lista</Link>
            </p>
            <div className="vd__chips">
              <span className={`pill ${esCancelado ? "pill--bad" : "pill--ok"}`}>
                {esCancelado ? "Cancelado" : v.estado || "Programado"}
              </span>
              {v.activo === false ? (
                <span className="pill">Inactivo</span>
              ) : (
                <span className="pill pill--ok">Activo</span>
              )}
            </div>
          </div>
        </header>

        <div className="vd__grid">
          <section>
            <h3 className="block-title">Ruta</h3>
            <div className="vd__route">
              <div>
                <div className="label">Origen</div>
                <div className="vd__big">{v.origen || "—"}</div>
              </div>
              <div className="vd__arrow">→</div>
              <div>
                <div className="label">Destino</div>
                <div className="vd__big">{v.destino || "—"}</div>
              </div>
            </div>

            <h3 className="block-title" style={{ marginTop: 16 }}>
              Fechas
            </h3>
            <div className="vd__timeline">
              <div className="vd__titem">
                <div className="vd__dot" />
                <div>
                  <div className="label">Salida</div>
                  <div className="vd__tval">{salida}</div>
                </div>
              </div>
              <div className="vd__titem">
                <div className="vd__dot" />
                <div>
                  <div className="label">Llegada</div>
                  <div className="vd__tval">{llegada}</div>
                </div>
              </div>
            </div>
          </section>

          <section>
            <h3 className="block-title">Clases y precios</h3>
            {!v.clases || v.clases.length === 0 ? (
              <p className="hint">No hay clases configuradas.</p>
            ) : (
              <div className="vd__classgrid">
                {v.clases.map((c, i) => (
                  <div className="vd__classcard" key={i}>
                    <div className="vd__classname">{claseName(c.idClase)}</div>
                    <div className="vd__classmeta">
                      <span className="label">Cupo {c.cupoTotal}</span>
                      <span className="label">Precio: {fmtMoney(c.precio)}</span>
                    </div>
                  </div>
                ))}
              </div>
            )}

            <h3 className="block-title" style={{ marginTop: 16 }}>
              Escala
            </h3>
            {!v.escalas || v.escalas.length === 0 ? (
              <p>Vuelo directo.</p>
            ) : (
              v.escalas.map((e, idx) => (
                <div className="vd__escala" key={idx}>
                  <div className="vd__esc__head">
                    <strong>{e.ciudad}</strong> <span>({e.pais})</span>
                  </div>
                  <div className="vd__esc__times">
                    <div>
                      <span className="label">Llega</span>{" "}
                      {fmtDateTime(e.llegada)}
                    </div>
                    <div>
                      <span className="label">Sale</span>{" "}
                      {fmtDateTime(e.salida)}
                    </div>
                  </div>
                </div>
              ))
            )}
          </section>
        </div>

        <div className="actions" style={{ marginTop: 12, display: "flex", gap: 8, alignItems: "center" }}>
          <Link to="/vuelos" className="btn">
            Cerrar
          </Link>

          {v?.clases?.length > 0 && (
            <>
              <select
                className="input"
                value={idClaseSel ?? ""}
                onChange={(e) => setIdClaseSel(Number(e.target.value))}
              >
                {v.clases.map((c) => (
                  <option key={c.idClase} value={c.idClase}>
                    {claseName(c.idClase)}
                  </option>
                ))}
              </select>

              <input
                className="input"
                type="number"
                min={1}
                max={9}
                value={cant}
                onChange={(e) => setCant(Math.max(1, Math.min(9, Number(e.target.value) || 1)))}
                style={{ width: 64 }}
              />

              <button
                className="btn btn-secondary"
                onClick={comprar}
                disabled={esCancelado || v.activo === false || !v?.clases?.length}
              >
                Comprar
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
