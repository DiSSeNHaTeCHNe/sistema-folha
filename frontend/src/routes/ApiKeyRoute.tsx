import { Navigate, Outlet } from 'react-router-dom';
import { CircularProgress, Box } from '@mui/material';
import { useAuth } from '../contexts/AuthContext';
import { canAccessApiKeysPage } from '../utils/permissions';

export function ApiKeyRoute() {
  const { user, loading } = useAuth();

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

  if (!canAccessApiKeysPage(user)) {
    return <Navigate to="/dashboard" replace state={{ acessoNegado: true }} />;
  }

  return <Outlet />;
}
