import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { CircularProgress, Box } from '@mui/material';
import { useAuth } from '../contexts/AuthContext';
import { Login } from '../pages/Login';
import Dashboard from '../pages/Dashboard';
import Usuarios from '../pages/Usuarios';
import Funcionarios from '../pages/Funcionarios';
import { FolhaPagamento } from '../pages/FolhaPagamento';
import Rubricas from '../pages/Rubricas';
import { Beneficios } from '../pages/Beneficios';
import Importacao from '../pages/Importacao';
import { Relatorios } from '../pages/Relatorios';
import { AuthProvider } from '../contexts/AuthContext';
import { Layout } from '../components/Layout';
import Cargos from '../pages/Cargos';
import CentrosCusto from '../pages/CentrosCusto';
import LinhasNegocio from '../pages/LinhasNegocio';
import Organograma from '../pages/Organograma';

function PrivateRoute() {
  const { user, loading } = useAuth();
  console.log('PrivateRoute - Estado:', { user, loading });

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="100vh">
        <CircularProgress />
      </Box>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}

// Componente que envolve tudo com o BrowserRouter
export function RouterWithAuth() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route element={<PrivateRoute />}>
            <Route element={<Layout />}>
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/usuarios" element={<Usuarios />} />
              <Route path="/funcionarios" element={<Funcionarios />} />
              <Route path="/folha-pagamento" element={<FolhaPagamento />} />
              <Route path="/rubricas" element={<Rubricas />} />
              <Route path="/beneficios" element={<Beneficios />} />
              <Route path="/importacao" element={<Importacao />} />
              <Route path="/relatorios" element={<Relatorios />} />
              <Route path="/cargos" element={<Cargos />} />
              <Route path="/centros-custo" element={<CentrosCusto />} />
              <Route path="/linhas-negocio" element={<LinhasNegocio />} />
              <Route path="/organograma" element={<Organograma />} />
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

// Mantendo o AppRoutes para compatibilidade
export function AppRoutes() {
  return <RouterWithAuth />;
} 