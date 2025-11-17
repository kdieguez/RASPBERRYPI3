import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { agenciasApi } from "../../api/agencias";

export default function AgenciasList() {
  const nav = useNavigate();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");
  const [soloHab, setSoloHab] = useState(false);

  const load = async () => {
    try {
      setLoading(true);
      setErr("");
      const { data } = await agenciasApi.list(soloHab);
      setRows(Array.isArray(data) ? data : []);
    } catch (e) {
      setErr(e?.response?.data?.error || "No se pudieron cargar las agencias");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [soloHab]);

  return (
    <div className="container">
      <h2>Agencias vinculadas</h2>
      <div style={{display:"flex", gap:8, alignItems:"center", marginBottom:12}}>
        <label><input type="checkbox" checked={soloHab} onChange={e=>setSoloHab(e.target.checked)} /> Solo habilitadas</label>
        <button className="btn primary" onClick={()=>nav("/admin/agencias/nueva")}>Nueva agencia</button>
      </div>
      {err && <p className="error">{err}</p>}
      {loading ? <p>Cargando…</p> : (
        <table className="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Nombre</th>
              <th>API</th>
              <th>Usuario WS</th>
              <th>Habilitada</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {rows.map(r=>(
              <tr key={r.idAgencia}>
                <td>{r.idAgencia}</td>
                <td>{r.nombre}</td>
                <td>{r.apiUrl}</td>
                <td>{r.idUsuarioWs ?? "—"}</td>
                <td>{r.habilitado ? "Sí" : "No"}</td>
                <td>
                  <Link className="btn" to={`/admin/agencias/${encodeURIComponent(r.idAgencia)}`}>Editar</Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}




