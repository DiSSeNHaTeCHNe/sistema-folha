import { useState } from 'react';
import { Button, Stack, TextField } from '@mui/material';
import { useAuth } from '../../contexts/AuthContext';
import { useNotification } from '../../hooks/useNotification';
import { Notification } from '../../components/Notification';
import {
  confirmWorkspaceProposal,
  createWorkspaceProposal,
  discardWorkspaceProposal,
  listWorkspaces,
  WorkspaceApiError,
} from '../../services/workspaceService';
import { WORKSPACE_IA_CRIAR, type WorkspaceProposal, type WorkspaceSummary } from './types';
import { WorkspacePageShell } from './components/WorkspacePageShell';
import { InfoBanner } from './components/InfoBanner';
import { ProposalReviewPanel } from './components/ProposalReviewPanel';

export default function WorkspaceAssistantPage() {
  const { user } = useAuth();
  const { notification, showNotification, hideNotification } = useNotification();
  const podeUsarIa = user?.permissoes?.includes(WORKSPACE_IA_CRIAR) ?? false;

  const [descricao, setDescricao] = useState('');
  const [workspaces, setWorkspaces] = useState<WorkspaceSummary[]>([]);
  const [activeProposal, setActiveProposal] = useState<WorkspaceProposal | null>(null);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadWorkspaces = async () => {
    try {
      const items = await listWorkspaces();
      setWorkspaces(items);
    } catch {
      setWorkspaces([]);
    }
  };

  const handleGerarProposta = async () => {
    if (!descricao.trim()) {
      setError('Descreva o que você deseja criar antes de gerar a proposta.');
      return;
    }
    setLoading(true);
    setError(null);
    setActiveProposal(null);
    try {
      await loadWorkspaces();
      const proposal = await createWorkspaceProposal('ASSISTENTE', descricao.trim());
      setActiveProposal(proposal);
    } catch (err) {
      const message = err instanceof WorkspaceApiError ? err.message : 'Erro ao gerar proposta';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const handleConfirm = async (workspaceId?: number) => {
    if (!activeProposal) {
      return;
    }
    setSubmitting(true);
    try {
      await confirmWorkspaceProposal(
        activeProposal.id,
        workspaceId != null ? { workspaceId } : undefined,
      );
      showNotification('Proposta aplicada com sucesso', 'success');
      setActiveProposal(null);
      setDescricao('');
    } catch (err) {
      const message = err instanceof WorkspaceApiError ? err.message : 'Erro ao confirmar proposta';
      showNotification(message, 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDiscard = async () => {
    if (!activeProposal) {
      return;
    }
    setSubmitting(true);
    try {
      await discardWorkspaceProposal(activeProposal.id);
      showNotification('Proposta descartada', 'success');
      setActiveProposal(null);
    } catch {
      showNotification('Erro ao descartar proposta', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (!podeUsarIa) {
    return (
      <WorkspacePageShell
        title="Assistente IA"
        subtitle="Propostas revisáveis antes de aplicar qualquer alteração"
      >
        <InfoBanner variant="warn" title="Indisponível">
          Você não possui permissão para usar o assistente de IA do Workspace. Solicite a capacidade
          WORKSPACE_IA_CRIAR ao administrador.
        </InfoBanner>
      </WorkspacePageShell>
    );
  }

  return (
    <>
      <WorkspacePageShell
        title="Assistente IA"
        subtitle="Descreva o que deseja e revise a proposta antes de confirmar"
        actions={
          !activeProposal ? (
            <Button
              variant="contained"
              onClick={() => void handleGerarProposta()}
              disabled={loading || !descricao.trim()}
            >
              Gerar proposta
            </Button>
          ) : undefined
        }
      >
        <Stack spacing={3}>
          {!activeProposal && (
            <TextField
              label="O que você deseja criar?"
              multiline
              minRows={3}
              fullWidth
              value={descricao}
              onChange={(event) => setDescricao(event.target.value)}
              helperText="Ex.: um KPI com total de folha ou um dataset de headcount por centro de custo"
            />
          )}

          <ProposalReviewPanel
            proposal={activeProposal}
            workspaces={workspaces}
            loading={loading}
            submitting={submitting}
            error={error}
            onConfirm={(workspaceId) => void handleConfirm(workspaceId)}
            onDiscard={() => void handleDiscard()}
          />
        </Stack>
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
