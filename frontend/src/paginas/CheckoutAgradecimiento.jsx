import { Link, useParams } from "react-router-dom";
import { useEffect, useState } from "react";
 "../styles/checkout.css";
import { comprasApi } from "../api/compras";

export default function CheckoutAgradecimiento() {
  const { id } = useParams();
  const [loading, setLoading] = useState(true);
  const [codigo, setCodigo] = useState("");
  const [err, setErr] = useState("");
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        setLoading(true);
        setErr("");
        const { data } = await comprasApi.getReserva(id);
        setCodigo(data?.codigo || "");
      } catch (e) {
        setErr(e?.response?.data?.error || e.message || "No se pudo cargar la reserva");
      } finally {
        setLoading(false);
      }
    })();
  }, [id]);

  const onDownload = async () => {
    try {
      setDownloading(true);
      const { data } = await comprasApi.downloadBoleto(id);
      const blob = new Blob([data], { type: "application/pdf" });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `boleto-${(codigo || `reserva-${id}`)}.pdf`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (e) {
      alert(e?.response?.data?.error || e.message || "No se pudo descargar el boleto");
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div className="container checkout">
      <div className="card chk-card">
        <div className="chk-icon" aria-hidden="true">✔</div>
        <h1>¡Reserva confirmada!</h1>

        <p className="chk-subtitle">
          {loading ? "Cargando código…" : err ? (
            <span className="error">{err}</span>
          ) : (
            <>
              Tu código de reserva es{" "}
              <strong className="pill">{
                codigo ? codigo : `#${id}`
              }</strong>.
            </>
          )}
        </p>

        <div className="chk-actions" style={{ gap: 8, flexWrap: "wrap" }}>
          <button className="btn btn-primary" onClick={onDownload} disabled={downloading || loading || !!err}>
            {downloading ? "Generando PDF..." : "Descargar PDF"}
          </button>
          <Link className="btn btn-primary" to="/vuelos">Seguir comprando</Link>
          <Link className="btn" to="/">Ir al inicio</Link>
        </div>

        <p className="hint">
          También te enviamos un correo con el resumen de tu reserva.
        </p>
      </div>
    </div>
  );
}
