import { useEffect, useState } from "react";
import { paisesApi } from "../../api/adminCatalogos";
import AdminNav from "./AdminNav";

export default function AdminPaises(){
  const [items, setItems] = useState([]);
  const [nombre, setNombre] = useState("");
  const [err, setErr] = useState(""); const [ok, setOk] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const { data } = await paisesApi.list();
        setItems(data || []);
      } catch (e) {
        setErr(e?.response?.data?.error || "No se pudieron cargar los países");
      } finally { setLoading(false); }
    })();
  }, []);

  const crear = async (e) => {
    e.preventDefault(); setErr(""); setOk(false);
    try {
      await paisesApi.create(nombre.trim());
      const { data } = await paisesApi.list();
      setItems(data || []);
      setOk(true); setNombre("");
    } catch (e2) {
      setErr(e2?.response?.data?.error || "No se pudo crear el país");
    }
  };

  return (
    <>
      <AdminNav/>
      <div className="container">
        <div className="card">
          <h2>Países</h2>
          <form className="form" onSubmit={crear}>
            <label className="label">Nombre del país</label>
            <input className="input" value={nombre} onChange={e=>setNombre(e.target.value)} />
            {err && <div className="error">{err}</div>}
            {ok  && <div className="ok">¡País creado!</div>}
            <button className="btn btn-secondary" disabled={!nombre.trim()}>Guardar</button>
          </form>

          <hr className="sep" />
          <h3>Listado</h3>
          {loading ? <div className="skl row" /> : (
            <ul className="adm__list">
              {items.map(p => <li key={p.idPais}>{p.nombre}</li>)}
            </ul>
          )}
        </div>
      </div>
    </>
  );
}
