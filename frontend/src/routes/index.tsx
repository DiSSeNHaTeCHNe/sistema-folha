import { useMemo } from 'react';
import {
  createBrowserRouter,
  RouterProvider,
  Navigate,
  Outlet,
  type RouteObject,
} from 'react-router-dom';
import { CircularProgress, Box } from '@mui/material';
import { useAuth } from '../contexts/AuthContext';
import { Login } from '../pages/Login';
import Dashboard from '../pages/Dashboard';
import Usuarios from '../pages/Usuarios';
import Funcionarios from '../pages/Funcionarios';
import { FolhaPagamento } from '../pages/FolhaPagamento';
import BeneficiosMensais from '../pages/BeneficiosMensais';
import Rubricas from '../pages/Rubricas';
import RubricasFixas from '../pages/RubricasFixas';
import Importacao from '../pages/Importacao';
import { Relatorios } from '../pages/Relatorios';
import { AuthProvider } from '../contexts/AuthContext';
import { Layout } from '../components/Layout';
import { AdminRoute } from './AdminRoute';
import { ApiKeyRoute } from './ApiKeyRoute';
import Cargos from '../pages/Cargos';
import CentrosCusto from '../pages/CentrosCusto';
import LinhasNegocio from '../pages/LinhasNegocio';
import Organograma from '../pages/Organograma';
import TiposBeneficio from '../pages/TiposBeneficio';
import ApiKeys from '../pages/ApiKeys';
import MeuDashboard from '../pages/MeuDashboard';
import WorkspaceHubPage from '../pages/Workspace/WorkspaceHubPage';
import WorkspaceDetailPage from '../pages/Workspace/WorkspaceDetailPage';
import DatasetListPage from '../pages/Workspace/DatasetListPage';
import DatasetEditorPage from '../pages/Workspace/DatasetEditorPage';
import DatasetHistoryPage from '../pages/Workspace/DatasetHistoryPage';
import TemplateCatalogPage from '../pages/Workspace/TemplateCatalogPage';
import TemplatePublishPage from '../pages/Workspace/TemplatePublishPage';
import TemplateUpgradePage from '../pages/Workspace/TemplateUpgradePage';
import WidgetBuilderPage from '../pages/Workspace/WidgetBuilderPage';
import WorkspaceAssistantPage from '../pages/Workspace/WorkspaceAssistantPage';
import WorkspaceSuggestionsPage from '../pages/Workspace/WorkspaceSuggestionsPage';
import { DashboardCustomRoute } from './DashboardCustomRoute';
import { WorkspaceRoute } from './WorkspaceRoute';

function AuthLayout() {
  return (
    <AuthProvider>
      <Outlet />
    </AuthProvider>
  );
}

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

const appRouteObjects: RouteObject[] = [
  { path: '/login', element: <Login /> },
  {
    element: <PrivateRoute />,
    children: [
      {
        element: <Layout />,
        children: [
          { path: '/dashboard', element: <Dashboard /> },
          {
            element: <DashboardCustomRoute />,
            children: [{ path: '/meu-dashboard', element: <MeuDashboard /> }],
          },
          {
            element: <WorkspaceRoute />,
            children: [
              { path: '/workspace', element: <WorkspaceHubPage /> },
              { path: '/workspace/datasets', element: <DatasetListPage /> },
              { path: '/workspace/datasets/:id/historico', element: <DatasetHistoryPage /> },
              { path: '/workspace/datasets/:id', element: <DatasetEditorPage /> },
              { path: '/workspace/templates/publish', element: <TemplatePublishPage /> },
              { path: '/workspace/templates/:templateId/upgrade', element: <TemplateUpgradePage /> },
              { path: '/workspace/templates', element: <TemplateCatalogPage /> },
              { path: '/workspace/assistente', element: <WorkspaceAssistantPage /> },
              { path: '/workspace/:workspaceId/widgets/novo', element: <WidgetBuilderPage /> },
              { path: '/workspace/:workspaceId/sugestoes', element: <WorkspaceSuggestionsPage /> },
              { path: '/workspace/:workspaceId', element: <WorkspaceDetailPage /> },
            ],
          },
          { path: '/funcionarios', element: <Funcionarios /> },
          { path: '/folha-pagamento', element: <FolhaPagamento /> },
          { path: '/beneficios-mensais', element: <BeneficiosMensais /> },
          { path: '/beneficios', element: <Navigate to="/beneficios-mensais" replace /> },
          { path: '/relatorios', element: <Relatorios /> },
          {
            element: <ApiKeyRoute />,
            children: [{ path: '/api-keys', element: <ApiKeys /> }],
          },
          {
            element: <AdminRoute />,
            children: [
              { path: '/usuarios', element: <Usuarios /> },
              { path: '/linhas-negocio', element: <LinhasNegocio /> },
              { path: '/centros-custo', element: <CentrosCusto /> },
              { path: '/cargos', element: <Cargos /> },
              { path: '/rubricas', element: <Rubricas /> },
              { path: '/rubricas-fixas', element: <RubricasFixas /> },
              { path: '/tipos-beneficio', element: <TiposBeneficio /> },
              { path: '/organograma', element: <Organograma /> },
              { path: '/importacao', element: <Importacao /> },
            ],
          },
          { path: '/', element: <Navigate to="/dashboard" replace /> },
        ],
      },
    ],
  },
  { path: '*', element: <Navigate to="/dashboard" replace /> },
];

export const routeObjects: RouteObject[] = [
  {
    element: <AuthLayout />,
    children: appRouteObjects,
  },
];

export function RouterWithAuth() {
  const router = useMemo(() => createBrowserRouter(routeObjects), []);

  return <RouterProvider router={router} />;
}

// Mantendo o AppRoutes para compatibilidade
export function AppRoutes() {
  return <RouterWithAuth />;
}
