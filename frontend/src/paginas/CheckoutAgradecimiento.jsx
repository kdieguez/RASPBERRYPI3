import { Link, useParams } from "react-router-dom";
import "../styles/checkout.css";

export default function CheckoutAgradecimiento() {
  const { id } = useParams();
  return (
    <div className="container checkout">
      <div className="card chk-card">
        <div className="chk-icon" aria-hidden="true">✔</div>
        <h1>¡Reserva confirmada!</h1>
        <p className="chk-subtitle">
          Tu código de reserva es <strong>#{id}</strong>.
        </p>
        <div className="chk-actions">
          <Link className="btn btn-primary" to="/vuelos">Seguir comprando</Link>
          <Link className="btn" to="/">Ir al inicio</Link>
        </div>
        <p className="hint">Te enviamos un correo con el resumen de tu reserva.</p>
      </div>
    </div>
  );
}
