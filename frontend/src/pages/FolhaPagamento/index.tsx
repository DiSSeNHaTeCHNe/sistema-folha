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
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Search as SearchIcon,
  Upload as UploadIcon,
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

export function FolhaPagamento() {
  const [searchTerm, setSearchTerm] = useState('');
  const [mes, setMes] = useState('');
  const [ano, setAno] = useState('');
  const [funcionarioId, setFuncionarioId] = useState('');
  const [centroCusto, setCentroCusto] = useState('');
  const [linhaNegocio, setLinhaNegocio] = useState('');
  const [folha, setFolha] = useState<FolhaPagamento[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [openSnackbar, setOpenSnackbar] = useState(false);
  const [snackbarMsg, setSnackbarMsg] = useState('');
  const [openDialog, setOpenDialog] = useState(false);
  const [form, setForm] = useState<Omit<FolhaPagamento, 'id'>>(initialForm);
  const [editId, setEditId] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);

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

  const filteredFolha = folha.filter((item) =>
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

      {loading ? (
        <Typography>Carregando...</Typography>
      ) : error ? (
        <Typography color="error">{error}</Typography>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Funcionário</TableCell>
                <TableCell>Rubrica</TableCell>
                <TableCell>Período</TableCell>
                <TableCell>Valor</TableCell>
                <TableCell>Quantidade</TableCell>
                <TableCell>Base de Cálculo</TableCell>
                <TableCell>Ações</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredFolha.map((item) => (
                <TableRow key={item.id}>
                  <TableCell>{item.funcionarioNome}</TableCell>
                  <TableCell>
                    {item.rubricaCodigo} - {item.rubricaDescricao}
                  </TableCell>
                  <TableCell>
                    {item.dataInicio} a {item.dataFim}
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
      )}
      <Snackbar
        open={openSnackbar}
        autoHideDuration={3000}
        onClose={() => setOpenSnackbar(false)}
        message={snackbarMsg}
      />
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