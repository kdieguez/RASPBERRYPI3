import { useEffect, useState } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { vuelosApi, rutasApi, clasesApi, paisesApi, ciudadesApi } from "../../api/adminCatalogos";

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
const toLocalInput = (val) => {
  const d = toDate(val);
  if (!d) return "";
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
};
const ensureSeconds = (s) => (!s ? "" : s.length === 16 ? `${s}:00` : s);

const CANCELADO_ID = 2;

export default function VueloEdit() {
  const { id } = useParams();
  const nav = useNavigate();

  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");
  const [msg, setMsg] = useState("");

  const [codigo, setCodigo]     = useState("");
  const [idRuta, setIdRuta]     = useState("");
  const [salida, setSalida]     = useState("");
  const [llegada, setLlegada]   = useState("");
  const [activo, setActivo]     = useState(true);

  const [idEstado, setIdEstado] = useState(null);
  const [estadoNombre, setEstadoNombre] = useState("");

  const [clases, setClases]     = useState([]);
  const [rutas, setRutas]       = useState([]);

  const [editEsc, setEditEsc]   = useState(false);
  const [escPais, setEscPais]   = useState("");
  const [escCiudad, setEscCiudad] = useState("");
  const [escLlegada, setEscLlegada] = useState("");
  const [escSalida, setEscSalida]   = useState("");
  const [paises, setPaises]     = useState([]);
  const [ciudades, setCiudades] = useState([]);

  const [hasReturn, setHasReturn] = useState(false);
  const [returnId, setReturnId]   = useState(null);
  const [rCodigo, setRCodigo]     = useState("");
  const [rRuta, setRRuta]         = useState("");
  const [rSalida, setRSalida]     = useState("");
  const [rLlegada, setRLlegada]   = useState("");
  const [rActivo, setRActivo]     = useState(true);

  const esCancelado = idEstado === CANCELADO_ID || (estadoNombre || "").toLowerCase().includes("cancel");
  const disableAll = esCancelado;

  useEffect(() => {
    (async () => {
      try {
        setLoading(true);
        setErr("");
        setMsg("");

        const [{ data: v }, { data: rutasRes }, { data: clasesRes }, { data: paisesRes }] =
          await Promise.all([
            vuelosApi.getAdmin(id),
            rutasApi.list(),
            clasesApi.list(),
            paisesApi.list(),
          ]);

        setCodigo(v.codigo ?? "");
        setIdRuta(String(v.idRuta ?? ""));
        setSalida(toLocalInput(v.fechaSalida));
        setLlegada(toLocalInput(v.fechaLlegada));
        setActivo(Boolean(v.activo));

        setIdEstado(v.idEstado ?? null);
        setEstadoNombre(v.estado ?? "");

        const base = (clasesRes || []).map((c) => ({
          idClase: c.idClase, nombre: c.nombre,
          cupoTotal: "", precio: "", enabled: false
        }));
        (v.clases || []).forEach((vc) => {
          const i = base.findIndex(b => Number(b.idClase) === Number(vc.idClase));
          if (i >= 0) base[i] = { ...base[i], enabled: true, cupoTotal: vc.cupoTotal, precio: vc.precio };
        });
        setClases(base);
        setRutas(rutasRes || []);
        setPaises(paisesRes || []);

        if (Array.isArray(v.escalas) && v.escalas.length > 0) {
          const e = v.escalas[0];
          setEditEsc(true);
          setEscCiudad(String(e.idCiudad));
          setEscLlegada(toLocalInput(e.llegada));
          setEscSalida(toLocalInput(e.salida));
          setCiudades([{ idCiudad: e.idCiudad, nombre: `${e.ciudad} (${e.pais})` }]);
        } else {
          setEditEsc(false);
          setEscPais(""); setEscCiudad(""); setEscLlegada(""); setEscSalida(""); setCiudades([]);
        }

        if (v.idVueloPareja) {
          setHasReturn(true);
          setReturnId(v.idVueloPareja);
          const { data: r } = await vuelosApi.getAdmin(v.idVueloPareja);
          setRCodigo(r.codigo ?? "");
          setRRuta(String(r.idRuta ?? ""));
          setRSalida(toLocalInput(r.fechaSalida));
          setRLlegada(toLocalInput(r.fechaLlegada));
          setRActivo(Boolean(r.activo));
        } else {
          setHasReturn(false);
          setReturnId(null);
          const rutaIdNum = Number(v.idRuta);
          const ruta = (rutasRes || []).find(x => Number(x.idRuta) === rutaIdNum);
          if (ruta) {
            const invertida = (rutasRes || []).find(x =>
              x.ciudadOrigen === ruta.ciudadDestino && x.ciudadDestino === ruta.ciudadOrigen
            );
            setRRuta(invertida ? String(invertida.idRuta) : "");
          } else {
            setRRuta("");
          }
          setRCodigo(""); setRSalida(""); setRLlegada(""); setRActivo(true);
        }
      } catch (e) {
        console.error(e);
        setErr(e?.response?.data?.error || "No se pudo cargar el vuelo");
      } finally {
        setLoading(false);
      }
    })();
  }, [id]);

  const onClase = (idClase, key, val) =>
    setClases(list => list.map(c => c.idClase === idClase ? { ...c, [key]: val.replace(/[^0-9.]/g,"") } : c));
  const toggleClase = (idClase) =>
    setClases(list => list.map(c => c.idClase === idClase ? { ...c, enabled: !c.enabled } : c));
  const payloadClases = () => {
    const on = clases.filter(c => c.enabled);
    if (!on.length) return null;
    const out = [];
    for (const c of on) {
      const cupo = Number(c.cupoTotal), precio = Number(c.precio);
      if (!Number.isFinite(cupo) || cupo <= 0) return null;
      if (!Number.isFinite(precio) || precio <= 0) return null;
      out.push({ idClase: c.idClase, cupoTotal: cupo, precio });
    }
    return out;
  };

  const onPaisChange = async (idPais) => {
    setEscPais(idPais); setEscCiudad(""); setCiudades([]);
    if (!idPais) return;
    try { const { data } = await ciudadesApi.list(idPais); setCiudades(data || []); } catch {}
  };

  const canSaveIda =
    !disableAll &&
    codigo.trim() && idRuta && salida && llegada &&
    payloadClases() &&
    new Date(ensureSeconds(salida)) < new Date(ensureSeconds(llegada)) &&
    (!editEsc ||
      (escCiudad && escLlegada && escSalida &&
       new Date(ensureSeconds(escLlegada)) <= new Date(ensureSeconds(escSalida))));

  const save = async (e) => {
    e.preventDefault();
    if (disableAll) { setErr("Este vuelo está cancelado y no puede modificarse."); return; }

    const clasesOut = payloadClases();
    if (!clasesOut) { setErr("Revisa las clases (cupo y precio)."); return; }

    let mot = window.prompt("Escribe el motivo de modificación:");
    if (!mot || !mot.trim()) {
      setErr("El motivo del cambio es requerido.");
      return;
    }
    mot = mot.trim();

    setErr("");
    setMsg("");

    const escalasOut = !editEsc ? null : [{
      idCiudad: Number(escCiudad),
      llegada: ensureSeconds(escLlegada),
      salida: ensureSeconds(escSalida),
    }];

    try {
      await vuelosApi.updateAdmin(id, {
        codigo: codigo.trim().toUpperCase(),
        idRuta: Number(idRuta),
        fechaSalida: ensureSeconds(salida),
        fechaLlegada: ensureSeconds(llegada),
        activo,
        clases: clasesOut,
        escalas: escalasOut,
        motivoCambio: mot
      });

      if (hasReturn) {
        const regresoDto = {
          codigo: (rCodigo || "").trim().toUpperCase(),
          idRuta: Number(rRuta),
          fechaSalida: ensureSeconds(rSalida),
          fechaLlegada: ensureSeconds(rLlegada),
          activo: rActivo,
          clases: clasesOut,
          escalas: [],
          motivoCambio: mot
        };

        if (returnId) {
          await vuelosApi.updateAdmin(returnId, regresoDto);
        } else {
          const { data } = await vuelosApi.createAdmin(regresoDto);
          const nuevoId = data?.idVuelo;
          if (!nuevoId) throw new Error("No se obtuvo id del vuelo de regreso");
          await vuelosApi.link({ idIda: Number(id), idRegreso: Number(nuevoId) });
        }
      }

      setMsg("Cambios guardados.");
      setTimeout(() => setMsg(""), 3000);
    } catch (e2) {
      console.error(e2);
      setErr(e2?.response?.data?.error || "No se pudo guardar");
    }
  };

  const cancelarVuelo = async () => {
    if (esCancelado) return;
    const motivo = prompt("Escribe el motivo de cancelación:");
    if (!motivo || !motivo.trim()) return;

    setErr("");
    setMsg("");
    try {
      await vuelosApi.updateEstado(id, CANCELADO_ID, motivo.trim());
      setIdEstado(CANCELADO_ID);
      setEstadoNombre("Cancelado");
      setMsg("Vuelo cancelado.");
      setTimeout(() => setMsg(""), 3000);
    } catch (e) {
      console.error(e);
      setErr(e?.response?.data?.error || "No se pudo cambiar el estado");
    }
  };

  return (
    <div className="container pag-vuelo">
      <div className="card">
        <div className="perfil__head">
          <div className="avatar">
            <svg width="36" height="36" viewBox="0 0 24 24" aria-hidden="true">
              <path fill="currentColor" d="M2 13c7-1 10-4 12-9l2 1-2 6 7 1 1 2-7 1 2 6-2 1c-2-5-5-8-12-9v-1Z"/>
            </svg>
          </div>
          <div>
            <h2>Editar vuelo #{id}</h2>
            <p className="subtitle"><Link to="/vuelos">Volver a la lista</Link></p>
            <div style={{marginTop:6}}>
              <span className={`pill ${esCancelado ? "pill--bad" : "pill--ok"}`} style={{marginRight:6}}>
                {esCancelado ? "Cancelado" : (estadoNombre || "Programado")}
              </span>
              <span className={`pill ${activo ? "pill--ok" : ""}`}>
                {activo ? "Activo" : "Inactivo"}
              </span>
            </div>
          </div>
        </div>

        {esCancelado && (
          <div className="error" style={{marginTop:8}}>
            Este vuelo está cancelado. La edición está deshabilitada.
          </div>
        )}

        {loading ? (
          <>
            <div className="skl title" /><div className="skl row" /><div className="skl row" />
          </>
        ) : (
          <form className={`form ${disableAll ? "input-readonly" : ""}`} onSubmit={save}>
            <div className="grid-2">
              <div>
                <label className="label">Código</label>
                <input className="input" value={codigo} onChange={(e)=>setCodigo(e.target.value)} disabled={disableAll} />
              </div>
              <div>
                <label className="label">Ruta</label>
                <select className="input" value={idRuta} onChange={(e)=>setIdRuta(e.target.value)} disabled={disableAll}>
                  <option value="">-- Selecciona una ruta --</option>
                  {rutas.map(r => (
                    <option key={r.idRuta} value={r.idRuta}>
                      {r.ciudadOrigen} → {r.ciudadDestino}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="grid-2">
              <div>
                <label className="label">Salida</label>
                <input type="datetime-local" className="input" value={salida} onChange={(e)=>setSalida(e.target.value)} disabled={disableAll} />
              </div>
              <div>
                <label className="label">Llegada</label>
                <input type="datetime-local" className="input" value={llegada} onChange={(e)=>setLlegada(e.target.value)} disabled={disableAll} />
              </div>
            </div>

            <label className="check" style={{marginTop:8}}>
              <input type="checkbox" checked={activo} onChange={(e)=>setActivo(e.target.checked)} disabled={disableAll} />
              <span>Activo</span>
            </label>

            <h3 className="block-title" style={{marginTop:16}}>Estado del vuelo</h3>
            <div className="grid-2" style={{alignItems:"end"}}>
              <div>
                <div className="label" style={{marginBottom:6}}>Estado actual</div>
                <span className={`pill ${esCancelado ? "pill--bad" : "pill--ok"}`}>
                  {esCancelado ? "Cancelado" : (estadoNombre || "Programado")}
                </span>
              </div>
              <div style={{textAlign:"right"}}>
                <button
                  type="button"
                  className="btn btn-danger"
                  onClick={cancelarVuelo}
                  disabled={esCancelado}
                  title={esCancelado ? "Un vuelo cancelado no puede volver a programado" : "Cambiar a Cancelado"}
                >
                  Cancelar vuelo
                </button>
              </div>
            </div>

            <h3 className="block-title" style={{marginTop:16}}>Clases y precios</h3>
            {!clases.length ? (
              <p className="hint">No hay clases en catálogo.</p>
            ) : (
              <div className="grid-2">
                {clases.map((c) => (
                  <div key={c.idClase} className={`clase ${!c.enabled ? "off" : ""}`}>
                    <label className="check" style={{marginBottom:8}}>
                      <input
                        type="checkbox"
                        checked={c.enabled}
                        onChange={() => toggleClase(c.idClase)}
                        disabled={disableAll}
                      />
                      <span>{c.nombre}</span>
                    </label>

                    <div className="grid-2">
                      <div>
                        <label className="label">Cupo</label>
                        <input
                          className="input"
                          value={c.cupoTotal}
                          onChange={(e)=>onClase(c.idClase, "cupoTotal", e.target.value)}
                          disabled={!c.enabled || disableAll}
                          inputMode="numeric"
                          placeholder="Ej. 20"
                        />
                      </div>
                      <div>
                        <label className="label">Precio</label>
                        <input
                          className="input"
                          value={c.precio}
                          onChange={(e)=>onClase(c.idClase, "precio", e.target.value)}
                          disabled={!c.enabled || disableAll}
                          inputMode="decimal"
                          placeholder="Ej. 499.00"
                        />
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}

            <h3 className="block-title" style={{marginTop:16}}>Escala (máx. 1)</h3>
            <label className="check" style={{marginBottom:8}}>
              <input type="checkbox" checked={editEsc} onChange={(e)=>setEditEsc(e.target.checked)} disabled={disableAll} />
              <span>Editar/Añadir 1 escala</span>
            </label>

            {editEsc && (
              <>
                <div className="grid-3" style={{alignItems:"end"}}>
                  <div>
                    <label className="label">País (opcional para cambiar ciudad)</label>
                    <select className="input" value={escPais} onChange={(e)=>onPaisChange(e.target.value)} disabled={disableAll}>
                      <option value="">-- Selecciona país --</option>
                      {paises.map(p => <option key={p.idPais} value={p.idPais}>{p.nombre}</option>)}
                    </select>
                  </div>
                  <div>
                    <label className="label">Ciudad</label>
                    <select className="input" value={escCiudad} onChange={(e)=>setEscCiudad(e.target.value)} disabled={disableAll}>
                      <option value="">-- Selecciona ciudad --</option>
                      {ciudades.map(c => <option key={c.idCiudad} value={c.idCiudad}>{c.nombre}</option>)}
                    </select>
                  </div>
                  <div />
                </div>
                <div className="grid-2" style={{marginTop:8}}>
                  <div>
                    <label className="label">Llegada (escala)</label>
                    <input type="datetime-local" className="input" value={escLlegada} onChange={(e)=>setEscLlegada(e.target.value)} disabled={disableAll} />
                  </div>
                  <div>
                    <label className="label">Salida (escala)</label>
                    <input type="datetime-local" className="input" value={escSalida} onChange={(e)=>setEscSalida(e.target.value)} disabled={disableAll} />
                  </div>
                </div>
              </>
            )}

            <h3 className="block-title" style={{marginTop:24}}>Vuelo de regreso</h3>
            <label className="check" style={{marginBottom:8}}>
              <input
                type="checkbox"
                checked={hasReturn}
                onChange={(e)=>setHasReturn(e.target.checked)}
                disabled={disableAll}
              />
              <span>{returnId ? `Editar regreso #${returnId}` : "Añadir vuelo de regreso"}</span>
            </label>

            {hasReturn && (
              <>
                <div className="grid-2">
                  <div>
                    <label className="label">Código (regreso)</label>
                    <input className="input" value={rCodigo} onChange={(e)=>setRCodigo(e.target.value)} disabled={disableAll} />
                  </div>
                  <div>
                    <label className="label">Ruta (regreso)</label>
                    <select className="input" value={rRuta} onChange={(e)=>setRRuta(e.target.value)} disabled={disableAll}>
                      <option value="">-- Selecciona una ruta --</option>
                      {rutas.map(r => (
                        <option key={r.idRuta} value={r.idRuta}>
                          {r.ciudadOrigen} → {r.ciudadDestino}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="grid-2">
                  <div>
                    <label className="label">Salida (regreso)</label>
                    <input type="datetime-local" className="input" value={rSalida} onChange={(e)=>setRSalida(e.target.value)} disabled={disableAll} />
                  </div>
                  <div>
                    <label className="label">Llegada (regreso)</label>
                    <input type="datetime-local" className="input" value={rLlegada} onChange={(e)=>setRLlegada(e.target.value)} disabled={disableAll} />
                  </div>
                </div>

                <label className="check" style={{marginTop:8}}>
                  <input type="checkbox" checked={rActivo} onChange={(e)=>setRActivo(e.target.checked)} disabled={disableAll} />
                  <span>Activo (regreso)</span>
                </label>
              </>
            )}

            {err && <div className="error" role="alert" style={{marginTop:12}}>{err}</div>}
            {msg && <div className="success" role="status" style={{marginTop:12}}>{msg}</div>}

            <div className="actions" style={{marginTop:12}}>
              {!esCancelado && (
                <button
                  className="btn btn-secondary"
                  disabled={!canSaveIda}
                >
                  Guardar cambios
                </button>
              )}
              <Link to="/vuelos" className="btn" style={{marginLeft:8}}>Cerrar</Link>
            </div>

          </form>
        )}
      </div>
    </div>
  );
}
