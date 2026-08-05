import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import {
  Box,
  Button,
  CircularProgress,
  List,
  ListItem,
  ListItemText,
  Stack,
  Typography,
} from '@mui/material';
import {
  listTemplateVersions,
  upgradeTemplateInstallation,
  WorkspaceApiError,
} from '../../services/workspaceService';
import type { TemplateVersionSummary } from './types';
import { WorkspacePageShell } from './components/WorkspacePageShell';
import { InfoBanner } from './components/InfoBanner';
import { StatusChip } from './components/StatusChip';
import {
  compareTemplateStructures,
  hasStructureChanges,
  type TemplateStructureDiff,
} from './utils/compareTemplateStructures';

function DiffSection({ title, items }: { title: string; items: string[] }) {
  if (items.length === 0) {
    return null;
  }
  return (
    <Box mb={2}>
      <Typography variant="subtitle2" component="h3" gutterBottom>
        {title}
      </Typography>
      <List dense aria-label={title}>
        {items.map((item) => (
          <ListItem key={item} disablePadding>
            <ListItemText primary={item} />
          </ListItem>
        ))}
      </List>
    </Box>
  );
}

function DiffPanel({ diff }: { diff: TemplateStructureDiff }) {
  if (!hasStructureChanges(diff)) {
    return (
      <Typography variant="body2" color="text.secondary" role="status">
        Nenhuma diferença estrutural detectada entre as versões.
      </Typography>
    );
  }

  return (
    <Box>
      <DiffSection title="Campos adicionados" items={diff.camposAdicionados} />
      <DiffSection title="Campos removidos" items={diff.camposRemovidos} />
      <DiffSection title="Widgets adicionados" items={diff.widgetsAdicionados} />
      <DiffSection title="Widgets removidos" items={diff.widgetsRemovidos} />
      <DiffSection title="Fórmulas alteradas" items={diff.formulasAlteradas} />
    </Box>
  );
}

export default function TemplateUpgradePage() {
  const { templateId: templateIdParam } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const templateId = Number(templateIdParam);
  const installationId = Number(searchParams.get('installationId'));
  const versaoInstalada = Number(searchParams.get('versaoInstalada'));
  const invalidParams =
    Number.isNaN(templateId) || Number.isNaN(installationId) || Number.isNaN(versaoInstalada);

  const [versions, setVersions] = useState<TemplateVersionSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [upgrading, setUpgrading] = useState(false);

  const load = useCallback(async () => {
    if (invalidParams) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const items = await listTemplateVersions(templateId);
      setVersions(items);
    } catch {
      setError('Erro ao carregar versões do template');
    } finally {
      setLoading(false);
    }
  }, [templateId, invalidParams]);

  useEffect(() => {
    void load();
  }, [load]);

  const installedVersion = useMemo(
    () => versions.find((item) => item.versao === versaoInstalada) ?? null,
    [versions, versaoInstalada],
  );

  const latestVersion = versions[0] ?? null;

  const diff = useMemo(() => {
    if (!installedVersion || !latestVersion) {
      return null;
    }
    return compareTemplateStructures(installedVersion.estruturaResumo, latestVersion.estruturaResumo);
  }, [installedVersion, latestVersion]);

  const handleUpgrade = async () => {
    if (invalidParams) {
      return;
    }
    setUpgrading(true);
    setActionError(null);
    try {
      await upgradeTemplateInstallation(installationId);
      navigate('/workspace/templates');
    } catch (err) {
      setActionError(err instanceof WorkspaceApiError ? err.message : 'Erro ao atualizar template');
    } finally {
      setUpgrading(false);
    }
  };

  const handleStay = () => {
    navigate('/workspace/templates');
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight={400}>
        <CircularProgress aria-label="Carregando diff de versões" />
      </Box>
    );
  }

  return (
    <WorkspacePageShell
      title="Atualizar template"
      subtitle={
        installedVersion && latestVersion
          ? `Comparando v${installedVersion.versao} → v${latestVersion.versao}`
          : 'Revisão de versões'
      }
      actions={
        <Stack direction="row" spacing={1}>
          <Button variant="outlined" onClick={handleStay} disabled={upgrading}>
            Permanecer na versão atual
          </Button>
          <Button variant="contained" onClick={() => void handleUpgrade()} disabled={upgrading || invalidParams}>
            {upgrading ? 'Atualizando…' : 'Atualizar'}
          </Button>
        </Stack>
      }
    >
      {invalidParams ? (
        <InfoBanner variant="danger">Parâmetros inválidos (templateId ou installationId).</InfoBanner>
      ) : null}

      {error ? (
        <InfoBanner variant="danger">{error}</InfoBanner>
      ) : null}

      {actionError ? (
        <Box mb={2}>
          <InfoBanner variant="danger">{actionError}</InfoBanner>
        </Box>
      ) : null}

      {installedVersion && latestVersion ? (
        <Stack spacing={2}>
          <Stack direction="row" spacing={1}>
            <StatusChip variant="info" label={`Instalada: v${installedVersion.versao}`} />
            <StatusChip variant="warn" label={`Disponível: v${latestVersion.versao}`} />
          </Stack>
          {diff ? <DiffPanel diff={diff} /> : null}
        </Stack>
      ) : null}
    </WorkspacePageShell>
  );
}
