import { useEffect, useState } from "react";
import { paisesApi, ciudadesApi, rutasApi } from "../../api/adminCatalogos";
import AdminNav from "./AdminNav";

export default function AdminRutas(){
  const [paises, setPaises] = useState([]);

  const [paisO, setPaisO] = useState("");
  const [paisD, setPaisD] = useState("");
  const [ciudadesO, setCiudadesO] = useState([]);
  const [ciudadesD, setCiudadesD] = useState([]);
  const [idCiudadO, setIdCiudadO] = useState("");
  const [idCiudadD, setIdCiudadD] = useState("");

  const [err, setErr] = useState(""); const [ok, setOk] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const { data } = await paisesApi.list();
        setPaises(data || []);
      } catch { setErr("No se pudieron cargar países"); }
    })();
  }, []);

  useEffect(() => {
    (async () => {
      setIdCiudadO(""); setCiudadesO([]);
      if (!paisO) return;
      try { const { data } = await ciudadesApi.list(paisO); setCiudadesO(data || []); }
      catch { setErr("No se pudieron cargar ciudades de origen"); }
    })();
  }, [paisO]);

  useEffect(() => {
    (async () => {
      setIdCiudadD(""); setCiudadesD([]);
      if (!paisD) return;
      try { const { data } = await ciudadesApi.list(paisD); setCiudadesD(data || []); }
      catch { setErr("No se pudieron cargar ciudades de destino"); }
    })();
  }, [paisD]);

  const crear = async (e) => {
    e.preventDefault(); setErr(""); setOk(false);
    try {
      await rutasApi.create(Number(idCiudadO), Number(idCiudadD));
      setOk(true);
      setPaisO(""); setPaisD(""); setIdCiudadO(""); setIdCiudadD(""); setCiudadesO([]); setCiudadesD([]);
    } catch (e2) {
      setErr(e2?.response?.data?.error || "No se pudo crear la ruta");
    }
  };

  return (
    <>
      <AdminNav/>
      <div className="container">
        <div className="card">
          <h2>Rutas</h2>

          <form className="form" onSubmit={crear}>
            <div className="grid-2">
              <div>
                <label className="label">País origen</label>
                <select className="input" value={paisO} onChange={e=>setPaisO(e.target.value)}>
                  <option value="">-- Selecciona país --</option>
                  {paises.map(p => <option key={p.idPais} value={p.idPais}>{p.nombre}</option>)}
                </select>
                <label className="label" style={{marginTop:8}}>Ciudad origen</label>
                <select className="input" value={idCiudadO} onChange={e=>setIdCiudadO(e.target.value)} disabled={!ciudadesO.length}>
                  <option value="">-- Selecciona ciudad --</option>
                  {ciudadesO.map(c => <option key={c.idCiudad} value={c.idCiudad}>{c.nombre}</option>)}
                </select>
              </div>

              <div>
                <label className="label">País destino</label>
                <select className="input" value={paisD} onChange={e=>setPaisD(e.target.value)}>
                  <option value="">-- Selecciona país --</option>
                  {paises.map(p => <option key={p.idPais} value={p.idPais}>{p.nombre}</option>)}
                </select>
                <label className="label" style={{marginTop:8}}>Ciudad destino</label>
                <select className="input" value={idCiudadD} onChange={e=>setIdCiudadD(e.target.value)} disabled={!ciudadesD.length}>
                  <option value="">-- Selecciona ciudad --</option>
                  {ciudadesD.map(c => <option key={c.idCiudad} value={c.idCiudad}>{c.nombre}</option>)}
                </select>
              </div>
            </div>

            {err && <div className="error">{err}</div>}
            {ok  && <div className="ok">¡Ruta creada!</div>}

            <button className="btn btn-secondary" disabled={!idCiudadO || !idCiudadD || idCiudadO === idCiudadD}>
              Guardar
            </button>
          </form>
        </div>
      </div>
    </>
  );
}
