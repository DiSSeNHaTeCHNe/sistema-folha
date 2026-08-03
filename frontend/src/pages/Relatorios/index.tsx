import { useState, useEffect, useCallback, useRef } from 'react';
import {
  Container,
  Typography,
  Box,
  Grid,
  Alert,
  CircularProgress,
} from '@mui/material';
import { Assessment as AssessmentIcon, CardGiftcard as CardGiftcardIcon } from '@mui/icons-material';
import { relatorioService } from '../../services/relatorioService';
import type { RelatorioFolha, RelatorioBeneficio } from '../../services/relatorioService';
import { useNotification } from '../../hooks/useNotification';
import { Notification } from '../../components/Notification';
import { CompetenciaPicker, type Competencia } from './CompetenciaPicker';
import { RelatorioCatalogCard } from './RelatorioCatalogCard';

const POLL_INTERVAL_MS = 2000;

function currentCompetencia(): Competencia {
  const now = new Date();
  return { mes: now.getMonth() + 1, ano: now.getFullYear() };
}

function findForCompetencia<T extends { mes: number; ano: number }>(
  list: T[],
  competencia: Competencia,
): T | undefined {
  return list.find((r) => r.mes === competencia.mes && r.ano === competencia.ano);
}

export function Relatorios() {
  const [competencia, setCompetencia] = useState<Competencia>(currentCompetencia);
  const [relatoriosFolha, setRelatoriosFolha] = useState<RelatorioFolha[]>([]);
  const [relatoriosBeneficio, setRelatoriosBeneficio] = useState<RelatorioBeneficio[]>([]);
  const [loading, setLoading] = useState(true);
  const [generatingFolha, setGeneratingFolha] = useState(false);
  const [generatingBeneficio, setGeneratingBeneficio] = useState(false);
  const [downloadingFolha, setDownloadingFolha] = useState(false);
  const [downloadingBeneficio, setDownloadingBeneficio] = useState(false);
  const { notification, showNotification, hideNotification } = useNotification();
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const relatorioFolha = findForCompetencia(relatoriosFolha, competencia);
  const relatorioBeneficio = findForCompetencia(relatoriosBeneficio, competencia);

  const carregarRelatorios = useCallback(async (silent = false) => {
    try {
      if (!silent) setLoading(true);
      const [folha, beneficio] = await Promise.all([
        relatorioService.listarRelatoriosFolha(),
        relatorioService.listarRelatoriosBeneficio(),
      ]);
      setRelatoriosFolha(folha);
      setRelatoriosBeneficio(beneficio);
    } catch {
      if (!silent) {
        showNotification('Erro ao carregar relatórios', 'error');
      }
    } finally {
      if (!silent) setLoading(false);
    }
  }, [showNotification]);

  const hasPending = relatorioFolha?.status === 'PENDENTE' || relatorioBeneficio?.status === 'PENDENTE';

  useEffect(() => {
    carregarRelatorios();
  }, [carregarRelatorios]);

  useEffect(() => {
    if (pollRef.current) {
      clearInterval(pollRef.current);
      pollRef.current = null;
    }

    if (hasPending) {
      pollRef.current = setInterval(() => {
        void carregarRelatorios(true);
      }, POLL_INTERVAL_MS);
    }

    return () => {
      if (pollRef.current) {
        clearInterval(pollRef.current);
      }
    };
  }, [hasPending, carregarRelatorios]);

  const downloadBlob = (blob: Blob, filename: string) => {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  };

  const gerarRelatorioFolha = async () => {
    if (relatorioFolha?.status === 'PENDENTE') return;
    try {
      setGeneratingFolha(true);
      await relatorioService.gerarRelatorioFolha(competencia.mes, competencia.ano);
      showNotification('Relatório de folha em geração', 'success');
      await carregarRelatorios(true);
    } catch {
      showNotification('Erro ao gerar relatório de folha', 'error');
    } finally {
      setGeneratingFolha(false);
    }
  };

  const gerarRelatorioBeneficio = async () => {
    if (relatorioBeneficio?.status === 'PENDENTE') return;
    try {
      setGeneratingBeneficio(true);
      await relatorioService.gerarRelatorioBeneficio(competencia.mes, competencia.ano);
      showNotification('Relatório de benefícios em geração', 'success');
      await carregarRelatorios(true);
    } catch {
      showNotification('Erro ao gerar relatório de benefícios', 'error');
    } finally {
      setGeneratingBeneficio(false);
    }
  };

  const downloadRelatorioFolha = async () => {
    if (!relatorioFolha || relatorioFolha.status !== 'PROCESSADO') return;
    try {
      setDownloadingFolha(true);
      const blob = await relatorioService.downloadRelatorioFolha(relatorioFolha.id);
      downloadBlob(blob, `relatorio-folha-${competencia.mes}-${competencia.ano}.pdf`);
      showNotification('Relatório baixado com sucesso', 'success');
    } catch {
      showNotification('Erro ao baixar relatório', 'error');
    } finally {
      setDownloadingFolha(false);
    }
  };

  const downloadRelatorioBeneficio = async () => {
    if (!relatorioBeneficio || relatorioBeneficio.status !== 'PROCESSADO') return;
    try {
      setDownloadingBeneficio(true);
      const blob = await relatorioService.downloadRelatorioBeneficio(relatorioBeneficio.id);
      downloadBlob(blob, `relatorio-beneficio-${competencia.mes}-${competencia.ano}.pdf`);
      showNotification('Relatório baixado com sucesso', 'success');
    } catch {
      showNotification('Erro ao baixar relatório', 'error');
    } finally {
      setDownloadingBeneficio(false);
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress size={60} />
      </Box>
    );
  }

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Typography variant="h4" component="h1" gutterBottom fontWeight={600}>
        Relatórios Executivos
      </Typography>
      <Typography variant="body1" color="text.secondary" mb={4}>
        Gere PDFs premium com KPIs, gráficos e breakdowns para apresentações gerenciais.
      </Typography>

      <Box mb={4} maxWidth={320}>
        <CompetenciaPicker value={competencia} onChange={setCompetencia} />
      </Box>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 6 }}>
          <RelatorioCatalogCard
            title="Executivo de Folha"
            description="PDF com KPIs de custo empresa, breakdown por centro de custo, linha de negócio, top rubricas e evolução mensal."
            icon={<AssessmentIcon />}
            status={relatorioFolha?.status}
            erro={relatorioFolha?.erro ?? 'Erro ao gerar relatório. Tente novamente.'}
            totalLabel="Total folha"
            totalValue={relatorioFolha?.totalFolha}
            onGenerate={gerarRelatorioFolha}
            onDownload={downloadRelatorioFolha}
            onRetry={gerarRelatorioFolha}
            generating={generatingFolha}
            downloading={downloadingFolha}
          />
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <RelatorioCatalogCard
            title="Custo Benefício + Folha"
            description="PDF consolidando custo de benefícios por tipo e custo de folha — substitui planilhas manuais de fechamento."
            icon={<CardGiftcardIcon />}
            status={relatorioBeneficio?.status}
            erro={relatorioBeneficio?.erro ?? 'Erro ao gerar relatório. Tente novamente.'}
            totalLabel="Custo consolidado"
            totalValue={relatorioBeneficio?.totalValor}
            onGenerate={gerarRelatorioBeneficio}
            onDownload={downloadRelatorioBeneficio}
            onRetry={gerarRelatorioBeneficio}
            generating={generatingBeneficio}
            downloading={downloadingBeneficio}
          />
        </Grid>
      </Grid>

      {!relatorioFolha && !relatorioBeneficio && (
        <Alert severity="info" sx={{ mt: 3 }}>
          Nenhum relatório gerado para {competencia.mes}/{competencia.ano}. Selecione o tipo e clique em Gerar.
        </Alert>
      )}

      <Notification
        open={notification.open}
        message={notification.message}
        severity={notification.severity}
        onClose={hideNotification}
      />
    </Container>
  );
}
