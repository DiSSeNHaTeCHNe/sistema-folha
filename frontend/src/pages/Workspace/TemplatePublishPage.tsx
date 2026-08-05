import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  Box,
  Button,
  CircularProgress,
  Grid,
  List,
  ListItem,
  ListItemText,
  Stack,
  Typography,
} from '@mui/material';
import {
  getDataset,
  getWidgetDefinition,
  publishDatasetTemplate,
  publishWidgetTemplate,
  WorkspaceApiError,
} from '../../services/workspaceService';
import type { DatasetDefinition, UserWidgetDefinition } from './types';
import { WorkspacePageShell } from './components/WorkspacePageShell';
import { InfoBanner } from './components/InfoBanner';

const SERa_PUBLICADO_DATASET = [
  'Esquema de campos (nomes, tipos, obrigatoriedade)',
  'Metadados do dataset (nome, versão do schema)',
  'Estrutura compartilhável na hierarquia',
];

const SERa_PUBLICADO_WIDGET = [
  'Definição do widget (nome, tipo)',
  'Fontes de dado referenciadas',
  'Fórmula e configuração',
  'Layout sugerido para instalação',
];

const NUNCA_PUBLICADO = [
  'Linhas e valores preenchidos',
  'Dados sensíveis ou privados',
  'Histórico de auditoria',
  'Alterações não salvas',
];

function ChecklistColumn({ title, items, variant }: { title: string; items: string[]; variant: 'ok' | 'danger' }) {
  return (
    <Box
      sx={{
        bgcolor: 'background.paper',
        borderRadius: 2,
        p: 2,
        border: 1,
        borderColor: 'divider',
        flex: 1,
      }}
    >
      <Typography variant="subtitle1" component="h2" gutterBottom>
        {title}
      </Typography>
      <List dense aria-label={title}>
        {items.map((item) => (
          <ListItem key={item} disablePadding sx={{ py: 0.5 }}>
            <ListItemText
              primary={item}
              primaryTypographyProps={{
                variant: 'body2',
                color: variant === 'danger' ? 'error.main' : 'text.primary',
              }}
            />
          </ListItem>
        ))}
      </List>
    </Box>
  );
}

export default function TemplatePublishPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const datasetIdParam = searchParams.get('datasetId');
  const widgetIdParam = searchParams.get('widgetId');
  const datasetId = datasetIdParam ? Number(datasetIdParam) : null;
  const widgetId = widgetIdParam ? Number(widgetIdParam) : null;
  const invalidParams =
    (datasetIdParam != null && Number.isNaN(datasetId)) ||
    (widgetIdParam != null && Number.isNaN(widgetId)) ||
    (datasetIdParam == null && widgetIdParam == null);

  const [dataset, setDataset] = useState<DatasetDefinition | null>(null);
  const [widget, setWidget] = useState<UserWidgetDefinition | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [publishError, setPublishError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [publishing, setPublishing] = useState(false);

  const loadItem = useCallback(async () => {
    if (invalidParams) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setLoadError(null);
    try {
      if (datasetId != null) {
        const data = await getDataset(datasetId);
        setDataset(data);
        setWidget(null);
      } else if (widgetId != null) {
        const data = await getWidgetDefinition(widgetId);
        setWidget(data);
        setDataset(null);
      }
    } catch {
      setLoadError('Item não encontrado ou não salvo. Salve antes de publicar.');
      setDataset(null);
      setWidget(null);
    } finally {
      setLoading(false);
    }
  }, [datasetId, widgetId, invalidParams]);

  useEffect(() => {
    void loadItem();
  }, [loadItem]);

  const itemName = dataset?.nome ?? widget?.nome ?? '';
  const seraPublicado = dataset ? SERa_PUBLICADO_DATASET : SERa_PUBLICADO_WIDGET;

  const extraPublishedItems = useMemo(() => {
    if (dataset) {
      return dataset.campos.map((campo) => `Campo: ${campo.nome} (${campo.tipo})`);
    }
    if (widget?.formula) {
      return [`Fórmula: ${widget.formula}`];
    }
    return [];
  }, [dataset, widget]);

  const canPublish = !invalidParams && !loadError && (dataset != null || widget != null) && !publishing;

  const handlePublish = async () => {
    if (!canPublish) {
      return;
    }
    setPublishing(true);
    setPublishError(null);
    setSuccessMessage(null);
    try {
      if (datasetId != null) {
        const result = await publishDatasetTemplate(datasetId);
        setSuccessMessage(
          result.novaVersaoCriada
            ? `Template "${result.nome}" publicado (v${result.versaoAtual}).`
            : `Template "${result.nome}" já estava publicado com a mesma estrutura.`,
        );
      } else if (widgetId != null) {
        const result = await publishWidgetTemplate(widgetId);
        setSuccessMessage(
          result.novaVersaoCriada
            ? `Template "${result.nome}" publicado (v${result.versaoAtual}).`
            : `Template "${result.nome}" já estava publicado com a mesma estrutura.`,
        );
      }
    } catch (err) {
      setPublishError(err instanceof WorkspaceApiError ? err.message : 'Erro ao publicar template');
    } finally {
      setPublishing(false);
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight={400}>
        <CircularProgress aria-label="Carregando item para publicação" />
      </Box>
    );
  }

  return (
    <WorkspacePageShell
      title="Publicar template"
      subtitle={itemName ? `Revisando: ${itemName}` : 'Selecione um item salvo para publicar'}
      actions={
        <Stack direction="row" spacing={1}>
          <Button variant="outlined" onClick={() => navigate('/workspace/templates')}>
            Voltar ao catálogo
          </Button>
          <Button variant="contained" onClick={() => void handlePublish()} disabled={!canPublish}>
            {publishing ? 'Publicando…' : 'Confirmar publicação'}
          </Button>
        </Stack>
      }
    >
      {invalidParams ? (
        <InfoBanner variant="danger" title="Publicação bloqueada">
          Informe um datasetId ou widgetId salvo na URL (?datasetId= ou ?widgetId=).
        </InfoBanner>
      ) : null}

      {loadError ? (
        <InfoBanner variant="danger" title="Publicação bloqueada (WKS2-24)">
          {loadError}
        </InfoBanner>
      ) : null}

      {publishError ? (
        <Box mb={2}>
          <InfoBanner variant="danger">{publishError}</InfoBanner>
        </Box>
      ) : null}

      {successMessage ? (
        <Box mb={2}>
          <InfoBanner variant="info" title="Publicação concluída">
            {successMessage}
          </InfoBanner>
        </Box>
      ) : null}

      {!invalidParams && !loadError && (dataset || widget) ? (
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, md: 6 }}>
            <ChecklistColumn
              title="Será publicado"
              items={[...seraPublicado, ...extraPublishedItems]}
              variant="ok"
            />
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <ChecklistColumn title="Nunca será publicado" items={NUNCA_PUBLICADO} variant="danger" />
          </Grid>
        </Grid>
      ) : null}
    </WorkspacePageShell>
  );
}
