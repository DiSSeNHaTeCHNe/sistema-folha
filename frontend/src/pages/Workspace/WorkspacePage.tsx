import { useEffect, useMemo, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Link,
  Stack,
  Typography,
} from '@mui/material';
import { useNotification } from '../../hooks/useNotification';
import { Notification } from '../../components/Notification';
import { createDashboardQueryClient } from '../MeuDashboard/queryClient';
import { useWorkspaceLayout } from './hooks/useWorkspaceLayout';
import { WorkspaceEmptyState, WorkspaceSwitcher } from './WorkspaceSwitcher';
import { WorkspaceGrid } from './WorkspaceGrid';
import { WidgetBuilderDrawer } from './WidgetBuilderDrawer';
import {
  installOrcamentoTemplate,
  listWidgetDefinitions,
} from '../../services/workspaceService';
import type { UserWidgetDefinition } from './types';

function WorkspacePageContent() {
  const { notification, showNotification, hideNotification } = useNotification();
  const [userDefinitions, setUserDefinitions] = useState<UserWidgetDefinition[]>([]);
  const [widgetBuilderOpen, setWidgetBuilderOpen] = useState(false);
  const [installingOrcamento, setInstallingOrcamento] = useState(false);
  const {
    summaries,
    activeWorkspaceId,
    activeLayout,
    editMode,
    loading,
    saving,
    dirty,
    switchWorkspace,
    createNewWorkspace,
    removeWorkspace,
    enterEditMode,
    cancelEdit,
    save,
    updateDraftWidgets,
    loadWorkspaceLayout,
    resetActiveLayoutCache,
  } = useWorkspaceLayout();

  useEffect(() => {
    void listWidgetDefinitions()
      .then(setUserDefinitions)
      .catch(() => setUserDefinitions([]));
  }, []);

  useEffect(() => {
    if (!dirty) {
      return;
    }
    const handler = (event: BeforeUnloadEvent) => {
      event.preventDefault();
      event.returnValue = '';
    };
    window.addEventListener('beforeunload', handler);
    return () => window.removeEventListener('beforeunload', handler);
  }, [dirty]);

  const widgets = useMemo(() => {
    if (!activeLayout) {
      return [];
    }
    return [...activeLayout.widgets].sort((a, b) => a.ordem - b.ordem);
  }, [activeLayout]);

  const hasWorkspaces = summaries.length > 0;

  const handleCreate = async (nome: string) => {
    try {
      await createNewWorkspace(nome);
      showNotification('Workspace criado', 'success');
    } catch {
      showNotification('Erro ao criar workspace', 'error');
    }
  };

  const handleDelete = async (workspaceId: number) => {
    try {
      await removeWorkspace(workspaceId);
      showNotification('Workspace excluído', 'success');
    } catch {
      showNotification('Erro ao excluir workspace', 'error');
    }
  };

  const handleSave = async () => {
    const ok = await save();
    if (ok) {
      showNotification('Layout salvo com sucesso', 'success');
    } else {
      showNotification('Erro ao salvar layout', 'error');
    }
  };

  const handleRemoveWidget = (instanceId: string) => {
    if (!editMode) {
      return;
    }
    const next = widgets.filter((widget) => widget.instanceId !== instanceId).map((widget, index) => ({
      ...widget,
      ordem: index,
    }));
    updateDraftWidgets(next);
  };

  const handleWidgetSaved = (definition: UserWidgetDefinition) => {
    setUserDefinitions((current) => {
      const exists = current.some((item) => item.id === definition.id);
      if (exists) {
        return current.map((item) => (item.id === definition.id ? definition : item));
      }
      return [...current, definition];
    });
    showNotification('Widget salvo', 'success');
  };

  const handleInstallOrcamento = async () => {
    if (activeWorkspaceId == null) {
      return;
    }
    setInstallingOrcamento(true);
    try {
      await installOrcamentoTemplate(activeWorkspaceId);
      resetActiveLayoutCache();
      await loadWorkspaceLayout(activeWorkspaceId);
      const defs = await listWidgetDefinitions();
      setUserDefinitions(defs);
      showNotification('Template de orçamento instalado', 'success');
    } catch {
      showNotification('Erro ao instalar template de orçamento', 'error');
    } finally {
      setInstallingOrcamento(false);
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress size={60} aria-label="Carregando Workspace" />
      </Box>
    );
  }

  return (
    <>
      <Box sx={{ backgroundColor: 'background.default', minHeight: '100vh' }}>
        <Box mb={4} display="flex" justifyContent="space-between" alignItems="flex-start" gap={2} flexWrap="wrap">
          <Box>
            <Typography variant="h4" gutterBottom>
              Workspace
            </Typography>
            <Typography variant="subtitle1" color="text.secondary" sx={{ mb: 1 }}>
              Datasets, widgets e layouts personalizados
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              <Link component={RouterLink} to="/workspace/datasets">
                Gerenciar datasets
              </Link>
            </Typography>
            {hasWorkspaces && (
              <WorkspaceSwitcher
                summaries={summaries}
                activeWorkspaceId={activeWorkspaceId}
                onSwitch={switchWorkspace}
                onCreate={handleCreate}
                onDelete={handleDelete}
                disabled={editMode && dirty}
              />
            )}
          </Box>
          {hasWorkspaces && activeLayout && (
            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
              {!editMode && (
                <>
                  <Button variant="outlined" onClick={() => setWidgetBuilderOpen(true)}>
                    Novo widget
                  </Button>
                  <Button
                    variant="outlined"
                    onClick={() => void handleInstallOrcamento()}
                    disabled={installingOrcamento || activeWorkspaceId == null}
                  >
                    Instalar template de orçamento
                  </Button>
                  <Button variant="outlined" onClick={enterEditMode}>
                    Editar layout
                  </Button>
                </>
              )}
              {editMode && (
                <>
                  <Button variant="contained" onClick={() => void handleSave()} disabled={saving || !dirty}>
                    Salvar
                  </Button>
                  <Button variant="outlined" onClick={cancelEdit} disabled={saving}>
                    Cancelar
                  </Button>
                </>
              )}
            </Stack>
          )}
        </Box>

        {!hasWorkspaces ? (
          <WorkspaceEmptyState onCreate={handleCreate} />
        ) : !activeLayout ? (
          <Alert severity="error">Erro ao carregar workspace selecionado</Alert>
        ) : widgets.length === 0 ? (
          <Alert severity="info" role="status">
            Workspace vazio — entre no modo de edição para adicionar widgets.
          </Alert>
        ) : activeWorkspaceId != null ? (
          <WorkspaceGrid
            workspaceId={activeWorkspaceId}
            widgets={widgets}
            userDefinitions={userDefinitions}
            editMode={editMode}
            onWidgetsChange={editMode ? updateDraftWidgets : undefined}
            onRemoveWidget={editMode ? handleRemoveWidget : undefined}
          />
        ) : null}
      </Box>

      <WidgetBuilderDrawer
        open={widgetBuilderOpen}
        onClose={() => setWidgetBuilderOpen(false)}
        onSaved={handleWidgetSaved}
      />

      <Notification
        open={notification.open}
        message={notification.message}
        severity={notification.severity}
        onClose={hideNotification}
      />
    </>
  );
}

export default function WorkspacePage() {
  const queryClient = useMemo(() => createDashboardQueryClient(), []);
  return (
    <QueryClientProvider client={queryClient}>
      <WorkspacePageContent />
    </QueryClientProvider>
  );
}
