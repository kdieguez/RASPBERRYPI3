import { useEffect, useState } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { vuelosApi, clasesApi } from "../api/adminCatalogos";
import { isLoggedIn, getUser } from "../lib/auth";
import { comprasApi } from "../api/compras";
import "../styles/vueloDetalle.css";
import { comentariosApi } from "../api/comentarios";
import { ratingsApi } from "../api/ratings";

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

function Star({ filled, onClick, onMouseEnter, onMouseLeave, disabled }) {
  return (
    <button
      type="button"
      className={`star ${filled ? "star--on" : ""}`}
      onClick={disabled ? undefined : onClick}
      onMouseEnter={disabled ? undefined : onMouseEnter}
      onMouseLeave={disabled ? undefined : onMouseLeave}
      aria-label={filled ? "Estrella seleccionada" : "Estrella no seleccionada"}
      disabled={disabled}
    >
      ★
    </button>
  );
}

function StarsBar({ value = 0, size = "md" }) {
  const v = Math.max(0, Math.min(5, Number(value) || 0));
  return (
    <div className={`starsbar starsbar--${size}`} aria-label={`Rating ${v} de 5`}>
      {[1, 2, 3, 4, 5].map((i) => (
        <span key={i} className={`star ${i <= v ? "star--on" : ""}`}>★</span>
      ))}
    </div>
  );
}

