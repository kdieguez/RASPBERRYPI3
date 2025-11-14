import { NavLink } from "react-router-dom";
import "./admin.css";

export default function AdminNav() {
  const link = ({ isActive }) => "adm__link" + (isActive ? " is-active" : "");
  return (
    <div className="adm__nav container">
      <NavLink to="/admin"              className={link}>Panel</NavLink>
      <NavLink to="/admin/paises"       className={link}>Países</NavLink>
      <NavLink to="/admin/ciudades"     className={link}>Ciudades</NavLink>
      <NavLink to="/admin/rutas"        className={link}>Rutas</NavLink>
      <NavLink to="/admin/vuelos/nuevo" className={link}>Crear vuelo</NavLink>
      <NavLink to="/admin/info"         className={link}>Informativas</NavLink>
      <NavLink to="/admin/contenido-home" className={link}>Contenido Home</NavLink>
    </div>
  );
}
