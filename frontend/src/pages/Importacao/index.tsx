import React, { useMemo, useRef, useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Alert,
  CircularProgress,
  Paper,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  IconButton,
  FormControlLabel,
  Checkbox,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
} from '@mui/material';
import {
  CloudUpload as CloudUploadIcon,
  Error as ErrorIcon,
  Description as DescriptionIcon,
  AttachMoney as AttachMoneyIcon,
  CardGiftcard as CardGiftcardIcon,
  HelpOutline as HelpOutlineIcon,
} from '@mui/icons-material';
import { toast } from 'react-toastify';
import { importacaoService } from '../../services/importacaoService';
import { beneficioMensalService } from '../../services/beneficioMensalService';
import { folhaPagamentoService } from '../../services/folhaPagamentoService';
import type { BeneficioMensalCompetenciaParams, ImportacaoResponse } from '../../types';

const MESES = [
  { valor: 1, label: 'Janeiro' },
  { valor: 2, label: 'Fevereiro' },
  { valor: 3, label: 'Março' },
  { valor: 4, label: 'Abril' },
  { valor: 5, label: 'Maio' },
  { valor: 6, label: 'Junho' },
  { valor: 7, label: 'Julho' },
  { valor: 8, label: 'Agosto' },
  { valor: 9, label: 'Setembro' },
  { valor: 10, label: 'Outubro' },
  { valor: 11, label: 'Novembro' },
  { valor: 12, label: 'Dezembro' },
];

const competenciaParams = (mes: number, ano: number): BeneficioMensalCompetenciaParams => {
  const mesStr = String(mes).padStart(2, '0');
  const ultimoDia = new Date(ano, mes, 0).getDate();
  return {
    competenciaInicio: `${ano}-${mesStr}-01`,
    competenciaFim: `${ano}-${mesStr}-${String(ultimoDia).padStart(2, '0')}`,
  };
};

const gerarAnosDisponiveis = (): number[] => {
  const anoAtual = new Date().getFullYear();
  return Array.from({ length: 5 }, (_, i) => anoAtual - 2 + i);
};

type ImportacaoTipo = 'beneficiosMensais' | 'folhaAdp';

interface UploadState {
  loading: boolean;
  success: boolean;
  error: string | null;
  registrosProcessados?: number;
  errosCount?: number;
  totalValor?: number;
  erros?: string[];
  arquivo?: string;
  tamanho?: number;
}

