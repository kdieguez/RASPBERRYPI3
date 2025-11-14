import { useEffect, useState } from "react";
import { paisesApi, ciudadesApi } from "../../api/adminCatalogos";
import AdminNav from "./AdminNav";

export default function AdminCiudades() {
  const [paises, setPaises] = useState([]);
  const [idPais, setIdPais] = useState("");
  const [ciudades, setCiudades] = useState([]);

  const [nombre, setNombre] = useState("");
  const [weatherQuery, setWeatherQuery] = useState("");

  const [err, setErr] = useState("");
  const [ok, setOk] = useState(false);

        useEffect(() => {
    (async () => {
      try {
        const { data } = await paisesApi.list();
        setPaises(data || []);
      } catch (e) {
        setErr("No se pudieron cargar países");
      }
    })();
  }, []);

        useEffect(() => {
    if (!idPais) {
      setCiudades([]);
      return;
    }
    (async () => {
      try {
        const { data } = await ciudadesApi.list(idPais);
        setCiudades(data || []);
      } catch (e) {
        setErr("No se pudieron cargar ciudades del país");
      }
    })();
  }, [idPais]);

        const crear = async (e) => {
    e.preventDefault();
    setErr("");
    setOk(false);

    try {
      await ciudadesApi.create(
        Number(idPais),
        nombre.trim(),
        weatherQuery.trim() || null
      );

      const { data } = await ciudadesApi.list(idPais);
      setCiudades(data || []);
      setOk(true);
      setNombre("");
      setWeatherQuery("");
    } catch (e2) {
      setErr(e2?.response?.data?.error || "No se pudo crear la ciudad");
    }
  };

  const toggle = async (idCiudad) => {
    setErr("");
    setOk(false);
    try {
      await ciudadesApi.toggle(idCiudad);
      const { data } = await ciudadesApi.list(idPais);
      setCiudades(data || []);
      setOk(true);
    } catch (e2) {
      setErr(e2?.response?.data?.error || "No se pudo cambiar el estado");
    }
  };

  return (
    <>
      <AdminNav />
      <div className="container">
        <div className="card">
          <h2>Ciudades</h2>

          <div className="grid-2">
            <div>
              <label className="label">País</label>
              <select
                className="input"
                value={idPais}
                onChange={(e) => setIdPais(e.target.value)}
              >
                <option value="">-- Selecciona un país --</option>
                {paises.map((p) => (
                  <option key={p.idPais} value={p.idPais}>
                    {p.nombre}
                  </option>
                ))}
              </select>
            </div>
            <div />
          </div>

          <form className="form" onSubmit={crear}>
            <label className="label">Nueva ciudad</label>
            <input
              className="input"
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
            />

            <label className="label" style={{ marginTop: 8 }}>
              Consulta clima (OpenWeather)
              <span className="hint" style={{ fontSize: 12, display: "block" }}>
                Opcional. Ejemplos:{" "}
                <code>Guatemala City,GT</code> &nbsp;|&nbsp;
                <code>Panama City,PA</code>
              </span>
            </label>
            <input
              className="input"
              value={weatherQuery}
              onChange={(e) => setWeatherQuery(e.target.value)}
              placeholder="Guatemala City,GT"
            />

            {err && <div className="error" style={{ marginTop: 8 }}>{err}</div>}
            {ok && <div className="ok" style={{ marginTop: 8 }}>¡Actualizado!</div>}

            <button
              className="btn btn-secondary"
              style={{ marginTop: 12 }}
              disabled={!idPais || !nombre.trim()}
            >
              Guardar
            </button>
          </form>

          <hr className="sep" />
          <h3>Listado</h3>
          <ul className="adm__list">
            {ciudades.map((c) => (
              <li key={c.idCiudad} className="adm__row">
                <span>
                  {c.nombre}{" "}
                  <small className="adm__muted">({c.pais})</small>
                </span>
                <button
                  className={
                    "adm__pill " + (c.activo ? "is-on" : "is-off")
                  }
                  onClick={() => toggle(c.idCiudad)}
                >
                  {c.activo ? "Activa" : "Inactiva"}
                </button>
              </li>
            ))}
            {!idPais && (
              <li className="adm__muted">
                Selecciona un país para ver sus ciudades.
              </li>
            )}
          </ul>
        </div>
      </div>
    </>
  );
}
