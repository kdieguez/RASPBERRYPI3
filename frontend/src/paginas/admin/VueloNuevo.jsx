import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import axios from "../../lib/axios";
import "../../styles/adminVuelos.css";

const CLASES = [
  { idClase: 1, nombre: "Económica" },
  { idClase: 2, nombre: "Ejecutiva" },
  { idClase: 3, nombre: "Primera"  },
];

const toIso = (v) => {
  if (!v) return null;
  return v.length === 16 ? `${v}:00` : v; 
};

export default function VueloNuevo(){
  const nav = useNavigate();

  const [loading, setLoading] = useState(true);
  const [saving,  setSaving] = useState(false);
  const [err, setErr] = useState("");
  const [ok, setOk] = useState(false);

  const [rutas, setRutas] = useState([]);
  const [ciudades,setCiudades]= useState([]);

  const [codigo, setCodigo] = useState("");
  const [idRuta, setIdRuta] = useState("");
  const [salida, setSalida] = useState("");   
  const [llegada,setLlegada]= useState("");   

  const [clases, setClases] = useState(
    CLASES.map(c => ({ ...c, enabled: true, cupoTotal: "", precio: "" }))
  );

  const [usarEscala, setUsarEscala] = useState(false);
  const [escCiudad,  setEscCiudad]  = useState("");
  const [escLlegada, setEscLlegada] = useState("");
  const [escSalida,  setEscSalida]  = useState("");

  const canSubmit = useMemo(() => {
    if (!codigo.trim() || !idRuta || !salida || !llegada) return false;
    const s = new Date(salida); const l = new Date(llegada);
    if (!(s < l)) return false;

    const clasesValidas = clases
      .filter(c => c.enabled)
      .filter(c => Number(c.cupoTotal) > 0 && Number(c.precio) > 0);
    if (clasesValidas.length === 0) return false;

    if (usarEscala) {
      if (!escCiudad || !escLlegada || !escSalida) return false;
      const el = new Date(escLlegada);
      const es = new Date(escSalida);
      if (!(el <= es)) return false; 
    }
    return true;
  }, [codigo, idRuta, salida, llegada, clases, usarEscala, escCiudad, escLlegada, escSalida]);

  useEffect(() => {
    (async () => {
      try {
        setLoading(true);
        const [rutasRes, ciuRes] = await Promise.all([
          axios.get("/api/v1/rutas"),         
          axios.get("/api/public/ciudades"),    
        ]);
        setRutas(rutasRes.data || []);
        setCiudades(ciuRes.data || []);
      } catch (e) {
        setErr(e?.response?.data?.error || "No se pudieron cargar catálogos");
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const toggleClase = (id) => {
    setClases(list => list.map(c => c.idClase === id ? { ...c, enabled: !c.enabled } : c));
  };

  const setClaseField = (id, field, value) => {
    setClases(list => list.map(c => c.idClase === id ? { ...c, [field]: value } : c));
  };

  const submit = async (e) => {
    e.preventDefault();
    if (!canSubmit) return;

    setErr(""); setOk(false); setSaving(true);

    try {
      const payload = {
        codigo: codigo.trim(),
        idRuta: Number(idRuta),
        fechaSalida: toIso(salida),
        fechaLlegada: toIso(llegada),
        clases: clases
          .filter(c => c.enabled && Number(c.cupoTotal) > 0 && Number(c.precio) > 0)
          .map(c => ({
            idClase: c.idClase,
            cupoTotal: Number(c.cupoTotal),
            precio: Number(c.precio),
          })),
        escalas: (!usarEscala || !escCiudad || !escLlegada || !escSalida)
          ? []
          : [{
              idCiudad: Number(escCiudad),
              llegada:  toIso(escLlegada),
              salida:   toIso(escSalida),
            }],
      };

      await axios.post("/api/v1/vuelos", payload);
      setOk(true);
      setTimeout(() => nav("/admin", { replace: true }), 900);
    } catch (e2) {
      setErr(e2?.response?.data?.error || "No se pudo crear el vuelo");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="container pag-admin">
        <div className="card">
          <div className="skl title" />
          <div className="skl row" />
          <div className="skl row" />
        </div>
      </div>
    );
  }

  return (
    <div className="container pag-admin">
      <div className="card">
        <div className="perfil__head">
          <div>
            <h2>Crear vuelo</h2>
            <p className="subtitle">
              Configura un nuevo vuelo. <Link to="/admin">Volver al panel</Link>
            </p>
          </div>
        </div>

        <form onSubmit={submit} className="form">
          <div className="grid-3">
            <div>
              <label className="label">Código</label>
              <input className="input" value={codigo} onChange={(e)=>setCodigo(e.target.value)} placeholder="ej: AR1234" />
            </div>

            <div>
              <label className="label">Ruta</label>
              <select className="input" value={idRuta} onChange={(e)=>setIdRuta(e.target.value)}>
                <option value="">-- Selecciona una ruta --</option>
                {rutas.map(r => (
                  <option key={r.idRuta} value={r.idRuta}>
                    {r.ciudadOrigen} → {r.ciudadDestino} {r.activa ? "" : "(inactiva)"}
                  </option>
                ))}
              </select>
            </div>

            <div />
          </div>

          <div className="grid-3">
            <div>
              <label className="label">Salida</label>
              <input className="input" type="datetime-local" value={salida} onChange={(e)=>setSalida(e.target.value)} />
            </div>
            <div>
              <label className="label">Llegada</label>
              <input className="input" type="datetime-local" value={llegada} onChange={(e)=>setLlegada(e.target.value)} />
            </div>
            <div />
          </div>

          <hr className="sep" />

          <h3 className="h3">Clases</h3>
          <div className="grid-3">
            {clases.map(c => (
              <div key={c.idClase} className={`clase ${!c.enabled ? "is-off" : ""}`}>
                <label className="label">
                  <input type="checkbox" checked={c.enabled} onChange={()=>toggleClase(c.idClase)} />
                  <span style={{marginLeft:8}}>{c.nombre}</span>
                </label>
                <div className="grid-2" style={{marginTop:6}}>
                  <input
                    className="input"
                    type="number"
                    min="0"
                    placeholder="Cupo total"
                    value={c.cupoTotal}
                    onChange={(e)=>setClaseField(c.idClase, "cupoTotal", e.target.value)}
                    disabled={!c.enabled}
                  />
                  <input
                    className="input"
                    type="number"
                    min="0"
                    step="0.01"
                    placeholder="Precio"
                    value={c.precio}
                    onChange={(e)=>setClaseField(c.idClase, "precio", e.target.value)}
                    disabled={!c.enabled}
                  />
                </div>
              </div>
            ))}
          </div>

          <hr className="sep" />

          <div className="escala-row">
            <label className="label" style={{display:"flex", gap:8, alignItems:"center"}}>
              <input type="checkbox" checked={usarEscala} onChange={(e)=>setUsarEscala(e.target.checked)} />
              Añadir 1 escala (opcional)
            </label>
          </div>

          {usarEscala && (
            <div className="grid-3">
              <div>
                <label className="label">Ciudad</label>
                <select className="input" value={escCiudad} onChange={(e)=>setEscCiudad(e.target.value)}>
                  <option value="">-- Selecciona --</option>
                  {ciudades.map(c => (
                    <option key={c.idCiudad} value={c.idCiudad}>
                      {c.pais} — {c.nombre}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="label">Llegada</label>
                <input className="input" type="datetime-local" value={escLlegada} onChange={(e)=>setEscLlegada(e.target.value)} />
              </div>
              <div>
                <label className="label">Salida</label>
                <input className="input" type="datetime-local" value={escSalida} onChange={(e)=>setEscSalida(e.target.value)} />
              </div>
            </div>
          )}

          {err && <div className="error" role="alert">{err}</div>}
          {ok  && <div className="ok">¡Vuelo creado!</div>}

          <div className="actions">
            <button className="btn btn-secondary" disabled={!canSubmit || saving}>
              {saving ? "Guardando..." : "Crear vuelo"}
            </button>
            <Link to="/admin" className="btn btn-ghost">Cancelar</Link>
          </div>
        </form>
      </div>
    </div>
  );
}
