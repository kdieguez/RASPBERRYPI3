import { Routes, Route, Navigate } from "react-router-dom";
import Header from "./components/Header";
import Footer from "./components/Footer";
import Register from "./paginas/Registro";
import Login from "./paginas/Login"

function Home() {
  return (
    <div className="container" style={{ padding: "28px 0 40px" }}>
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
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
      <Footer />
    </>
  );
}
