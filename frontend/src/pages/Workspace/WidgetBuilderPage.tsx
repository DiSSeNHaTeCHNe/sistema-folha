import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Box,
  Button,
  CircularProgress,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import {
  createWidgetDefinition,
  getWorkspace,
  listDatasets,
  previewWidgetDefinition,
  saveWorkspaceLayout,
  validateFormula,
  WorkspaceApiError,
} from '../../services/workspaceService';
import type { DatasetSummary, UserWidgetTipo, WidgetSourceRef, WorkspaceWidgetData } from './types';
import { FormulaEditor } from './FormulaEditor';
import { WorkspacePageShell } from './components/WorkspacePageShell';
import { InfoBanner } from './components/InfoBanner';
import { DynamicKpiWidget } from './widgets/DynamicKpiWidget';
import { DynamicTableWidget } from './widgets/DynamicTableWidget';
import { DynamicChartWidget } from './widgets/DynamicChartWidget';
import { COL_SPAN_PRESETS } from './types';

const TIPOS: UserWidgetTipo[] = ['KPI', 'TABELA', 'GRAFICO_LINHA', 'GRAFICO_BARRA'];

const TIPO_LABELS: Record<UserWidgetTipo, string> = {
  KPI: 'KPI',
  TABELA: 'Tabela',
  GRAFICO_LINHA: 'Gráfico linha',
  GRAFICO_BARRA: 'Gráfico barra',
};

