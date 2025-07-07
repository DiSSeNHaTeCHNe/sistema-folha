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

const initialForm: Omit<FolhaPagamento, 'id'> = {
  funcionarioId: 0,
  funcionarioNome: '',
  rubricaId: 0,
  rubricaCodigo: '',
  rubricaDescricao: '',
  dataInicio: '',
  dataFim: '',
  valor: 0,
  quantidade: 1,
  baseCalculo: 0,
};

interface FuncionarioResumo {
  funcionarioId: number;
  funcionarioNome: string;
  dataInicio: string;
  dataFim: string;
  totalRubricas: number;
  valorTotal: number;
}

export function FolhaPagamento() {
  const [searchTerm, setSearchTerm] = useState('');
  const [mes, setMes] = useState('');
  const [ano, setAno] = useState('');
  const [funcionarioId, setFuncionarioId] = useState('');
  const [centroCusto, setCentroCusto] = useState('');
  const [linhaNegocio, setLinhaNegocio] = useState('');
  const [folha, setFolha] = useState<FolhaPagamento[]>([]);
  const [funcionariosResumo, setFuncionariosResumo] = useState<FuncionarioResumo[]>([]);
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
          };
        }
        acc[key].totalRubricas += 1;
        acc[key].valorTotal += item.valor;
        return acc;
      }, {} as Record<string, FuncionarioResumo>);
      
      setFuncionariosResumo(Object.values(resumo));
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

  const filteredFuncionarios = funcionariosResumo.filter((item) =>
    item.funcionarioNome.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 3 }}>
        <Typography component="h1" variant="h4">
          Folha de Pagamento
        </Typography>
        <Box>
          <Button
            variant="outlined"
            startIcon={<UploadIcon />}
            onClick={() => {
              // Implementar importação
            }}
            sx={{ mr: 2 }}
          >
            Importar
          </Button>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => handleOpenDialog()}
          >
            Novo Registro
          </Button>
        </Box>
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
      ) : (
        <Grid container spacing={2}>
          {filteredFuncionarios.map((funcionario) => (
            <Grid item xs={12} sm={6} md={4} key={`${funcionario.funcionarioId}-${funcionario.dataInicio}`}>
              <Card>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    {funcionario.funcionarioNome}
                  </Typography>
                  <Typography color="textSecondary" gutterBottom>
                    Período: {new Date(funcionario.dataInicio).toLocaleDateString()} a {new Date(funcionario.dataFim).toLocaleDateString()}
                  </Typography>
                  <Typography color="textSecondary" gutterBottom>
                    Rubricas: {funcionario.totalRubricas}
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
            </Grid>
          ))}
        </Grid>
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
          Período: {funcionarioSelecionado?.dataInicio} a {funcionarioSelecionado?.dataFim}
        </DialogTitle>
        <DialogContent>
          <TableContainer component={Paper}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Rubrica</TableCell>
                  <TableCell>Valor</TableCell>
                  <TableCell>Quantidade</TableCell>
                  <TableCell>Base de Cálculo</TableCell>
                  <TableCell>Ações</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rubricasFuncionario.map((item) => (
                  <TableRow key={item.id}>
                    <TableCell>
                      {item.rubricaCodigo} - {item.rubricaDescricao}
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
                    <TableCell>
                      <IconButton
                        color="primary"
                        onClick={() => handleOpenDialog(item)}
                      >
                        <EditIcon />
                      </IconButton>
                      <IconButton
                        color="error"
                        onClick={() => handleDelete(item.id)}
                      >
                        <DeleteIcon />
                      </IconButton>
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