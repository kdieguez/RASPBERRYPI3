import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { clasesApi } from "../api/adminCatalogos";
import "../styles/cajaCompras.css";

const todayISO = () => new Date().toISOString().slice(0, 10);

export default function CajaCompras() {
  const nav = useNavigate();

  const [tab, setTab] = useState("rt");
  const [origen, setOrigen] = useState("");
  const [destino, setDestino] = useState("");
  const [fechaIda, setFechaIda] = useState(todayISO());
  const [fechaVuelta, setFechaVuelta] = useState("");
  const [pax, setPax] = useState(1);

  const [clases, setClases] = useState([]);
  const [idClase, setIdClase] = useState("");

  useEffect(() => {
    (async () => {
      try {
        const { data } = await clasesApi.list();
        const arr = Array.isArray(data) ? data : [];
        setClases(arr);
        const turista = arr.find((c) =>
          String(c.nombre).toLowerCase().includes("tur")
        );
        setIdClase(String(turista?.idClase ?? arr?.[0]?.idClase ?? ""));
      } catch {
        setClases([]);
        setIdClase("");
      }
    })();
  }, []);

  const onTab = (t) => {
    setTab(t);
    if (t === "ow") setFechaVuelta("");
  };

  const canSubmit = useMemo(() => {
    if (!origen.trim() || !destino.trim() || !fechaIda) return false;
    if (tab === "rt" && (!fechaVuelta || fechaVuelta < fechaIda)) return false;
    if (!idClase) return false;
    return pax > 0 && pax <= 9;
  }, [tab, origen, destino, fechaIda, fechaVuelta, idClase, pax]);

  const swap = () => {
    setOrigen(destino);
    setDestino(origen);
  };

  const submit = (e) => {
    e.preventDefault();
    const q = new URLSearchParams();
    const add = (k, v) => {
      if (v !== undefined && v !== null && String(v).trim() !== "")
        q.set(k, String(v).trim());
    };

    add("trip", tab === "rt" ? "round" : "one");

    add("origen", origen);
    add("destino", destino);

    add("fsd", fechaIda);
    add("fsh", fechaIda);

    if (tab === "rt" && fechaVuelta) {
      add("frd", fechaVuelta);
      add("frh", fechaVuelta);
    }

    add("clase", idClase);
    add("pax", pax);

    nav(`/vuelos?${q.toString()}`);
  };

  return (
    <div className="bkbox">
      <div className="bkbox__tabs" role="tablist" aria-label="Tipo de viaje">
        <button
          role="tab"
          aria-selected={tab === "rt"}
          className={`bkbox__tab ${tab === "rt" ? "is-active" : ""}`}
          onClick={() => onTab("rt")}
          type="button"
        >
          Viaje de ida y vuelta
        </button>
        <button
          role="tab"
          aria-selected={tab === "ow"}
          className={`bkbox__tab ${tab === "ow" ? "is-active" : ""}`}
          onClick={() => onTab("ow")}
          type="button"
        >
          Viaje sencillo (solo ida)
        </button>
      </div>

      <form className="bkbox__form" onSubmit={submit}>
        <div className="bkbox__row">
          <div className="bkbox__col">
            <label className="label">Desde</label>
            <input
              className="input"
              placeholder="Ciudad o país"
              value={origen}
              onChange={(e) => setOrigen(e.target.value)}
            />
          </div>
          <div className="bkbox__swap">
            <button
              type="button"
              className="swapbtn"
              onClick={swap}
              title="Intercambiar"
            >
              ⇄
            </button>
          </div>
          <div className="bkbox__col">
            <label className="label">Hacia</label>
            <input
              className="input"
              placeholder="Destino"
              value={destino}
              onChange={(e) => setDestino(e.target.value)}
            />
          </div>
        </div>

        <div className={`bkbox__row ${tab === "ow" ? "bkbox__row--single" : ""}`}>
          <div className="bkbox__col">
            <label className="label">Fecha de ida</label>
            <input
              className="input"
              type="date"
              min={todayISO()}
              value={fechaIda}
              onChange={(e) => setFechaIda(e.target.value)}
            />
          </div>

          {tab === "rt" && (
            <div className="bkbox__col">
              <label className="label">Fecha de vuelta</label>
              <input
                className="input"
                type="date"
                min={fechaIda || todayISO()}
                value={fechaVuelta}
                onChange={(e) => setFechaVuelta(e.target.value)}
              />
            </div>
          )}
        </div>

        <div className="bkbox__row">
          <div className="bkbox__col">
            <label className="label">Clase</label>
            <select
              className="input"
              value={idClase}
              onChange={(e) => setIdClase(e.target.value)}
            >
              {clases.map((c) => (
                <option key={c.idClase} value={c.idClase}>
                  {c.nombre}
                </option>
              ))}
              {clases.length === 0 && <option value="">—</option>}
            </select>
          </div>
          <div className="bkbox__col">
            <label className="label">Viajeros</label>
            <select
              className="input"
              value={pax}
              onChange={(e) => setPax(Number(e.target.value))}
            >
              {Array.from({ length: 9 }, (_, i) => i + 1).map((n) => (
                <option key={n} value={n}>
                  {n} {n === 1 ? "" : "Viajeros"}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="bkbox__actions">
          <button className="btn btn-secondary" type="submit" disabled={!canSubmit}>
            Buscar vuelos
          </button>
        </div>
      </form>
    </div>
  );
}
