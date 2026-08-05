import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  IconButton,
  MenuItem,
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
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import {
  createDatasetRow,
  deleteDatasetRow,
  getDataset,
  listDatasetRows,
  updateDatasetRow,
  updateDatasetSchema,
  WorkspaceApiError,
} from '../../services/workspaceService';
import type { DatasetDefinition, DatasetFieldSchema, DatasetFieldType, DatasetRow } from './types';

const FIELD_TYPES: DatasetFieldType[] = ['NUMERO', 'TEXTO', 'DATA', 'MOEDA', 'REFERENCIA'];

interface FieldEditorRow extends DatasetFieldSchema {
  key: string;
}

function newFieldKey(): string {
  return `field-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
}

function toEditorFields(campos: DatasetFieldSchema[]): FieldEditorRow[] {
  return campos.map((campo) => ({ ...campo, key: newFieldKey() }));
}

function stripEditorFields(fields: FieldEditorRow[]): DatasetFieldSchema[] {
  return fields.map(({ nome, tipo, referenciaEntidade, obrigatorio }) => ({
    nome,
    tipo,
    referenciaEntidade,
    obrigatorio,
  }));
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

  const load = useCallback(async () => {
    if (!datasetId || Number.isNaN(datasetId)) {
      return;
    }
    setLoading(true);
    try {
      const [ds, rowList] = await Promise.all([getDataset(datasetId), listDatasetRows(datasetId)]);
      setDataset(ds);
      setFields(toEditorFields(ds.campos));
      setRows(rowList);
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
    setFields((current) => [
      ...current,
      { key: newFieldKey(), nome: `campo_${current.length + 1}`, tipo: 'TEXTO' },
    ]);
  };

  const handleRemoveField = (key: string) => {
    setFields((current) => current.filter((field) => field.key !== key));
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
  };

  if (loading) {
    return <Typography role="status">Carregando dataset…</Typography>;
  }

  if (!dataset) {
    return <Alert severity="error">Dataset não encontrado</Alert>;
  }

  return (
    <Box>
      <Typography variant="h5" gutterBottom>
        Editor: {dataset.nome}
      </Typography>

      <Paper sx={{ p: 2, mb: 3 }}>
        <Typography variant="h6" gutterBottom>
          Esquema de campos
        </Typography>
        {schemaError && (
          <Alert severity="warning" sx={{ mb: 2 }}>
            {schemaError}
          </Alert>
        )}
        <Stack spacing={1}>
          {fields.map((field) => (
            <Stack key={field.key} direction="row" spacing={1} alignItems="center">
              <TextField
                label="Nome do campo"
                size="small"
                value={field.nome}
                onChange={(event) => handleFieldChange(field.key, { nome: event.target.value })}
              />
              <TextField
                select
                label="Tipo"
                size="small"
                value={field.tipo}
                onChange={(event) =>
                  handleFieldChange(field.key, { tipo: event.target.value as DatasetFieldType })
                }
                sx={{ minWidth: 140 }}
              >
                {FIELD_TYPES.map((tipo) => (
                  <MenuItem key={tipo} value={tipo}>
                    {tipo}
                  </MenuItem>
                ))}
              </TextField>
              <IconButton aria-label={`Remover campo ${field.nome}`} onClick={() => handleRemoveField(field.key)}>
                <DeleteOutlineIcon />
              </IconButton>
            </Stack>
          ))}
        </Stack>
        <Stack direction="row" spacing={1} mt={2}>
          <Button startIcon={<AddIcon />} onClick={handleAddField}>
            Adicionar campo
          </Button>
          <Button variant="contained" onClick={() => void saveSchema()} disabled={savingSchema}>
            Salvar esquema
          </Button>
        </Stack>
      </Paper>

      <Paper sx={{ p: 2 }}>
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
                  {fields.map((field) => (
                    <TableCell key={field.key}>
                      <TextField
                        size="small"
                        label={field.nome}
                        value={rowDrafts[row.id]?.[field.nome] ?? ''}
                        error={Boolean(rowErrors[row.id]?.[field.nome])}
                        helperText={rowErrors[row.id]?.[field.nome]}
                        onChange={(event) => handleRowChange(row.id, field.nome, event.target.value)}
                      />
                    </TableCell>
                  ))}
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
    </Box>
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
