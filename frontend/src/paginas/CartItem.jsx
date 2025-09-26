import "../styles/cartItem.css";
import RouteLine from "../components/RouteLine";

const fmt = (v) => new Date(v).toLocaleString("es-GT", { dateStyle: "medium", timeStyle: "short" });
const money = (n) => Number(n || 0).toLocaleString("es-GT", { style: "currency", currency: "GTQ" });

export default function CartItem({ item, onChangeQty, onRemove }) {
  return (
    <li className="citem">
      <div className="citem__main">
        <div className="citem__title">
          <span className="badge">{item.codigoVuelo}</span>
          <span className="pill">{item.clase}</span>
        </div>

        <RouteLine item={item} />

        <div className="citem__times">
          <div><span className="label">Salida</span> {fmt(item.fechaSalida)}</div>
          <div><span className="label">Llegada</span> {fmt(item.fechaLlegada)}</div>
        </div>
      </div>

      <div className="citem__right">
        <div className="citem__price">
          <span className="label">Precio</span>
          <strong>{money(item.precioUnitario)}</strong>
        </div>
        <div className="citem__qty">
          <span className="label">Cantidad</span>
          <input
            className="input"
            type="number"
            min={1}
            max={9}
            value={item.cantidad}
            onChange={(e) => onChangeQty(Math.max(1, Math.min(9, Number(e.target.value) || 1)))}
          />
        </div>
        <div className="citem__subtotal">
          <span className="label">Subtotal</span>
          <strong>{money(item.subtotal)}</strong>
        </div>
        <button className="btn btn-danger" onClick={onRemove}>Quitar</button>
      </div>
    </li>
  );
}
