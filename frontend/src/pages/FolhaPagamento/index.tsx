import { useState, useEffect } from 'react';
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
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import {
  Search as SearchIcon,
  Visibility as VisibilityIcon,
} from '@mui/icons-material';
import { Controller, useForm } from 'react-hook-form';
import type { FolhaPagamento } from '../../types';
import { folhaPagamentoService } from '../../services/folhaPagamentoService';
import { resumoFolhaPagamentoService, type ResumoFolhaPagamento } from '../../services/resumoFolhaPagamentoService';
import api from '../../services/api';

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
  valorTotal: number;
  cargoDescricao?: string;
  centroCustoDescricao?: string;
  linhaNegocioDescricao?: string;
}

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
  const [folha, setFolha] = useState<FolhaPagamento[]>([]);
  const [funcionariosResumo, setFuncionariosResumo] = useState<FuncionarioResumo[]>([]);
  const [resumosFolha, setResumosFolha] = useState<ResumoFolhaPagamento[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [openDetalhesDialog, setOpenDetalhesDialog] = useState(false);
  const [funcionarioSelecionado, setFuncionarioSelecionado] = useState<FuncionarioResumo | null>(null);
  const [rubricasFuncionario, setRubricasFuncionario] = useState<FolhaPagamento[]>([]);
  const [resumoSelecionado, setResumoSelecionado] = useState<ResumoFolhaPagamento | null>(null);
  const [mostrarFuncionarios, setMostrarFuncionarios] = useState(false);

  // Formulário para filtros da tela de Resumo
  const { control: controlResumo, handleSubmit: handleSubmitResumo, reset: resetResumo } = useForm<FiltrosResumo>({
    defaultValues: {
      mes: '',
      ano: ''
    }
  });

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
      const [centrosCustoRes, linhasNegocioRes] = await Promise.all([
        api.get('/centros-custo'),
        api.get('/linhas-negocio')
      ]);
      
      // Ordenar em ordem alfabética crescente
      const centrosCustoOrdenados = [...centrosCustoRes.data].sort((a, b) => 
        a.descricao.localeCompare(b.descricao)
      );
      const linhasNegocioOrdenadas = [...linhasNegocioRes.data].sort((a, b) => 
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
      const resumos = await resumoFolhaPagamentoService.listarTodos();
      
      let resumosFiltrados = resumos;
      
      // Aplicar filtros de mês e ano se fornecidos
      if (filtros?.mes || filtros?.ano) {
        resumosFiltrados = resumos.filter(resumo => {
          // Extrai mês e ano diretamente da string no formato YYYY-MM-DD
          const [anoResumo, mesResumo] = resumo.competenciaInicio.split('-');
          
          // Remove zeros à esquerda para comparação
          const mesResumoNum = parseInt(mesResumo, 10).toString();
          
          const mesMatch = !filtros.mes || mesResumoNum === filtros.mes;
          const anoMatch = !filtros.ano || anoResumo === filtros.ano;
          
          return mesMatch && anoMatch;
        });
      }
      
      // Ordenar da mais nova para a mais antiga
      const resumosOrdenados = resumosFiltrados.sort((a, b) => {
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
      const data = await folhaPagamentoService.buscarPorPeriodo(
        resumo.competenciaInicio,
        resumo.competenciaFim
      );
      
      setFolha(data);
      
      // Criar resumo por funcionário
      const resumoMap = data.reduce((acc: Record<string, FuncionarioResumo>, item: FolhaPagamento) => {
        const key = `${item.funcionarioId}`;
        if (!acc[key]) {
          acc[key] = {
            funcionarioId: item.funcionarioId,
            funcionarioNome: item.funcionarioNome,
            dataInicio: item.dataInicio,
            dataFim: item.dataFim,
            totalRubricas: 0,
            valorTotal: 0,
            cargoDescricao: item.cargoDescricao,
            centroCustoDescricao: item.centroCustoDescricao,
            linhaNegocioDescricao: item.linhaNegocioDescricao,
          };
        }
        acc[key].totalRubricas += 1;
        
        // Calcula o total baseado no tipo da rubrica
        switch (item.rubricaTipo) {
          case 'PROVENTO':
            acc[key].valorTotal += item.valor;
            break;
          case 'DESCONTO':
            acc[key].valorTotal -= item.valor;
            break;
          case 'INFORMATIVO':
            // Ignora rubricas informativas no cálculo
            break;
          default:
            // Fallback: soma o valor
            acc[key].valorTotal += item.valor;
        }
        
        return acc;
      }, {} as Record<string, FuncionarioResumo>);
      
      const funcionariosArray = Object.values(resumoMap) as FuncionarioResumo[];
      const funcionariosOrdenados = funcionariosArray.sort((a, b) => 
        a.funcionarioNome.localeCompare(b.funcionarioNome)
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
    fetchResumosFolha();
  }, []);

  const handleFiltrarResumos = async (filtros: FiltrosResumo) => {
    await fetchResumosFolha(filtros);
  };

  const handleLimparFiltrosResumo = () => {
    resetResumo();
    fetchResumosFolha();
  };

  const handleDetalharRubricas = async (funcionario: FuncionarioResumo) => {
    setFuncionarioSelecionado(funcionario);
    const rubricas = folha.filter(item => 
      item.funcionarioId === funcionario.funcionarioId &&
      item.dataInicio === funcionario.dataInicio &&
      item.dataFim === funcionario.dataFim
    );
    setRubricasFuncionario(rubricas);
    setOpenDetalhesDialog(true);
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
    setFolha([]);
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
                    Total: {new Intl.NumberFormat('pt-BR', {
                      style: 'currency',
                      currency: 'BRL',
                    }).format(funcionario.valorTotal)}
                  </Typography>
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
              render={({ field }) => (
                <TextField
                  {...field}
                  label="Ano"
                  type="number"
                  sx={{ minWidth: 120 }}
                  inputProps={{ min: 2000, max: 2100 }}
                />
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
                  <TableCell align="right">Total Empregados</TableCell>
                  <TableCell align="right">Total Encargos</TableCell>
                  <TableCell align="right">Total Pagamentos</TableCell>
                  <TableCell align="right">Total Descontos</TableCell>
                  <TableCell align="right">Total Líquido</TableCell>
                  <TableCell>Data Importação</TableCell>
                  <TableCell align="center">Ações</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {resumosFolha.map((resumo) => (
                  <TableRow key={resumo.id || Math.random()}>
                    <TableCell>
                      {formatarDataCompetencia(resumo.competenciaInicio)} a {formatarDataCompetencia(resumo.competenciaFim)}
                    </TableCell>
                    <TableCell align="right">{resumo.totalEmpregados}</TableCell>
                    <TableCell align="right">
                      {new Intl.NumberFormat('pt-BR', {
                        style: 'currency',
                        currency: 'BRL',
                      }).format(resumo.totalEncargos)}
                    </TableCell>
                    <TableCell align="right">
                      {new Intl.NumberFormat('pt-BR', {
                        style: 'currency',
                        currency: 'BRL',
                      }).format(resumo.totalPagamentos)}
                    </TableCell>
                    <TableCell align="right">
                      {new Intl.NumberFormat('pt-BR', {
                        style: 'currency',
                        currency: 'BRL',
                      }).format(resumo.totalDescontos)}
                    </TableCell>
                    <TableCell align="right">
                      <Typography color="primary" variant="h6">
                        {new Intl.NumberFormat('pt-BR', {
                          style: 'currency',
                          currency: 'BRL',
                        }).format(resumo.totalLiquido)}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      {new Date(resumo.dataImportacao).toLocaleString('pt-BR')}
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

      {/* Dialog para detalhes das rubricas */}
      <Dialog open={openDetalhesDialog} onClose={() => setOpenDetalhesDialog(false)} maxWidth="lg" fullWidth>
        <DialogTitle>
          Rubricas de {funcionarioSelecionado?.funcionarioNome} - 
          Período: {formatarDataCompetencia(funcionarioSelecionado?.dataInicio || '')} a {formatarDataCompetencia(funcionarioSelecionado?.dataFim || '')}
        </DialogTitle>
        <DialogContent>
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Rubrica</TableCell>
                  <TableCell>Tipo</TableCell>
                  <TableCell>Valor</TableCell>
                  <TableCell>Quantidade</TableCell>
                  <TableCell>Base de Cálculo</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rubricasFuncionario.map((item) => (
                  <TableRow key={item.id}>
                    <TableCell>
                      {item.rubricaCodigo} - {item.rubricaDescricao}
                    </TableCell>
                    <TableCell>
                      {item.rubricaTipo}
                    </TableCell>
                    <TableCell>
                      {new Intl.NumberFormat('pt-BR', {
                        style: 'currency',
                        currency: 'BRL',
                      }).format(item.valor)}
                    </TableCell>
                    <TableCell>{item.quantidade}</TableCell>
                    <TableCell>
                      {new Intl.NumberFormat('pt-BR', {
                        style: 'currency',
                        currency: 'BRL',
                      }).format(item.baseCalculo)}
                    </TableCell>
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