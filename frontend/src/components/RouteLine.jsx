export default function RouteLine({ item, className = "" }) {
  const o = [item?.ciudadOrigen, item?.paisOrigen].filter(Boolean).join(", ");
  const d = [item?.ciudadDestino, item?.paisDestino].filter(Boolean).join(", ");
  if (!o && !d) return null;
  return (
    <div className={`route-line ${className}`}>
      {o} <span className="route-arrow">→</span> {d}
    </div>
  );
}
