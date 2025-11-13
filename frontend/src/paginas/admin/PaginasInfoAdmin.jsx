import { useEffect, useState } from "react";
import paginasInfoApi from "../../api/paginasInfo";

export default function PaginasInfoAdmin() {
  const [paginas, setPaginas] = useState([]);
  const [idPaginaSel, setIdPaginaSel] = useState("");

  const [nombrePagina, setNombrePagina] = useState("");
  const [titulo, setTitulo] = useState("");
  const [descripcion, setDescripcion] = useState("");

  const [puntos, setPuntos] = useState([]);
  const [seccionesEliminadas, setSeccionesEliminadas] = useState([]);

  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [msg, setMsg] = useState("");

  const movePunto = (from, to) => {
    setPuntos((list) => {
      if (to < 0 || to >= list.length) return list;
      const copy = [...list];
      const [item] = copy.splice(from, 1);
      copy.splice(to, 0, item);
      return copy;
    });
  };

  const moveMedia = (idxSeccion, from, to) => {
    setPuntos((list) =>
      list.map((p, i) => {
        if (i !== idxSeccion) return p;
        const media = p.media || [];
        if (to < 0 || to >= media.length) return p;
        const copy = [...media];
        const [item] = copy.splice(from, 1);
        copy.splice(to, 0, item);
        return { ...p, media: copy };
      })
    );
  };

  useEffect(() => {
    (async () => {
      try {
        const { data } = await paginasInfoApi.list();
        setPaginas(Array.isArray(data) ? data : []);
      } catch (e) {
        console.error(e);
        setError("No se pudo cargar la lista de páginas.");
      }
    })();
  }, []);

  const limpiarPagina = () => {
    setNombrePagina("");
    setTitulo("");
    setDescripcion("");
    setPuntos([]);
    setSeccionesEliminadas([]);
    setError("");
    setMsg("");
  };

  const hidratarDesdeDTO = (dto) => {
    setNombrePagina(dto?.nombrePagina || "");
    setTitulo(dto?.titulo || "");
    setDescripcion(dto?.descripcion || "");

    const secs = Array.isArray(dto?.secciones) ? dto.secciones : [];

    setPuntos(
      secs.map((s) => ({
        idSeccion: s.idSeccion,
        idPagina: s.idPagina,
        nombreSeccion: s.nombreSeccion || "",
        descripcion: s.descripcion || "",
        orden: s.orden ?? 0,
        media: Array.isArray(s.media)
          ? s.media.map((m) => ({
              idMedia: m.idMedia,
              idSeccion: m.idSeccion,
              tipoMedia: m.tipoMedia || "IMAGEN",
              url: m.url || "",
              orden: m.orden ?? 0,
            }))
          : [],
      }))
    );
    setSeccionesEliminadas([]);
  };

  const onChangePagina = async (e) => {
    const val = e.target.value;
    setIdPaginaSel(val);
    limpiarPagina();

    if (!val) return;

    try {
      setLoading(true);
      const { data } = await paginasInfoApi.getAdmin(val);
      hidratarDesdeDTO(data);
    } catch (e) {
      console.error(e);
      setError("No se pudo cargar la página seleccionada.");
    } finally {
      setLoading(false);
    }
  };

  const addPunto = () => {
    setPuntos((list) => [
      ...list,
      {
        idSeccion: null,
        nombreSeccion: "",
        descripcion: "",
        orden: list.length + 1,
        media: [],
      },
    ]);
  };

  const updatePuntoField = (idx, key, value) => {
    setPuntos((list) =>
      list.map((p, i) => (i === idx ? { ...p, [key]: value } : p))
    );
  };

  const removePunto = (idx) => {
    setPuntos((list) => {
      const toRemove = list[idx];
      if (toRemove?.idSeccion) {
        setSeccionesEliminadas((prev) => [...prev, toRemove.idSeccion]);
      }
      return list.filter((_, i) => i !== idx);
    });
  };

  const addMedia = (idxSeccion) => {
    setPuntos((list) =>
      list.map((p, i) =>
        i === idxSeccion
          ? {
              ...p,
              media: [
                ...(p.media || []),
                {
                  idMedia: null,
                  tipoMedia: "IMAGEN",
                  url: "",
                  orden: (p.media?.length || 0) + 1,
                },
              ],
            }
          : p
      )
    );
  };

  const updateMediaField = (idxSeccion, idxMedia, key, value) => {
    setPuntos((list) =>
      list.map((p, i) => {
        if (i !== idxSeccion) return p;
        const media = (p.media || []).map((m, j) =>
          j === idxMedia ? { ...m, [key]: value } : m
        );
        return { ...p, media };
      })
    );
  };

  const removeMedia = (idxSeccion, idxMedia) => {
    setPuntos((list) =>
      list.map((p, i) => {
        if (i !== idxSeccion) return p;
        const media = (p.media || []).filter((_, j) => j !== idxMedia);
        return { ...p, media };
      })
    );
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    if (!idPaginaSel) {
      setError("Selecciona una página primero.");
      return;
    }
    if (!titulo.trim()) {
      setError("El título es obligatorio.");
      return;
    }

    setError("");
    setMsg("");
    setSaving(true);

    try {
  
      for (const idSec of seccionesEliminadas) {
        try {
          await paginasInfoApi.deleteSection(idSec);
        } catch (err) {
          console.warn("No se pudo eliminar sección", idSec, err);
        }
      }

  
      await paginasInfoApi.saveAdmin(idPaginaSel, {
        nombrePagina: nombrePagina || undefined,
        titulo: titulo.trim(),
        descripcion: descripcion.trim(),
      });

  
      const seccionesConId = [];

      for (const p of puntos) {
        const payload = {
          nombreSeccion: (p.nombreSeccion || "").trim(),
          descripcion: (p.descripcion || "").trim(),
      
        };

        if (!payload.nombreSeccion) continue;

        let idSeccionReal = p.idSeccion;
        if (p.idSeccion) {
          await paginasInfoApi.updateSection(p.idSeccion, payload);
        } else {
          const { data } = await paginasInfoApi.createSection(
            idPaginaSel,
            payload
          );
          idSeccionReal = data?.idSeccion;
        }

        if (!idSeccionReal) continue;

        seccionesConId.push({
          ...p,
          idSeccion: idSeccionReal,
        });
      }

  
      await paginasInfoApi.reorderSections(
        idPaginaSel,
        seccionesConId.map((sec, idx) => ({
          idSeccion: sec.idSeccion,
          orden: idx + 1,
        }))
      );

  
      for (const sec of seccionesConId) {
        const idSeccion = sec.idSeccion;
        const mediaList = (sec.media || []).map((m, idx) => ({
          ...m,
          orden: idx + 1,
        }));

    
        for (const m of mediaList) {
          if (m.idMedia) {
            try {
              await paginasInfoApi.deleteMedia(m.idMedia);
            } catch (err) {
              console.warn("No se pudo borrar media", m.idMedia, err);
            }
          }
        }

    
        for (const m of mediaList) {
          const url = (m.url || "").trim();
          if (!url) continue;

          await paginasInfoApi.createMedia(idSeccion, {
            tipoMedia: m.tipoMedia || "IMAGEN",
            url,
            orden: m.orden,
          });
        }
      }

  
      const { data } = await paginasInfoApi.getAdmin(idPaginaSel);
      hidratarDesdeDTO(data);

      setMsg("Cambios guardados correctamente.");
      setTimeout(() => setMsg(""), 3000);
    } catch (e) {
      console.error(e);
      setError(
        e?.response?.data?.error || "No se pudieron guardar los cambios."
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="container" style={{ padding: "32px 0 40px" }}>
      <h1>Administrar páginas informativas</h1>
      <p className="subtitle" style={{ marginBottom: 24 }}>
        Aquí puedes editar el título, la descripción y los puntos (secciones) de
        cada página como Check-in, Abordaje, etc.
      </p>

      <form onSubmit={onSubmit} className="form">
        <div style={{ marginBottom: 16 }}>
          <label className="label">Página</label>
          <select
            className="input"
            value={idPaginaSel}
            onChange={onChangePagina}
          >
            <option value="">-- Selecciona una página --</option>
            {paginas.map((p) => (
              <option key={p.idPagina} value={p.idPagina}>
                {p.nombrePagina || p.titulo || `#${p.idPagina}`}
              </option>
            ))}
          </select>
        </div>

        <div style={{ marginBottom: 16 }}>
          <label className="label">Título</label>
          <input
            className="input"
            value={titulo}
            onChange={(e) => setTitulo(e.target.value)}
            disabled={!idPaginaSel || loading}
          />
        </div>

        <div style={{ marginBottom: 24 }}>
          <label className="label">Descripción</label>
          <textarea
            className="input"
            rows={3}
            value={descripcion}
            onChange={(e) => setDescripcion(e.target.value)}
            disabled={!idPaginaSel || loading}
          />
        </div>

        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: 8,
          }}
        >
          <h2
            style={{
              fontSize: "1.1rem",
              fontWeight: 600,
              margin: 0,
            }}
          >
            Puntos / Secciones
          </h2>

          <button
            type="button"
            className="btn btn-secondary"
            onClick={addPunto}
            disabled={!idPaginaSel || loading}
          >
            Añadir punto
          </button>
        </div>

        {loading && <p className="hint">Cargando contenido de la página…</p>}

        {!loading && puntos.length === 0 && (
          <div className="hint" style={{ marginBottom: 12 }}>
            Aún no hay puntos configurados. Usa &quot;Añadir punto&quot; para
            crear uno.
          </div>
        )}

        {!loading &&
          puntos.map((p, idx) => (
            <div
              key={p.idSeccion || `nuevo-${idx}`}
              className="card"
              style={{ padding: 16, marginBottom: 16 }}
            >
    
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  marginBottom: 8,
                  gap: 8,
                }}
              >
                <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <strong>Punto {idx + 1}</strong>
                  <span style={{ fontSize: 12, color: "#777" }}>
                    (orden {idx + 1})
                  </span>
                  <div style={{ display: "flex", gap: 4 }}>
                    <button
                      type="button"
                      className="btn btn-sm"
                      onClick={() => movePunto(idx, idx - 1)}
                      disabled={idx === 0}
                      title="Subir"
                    >
                      ▲
                    </button>
                    <button
                      type="button"
                      className="btn btn-sm"
                      onClick={() => movePunto(idx, idx + 1)}
                      disabled={idx === puntos.length - 1}
                      title="Bajar"
                    >
                      ▼
                    </button>
                  </div>
                </div>
                <button
                  type="button"
                  className="link"
                  onClick={() => removePunto(idx)}
                >
                  Eliminar punto
                </button>
              </div>

              <div style={{ marginBottom: 8 }}>
                <label className="label">Título del punto</label>
                <input
                  className="input"
                  value={p.nombreSeccion}
                  onChange={(e) =>
                    updatePuntoField(idx, "nombreSeccion", e.target.value)
                  }
                />
              </div>

              <div style={{ marginBottom: 12 }}>
                <label className="label">Descripción del punto</label>
                <textarea
                  className="input"
                  rows={2}
                  value={p.descripcion}
                  onChange={(e) =>
                    updatePuntoField(idx, "descripcion", e.target.value)
                  }
                />
              </div>

    
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  marginBottom: 8,
                  marginTop: 4,
                }}
              >
                <h3
                  style={{
                    fontSize: "1rem",
                    fontWeight: 600,
                    margin: 0,
                  }}
                >
                  Imágenes / Media
                </h3>
                <button
                  type="button"
                  className="btn btn-sm btn-secondary"
                  onClick={() => addMedia(idx)}
                >
                  Añadir imagen
                </button>
              </div>

              {(p.media || []).length === 0 && (
                <p className="hint">
                  Aún no hay imágenes en este punto. Usa &quot;Añadir
                  imagen&quot; para agregar una.
                </p>
              )}

              {(p.media || []).map((m, j) => (
                <div
                  key={m.idMedia || `m-${j}`}
                  style={{
                    border: "1px dashed #ddd",
                    borderRadius: 8,
                    padding: 8,
                    marginBottom: 8,
                  }}
                >
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      marginBottom: 4,
                      gap: 8,
                    }}
                  >
                    <div
                      style={{
                        display: "flex",
                        alignItems: "center",
                        gap: 8,
                      }}
                    >
                      <span style={{ fontSize: 13, fontWeight: 600 }}>
                        Imagen {j + 1} (orden {j + 1})
                      </span>
                      <div style={{ display: "flex", gap: 4 }}>
                        <button
                          type="button"
                          className="btn btn-sm"
                          onClick={() => moveMedia(idx, j, j - 1)}
                          disabled={j === 0}
                          title="Subir imagen"
                        >
                          ▲
                        </button>
                        <button
                          type="button"
                          className="btn btn-sm"
                          onClick={() => moveMedia(idx, j, j + 1)}
                          disabled={j === (p.media || []).length - 1}
                          title="Bajar imagen"
                        >
                          ▼
                        </button>
                      </div>
                    </div>
                    <button
                      type="button"
                      className="link"
                      onClick={() => removeMedia(idx, j)}
                    >
                      Quitar
                    </button>
                  </div>
                  <div
                    style={{
                      display: "grid",
                      gridTemplateColumns: "130px 1fr",
                      gap: 8,
                    }}
                  >
                    <div>
                      <label className="label">Tipo</label>
                      <select
                        className="input"
                        value={m.tipoMedia}
                        onChange={(e) =>
                          updateMediaField(
                            idx,
                            j,
                            "tipoMedia",
                            e.target.value
                          )
                        }
                      >
                        <option value="IMAGEN">Imagen</option>
                        <option value="INFOGRAFIA">Infografía</option>
                      </select>
                    </div>
                    <div>
                      <label className="label">URL</label>
                      <input
                        className="input"
                        placeholder="https://..."
                        value={m.url}
                        onChange={(e) =>
                          updateMediaField(idx, j, "url", e.target.value)
                        }
                      />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ))}

        {error && (
          <div className="error" style={{ marginTop: 12 }} role="alert">
            {error}
          </div>
        )}
        {msg && (
          <div className="success" style={{ marginTop: 12 }} role="status">
            {msg}
          </div>
        )}

        <div className="actions" style={{ marginTop: 16 }}>
          <button
            className="btn btn-secondary"
            type="submit"
            disabled={!idPaginaSel || saving}
          >
            {saving ? "Guardando..." : "Guardar cambios"}
          </button>
        </div>
      </form>
    </div>
  );
}
