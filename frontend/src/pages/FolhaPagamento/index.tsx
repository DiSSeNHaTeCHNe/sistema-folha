import { useState, useEffect, useMemo } from 'react';
import {
  Box,
  Button,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  TextField,
  InputAdornment,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Card,
  CardContent,
  FormControl,
  FormHelperText,
  InputLabel,
  Select,
  MenuItem,
  Tabs,
  Tab,
} from '@mui/material';
import {
  Search as SearchIcon,
  Visibility as VisibilityIcon,
  CardGiftcard as CardGiftcardIcon,
  AttachMoney as AttachMoneyIcon,
} from '@mui/icons-material';
import { Controller, useForm } from 'react-hook-form';
import {
  folhaPagamentoService,
  type FichaLinhaDetalhe,
  type TotalizadorFolha,
} from '../../services/folhaPagamentoService';
import { resumoFolhaPagamentoService, type ResumoFolhaPagamento } from '../../services/resumoFolhaPagamentoService';
import { centroCustoService } from '../../services/centroCustoService';
import { linhaNegocioService } from '../../services/linhaNegocioService';
import { formatMoneyDisplay } from '../../utils/money';

interface CentroCusto {
  id: number;
  descricao: string;
}

interface LinhaNegocio {
  id: number;
  descricao: string;
}

interface FiltrosResumo {
  mes: string;
  ano: string;
}

interface FiltrosFuncionarios {
  linhaNegocioId: string | number;
  centroCustoId: string | number;
  busca: string;
}

interface FuncionarioResumo {
  funcionarioId: number;
  funcionarioNome: string;
  dataInicio: string;
  dataFim: string;
  totalRubricas: number;
  salBruto: string | number;
  salLiquido: string | number;
  salCustoFolha?: string | number;
  salCustoBeneficios?: string | number;
  custoEmpresa: string | number;
  cargoDescricao?: string;
  centroCustoDescricao?: string;
  linhaNegocioDescricao?: string;
}

const TOTALIZADORES: { label: string; value: TotalizadorFolha; panelId: string; tabId: string }[] = [
  { label: 'Bruto', value: 'GROSS', panelId: 'folha-detalhe-bruto', tabId: 'folha-tab-bruto' },
  { label: 'Líquido', value: 'NET', panelId: 'folha-detalhe-liquido', tabId: 'folha-tab-liquido' },
  { label: 'Custo', value: 'COMPANY_COST', panelId: 'folha-detalhe-custo', tabId: 'folha-tab-custo' },
];

const ORIGEM_LABELS: Record<string, string> = {
  FOLHA_ADP: 'Folha ADP',
  CUSTO_FIXO: 'Custo Fixo',
  CALCULADO: 'Calculado',
  BENEFICIO: 'Benefício',
};

const ORIGEM_ORDER = ['FOLHA_ADP', 'CUSTO_FIXO', 'CALCULADO', 'BENEFICIO'] as const;

const parseMoneyValue = (value: string | number): number => {
  const normalized = typeof value === 'string' ? value.replace(',', '.') : String(value);
  const parsed = Number.parseFloat(normalized);
  return Number.isNaN(parsed) ? 0 : parsed;
};

const sumContribuicoes = (linhas: FichaLinhaDetalhe[]): number =>
  linhas.reduce((acc, linha) => acc + parseMoneyValue(linha.contribuicao), 0);

const formatPercentual = (
  porcentagem: string | number | null | undefined,
  origemLinha: string,
): string => {
  if (origemLinha === 'BENEFICIO') {
    return '—';
  }
  if (porcentagem === null || porcentagem === undefined || porcentagem === '') {
    return '100,00%';
  }
  const normalized = typeof porcentagem === 'string' ? porcentagem.replace(',', '.') : String(porcentagem);
  const parsed = Number.parseFloat(normalized);
  if (Number.isNaN(parsed)) {
    return '100,00%';
  }
  return `${parsed.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}%`;
};

const gerarAnosDisponiveis = (): number[] => {
  const anoAtual = new Date().getFullYear();
  return Array.from({ length: 6 }, (_, i) => anoAtual - i);
};

