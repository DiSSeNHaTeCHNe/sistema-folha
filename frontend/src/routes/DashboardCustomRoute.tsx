import { Navigate, Outlet } from 'react-router-dom';
import { Box, CircularProgress } from '@mui/material';
import { useAuth } from '../contexts/AuthContext';
import { podeAcessarMeuDashboard } from '../utils/dashboardAccess';

export function DashboardCustomRoute() {
  const { user, loading, acessoUsuario } = useAuth();

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

  if (!podeAcessarMeuDashboard(acessoUsuario)) {
    return <Navigate to="/dashboard" replace state={{ acessoNegadoMeuDashboard: true }} />;
  }

  return <Outlet />;
}
