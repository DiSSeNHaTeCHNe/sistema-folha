import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardActions,
  CardContent,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import { QueryClientProvider } from '@tanstack/react-query';
import {
  createWorkspace,
  listDatasets,
  listWidgetDefinitions,
  listWorkspaces,
} from '../../services/workspaceService';
import { createDashboardQueryClient } from '../MeuDashboard/queryClient';
import { WorkspacePageShell } from './components/WorkspacePageShell';
import { QuotaProgressBar } from './components/QuotaProgressBar';
import { InfoBanner } from './components/InfoBanner';
import { WorkspaceEmptyState } from './WorkspaceSwitcher';
import { WORKSPACE_LIMITS } from './workspaceLimits';
import { colors } from './workspaceTheme';
import type { DatasetSummary, UserWidgetDefinition, WorkspaceSummary } from './types';

function countWidgetsUsingDataset(datasetId: number, definitions: UserWidgetDefinition[]): number {
  const idStr = String(datasetId);
  return definitions.filter((def) =>
    def.fontes.some((fonte) => fonte.kind === 'DATASET' && fonte.ref === idStr),
  ).length;
}

function WorkspaceHubContent() {
  const navigate = useNavigate();
  const [workspaces, setWorkspaces] = useState<WorkspaceSummary[]>([]);
  const [datasets, setDatasets] = useState<DatasetSummary[]>([]);
  const [widgetDefinitions, setWidgetDefinitions] = useState<UserWidgetDefinition[]>([]);
  const [workspacesLoading, setWorkspacesLoading] = useState(true);
  const [datasetsLoading, setDatasetsLoading] = useState(true);
  const [datasetsError, setDatasetsError] = useState<string | null>(null);
  const [workspacesError, setWorkspacesError] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [nome, setNome] = useState('');
  const [creating, setCreating] = useState(false);

  const loadWorkspaces = useCallback(async () => {
    setWorkspacesLoading(true);
    setWorkspacesError(null);
    try {
      setWorkspaces(await listWorkspaces());
    } catch {
      setWorkspacesError('Erro ao carregar workspaces');
      setWorkspaces([]);
    } finally {
      setWorkspacesLoading(false);
    }
  }, []);

  const loadDatasetsSection = useCallback(async () => {
    setDatasetsLoading(true);
    setDatasetsError(null);
    try {
      const [datasetList, definitions] = await Promise.all([
        listDatasets(),
        listWidgetDefinitions().catch(() => [] as UserWidgetDefinition[]),
      ]);
      setDatasets(datasetList);
      setWidgetDefinitions(definitions);
    } catch {
      setDatasetsError('Erro ao carregar datasets');
      setDatasets([]);
    } finally {
      setDatasetsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadWorkspaces();
    void loadDatasetsSection();
  }, [loadWorkspaces, loadDatasetsSection]);

  const datasetQuotaAtLimit = datasets.length >= WORKSPACE_LIMITS.MAX_DATASETS_PER_USER;

  const datasetUsageById = useMemo(
    () =>
      Object.fromEntries(
        datasets.map((ds) => [ds.id, countWidgetsUsingDataset(ds.id, widgetDefinitions)]),
      ),
    [datasets, widgetDefinitions],
  );

  const handleCreateWorkspace = async () => {
    if (!nome.trim()) {
      return;
    }
    setCreating(true);
    try {
      const created = await createWorkspace(nome.trim());
      setCreateOpen(false);
      setNome('');
      await loadWorkspaces();
      navigate(`/workspace/${created.id}`);
    } finally {
      setCreating(false);
    }
  };

  const handleOpenWorkspace = (workspaceId: number) => {
    navigate(`/workspace/${workspaceId}`);
  };

  const loading = workspacesLoading && datasetsLoading;

  if (loading && workspaces.length === 0 && datasets.length === 0) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight={400}>
        <CircularProgress size={60} aria-label="Carregando hub do Workspace" />
      </Box>
    );
  }

  return (
    <WorkspacePageShell
      title="Meus workspaces"
      subtitle="Painéis e conjuntos de dados em um só lugar"
      actions={
        workspaces.length > 0 ? (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setCreateOpen(true)}
            disabled={workspaces.length >= WORKSPACE_LIMITS.MAX_WORKSPACES_PER_USER}
          >
            Novo workspace
          </Button>
        ) : undefined
      }
    >
      {workspacesError ? (
        <InfoBanner variant="danger">{workspacesError}</InfoBanner>
      ) : null}

      {!workspacesLoading && workspaces.length === 0 ? (
        <WorkspaceEmptyState onCreate={async (workspaceNome) => {
          const created = await createWorkspace(workspaceNome);
          await loadWorkspaces();
          navigate(`/workspace/${created.id}`);
        }} />
      ) : (
        <Box
          display="grid"
          gridTemplateColumns={{ xs: '1fr', md: 'repeat(auto-fill, minmax(280px, 1fr))' }}
          gap={2}
          mb={4}
          aria-label="Cards de workspaces"
        >
          {workspaces.map((workspace) => (
            <Card
              key={workspace.id}
              variant="outlined"
              sx={{ borderColor: colors.line, bgcolor: colors.card }}
            >
              <CardContent>
                <Typography variant="h6" component="h2" sx={{ color: colors.navy }}>
                  {workspace.nome}
                </Typography>
                <Typography variant="body2" sx={{ color: colors.soft, mt: 1 }}>
                  {workspace.totalWidgets} widget{workspace.totalWidgets !== 1 ? 's' : ''}
                </Typography>
                <Typography variant="caption" sx={{ color: colors.soft, display: 'block', mt: 0.5 }}>
                  Última edição: —
                </Typography>
              </CardContent>
              <CardActions>
                <Button
                  size="small"
                  startIcon={<OpenInNewIcon />}
                  onClick={() => handleOpenWorkspace(workspace.id)}
                >
                  Abrir
                </Button>
              </CardActions>
            </Card>
          ))}
        </Box>
      )}

      <Paper sx={{ p: 2, borderColor: colors.line, bgcolor: colors.card }} variant="outlined">
        <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" alignItems={{ sm: 'center' }} spacing={2} mb={2}>
          <Typography variant="h6" component="h2" sx={{ color: colors.navy }}>
            Meus datasets
          </Typography>
          <Button
            variant="outlined"
            startIcon={<AddIcon />}
            onClick={() => navigate('/workspace/datasets')}
            disabled={datasetQuotaAtLimit}
          >
            Novo dataset
          </Button>
        </Stack>

        <Box mb={2} maxWidth={360}>
          <QuotaProgressBar
            label="Datasets"
            current={datasets.length}
            max={WORKSPACE_LIMITS.MAX_DATASETS_PER_USER}
          />
        </Box>

        {datasetsError ? (
          <InfoBanner variant="danger">{datasetsError}</InfoBanner>
        ) : datasetsLoading ? (
          <Typography role="status">Carregando datasets…</Typography>
        ) : datasets.length === 0 ? (
          <Typography role="status" sx={{ color: colors.soft }}>
            Nenhum dataset criado ainda.
          </Typography>
        ) : (
          <Table size="small" aria-label="Tabela resumo de datasets">
            <TableHead>
              <TableRow>
                <TableCell>Nome</TableCell>
                <TableCell>Campos</TableCell>
                <TableCell>Linhas</TableCell>
                <TableCell>Usado por</TableCell>
                <TableCell>Publicado</TableCell>
                <TableCell>Última alteração</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {datasets.map((dataset) => (
                <TableRow key={dataset.id}>
                  <TableCell>{dataset.nome}</TableCell>
                  <TableCell>{dataset.totalCampos}</TableCell>
                  <TableCell>{dataset.totalLinhas}</TableCell>
                  <TableCell>
                    {datasetUsageById[dataset.id] ?? 0} widget
                    {(datasetUsageById[dataset.id] ?? 0) !== 1 ? 's' : ''}
                  </TableCell>
                  <TableCell>—</TableCell>
                  <TableCell>—</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Paper>

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} aria-labelledby="hub-create-workspace-title">
        <DialogTitle id="hub-create-workspace-title">Novo workspace</DialogTitle>
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
          <Button onClick={() => void handleCreateWorkspace()} disabled={creating || !nome.trim()}>
            Criar
          </Button>
        </DialogActions>
      </Dialog>
    </WorkspacePageShell>
  );
}

export default function WorkspaceHubPage() {
  const queryClient = useMemo(() => createDashboardQueryClient(), []);
  return (
    <QueryClientProvider client={queryClient}>
      <WorkspaceHubContent />
    </QueryClientProvider>
  );
}
