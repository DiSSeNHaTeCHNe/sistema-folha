import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Login } from '../pages/Login';
import { Layout } from '../components/Layout';
import { Dashboard } from '../pages/Dashboard';
import { Funcionarios } from '../pages/Funcionarios';
import { FolhaPagamento } from '../pages/FolhaPagamento';
import { Beneficios } from '../pages/Beneficios';
import { Relatorios } from '../pages/Relatorios';
import { Example } from '../pages/Example';

interface PrivateRouteProps {
  children: React.ReactNode;
}

function PrivateRoute({ children }: PrivateRouteProps) {
  const { user, loading } = useAuth();

  if (loading) {
    return <div>Carregando...</div>;
  }

  if (!user) {
    return <Navigate to="/login" />;
  }

  return <Layout>{children}</Layout>;
}

export function AppRoutes() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route
          path="/"
          element={
            <PrivateRoute>
              <Dashboard />
            </PrivateRoute>
          }
        />
        <Route
          path="/funcionarios"
          element={
            <PrivateRoute>
              <Funcionarios />
            </PrivateRoute>
          }
        />
        <Route
          path="/folha-pagamento"
          element={
            <PrivateRoute>
              <FolhaPagamento />
            </PrivateRoute>
          }
        />
        <Route
          path="/beneficios"
          element={
            <PrivateRoute>
              <Beneficios />
            </PrivateRoute>
          }
        />
        <Route
          path="/relatorios"
          element={
            <PrivateRoute>
              <Relatorios />
            </PrivateRoute>
          }
        />
        <Route
          path="/exemplo"
          element={
            <PrivateRoute>
              <Example />
            </PrivateRoute>
          }
        />
      </Routes>
    </BrowserRouter>
  );
} 