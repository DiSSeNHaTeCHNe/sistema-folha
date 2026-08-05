import { Button, Dialog, DialogActions, DialogContent, DialogTitle } from '@mui/material';
import type { WorkspaceProposal, WorkspaceSummary } from './types';
import { ProposalReviewPanel } from './components/ProposalReviewPanel';

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
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth aria-labelledby="proposal-review-title">
      <DialogTitle id="proposal-review-title">Revisar proposta</DialogTitle>
      <DialogContent>
        <ProposalReviewPanel
          proposal={proposal}
          workspaces={workspaces}
          loading={loading}
          submitting={submitting}
          error={error}
          showHeading={false}
          onConfirm={onConfirm}
          onDiscard={onDiscard}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={submitting}>
          Fechar
        </Button>
      </DialogActions>
    </Dialog>
  );
}
