import { Navigate, Outlet } from 'react-router-dom';
import { Alert, Box, CircularProgress } from '@mui/material';
import { useAuth } from '../contexts/AuthContext';
import { podeAcessarWorkspace } from '../utils/workspaceAccess';

export function WorkspaceRoute() {
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

  if (!podeAcessarWorkspace(acessoUsuario)) {
    return (
      <Box p={3}>
        <Alert severity="warning" role="alert">
          Acesso negado ao Workspace. É necessário vínculo no organograma com escopo de centro de custo.
        </Alert>
      </Box>
    );
  }

  return <Outlet />;
}
