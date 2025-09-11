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

          {/* Rutas admin */}
          <Route element={<ProtectedRoute allowRoles={[1]} />}>
            <Route path="/admin/vuelos/nuevo" element={<VueloNuevo />} />
            <Route path="/admin/usuarios" element={<UsuariosList />} />
            <Route path="/admin/usuarios/:id" element={<UsuarioEdit />} />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
      <Footer />
    </>
  );
}
