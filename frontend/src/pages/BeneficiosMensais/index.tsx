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
} from '@mui/material';
import {
  Search as SearchIcon,
  Visibility as VisibilityIcon,
} from '@mui/icons-material';
import { Controller, useForm } from 'react-hook-form';
import type { BeneficioMensal, BeneficioMensalCompetenciaResumo } from '../../types';
import { beneficioMensalService } from '../../services/beneficioMensalService';
import { centroCustoService } from '../../services/centroCustoService';
import { linhaNegocioService } from '../../services/linhaNegocioService';

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
  competenciaInicio: string;
  competenciaFim: string;
  totalBeneficios: number;
  qtdLancamentos: number;
  cargoDescricao?: string;
  centroCustoDescricao?: string;
  linhaNegocioDescricao?: string;
}

const gerarAnosDisponiveis = (): number[] => {
  const anoAtual = new Date().getFullYear();
  return Array.from({ length: 6 }, (_, i) => anoAtual - i);
};

const anoCorrente = (): string => String(new Date().getFullYear());

const formatarDataCompetencia = (dataString: string): string => {
  if (!dataString) return '';

  if (dataString.includes('-')) {
    const [ano, mes, dia] = dataString.split('-');
    return `${dia}/${mes}/${ano}`;
  }

  return dataString;
};

