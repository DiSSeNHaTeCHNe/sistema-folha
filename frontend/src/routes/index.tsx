import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom';
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
              <Route element={<DashboardCustomRoute />}>
                <Route path="/meu-dashboard" element={<MeuDashboard />} />
              </Route>
              <Route element={<WorkspaceRoute />}>
                <Route path="/workspace" element={<WorkspaceHubPage />} />
                <Route path="/workspace/datasets" element={<DatasetListPage />} />
                <Route path="/workspace/datasets/:id/historico" element={<DatasetHistoryPage />} />
                <Route path="/workspace/datasets/:id" element={<DatasetEditorPage />} />
                <Route path="/workspace/templates/publish" element={<TemplatePublishPage />} />
                <Route path="/workspace/templates/:templateId/upgrade" element={<TemplateUpgradePage />} />
                <Route path="/workspace/templates" element={<TemplateCatalogPage />} />
                <Route path="/workspace/assistente" element={<WorkspaceAssistantPage />} />
                <Route path="/workspace/:workspaceId/widgets/novo" element={<WidgetBuilderPage />} />
                <Route path="/workspace/:workspaceId/sugestoes" element={<WorkspaceSuggestionsPage />} />
                <Route path="/workspace/:workspaceId" element={<WorkspaceDetailPage />} />
              </Route>
              <Route path="/funcionarios" element={<Funcionarios />} />
              <Route path="/folha-pagamento" element={<FolhaPagamento />} />
              <Route path="/beneficios-mensais" element={<BeneficiosMensais />} />
              <Route path="/beneficios" element={<Navigate to="/beneficios-mensais" replace />} />
              <Route path="/relatorios" element={<Relatorios />} />
              <Route element={<ApiKeyRoute />}>
                <Route path="/api-keys" element={<ApiKeys />} />
              </Route>
              <Route element={<AdminRoute />}>
                <Route path="/usuarios" element={<Usuarios />} />
                <Route path="/linhas-negocio" element={<LinhasNegocio />} />
                <Route path="/centros-custo" element={<CentrosCusto />} />
                <Route path="/cargos" element={<Cargos />} />
                <Route path="/rubricas" element={<Rubricas />} />
                <Route path="/rubricas-fixas" element={<RubricasFixas />} />
                <Route path="/tipos-beneficio" element={<TiposBeneficio />} />
                <Route path="/organograma" element={<Organograma />} />
                <Route path="/importacao" element={<Importacao />} />
              </Route>
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