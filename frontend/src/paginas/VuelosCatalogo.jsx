import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { vuelosApi, clasesApi } from "../api/adminCatalogos";
import { isLoggedIn, getUser } from "../lib/auth";
import { comprasApi } from "../api/compras";
import "../styles/vuelosCatalogo.css";

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
      const [, Y, Mo, D, hh, mi, se] = m;
      d = new Date(+Y, +Mo - 1, +D, +hh, +mi, se ? +se : 0);
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

const fmtDt = (val) =>
  toDate(val)
    ? toDate(val).toLocaleString("es-MX", {
        dateStyle: "medium",
        timeStyle: "short",
      })
    : "—";

const fmtMoney = (n) =>
  Number(n).toLocaleString("es-GT", {
    style: "currency",
    currency: "GTQ",
    maximumFractionDigits: 2,
  });

const EstadoPill = ({ estado }) => {
  const s = (estado || "").toLowerCase();
  const isCancel = s.includes("cancel");
  return (
    <span className={`pill ${isCancel ? "pill--danger" : "pill--ok"}`}>
      {estado || "Programado"}
    </span>
  );
};

const minPrecio = (vuelo) => {
  if (!vuelo?.clases || vuelo.clases.length === 0) return null;
  let m = Infinity;
  for (const c of vuelo.clases) {
    const p = Number(c.precio);
    if (Number.isFinite(p)) m = Math.min(m, p);
  }
  return m === Infinity ? null : m;
};

const precioDeClase = (vuelo, idClase) => {
  if (!vuelo?.clases) return null;
  const c = vuelo.clases.find((x) => Number(x.idClase) === Number(idClase));
  return c ? Number(c.precio) : null;
};

