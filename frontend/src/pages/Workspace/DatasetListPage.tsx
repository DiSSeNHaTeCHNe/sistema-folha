import { useCallback, useEffect, useState } from 'react';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Link,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { createDataset, listDatasets } from '../../services/workspaceService';
import type { DatasetSummary } from './types';

export default function DatasetListPage() {
  const navigate = useNavigate();
  const [datasets, setDatasets] = useState<DatasetSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [nome, setNome] = useState('');
  const [creating, setCreating] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setDatasets(await listDatasets());
    } catch {
      setError('Erro ao carregar datasets');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const handleCreate = async () => {
    if (!nome.trim()) {
      return;
    }
    setCreating(true);
    try {
      const created = await createDataset(nome.trim(), [{ nome: 'valor', tipo: 'NUMERO' }]);
      setCreateOpen(false);
      setNome('');
      navigate(`/workspace/datasets/${created.id}`);
    } catch {
      setError('Erro ao criar dataset');
    } finally {
      setCreating(false);
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight={240}>
        <CircularProgress aria-label="Carregando datasets" />
      </Box>
    );
  }

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" mb={3} flexWrap="wrap" gap={1}>
        <Typography variant="h5" component="h1">
          Datasets
        </Typography>
        <Stack direction="row" spacing={1}>
          <Button component={RouterLink} to="/workspace" startIcon={<ArrowBackIcon />} variant="outlined">
            Voltar ao workspace
          </Button>
          <Button variant="contained" startIcon={<AddIcon />} onClick={() => setCreateOpen(true)}>
            Novo dataset
          </Button>
        </Stack>
      </Stack>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {datasets.length === 0 ? (
        <Alert severity="info" role="status">
          Nenhum dataset ainda. Crie um para começar.
        </Alert>
      ) : (
        <List>
          {datasets.map((dataset) => (
            <ListItem key={dataset.id} disablePadding>
              <ListItemButton component={RouterLink} to={`/workspace/datasets/${dataset.id}`}>
                <ListItemText
                  primary={dataset.nome}
                  secondary={`${dataset.totalCampos} campos · ${dataset.totalLinhas} linhas`}
                />
              </ListItemButton>
            </ListItem>
          ))}
        </List>
      )}

      <Typography variant="body2" color="text.secondary" mt={2}>
        <Link component={RouterLink} to="/workspace">
          Workspace
        </Link>
      </Typography>

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} aria-labelledby="create-dataset-title">
        <DialogTitle id="create-dataset-title">Novo dataset</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Nome do dataset"
            fullWidth
            value={nome}
            onChange={(event) => setNome(event.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Cancelar</Button>
          <Button onClick={() => void handleCreate()} disabled={creating || !nome.trim()}>
            Criar
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
