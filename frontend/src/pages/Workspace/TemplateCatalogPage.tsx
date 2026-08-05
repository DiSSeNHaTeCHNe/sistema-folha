import {
  Alert,
  Box,
  Button,
  Card,
  CardActions,
  CardContent,
  Chip,
  CircularProgress,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  Select,
  Typography,
} from '@mui/material';
import {
  Dataset as DatasetIcon,
  Publish as PublishIcon,
  Widgets as WidgetsIcon,
} from '@mui/icons-material';
import { useCallback, useEffect, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import {
  installOrcamentoTemplate,
  installTemplate,
  listDatasets,
  listTemplateCatalog,
  listWidgetDefinitions,
  listWorkspaces,
  publishDatasetTemplate,
  publishWidgetTemplate,
  upgradeTemplateInstallation,
  WorkspaceApiError,
} from '../../services/workspaceService';
import type { DatasetSummary, TemplateCatalogItem, UserWidgetDefinition, WorkspaceSummary } from './types';
import { TemplateUpgradeBanner } from './components/TemplateUpgradeBanner';

function tipoLabel(tipo: TemplateCatalogItem['tipo']): string {
  switch (tipo) {
    case 'DATASET': return 'Dataset';
    case 'WIDGET': return 'Widget';
    default: return tipo;
  }
}

function tipoIcon(tipo: TemplateCatalogItem['tipo']) {
  return tipo === 'DATASET' ? <DatasetIcon /> : <WidgetsIcon />;
}

export default function TemplateCatalogPage() {
  const [catalog, setCatalog] = useState<TemplateCatalogItem[]>([]);
  const [workspaces, setWorkspaces] = useState<WorkspaceSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [installTarget, setInstallTarget] = useState<TemplateCatalogItem | null>(null);
  const [selectedWorkspaceId, setSelectedWorkspaceId] = useState<number | ''>('');
  const [installing, setInstalling] = useState(false);
  const [publishOpen, setPublishOpen] = useState(false);
  const [datasets, setDatasets] = useState<DatasetSummary[]>([]);
  const [widgets, setWidgets] = useState<UserWidgetDefinition[]>([]);
  const [publishDatasetId, setPublishDatasetId] = useState<number | ''>('');
  const [publishWidgetId, setPublishWidgetId] = useState<number | ''>('');
  const [publishing, setPublishing] = useState(false);
  const [upgradingId, setUpgradingId] = useState<number | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [items, ws] = await Promise.all([listTemplateCatalog(), listWorkspaces()]);
      setCatalog(items);
      setWorkspaces(ws);
      if (ws.length > 0) {
        setSelectedWorkspaceId(ws[0].id);
      }
    } catch {
      setError('Erro ao carregar catálogo de templates');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const openPublish = async () => {
    setPublishOpen(true);
    setActionError(null);
    try {
      const [ds, wd] = await Promise.all([listDatasets(), listWidgetDefinitions()]);
      setDatasets(ds);
      setWidgets(wd);
    } catch {
      setActionError('Erro ao carregar itens para publicação');
    }
  };

  const handlePublish = async () => {
    setPublishing(true);
    setActionError(null);
    try {
      if (publishDatasetId !== '') {
        await publishDatasetTemplate(publishDatasetId);
      } else if (publishWidgetId !== '') {
        await publishWidgetTemplate(publishWidgetId);
      } else {
        setActionError('Selecione um dataset ou widget salvo para publicar');
        return;
      }
      setPublishOpen(false);
      setPublishDatasetId('');
      setPublishWidgetId('');
      await load();
    } catch (err) {
      setActionError(err instanceof WorkspaceApiError ? err.message : 'Erro ao publicar template');
    } finally {
      setPublishing(false);
    }
  };

  const handleInstall = async () => {
    if (!installTarget || selectedWorkspaceId === '') return;
    setInstalling(true);
    setActionError(null);
    try {
      if (installTarget.id === 0) {
        await installOrcamentoTemplate(selectedWorkspaceId);
      } else {
        await installTemplate(installTarget.id, selectedWorkspaceId);
      }
      setInstallTarget(null);
      await load();
    } catch (err) {
      setActionError(err instanceof WorkspaceApiError ? err.message : 'Erro ao instalar template');
    } finally {
      setInstalling(false);
    }
  };

  const handleUpgrade = async (installationId: number) => {
    setUpgradingId(installationId);
    setActionError(null);
    try {
      await upgradeTemplateInstallation(installationId);
      await load();
    } catch (err) {
      setActionError(err instanceof WorkspaceApiError ? err.message : 'Erro ao atualizar template');
    } finally {
      setUpgradingId(null);
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress aria-label="Carregando catálogo de templates" />
      </Box>
    );
  }

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Box display="flex" justifyContent="space-between" alignItems="flex-start" mb={2} gap={2} flexWrap="wrap">
        <Box>
          <Typography variant="h4" component="h1" gutterBottom>
            Catálogo de Templates
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Publique estruturas salvas ou instale templates compartilhados na sua hierarquia.
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<PublishIcon />}
          onClick={() => void openPublish()}
          aria-label="Publicar template"
        >
          Publicar template
        </Button>
      </Box>

      <Typography component={RouterLink} to="/workspace" variant="body2" sx={{ display: 'block', mb: 3 }}>
        ← Voltar ao Workspace
      </Typography>

      {error && (
        <Alert severity="error" role="alert" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}
      {actionError && (
        <Alert severity="error" role="alert" sx={{ mb: 2 }}>
          {actionError}
        </Alert>
      )}

      {catalog.some((item) => item.atualizacaoDisponivel && item.installationId) && (
        <Box mb={3}>
          {catalog
            .filter((item) => item.atualizacaoDisponivel && item.installationId)
            .map((item) => (
              <TemplateUpgradeBanner
                key={item.id}
                templateName={item.nome}
                versaoInstalada={item.versaoInstalada ?? 1}
                versaoDisponivel={item.versaoMaisRecente}
                upgrading={upgradingId === item.installationId}
                onUpgrade={() => void handleUpgrade(item.installationId!)}
              />
            ))}
        </Box>
      )}

      {catalog.length === 0 ? (
        <Alert severity="info" role="status">
          Nenhum template visível na sua hierarquia. Publique um dataset ou widget salvo para compartilhar.
        </Alert>
      ) : (
        <Grid container spacing={3}>
          {catalog.map((item) => (
            <Grid key={item.id} size={{ xs: 12, md: 6 }}>
              <Card component="article" aria-labelledby={`template-${item.id}-title`}>
                <CardContent>
                  <Box display="flex" alignItems="center" gap={2} mb={1}>
                    <Box
                      sx={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        width: 48,
                        height: 48,
                        borderRadius: 2,
                        bgcolor: 'primary.main',
                        color: 'primary.contrastText',
                      }}
                      aria-hidden="true"
                    >
                      {tipoIcon(item.tipo)}
                    </Box>
                    <Box flex={1}>
                      <Typography id={`template-${item.id}-title`} variant="h6" component="h2">
                        {item.nome}
                      </Typography>
                      <Chip label={tipoLabel(item.tipo)} size="small" sx={{ mt: 0.5 }} />
                    </Box>
                  </Box>
                  <Typography variant="body2" color="text.secondary">
                    Versão {item.versaoAtual} · publicador #{item.publicadorUsuarioId}
                  </Typography>
                  {item.versaoInstalada != null && (
                    <Typography variant="body2" color="text.secondary" mt={1}>
                      Instalado (v{item.versaoInstalada})
                    </Typography>
                  )}
                </CardContent>
                <CardActions>
                  {item.installationId == null ? (
                    <Button
                      variant="contained"
                      onClick={() => setInstallTarget(item)}
                      aria-label={`Instalar template ${item.nome}`}
                    >
                      Instalar
                    </Button>
                  ) : (
                    <Button variant="outlined" disabled aria-label={`Template ${item.nome} já instalado`}>
                      Instalado
                    </Button>
                  )}
                </CardActions>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      <Dialog open={installTarget != null} onClose={() => setInstallTarget(null)} aria-labelledby="install-dialog-title">
        <DialogTitle id="install-dialog-title">Instalar template</DialogTitle>
        <DialogContent>
          <Typography variant="body2" mb={2}>
            Será criada uma cópia independente de &quot;{installTarget?.nome}&quot; no workspace selecionado.
          </Typography>
          <FormControl fullWidth margin="normal">
            <InputLabel id="workspace-select-label">Workspace</InputLabel>
            <Select
              labelId="workspace-select-label"
              label="Workspace"
              value={selectedWorkspaceId}
              onChange={(e) => setSelectedWorkspaceId(e.target.value as number)}
            >
              {workspaces.map((ws) => (
                <MenuItem key={ws.id} value={ws.id}>
                  {ws.nome}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setInstallTarget(null)}>Cancelar</Button>
          <Button variant="contained" onClick={() => void handleInstall()} disabled={installing}>
            {installing ? 'Instalando…' : 'Confirmar instalação'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={publishOpen} onClose={() => setPublishOpen(false)} aria-labelledby="publish-dialog-title">
        <DialogTitle id="publish-dialog-title">Publicar template</DialogTitle>
        <DialogContent>
          <Typography variant="body2" mb={2}>
            Apenas a estrutura é publicada — nenhuma linha de dado será incluída (WKS-15).
          </Typography>
          <FormControl fullWidth margin="normal">
            <InputLabel id="dataset-publish-label">Dataset salvo</InputLabel>
            <Select
              labelId="dataset-publish-label"
              label="Dataset salvo"
              value={publishDatasetId}
              onChange={(e) => {
                setPublishDatasetId(e.target.value as number);
                setPublishWidgetId('');
              }}
            >
              <MenuItem value="">Nenhum</MenuItem>
              {datasets.map((ds) => (
                <MenuItem key={ds.id} value={ds.id}>
                  {ds.nome}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <FormControl fullWidth margin="normal">
            <InputLabel id="widget-publish-label">Widget salvo</InputLabel>
            <Select
              labelId="widget-publish-label"
              label="Widget salvo"
              value={publishWidgetId}
              onChange={(e) => {
                setPublishWidgetId(e.target.value as number);
                setPublishDatasetId('');
              }}
            >
              <MenuItem value="">Nenhum</MenuItem>
              {widgets.map((w) => (
                <MenuItem key={w.id} value={w.id}>
                  {w.nome}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPublishOpen(false)}>Cancelar</Button>
          <Button variant="contained" onClick={() => void handlePublish()} disabled={publishing}>
            {publishing ? 'Publicando…' : 'Publicar'}
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
}
