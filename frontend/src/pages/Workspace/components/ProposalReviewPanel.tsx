import {
  Button,
  CircularProgress,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Typography,
} from '@mui/material';
import { useEffect, useState } from 'react';
import type { WorkspaceProposal, WorkspaceSummary } from '../types';
import { InfoBanner } from './InfoBanner';
import { StatusChip } from './StatusChip';

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

function formatExpiration(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString('pt-BR');
}

export interface ProposalReviewPanelProps {
  proposal: WorkspaceProposal | null;
  workspaces: WorkspaceSummary[];
  loading?: boolean;
  submitting?: boolean;
  error?: string | null;
  showHeading?: boolean;
  onConfirm: (workspaceId?: number) => void;
  onDiscard: () => void;
}

export function ProposalReviewPanel({
  proposal,
  workspaces,
  loading = false,
  submitting = false,
  error = null,
  showHeading = true,
  onConfirm,
  onDiscard,
}: ProposalReviewPanelProps) {
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

  if (loading) {
    return (
      <Stack alignItems="center" py={3}>
        <CircularProgress aria-label="Carregando proposta" />
      </Stack>
    );
  }

  if (error) {
    return (
      <InfoBanner variant="danger" title="Erro">
        {error}
      </InfoBanner>
    );
  }

  if (!proposal) {
    return null;
  }

  return (
    <Stack spacing={2} aria-labelledby={showHeading ? 'proposal-review-heading' : undefined}>
      {showHeading ? (
        <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" useFlexGap>
          <Typography id="proposal-review-heading" variant="h6" component="h2">
            Revisar proposta
          </Typography>
          <StatusChip variant="ai" label={proposal.status} />
        </Stack>
      ) : (
        <StatusChip variant="ai" label={proposal.status} />
      )}

      {proposal.status === 'PENDENTE' && (
        <InfoBanner variant="ai" title="Expiração">
          Esta proposta expira em {formatExpiration(proposal.dataExpiracao)} (TTL de 72 horas).
        </InfoBanner>
      )}

      <Typography variant="subtitle1">
        {kindLabel(proposal.payload.kind)}
        {proposal.payload.nome ? `: ${proposal.payload.nome}` : ''}
      </Typography>

      {proposal.payload.descricao ? (
        <Typography variant="body2">{proposal.payload.descricao}</Typography>
      ) : null}

      {proposal.payload.kind === 'DATASET' &&
        proposal.payload.campos &&
        proposal.payload.campos.length > 0 && (
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

      <InfoBanner variant="info">Nada será aplicado até você confirmar.</InfoBanner>

      <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
        <Button
          onClick={onDiscard}
          color="inherit"
          disabled={submitting}
        >
          Descartar
        </Button>
        <Button
          variant="contained"
          onClick={handleConfirm}
          disabled={submitting || (needsWorkspace && selectedWorkspaceId === '')}
        >
          Confirmar
        </Button>
      </Stack>
    </Stack>
  );
}
