import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Stack,
  Typography,
} from '@mui/material';
import { useNotification } from '../../hooks/useNotification';
import { Notification } from '../../components/Notification';
import { createDashboardQueryClient } from '../MeuDashboard/queryClient';
import { useWorkspaceLayout } from './hooks/useWorkspaceLayout';
import { useUnsavedChangesGuard } from './hooks/useUnsavedChangesGuard';
import { WorkspaceGrid } from './WorkspaceGrid';
import { WidgetBuilderDrawer } from './WidgetBuilderDrawer';
import { WorkspacePageShell } from './components/WorkspacePageShell';
import { StatusChip } from './components/StatusChip';
import {
  installOrcamentoTemplate,
  listWidgetDefinitions,
} from '../../services/workspaceService';
import type { UserWidgetDefinition } from './types';

function WorkspaceDetailEmptyState({
  onAddWidget,
  onInstallTemplate,
  installing,
}: {
  onAddWidget: () => void;
  onInstallTemplate: () => void;
  installing: boolean;
}) {
  return (
    <Box
      display="flex"
      flexDirection="column"
      alignItems="center"
      justifyContent="center"
      minHeight={280}
      role="status"
      aria-label="Workspace vazio"
      sx={{ border: '2px dashed', borderColor: 'divider', borderRadius: 2, p: 4 }}
    >
      <Typography variant="h6" gutterBottom>
        Workspace vazio
      </Typography>
      <Typography color="text.secondary" align="center" mb={2}>
        Adicione widgets ou instale um template para começar a montar seu painel.
      </Typography>
      <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
        <Button variant="contained" onClick={onAddWidget}>
          Adicionar widget
        </Button>
        <Button variant="outlined" onClick={onInstallTemplate} disabled={installing}>
          Instalar template
        </Button>
      </Stack>
    </Box>
  );
}

function WorkspaceDetailContent() {
  const { workspaceId: workspaceIdParam } = useParams();
  const navigate = useNavigate();
  const workspaceId = Number(workspaceIdParam);
  const invalidId = Number.isNaN(workspaceId);
  const { showNotification, notification, hideNotification } = useNotification();
  const [userDefinitions, setUserDefinitions] = useState<UserWidgetDefinition[]>([]);
  const [widgetBuilderOpen, setWidgetBuilderOpen] = useState(false);
  const [installingOrcamento, setInstallingOrcamento] = useState(false);
  const [notFound, setNotFound] = useState(false);

  const {
    summaries,
    activeLayout,
    editMode,
    loading,
    saving,
    dirty,
    enterEditMode,
    cancelEdit,
    save,
    updateDraftWidgets,
    loadWorkspaceLayout,
    resetActiveLayoutCache,
    error,
  } = useWorkspaceLayout({ fixedWorkspaceId: invalidId ? null : workspaceId });

  useUnsavedChangesGuard({ dirty: editMode && dirty });

  useEffect(() => {
    if (invalidId) {
      setNotFound(true);
      return;
    }
    void listWidgetDefinitions()
      .then(setUserDefinitions)
      .catch(() => setUserDefinitions([]));
  }, [invalidId]);

  useEffect(() => {
    if (invalidId || loading) {
      return;
    }
    const exists = summaries.some((item) => item.id === workspaceId);
    if (summaries.length > 0 && !exists) {
      setNotFound(true);
    }
  }, [invalidId, loading, summaries, workspaceId]);

  useEffect(() => {
    if (error && !activeLayout && !loading) {
      setNotFound(true);
    }
  }, [error, activeLayout, loading]);

  const widgets = useMemo(() => {
    if (!activeLayout) {
      return [];
    }
    return [...activeLayout.widgets].sort((a, b) => a.ordem - b.ordem);
  }, [activeLayout]);

  const workspaceName = summaries.find((item) => item.id === workspaceId)?.nome ?? activeLayout?.nome ?? 'Workspace';

  const handleSave = async () => {
    const ok = await save();
    showNotification(ok ? 'Layout salvo com sucesso' : 'Erro ao salvar layout', ok ? 'success' : 'error');
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
    setInstallingOrcamento(true);
    try {
      await installOrcamentoTemplate(workspaceId);
      resetActiveLayoutCache();
      await loadWorkspaceLayout(workspaceId);
      const defs = await listWidgetDefinitions();
      setUserDefinitions(defs);
      showNotification('Template de orçamento instalado', 'success');
    } catch {
      showNotification('Erro ao instalar template de orçamento', 'error');
    } finally {
      setInstallingOrcamento(false);
    }
  };

  if (invalidId || notFound) {
    return (
      <WorkspacePageShell title="Workspace não encontrado">
        <Alert severity="error" role="alert">
          Workspace não encontrado ou acesso negado.
        </Alert>
        <Button sx={{ mt: 2 }} onClick={() => navigate('/workspace')}>
          Voltar ao hub
        </Button>
      </WorkspacePageShell>
    );
  }

  if (loading || !activeLayout) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight={400}>
        <CircularProgress size={60} aria-label="Carregando workspace" />
      </Box>
    );
  }

  return (
    <>
      <WorkspacePageShell
        title={workspaceName}
        subtitle={`${widgets.length} widget${widgets.length !== 1 ? 's' : ''}`}
        actions={
          <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap alignItems="center">
            {editMode ? <StatusChip variant="warn" label="editando" /> : null}
            {!editMode && (
              <>
                <Button variant="outlined" onClick={() => setWidgetBuilderOpen(true)}>
                  Adicionar widget
                </Button>
                <Button
                  variant="outlined"
                  onClick={() => void handleInstallOrcamento()}
                  disabled={installingOrcamento}
                >
                  Instalar template
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
        }
      >
        {widgets.length === 0 ? (
          <WorkspaceDetailEmptyState
            onAddWidget={() => setWidgetBuilderOpen(true)}
            onInstallTemplate={() => void handleInstallOrcamento()}
            installing={installingOrcamento}
          />
        ) : (
          <WorkspaceGrid
            workspaceId={workspaceId}
            widgets={widgets}
            userDefinitions={userDefinitions}
            editMode={editMode}
            onWidgetsChange={editMode ? updateDraftWidgets : undefined}
            onRemoveWidget={editMode ? handleRemoveWidget : undefined}
          />
        )}
      </WorkspacePageShell>

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

export default function WorkspaceDetailPage() {
  const queryClient = useMemo(() => createDashboardQueryClient(), []);
  return (
    <QueryClientProvider client={queryClient}>
      <WorkspaceDetailContent />
    </QueryClientProvider>
  );
}
