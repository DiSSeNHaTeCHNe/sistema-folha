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
  IconButton,
  TextField,
  InputAdornment,
  Grid,
  Snackbar,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  MenuItem,
  Card,
  CardContent,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Search as SearchIcon,
  Upload as UploadIcon,
  Visibility as VisibilityIcon,
} from '@mui/icons-material';
import type { FolhaPagamento } from '../../types';
import { folhaPagamentoService } from '../../services/folhaPagamentoService';
import { resumoFolhaPagamentoService, type ResumoFolhaPagamento } from '../../services/resumoFolhaPagamentoService';

const initialForm: Omit<FolhaPagamento, 'id'> = {
  funcionarioId: 0,
  funcionarioNome: '',
  rubricaId: 0,
  rubricaCodigo: '',
  rubricaDescricao: '',
  rubricaTipo: '',
  dataInicio: '',
  dataFim: '',
  valor: 0,
  quantidade: 1,
  baseCalculo: 0,
  ativo: true,
};

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
  const [searchTerm, setSearchTerm] = useState('');
  const [mes, setMes] = useState('');
  const [ano, setAno] = useState('');
  const [funcionarioId, setFuncionarioId] = useState('');
  const [centroCusto, setCentroCusto] = useState('');
  const [linhaNegocio, setLinhaNegocio] = useState('');
  const [folha, setFolha] = useState<FolhaPagamento[]>([]);
  const [funcionariosResumo, setFuncionariosResumo] = useState<FuncionarioResumo[]>([]);
  const [resumosFolha, setResumosFolha] = useState<ResumoFolhaPagamento[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [openSnackbar, setOpenSnackbar] = useState(false);
  const [snackbarMsg, setSnackbarMsg] = useState('');
  const [openDialog, setOpenDialog] = useState(false);
  const [openDetalhesDialog, setOpenDetalhesDialog] = useState(false);
  const [form, setForm] = useState<Omit<FolhaPagamento, 'id'>>(initialForm);
  const [editId, setEditId] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);
  const [funcionarioSelecionado, setFuncionarioSelecionado] = useState<FuncionarioResumo | null>(null);
  const [rubricasFuncionario, setRubricasFuncionario] = useState<FolhaPagamento[]>([]);
  const [resumoSelecionado, setResumoSelecionado] = useState<ResumoFolhaPagamento | null>(null);
  const [mostrarFuncionarios, setMostrarFuncionarios] = useState(false);

  const getPeriodo = () => {
    if (mes && ano) {
      const m = mes.padStart(2, '0');
      const dataInicio = `${ano}-${m}-01`;
      const dataFim = `${ano}-${m}-31`;
      return { dataInicio, dataFim };
    }
    return { dataInicio: '', dataFim: '' };
  };

  const fetchFolha = async () => {
    setLoading(true);
    try {
      let data: FolhaPagamento[] = [];
      const { dataInicio, dataFim } = getPeriodo();
      
      // Buscar dados da folha
      if (funcionarioId) {
        data = await folhaPagamentoService.buscarPorFuncionario(Number(funcionarioId), dataInicio, dataFim);
      } else if (centroCusto) {
        data = await folhaPagamentoService.buscarPorCentroCusto(centroCusto, dataInicio, dataFim);
      } else if (linhaNegocio) {
        data = await folhaPagamentoService.buscarPorLinhaNegocio(linhaNegocio, dataInicio, dataFim);
      } else if (mes && ano) {
        data = await folhaPagamentoService.buscarPorPeriodo(dataInicio, dataFim);
      } else {
        data = await folhaPagamentoService.listar();
      }
      setFolha(data);
      
      // Buscar todos os resumos da folha
      try {
        const resumos = await resumoFolhaPagamentoService.listarTodos();
        // Ordenar por competência em ordem decrescente (mais novo primeiro)
        const resumosOrdenados = resumos.sort((a, b) => {
          const dataA = new Date(a.competenciaInicio).getTime();
          const dataB = new Date(b.competenciaInicio).getTime();
          return dataB - dataA; // Ordem decrescente
        });
        setResumosFolha(resumosOrdenados);
      } catch (err) {
        console.log('Nenhum resumo encontrado');
      }
      
      // Criar resumo por funcionário
      const resumo = data.reduce((acc, item) => {
        const key = `${item.funcionarioId}-${item.dataInicio}-${item.dataFim}`;
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
      
      setFuncionariosResumo(Object.values(resumo).sort((a, b) => 
        a.funcionarioNome.localeCompare(b.funcionarioNome)
      ));
    } catch (err) {
      setError('Erro ao buscar registros');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFolha();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearch = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(event.target.value);
  };

  const handleFilter = async () => {
    await fetchFolha();
  };

  const handleOpenDialog = (registro?: FolhaPagamento) => {
    if (registro) {
      const { id, ...rest } = registro;
      setForm(rest);
      setEditId(id);
    } else {
      setForm(initialForm);
      setEditId(null);
    }
    setOpenDialog(true);
  };

  const handleCloseDialog = () => {
    setOpenDialog(false);
    setForm(initialForm);
    setEditId(null);
  };

  const handleChangeForm = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      if (editId) {
        await folhaPagamentoService.atualizar(editId, form);
        setSnackbarMsg('Registro atualizado com sucesso!');
      } else {
        await folhaPagamentoService.criar(form);
        setSnackbarMsg('Registro criado com sucesso!');
      }
      await fetchFolha();
      setOpenDialog(false);
    } catch (err) {
      setSnackbarMsg('Erro ao salvar registro');
    } finally {
      setSaving(false);
      setOpenSnackbar(true);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Tem certeza que deseja remover este registro?')) return;
    try {
      await folhaPagamentoService.remover(id);
      setSnackbarMsg('Registro removido com sucesso!');
      await fetchFolha();
    } catch (err) {
      setSnackbarMsg('Erro ao remover registro');
    } finally {
      setOpenSnackbar(true);
    }
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

  const handleVerFuncionarios = (resumo: ResumoFolhaPagamento) => {
    setResumoSelecionado(resumo);
    setMostrarFuncionarios(true);
  };

  const handleVoltarParaResumos = () => {
    setMostrarFuncionarios(false);
    setResumoSelecionado(null);
  };

  const filteredFuncionarios = funcionariosResumo.filter((item) => {
    const nomeMatch = item.funcionarioNome.toLowerCase().includes(searchTerm.toLowerCase());
    if (mostrarFuncionarios && resumoSelecionado) {
      // Filtra também por competência
      return (
        nomeMatch &&
        item.dataInicio === resumoSelecionado.competenciaInicio &&
        item.dataFim === resumoSelecionado.competenciaFim
      );
    }
    return nomeMatch;
  });

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h4">Folha de Pagamento</Typography>
      </Box>

      <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
        <TextField
          label="Funcionário (ID)"
          value={funcionarioId}
          onChange={e => setFuncionarioId(e.target.value)}
          type="number"
        />
        <TextField
          label="Centro de Custo"
          value={centroCusto}
          onChange={e => setCentroCusto(e.target.value)}
        />
        <TextField
          label="Linha de Negócio"
          value={linhaNegocio}
          onChange={e => setLinhaNegocio(e.target.value)}
        />
        <TextField
          label="Mês"
          type="number"
          value={mes}
          onChange={e => setMes(e.target.value)}
          inputProps={{ min: 1, max: 12 }}
        />
        <TextField
          label="Ano"
          type="number"
          value={ano}
          onChange={e => setAno(e.target.value)}
          inputProps={{ min: 2000, max: 2100 }}
        />
        <Button variant="outlined" onClick={handleFilter}>Filtrar</Button>
        <Button variant="text" onClick={() => {
          setFuncionarioId(''); setCentroCusto(''); setLinhaNegocio(''); setMes(''); setAno(''); fetchFolha();
        }}>Limpar</Button>
      </Box>

      <Box sx={{ mb: 2 }}>
        <TextField
          fullWidth
          label="Buscar por funcionário"
          value={searchTerm}
          onChange={handleSearch}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            ),
          }}
        />
      </Box>

      {loading ? (
        <Typography>Carregando...</Typography>
      ) : error ? (
        <Typography color="error">{error}</Typography>
      ) : mostrarFuncionarios ? (
        <>
          {/* Lista de Funcionários */}
          <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
            <Button
              variant="outlined"
              onClick={handleVoltarParaResumos}
              sx={{ mr: 2 }}
            >
              ← Voltar para Resumos
            </Button>
            <Typography variant="h5">
              Funcionários - Competência: {resumoSelecionado && (
                `${formatarDataCompetencia(resumoSelecionado.competenciaInicio)} a ${formatarDataCompetencia(resumoSelecionado.competenciaFim)}`
              )}
            </Typography>
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
          {/* Lista de Resumos da Folha */}
          <Typography variant="h5" gutterBottom sx={{ mb: 2 }}>
            Resumos da Folha de Pagamento
          </Typography>
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
                      {new Date(resumo.dataImportacao).toLocaleString()}
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

      <Snackbar
        open={openSnackbar}
        autoHideDuration={3000}
        onClose={() => setOpenSnackbar(false)}
        message={snackbarMsg}
      />

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

      {/* Dialog para edição/criação */}
      <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
        <DialogTitle>{editId ? 'Editar Registro' : 'Novo Registro'}</DialogTitle>
        <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
          <TextField
            label="Funcionário (ID)"
            name="funcionarioId"
            type="number"
            value={form.funcionarioId}
            onChange={handleChangeForm}
            required
          />
          <TextField
            label="Nome do Funcionário"
            name="funcionarioNome"
            value={form.funcionarioNome}
            onChange={handleChangeForm}
            required
          />
          <TextField
            label="Rubrica (ID)"
            name="rubricaId"
            type="number"
            value={form.rubricaId}
            onChange={handleChangeForm}
            required
          />
          <TextField
            label="Código da Rubrica"
            name="rubricaCodigo"
            value={form.rubricaCodigo}
            onChange={handleChangeForm}
            required
          />
          <TextField
            label="Descrição da Rubrica"
            name="rubricaDescricao"
            value={form.rubricaDescricao}
            onChange={handleChangeForm}
            required
          />
          <TextField
            label="Tipo da Rubrica"
            name="rubricaTipo"
            value={form.rubricaTipo}
            onChange={handleChangeForm}
            required
          />
          <TextField
            label="Data Início"
            name="dataInicio"
            type="date"
            value={form.dataInicio}
            onChange={handleChangeForm}
            InputLabelProps={{ shrink: true }}
            required
          />
          <TextField
            label="Data Fim"
            name="dataFim"
            type="date"
            value={form.dataFim}
            onChange={handleChangeForm}
            InputLabelProps={{ shrink: true }}
            required
          />
          <TextField
            label="Valor"
            name="valor"
            type="number"
            value={form.valor}
            onChange={handleChangeForm}
            required
          />
          <TextField
            label="Quantidade"
            name="quantidade"
            type="number"
            value={form.quantidade}
            onChange={handleChangeForm}
            required
          />
          <TextField
            label="Base de Cálculo"
            name="baseCalculo"
            type="number"
            value={form.baseCalculo}
            onChange={handleChangeForm}
            required
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog} color="secondary">Cancelar</Button>
          <Button onClick={handleSave} variant="contained" color="primary" disabled={saving}>
            {editId ? 'Salvar' : 'Cadastrar'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
} 