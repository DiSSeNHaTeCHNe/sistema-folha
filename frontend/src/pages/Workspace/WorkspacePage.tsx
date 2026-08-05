import { useMemo } from 'react';
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
import { WorkspaceEmptyState, WorkspaceSwitcher } from './WorkspaceSwitcher';

function WorkspacePageContent() {
  const { notification, showNotification, hideNotification } = useNotification();
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
  } = useWorkspaceLayout();

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
            <Typography variant="subtitle1" color="text.secondary" sx={{ mb: 2 }}>
              Datasets, widgets e layouts personalizados
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
                <Button variant="outlined" onClick={enterEditMode}>
                  Editar layout
                </Button>
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
          <WorkspaceEmptyState />
        ) : !activeLayout ? (
          <Alert severity="error">Erro ao carregar workspace selecionado</Alert>
        ) : activeLayout.widgets.length === 0 ? (
          <Alert severity="info" role="status">
            Workspace vazio — adicione widgets no modo de edição (em breve nesta tela).
          </Alert>
        ) : null}
      </Box>

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
