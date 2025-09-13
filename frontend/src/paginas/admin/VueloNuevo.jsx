import { useEffect, useMemo, useState } from "react";
import axios from "../../lib/axios";
import { rutasApi, paisesApi, ciudadesApi, clasesApi, vuelosApi } from "../../api/adminCatalogos";
import AdminNav from "./AdminNav";
import "../../styles/vuelos.css";

const ensureSeconds = (s) => (!s ? null : (s.length === 16 ? `${s}:00` : s));

const genFlightCode = () => {
  const L = "ABCDEFGHJKMNPQRSTUVWXYZ"; 
  const D = "0123456789";
  const pick = (str) => str[Math.floor(Math.random() * str.length)];
  return `AR${pick(L)}${pick(D)}${pick(L)}${pick(D)}`;
};

export default function VueloNuevo() {
  const [codigo, setCodigo] = useState("");
  const [idRuta, setIdRuta] = useState("");
  const [salida, setSalida] = useState("");
  const [llegada, setLlegada] = useState("");

  const [isRoundTrip, setIsRoundTrip] = useState(false);
  const [codigoR, setCodigoR] = useState("");
  const [idRutaR, setIdRutaR] = useState("");
  const [salidaR, setSalidaR] = useState("");
  const [llegadaR, setLlegadaR] = useState("");

  const [rutas, setRutas] = useState([]);
  const [paises, setPaises] = useState([]);
  const [clases, setClases] = useState([]);     
  const [clasesR, setClasesR] = useState([]);  
  const [sameClasses, setSameClasses] = useState(true);

  const [usaEscala, setUsaEscala] = useState(false);
  const [paisEsc, setPaisEsc] = useState("");
  const [ciudadesEsc, setCiudadesEsc] = useState([]);
  const [idCiudadEsc, setIdCiudadEsc] = useState("");
  const [llegadaEsc, setLlegadaEsc] = useState("");
  const [salidaEsc, setSalidaEsc] = useState("");

  const [err, setErr] = useState("");
  const [ok, setOk] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        setLoading(true);
        setErr("");
        const [rutasRes, paisesRes, clasesRes] = await Promise.all([
          rutasApi.list(),
          paisesApi.list(),
          clasesApi.list(),
        ]);
        setRutas(rutasRes.data || []);
        setPaises(paisesRes.data || []);

        const base = (clasesRes.data || []).map((c, idx) => ({
          idClase: c.idClase,
          nombre: c.nombre,
          enabled: idx === 0,
          cupo: "",
          precio: "",
        }));
        setClases(base);
        setClasesR(base.map(x => ({ ...x })));

        setCodigo(genFlightCode());
        setCodigoR(genFlightCode());
      } catch (e) {
        setErr(e?.response?.data?.error || "No se pudieron cargar catálogos");
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  useEffect(() => {
    if (!paisEsc) { setCiudadesEsc([]); setIdCiudadEsc(""); return; }
    (async () => {
      try {
        const { data } = await ciudadesApi.list(paisEsc);
        setCiudadesEsc(data || []);
      } catch {
        setErr("No se pudieron cargar ciudades de la escala");
      }
    })();
  }, [paisEsc]);

  useEffect(() => {
    if (!isRoundTrip || !idRuta) return;
    const r = rutas.find(x => String(x.idRuta) === String(idRuta));
    if (!r) return;
    const inv = rutas.find(x =>
      String(x.idCiudadOrigen) === String(r.idCiudadDestino) &&
      String(x.idCiudadDestino) === String(r.idCiudadOrigen)
    );
    if (inv) setIdRutaR(String(inv.idRuta));
  }, [isRoundTrip, idRuta, rutas]);

  const toggleClase = (listSetter) => (idClase) =>
    listSetter(list => list.map(c => c.idClase === idClase ? { ...c, enabled: !c.enabled } : c));
  const onClase = (listSetter) => (idClase, k, v) =>
    listSetter(list => list.map(c => c.idClase === idClase ? { ...c, [k]: v.replace(/[^0-9.]/g, "") } : c));

  const clasesPayload = (list) => {
    const on = list.filter(c => c.enabled);
    if (on.length === 0) return null;
    const out = [];
    for (const c of on) {
      const cupo = Number(c.cupo);
      const precio = Number(c.precio);
      if (!Number.isFinite(cupo) || cupo <= 0) return null;
      if (!Number.isFinite(precio) || precio <= 0) return null;
      out.push({ idClase: c.idClase, cupoTotal: cupo, precio });
    }
    return out;
  };

  const escalaPayloadIda = useMemo(() => {
    if (!usaEscala) return [];
    if (!idCiudadEsc || !llegadaEsc || !salidaEsc) return null;
    if (new Date(llegadaEsc) > new Date(salidaEsc)) return null;
    return [{
      idCiudad: Number(idCiudadEsc),
      llegada: ensureSeconds(llegadaEsc),
      salida:  ensureSeconds(salidaEsc),
    }];
  }, [usaEscala, idCiudadEsc, llegadaEsc, salidaEsc]);

  const idaOk =
    codigo.trim() && idRuta && salida && llegada &&
    clasesPayload(clases) && escalaPayloadIda !== null &&
    new Date(salida) < new Date(llegada);

  const regresoOk = !isRoundTrip || (
    codigoR.trim() && idRutaR && salidaR && llegadaR &&
    (sameClasses ? true : !!clasesPayload(clasesR)) &&
    new Date(salidaR) < new Date(llegadaR)
  );

  const canSave = idaOk && regresoOk;

  const submit = async (e) => {
    e.preventDefault();
    setErr(""); setOk(false);
    if (!canSave) { setErr("Revisa los campos obligatorios."); return; }

    try {
      setSaving(true);

      if (!isRoundTrip) {
        const payload = {
          codigo: codigo.trim().toUpperCase(),
          idRuta: Number(idRuta),
          fechaSalida: ensureSeconds(salida),
          fechaLlegada: ensureSeconds(llegada),
          clases: clasesPayload(clases),
          escalas: escalaPayloadIda || [],
        };
        await vuelosApi.create(payload);
      } else {
        const ida = {
          codigo: codigo.trim().toUpperCase(),
          idRuta: Number(idRuta),
          fechaSalida: ensureSeconds(salida),
          fechaLlegada: ensureSeconds(llegada),
          clases: clasesPayload(clases),
          escalas: escalaPayloadIda || [],
        };
        const regreso = {
          codigo: codigoR.trim().toUpperCase(),
          idRuta: Number(idRutaR),
          fechaSalida: ensureSeconds(salidaR),
          fechaLlegada: ensureSeconds(llegadaR),
          clases: sameClasses ? clasesPayload(clases) : clasesPayload(clasesR),
          escalas: [],
        };
        await vuelosApi.createRoundTrip({ ida, regreso });
      }

      setOk(true);

      setCodigo(genFlightCode());
      setCodigoR(genFlightCode());
      setIdRuta(""); setSalida(""); setLlegada("");
      setIsRoundTrip(false);
      setIdRutaR(""); setSalidaR(""); setLlegadaR("");
      setClases(clases.map((c, i) => ({ ...c, enabled: i === 0, cupo: "", precio: "" })));
      setClasesR(clasesR.map((c, i) => ({ ...c, enabled: i === 0, cupo: "", precio: "" })));
      setUsaEscala(false); setPaisEsc(""); setIdCiudadEsc(""); setLlegadaEsc(""); setSalidaEsc("");
    } catch (e2) {
      setErr(e2?.response?.data?.error || "No se pudo crear el vuelo");
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <AdminNav/>
      <div className="container pag-vuelo">
        <div className="card">
          <div className="perfil__head">
            <div className="avatar">
              <svg width="36" height="36" viewBox="0 0 24 24" aria-hidden="true">
                <path fill="currentColor" d="M2 13c7-1 10-4 12-9l2 1-2 6 7 1 1 2-7 1 2 6-2 1c-2-5-5-8-12-9v-1Z"/>
              </svg>
            </div>
            <div>
              <h2>Crear vuelo</h2>
              <p className="subtitle">Configura un nuevo vuelo. Volver al panel</p>
            </div>
          </div>

          {loading ? (
            <>
              <div className="skl title" />
              <div className="skl row" /><div className="skl row" />
            </>
          ) : (
            <form className="form" onSubmit={submit}>
              <h3 className="block-title">Ida</h3>
              <div className="grid-2">
                <div>
                  <label className="label">Código</label>
                  <input className="input" value={codigo} readOnly />
                  <div className="hint">Se genera automáticamente</div>
                </div>
                <div>
                  <label className="label">Ruta</label>
                  <select className="input" value={idRuta} onChange={e=>setIdRuta(e.target.value)}>
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
                  <input type="datetime-local" className="input" value={salida}
                         onChange={e=>setSalida(e.target.value)} />
                </div>
                <div>
                  <label className="label">Llegada</label>
                  <input type="datetime-local" className="input" value={llegada}
                         onChange={e=>setLlegada(e.target.value)} />
                </div>
              </div>

              <h4 className="block-subtitle">Clases (ida)</h4>
              <div className="grid-3">
                {clases.map(c => (
                  <div key={c.idClase} className={"clase " + (c.enabled ? "on" : "off")}>
                    <label className="check">
                      <input type="checkbox" checked={c.enabled} onChange={()=>toggleClase(setClases)(c.idClase)} />
                      <span>{c.nombre}</span>
                    </label>
                    <div className="grid-2">
                      <input className="input" placeholder="Cupo total" inputMode="numeric"
                             value={c.cupo} onChange={e=>onClase(setClases)(c.idClase,"cupo",e.target.value)} />
                      <input className="input" placeholder="Precio" inputMode="decimal"
                             value={c.precio} onChange={e=>onClase(setClases)(c.idClase,"precio",e.target.value)} />
                    </div>
                  </div>
                ))}
              </div>

              <hr className="sep" />

              <label className="check" style={{marginBottom:8}}>
                <input type="checkbox" checked={usaEscala} onChange={e=>setUsaEscala(e.target.checked)} />
                <span>Añadir 1 escala (opcional)</span>
              </label>

              {usaEscala && (
                <div className="grid-3">
                  <div>
                    <label className="label">País de escala</label>
                    <select className="input" value={paisEsc} onChange={e=>setPaisEsc(e.target.value)}>
                      <option value="">-- Selecciona --</option>
                      {paises.map(p => <option key={p.idPais} value={p.idPais}>{p.nombre}</option>)}
                    </select>
                    <label className="label" style={{marginTop:8}}>Ciudad de escala</label>
                    <select className="input" value={idCiudadEsc}
                            onChange={e=>setIdCiudadEsc(e.target.value)} disabled={!ciudadesEsc.length}>
                      <option value="">-- Selecciona --</option>
                      {ciudadesEsc.map(c => <option key={c.idCiudad} value={c.idCiudad}>{c.nombre}</option>)}
                    </select>
                  </div>
                  <div>
                    <label className="label">Llegada escala</label>
                    <input type="datetime-local" className="input" value={llegadaEsc}
                           onChange={e=>setLlegadaEsc(e.target.value)} />
                  </div>
                  <div>
                    <label className="label">Salida escala</label>
                    <input type="datetime-local" className="input" value={salidaEsc}
                           onChange={e=>setSalidaEsc(e.target.value)} />
                    <div className="hint">Debe ser mayor o igual a la hora de llegada</div>
                  </div>
                </div>
              )}

              <hr className="sep" />

              <label className="check" style={{marginBottom:8}}>
                <input type="checkbox" checked={isRoundTrip} onChange={e=>setIsRoundTrip(e.target.checked)} />
                <span>Ida y regreso</span>
              </label>

              {isRoundTrip && (
                <>
                  <h3 className="block-title">Regreso</h3>
                  <div className="grid-2">
                    <div>
                      <label className="label">Código (regreso)</label>
                      <input className="input" value={codigoR} readOnly />
                      <div className="hint">Se genera automáticamente</div>
                    </div>
                    <div>
                      <label className="label">Ruta (regreso)</label>
                      <select className="input" value={idRutaR} onChange={e=>setIdRutaR(e.target.value)}>
                        <option value="">-- Selecciona una ruta --</option>
                        {rutas.map(r => (
                          <option key={r.idRuta} value={r.idRuta}>
                            {r.ciudadOrigen} → {r.ciudadDestino}
                          </option>
                        ))}
                      </select>
                      <div className="hint">Se sugiere la ruta inversa de la ida</div>
                    </div>
                  </div>

                  <div className="grid-2">
                    <div>
                      <label className="label">Salida (regreso)</label>
                      <input type="datetime-local" className="input" value={salidaR}
                             onChange={e=>setSalidaR(e.target.value)} />
                    </div>
                    <div>
                      <label className="label">Llegada (regreso)</label>
                      <input type="datetime-local" className="input" value={llegadaR}
                             onChange={e=>setLlegadaR(e.target.value)} />
                    </div>
                  </div>

                  <label className="check" style={{marginTop:8, marginBottom:8}}>
                    <input type="checkbox" checked={sameClasses} onChange={e=>setSameClasses(e.target.checked)} />
                    <span>Usar las mismas clases y precios que la ida</span>
                  </label>

                  {!sameClasses && (
                    <>
                      <h4 className="block-subtitle">Clases (regreso)</h4>
                      <div className="grid-3">
                        {clasesR.map(c => (
                          <div key={c.idClase} className={"clase " + (c.enabled ? "on" : "off")}>
                            <label className="check">
                              <input type="checkbox" checked={c.enabled} onChange={()=>toggleClase(setClasesR)(c.idClase)} />
                              <span>{c.nombre}</span>
                            </label>
                            <div className="grid-2">
                              <input className="input" placeholder="Cupo total" inputMode="numeric"
                                     value={c.cupo} onChange={e=>onClase(setClasesR)(c.idClase,"cupo",e.target.value)} />
                              <input className="input" placeholder="Precio" inputMode="decimal"
                                     value={c.precio} onChange={e=>onClase(setClasesR)(c.idClase,"precio",e.target.value)} />
                            </div>
                          </div>
                        ))}
                      </div>
                    </>
                  )}

                  <hr className="sep" />
                </>
              )}

              {err && <div className="error" role="alert">{err}</div>}
              {ok  && <div className="ok">¡Vuelo(s) creado(s)!</div>}

              <div className="actions">
                <button className="btn btn-secondary" disabled={!canSave || saving}>
                  {saving ? "Guardando..." : "Guardar"}
                </button>
              </div>
            </form>
          )}
        </div>
      </div>
    </>
  );
}
