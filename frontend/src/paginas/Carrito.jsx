import React, { useEffect, useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { comprasApi } from "../api/compras";
import { getUser } from "../lib/auth";
import CartItem from "./CartItem";
import "../styles/carrito.css";

const fmtMoney = (n) =>
  Number(n || 0).toLocaleString("es-GT", { style: "currency", currency: "GTQ" });

export default function Carrito() {
  const nav = useNavigate();
  const u = getUser();

  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  const load = async () => {
    try {
      setLoading(true);
      setErr("");
      const { data } = await comprasApi.getCart();
      setData(data);
    } catch (e) {
      setErr(e?.response?.data?.error || e.message || "No se pudo cargar el carrito");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!u) { nav("/login"); return; }
    load();
  }, []);

  const onQty = async (idItem, qty) => {
    await comprasApi.updateItem(idItem, Math.max(1, Number(qty) || 1), { syncPareja: true });
    await load();
  };

  const onRemove = async (idItem) => {
    await comprasApi.removeItem(idItem, { syncPareja: true });
    await load();
  };

  const onCheckout = () => {
    if (!data || !data.items || data.items.length === 0) {
      alert("Tu carrito está vacío.");
      return;
    }
    nav("/compras/checkout");
  };

  if (!u) return null;

  return (
    <div className="container carrito">
      <div className="c-head">
        <h1>Tu carrito</h1>
        <Link className="btn btn-primary" to="/vuelos">Seguir comprando</Link>
      </div>

      <div className="card c-card">
        {loading ? (
          <>
            <div className="skl title" />
            <div className="skl row" />
            <div className="skl row" />
          </>
        ) : err ? (
          <div className="error">{err}</div>
        ) : !data || (data.items || []).length === 0 ? (
          <div className="empty">Tu carrito está vacío.</div>
        ) : (
          <>
            <ul className="c-list">
              {data.items.map((it) => (
                <CartItem
                  key={it.idItem}
                  item={it}
                  onChangeQty={(q) => onQty(it.idItem, q)}
                  onRemove={() => onRemove(it.idItem)}
                />
              ))}
            </ul>
            <div className="c-footer">
              <div className="c-total">
                Total: <strong>{fmtMoney(data.total)}</strong>
              </div>
              <button className="btn btn-primary" onClick={onCheckout}>
                Ir a pagar
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
