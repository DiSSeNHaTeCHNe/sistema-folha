import { useState } from 'react';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  IconButton,
  InputLabel,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
  Select,
  TextField,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import type { WorkspaceSummary } from './types';

interface WorkspaceSwitcherProps {
  summaries: WorkspaceSummary[];
  activeWorkspaceId: number | null;
  onSwitch: (workspaceId: number) => void;
  onCreate: (nome: string) => Promise<void>;
  onDelete: (workspaceId: number) => Promise<void>;
  disabled?: boolean;
}

export function WorkspaceSwitcher({
  summaries,
  activeWorkspaceId,
  onSwitch,
  onCreate,
  onDelete,
  disabled = false,
}: WorkspaceSwitcherProps) {
  const [createOpen, setCreateOpen] = useState(false);
  const [deleteAnchor, setDeleteAnchor] = useState<null | HTMLElement>(null);
  const [nome, setNome] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleCreate = async () => {
    if (!nome.trim()) {
      return;
    }
    setSubmitting(true);
    try {
      await onCreate(nome.trim());
      setNome('');
      setCreateOpen(false);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (activeWorkspaceId == null) {
      return;
    }
    setSubmitting(true);
    try {
      await onDelete(activeWorkspaceId);
      setDeleteAnchor(null);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box display="flex" alignItems="center" gap={1} flexWrap="wrap">
      <FormControl size="small" sx={{ minWidth: 220 }} disabled={disabled || summaries.length === 0}>
        <InputLabel id="workspace-switcher-label">Workspace</InputLabel>
        <Select
          labelId="workspace-switcher-label"
          label="Workspace"
          value={activeWorkspaceId ?? ''}
          onChange={(event) => onSwitch(Number(event.target.value))}
        >
          {summaries.map((item) => (
            <MenuItem key={item.id} value={item.id}>
              {item.nome}
            </MenuItem>
          ))}
        </Select>
      </FormControl>

      <Button
        variant="outlined"
        size="small"
        startIcon={<AddIcon />}
        onClick={() => setCreateOpen(true)}
        disabled={disabled}
      >
        Novo workspace
      </Button>

      {activeWorkspaceId != null && summaries.length > 1 && (
        <IconButton
          aria-label="Excluir workspace atual"
          size="small"
          disabled={disabled || submitting}
          onClick={(event) => setDeleteAnchor(event.currentTarget)}
        >
          <DeleteOutlineIcon fontSize="small" />
        </IconButton>
      )}

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} aria-labelledby="create-workspace-title">
        <DialogTitle id="create-workspace-title">Novo workspace</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Nome do workspace"
            fullWidth
            value={nome}
            onChange={(event) => setNome(event.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Cancelar</Button>
          <Button onClick={() => void handleCreate()} disabled={submitting || !nome.trim()}>
            Criar
          </Button>
        </DialogActions>
      </Dialog>

      <Menu anchorEl={deleteAnchor} open={Boolean(deleteAnchor)} onClose={() => setDeleteAnchor(null)}>
        <MenuItem disabled>
          <ListItemText primary="Excluir workspace atual?" secondary="Datasets não serão removidos." />
        </MenuItem>
        <MenuItem onClick={() => void handleDelete()} disabled={submitting}>
          <ListItemIcon>
            <DeleteOutlineIcon fontSize="small" />
          </ListItemIcon>
          Confirmar exclusão
        </MenuItem>
      </Menu>
    </Box>
  );
}

interface WorkspaceEmptyStateProps {
  onCreate?: (nome: string) => Promise<void>;
}

export function WorkspaceEmptyState({ onCreate }: WorkspaceEmptyStateProps) {
  const [createOpen, setCreateOpen] = useState(false);
  const [nome, setNome] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleCreate = async () => {
    if (!nome.trim() || !onCreate) {
      return;
    }
    setSubmitting(true);
    try {
      await onCreate(nome.trim());
      setNome('');
      setCreateOpen(false);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box
      display="flex"
      flexDirection="column"
      alignItems="center"
      justifyContent="center"
      minHeight={240}
      role="status"
      aria-label="Nenhum workspace configurado"
    >
      <Typography variant="h6" gutterBottom>
        Nenhum workspace ainda
      </Typography>
      <Typography color="text.secondary" align="center" mb={2}>
        Crie um workspace para organizar widgets personalizados.
      </Typography>
      {onCreate && (
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
          Criar workspace
        </Button>
      )}

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} aria-labelledby="empty-create-workspace-title">
        <DialogTitle id="empty-create-workspace-title">Novo workspace</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Nome do workspace"
            fullWidth
            value={nome}
            onChange={(event) => setNome(event.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Cancelar</Button>
          <Button onClick={() => void handleCreate()} disabled={submitting || !nome.trim()}>
            Criar
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
