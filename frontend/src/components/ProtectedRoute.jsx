import { Navigate, Outlet, useLocation } from "react-router-dom";
import { isLoggedIn, getUser } from "../lib/auth";

export default function ProtectedRoute({ allowRoles }) {
  const location = useLocation();

  if (!isLoggedIn()) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (allowRoles?.length) {
    const u = getUser();
    if (!u || !allowRoles.includes(Number(u.idRol))) {
      return <Navigate to="/" replace />;
    }
  }

  return <Outlet />;
}
