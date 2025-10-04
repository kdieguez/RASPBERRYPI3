import { useEffect, useState } from "react";
import { configApi } from "../../api/config";
import "./config-sitio.css"; 

export default function ConfigSitio() {

  const [headerForm, setHeaderForm] = useState({
    nombre: "",
    logo: "",
  });

  const [footerForm, setFooterForm] = useState({
    telefono: "",
    direccion: "",
    copyRight: "",
  });

  const [loading, setLoading] = useState(true);
  const [savingHeader, setSavingHeader] = useState(false);
  const [savingFooter, setSavingFooter] = useState(false);

  const notify = (msg, type = "info") => {
    if (type === "error") console.error(msg);
    else console.log(msg);
    if (window?.toast?.[type]) window.toast[type](msg);
  };

  useEffect(() => {
    (async () => {
      try {
        const h = await configApi.getHeader();
        setHeaderForm({
          nombre: h?.nombre ?? "",
          logo: h?.logo ?? "",
        });
      } catch (e) {
        notify("No se pudo cargar el header", "error");
      }

      try {
        const f = await configApi.getFooter();
        const norm = Object.fromEntries(
          Object.entries(f || {}).map(([k, v]) => [String(k).toLowerCase(), v])
        );
        setFooterForm((prev) => ({
          ...prev,
          telefono: norm.telefono ?? prev.telefono,
          direccion: norm.direccion ?? prev.direccion,
          copyRight:
            norm.copyright ?? norm.copyright ?? prev.copyRight, 
        }));
      } catch (e) {
        notify("No se pudo cargar el footer", "error");
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const saveHeader = async () => {
    setSavingHeader(true);
    try {
      await configApi.updateHeader(headerForm);
      notify("Header guardado", "success");
    } catch (e) {
      notify("No se pudo guardar el header", "error");
    } finally {
      setSavingHeader(false);
    }
  };

  const saveFooter = async () => {
    setSavingFooter(true);
    try {
      const payload = {
        ...footerForm,
        copyright: footerForm.copyRight,
      };
      delete payload.copyRight;
      await configApi.updateFooter(payload); 
      notify("Footer guardado", "success");
    } catch (e) {
      notify("No se pudo guardar el footer", "error");
    } finally {
      setSavingFooter(false);
    }
  };

  if (loading) {
    return <div className="container" style={{ padding: 20 }}>Cargando…</div>;
  }

  return (
    <div className="container" style={{ padding: "20px 0 40px" }}>
      <h2 style={{ marginBottom: 10 }}>Configuración del sitio</h2>
      <p style={{ color: "#64748b", marginTop: 0 }}>
        Administra los textos e imágenes del encabezado y pie de página.
      </p>

      <section className="panel">
        <div className="panel__head">
          <h3>Header</h3>
          <button
            className="btn btn-secondary"
            onClick={saveHeader}
            disabled={savingHeader}
          >
            {savingHeader ? "Guardando…" : "Guardar Header"}
          </button>
        </div>

        <div className="grid-2">
          <div className="field">
            <label>Nombre del sitio</label>
            <input
              className="input"
              value={headerForm.nombre}
              onChange={(e) =>
                setHeaderForm((f) => ({ ...f, nombre: e.target.value }))
              }
              placeholder="Ej. Aerolínea GuateFly"
            />
          </div>

          <div className="field">
            <label>URL del logo</label>
            <input
              className="input"
              value={headerForm.logo}
              onChange={(e) =>
                setHeaderForm((f) => ({ ...f, logo: e.target.value }))
              }
              placeholder="https://…/logo.png"
            />
          </div>
        </div>

        <div className="preview">
          <span>Vista previa:</span>
          <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
            {headerForm.logo ? (
              <img
                src={headerForm.logo}
                alt="logo"
                width={44}
                height={44}
                style={{ objectFit: "contain" }}
                onError={(e) => (e.currentTarget.style.display = "none")}
              />
            ) : (
              <div
                style={{
                  width: 44,
                  height: 44,
                  borderRadius: 10,
                  display: "grid",
                  placeItems: "center",
                  background: "#1E93AB",
                  color: "#fff",
                  fontWeight: 700,
                }}
              >
                ✈
              </div>
            )}
            <strong>{headerForm.nombre || "Nombre de muestra"}</strong>
          </div>
        </div>
      </section>

      <section className="panel">
        <div className="panel__head">
          <h3>Footer</h3>
          <button
            className="btn"
            onClick={saveFooter}
            disabled={savingFooter}
          >
            {savingFooter ? "Guardando…" : "Guardar Footer"}
          </button>
        </div>

        <div className="grid-2">
          <div className="field">
            <label>Teléfono</label>
            <input
              className="input"
              value={footerForm.telefono}
              onChange={(e) =>
                setFooterForm((f) => ({ ...f, telefono: e.target.value }))
              }
              placeholder="+502 …"
            />
          </div>

          <div className="field">
            <label>Dirección</label>
            <input
              className="input"
              value={footerForm.direccion}
              onChange={(e) =>
                setFooterForm((f) => ({ ...f, direccion: e.target.value }))
              }
              placeholder="Carretera a El Salvador Km. 30"
            />
          </div>


          <div className="field" style={{ gridColumn: "1 / -1" }}>
            <label>Copyright</label>
            <input
              className="input"
              value={footerForm.copyRight}
              onChange={(e) =>
                setFooterForm((f) => ({ ...f, copyRight: e.target.value }))
              }
              placeholder="© 2025 Aerolíneas. Todos los derechos reservados."
            />
          </div>
        </div>
      </section>
    </div>
  );
}