function newInstanceId(): string {
  return `uw-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
}

function WidgetPreviewPanel({
  nome,
  tipo,
  previewData,
  loading,
  error,
}: {
  nome: string;
  tipo: UserWidgetTipo;
  previewData: WorkspaceWidgetData | null;
  loading: boolean;
  error: string | null;
}) {
  if (loading) {
    return (
      <Box display="flex" justifyContent="center" py={4}>
        <CircularProgress size={32} aria-label="Gerando pré-visualização" />
      </Box>
    );
  }

  if (error) {
    return (
      <InfoBanner variant="danger" title="Pré-visualização indisponível">
        {error}
      </InfoBanner>
    );
  }

  if (!previewData) {
    return (
      <Typography variant="body2" color="text.secondary" role="status">
        Preencha nome, fonte e fórmula válida para ver a pré-visualização.
      </Typography>
    );
  }

  const title = nome.trim() || 'Pré-visualização';

  if (tipo === 'TABELA') {
    return <DynamicTableWidget title={title} data={previewData} />;
  }
  if (tipo === 'GRAFICO_LINHA') {
    return <DynamicChartWidget title={title} data={previewData} variant="GRAFICO_LINHA" />;
  }
  if (tipo === 'GRAFICO_BARRA') {
    return <DynamicChartWidget title={title} data={previewData} variant="GRAFICO_BARRA" />;
  }

  return <DynamicKpiWidget title={title} data={previewData} />;
}

export default function WidgetBuilderPage() {
  const { workspaceId: workspaceIdParam } = useParams();
  const navigate = useNavigate();
  const workspaceId = Number(workspaceIdParam);
  const invalidId = Number.isNaN(workspaceId);

  const [nome, setNome] = useState('');
  const [tipo, setTipo] = useState<UserWidgetTipo>('KPI');
  const [fonteRef, setFonteRef] = useState('');
  const [formula, setFormula] = useState('');
  const [datasets, setDatasets] = useState<DatasetSummary[]>([]);
  const [loadingSources, setLoadingSources] = useState(true);
  const [previewData, setPreviewData] = useState<WorkspaceWidgetData | null>(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [previewError, setPreviewError] = useState<string | null>(null);
  const [formulaValid, setFormulaValid] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (invalidId) {
      return;
    }
    setLoadingSources(true);
    void listDatasets()
      .then((items) => {
        setDatasets(items);
        if (items.length > 0) {
          setFonteRef(String(items[0].id));
        }
      })
      .catch(() => setDatasets([]))
      .finally(() => setLoadingSources(false));
  }, [invalidId]);

  const buildFontes = useCallback((): WidgetSourceRef[] => {
    if (!fonteRef.trim()) {
      return [];
    }
    if (fonteRef.startsWith('SISTEMA:')) {
      return [{ kind: 'SISTEMA', ref: fonteRef.replace(/^SISTEMA:/, '') }];
    }
    return [{ kind: 'DATASET', ref: fonteRef }];
  }, [fonteRef]);

  const buildPayload = useCallback(
    (expression: string) => ({
      nome: nome.trim(),
      tipo,
      fontes: buildFontes(),
      formula: expression.trim() || null,
      config: {},
    }),
    [nome, tipo, buildFontes],
  );

  const handleValidateFormula = useCallback(
    async (expression: string) => {
      const fontes = buildFontes();
      const result = await validateFormula(expression, fontes);
      if (!result.valid) {
        setFormulaValid(false);
        setPreviewData(null);
        throw new WorkspaceApiError(
          400,
          'Fórmula inválida',
          result.errors.map((message) => ({ field: 'formula', message })),
        );
      }
      setFormulaValid(true);

      if (!nome.trim()) {
        return;
      }

      setPreviewLoading(true);
      setPreviewError(null);
      try {
        const data = await previewWidgetDefinition(buildPayload(expression));
        setPreviewData(data);
      } catch (err) {
        setPreviewData(null);
        setPreviewError(err instanceof WorkspaceApiError ? err.message : 'Erro ao gerar pré-visualização');
      } finally {
        setPreviewLoading(false);
      }
    },
    [buildFontes, buildPayload, nome],
  );

  const canConfirm = useMemo(
    () => nome.trim().length > 0 && fonteRef.trim().length > 0 && formulaValid && !saving,
    [nome, fonteRef, formulaValid, saving],
  );

  const handleConfirm = async () => {
    if (!canConfirm || invalidId) {
      return;
    }
    setSaving(true);
    setSubmitError(null);
    try {
      const definition = await createWidgetDefinition(buildPayload(formula));
      const workspace = await getWorkspace(workspaceId);
      const nextWidget = {
        instanceId: newInstanceId(),
        ordem: workspace.widgets.length,
        colSpan: COL_SPAN_PRESETS.M,
        rowSpan: 1,
        userWidgetDefinitionId: definition.id,
        config: {},
      };
      await saveWorkspaceLayout(workspaceId, [...workspace.widgets, nextWidget]);
      navigate(`/workspace/${workspaceId}`);
    } catch (err) {
      if (err instanceof WorkspaceApiError) {
        const formulaError = err.errors?.find((item) => item.field === 'formula');
        setSubmitError(formulaError?.message ?? err.message);
      } else {
        setSubmitError('Erro ao salvar widget');
      }
    } finally {
      setSaving(false);
    }
  };

  if (invalidId) {
    return (
      <WorkspacePageShell title="Workspace inválido">
        <InfoBanner variant="danger">Identificador de workspace inválido.</InfoBanner>
        <Button sx={{ mt: 2 }} onClick={() => navigate('/workspace')}>
          Voltar ao hub
        </Button>
      </WorkspacePageShell>
    );
  }

  return (
    <WorkspacePageShell
      title="Novo widget"
      subtitle="Monte, valide a fórmula e pré-visualize antes de adicionar ao workspace"
      actions={
        <Stack direction="row" spacing={1}>
          <Button variant="outlined" onClick={() => navigate(`/workspace/${workspaceId}`)} disabled={saving}>
            Cancelar
          </Button>
          <Button variant="contained" onClick={() => void handleConfirm()} disabled={!canConfirm}>
            {saving ? 'Salvando…' : 'Adicionar ao workspace'}
          </Button>
        </Stack>
      }
    >
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={3}>
        <Box flex={1}>
          <Stack spacing={2}>
            <TextField
              label="Nome"
              value={nome}
              onChange={(event) => setNome(event.target.value)}
              fullWidth
              disabled={saving}
            />
            <FormControl fullWidth disabled={saving}>
              <InputLabel id="widget-tipo-label">Tipo</InputLabel>
              <Select
                labelId="widget-tipo-label"
                label="Tipo"
                value={tipo}
                onChange={(event) => {
                  setTipo(event.target.value as UserWidgetTipo);
                  setPreviewData(null);
                }}
              >
                {TIPOS.map((item) => (
                  <MenuItem key={item} value={item}>
                    {TIPO_LABELS[item]}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <FormControl fullWidth disabled={saving || loadingSources}>
              <InputLabel id="widget-fonte-label">Fonte de dado</InputLabel>
              <Select
                labelId="widget-fonte-label"
                label="Fonte de dado"
                value={fonteRef}
                onChange={(event) => {
                  setFonteRef(event.target.value);
                  setPreviewData(null);
                  setFormulaValid(false);
                }}
              >
                {datasets.map((ds) => (
                  <MenuItem key={ds.id} value={String(ds.id)}>
                    Dataset: {ds.nome}
                  </MenuItem>
                ))}
                <MenuItem value="SISTEMA:ORCAMENTO">Sistema: Orçamento</MenuItem>
              </Select>
            </FormControl>
            <FormulaEditor
              value={formula}
              onChange={(value) => {
                setFormula(value);
                setFormulaValid(false);
                setPreviewData(null);
              }}
              onValidate={handleValidateFormula}
              disabled={saving}
            />
            {submitError ? (
              <InfoBanner variant="danger">{submitError}</InfoBanner>
            ) : null}
          </Stack>
        </Box>

        <Box flex={1} sx={{ bgcolor: 'background.paper', borderRadius: 2, p: 2, minHeight: 240 }}>
          <Typography variant="subtitle1" component="h2" gutterBottom>
            Pré-visualização
          </Typography>
          <WidgetPreviewPanel
            nome={nome}
            tipo={tipo}
            previewData={previewData}
            loading={previewLoading}
            error={previewError}
          />
        </Box>
      </Stack>
    </WorkspacePageShell>
  );
}