export default function VuelosCatalogo() {
  const [vuelos, setVuelos] = useState([]);
  const [clases, setClases] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  const user = getUser();
  const isAdmin = !!user && Number(user.idRol) === 1;

  const [pairIds, setPairIds] = useState(new Set());

  const [logged, setLogged] = useState(isLoggedIn());
  useEffect(() => {
    const onChange = () => setLogged(isLoggedIn());
    window.addEventListener("auth:changed", onChange);
    return () => window.removeEventListener("auth:changed", onChange);
  }, []);

  const [sp] = useSearchParams();
  const nav = useNavigate();

  useEffect(() => {
    (async () => {
      try {
        setLoading(true);
        setErr("");
        const [vuelosRes, clasesRes] = await Promise.all([
          (async () => {
            try {
              return (await (isAdmin ? vuelosApi.listAdmin() : vuelosApi.listPublic())).data;
            } catch {
              return (await vuelosApi.listPublic()).data;
            }
          })(),
          (await clasesApi.list()).data,
        ]);

        const src = Array.isArray(vuelosRes) ? vuelosRes : [];
        console.log("Vuelos cargados:", src.length);
        const now = new Date();
      
        const pids = new Set();
        for (const v of src) {
          const a = Number(v.idVuelo);
          const b = v.idVueloPareja ? Number(v.idVueloPareja) : null;
          if (b) { pids.add(a); pids.add(b); }
        }
        setPairIds(pids);
      
        const onlyActiveAndFuture = src
          .filter(
            (v) =>
              v.activo !== false &&
              !String(v.estado || "").toLowerCase().includes("cancel")
          )
          .filter((v) => {
            const fs = toDate(v.fechaSalida);
            return fs && fs >= now;
          });
      
        const baseList = isAdmin ? src : onlyActiveAndFuture;
        console.log("Vuelos activos y futuros:", baseList.length);
        const finalList = foldPairsFromList(baseList);
        console.log("Vuelos después de foldPairs:", finalList.length);

        let vuelosConEscalaLogicos = [];
        try {
          vuelosConEscalaLogicos = crearVuelosConEscalaLogicos(finalList, sp);
        } catch (error) {
          console.error("Error creando vuelos con escala:", error);
          vuelosConEscalaLogicos = [];
        }
        console.log("Vuelos con escala creados:", vuelosConEscalaLogicos.length);
        const allVuelos = [...finalList, ...vuelosConEscalaLogicos];
        console.log("Total de vuelos a mostrar:", allVuelos.length);

        setVuelos(
          allVuelos.map((v) => ({
            ...v,
            _idClaseSel: v?.clases?.[0]?.idClase ?? null,
            _cant: 1,
          }))
        );
        setClases(clasesRes || []);
      } catch (e) {
        setErr(e?.response?.data?.error || "No se pudieron cargar los vuelos");
      } finally {
        setLoading(false);
      }
    })();
  }, [isAdmin, sp]);

  // Función para crear vuelos con escala lógicos basados en vuelos individuales
  const crearVuelosConEscalaLogicos = (vuelos, searchParams) => {
    try {
      const f_origen = (searchParams.get("origen") || "").trim().toLowerCase();
      const f_dest = (searchParams.get("destino") || "").trim().toLowerCase();
      
      if (!f_origen && !f_dest) {
        return [];
      }
      
      if (!vuelos || vuelos.length === 0) {
        return [];
      }
      
      const str = (x) => (x || "").toLowerCase();
      const vuelosConEscala = [];
      const procesados = new Set();
      
      // Buscar combinaciones de vuelos que puedan formar escalas
      for (let i = 0; i < vuelos.length; i++) {
        const vuelo1 = vuelos[i];
        if (!vuelo1 || procesados.has(vuelo1.idVuelo)) continue;
        
        // Verificar si el primer vuelo coincide con el origen buscado
        const origenMatch = !f_origen || 
          str(vuelo1.origen).includes(f_origen) ||
          str(vuelo1.origenPais).includes(f_origen);
        
        if (!origenMatch) continue;
        
        // Buscar un segundo vuelo que salga del destino del primero
        for (let j = 0; j < vuelos.length; j++) {
          const vuelo2 = vuelos[j];
          if (!vuelo2 || i === j || procesados.has(vuelo2.idVuelo)) continue;
          
          // Verificar si el segundo vuelo va al destino buscado
          const destinoMatch = !f_dest ||
            str(vuelo2.destino).includes(f_dest) ||
            str(vuelo2.destinoPais).includes(f_dest);
          
          if (!destinoMatch) continue;
          
          // Verificar que el destino del primer vuelo sea el origen del segundo
          const destino1 = str(vuelo1.destino);
          const origen2 = str(vuelo2.origen);
          
          if (destino1 !== origen2) continue;
          
          // Verificar que la llegada del primer vuelo sea antes o igual a la salida del segundo
          const llegada1 = toDate(vuelo1.fechaLlegada);
          const salida2 = toDate(vuelo2.fechaSalida);
          
          if (!llegada1 || !salida2 || llegada1 > salida2) continue;
          
          // Crear vuelo con escala lógico
          const vueloConEscala = {
            idVuelo: `escala_${vuelo1.idVuelo}_${vuelo2.idVuelo}`,
            codigo: `${vuelo1.codigo} + ${vuelo2.codigo}`,
            idRuta: vuelo1.idRuta,
            origen: vuelo1.origen,
            destino: vuelo2.destino,
            origenPais: vuelo1.origenPais,
            destinoPais: vuelo2.destinoPais,
            fechaSalida: vuelo1.fechaSalida,
            fechaLlegada: vuelo2.fechaLlegada,
            activo: vuelo1.activo && vuelo2.activo,
            estado: "ACTIVO",
            clases: vuelo1.clases || [],
            escalas: [
              {
                ciudad: vuelo1.destino,
                pais: vuelo1.destinoPais,
                llegada: vuelo1.fechaLlegada,
                salida: vuelo2.fechaSalida
              }
            ],
            // Información especial para vuelos con escala
            _esVueloConEscala: true,
            _primerTramo: vuelo1,
            _segundoTramo: vuelo2,
            // Crear una "pareja" virtual para el segundo tramo
            pareja: {
              idVuelo: vuelo2.idVuelo,
              codigo: vuelo2.codigo,
              origen: vuelo2.origen,
              destino: vuelo2.destino,
              fechaSalida: vuelo2.fechaSalida,
              fechaLlegada: vuelo2.fechaLlegada,
              clases: vuelo2.clases || [],
              escalas: []
            }
          };
          
          vuelosConEscala.push(vueloConEscala);
          procesados.add(vuelo1.idVuelo);
          procesados.add(vuelo2.idVuelo);
          break; // Solo una combinación por vuelo
        }
      }
      
      return vuelosConEscala;
    } catch (error) {
      console.error("Error en crearVuelosConEscalaLogicos:", error);
      return [];
    }
  };

  const foldPairsFromList = (list) => {
    const byId = new Map(list.map((v) => [v.idVuelo, { ...v }]));
    const hidden = new Set();

    for (const v of list) {
      if (hidden.has(v.idVuelo)) continue;

      const pairId = v.idVueloPareja;
      if (!pairId) continue;

      const pair = byId.get(pairId);
      if (!pair) continue;

      const fs1 = toDate(v.fechaSalida);
      const fs2 = toDate(pair.fechaSalida);

      let ida = v;
      let regreso = pair;
      if (fs1 && fs2 && fs1 > fs2) {
        ida = pair;
        regreso = v;
      }

      if (hidden.has(regreso.idVuelo)) continue;

      const host = byId.get(ida.idVuelo);
      if (host) {
        host.pareja = {
          idVuelo: regreso.idVuelo,
          codigo: regreso.codigo,
          origen: regreso.origen,
          destino: regreso.destino,
          fechaSalida: regreso.fechaSalida,
          fechaLlegada: regreso.fechaLlegada,
          clases: Array.isArray(regreso.clases) ? regreso.clases : [],
          escalas: Array.isArray(regreso.escalas) ? regreso.escalas : [],
        };
        byId.set(ida.idVuelo, host);
        hidden.add(regreso.idVuelo);
      }
    }

    return Array.from(byId.values()).filter((v) => !hidden.has(v.idVuelo));
  };

  const claseName = (idClase) =>
    clases.find((c) => Number(c.idClase) === Number(idClase))?.nombre ||
    `Clase ${idClase}`;

  const activeFilters = useMemo(() => {
    const get = (k) => sp.get(k);
    const af = [];
    if (get("origen")) af.push({ k: "origen", v: get("origen") });
    if (get("destino")) af.push({ k: "destino", v: get("destino") });
    if (get("fsd") || get("fsh"))
      af.push({ k: "salida", v: `${get("fsd") || "—"} → ${get("fsh") || "—"}` });
    if (get("frd") || get("frh"))
      af.push({ k: "regreso", v: `${get("frd") || "—"} → ${get("frh") || "—"}` });
    if (get("clase")) {
      const nm = claseName(get("clase"));
      af.push({ k: "clase", v: nm });
    }
    if (get("pmin") || get("pmax"))
      af.push({ k: "precio", v: `${get("pmin") || "0"} - ${get("pmax") || "∞"}` });
    if (get("direct") === "1") af.push({ k: "directo", v: "Solo directos" });
    if (get("trip") === "round") af.push({ k: "tipo", v: "Ida y vuelta" });
    if (get("trip") === "one") af.push({ k: "tipo", v: "Solo ida" });
    return af;
  }, [sp, clases]);

  const clearFilters = () => nav("/vuelos", { replace: true });

  const filtered = useMemo(() => {
    const spGet = (k) => (sp.get(k) || "").trim().toLowerCase();

    const trip = sp.get("trip");
    const f_origen = spGet("origen");
    const f_dest = spGet("destino");
    const fsd = sp.get("fsd");
    const fsh = sp.get("fsh");
    const frd = sp.get("frd");
    const frh = sp.get("frh");
    const pmin = Number(sp.get("pmin"));
    const pmax = Number(sp.get("pmax"));
    const directOnly = sp.get("direct") === "1";
    const claseSel = sp.get("clase");

    const inDate = (d, fromStr, toStr) => {
      const dt = toDate(d);
      const from = fromStr ? new Date(`${fromStr}T00:00:00`) : null;
      const to = toStr ? new Date(`${toStr}T23:59:59`) : null;
      if (!dt) return false;
      if (from && dt < from) return false;
      if (to && dt > to) return false;
      return true;
    };

    const str = (x) => (x || "").toLowerCase();

    return (vuelos || []).filter((v) => {
    
      if (trip === "round") {
        if (!v.pareja) return false;
      } else if (trip === "one") {
      
        const idv = Number(v.idVuelo);
        if (v.pareja) return false;
        if (v.idVueloPareja) return false;
        if (pairIds.has(idv)) return false;
      }

      const escalaPaises = Array.isArray(v.escalas)
        ? v.escalas.map((e) => str(e.pais)).join(" ")
        : "";

      if (f_origen) {
        const hit =
          str(v.origen).includes(f_origen) ||
          str(v.origenPais).includes(f_origen) ||
          escalaPaises.includes(f_origen);
        if (!hit) return false;
      }
      if (f_dest) {
        const hit =
          str(v.destino).includes(f_dest) ||
          str(v.destinoPais).includes(f_dest) ||
          escalaPaises.includes(f_dest);
        if (!hit) return false;
      }

      if (claseSel) {
        const tiene =
          Array.isArray(v.clases) &&
          v.clases.some((c) => Number(c.idClase) === Number(claseSel));
        if (!tiene) return false;
      }

      if ((fsd || fsh) && !inDate(v.fechaSalida, fsd, fsh)) return false;

      if ((frd || frh) && (!v.pareja || !inDate(v.pareja.fechaSalida, frd, frh)))
        return false;

      const precioBase = claseSel ? precioDeClase(v, claseSel) : minPrecio(v);
      if (
        Number.isFinite(pmin) &&
        !Number.isNaN(pmin) &&
        pmin > 0 &&
        precioBase !== null &&
        precioBase < pmin
      )
        return false;
      if (
        Number.isFinite(pmax) &&
        !Number.isNaN(pmax) &&
        pmax > 0 &&
        precioBase !== null &&
        precioBase > pmax
      )
        return false;

      if (directOnly && Array.isArray(v.escalas) && v.escalas.length > 0)
        return false;

      return true;
    });
  }, [vuelos, sp, pairIds]);

  const onChangeClase = (idVuelo, idClase) => {
    setVuelos((prev) =>
      prev.map((v) =>
        v.idVuelo === idVuelo ? { ...v, _idClaseSel: Number(idClase) } : v
      )
    );
  };
  const onChangeCant = (idVuelo, cant) => {
    const n = Math.max(1, Math.min(9, Number(cant) || 1));
    setVuelos((prev) =>
      prev.map((v) => (v.idVuelo === idVuelo ? { ...v, _cant: n } : v))
    );
  };

  const comprar = async (vuelo) => {
    const u = getUser();
    if (!u) {
      nav("/login");
      return;
    }
    const s = (vuelo.estado || "").toLowerCase();
    if (s.includes("cancel") || vuelo.activo === false) {
      alert("Este vuelo no está disponible para compra.");
      return;
    }
    const idClase = vuelo._idClaseSel ?? vuelo?.clases?.[0]?.idClase;
    if (!idClase) {
      alert("Selecciona una clase.");
      return;
    }
    try {
      if (vuelo._esVueloConEscala) {
        // Para vuelos con escala lógicos, agregar ambos tramos al carrito
        await comprasApi.addItem(
          {
            idVuelo: Number(vuelo._primerTramo.idVuelo),
            idClase: Number(idClase),
            cantidad: Number(vuelo._cant || 1),
          },
          { pair: false } // Agregar individualmente
        );
        await comprasApi.addItem(
          {
            idVuelo: Number(vuelo._segundoTramo.idVuelo),
            idClase: Number(idClase),
            cantidad: Number(vuelo._cant || 1),
          },
          { pair: false } // Agregar individualmente
        );
      } else {
        // Para vuelos normales, solo agregar el vuelo seleccionado
        await comprasApi.addItem(
          {
            idVuelo: Number(vuelo.idVuelo),
            idClase: Number(idClase),
            cantidad: Number(vuelo._cant || 1),
          },
          { pair: !!vuelo.pareja } // Usar pareja solo si existe
        );
      }
      nav(`/compras/carrito`);
    } catch (e) {
      alert(e?.response?.data?.error || e?.message || "No se pudo agregar al carrito");
    }
  };

  return (
    <div className="container pag-vuelos-list">
      <div className="vlist__head">
        <h1>
          Vuelos{" "}
          {isAdmin && <span className="pill" style={{ marginLeft: 8 }}>Admin</span>}
        </h1>
      </div>

      {activeFilters.length > 0 && (
        <div className="filter-chips" style={{ marginBottom: 10 }}>
          {activeFilters.map((f, i) => (
            <span key={i} className="pill">
              {f.k}: <strong>{f.v}</strong>
            </span>
          ))}
          <button
            className="btn btn-small"
            onClick={clearFilters}
            style={{ marginLeft: 8 }}
          >
            Quitar filtros
          </button>
        </div>
      )}

      <div className="card">
        {loading ? (
          <>
            <div className="skl title" />
            <div className="skl row" />
            <div className="skl row" />
          </>
        ) : err ? (
          <div className="error">{err}</div>
        ) : filtered.length === 0 ? (
          <div className="empty">No hay vuelos que coincidan con el filtro.</div>
        ) : (
          <ul className="vlist">
            {filtered.map((v) => (
              <li key={v.idVuelo} className="vitem">
                <div className="vitem__main">
                  <div className="vitem__route">
                    <div className="vitem__code">
                      <span className="badge">{v.codigo}</span>
                      <EstadoPill estado={v.estado} />
                      {v._esVueloConEscala && (
                        <span className="pill pill--escala">Con Escala</span>
                      )}
                      {isAdmin &&
                        (v.activo === false ? (
                          <span className="pill">Inactivo</span>
                        ) : (
                          <span className="pill pill--ok">Activo</span>
                        ))}
                    </div>

                    <div className="vitem__city">
                      <strong>
                        {v.origen && v.destino
                          ? `${v.origen} → ${v.destino}`
                          : `Ruta #${v.idRuta}`}
                      </strong>
                      {(v.origenPais || v.destinoPais) && (
                        <div className="label" style={{ marginTop: 2 }}>
                          {v.origenPais || "—"} → {v.destinoPais || "—"}
                        </div>
                      )}
                    </div>

                    <div className="vitem__times">
                      <div>
                        <span className="label">Salida</span>
                        <span>{fmtDt(v.fechaSalida)}</span>
                      </div>
                      <div>
                        <span className="label">Llegada</span>
                        <span>{fmtDt(v.fechaLlegada)}</span>
                      </div>
                    </div>
                  </div>

                  {Array.isArray(v.escalas) && v.escalas.length > 0 && (
                    <div className="vitem__escalas">
                      {v.escalas.map((e, i) => (
                        <div key={i} className="esc">
                          <span className="label">
                            {v._esVueloConEscala ? "Escala en" : "Escala"}
                          </span>
                          <span>
                            {e.ciudad} ({e.pais}) — {fmtDt(e.llegada)} → {fmtDt(e.salida)}
                          </span>
                        </div>
                      ))}
                    </div>
                  )}

                  {v.pareja && (
                    <div className="vitem__escalas">
                      <div className="esc">
                        <span className="label">
                          {v._esVueloConEscala ? "Segundo tramo" : "Regreso"}
                        </span>
                        <span>
                          <strong>
                            <Link to={`/vuelos/${v.pareja.idVuelo}`}>{v.pareja.codigo}</Link>
                          </strong>{" "}
                          {v.pareja.origen && v.pareja.destino
                            ? `${v.pareja.origen} → ${v.pareja.destino}`
                            : ""}
                          {" — "}
                          {fmtDt(v.pareja.fechaSalida)} → {fmtDt(v.pareja.fechaLlegada)}
                        </span>
                      </div>
                    </div>
                  )}

                  {v.pareja?.clases?.length > 0 && (
                    <div className="vitem__classes" style={{ marginTop: 6 }}>
                      {v.pareja.clases.map((c, i) => (
                        <div key={i} className="cl">
                          <div className="cl__name">{claseName(c.idClase)}</div>
                          <div className="cl__meta">
                            <span className="label">Cupo</span>
                            <span>{c.cupoTotal}</span>
                            <span className="label">Precio</span>
                            <span>{fmtMoney(c.precio)}</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                <div className="vitem__classes">
                  {v.clases.map((c, i) => (
                    <div key={i} className="cl">
                      <div className="cl__name">{claseName(c.idClase)}</div>
                      <div className="cl__meta">
                        <span className="label">Cupo</span>
                        <span>{c.cupoTotal}</span>
                        <span className="label">Precio</span>
                        <span>{fmtMoney(c.precio)}</span>
                      </div>
                    </div>
                  ))}
                </div>

                <div className="vitem__actions">
                  <Link className="btn" to={`/vuelos/${v.idVuelo}`}>
                    Ver detalles
                  </Link>

                  {logged ? (
                    <>
                      <select
                        className="input"
                        value={v._idClaseSel ?? ""}
                        onChange={(e) => onChangeClase(v.idVuelo, e.target.value)}
                        style={{ marginLeft: 8 }}
                      >
                        {v.clases.map((c) => (
                          <option key={c.idClase} value={c.idClase}>
                            {claseName(c.idClase)}
                          </option>
                        ))}
                      </select>

                      <input
                        className="input"
                        type="number"
                        min={1}
                        max={9}
                        value={v._cant}
                        onChange={(e) => onChangeCant(v.idVuelo, e.target.value)}
                        style={{ width: 64, marginLeft: 8 }}
                      />

                      <button
                        className="btn btn-secondary"
                        style={{ marginLeft: 8 }}
                        onClick={() => comprar(v)}
                        disabled={
                          !v.clases ||
                          v.clases.length === 0 ||
                          (v.estado || "").toLowerCase().includes("cancel") ||
                          v.activo === false
                        }
                      >
                        Añadir a carrito
                      </button>

                      {v.pareja && !v._esVueloConEscala && (
                        <button
                          className="btn btn-primary"
                          style={{ marginLeft: 8 }}
                          onClick={async () => {
                            try {
                              // Agregar ambos vuelos (ida y regreso)
                              await comprasApi.addItem(
                                {
                                  idVuelo: Number(v.idVuelo),
                                  idClase: Number(v._idClaseSel || v.clases[0].idClase),
                                  cantidad: Number(v._cant || 1),
                                },
                                { pair: true }
                              );
                              nav(`/compras/carrito`);
                            } catch (e) {
                              alert(e?.response?.data?.error || e?.message || "No se pudo agregar al carrito");
                            }
                          }}
                          disabled={
                            !v.clases ||
                            v.clases.length === 0 ||
                            (v.estado || "").toLowerCase().includes("cancel") ||
                            v.activo === false
                          }
                        >
                          Ida y vuelta
                        </button>
                      )}
                    </>
                  ) : (
                    <button
                      className="btn btn-secondary"
                      style={{ marginLeft: 8 }}
                      onClick={() => nav("/login")}
                    >
                      Añadir a carrito
                    </button>
                  )}

                  {isAdmin && (
                    <Link
                      className="btn btn-secondary"
                      to={`/admin/vuelos/${v.idVuelo}`}
                      style={{ marginLeft: 8 }}
                    >
                      Editar
                    </Link>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}