export default function Importacao() {
  const now = new Date();
  const [mesBeneficios, setMesBeneficios] = useState(now.getMonth() + 1);
  const [anoBeneficios, setAnoBeneficios] = useState(now.getFullYear());

  const [beneficiosMensaisState, setBeneficiosMensaisState] = useState<UploadState>({
    loading: false,
    success: false,
    error: null,
  });

  const [folhaAdpState, setFolhaAdpState] = useState<UploadState>({
    loading: false,
    success: false,
    error: null,
  });

  const beneficiosMensaisFileRef = useRef<HTMLInputElement>(null);
  const folhaAdpFileRef = useRef<HTMLInputElement>(null);

  const [helpOpen, setHelpOpen] = useState(false);
  const [conflictDialogOpen, setConflictDialogOpen] = useState(false);
  const [conflictMessage, setConflictMessage] = useState('');
  const [pendingFile, setPendingFile] = useState<File | null>(null);
  const [pendingTipo, setPendingTipo] = useState<ImportacaoTipo | null>(null);
  const [pendingCompetencia, setPendingCompetencia] = useState<BeneficioMensalCompetenciaParams | null>(null);

  const [beneficiosMensaisFileName, setBeneficiosMensaisFileName] = useState('');
  const [folhaAdpFileName, setFolhaAdpFileName] = useState('');
  const [isDecimoTerceiro, setIsDecimoTerceiro] = useState(false);

  const [mesProcessamento, setMesProcessamento] = useState(now.getMonth() + 1);
  const [anoProcessamento, setAnoProcessamento] = useState(now.getFullYear());
  const [decimoTerceiroProcessamento, setDecimoTerceiroProcessamento] = useState(false);
  const [recalcularFerias, setRecalcularFerias] = useState(false);
  const [processamentoManualLoading, setProcessamentoManualLoading] = useState(false);

  const anosDisponiveis = useMemo(() => gerarAnosDisponiveis(), []);
  const competenciaBeneficios = useMemo(
    () => competenciaParams(mesBeneficios, anoBeneficios),
    [mesBeneficios, anoBeneficios],
  );
  const competenciaProcessamento = useMemo(
    () => competenciaParams(mesProcessamento, anoProcessamento),
    [mesProcessamento, anoProcessamento],
  );

  const handleFolhaAdpUpload = async (file: File | null, confirmarSubstituicao = false) => {
    if (!file) {
      toast.error('Por favor, selecione um arquivo');
      return;
    }

    if (!file.name.toLowerCase().endsWith('.txt')) {
      toast.error('Para importação de folha ADP, selecione apenas arquivos .txt');
      return;
    }

    setFolhaAdpState({ loading: true, success: false, error: null });

    try {
      const response: ImportacaoResponse = await importacaoService.importarFolhaAdp(
        file,
        isDecimoTerceiro,
        confirmarSubstituicao,
      );

      if (response.success) {
        setFolhaAdpState({
          loading: false,
          success: true,
          error: null,
          registrosProcessados: response.registrosProcessados,
          erros: response.erros,
          arquivo: response.arquivo,
          tamanho: response.tamanho,
        });

        toast.success(response.message);
        setFolhaAdpFileName('');
        setIsDecimoTerceiro(false);
        if (folhaAdpFileRef.current) {
          folhaAdpFileRef.current.value = '';
        }
      } else {
        setFolhaAdpState({
          loading: false,
          success: false,
          error: response.message,
          arquivo: response.arquivo,
        });
        if (response.message?.startsWith('Funcionários não encontrados:')) {
          alert(response.message);
        } else {
          toast.error(response.message);
        }
      }
    } catch (error: unknown) {
      const axiosError = error as { response?: { status?: number; data?: { message?: string } } };
      if (axiosError.response?.status === 409) {
        setConflictMessage(axiosError.response.data?.message ?? 'Já existe uma folha para este período.');
        setPendingFile(file);
        setPendingTipo('folhaAdp');
        setPendingCompetencia(null);
        setConflictDialogOpen(true);
        setFolhaAdpState({ loading: false, success: false, error: null });
        return;
      }

      const errorMessage = axiosError.response?.data?.message || 'Erro ao importar arquivo';
      setFolhaAdpState({ loading: false, success: false, error: errorMessage });
      if (errorMessage.startsWith('Funcionários não encontrados:')) {
        alert(errorMessage);
      } else {
        toast.error(errorMessage);
      }
    }
  };

  const handleBeneficiosMensaisUpload = async (file: File | null, confirmar = false) => {
    if (!file) {
      toast.error('Por favor, selecione um arquivo');
      return;
    }

    if (!file.name.toLowerCase().endsWith('.xlsx')) {
      toast.error('Para importação de benefícios mensais, selecione apenas arquivos .xlsx');
      return;
    }

    setBeneficiosMensaisState({ loading: true, success: false, error: null });

    try {
      const resultado = await beneficioMensalService.importar(
        file,
        competenciaBeneficios.competenciaInicio,
        competenciaBeneficios.competenciaFim,
        confirmar,
      );

      setBeneficiosMensaisState({
        loading: false,
        success: true,
        error: null,
        registrosProcessados: resultado.processadas,
        errosCount: resultado.erros,
        totalValor: resultado.totalValor,
        erros: resultado.detalhesErros,
        arquivo: file.name,
        tamanho: file.size,
      });

      toast.success(
        confirmar
          ? 'Benefícios mensais importados com sucesso! Os dados anteriores foram substituídos.'
          : 'Benefícios mensais importados com sucesso!',
      );

      setBeneficiosMensaisFileName('');
      if (beneficiosMensaisFileRef.current) {
        beneficiosMensaisFileRef.current.value = '';
      }
    } catch (error: unknown) {
      const axiosError = error as { response?: { status?: number; data?: { message?: string } } };
      if (axiosError.response?.status === 409) {
        setConflictMessage(
          axiosError.response.data?.message ?? 'Já existem dados para esta competência.',
        );
        setPendingFile(file);
        setPendingTipo('beneficiosMensais');
        setPendingCompetencia(competenciaBeneficios);
        setConflictDialogOpen(true);
        setBeneficiosMensaisState({ loading: false, success: false, error: null });
        return;
      }

      const errorMessage = axiosError.response?.data?.message || 'Erro ao importar arquivo';
      setBeneficiosMensaisState({ loading: false, success: false, error: errorMessage });
      if (errorMessage.includes('Erros encontrados:')) {
        alert(errorMessage);
      } else {
        toast.error(errorMessage);
      }
    }
  };

  const handleBeneficiosMensaisFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    setBeneficiosMensaisFileName(file ? file.name : '');
    setBeneficiosMensaisState((prev) => ({ ...prev, success: false, error: null }));
  };

  const handleFolhaAdpFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    setFolhaAdpFileName(file ? file.name : '');
    setFolhaAdpState((prev) => ({ ...prev, success: false, error: null }));
  };

  const resetBeneficiosMensaisState = () => {
    setBeneficiosMensaisState({ loading: false, success: false, error: null });
    setBeneficiosMensaisFileName('');
    if (beneficiosMensaisFileRef.current) {
      beneficiosMensaisFileRef.current.value = '';
    }
  };

  const resetFolhaAdpState = () => {
    setFolhaAdpState({ loading: false, success: false, error: null });
    setFolhaAdpFileName('');
    setIsDecimoTerceiro(false);
    if (folhaAdpFileRef.current) {
      folhaAdpFileRef.current.value = '';
    }
  };

  const handleProcessarCompetencia = async () => {
    setProcessamentoManualLoading(true);
    try {
      const resultado = await folhaPagamentoService.processarCompetencia({
        competenciaInicio: competenciaProcessamento.competenciaInicio,
        competenciaFim: competenciaProcessamento.competenciaFim,
        decimoTerceiro: decimoTerceiroProcessamento,
        recalcularFerias,
      });
      toast.success(
        `Ficha processada: ${resultado.totalFichas} fichas, ${resultado.totalLinhas} linhas`,
      );
    } catch (error: unknown) {
      const axiosError = error as { response?: { status?: number; data?: { message?: string; detail?: string } } };
      if (axiosError.response?.status === 403) {
        toast.error('Você não tem permissão para processar a ficha. Apenas administradores podem executar esta ação.');
        return;
      }
      const errorMessage =
        axiosError.response?.data?.message ||
        axiosError.response?.data?.detail ||
        'Erro ao processar ficha da competência';
      toast.error(errorMessage);
    } finally {
      setProcessamentoManualLoading(false);
    }
  };

  const handleConfirmSubstituicao = async () => {
    setConflictDialogOpen(false);
    if (!pendingFile || !pendingTipo) return;

    if (pendingTipo === 'folhaAdp') {
      await handleFolhaAdpUpload(pendingFile, true);
    } else if (pendingTipo === 'beneficiosMensais' && pendingCompetencia) {
      setBeneficiosMensaisState({ loading: true, success: false, error: null });
      try {
        const resultado = await beneficioMensalService.importar(
          pendingFile,
          pendingCompetencia.competenciaInicio,
          pendingCompetencia.competenciaFim,
          true,
        );
        setBeneficiosMensaisState({
          loading: false,
          success: true,
          error: null,
          registrosProcessados: resultado.processadas,
          errosCount: resultado.erros,
          totalValor: resultado.totalValor,
          erros: resultado.detalhesErros,
          arquivo: pendingFile.name,
          tamanho: pendingFile.size,
        });
        toast.success('Benefícios mensais importados com sucesso! Os dados anteriores foram substituídos.');
        setBeneficiosMensaisFileName('');
        if (beneficiosMensaisFileRef.current) {
          beneficiosMensaisFileRef.current.value = '';
        }
      } catch (error: unknown) {
        const axiosError = error as { response?: { data?: { message?: string } } };
        const errorMessage = axiosError.response?.data?.message || 'Erro ao importar arquivo';
        setBeneficiosMensaisState({ loading: false, success: false, error: errorMessage });
        if (errorMessage.includes('Erros encontrados:')) {
          alert(errorMessage);
        } else {
          toast.error(errorMessage);
        }
      }
    }

    setPendingFile(null);
    setPendingTipo(null);
    setPendingCompetencia(null);
  };

  const handleCancelSubstituicao = () => {
    const tipo = pendingTipo;
    setConflictDialogOpen(false);
    setPendingFile(null);
    setPendingTipo(null);
    setPendingCompetencia(null);
    if (tipo === 'folhaAdp') {
      setFolhaAdpState({ loading: false, success: false, error: null });
    } else if (tipo === 'beneficiosMensais') {
      setBeneficiosMensaisState({ loading: false, success: false, error: null });
    }
  };

  const conflictTitle =
    pendingTipo === 'beneficiosMensais' ? 'Dados já existentes' : 'Confirmar Substituição de Folha';

  const conflictDescription =
    pendingTipo === 'beneficiosMensais'
      ? 'Substituir irá desativar os lançamentos existentes para esta competência e importar os dados do arquivo.'
      : 'Esta ação irá desativar a folha de pagamento existente e todos os seus registros, substituindo-os pelos dados do novo arquivo.';

  return (
    <Box p={3}>
      <Box display="flex" alignItems="center" gap={1}>
        <Typography variant="h4" gutterBottom>
          Importação de Dados
        </Typography>
        <IconButton size="small" onClick={() => setHelpOpen(true)}>
          <HelpOutlineIcon fontSize="small" />
        </IconButton>
      </Box>
      <Dialog open={helpOpen} onClose={() => setHelpOpen(false)}>
        <DialogTitle>Ajuda - Importação de Dados</DialogTitle>
        <DialogContent>
          <Typography gutterBottom>
            Faça upload dos arquivos de folha de pagamento ADP (.txt) ou benefícios mensais (.xlsx).
            <br />
            Para benefícios mensais, selecione a competência (mês/ano) e envie um arquivo Excel com a
            aba &quot;Lancamentos&quot;.
            <br />
            O sistema irá processar os dados e exibir o resultado da importação.
            <br />
            Caso algum funcionário não seja encontrado, será exibida uma lista ao final.
            <br />
            Utilize o campo abaixo para visualizar o retorno detalhado da importação.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setHelpOpen(false)}>Fechar</Button>
        </DialogActions>
      </Dialog>
      <Typography variant="body1" color="text.secondary" paragraph>
        Faça upload dos arquivos para importar dados de folha de pagamento e benefícios mensais no sistema.
      </Typography>

      <Box display="flex" gap={3} flexWrap="wrap">
        {/* Importação de Benefícios Mensais */}
        <Box flex="1" minWidth="400px">
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center" mb={2}>
                <CardGiftcardIcon color="primary" sx={{ mr: 1 }} />
                <Typography variant="h6">Importação de Benefícios Mensais</Typography>
              </Box>

              <Typography variant="body2" color="text.secondary" paragraph>
                Importe arquivos Excel (.xlsx) com a aba &quot;Lancamentos&quot; contendo lançamentos de
                benefícios mensais dos funcionários.
              </Typography>

              <Box
                sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' }, gap: 2, mb: 2 }}
              >
                <FormControl sx={{ minWidth: { xs: '100%', sm: 180 } }}>
                  <InputLabel id="importacao-mes-label">Mês</InputLabel>
                  <Select
                    labelId="importacao-mes-label"
                    label="Mês"
                    value={mesBeneficios}
                    onChange={(e) => setMesBeneficios(Number(e.target.value))}
                    disabled={beneficiosMensaisState.loading}
                  >
                    {MESES.map((item) => (
                      <MenuItem key={item.valor} value={item.valor}>
                        {item.label}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
                <FormControl sx={{ minWidth: { xs: '100%', sm: 120 } }}>
                  <InputLabel id="importacao-ano-label">Ano</InputLabel>
                  <Select
                    labelId="importacao-ano-label"
                    label="Ano"
                    value={anoBeneficios}
                    onChange={(e) => setAnoBeneficios(Number(e.target.value))}
                    disabled={beneficiosMensaisState.loading}
                  >
                    {anosDisponiveis.map((itemAno) => (
                      <MenuItem key={itemAno} value={itemAno}>
                        {itemAno}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Box>

              <Box mb={2}>
                <input
                  ref={beneficiosMensaisFileRef}
                  type="file"
                  accept=".xlsx"
                  style={{ display: 'none' }}
                  onChange={handleBeneficiosMensaisFileChange}
                />
                <Button
                  variant="outlined"
                  startIcon={<CloudUploadIcon />}
                  onClick={() => beneficiosMensaisFileRef.current?.click()}
                  fullWidth
                  sx={{ mb: 1 }}
                  disabled={beneficiosMensaisState.loading}
                >
                  Selecionar Arquivo (.xlsx)
                </Button>

                <Typography variant="body2" color="primary">
                  Arquivo selecionado: {beneficiosMensaisFileName || ''}
                </Typography>
              </Box>

              <Box display="flex" gap={1}>
                <Button
                  variant="contained"
                  onClick={() =>
                    handleBeneficiosMensaisUpload(beneficiosMensaisFileRef.current?.files?.[0] ?? null)
                  }
                  disabled={
                    beneficiosMensaisState.loading || !beneficiosMensaisFileRef.current?.files?.[0]
                  }
                  startIcon={
                    beneficiosMensaisState.loading ? (
                      <CircularProgress size={20} />
                    ) : (
                      <DescriptionIcon />
                    )
                  }
                  fullWidth
                >
                  {beneficiosMensaisState.loading ? 'Importando...' : 'Importar Benefícios Mensais'}
                </Button>

                {beneficiosMensaisState.success && (
                  <Button variant="outlined" onClick={resetBeneficiosMensaisState} size="small">
                    Novo
                  </Button>
                )}
              </Box>
            </CardContent>
          </Card>
        </Box>

        {/* Importação de Folha ADP */}
        <Box flex="1" minWidth="400px">
          <Card>
            <CardContent>
              <Box display="flex" alignItems="center" mb={2}>
                <AttachMoneyIcon color="secondary" sx={{ mr: 1 }} />
                <Typography variant="h6">Importação de Folha ADP</Typography>
              </Box>

              <Typography variant="body2" color="text.secondary" paragraph>
                Importe arquivos de texto (.txt) com layout específico do ADP contendo dados da folha de
                pagamento.
              </Typography>

              <Box mb={2}>
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={isDecimoTerceiro}
                      onChange={(e) => setIsDecimoTerceiro(e.target.checked)}
                      color="secondary"
                    />
                  }
                  label="Marcar como folha de 13º salário"
                  sx={{ mb: 2 }}
                />
              </Box>

              <Box mb={2}>
                <input
                  ref={folhaAdpFileRef}
                  type="file"
                  accept=".txt"
                  style={{ display: 'none' }}
                  onChange={handleFolhaAdpFileChange}
                />
                <Button
                  variant="outlined"
                  startIcon={<CloudUploadIcon />}
                  onClick={() => folhaAdpFileRef.current?.click()}
                  fullWidth
                  sx={{ mb: 1 }}
                >
                  Selecionar Arquivo (.txt)
                </Button>

                <Typography variant="body2" color="primary">
                  Arquivo selecionado: {folhaAdpFileName || ''}
                </Typography>
              </Box>

              <Box display="flex" gap={1}>
                <Button
                  variant="contained"
                  onClick={() => handleFolhaAdpUpload(folhaAdpFileRef.current?.files?.[0] ?? null)}
                  disabled={folhaAdpState.loading || !folhaAdpFileRef.current?.files?.[0]}
                  startIcon={
                    folhaAdpState.loading ? <CircularProgress size={20} /> : <DescriptionIcon />
                  }
                  fullWidth
                >
                  {folhaAdpState.loading ? 'Importando e processando ficha…' : 'Importar Folha ADP'}
                </Button>

                {folhaAdpState.success && (
                  <Button variant="outlined" onClick={resetFolhaAdpState} size="small">
                    Novo
                  </Button>
                )}
              </Box>
            </CardContent>
          </Card>

          <Card sx={{ mt: 2 }}>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Processar ficha da competência
              </Typography>
              <Typography variant="body2" color="text.secondary" component="p" sx={{ mb: 2 }}>
                Reprocesse a ficha mensal de uma competência já importada, sem reenviar o arquivo ADP.
              </Typography>

              <Box
                sx={{ display: 'flex', flexDirection: { xs: 'column', sm: 'row' }, gap: 2, mb: 2 }}
              >
                <FormControl sx={{ minWidth: { xs: '100%', sm: 180 } }}>
                  <InputLabel id="processamento-mes-label">Mês</InputLabel>
                  <Select
                    labelId="processamento-mes-label"
                    label="Mês"
                    value={mesProcessamento}
                    onChange={(e) => setMesProcessamento(Number(e.target.value))}
                    disabled={processamentoManualLoading}
                  >
                    {MESES.map((item) => (
                      <MenuItem key={item.valor} value={item.valor}>
                        {item.label}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
                <FormControl sx={{ minWidth: { xs: '100%', sm: 120 } }}>
                  <InputLabel id="processamento-ano-label">Ano</InputLabel>
                  <Select
                    labelId="processamento-ano-label"
                    label="Ano"
                    value={anoProcessamento}
                    onChange={(e) => setAnoProcessamento(Number(e.target.value))}
                    disabled={processamentoManualLoading}
                  >
                    {anosDisponiveis.map((itemAno) => (
                      <MenuItem key={itemAno} value={itemAno}>
                        {itemAno}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Box>

              <FormControlLabel
                control={
                  <Checkbox
                    checked={decimoTerceiroProcessamento}
                    onChange={(e) => setDecimoTerceiroProcessamento(e.target.checked)}
                    color="secondary"
                    disabled={processamentoManualLoading}
                  />
                }
                label="Marcar como folha de 13º salário"
                sx={{ mb: 1, display: 'block' }}
              />
              <FormControlLabel
                control={
                  <Checkbox
                    checked={recalcularFerias}
                    onChange={(e) => setRecalcularFerias(e.target.checked)}
                    color="secondary"
                    disabled={processamentoManualLoading}
                  />
                }
                label="Recalcular férias proporcionais"
                sx={{ mb: 2, display: 'block' }}
              />

              <Button
                variant="contained"
                onClick={handleProcessarCompetencia}
                disabled={processamentoManualLoading}
                startIcon={
                  processamentoManualLoading ? <CircularProgress size={20} /> : <DescriptionIcon />
                }
                fullWidth
              >
                {processamentoManualLoading ? 'Processando ficha…' : 'Processar'}
              </Button>
            </CardContent>
          </Card>
        </Box>
      </Box>
      <Card sx={{ mt: 4 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Status da Importação
          </Typography>
          {(beneficiosMensaisState.loading || folhaAdpState.loading) && (
            <Box display="flex" alignItems="center" mb={2}>
              <CircularProgress size={20} sx={{ mr: 1 }} />
              <Typography variant="body2">
                {folhaAdpState.loading
                  ? 'Importando e processando ficha…'
                  : 'Processando arquivo...'}
              </Typography>
            </Box>
          )}
          {(beneficiosMensaisState.success || folhaAdpState.success) && (
            <Alert severity="success" sx={{ mb: 2 }}>
              <Typography variant="body2">
                Importação realizada com sucesso!
                {beneficiosMensaisState.success && beneficiosMensaisState.arquivo && (
                  <>
                    <br />
                    Arquivo: {beneficiosMensaisState.arquivo}
                  </>
                )}
                {folhaAdpState.success && folhaAdpState.arquivo && (
                  <>
                    <br />
                    Arquivo: {folhaAdpState.arquivo}
                  </>
                )}
                {beneficiosMensaisState.success && beneficiosMensaisState.tamanho && (
                  <>
                    <br />
                    Tamanho: {(beneficiosMensaisState.tamanho / 1024).toFixed(2)} KB
                  </>
                )}
                {folhaAdpState.success && folhaAdpState.tamanho && (
                  <>
                    <br />
                    Tamanho: {(folhaAdpState.tamanho / 1024).toFixed(2)} KB
                  </>
                )}
                {beneficiosMensaisState.success &&
                  beneficiosMensaisState.registrosProcessados !== undefined && (
                    <>
                      <br />
                      Registros processados: {beneficiosMensaisState.registrosProcessados}
                    </>
                  )}
                {folhaAdpState.success && folhaAdpState.registrosProcessados && (
                  <>
                    <br />
                    Registros processados: {folhaAdpState.registrosProcessados}
                  </>
                )}
                {beneficiosMensaisState.success && beneficiosMensaisState.errosCount !== undefined && (
                  <>
                    <br />
                    Erros: {beneficiosMensaisState.errosCount}
                  </>
                )}
                {beneficiosMensaisState.success && beneficiosMensaisState.totalValor !== undefined && (
                  <>
                    <br />
                    Valor total:{' '}
                    {beneficiosMensaisState.totalValor.toLocaleString('pt-BR', {
                      style: 'currency',
                      currency: 'BRL',
                    })}
                  </>
                )}
              </Typography>
            </Alert>
          )}
          {(beneficiosMensaisState.error || folhaAdpState.error) && (
            <Alert severity="error" sx={{ mb: 2 }}>
              <Typography variant="body2">
                {beneficiosMensaisState.error || folhaAdpState.error}
              </Typography>
            </Alert>
          )}
          {((beneficiosMensaisState.erros && beneficiosMensaisState.erros.length > 0) ||
            (folhaAdpState.erros && folhaAdpState.erros.length > 0)) && (
            <Paper sx={{ p: 2, maxHeight: 200, overflow: 'auto' }}>
              <Typography variant="subtitle2" color="error" gutterBottom>
                Erros encontrados:
              </Typography>
              <List dense>
                {beneficiosMensaisState.erros &&
                  beneficiosMensaisState.erros.map((erro, index) => (
                    <ListItem key={'beneficio-' + index}>
                      <ListItemIcon>
                        <ErrorIcon color="error" fontSize="small" />
                      </ListItemIcon>
                      <ListItemText primary={erro} />
                    </ListItem>
                  ))}
                {folhaAdpState.erros &&
                  folhaAdpState.erros.map((erro, index) => (
                    <ListItem key={'adp-' + index}>
                      <ListItemIcon>
                        <ErrorIcon color="error" fontSize="small" />
                      </ListItemIcon>
                      <ListItemText primary={erro} />
                    </ListItem>
                  ))}
              </List>
            </Paper>
          )}
        </CardContent>
      </Card>

      <Dialog open={conflictDialogOpen} onClose={handleCancelSubstituicao}>
        <DialogTitle>{conflictTitle}</DialogTitle>
        <DialogContent>
          <Alert severity="warning" sx={{ mb: 2 }}>
            {conflictMessage}
          </Alert>
          <Typography variant="body2">{conflictDescription}</Typography>
          <Typography variant="body2" sx={{ mt: 2, fontWeight: 'bold' }}>
            Deseja continuar?
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCancelSubstituicao} color="inherit">
            Cancelar
          </Button>
          <Button onClick={handleConfirmSubstituicao} variant="contained" color="warning">
            Confirmar Substituição
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
