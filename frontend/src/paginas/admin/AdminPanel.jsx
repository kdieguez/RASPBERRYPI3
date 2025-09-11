import { Outlet, NavLink } from "react-router-dom";

export default function AdminPanel() {
  return (
    <div className="container" style={{ padding: "28px 0 40px" }}>
      <h1>Panel de administración</h1>
      <nav style={{ margin: "12px 0 20px", display: "flex", gap: 12 }}>
        <NavLink to="/admin" end>Inicio</NavLink>
        <NavLink to="/admin/vuelos">Vuelos</NavLink>
      </nav>
      <Outlet />
    </div>
  );
}