function StarsMini({ value = 0, title = "" }) {
  const v = Math.max(0, Math.min(5, Number(value) || 0));
  return (
    <span className="starsmini" title={title} aria-label={`Rating ${v} de 5`}>
      {[1,2,3,4,5].map(i => (
        <span key={i} className={`star ${i <= v ? "star--on" : ""}`}>★</span>
      ))}
    </span>
  );
}

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

  const [cmtLoading, setCmtLoading] = useState(true);
  const [cmtErr, setCmtErr] = useState("");
  const [comentarios, setComentarios] = useState([]);
  const [text, setText] = useState("");
  const [replyTo, setReplyTo] = useState(null);

  const [rLoading, setRLoading] = useState(true);
  const [rErr, setRErr] = useState("");
  const [avg, setAvg] = useState(0);
  const [total, setTotal] = useState(0);
  const [myRating, setMyRating] = useState(null);
  const [hover, setHover] = useState(0);
  const [sel, setSel] = useState(0);
  const [publishing, setPublishing] = useState(false);

  const [logged, setLogged] = useState(isLoggedIn());
  useEffect(() => {
    const onChange = () => setLogged(isLoggedIn());
    window.addEventListener("auth:changed", onChange);
    return () => window.removeEventListener("auth:changed", onChange);
  }, []);

  const fetchVueloSmart = async (vueloId) => {
    const u = getUser();
    const isAdmin = !!u && Number(u.idRol) === 1;
    try {
      return isAdmin ? await vuelosApi.getAdmin(vueloId) : await vuelosApi.getPublic(vueloId);
    } catch (e) {
      if (isAdmin) return await vuelosApi.getPublic(vueloId);
      throw e;
    }
  };

  const loadRating = async (vueloId) => {
    try {
      setRLoading(true);
      setRErr("");
      const { data } = await ratingsApi.resumen(vueloId);
      setAvg(Number(data?.promedio || 0));
      setTotal(Number(data?.total || 0));
      const mine = data?.miRating ?? null;
      setMyRating(mine);
      if (mine != null) setSel(Number(mine) || 0);
    } catch (e) {
      setRErr(e?.response?.data?.error || "No se pudo cargar el rating");
      setAvg(0);
      setTotal(0);
      setMyRating(null);
    } finally {
      setRLoading(false);
    }
  };

  useEffect(() => {
    (async () => {
      try {
        setLoading(true);
        setErr("");
        const [{ data: vuelo }, { data: catClases }] = await Promise.all([
          fetchVueloSmart(id),
          clasesApi.list(),
        ]);

        setV(vuelo);
        setClasesCat(Array.isArray(catClases) ? catClases : []);
        setIdClaseSel(vuelo?.clases?.[0]?.idClase ?? null);

        if (vuelo?.idVueloPareja && Number(vuelo.idVueloPareja) !== Number(id)) {
          try {
            const { data: vp } = await fetchVueloSmart(vuelo.idVueloPareja);
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

  useEffect(() => {
    if (!id) return;
    reloadComentarios(id);
    loadRating(id);
  }, [id]);

  const reloadComentarios = async (vueloId) => {
    try {
      setCmtLoading(true);
      setCmtErr("");
      const { data } = await comentariosApi.listPublic(vueloId);
      setComentarios(Array.isArray(data) ? data : []);
    } catch (e) {
      setCmtErr(e?.response?.data?.error || "No se pudieron cargar los comentarios");
    } finally {
      setCmtLoading(false);
    }
  };

  const publicar = async () => {
    const u = getUser();
    if (!u) { nav("/login"); return; }
    if (!v?.idVuelo) return;

    try {
      setPublishing(true);

      if (sel > 0 && myRating == null) {
        try {
          await ratingsApi.create(v.idVuelo, { calificacion: sel });
          await loadRating(v.idVuelo); 
        } catch (e) {
          if (e?.response?.status === 409) {
            alert("Ya calificaste este vuelo");
          } else {
            throw e;
          }
        }
      }

      const body = (text || "").trim();
      if (body) {
        await comentariosApi.create(v.idVuelo, { comentario: body, idPadre: replyTo });
        setText("");
        setReplyTo(null);
        await reloadComentarios(v.idVuelo);
      }
    } catch (e) {
      alert(e?.response?.data?.error || "No se pudo guardar tu calificación/comentario");
    } finally {
      setPublishing(false);
    }
  };

const CommentItem = ({ item, depth = 0 }) => (
  <div className="cmt__item" style={{ marginLeft: depth * 16 }}>
    <div className="cmt__meta" style={{ gap: 8 }}>
      <strong>{item.autor || `Usuario #${item.idUsuario}`}</strong>
      <span className="cmt__date">{fmtDateTime(item.creadaEn)}</span>
      {depth === 0 && item.ratingAutor != null && (
        <span style={{ marginLeft: 6 }}>
          <StarsBar value={item.ratingAutor} size="md" />
        </span>
      )}
    </div>
    <div className="cmt__body">{item.comentario}</div>
    {logged && (
      <button
        className="link"
        onClick={() => setReplyTo(replyTo === item.idComentario ? null : item.idComentario)}
      >
        {replyTo === item.idComentario ? "Cancelar respuesta" : "Responder"}
      </button>
    )}
    {Array.isArray(item.respuestas) &&
      item.respuestas.map((h) => (
        <CommentItem key={h.idComentario} item={h} depth={depth + 1} />
      ))}
  </div>
);


  const claseName = (idClase) =>
    clasesCat.find((c) => Number(c.idClase) === Number(idClase))?.nombre ||
    `Clase #${idClase}`;

  const findCommentById = (cid, list) => {
    if (!cid || !Array.isArray(list)) return null;
    for (const c of list) {
      if (Number(c.idComentario) === Number(cid)) return c;
      const hit = findCommentById(cid, c.respuestas || []);
      if (hit) return hit;
    }
    return null;
  };
  const replyMeta = replyTo ? findCommentById(replyTo, comentarios) : null;

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
    if (!u) { nav("/login"); return; }
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

            <h3 className="block-title" style={{ marginTop: 16 }}>Fechas</h3>
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

            {regreso && regreso.idVuelo && (
              <section style={{ marginTop: 16 }}>
                <h3 className="block-title">Regreso</h3>
                <div className="vd__escala">
                  <div className="vd__esc__head">
                    Vuelo regreso{" "}
                    <Link to={`/vuelos/${regreso.idVuelo}`} className="link">
                      #{regreso.codigo || regreso.idVuelo}
                    </Link>
                  </div>
                  <div className="vd__esc__times">
                    <div>
                      <span className="label">Sale</span>{" "}
                      {fmtDateTime(regreso.fechaSalida)}
                    </div>
                    <div>
                      <span className="label">Llega</span>{" "}
                      {fmtDateTime(regreso.fechaLlegada)}
                    </div>
                  </div>
                  <div style={{ marginTop: 6, display: "flex", gap: 12, flexWrap: "wrap" }}>
                    <span className="label">Origen:</span> <strong>{regreso.origen || "—"}</strong>
                    <span className="label">Destino:</span> <strong>{regreso.destino || "—"}</strong>
                    {Array.isArray(regreso.clases) && regreso.clases.length > 0 && (
                      <span className="label">
                        Clases: {regreso.clases.map(c => c.idClase).join(", ")}
                      </span>
                    )}
                  </div>
                </div>
              </section>
            )}
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

            <h3 className="block-title" style={{ marginTop: 16 }}>Escala</h3>
            {!v.escalas || v.escalas.length === 0 ? (
              <p>Vuelo directo.</p>
            ) : (
              v.escalas.map((e, idx) => (
                <div className="vd__escala" key={idx}>
                  <div className="vd__esc__head">
                    <strong>{e.ciudad}</strong> <span>({e.pais})</span>
                  </div>
                  <div className="vd__esc__times">
                    <div><span className="label">Llega</span> {fmtDateTime(e.llegada)}</div>
                    <div><span className="label">Sale</span> {fmtDateTime(e.salida)}</div>
                  </div>
                </div>
              ))
            )}
          </section>
        </div>

        <div className="actions" style={{ marginTop: 12, display: "flex", gap: 8, alignItems: "center" }}>
          <Link to="/vuelos" className="btn">Cerrar</Link>

          {logged ? (
            v?.clases?.length > 0 && (
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
                  Añadir a carrito
                </button>
              </>
            )
          ) : (
            <button className="btn btn-secondary" onClick={() => nav("/login")}>
              Añadir a carrito
            </button>
          )}
        </div>

        <section style={{ marginTop: 24 }}>
          <h3 className="block-title">Calificación del vuelo</h3>

          {rLoading ? (
            <div className="skl row" />
          ) : rErr ? (
            <div className="error">{rErr}</div>
          ) : (
            <div className="rating__summary" style={{ marginBottom: 10 }}>
              <StarsBar value={avg} size="lg" />
              <div className="rating__nums">
                <strong>{avg?.toFixed(1) ?? "0.0"}</strong>
                <span className="label">promedio</span>
                <span className="sep">•</span>
                <span className="label">{total} {total === 1 ? "voto" : "votos"}</span>
              </div>
            </div>
          )}

          <div className="cmt__form">
            {!logged ? (
              <div className="hint">
                <button className="link" onClick={() => nav("/login")}>Inicia sesión</button> para calificar y comentar.
              </div>
            ) : (
              <>
                <div className="label" style={{ marginBottom: 6 }}>
                  {myRating != null ? (
                    <>Tu calificación: <b>{myRating}★</b></>
                  ) : (
                    "Tu calificación:"
                  )}
                </div>

                <div
                  className="starsedit"
                  onMouseLeave={() => setHover(0)}
                  style={{ marginBottom: 8 }}
                >
                  {[1, 2, 3, 4, 5].map((i) => (
                    <Star
                      key={i}
                      filled={(hover || sel || myRating || 0) >= i}
                      onClick={() => (myRating == null ? setSel(i) : null)}
                      onMouseEnter={() => (myRating == null ? setHover(i) : null)}
                      onMouseLeave={() => (myRating == null ? setHover(0) : null)}
                      disabled={myRating != null}
                    />
                  ))}
                </div>

                {replyTo && (
                  <div className="hint" style={{ marginBottom: 6 }}>
                    Respondiendo al comentario de{" "}
                    <strong>{replyMeta?.autor || `Usuario #${replyTo}`}</strong>{" "}
                    <button className="link" onClick={() => setReplyTo(null)}>
                      (cancelar)
                    </button>
                  </div>
                )}

                <textarea
                  className="input"
                  rows={3}
                  placeholder="Escribe tu comentario…"
                  value={text}
                  onChange={(e) => setText(e.target.value)}
                />
                <div style={{ display: "flex", gap: 8, marginTop: 6 }}>
                  <button
                    className="btn btn-secondary"
                    onClick={publicar}
                    disabled={publishing}
                  >
                    {publishing ? "Publicando…" : "Publicar"}
                  </button>
                  <button
                    className="btn"
                    onClick={() => { setText(""); setReplyTo(null); if (myRating == null) setSel(0); }}
                    disabled={publishing}
                  >
                    Limpiar
                  </button>
                </div>
              </>
            )}
          </div>

          <div className="cmt__list" style={{ marginTop: 12 }}>
            {cmtLoading ? (
              <div className="skl row" />
            ) : cmtErr ? (
              <div className="error">{cmtErr}</div>
            ) : comentarios.length === 0 ? (
              <p className="hint">No hay comentarios</p>
            ) : (
              comentarios.map((c) => (
                <CommentItem key={c.idComentario} item={c} />
              ))
            )}
          </div>
        </section>
      </div>
    </div>
  );
}
