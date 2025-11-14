
import { useEffect, useState } from "react";
import { tipsApi, noticiasApi } from "../../api/contenidoHome";

export default function ContenidoHomeAdmin() {
  const [activeTab, setActiveTab] = useState("tips"); 

  
  const [tips, setTips] = useState([]);
  const [tipSeleccionado, setTipSeleccionado] = useState(null);
  const [tipTitulo, setTipTitulo] = useState("");
  const [tipDescripcion, setTipDescripcion] = useState("");
  const [tipOrden, setTipOrden] = useState("");
  const [loadingTips, setLoadingTips] = useState(false);

  
  const [noticias, setNoticias] = useState([]);
  const [noticiaSeleccionada, setNoticiaSeleccionada] = useState(null);
  const [notTitulo, setNotTitulo] = useState("");
  const [notContenido, setNotContenido] = useState("");
  const [notOrden, setNotOrden] = useState("");
  const [notUrlImagen, setNotUrlImagen] = useState("");
  const [loadingNoticias, setLoadingNoticias] = useState(false);

  
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [msg, setMsg] = useState("");

  
  
  
  useEffect(() => {
    cargarTips();
    cargarNoticias();
  }, []);

  const cargarTips = async () => {
    try {
      setLoadingTips(true);
      setError("");
      const { data } = await tipsApi.listAdmin();
      const arr = Array.isArray(data) ? [...data] : [];
      
      arr.sort(
        (a, b) => (a.orden ?? 999999) - (b.orden ?? 999999)
      );
      setTips(arr);
    } catch (e) {
      console.error(e);
      setError("No se pudieron cargar los tips de viaje.");
    } finally {
      setLoadingTips(false);
    }
  };

  const cargarNoticias = async () => {
    try {
      setLoadingNoticias(true);
      setError("");
      const { data } = await noticiasApi.listAdmin();
      const arr = Array.isArray(data) ? [...data] : [];
      arr.sort(
        (a, b) => (a.orden ?? 999999) - (b.orden ?? 999999)
      );
      setNoticias(arr);
    } catch (e) {
      console.error(e);
      setError("No se pudieron cargar las noticias.");
    } finally {
      setLoadingNoticias(false);
    }
  };

  
  
  
  const limpiarTipForm = () => {
    setTipSeleccionado(null);
    setTipTitulo("");
    setTipDescripcion("");
    setTipOrden("");
  };

  const onSelectTip = (tip) => {
    setTipSeleccionado(tip);
    setTipTitulo(tip.titulo || "");
    setTipDescripcion(tip.descripcion || "");
    setTipOrden(tip.orden != null ? String(tip.orden) : "");
  };

  const onNuevoTip = () => {
    
    const nextOrden = (tips?.length || 0) + 1;
    setTipSeleccionado(null);
    setTipTitulo("");
    setTipDescripcion("");
    setTipOrden(String(nextOrden));
  };

  const onGuardarTip = async (e) => {
    e.preventDefault();
    if (!tipTitulo.trim() || !tipDescripcion.trim()) {
      setError("Título y descripción del tip son obligatorios.");
      return;
    }

    let ordenNum =
      tipOrden === "" || tipOrden == null ? null : Number(tipOrden);

    
    if (ordenNum == null || Number.isNaN(ordenNum) || ordenNum <= 0) {
      ordenNum = tips.length + 1;
    }

    const payload = {
      titulo: tipTitulo.trim(),
      descripcion: tipDescripcion.trim(),
      orden: ordenNum, 
    };

    setSaving(true);
    setError("");
    setMsg("");
    try {
      if (tipSeleccionado && tipSeleccionado.idTip) {
        await tipsApi.update(tipSeleccionado.idTip, payload);
      } else {
        await tipsApi.create(payload);
      }
      await cargarTips();
      limpiarTipForm();
      setMsg("Tip guardado correctamente.");
      setTimeout(() => setMsg(""), 3000);
    } catch (e) {
      console.error(e);
      setError(
        e?.response?.data?.error || "No se pudo guardar el tip de viaje."
      );
    } finally {
      setSaving(false);
    }
  };

  const onEliminarTip = async () => {
    if (!tipSeleccionado || !tipSeleccionado.idTip) return;
    if (
      !window.confirm(
        "¿Eliminar este tip? Esta acción no se puede deshacer."
      )
    )
      return;

    setSaving(true);
    setError("");
    setMsg("");
    try {
      await tipsApi.remove(tipSeleccionado.idTip);
      await cargarTips();
      limpiarTipForm();
      setMsg("Tip eliminado correctamente.");
      setTimeout(() => setMsg(""), 3000);
    } catch (e) {
      console.error(e);
      setError(
        e?.response?.data?.error || "No se pudo eliminar el tip de viaje."
      );
    } finally {
      setSaving(false);
    }
  };

  
  
  
  const limpiarNoticiaForm = () => {
    setNoticiaSeleccionada(null);
    setNotTitulo("");
    setNotContenido("");
    setNotOrden("");
    setNotUrlImagen("");
  };

  const onSelectNoticia = (n) => {
    setNoticiaSeleccionada(n);
    setNotTitulo(n.titulo || "");
    setNotContenido(n.contenido || "");
    setNotOrden(n.orden != null ? String(n.orden) : "");
    setNotUrlImagen(n.urlImagen || "");
  };

  const onNuevaNoticia = () => {
    const nextOrden = (noticias?.length || 0) + 1;
    setNoticiaSeleccionada(null);
    setNotTitulo("");
    setNotContenido("");
    setNotUrlImagen("");
    setNotOrden(String(nextOrden));
  };

  const onGuardarNoticia = async (e) => {
    e.preventDefault();
    if (!notTitulo.trim() || !notContenido.trim()) {
      setError("Título y contenido de la noticia son obligatorios.");
      return;
    }

    let ordenNum =
      notOrden === "" || notOrden == null ? null : Number(notOrden);

    if (ordenNum == null || Number.isNaN(ordenNum) || ordenNum <= 0) {
      ordenNum = noticias.length + 1;
    }

    const payload = {
      titulo: notTitulo.trim(),
      contenido: notContenido.trim(),
      orden: ordenNum,
      urlImagen: notUrlImagen?.trim() || null,
    };

    setSaving(true);
    setError("");
    setMsg("");
    try {
      if (noticiaSeleccionada && noticiaSeleccionada.idNoticia) {
        await noticiasApi.update(noticiaSeleccionada.idNoticia, payload);
      } else {
        await noticiasApi.create(payload);
      }
      await cargarNoticias();
      limpiarNoticiaForm();
      setMsg("Noticia guardada correctamente.");
      setTimeout(() => setMsg(""), 3000);
    } catch (e) {
      console.error(e);
      setError(
        e?.response?.data?.error || "No se pudo guardar la noticia."
      );
    } finally {
      setSaving(false);
    }
  };

  const onEliminarNoticia = async () => {
    if (!noticiaSeleccionada || !noticiaSeleccionada.idNoticia) return;
    if (
      !window.confirm(
        "¿Eliminar esta noticia? Esta acción no se puede deshacer."
      )
    )
      return;

    setSaving(true);
    setError("");
    setMsg("");
    try {
      await noticiasApi.remove(noticiaSeleccionada.idNoticia);
      await cargarNoticias();
      limpiarNoticiaForm();
      setMsg("Noticia eliminada correctamente.");
      setTimeout(() => setMsg(""), 3000);
    } catch (e) {
      console.error(e);
      setError(
        e?.response?.data?.error || "No se pudo eliminar la noticia."
      );
    } finally {
      setSaving(false);
    }
  };

  
  
  

  return (
    <div className="container" style={{ padding: "32px 0 40px" }}>
      <h1>Contenido del Home</h1>
      <p className="subtitle" style={{ marginBottom: 24 }}>
        Administra los <strong>tips de viaje</strong> y las{" "}
        <strong>noticias</strong> que se mostrarán en la página de inicio.
      </p>

            <div
        style={{
          display: "flex",
          gap: 8,
          marginBottom: 24,
          borderBottom: "1px solid #ddd",
          paddingBottom: 8,
        }}
      >
        <button
          type="button"
          className={`btn btn-sm ${
            activeTab === "tips" ? "btn-secondary" : ""
          }`}
          onClick={() => setActiveTab("tips")}
        >
          Tips de viaje
        </button>
        <button
          type="button"
          className={`btn btn-sm ${
            activeTab === "noticias" ? "btn-secondary" : ""
          }`}
          onClick={() => setActiveTab("noticias")}
        >
          Noticias
        </button>
      </div>

      {error && (
        <div className="error" style={{ marginBottom: 16 }} role="alert">
          {error}
        </div>
      )}
      {msg && (
        <div className="success" style={{ marginBottom: 16 }} role="status">
          {msg}
        </div>
      )}

      {activeTab === "tips" && (
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "minmax(0, 1.1fr) minmax(0, 1.2fr)",
            gap: 24,
          }}
        >
                    <div>
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                marginBottom: 8,
              }}
            >
              <h2 style={{ fontSize: "1.1rem", margin: 0 }}>Tips de viaje</h2>
              <button
                type="button"
                className="btn btn-sm btn-secondary"
                onClick={onNuevoTip}
              >
                Nuevo tip
              </button>
            </div>

            {loadingTips && <p>Cargando tips…</p>}

            {!loadingTips && tips.length === 0 && (
              <p className="hint">
                Aún no hay tips configurados. Crea el primero con &quot;Nuevo
                tip&quot;.
              </p>
            )}

            {!loadingTips && tips.length > 0 && (
              <ul style={{ listStyle: "none", padding: 0, marginTop: 8 }}>
                {tips.map((t) => (
                  <li
                    key={t.idTip}
                    onClick={() => onSelectTip(t)}
                    style={{
                      border: "1px solid #eee",
                      borderRadius: 8,
                      padding: 10,
                      marginBottom: 8,
                      cursor: "pointer",
                      backgroundColor:
                        tipSeleccionado && tipSeleccionado.idTip === t.idTip
                          ? "#f0f4ff"
                          : "white",
                    }}
                  >
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        gap: 8,
                      }}
                    >
                      <div>
                        <strong>{t.titulo}</strong>
                        <div style={{ fontSize: 12, color: "#666" }}>
                          Orden: {t.orden ?? "—"}
                        </div>
                      </div>
                    </div>
                    {t.descripcion && (
                      <p
                        style={{
                          fontSize: 13,
                          color: "#555",
                          marginTop: 4,
                          maxHeight: 48,
                          overflow: "hidden",
                        }}
                      >
                        {t.descripcion}
                      </p>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </div>

                    <form onSubmit={onGuardarTip} className="form">
            <h3 style={{ marginTop: 0, marginBottom: 12 }}>
              {tipSeleccionado ? "Editar tip" : "Nuevo tip"}
            </h3>

            <div style={{ marginBottom: 8 }}>
              <label className="label">Título</label>
              <input
                className="input"
                value={tipTitulo}
                onChange={(e) => setTipTitulo(e.target.value)}
              />
            </div>

            <div style={{ marginBottom: 8 }}>
              <label className="label">Descripción</label>
              <textarea
                className="input"
                rows={4}
                value={tipDescripcion}
                onChange={(e) => setTipDescripcion(e.target.value)}
              />
            </div>

            <div style={{ marginBottom: 8 }}>
              <label className="label">
                Orden{" "}
                <span style={{ fontWeight: 400 }}>(si lo dejas vacío, se usa el siguiente)</span>
              </label>
              <input
                className="input"
                type="number"
                min="1"
                value={tipOrden}
                onChange={(e) => setTipOrden(e.target.value)}
              />
            </div>

            <div
              className="actions"
              style={{ marginTop: 12, display: "flex", gap: 8 }}
            >
              <button
                className="btn btn-secondary"
                type="submit"
                disabled={saving}
              >
                {saving ? "Guardando…" : "Guardar tip"}
              </button>
              {tipSeleccionado && (
                <button
                  type="button"
                  className="btn"
                  style={{ backgroundColor: "#eee", color: "#333" }}
                  onClick={onEliminarTip}
                  disabled={saving}
                >
                  Eliminar
                </button>
              )}
            </div>
          </form>
        </div>
      )}

      {activeTab === "noticias" && (
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "minmax(0, 1.1fr) minmax(0, 1.2fr)",
            gap: 24,
          }}
        >
                    <div>
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                marginBottom: 8,
              }}
            >
              <h2 style={{ fontSize: "1.1rem", margin: 0 }}>Noticias</h2>
              <button
                type="button"
                className="btn btn-sm btn-secondary"
                onClick={onNuevaNoticia}
              >
                Nueva noticia
              </button>
            </div>

            {loadingNoticias && <p>Cargando noticias…</p>}

            {!loadingNoticias && noticias.length === 0 && (
              <p className="hint">
                Aún no hay noticias configuradas. Crea la primera con
                &quot;Nueva noticia&quot;.
              </p>
            )}

            {!loadingNoticias && noticias.length > 0 && (
              <ul style={{ listStyle: "none", padding: 0, marginTop: 8 }}>
                {noticias.map((n) => (
                  <li
                    key={n.idNoticia}
                    onClick={() => onSelectNoticia(n)}
                    style={{
                      border: "1px solid #eee",
                      borderRadius: 8,
                      padding: 10,
                      marginBottom: 8,
                      cursor: "pointer",
                      backgroundColor:
                        noticiaSeleccionada &&
                        noticiaSeleccionada.idNoticia === n.idNoticia
                          ? "#f0f4ff"
                          : "white",
                    }}
                  >
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "space-between",
                        gap: 8,
                      }}
                    >
                      <div>
                        <strong>{n.titulo}</strong>
                        <div style={{ fontSize: 12, color: "#666" }}>
                          Orden: {n.orden ?? "—"}
                        </div>
                        {n.fechaPublicacion && (
                          <div style={{ fontSize: 11, color: "#999" }}>
                            Publicada:{" "}
                            {String(n.fechaPublicacion).replace("T", " ")}
                          </div>
                        )}
                      </div>
                    </div>
                    {n.contenido && (
                      <p
                        style={{
                          fontSize: 13,
                          color: "#555",
                          marginTop: 4,
                          maxHeight: 48,
                          overflow: "hidden",
                        }}
                      >
                        {n.contenido}
                      </p>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </div>

                    <form onSubmit={onGuardarNoticia} className="form">
            <h3 style={{ marginTop: 0, marginBottom: 12 }}>
              {noticiaSeleccionada ? "Editar noticia" : "Nueva noticia"}
            </h3>

            <div style={{ marginBottom: 8 }}>
              <label className="label">Título</label>
              <input
                className="input"
                value={notTitulo}
                onChange={(e) => setNotTitulo(e.target.value)}
              />
            </div>

            <div style={{ marginBottom: 8 }}>
              <label className="label">Contenido</label>
              <textarea
                className="input"
                rows={6}
                value={notContenido}
                onChange={(e) => setNotContenido(e.target.value)}
              />
            </div>

            <div style={{ marginBottom: 8 }}>
              <label className="label">
                Orden{" "}
                <span style={{ fontWeight: 400 }}>(si lo dejas vacío, se usa el siguiente)</span>
              </label>
              <input
                className="input"
                type="number"
                min="1"
                value={notOrden}
                onChange={(e) => setNotOrden(e.target.value)}
              />
            </div>

            <div style={{ marginBottom: 8 }}>
              <label className="label">
                URL de imagen <span style={{ fontWeight: 400 }}>(opcional)</span>
              </label>
              <input
                className="input"
                placeholder="https://..."
                value={notUrlImagen}
                onChange={(e) => setNotUrlImagen(e.target.value)}
              />
            </div>

            <div
              className="actions"
              style={{ marginTop: 12, display: "flex", gap: 8 }}
            >
              <button
                className="btn btn-secondary"
                type="submit"
                disabled={saving}
              >
                {saving ? "Guardando…" : "Guardar noticia"}
              </button>
              {noticiaSeleccionada && (
                <button
                  type="button"
                  className="btn"
                  style={{ backgroundColor: "#eee", color: "#333" }}
                  onClick={onEliminarNoticia}
                  disabled={saving}
                >
                  Eliminar
                </button>
              )}
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
