import { useEffect, useState } from "react";
import { useParams, Link, useNavigate } from "react-router-dom";
import { vuelosApi, rutasApi, clasesApi } from "../../api/adminCatalogos";

const toLocalInput = (s) => (!s ? "" : s.replace(" ", "T").slice(0, 16));
const ensureSeconds = (s) => (!s ? null : (s.length === 16 ? `${s}:00` : s));

export default function VueloEdit() {
  const { id } = useParams();
  const nav = useNavigate();

  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  const [codigo, setCodigo] = useState("");
  const [idRuta, setIdRuta] = useState("");
  const [salida, setSalida] = useState("");   
  const [llegada, setLlegada] = useState("");
  const [activo, setActivo] = useState(true);

  const [clases, setClases] = useState([]);  
  const [rutas, setRutas] = useState([]);

  useEffect(() => {
    (async () => {
      try {
        setLoading(true);
        setErr("");

        const [{ data: v }, { data: rutasRes }, { data: clasesRes }] = await Promise.all([
          vuelosApi.getAdmin(id), 
          rutasApi.list(),
          clasesApi.list(),
        ]);

        setCodigo(v.codigo || "");
        setIdRuta(String(v.idRuta || ""));
        setSalida(toLocalInput(v.fechaSalida));
        setLlegada(toLocalInput(v.fechaLlegada));
        setActivo(!!v.activo);

        const base = (clasesRes || []).map(c => ({
          idClase: c.idClase,
          nombre:  c.nombre,
          cupoTotal: "",
          precio: "",
          enabled: false,
        }));
        (v.clases || []).forEach(vc => {
          const i = base.findIndex(b => Number(b.idClase) === Number(vc.idClase));
          if (i >= 0) base[i] = { ...base[i], enabled: true, cupoTotal: vc.cupoTotal, precio: vc.precio };
        });

        setClases(base);
        setRutas(rutasRes || []);
      } catch (e) {
        setErr(e?.response?.data?.error || "No se pudo cargar el vuelo");
      } finally {
        setLoading(false);
      }
    })();
  }, [id]);

  const onClase = (idClase, key, val) =>
    setClases(list =>
      list.map(c => c.idClase === idClase ? { ...c, [key]: val.replace(/[^0-9.]/g, "") } : c)
    );

  const toggleClase = (idClase) =>
    setClases(list =>
      list.map(c => c.idClase === idClase ? { ...c, enabled: !c.enabled } : c)
    );

  const payloadClases = () => {
    const on = clases.filter(c => c.enabled);
    if (!on.length) return null;
    const out = [];
    for (const c of on) {
      const cupo = Number(c.cupoTotal);
      const precio = Number(c.precio);
      if (!Number.isFinite(cupo) || cupo <= 0) return null;
      if (!Number.isFinite(precio) || precio <= 0) return null;
      out.push({ idClase: c.idClase, cupoTotal: cupo, precio });
    }
    return out;
  };

  const canSave =
    codigo.trim() &&
    idRuta &&
    salida &&
    llegada &&
    payloadClases() &&
    new Date(salida) < new Date(llegada);

  const save = async (e) => {
    e.preventDefault();
    setErr("");

    const clasesOut = payloadClases();
    if (!clasesOut) { setErr("Revisa las clases (cupo y precio)"); return; }

    try {
      await vuelosApi.updateAdmin(id, {
        codigo: codigo.trim().toUpperCase(),
        idRuta: Number(idRuta),
        fechaSalida: ensureSeconds(salida),
        fechaLlegada: ensureSeconds(llegada),
        activo,
        clases: clasesOut,
        escalas: [], 
      });
      nav("/admin/vuelos");
    } catch (e2) {
      setErr(e2?.response?.data?.error || "No se pudo guardar");
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
            <p className="subtitle"><Link to="/admin/vuelos">Volver a la lista</Link></p>
          </div>
        </div>

        {loading ? (
          <>
            <div className="skl title" />
            <div className="skl row" />
            <div className="skl row" />
          </>
        ) : (
          <form className="form" onSubmit={save}>
            <div className="grid-2">
              <div>
                <label className="label">Código</label>
                <input className="input" value={codigo} onChange={e=>setCodigo(e.target.value)} />
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
                <input type="datetime-local" className="input" value={salida} onChange={e=>setSalida(e.target.value)} />
              </div>
              <div>
                <label className="label">Llegada</label>
                <input type="datetime-local" className="input" value={llegada} onChange={e=>setLlegada(e.target.value)} />
              </div>
            </div>

            <label className="check" style={{ marginTop: 8, marginBottom: 8 }}>
              <input type="checkbox" checked={activo} onChange={e=>setActivo(e.target.checked)} />
              <span>Activo</span>
            </label>

            <h3 className="block-title">Clases</h3>
            <div className="grid-3">
              {clases.map(c => (
                <div key={c.idClase} className={"clase " + (c.enabled ? "on" : "off")}>
                  <label className="check">
                    <input type="checkbox" checked={c.enabled} onChange={() => toggleClase(c.idClase)} />
                    <span>{c.nombre}</span>
                  </label>
                  <div className="grid-2">
                    <input
                      className="input"
                      placeholder="Cupo total"
                      inputMode="numeric"
                      value={c.cupoTotal}
                      onChange={e => onClase(c.idClase, "cupoTotal", e.target.value)}
                    />
                    <input
                      className="input"
                      placeholder="Precio"
                      inputMode="decimal"
                      value={c.precio}
                      onChange={e => onClase(c.idClase, "precio", e.target.value)}
                    />
                  </div>
                </div>
              ))}
            </div>

            {err && <div className="error" role="alert">{err}</div>}

            <div className="actions">
              <button className="btn btn-secondary" disabled={!canSave}>Guardar cambios</button>
              <Link to="/admin/vuelos" className="btn" style={{ marginLeft: 8 }}>Cancelar</Link>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
