import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, CircularProgress, Stack } from '@mui/material';
import { useNotification } from '../../hooks/useNotification';
import { Notification } from '../../components/Notification';
import {
  confirmWorkspaceProposal,
  createWorkspaceProposal,
  discardWorkspaceProposal,
  getWorkspace,
  listWorkspaces,
  WorkspaceApiError,
} from '../../services/workspaceService';
import type { WorkspaceProposal, WorkspaceSummary } from './types';
import { WorkspacePageShell } from './components/WorkspacePageShell';
import { InfoBanner } from './components/InfoBanner';
import { ProposalReviewPanel } from './components/ProposalReviewPanel';

export default function WorkspaceSuggestionsPage() {
  const { workspaceId: workspaceIdParam } = useParams();
  const navigate = useNavigate();
  const workspaceId = Number(workspaceIdParam);
  const invalidId = Number.isNaN(workspaceId);

  const { notification, showNotification, hideNotification } = useNotification();
  const [workspaceName, setWorkspaceName] = useState('');
  const [workspaces, setWorkspaces] = useState<WorkspaceSummary[]>([]);
  const [proposal, setProposal] = useState<WorkspaceProposal | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadSuggestion = useCallback(async () => {
    if (invalidId) {
      setLoading(false);
      setError('Workspace inválido');
      return;
    }
    setLoading(true);
    setError(null);
    setProposal(null);
    try {
      const [workspace, workspaceList, created] = await Promise.all([
        getWorkspace(workspaceId),
        listWorkspaces(),
        createWorkspaceProposal('SUGESTAO'),
      ]);
      setWorkspaceName(workspace.nome);
      setWorkspaces(workspaceList);
      setProposal(created);
    } catch (err) {
      const message = err instanceof WorkspaceApiError ? err.message : 'Erro ao gerar sugestão';
      setError(message);
    } finally {
      setLoading(false);
    }
  }, [invalidId, workspaceId]);

  useEffect(() => {
    void loadSuggestion();
  }, [loadSuggestion]);

  const handleConfirm = async (targetWorkspaceId?: number) => {
    if (!proposal) {
      return;
    }
    setSubmitting(true);
    try {
      await confirmWorkspaceProposal(proposal.id, {
        workspaceId: targetWorkspaceId ?? workspaceId,
      });
      showNotification('Sugestão aplicada com sucesso', 'success');
      setProposal(null);
    } catch (err) {
      const message = err instanceof WorkspaceApiError ? err.message : 'Erro ao confirmar sugestão';
      showNotification(message, 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDiscard = async () => {
    if (!proposal) {
      return;
    }
    setSubmitting(true);
    try {
      await discardWorkspaceProposal(proposal.id);
      showNotification('Sugestão descartada', 'success');
      setProposal(null);
    } catch {
      showNotification('Erro ao descartar sugestão', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (invalidId) {
    return (
      <WorkspacePageShell title="Sugestões">
        <InfoBanner variant="danger">Workspace inválido.</InfoBanner>
        <Button sx={{ mt: 2 }} onClick={() => navigate('/workspace')}>
          Voltar ao hub
        </Button>
      </WorkspacePageShell>
    );
  }

  return (
    <>
      <WorkspacePageShell
        title="Sugerir para mim"
        subtitle={workspaceName ? `Sugestões para ${workspaceName}` : 'Carregando workspace…'}
        actions={
          !loading && !proposal ? (
            <Button variant="outlined" onClick={() => void loadSuggestion()}>
              Gerar nova sugestão
            </Button>
          ) : undefined
        }
      >
        {loading && !proposal ? (
          <Stack alignItems="center" py={4}>
            <CircularProgress aria-label="Gerando sugestão" />
          </Stack>
        ) : (
          <Stack spacing={2}>
            <InfoBanner variant="info">
              Sugestões nunca são aplicadas automaticamente — revise e confirme manualmente.
            </InfoBanner>
            <ProposalReviewPanel
              proposal={proposal}
              workspaces={workspaces}
              loading={loading && !proposal}
              submitting={submitting}
              error={error}
              onConfirm={(targetId) => void handleConfirm(targetId)}
              onDiscard={() => void handleDiscard()}
            />
          </Stack>
        )}
      </WorkspacePageShell>

      <Notification
        open={notification.open}
        message={notification.message}
        severity={notification.severity}
        onClose={hideNotification}
      />
    </>
  );
}
