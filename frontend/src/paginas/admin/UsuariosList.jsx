import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import axios from "../../lib/axios";
import "./admin.css";

const rolLabel = (idRol) =>
  idRol === 1 ? "administrador" :
  idRol === 2 ? "webservice" :
  "visitante";

export default function UsuariosList(){
  const [q, setQ] = useState("");
  const [loading, setLoading] = useState(true);
  const [items, setItems] = useState([]);
  const [err, setErr] = useState("");

  const fetchData = async () => {
    setErr(""); setLoading(true);
    try {
      const { data } = await axios.get("/api/admin/usuarios", { params: { q, offset: 0, limit: 50 } });
      setItems(data.items || []);
    } catch (e) {
      setErr(e?.response?.data?.error || "No se pudo cargar la lista");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  const submitSearch = (e) => { e.preventDefault(); fetchData(); };

  return (
    <div className="container admin">
      <div className="admin__header">
        <h2>Usuarios</h2>
        <form onSubmit={submitSearch} className="admin__search">
          <input className="input" placeholder="Buscar por email/nombre" value={q} onChange={(e)=>setQ(e.target.value)} />
          <button className="btn">Buscar</button>
        </form>
      </div>

      {loading ? <p>Cargando…</p> : err ? <div className="error">{err}</div> : (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Correo</th>
                <th>Nombre</th>
                <th>Rol</th>
                <th>Habilitado</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {items.map(it => (
                <tr key={it.idUsuario}>
                  <td>{it.idUsuario}</td>
                  <td>{it.email}</td>
                  <td>{it.nombres} {it.apellidos}</td>
                  <td>{it.rolNombre || rolLabel(Number(it.idRol))} ({it.idRol})</td>
                  <td>{Number(it.habilitado) === 1 ? "Sí" : "No"}</td>
                  <td><Link to={`/admin/usuarios/${it.idUsuario}`}>Editar</Link></td>
                </tr>
              ))}
              {items.length === 0 && (
                <tr><td colSpan={6} style={{textAlign:"center"}}>Sin resultados</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
