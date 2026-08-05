import { useCallback, useEffect, useState } from 'react';
import { Link as RouterLink, useParams } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  FormControlLabel,
  Grid,
  IconButton,
  Link,
  Paper,
  Stack,
  Tab,
  Tabs,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import {
  createDatasetRow,
  deleteDatasetRow,
  getDataset,
  listDatasetRows,
  listDatasets,
  updateDatasetRow,
  updateDatasetSchema,
  WorkspaceApiError,
} from '../../services/workspaceService';
import { WorkspacePageShell } from './components/WorkspacePageShell';
import { FieldTypePanel } from './components/FieldTypePanel';
import { QuotaProgressBar } from './components/QuotaProgressBar';
import { InlineCellError } from './components/InlineCellError';
import { WORKSPACE_LIMITS } from './workspaceLimits';
import { colors } from './workspaceTheme';
import type { DatasetDefinition, DatasetFieldSchema, DatasetRow } from './types';

interface FieldEditorRow extends DatasetFieldSchema {
  key: string;
  observacao?: string;
}

function newFieldKey(): string {
  return `field-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
}

function toEditorFields(campos: DatasetFieldSchema[]): FieldEditorRow[] {
  return campos.map((campo) => ({
    ...campo,
    key: newFieldKey(),
    observacao: campo.observacao ?? '',
  }));
}

function stripEditorFields(fields: FieldEditorRow[]): DatasetFieldSchema[] {
  return fields.map(({ nome, tipo, referenciaEntidade, obrigatorio, observacao }) => {
    const trimmed = observacao?.trim();
    const payload: DatasetFieldSchema = {
      nome,
      tipo,
      referenciaEntidade,
      obrigatorio,
    };
    if (trimmed) {
      payload.observacao = trimmed;
    }
    return payload;
  });
}

interface DatasetEditorPageProps {
  datasetId?: number;
}

export function DatasetEditorPage({ datasetId: datasetIdProp }: DatasetEditorPageProps = {}) {
  const params = useParams();
  const datasetId = datasetIdProp ?? Number(params.id);

  const [dataset, setDataset] = useState<DatasetDefinition | null>(null);
  const [fields, setFields] = useState<FieldEditorRow[]>([]);
  const [rows, setRows] = useState<DatasetRow[]>([]);
  const [rowDrafts, setRowDrafts] = useState<Record<number, Record<string, string>>>({});
  const [rowErrors, setRowErrors] = useState<Record<number, Record<string, string>>>({});
  const [schemaError, setSchemaError] = useState<string | null>(null);
  const [confirmRemovalOpen, setConfirmRemovalOpen] = useState(false);
  const [loading, setLoading] = useState(true);
  const [savingSchema, setSavingSchema] = useState(false);
  const [selectedFieldKey, setSelectedFieldKey] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState(0);
  const [totalDatasets, setTotalDatasets] = useState(0);

  const selectedField = fields.find((field) => field.key === selectedFieldKey) ?? null;

  const load = useCallback(async () => {
    if (!datasetId || Number.isNaN(datasetId)) {
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const [ds, rowList, datasetList] = await Promise.all([
        getDataset(datasetId),
        listDatasetRows(datasetId),
        listDatasets().catch(() => []),
      ]);
      const editorFields = toEditorFields(ds.campos);
      setDataset(ds);
      setFields(editorFields);
      setSelectedFieldKey(editorFields[0]?.key ?? null);
      setRows(rowList);
      setTotalDatasets(datasetList.length);
      setRowDrafts(
        Object.fromEntries(rowList.map((row) => [row.id, stringifyRowValues(row.valores)])),
      );
    } catch {
      setDataset(null);
    } finally {
      setLoading(false);
    }
  }, [datasetId]);

  useEffect(() => {
    void load();
  }, [load]);

  const handleFieldChange = (key: string, patch: Partial<FieldEditorRow>) => {
    setFields((current) => current.map((field) => (field.key === key ? { ...field, ...patch } : field)));
  };

  const handleAddField = () => {
    const key = newFieldKey();
    setFields((current) => [
      ...current,
      { key, nome: `campo_${current.length + 1}`, tipo: 'TEXTO', obrigatorio: false, observacao: '' },
    ]);
    setSelectedFieldKey(key);
  };

  const handleRemoveField = (key: string) => {
    setFields((current) => current.filter((field) => field.key !== key));
    if (selectedFieldKey === key) {
      setSelectedFieldKey(null);
    }
  };

  const saveSchema = async (confirmarRemocao = false) => {
    if (!dataset) {
      return;
    }
    setSavingSchema(true);
    setSchemaError(null);
    try {
      const updated = await updateDatasetSchema(
        dataset.id,
        stripEditorFields(fields),
        dataset.schemaVersion,
        confirmarRemocao,
      );
      setDataset(updated);
      setFields(toEditorFields(updated.campos));
      setConfirmRemovalOpen(false);
    } catch (error) {
      if (error instanceof WorkspaceApiError && error.status === 409) {
        setConfirmRemovalOpen(true);
        setSchemaError(error.message);
      } else {
        setSchemaError(error instanceof WorkspaceApiError ? error.message : 'Erro ao salvar esquema');
      }
    } finally {
      setSavingSchema(false);
    }
  };

  const handleRowChange = (rowId: number, fieldName: string, value: string) => {
    setRowDrafts((current) => ({
      ...current,
      [rowId]: { ...current[rowId], [fieldName]: value },
    }));
    setRowErrors((current) => {
      const rowErr = { ...current[rowId] };
      delete rowErr[fieldName];
      return { ...current, [rowId]: rowErr };
    });
  };

  const saveRow = async (rowId: number) => {
    if (!dataset) {
      return;
    }
    const draft = rowDrafts[rowId] ?? {};
    const valores = parseRowValues(fields, draft);
    try {
      const updated = await updateDatasetRow(dataset.id, rowId, valores);
      setRows((current) => current.map((row) => (row.id === rowId ? updated : row)));
      setRowErrors((current) => ({ ...current, [rowId]: {} }));
    } catch (error) {
      if (error instanceof WorkspaceApiError && error.errors) {
        const mapped: Record<string, string> = {};
        for (const item of error.errors) {
          const field = item.field.replace(/^valores\./, '');
          mapped[field] = item.message;
        }
        setRowErrors((current) => ({ ...current, [rowId]: mapped }));
      }
    }
  };

  const addRow = async () => {
    if (!dataset) {
      return;
    }
    const emptyValues = Object.fromEntries(fields.map((field) => [field.nome, '']));
    try {
      const created = await createDatasetRow(dataset.id, parseRowValues(fields, emptyValues));
      setRows((current) => [...current, created]);
      setRowDrafts((current) => ({ ...current, [created.id]: emptyValues }));
      setDataset((current) =>
        current ? { ...current, totalLinhas: current.totalLinhas + 1 } : current,
      );
    } catch (error) {
      if (error instanceof WorkspaceApiError) {
        setSchemaError(error.message);
      }
    }
  };

  const removeRow = async (rowId: number) => {
    if (!dataset) {
      return;
    }
    await deleteDatasetRow(dataset.id, rowId);
    setRows((current) => current.filter((row) => row.id !== rowId));
    setDataset((current) =>
      current ? { ...current, totalLinhas: Math.max(0, current.totalLinhas - 1) } : current,
    );
  };

  if (loading) {
    return <Typography role="status">Carregando dataset…</Typography>;
  }

  if (!dataset) {
    return <Alert severity="error">Dataset não encontrado</Alert>;
  }

  return (
    <WorkspacePageShell
      title={`Editor: ${dataset.nome}`}
      subtitle="Esquema e linhas do dataset"
      actions={
        <Button component={RouterLink} to={`/workspace/datasets/${dataset.id}/historico`} variant="outlined">
          Histórico
        </Button>
      }
    >
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} mb={3}>
        <Box flex={1}>
          <QuotaProgressBar
            label="Datasets do usuário"
            current={totalDatasets}
            max={WORKSPACE_LIMITS.MAX_DATASETS_PER_USER}
          />
        </Box>
        <Box flex={1}>
          <QuotaProgressBar
            label="Linhas deste dataset"
            current={dataset.totalLinhas}
            max={WORKSPACE_LIMITS.MAX_ROWS_PER_DATASET}
          />
        </Box>
      </Stack>

      <Tabs value={activeTab} onChange={(_e, value) => setActiveTab(value)} sx={{ mb: 2 }}>
        <Tab label="Esquema" />
        <Tab label="Linhas" />
      </Tabs>

      {activeTab === 0 && (
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 8 }}>
            <Paper sx={{ p: 2, borderColor: colors.line }} variant="outlined">
              <Typography variant="h6" gutterBottom>
                Campos do esquema
              </Typography>
              {schemaError ? (
                <Alert severity="warning" sx={{ mb: 2 }}>
                  {schemaError}
                </Alert>
              ) : null}
              <Table size="small" aria-label="Campos do esquema">
                <TableHead>
                  <TableRow>
                    <TableCell>Campo</TableCell>
                    <TableCell>Tipo</TableCell>
                    <TableCell>Obrigatório</TableCell>
                    <TableCell>Observação</TableCell>
                    <TableCell aria-label="Ações" />
                  </TableRow>
                </TableHead>
                <TableBody>
                  {fields.map((field) => (
                    <TableRow
                      key={field.key}
                      selected={selectedFieldKey === field.key}
                      onClick={() => setSelectedFieldKey(field.key)}
                    >
                      <TableCell>
                        <TextField
                          label="Campo"
                          size="small"
                          value={field.nome}
                          onChange={(event) => handleFieldChange(field.key, { nome: event.target.value })}
                          onClick={(event) => event.stopPropagation()}
                        />
                      </TableCell>
                      <TableCell>{field.tipo}</TableCell>
                      <TableCell>
                        <FormControlLabel
                          control={
                            <Checkbox
                              checked={Boolean(field.obrigatorio)}
                              onChange={(event) =>
                                handleFieldChange(field.key, { obrigatorio: event.target.checked })
                              }
                              onClick={(event) => event.stopPropagation()}
                            />
                          }
                          label="Obrigatório"
                        />
                      </TableCell>
                      <TableCell>
                        <TextField
                          label="Observação"
                          size="small"
                          value={field.observacao ?? ''}
                          onChange={(event) =>
                            handleFieldChange(field.key, { observacao: event.target.value })
                          }
                          onClick={(event) => event.stopPropagation()}
                        />
                      </TableCell>
                      <TableCell>
                        <IconButton
                          aria-label={`Remover campo ${field.nome}`}
                          onClick={(event) => {
                            event.stopPropagation();
                            handleRemoveField(field.key);
                          }}
                        >
                          <DeleteOutlineIcon />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <Stack direction="row" spacing={1} mt={2}>
                <Button startIcon={<AddIcon />} onClick={handleAddField}>
                  + Adicionar campo
                </Button>
                <Button variant="contained" onClick={() => void saveSchema()} disabled={savingSchema}>
                  Salvar esquema
                </Button>
              </Stack>
            </Paper>
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <FieldTypePanel
              selectedType={selectedField?.tipo ?? null}
              onSelect={(tipo) => {
                if (selectedFieldKey) {
                  handleFieldChange(selectedFieldKey, { tipo });
                }
              }}
            />
          </Grid>
        </Grid>
      )}

      {activeTab === 1 && (
        <Paper sx={{ p: 2, borderColor: colors.line }} variant="outlined">
          <Stack direction="row" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h6">Linhas ({rows.length})</Typography>
            <Button startIcon={<AddIcon />} onClick={() => void addRow()} disabled={fields.length === 0}>
              Adicionar linha
            </Button>
          </Stack>
          {fields.length === 0 ? (
            <Alert severity="info">Defina ao menos um campo no esquema.</Alert>
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  {fields.map((field) => (
                    <TableCell key={field.key}>{field.nome}</TableCell>
                  ))}
                  <TableCell>Ações</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => (
                  <TableRow key={row.id}>
                    {fields.map((field) => {
                      const errorMessage = rowErrors[row.id]?.[field.nome];
                      return (
                        <TableCell key={field.key}>
                          <TextField
                            size="small"
                            label={field.nome}
                            value={rowDrafts[row.id]?.[field.nome] ?? ''}
                            error={Boolean(errorMessage)}
                            onChange={(event) => handleRowChange(row.id, field.nome, event.target.value)}
                          />
                          {errorMessage ? <InlineCellError message={errorMessage} /> : null}
                        </TableCell>
                      );
                    })}
                    <TableCell>
                      <Button size="small" onClick={() => void saveRow(row.id)}>
                        Salvar linha
                      </Button>
                      <IconButton aria-label={`Excluir linha ${row.id}`} onClick={() => void removeRow(row.id)}>
                        <DeleteOutlineIcon />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </Paper>
      )}

      <Typography variant="body2" color="text.secondary" mt={2}>
        <Link component={RouterLink} to="/workspace/datasets">
          Voltar aos datasets
        </Link>
      </Typography>

      <Dialog open={confirmRemovalOpen} onClose={() => setConfirmRemovalOpen(false)} aria-labelledby="confirm-schema-title">
        <DialogTitle id="confirm-schema-title">Remover campos com dados?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Existem linhas preenchidas para campos removidos. Confirme para apagar os valores associados.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmRemovalOpen(false)}>Cancelar</Button>
          <Button color="warning" onClick={() => void saveSchema(true)} autoFocus>
            Confirmar remoção
          </Button>
        </DialogActions>
      </Dialog>
    </WorkspacePageShell>
  );
}

function stringifyRowValues(valores: Record<string, unknown>): Record<string, string> {
  return Object.fromEntries(
    Object.entries(valores).map(([key, value]) => [key, value == null ? '' : String(value)]),
  );
}

function parseRowValues(fields: FieldEditorRow[], draft: Record<string, string>): Record<string, unknown> {
  const result: Record<string, unknown> = {};
  for (const field of fields) {
    const raw = draft[field.nome] ?? '';
    if (raw === '') {
      continue;
    }
    switch (field.tipo) {
      case 'NUMERO':
        result[field.nome] = Number(raw);
        break;
      case 'MOEDA':
        result[field.nome] = raw.replace(',', '.');
        break;
      default:
        result[field.nome] = raw;
    }
  }
  return result;
}

export default DatasetEditorPage;
