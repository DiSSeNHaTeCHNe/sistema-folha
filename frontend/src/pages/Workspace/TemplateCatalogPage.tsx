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
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  Select,
  Stack,
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
  listTemplateCatalog,
  listWorkspaces,
  upgradeTemplateInstallation,
  WorkspaceApiError,
} from '../../services/workspaceService';
import type { TemplateCatalogItem } from './types';
import { TemplateUpgradeBanner } from './components/TemplateUpgradeBanner';
import { WorkspacePageShell } from './components/WorkspacePageShell';
import { StatusChip } from './components/StatusChip';
import { InfoBanner } from './components/InfoBanner';
import { colors } from './workspaceTheme';

function tipoLabel(tipo: TemplateCatalogItem['tipo']): string {
  switch (tipo) {
    case 'DATASET':
      return 'Dataset';
    case 'WIDGET':
      return 'Widget';
    case 'PACOTE':
      return 'Pacote';
    default:
      return tipo;
  }
}

function tipoIcon(tipo: TemplateCatalogItem['tipo']) {
  return tipo === 'DATASET' ? <DatasetIcon /> : <WidgetsIcon />;
}

function isNativeTemplate(item: TemplateCatalogItem): boolean {
  return item.id === 0 || item.tipo === 'PACOTE' || item.publicadorUsuarioId === 0;
}

export default function TemplateCatalogPage() {
  const [catalog, setCatalog] = useState<TemplateCatalogItem[]>([]);
  const [workspaces, setWorkspaces] = useState<{ id: number; nome: string; totalWidgets: number }[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [installTarget, setInstallTarget] = useState<TemplateCatalogItem | null>(null);
  const [selectedWorkspaceId, setSelectedWorkspaceId] = useState<number | ''>('');
  const [installing, setInstalling] = useState(false);
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
    <WorkspacePageShell
      title="Catálogo de Templates"
      subtitle="Publique estruturas salvas ou instale templates compartilhados na sua hierarquia"
      actions={
        <Button
          variant="contained"
          startIcon={<PublishIcon />}
          component={RouterLink}
          to="/workspace/templates/publish"
          aria-label="Publicar template"
        >
          Publicar template
        </Button>
      }
    >
      {error ? (
        <InfoBanner variant="danger">{error}</InfoBanner>
      ) : null}
      {actionError ? (
        <Box mb={2}>
          <InfoBanner variant="danger">{actionError}</InfoBanner>
        </Box>
      ) : null}

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
        <InfoBanner variant="info" title="Catálogo vazio">
          Nenhum template visível na sua hierarquia. Publique um dataset ou widget salvo para compartilhar.
        </InfoBanner>
      ) : (
        <Grid container spacing={3}>
          {catalog.map((item) => {
            const native = isNativeTemplate(item);
            return (
              <Grid key={item.id} size={{ xs: 12, md: 6 }}>
                <Card
                  component="article"
                  aria-labelledby={`template-${item.id}-title`}
                  sx={{
                    border: `1px solid ${colors.line}`,
                    boxShadow: 'none',
                    bgcolor: native ? colors.page : 'background.paper',
                  }}
                >
                  <CardContent>
                    <Stack direction="row" alignItems="flex-start" spacing={2} mb={1}>
                      <Box
                        sx={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                          width: 48,
                          height: 48,
                          borderRadius: 2,
                          bgcolor: native ? colors.navy : 'primary.main',
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
                        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap mt={0.5}>
                          <StatusChip variant="info" label={tipoLabel(item.tipo)} />
                          <StatusChip
                            variant={native ? 'ok' : 'info'}
                            label={native ? 'Nativo' : 'Usuário'}
                          />
                          <StatusChip variant="info" label={`v${item.versaoAtual}`} />
                          {item.atualizacaoDisponivel ? (
                            <StatusChip
                              variant="warn"
                              label={`v${item.versaoMaisRecente} disponível`}
                            />
                          ) : null}
                        </Stack>
                      </Box>
                    </Stack>
                    <Typography variant="body2" color="text.secondary">
                      Escopo hierárquico · publicador #{item.publicadorUsuarioId}
                    </Typography>
                    {item.versaoInstalada != null ? (
                      <Typography variant="body2" color="text.secondary" mt={1}>
                        Instalado (v{item.versaoInstalada})
                      </Typography>
                    ) : null}
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
                    {item.atualizacaoDisponivel && item.installationId != null ? (
                      <Button
                        component={RouterLink}
                        to={`/workspace/templates/${item.id}/upgrade?installationId=${item.installationId}&versaoInstalada=${item.versaoInstalada}`}
                        aria-label={`Ver diferenças de ${item.nome}`}
                      >
                        Ver diferenças
                      </Button>
                    ) : null}
                  </CardActions>
                </Card>
              </Grid>
            );
          })}
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
    </WorkspacePageShell>
  );
}
