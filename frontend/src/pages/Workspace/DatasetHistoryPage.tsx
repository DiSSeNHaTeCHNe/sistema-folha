import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Box,
  Button,
  CircularProgress,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Stack,
  Typography,
} from '@mui/material';
import { getDataset, listDatasetAudit, listDatasetRowAudit } from '../../services/workspaceService';
import type { DatasetAuditTimelineEntry, DatasetRowAuditEntry } from './types';
import { WorkspacePageShell } from './components/WorkspacePageShell';
import { InfoBanner } from './components/InfoBanner';
import { StatusChip } from './components/StatusChip';

function formatTimestamp(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString('pt-BR');
}

function acaoLabel(acao: DatasetAuditTimelineEntry['acao']): string {
  switch (acao) {
    case 'CREATE':
      return 'Criação';
    case 'UPDATE':
      return 'Edição';
    case 'DELETE':
      return 'Exclusão';
    default:
      return acao;
  }
}

export default function DatasetHistoryPage() {
  const { id: idParam } = useParams();
  const navigate = useNavigate();
  const datasetId = Number(idParam);
  const invalidId = Number.isNaN(datasetId);

  const [datasetName, setDatasetName] = useState('');
  const [timeline, setTimeline] = useState<DatasetAuditTimelineEntry[]>([]);
  const [selectedRowId, setSelectedRowId] = useState<number | null>(null);
  const [rowAudit, setRowAudit] = useState<DatasetRowAuditEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [rowLoading, setRowLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadTimeline = useCallback(async () => {
    if (invalidId) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const [dataset, audit] = await Promise.all([getDataset(datasetId), listDatasetAudit(datasetId)]);
      setDatasetName(dataset.nome);
      setTimeline(
        [...audit].sort(
          (a, b) => new Date(b.dataEvento).getTime() - new Date(a.dataEvento).getTime(),
        ),
      );
    } catch {
      setError('Erro ao carregar histórico do dataset');
      setTimeline([]);
    } finally {
      setLoading(false);
    }
  }, [datasetId, invalidId]);

  useEffect(() => {
    void loadTimeline();
  }, [loadTimeline]);

  const loadRowAudit = useCallback(
    async (rowId: number) => {
      setSelectedRowId(rowId);
      setRowLoading(true);
      try {
        const entries = await listDatasetRowAudit(datasetId, rowId);
        setRowAudit(entries);
      } catch {
        setRowAudit([]);
      } finally {
        setRowLoading(false);
      }
    },
    [datasetId],
  );

  const uniqueRowIds = useMemo(
    () => [...new Set(timeline.map((entry) => entry.rowId))],
    [timeline],
  );

  if (invalidId) {
    return (
      <WorkspacePageShell title="Histórico inválido">
        <InfoBanner variant="danger">Identificador de dataset inválido.</InfoBanner>
        <Button sx={{ mt: 2 }} onClick={() => navigate('/workspace/datasets')}>
          Voltar aos datasets
        </Button>
      </WorkspacePageShell>
    );
  }

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight={400}>
        <CircularProgress aria-label="Carregando histórico do dataset" />
      </Box>
    );
  }

  return (
    <WorkspacePageShell
      title={`Histórico — ${datasetName || 'Dataset'}`}
      subtitle="Timeline de alterações e drill-down por linha"
      actions={
        <Button variant="outlined" onClick={() => navigate(`/workspace/datasets/${datasetId}`)}>
          Voltar ao editor
        </Button>
      }
    >
      {error ? <InfoBanner variant="danger">{error}</InfoBanner> : null}

      {!error && timeline.length === 0 ? (
        <InfoBanner variant="info" title="Nenhuma alteração registrada">
          Este dataset ainda não possui histórico de criação, edição ou exclusão de linhas.
        </InfoBanner>
      ) : null}

      {!error && timeline.length > 0 ? (
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={3}>
          <Box flex={1}>
            <Typography variant="subtitle1" component="h2" gutterBottom>
              Timeline
            </Typography>
            <List aria-label="Timeline de auditoria">
              {timeline.map((entry, index) => (
                <ListItem key={`${entry.rowId}-${entry.dataEvento}-${index}`} disablePadding divider>
                  <ListItemButton
                    selected={selectedRowId === entry.rowId}
                    onClick={() => void loadRowAudit(entry.rowId)}
                    aria-label={`Ver histórico da linha ${entry.rowId}`}
                  >
                    <ListItemText
                      primary={`Linha ${entry.rowId} — ${acaoLabel(entry.acao)}`}
                      secondary={`${formatTimestamp(entry.dataEvento)} · autor #${entry.autorUsuarioId}${
                        entry.resumo ? ` · ${entry.resumo}` : ''
                      }`}
                    />
                  </ListItemButton>
                </ListItem>
              ))}
            </List>
          </Box>

          <Box flex={1} sx={{ bgcolor: 'background.paper', borderRadius: 2, p: 2 }}>
            <Typography variant="subtitle1" component="h2" gutterBottom>
              Drill-down por linha
            </Typography>
            {selectedRowId == null ? (
              <Typography variant="body2" color="text.secondary" role="status">
                Selecione uma entrada da timeline para ver o histórico detalhado da linha.
              </Typography>
            ) : rowLoading ? (
              <CircularProgress size={28} aria-label="Carregando histórico da linha" />
            ) : rowAudit.length === 0 ? (
              <Typography variant="body2" color="text.secondary" role="status">
                Nenhum detalhe adicional para a linha {selectedRowId}.
              </Typography>
            ) : (
              <List dense aria-label={`Histórico da linha ${selectedRowId}`}>
                {rowAudit.map((entry) => (
                  <ListItem key={entry.id} disablePadding sx={{ py: 0.5 }}>
                    <ListItemText
                      primary={
                        <Stack direction="row" spacing={1} alignItems="center">
                          <StatusChip variant="info" label={acaoLabel(entry.acao)} />
                          <Typography variant="body2">{formatTimestamp(entry.dataEvento)}</Typography>
                        </Stack>
                      }
                      secondary={`Autor #${entry.autorUsuarioId}`}
                    />
                  </ListItem>
                ))}
              </List>
            )}

            {uniqueRowIds.length > 0 ? (
              <Box mt={2}>
                <Typography variant="caption" color="text.secondary">
                  Linhas com alterações: {uniqueRowIds.join(', ')}
                </Typography>
              </Box>
            ) : null}
          </Box>
        </Stack>
      ) : null}
    </WorkspacePageShell>
  );
}
