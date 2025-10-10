import { Routes, Route, Navigate } from "react-router-dom";
import Header from "./components/Header";
import Footer from "./components/Footer";
import Register from "./paginas/Registro";
import Login from "./paginas/Login";
import Perfil from "./paginas/Perfil";
import ProtectedRoute from "./components/ProtectedRoute";
import UsuariosList from "./paginas/admin/UsuariosList";
import UsuarioEdit from "./paginas/admin/UsuarioEdit";
import "./styles/home.css";
import VueloNuevo from "./paginas/admin/VueloNuevo";
import AdminPaises from "./paginas/admin/Paises";
import AdminCiudades from "./paginas/admin/Ciudades";
import AdminRutas from "./paginas/admin/Rutas";
import VuelosCatalogo from "./paginas/VuelosCatalogo";
import VueloEdit from "./paginas/admin/VueloEdit";
import AdminVuelosList from "./paginas/admin/AdminVuelosList";
import VueloDetalle from "./paginas/VueloDetalle";
import Carrito from "./paginas/Carrito";
import CheckoutAgradecimiento from "./paginas/CheckoutAgradecimiento";
import Checkout from "./paginas/CheckOut";
import HistorialCompras from "./paginas/HistorialCompras";
import ReservaDetalle from "./paginas/ReservaDetalle";
import ConfigSitio from "./paginas/admin/ConfigSitio";
import HistorialReservas from "./paginas/admin/HistorialReservas";
import AdminReservaDetalle from "./paginas/admin/AdminReservaDetalle";


function Home() {
  return (
    <div className="container home">
      <h1>Bienvenida a Aerolíneas ✈</h1>
      <p>Esta es una vista de prueba. Luego conectamos con el backend.</p>
    </div>
  );
}

export default function App() {
  return (
    <>
      <Header />
      <main className="main">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/registro" element={<Register />} />
          <Route path="/login" element={<Login />} />
          <Route path="/perfil" element={<Perfil />} />
          <Route path="/vuelos" element={<VuelosCatalogo/>} />
          <Route path="/vuelos/:id" element={<VueloDetalle/>}/>
          <Route path="/compras/carrito" element={<Carrito />} />
          <Route path="/compras/checkout" element={<Checkout />} />
          <Route path="/compras/checkout/:id" element={<CheckoutAgradecimiento />} />
          <Route path="/compras/historial" element={<HistorialCompras/>} />
          <Route path="/compras/reservas/:id" element={<ReservaDetalle/>}/>
          

          {/* Rutas admin */}
          <Route element={<ProtectedRoute allowRoles={[1]} />}>
            <Route path="/admin/vuelos/nuevo" element={<VueloNuevo />} />
            <Route path="/admin/usuarios" element={<UsuariosList />} />
            <Route path="/admin/usuarios/:id" element={<UsuarioEdit />} />
            <Route path="/admin/paises" element={<AdminPaises />} />
            <Route path="/admin/ciudades" element={<AdminCiudades />} />
            <Route path="/admin/rutas" element={<AdminRutas />} />
            <Route path="/admin/vuelos" element={<AdminVuelosList />} />
            <Route path="/admin/vuelos/:id" element={<VueloEdit />} />
            <Route path="/admin/config" element={<ConfigSitio/>}/>
            <Route path="/admin/reservas" element={<HistorialReservas />} />
            <Route path="/admin/reservas/:id" element={<AdminReservaDetalle />} />

          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
      <Footer />
    </>
  );
}