const anoCorrente = (): string => String(new Date().getFullYear());

// Função utilitária para formatar datas do backend (formato ISO)
const formatarDataCompetencia = (dataString: string): string => {
  if (!dataString) return '';
  
  // Se a data já está no formato ISO (YYYY-MM-DD), converte para DD/MM/YYYY
  if (dataString.includes('-')) {
    const [ano, mes, dia] = dataString.split('-');
    return `${dia}/${mes}/${ano}`;
  }
  
  // Se já está no formato DD/MM/YYYY, retorna como está
  return dataString;
};

export function FolhaPagamento() {
  const [centrosCusto, setCentrosCusto] = useState<CentroCusto[]>([]);
  const [linhasNegocio, setLinhasNegocio] = useState<LinhaNegocio[]>([]);
  const [funcionariosResumo, setFuncionariosResumo] = useState<FuncionarioResumo[]>([]);
  const [resumosFolha, setResumosFolha] = useState<ResumoFolhaPagamento[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [openDetalhesDialog, setOpenDetalhesDialog] = useState(false);
  const [funcionarioSelecionado, setFuncionarioSelecionado] = useState<FuncionarioResumo | null>(null);
  const [fichaId, setFichaId] = useState<number | null>(null);
  const [abaDetalhe, setAbaDetalhe] = useState(0);
  const [linhasDetalhe, setLinhasDetalhe] = useState<FichaLinhaDetalhe[]>([]);
  const [detalheErro, setDetalheErro] = useState('');
  const [resumoSelecionado, setResumoSelecionado] = useState<ResumoFolhaPagamento | null>(null);
  const [mostrarFuncionarios, setMostrarFuncionarios] = useState(false);

  // Formulário para filtros da tela de Resumo
  const { control: controlResumo, handleSubmit: handleSubmitResumo, reset: resetResumo } = useForm<FiltrosResumo>({
    defaultValues: {
      mes: '',
      ano: anoCorrente(),
    },
  });

  const anosDisponiveis = useMemo(() => gerarAnosDisponiveis(), []);

  // Formulário para filtros da tela de Funcionários
  const { control: controlFuncionarios, reset: resetFuncionarios, watch: watchFuncionarios } = useForm<FiltrosFuncionarios>({
    defaultValues: {
      linhaNegocioId: '',
      centroCustoId: '',
      busca: ''
    }
  });

  const filtrosFuncionarios = watchFuncionarios();

  const carregarOpcoesDeFilters = async () => {
    try {
      const [centrosCustoData, linhasNegocioData] = await Promise.all([
        centroCustoService.listarTodos(),
        linhaNegocioService.listarTodos(),
      ]);
      
      // Ordenar em ordem alfabética crescente
      const centrosCustoOrdenados = [...centrosCustoData].sort((a, b) => 
        a.descricao.localeCompare(b.descricao)
      );
      const linhasNegocioOrdenadas = [...linhasNegocioData].sort((a, b) => 
        a.descricao.localeCompare(b.descricao)
      );
      
      console.log('Linhas de Negócio carregadas:', linhasNegocioOrdenadas);
      console.log('Centros de Custo carregados:', centrosCustoOrdenados);
      
      setCentrosCusto(centrosCustoOrdenados);
      setLinhasNegocio(linhasNegocioOrdenadas);
    } catch (error) {
      console.error('Erro ao carregar opções de filtros:', error);
    }
  };

  const fetchResumosFolha = async (filtros?: FiltrosResumo) => {
    setLoading(true);
    try {
      const ano = Number(filtros?.ano ?? anoCorrente());
      const mes = filtros?.mes ? Number(filtros.mes) : undefined;
      const resumos = await resumoFolhaPagamentoService.listarPorAno(ano, mes);

      const resumosOrdenados = [...resumos].sort((a, b) => {
        const dataA = new Date(a.competenciaInicio).getTime();
        const dataB = new Date(b.competenciaInicio).getTime();
        return dataB - dataA;
      });

      setResumosFolha(resumosOrdenados);
    } catch (err) {
      console.log('Nenhum resumo encontrado', err);
      setResumosFolha([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchFuncionariosPorResumo = async (resumo: ResumoFolhaPagamento) => {
    setLoading(true);
    try {
      const totais = await folhaPagamentoService.consultarTotaisPorFuncionario(
        resumo.competenciaInicio,
        resumo.competenciaFim,
        resumo.decimoTerceiro ?? false,
      );

      const funcionariosArray: FuncionarioResumo[] = totais.map((item) => ({
        funcionarioId: item.funcionarioId,
        funcionarioNome: item.funcionarioNome,
        dataInicio: item.competenciaInicio,
        dataFim: item.competenciaFim,
        totalRubricas: item.totalRubricas,
        salBruto: item.salBruto,
        salLiquido: item.salLiquido,
        salCustoFolha: item.salCustoFolha,
        salCustoBeneficios: item.salCustoBeneficios,
        custoEmpresa: item.custoEmpresa,
        cargoDescricao: item.cargoDescricao,
        centroCustoDescricao: item.centroCustoDescricao,
        linhaNegocioDescricao: item.linhaNegocioDescricao,
      }));

      setFuncionariosResumo(funcionariosArray);
    } catch (err) {
      console.error('Erro ao buscar funcionários:', err);
      setError('Erro ao buscar funcionários');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    carregarOpcoesDeFilters();
    fetchResumosFolha({ ano: anoCorrente(), mes: '' });
  }, []);

  const handleFiltrarResumos = async (filtros: FiltrosResumo) => {
    await fetchResumosFolha(filtros);
  };

  const handleLimparFiltrosResumo = () => {
    const ano = anoCorrente();
    resetResumo({ mes: '', ano });
    fetchResumosFolha({ mes: '', ano });
  };

  const carregarLinhasDetalhe = async (fichaMensalId: number, totalizer: TotalizadorFolha) => {
    const linhas = await folhaPagamentoService.listarLinhasPorTotalizador(fichaMensalId, totalizer);
    setLinhasDetalhe(linhas);
  };

  const handleDetalharRubricas = async (funcionario: FuncionarioResumo) => {
    if (!resumoSelecionado) {
      return;
    }

    setFuncionarioSelecionado(funcionario);
    setAbaDetalhe(0);
    setLinhasDetalhe([]);
    setDetalheErro('');
    setLoading(true);
    try {
      const idFicha = await folhaPagamentoService.buscarFichaPorFuncionario(
        funcionario.funcionarioId,
        funcionario.dataInicio,
        funcionario.dataFim,
        resumoSelecionado.decimoTerceiro ?? false,
      );

      if (idFicha === null) {
        setDetalheErro('Ficha não processada para este funcionário. Execute o processamento da competência.');
        setFichaId(null);
        setOpenDetalhesDialog(true);
        return;
      }

      setFichaId(idFicha);
      await carregarLinhasDetalhe(idFicha, TOTALIZADORES[0].value);
      setOpenDetalhesDialog(true);
    } catch (err) {
      console.error('Erro ao carregar detalhe do funcionário:', err);
      setDetalheErro('Erro ao carregar detalhe do funcionário');
      setOpenDetalhesDialog(true);
    } finally {
      setLoading(false);
    }
  };

  const handleChangeAbaDetalhe = async (_event: React.SyntheticEvent, newValue: number) => {
    setAbaDetalhe(newValue);
    if (fichaId === null) {
      return;
    }

    setLoading(true);
    try {
      await carregarLinhasDetalhe(fichaId, TOTALIZADORES[newValue].value);
    } catch (err) {
      console.error('Erro ao carregar linhas do totalizador:', err);
      setDetalheErro('Erro ao carregar linhas do totalizador');
    } finally {
      setLoading(false);
    }
  };

  const getCardTotal = (totalizer: TotalizadorFolha): string | number => {
    if (!funcionarioSelecionado) {
      return 0;
    }
    switch (totalizer) {
      case 'GROSS':
        return funcionarioSelecionado.salBruto;
      case 'NET':
        return funcionarioSelecionado.salLiquido;
      case 'COMPANY_COST':
        return funcionarioSelecionado.custoEmpresa;
    }
  };

  const renderDetalheAgrupado = (
    totalizer: TotalizadorFolha,
    linhas: FichaLinhaDetalhe[],
    cardTotal: string | number,
  ) => {
    const origensPermitidas =
      totalizer === 'COMPANY_COST'
        ? ORIGEM_ORDER
        : ORIGEM_ORDER.filter((origem) => origem !== 'BENEFICIO');

    const grupos = linhas.reduce<Record<string, FichaLinhaDetalhe[]>>((acc, linha) => {
      const origem = linha.origemLinha || 'FOLHA_ADP';
      if (!acc[origem]) {
        acc[origem] = [];
      }
      acc[origem].push(linha);
      return acc;
    }, {});

    const origensOrdenadas = origensPermitidas.filter((origem) => (grupos[origem]?.length ?? 0) > 0);

    if (linhas.length === 0) {
      return (
        <Box>
          <Typography color="textSecondary" sx={{ py: 2 }}>
            Nenhuma rubrica encontrada para este totalizador.
          </Typography>
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 2 }}>
            <Typography variant="subtitle1" fontWeight="bold">
              Total: {formatMoneyDisplay(0)}
            </Typography>
          </Box>
        </Box>
      );
    }

    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
        {origensOrdenadas.map((origem) => {
          const linhasGrupo = grupos[origem];
          const subtotal = sumContribuicoes(linhasGrupo);
          return (
            <Box key={origem}>
              <Typography variant="subtitle1" gutterBottom>
                {ORIGEM_LABELS[origem] ?? origem}
              </Typography>
              <TableContainer component={Paper}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Rubrica</TableCell>
                      <TableCell align="right">Valor</TableCell>
                      <TableCell align="right">Percentual</TableCell>
                      <TableCell align="right">Contribuição</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {linhasGrupo.map((item, index) => (
                      <TableRow key={`${origem}-${item.rubricaCodigo}-${index}`}>
                        <TableCell>
                          {item.rubricaCodigo} - {item.rubricaDescricao}
                        </TableCell>
                        <TableCell align="right">{formatMoneyDisplay(item.valor)}</TableCell>
                        <TableCell align="right">{formatPercentual(item.porcentagem, origem)}</TableCell>
                        <TableCell align="right">{formatMoneyDisplay(item.contribuicao)}</TableCell>
                      </TableRow>
                    ))}
                    <TableRow>
                      <TableCell colSpan={3} align="right">
                        <strong>Subtotal</strong>
                      </TableCell>
                      <TableCell align="right">
                        <strong>{formatMoneyDisplay(subtotal)}</strong>
                      </TableCell>
                    </TableRow>
                  </TableBody>
                </Table>
              </TableContainer>
            </Box>
          );
        })}
        <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
          <Typography variant="subtitle1" fontWeight="bold">
            Total: {formatMoneyDisplay(cardTotal)}
          </Typography>
        </Box>
      </Box>
    );
  };

  const renderLinhasDetalhe = (totalizer: TotalizadorFolha) => {
    if (detalheErro) {
      return (
        <Typography color="error" sx={{ py: 2 }}>
          {detalheErro}
        </Typography>
      );
    }

    return renderDetalheAgrupado(totalizer, linhasDetalhe, getCardTotal(totalizer));
  };

  const handleVerFuncionarios = async (resumo: ResumoFolhaPagamento) => {
    setResumoSelecionado(resumo);
    setMostrarFuncionarios(true);
    resetFuncionarios(); // Limpa os filtros de funcionários
    await fetchFuncionariosPorResumo(resumo);
  };

  const handleVoltarParaResumos = () => {
    setMostrarFuncionarios(false);
    setResumoSelecionado(null);
    setFuncionariosResumo([]);
  };

  // Aplicar filtros na lista de funcionários
  const filteredFuncionarios = funcionariosResumo.filter((item) => {
    const buscaMatch = !filtrosFuncionarios.busca || 
      item.funcionarioNome.toLowerCase().includes(filtrosFuncionarios.busca.toLowerCase());
    
    // Filtro de Linha de Negócio
    let linhaNegocioMatch = true;
    if (filtrosFuncionarios.linhaNegocioId && filtrosFuncionarios.linhaNegocioId !== '') {
      // Normalizar comparação: converter ambos para string
      const linhaSelecionada = linhasNegocio.find(l => 
        l.id.toString() === filtrosFuncionarios.linhaNegocioId.toString()
      );
      if (linhaSelecionada) {
        linhaNegocioMatch = item.linhaNegocioDescricao === linhaSelecionada.descricao;
      } else {
        linhaNegocioMatch = false; // Se não encontrar, não deve passar no filtro
      }
    }
    
    // Filtro de Centro de Custo
    let centroCustoMatch = true;
    if (filtrosFuncionarios.centroCustoId && filtrosFuncionarios.centroCustoId !== '') {
      // Normalizar comparação: converter ambos para string
      const centroSelecionado = centrosCusto.find(c => 
        c.id.toString() === filtrosFuncionarios.centroCustoId.toString()
      );
      if (centroSelecionado) {
        centroCustoMatch = item.centroCustoDescricao === centroSelecionado.descricao;
      } else {
        centroCustoMatch = false; // Se não encontrar, não deve passar no filtro
      }
    }
    
    return buscaMatch && linhaNegocioMatch && centroCustoMatch;
  });

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h4">Folha de Pagamento</Typography>
      </Box>

      {loading ? (
        <Typography>Carregando...</Typography>
      ) : error ? (
        <Typography color="error">{error}</Typography>
      ) : mostrarFuncionarios ? (
        <>
          {/* Tela de Funcionários */}
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
            <Button
              variant="outlined"
              onClick={handleVoltarParaResumos}
              sx={{ mr: 2 }}
            >
              ← Voltar
            </Button>
            <Typography variant="h5">
              Funcionários - Competência: {resumoSelecionado && (
                `${formatarDataCompetencia(resumoSelecionado.competenciaInicio)} a ${formatarDataCompetencia(resumoSelecionado.competenciaFim)}`
              )}
            </Typography>
          </Box>
          
          {/* Filtros da Tela de Funcionários */}
          <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
            <FormControl variant="outlined" sx={{ minWidth: 200 }}>
              <InputLabel id="linha-negocio-label">Linha de Negócio</InputLabel>
              <Controller
                name="linhaNegocioId"
                control={controlFuncionarios}
                render={({ field }) => (
                  <Select
                    labelId="linha-negocio-label"
                    label="Linha de Negócio"
                    value={field.value}
                    onChange={field.onChange}
                    onBlur={field.onBlur}
                    fullWidth
                  >
                    <MenuItem value="">Todas</MenuItem>
                    {linhasNegocio.map((linha) => (
                      <MenuItem key={linha.id} value={linha.id}>
                        {linha.descricao}
                      </MenuItem>
                    ))}
                  </Select>
                )}
              />
            </FormControl>
            <FormControl variant="outlined" sx={{ minWidth: 200 }}>
              <InputLabel id="centro-custo-label">Centro de Custo</InputLabel>
              <Controller
                name="centroCustoId"
                control={controlFuncionarios}
                render={({ field }) => (
                  <Select
                    labelId="centro-custo-label"
                    label="Centro de Custo"
                    value={field.value}
                    onChange={field.onChange}
                    onBlur={field.onBlur}
                    fullWidth
                  >
                    <MenuItem value="">Todos</MenuItem>
                    {centrosCusto.map((centro) => (
                      <MenuItem key={centro.id} value={centro.id}>
                        {centro.descricao}
                      </MenuItem>
                    ))}
                  </Select>
                )}
              />
            </FormControl>
            <Controller
              name="busca"
              control={controlFuncionarios}
              render={({ field }) => (
                <TextField
                  {...field}
                  label="Buscar funcionário"
                  variant="outlined"
                  sx={{ minWidth: 300 }}
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <SearchIcon />
                      </InputAdornment>
                    ),
                  }}
                />
              )}
            />
            <Button 
              variant="text" 
              onClick={() => resetFuncionarios()}
            >
              Limpar
            </Button>
          </Box>
          
          <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: 2 }}>
            {filteredFuncionarios.map((funcionario) => (
              <Card key={`${funcionario.funcionarioId}-${funcionario.dataInicio}`}>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    {funcionario.funcionarioNome}
                  </Typography>
                  <Typography color="textSecondary" gutterBottom>
                    Cargo: {funcionario.cargoDescricao || '-'}
                  </Typography>
                  <Typography color="textSecondary" gutterBottom>
                    Centro de Custo: {funcionario.centroCustoDescricao || '-'}
                  </Typography>
                  <Typography color="textSecondary" gutterBottom>
                    Linha de Negócio: {funcionario.linhaNegocioDescricao || '-'}
                  </Typography>
                  <Typography color="textSecondary" gutterBottom>
                    Bruto: {formatMoneyDisplay(funcionario.salBruto)}
                  </Typography>
                  <Typography color="textSecondary" gutterBottom>
                    Líquido: {formatMoneyDisplay(funcionario.salLiquido)}
                  </Typography>
                  <Typography color="textSecondary" gutterBottom>
                    Custo Empresa: {formatMoneyDisplay(funcionario.custoEmpresa)}
                  </Typography>
                  {(funcionario.salCustoFolha != null || funcionario.salCustoBeneficios != null) && (
                    <Typography variant="caption" color="textSecondary" display="block" sx={{ mt: -1, mb: 1 }}>
                      Folha: {formatMoneyDisplay(funcionario.salCustoFolha ?? 0)}
                      {' · '}
                      Benefícios: {formatMoneyDisplay(funcionario.salCustoBeneficios ?? 0)}
                    </Typography>
                  )}
                  <Box sx={{ mt: 2 }}>
                    <Button
                      variant="outlined"
                      startIcon={<VisibilityIcon />}
                      onClick={() => handleDetalharRubricas(funcionario)}
                      fullWidth
                    >
                      Ver Rubricas
                    </Button>
                  </Box>
                </CardContent>
              </Card>
            ))}
          </Box>
          
          {filteredFuncionarios.length === 0 && (
            <Typography color="textSecondary" align="center" sx={{ mt: 4 }}>
              Nenhum funcionário encontrado para este período.
            </Typography>
          )}
        </>
      ) : (
        <>
          {/* Tela de Resumos */}
          <Typography variant="h5" gutterBottom sx={{ mb: 2 }}>
            Resumos da Folha de Pagamento
          </Typography>
          
          {/* Filtros da Tela de Resumo */}
          <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
            <Controller
              name="mes"
              control={controlResumo}
              render={({ field }) => (
                <TextField
                  {...field}
                  label="Mês"
                  type="number"
                  sx={{ minWidth: 120 }}
                  inputProps={{ min: 1, max: 12 }}
                />
              )}
            />
            <Controller
              name="ano"
              control={controlResumo}
              rules={{ required: 'Ano é obrigatório' }}
              render={({ field, fieldState: { error } }) => (
                <FormControl sx={{ minWidth: 120 }} error={!!error}>
                  <InputLabel id="folha-ano-label">Ano</InputLabel>
                  <Select
                    labelId="folha-ano-label"
                    label="Ano"
                    value={field.value}
                    onChange={field.onChange}
                    onBlur={field.onBlur}
                  >
                    {anosDisponiveis.map((itemAno) => (
                      <MenuItem key={itemAno} value={String(itemAno)}>
                        {itemAno}
                      </MenuItem>
                    ))}
                  </Select>
                  {error && <FormHelperText>{error.message}</FormHelperText>}
                </FormControl>
              )}
            />
            <Button 
              variant="outlined" 
              onClick={handleSubmitResumo(handleFiltrarResumos)}
            >
              Filtrar
            </Button>
            <Button 
              variant="text" 
              onClick={handleLimparFiltrosResumo}
            >
              Limpar
            </Button>
          </Box>
          
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Competência</TableCell>
                  <TableCell align="center">Tipo</TableCell>
                  <TableCell align="right">Total Empregados</TableCell>
                  <TableCell align="right">Total Encargos</TableCell>
                  <TableCell align="right">Total Pagamentos</TableCell>
                  <TableCell align="right">Total Descontos</TableCell>
                  <TableCell align="right">Total Bruto</TableCell>
                  <TableCell align="right">Total Líquido</TableCell>
                  <TableCell align="right">Custo Empresa</TableCell>
                  <TableCell align="center">Ações</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {resumosFolha.map((resumo) => (
                  <TableRow key={resumo.id}>
                    <TableCell>
                      {formatarDataCompetencia(resumo.competenciaInicio)} a {formatarDataCompetencia(resumo.competenciaFim)}
                    </TableCell>
                    <TableCell align="center">
                      {resumo.decimoTerceiro ? (
                        <Box display="flex" alignItems="center" justifyContent="center" gap={0.5}>
                          <CardGiftcardIcon color="secondary" />
                          <Typography variant="body2" color="secondary">13º</Typography>
                        </Box>
                      ) : (
                        <Box display="flex" alignItems="center" justifyContent="center" gap={0.5}>
                          <AttachMoneyIcon color="primary" />
                          <Typography variant="body2" color="primary">Normal</Typography>
                        </Box>
                      )}
                    </TableCell>
                    <TableCell align="right">{resumo.totalEmpregados}</TableCell>
                    <TableCell align="right">
                      {formatMoneyDisplay(resumo.totalEncargos)}
                    </TableCell>
                    <TableCell align="right">
                      {formatMoneyDisplay(resumo.totalPagamentos)}
                    </TableCell>
                    <TableCell align="right">
                      {formatMoneyDisplay(resumo.totalDescontos)}
                    </TableCell>
                    <TableCell align="right">
                      {formatMoneyDisplay(resumo.totalBruto)}
                    </TableCell>
                    <TableCell align="right">
                      <Typography color="primary" variant="body1">
                        {formatMoneyDisplay(resumo.totalLiquido)}
                      </Typography>
                    </TableCell>
                    <TableCell align="right">
                      <Typography color="success.main" variant="body1" fontWeight="medium">
                        {formatMoneyDisplay(resumo.totalCustoEmpresa)}
                      </Typography>
                    </TableCell>
                    <TableCell align="center">
                      <Button
                        variant="outlined"
                        startIcon={<VisibilityIcon />}
                        onClick={() => handleVerFuncionarios(resumo)}
                        size="small"
                      >
                        Ver Funcionários
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          
          {resumosFolha.length === 0 && (
            <Typography color="textSecondary" align="center" sx={{ mt: 4 }}>
              Nenhum resumo de folha de pagamento encontrado.
            </Typography>
          )}
        </>
      )}

      {/* Dialog para detalhes por totalizador */}
      <Dialog
        open={openDetalhesDialog}
        onClose={() => setOpenDetalhesDialog(false)}
        maxWidth="lg"
        fullWidth
      >
        <DialogTitle>
          Detalhe de {funcionarioSelecionado?.funcionarioNome} —{' '}
          {resumoSelecionado?.decimoTerceiro ? '13º salário' : 'Folha regular'} — Período:{' '}
          {formatarDataCompetencia(funcionarioSelecionado?.dataInicio || '')} a{' '}
          {formatarDataCompetencia(funcionarioSelecionado?.dataFim || '')}
        </DialogTitle>
        <DialogContent>
          {fichaId !== null && (
            <Tabs
              value={abaDetalhe}
              onChange={handleChangeAbaDetalhe}
              aria-label="Totalizadores da folha"
              sx={{ mb: 2, borderBottom: 1, borderColor: 'divider' }}
            >
              {TOTALIZADORES.map((tab, index) => (
                <Tab
                  key={tab.value}
                  label={tab.label}
                  id={tab.tabId}
                  aria-controls={tab.panelId}
                  disabled={loading && abaDetalhe !== index}
                />
              ))}
            </Tabs>
          )}
          {TOTALIZADORES.map((tab, index) => (
            <Box
              key={tab.value}
              role="tabpanel"
              hidden={abaDetalhe !== index}
              id={tab.panelId}
              aria-labelledby={tab.tabId}
            >
              {abaDetalhe === index && renderLinhasDetalhe(tab.value)}
            </Box>
          ))}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDetalhesDialog(false)}>Fechar</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
} 