import {
  Alert,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Typography,
} from '@mui/material';
import { useEffect, useState } from 'react';
import type { WorkspaceProposal, WorkspaceSummary } from './types';

function kindLabel(kind: string): string {
  switch (kind) {
    case 'DATASET':
      return 'Novo dataset';
    case 'WIDGET':
      return 'Novo widget';
    case 'TEMPLATE_INSTALL':
      return 'Instalar template';
    default:
      return kind;
  }
}

export interface ProposalReviewDialogProps {
  open: boolean;
  proposal: WorkspaceProposal | null;
  workspaces: WorkspaceSummary[];
  loading?: boolean;
  submitting?: boolean;
  error?: string | null;
  onClose: () => void;
  onConfirm: (workspaceId?: number) => void;
  onDiscard: () => void;
}

export function ProposalReviewDialog({
  open,
  proposal,
  workspaces,
  loading = false,
  submitting = false,
  error = null,
  onClose,
  onConfirm,
  onDiscard,
}: ProposalReviewDialogProps) {
  const [selectedWorkspaceId, setSelectedWorkspaceId] = useState<number | ''>('');

  useEffect(() => {
    if (workspaces.length > 0) {
      setSelectedWorkspaceId(workspaces[0].id);
    } else {
      setSelectedWorkspaceId('');
    }
  }, [workspaces, proposal?.id]);

  const needsWorkspace = proposal?.payload.kind === 'TEMPLATE_INSTALL';

  const handleConfirm = () => {
    if (needsWorkspace && selectedWorkspaceId === '') {
      return;
    }
    onConfirm(needsWorkspace ? Number(selectedWorkspaceId) : undefined);
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth aria-labelledby="proposal-review-title">
      <DialogTitle id="proposal-review-title">Revisar proposta</DialogTitle>
      <DialogContent>
        {loading && (
          <Stack alignItems="center" py={3}>
            <CircularProgress aria-label="Carregando proposta" />
          </Stack>
        )}

        {!loading && error && (
          <Alert severity="error" role="alert">
            {error}
          </Alert>
        )}

        {!loading && !error && proposal && (
          <Stack spacing={2} sx={{ pt: 1 }}>
            <Typography variant="body2" color="text.secondary">
              Status: {proposal.status}
            </Typography>
            <Typography variant="subtitle1">
              {kindLabel(proposal.payload.kind)}
              {proposal.payload.nome ? `: ${proposal.payload.nome}` : ''}
            </Typography>
            {proposal.payload.descricao && (
              <Typography variant="body2">{proposal.payload.descricao}</Typography>
            )}
            {proposal.payload.kind === 'DATASET' && proposal.payload.campos && proposal.payload.campos.length > 0 && (
              <Typography variant="body2" component="div">
                Campos sugeridos:
                <ul>
                  {proposal.payload.campos.map((campo) => (
                    <li key={campo.nome}>
                      {campo.nome} ({campo.tipo})
                    </li>
                  ))}
                </ul>
              </Typography>
            )}
            {proposal.payload.kind === 'WIDGET' && (
              <Typography variant="body2">
                Tipo: {proposal.payload.tipoWidget ?? 'KPI'}
                {proposal.payload.formula ? ` · Fórmula: ${proposal.payload.formula}` : ''}
              </Typography>
            )}
            {needsWorkspace && (
              <FormControl fullWidth required>
                <InputLabel id="proposal-workspace-label">Workspace destino</InputLabel>
                <Select
                  labelId="proposal-workspace-label"
                  label="Workspace destino"
                  value={selectedWorkspaceId}
                  onChange={(event) => setSelectedWorkspaceId(event.target.value as number | '')}
                >
                  {workspaces.map((workspace) => (
                    <MenuItem key={workspace.id} value={workspace.id}>
                      {workspace.nome}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            )}
            <Alert severity="info" role="status">
              Nada será aplicado até você confirmar.
            </Alert>
          </Stack>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={submitting}>
          Fechar
        </Button>
        <Button
          onClick={onDiscard}
          color="inherit"
          disabled={loading || submitting || !proposal}
        >
          Descartar
        </Button>
        <Button
          variant="contained"
          onClick={handleConfirm}
          disabled={loading || submitting || !proposal || (needsWorkspace && selectedWorkspaceId === '')}
        >
          Confirmar
        </Button>
      </DialogActions>
    </Dialog>
  );
}
