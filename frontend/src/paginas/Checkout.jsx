import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { comprasApi } from "../api/compras";
import { getUser } from "../lib/auth";
import RouteLine from "../components/RouteLine";
import "../styles/checkout.css";

const money = (n) => Number(n || 0).toLocaleString("es-GT", { style: "currency", currency: "GTQ" });

const luhn = (num) => {
  const s = String(num || "").replace(/\D/g, "");
  if (s.length < 12) return false;
  let sum = 0, alt = false;
  for (let i = s.length - 1; i >= 0; i--) {
    let n = parseInt(s[i], 10);
    if (alt) { n *= 2; if (n > 9) n -= 9; }
    sum += n; alt = !alt;
  }
  return sum % 10 === 0;
};

const formatCard = (val) =>
  String(val || "").replace(/\D/g, "").slice(0,19).replace(/(\d{4})(?=\d)/g,"$1 ");

export default function Checkout() {
  const nav = useNavigate();
  const u = getUser();

  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  const [f, setF] = useState({
    nombre: u?.nombre || "",
    numero: "", expMes: "", expAnio: "", cvv: "",
    direccion: "", ciudad: "", pais: "", zip: "",
  });

  useEffect(() => {
    if (!u) { nav("/login"); return; }
    (async () => {
      try {
        setLoading(true);
        const { data } = await comprasApi.getCart();
        if (!data?.items?.length) { nav("/compras/carrito"); return; }
        setCart(data);
      } catch (e) {
        setErr(e?.response?.data?.error || e.message || "No se pudo cargar el carrito");
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const onChange = (e) => {
    const { name, value } = e.target;
    if (name === "numero") return setF(p => ({ ...p, numero: formatCard(value) }));
    if (name === "expMes")  return setF(p => ({ ...p, expMes:  value.replace(/\D/g,"").slice(0,2) }));
    if (name === "expAnio") return setF(p => ({ ...p, expAnio: value.replace(/\D/g,"").slice(0,4) }));
    if (name === "cvv")     return setF(p => ({ ...p, cvv:     value.replace(/\D/g,"").slice(0,4) }));
    setF(p => ({ ...p, [name]: value }));
  };

  const validar = () => {
    if (!f.nombre.trim()) return "Ingresa el nombre del titular.";
    const clean = f.numero.replace(/\s+/g,"");
    if (!luhn(clean)) return "Número de tarjeta inválido.";
    if (!/^\d{2}$/.test(f.expMes) || +f.expMes < 1 || +f.expMes > 12) return "Mes inválido.";
    if (!/^\d{2,4}$/.test(f.expAnio)) return "Año inválido.";
    const yy = f.expAnio.length === 2 ? ("20" + f.expAnio) : f.expAnio;
    const exp = new Date(+yy, +f.expMes, 0);
    if (exp < new Date()) return "Tarjeta expirada.";
    if (!/^\d{3,4}$/.test(f.cvv)) return "CVV inválido.";
    if (!f.direccion.trim() || !f.ciudad.trim() || !f.pais.trim()) return "Completa la dirección de cobro.";
    return null;
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    const v = validar();
    if (v) { alert(v); return; }
    try {
      const payload = {
        tarjeta: {
          nombre: f.nombre.trim(),
          numero: f.numero.replace(/\s+/g, ""),
          expMes: Number(f.expMes),
          expAnio: Number(f.expAnio.length === 2 ? ("20" + f.expAnio) : f.expAnio),
          cvv: f.cvv,
        },
        facturacion: {
          direccion: f.direccion.trim(),
          ciudad: f.ciudad.trim(),
          pais: f.pais.trim(),
          zip: f.zip.trim(),
        },
      };
      const { data } = await comprasApi.checkout(payload);
      nav(`/compras/checkout/${data.idReserva}`);
    } catch (e2) {
      alert(e2?.response?.data?.error || e2.message || "No fue posible completar el pago");
    }
  };

  if (!u) return null;

  return (
    <div className="container checkout">
      <div className="grid">
        <div className="card">
          <h2>Pagar reserva</h2>
          <form className="form" onSubmit={onSubmit}>
            <label>Nombre del titular
              <input className="input" name="nombre" value={f.nombre} onChange={onChange} />
            </label>

            <label>Número de tarjeta
              <input
                className="input"
                name="numero"
                value={f.numero}
                onChange={onChange}
                placeholder="4111 1111 1111 1111"
                inputMode="numeric"
                autoComplete="cc-number"
              />
            </label>

            <div className="row">
              <label>Mes (MM)
                <input className="input" name="expMes" value={f.expMes}
                       onChange={onChange} placeholder="MM" inputMode="numeric" autoComplete="cc-exp-month" />
              </label>
              <label>Año (YY/AAAA)
                <input className="input" name="expAnio" value={f.expAnio}
                       onChange={onChange} placeholder="YY o AAAA" inputMode="numeric" autoComplete="cc-exp-year" />
              </label>
              <label>CVV
                <input className="input" name="cvv" value={f.cvv}
                       onChange={onChange} placeholder="3-4 dígitos" inputMode="numeric" autoComplete="cc-csc" />
              </label>
            </div>

            <label>Dirección
              <textarea className="input input--textarea" name="direccion" value={f.direccion} onChange={onChange} rows={3} />
            </label>
            <div className="row">
              <label>Ciudad
                <input className="input" name="ciudad" value={f.ciudad} onChange={onChange} />
              </label>
              <label>País
                <input className="input" name="pais" value={f.pais} onChange={onChange} />
              </label>
              <label>Código postal
                <input className="input" name="zip" value={f.zip} onChange={onChange} />
              </label>
            </div>

            <button className="btn btn-primary" type="submit" disabled={loading}>
              Confirmar y pagar
            </button>
          </form>
        </div>

        <div className="card">
          <h2>Resumen</h2>
          {loading ? (
            <>
              <div className="skl title" />
              <div className="skl row" />
              <div className="skl row" />
            </>
          ) : err ? (
            <div className="error">{err}</div>
          ) : (
            <>
              <ul className="sum-list">
                {cart?.items?.map((it) => (
                  <li key={it.idItem} className="sum-item">
                    <div>
                      <div><strong>{it.codigoVuelo}</strong> <span className="pill">{it.clase}</span></div>
                      <RouteLine item={it} />
                      <div className="label">{new Date(it.fechaSalida).toLocaleString("es-GT")}</div>
                    </div>
                    <div>{money(it.subtotal)}</div>
                  </li>
                ))}
              </ul>
              <div className="sum-total">
                Total: <strong>{money(cart?.total)}</strong>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