export function BeneficiosMensais() {
  const [centrosCusto, setCentrosCusto] = useState<CentroCusto[]>([]);
  const [linhasNegocio, setLinhasNegocio] = useState<LinhaNegocio[]>([]);
  const [lancamentos, setLancamentos] = useState<BeneficioMensal[]>([]);
  const [funcionariosResumo, setFuncionariosResumo] = useState<FuncionarioResumo[]>([]);
  const [resumosCompetencia, setResumosCompetencia] = useState<BeneficioMensalCompetenciaResumo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [openDetalhesDialog, setOpenDetalhesDialog] = useState(false);
  const [funcionarioSelecionado, setFuncionarioSelecionado] = useState<FuncionarioResumo | null>(null);
  const [beneficiosFuncionario, setBeneficiosFuncionario] = useState<BeneficioMensal[]>([]);
  const [competenciaSelecionada, setCompetenciaSelecionada] = useState<BeneficioMensalCompetenciaResumo | null>(null);
  const [mostrarFuncionarios, setMostrarFuncionarios] = useState(false);

  const { control: controlResumo, handleSubmit: handleSubmitResumo, reset: resetResumo, getValues: getValuesResumo } = useForm<FiltrosResumo>({
    defaultValues: {
      mes: '',
      ano: anoCorrente(),
    },
  });

  const anosDisponiveis = useMemo(() => gerarAnosDisponiveis(), []);

  const { control: controlFuncionarios, reset: resetFuncionarios, watch: watchFuncionarios } = useForm<FiltrosFuncionarios>({
    defaultValues: {
      linhaNegocioId: '',
      centroCustoId: '',
      busca: '',
    },
  });

  const filtrosFuncionarios = watchFuncionarios();

  const carregarOpcoesDeFilters = async () => {
    try {
      const [centrosCustoData, linhasNegocioData] = await Promise.all([
        centroCustoService.listarTodos(),
        linhaNegocioService.listarTodos(),
      ]);

      const centrosCustoOrdenados = [...centrosCustoData].sort((a, b) =>
        a.descricao.localeCompare(b.descricao),
      );
      const linhasNegocioOrdenadas = [...linhasNegocioData].sort((a, b) =>
        a.descricao.localeCompare(b.descricao),
      );

      setCentrosCusto(centrosCustoOrdenados);
      setLinhasNegocio(linhasNegocioOrdenadas);
    } catch (err) {
      console.error('Erro ao carregar opções de filtros:', err);
    }
  };

  const fetchResumosCompetencia = async (filtros?: FiltrosResumo) => {
    setLoading(true);
    setError('');
    try {
      const ano = Number(filtros?.ano ?? anoCorrente());
      const mes = filtros?.mes ? Number(filtros.mes) : undefined;
      const resumos = await beneficioMensalService.listarCompetencias(ano, mes);

      const resumosOrdenados = [...resumos].sort((a, b) => {
        const dataA = new Date(a.competenciaInicio).getTime();
        const dataB = new Date(b.competenciaInicio).getTime();
        return dataB - dataA;
      });

      setResumosCompetencia(resumosOrdenados);
    } catch (err) {
      console.error('Erro ao buscar resumos de competência:', err);
      setError('Erro ao buscar resumos de benefícios');
      setResumosCompetencia([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchFuncionariosPorCompetencia = async (competencia: BeneficioMensalCompetenciaResumo) => {
    setLoading(true);
    setError('');
    try {
      const data = await beneficioMensalService.listar({
        competenciaInicio: competencia.competenciaInicio,
        competenciaFim: competencia.competenciaFim,
      });

      setLancamentos(data);

      const resumoMap = data.reduce((acc: Record<string, FuncionarioResumo>, item: BeneficioMensal) => {
        const key = `${item.funcionarioId}`;
        if (!acc[key]) {
          acc[key] = {
            funcionarioId: item.funcionarioId,
            funcionarioNome: item.funcionarioNome ?? '',
            competenciaInicio: item.competenciaInicio,
            competenciaFim: item.competenciaFim,
            totalBeneficios: 0,
            qtdLancamentos: 0,
            cargoDescricao: item.cargoDescricao,
            centroCustoDescricao: item.centroCustoDescricao,
            linhaNegocioDescricao: item.linhaNegocioDescricao,
          };
        }
        acc[key].totalBeneficios += item.valor;
        acc[key].qtdLancamentos += 1;
        return acc;
      }, {} as Record<string, FuncionarioResumo>);

      const funcionariosArray = Object.values(resumoMap) as FuncionarioResumo[];
      const funcionariosOrdenados = funcionariosArray.sort((a, b) =>
        a.funcionarioNome.localeCompare(b.funcionarioNome),
      );
      setFuncionariosResumo(funcionariosOrdenados);
    } catch (err) {
      console.error('Erro ao buscar funcionários:', err);
      setError('Erro ao buscar funcionários');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    carregarOpcoesDeFilters();
    fetchResumosCompetencia({ ano: anoCorrente(), mes: '' });
  }, []);

  const handleFiltrarResumos = async (filtros: FiltrosResumo) => {
    await fetchResumosCompetencia(filtros);
  };

  const handleLimparFiltrosResumo = () => {
    const ano = anoCorrente();
    resetResumo({ mes: '', ano });
    fetchResumosCompetencia({ mes: '', ano });
  };

  const handleDetalharBeneficios = (funcionario: FuncionarioResumo) => {
    setFuncionarioSelecionado(funcionario);
    const beneficios = lancamentos.filter(
      (item) => item.funcionarioId === funcionario.funcionarioId,
    );
    setBeneficiosFuncionario(beneficios);
    setOpenDetalhesDialog(true);
  };

  const handleVerFuncionarios = async (competencia: BeneficioMensalCompetenciaResumo) => {
    setCompetenciaSelecionada(competencia);
    setMostrarFuncionarios(true);
    resetFuncionarios();
    await fetchFuncionariosPorCompetencia(competencia);
  };

  const handleVoltarParaResumos = () => {
    setMostrarFuncionarios(false);
    setCompetenciaSelecionada(null);
    setFuncionariosResumo([]);
    setLancamentos([]);
    const filtrosAtuais = getValuesResumo();
    fetchResumosCompetencia(filtrosAtuais);
  };

  const filteredFuncionarios = funcionariosResumo.filter((item) => {
    const buscaMatch =
      !filtrosFuncionarios.busca ||
      item.funcionarioNome.toLowerCase().includes(filtrosFuncionarios.busca.toLowerCase());

    let linhaNegocioMatch = true;
    if (filtrosFuncionarios.linhaNegocioId && filtrosFuncionarios.linhaNegocioId !== '') {
      const linhaSelecionada = linhasNegocio.find(
        (l) => l.id.toString() === filtrosFuncionarios.linhaNegocioId.toString(),
      );
      if (linhaSelecionada) {
        linhaNegocioMatch = item.linhaNegocioDescricao === linhaSelecionada.descricao;
      } else {
        linhaNegocioMatch = false;
      }
    }

    let centroCustoMatch = true;
    if (filtrosFuncionarios.centroCustoId && filtrosFuncionarios.centroCustoId !== '') {
      const centroSelecionado = centrosCusto.find(
        (c) => c.id.toString() === filtrosFuncionarios.centroCustoId.toString(),
      );
      if (centroSelecionado) {
        centroCustoMatch = item.centroCustoDescricao === centroSelecionado.descricao;
      } else {
        centroCustoMatch = false;
      }
    }

    return buscaMatch && linhaNegocioMatch && centroCustoMatch;
  });

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h4">Benefícios Mensais</Typography>
      </Box>

      {loading ? (
        <Typography>Carregando...</Typography>
      ) : error ? (
        <Typography color="error">{error}</Typography>
      ) : mostrarFuncionarios ? (
        <>
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
            <Button
              variant="outlined"
              onClick={handleVoltarParaResumos}
              sx={{ mr: 2 }}
            >
              ← Voltar
            </Button>
            <Typography variant="h5">
              Funcionários - Competência:{' '}
              {competenciaSelecionada && (
                `${formatarDataCompetencia(competenciaSelecionada.competenciaInicio)} a ${formatarDataCompetencia(competenciaSelecionada.competenciaFim)}`
              )}
            </Typography>
          </Box>

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
            <Button variant="text" onClick={() => resetFuncionarios()}>
              Limpar
            </Button>
          </Box>

          <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: 2 }}>
            {filteredFuncionarios.map((funcionario) => (
              <Card key={`${funcionario.funcionarioId}-${funcionario.competenciaInicio}`}>
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
                    Total:{' '}
                    {new Intl.NumberFormat('pt-BR', {
                      style: 'currency',
                      currency: 'BRL',
                    }).format(funcionario.totalBeneficios)}
                  </Typography>
                  <Box sx={{ mt: 2 }}>
                    <Button
                      variant="outlined"
                      startIcon={<VisibilityIcon />}
                      onClick={() => handleDetalharBeneficios(funcionario)}
                      fullWidth
                    >
                      Ver Benefícios
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
          <Typography variant="h5" gutterBottom sx={{ mb: 2 }}>
            Resumos de Benefícios Mensais
          </Typography>

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
              render={({ field, fieldState: { error: fieldError } }) => (
                <FormControl sx={{ minWidth: 120 }} error={!!fieldError}>
                  <InputLabel id="beneficios-ano-label">Ano</InputLabel>
                  <Select
                    labelId="beneficios-ano-label"
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
                  {fieldError && <FormHelperText>{fieldError.message}</FormHelperText>}
                </FormControl>
              )}
            />
            <Button variant="outlined" onClick={handleSubmitResumo(handleFiltrarResumos)}>
              Filtrar
            </Button>
            <Button variant="text" onClick={handleLimparFiltrosResumo}>
              Limpar
            </Button>
          </Box>

          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Competência</TableCell>
                  <TableCell align="right">Total Funcionários</TableCell>
                  <TableCell align="right">Total (R$)</TableCell>
                  <TableCell align="right">Qtd. Lançamentos</TableCell>
                  <TableCell align="center">Ações</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {resumosCompetencia.map((resumo) => (
                  <TableRow
                    key={`${resumo.competenciaInicio}-${resumo.competenciaFim}`}
                  >
                    <TableCell>
                      {formatarDataCompetencia(resumo.competenciaInicio)} a{' '}
                      {formatarDataCompetencia(resumo.competenciaFim)}
                    </TableCell>
                    <TableCell align="right">{resumo.totalFuncionarios}</TableCell>
                    <TableCell align="right">
                      {new Intl.NumberFormat('pt-BR', {
                        style: 'currency',
                        currency: 'BRL',
                      }).format(resumo.totalBeneficios)}
                    </TableCell>
                    <TableCell align="right">{resumo.qtdLancamentos}</TableCell>
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

          {resumosCompetencia.length === 0 && (
            <Typography color="textSecondary" align="center" sx={{ mt: 4 }}>
              Nenhum benefício mensal encontrado.
            </Typography>
          )}
        </>
      )}

      <Dialog
        open={openDetalhesDialog}
        onClose={() => setOpenDetalhesDialog(false)}
        maxWidth="lg"
        fullWidth
      >
        <DialogTitle>
          Benefícios de {funcionarioSelecionado?.funcionarioNome} - Período:{' '}
          {formatarDataCompetencia(funcionarioSelecionado?.competenciaInicio || '')} a{' '}
          {formatarDataCompetencia(funcionarioSelecionado?.competenciaFim || '')}
        </DialogTitle>
        <DialogContent>
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Código</TableCell>
                  <TableCell>Descrição</TableCell>
                  <TableCell align="right">Valor</TableCell>
                  <TableCell>Observação</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {beneficiosFuncionario.map((item) => (
                  <TableRow key={item.id}>
                    <TableCell>{item.tipoBeneficioCodigo ?? '—'}</TableCell>
                    <TableCell>{item.tipoBeneficioDescricao ?? '—'}</TableCell>
                    <TableCell align="right">
                      {new Intl.NumberFormat('pt-BR', {
                        style: 'currency',
                        currency: 'BRL',
                      }).format(item.valor)}
                    </TableCell>
                    <TableCell>{item.observacao ?? '—'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDetalhesDialog(false)}>Fechar</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default BeneficiosMensais;
