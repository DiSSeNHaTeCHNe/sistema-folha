import { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { Add as AddIcon, Edit as EditIcon, Delete as DeleteIcon } from '@mui/icons-material';
import { useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { centroCustoService } from '../../services/centroCustoService';
import { linhaNegocioService } from '../../services/linhaNegocioService';
import type { CentroCusto, LinhaNegocio } from '../../types';

interface CentroCustoFormData {
  descricao: string;
  linhaNegocioId: number;
}

export default function CentrosCusto() {
  const [centrosCusto, setCentrosCusto] = useState<CentroCusto[]>([]);
  const [linhasNegocio, setLinhasNegocio] = useState<LinhaNegocio[]>([]);
  const [open, setOpen] = useState(false);
  const [selectedCentroCusto, setSelectedCentroCusto] = useState<CentroCusto | null>(null);
  const { register, handleSubmit, reset, setValue } = useForm<CentroCustoFormData>();

  useEffect(() => {
    carregarDados();
  }, []);

  const carregarDados = async () => {
    try {
      const [centrosCustoData, linhasNegocioData] = await Promise.all([
        centroCustoService.listarTodos(),
        linhaNegocioService.listarTodos(),
      ]);
      setCentrosCusto(centrosCustoData);
      setLinhasNegocio(linhasNegocioData);
    } catch (error) {
      toast.error('Erro ao carregar dados');
    }
  };

  const handleOpen = (centroCusto?: CentroCusto) => {
    if (centroCusto) {
      setSelectedCentroCusto(centroCusto);
      setValue('descricao', centroCusto.descricao);
      setValue('linhaNegocioId', centroCusto.linhaNegocioId);
    } else {
      setSelectedCentroCusto(null);
      reset();
    }
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
    reset();
  };

  const onSubmit = async (data: CentroCustoFormData) => {
    try {
      if (selectedCentroCusto) {
        await centroCustoService.atualizar(selectedCentroCusto.id, data);
        toast.success('Centro de Custo atualizado com sucesso');
      } else {
        await centroCustoService.cadastrar(data);
        toast.success('Centro de Custo cadastrado com sucesso');
      }
      handleClose();
      carregarDados();
    } catch (error) {
      toast.error('Erro ao salvar centro de custo');
    }
  };

  const handleDelete = async (id: number) => {
    if (window.confirm('Tem certeza que deseja excluir este centro de custo?')) {
      try {
        await centroCustoService.remover(id);
        toast.success('Centro de Custo excluído com sucesso');
        carregarDados();
      } catch (error) {
        toast.error('Erro ao excluir centro de custo');
      }
    }
  };

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Centros de Custo</Typography>
        <Button
          variant="contained"
          color="primary"
          startIcon={<AddIcon />}
          onClick={() => handleOpen()}
        >
          Novo Centro de Custo
        </Button>
      </Box>

      <Card>
        <CardContent>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell>
                  <TableCell>Descrição</TableCell>
                  <TableCell>Linha de Negócio</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="center">Ações</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {centrosCusto.map((centroCusto) => {
                  const linhaNegocio = linhasNegocio.find(ln => ln.id === centroCusto.linhaNegocioId);
                  return (
                    <TableRow key={centroCusto.id}>
                      <TableCell>{centroCusto.id}</TableCell>
                      <TableCell>{centroCusto.descricao}</TableCell>
                      <TableCell>{linhaNegocio?.descricao || 'N/A'}</TableCell>
                      <TableCell>{centroCusto.ativo ? 'Ativo' : 'Inativo'}</TableCell>
                      <TableCell align="center">
                        <IconButton
                          color="primary"
                          onClick={() => handleOpen(centroCusto)}
                        >
                          <EditIcon />
                        </IconButton>
                        <IconButton
                          color="error"
                          onClick={() => handleDelete(centroCusto.id)}
                        >
                          <DeleteIcon />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
        <DialogTitle>
          {selectedCentroCusto ? 'Editar Centro de Custo' : 'Novo Centro de Custo'}
        </DialogTitle>
        <form onSubmit={handleSubmit(onSubmit)}>
          <DialogContent>
            <TextField
              {...register('descricao', { required: 'Descrição é obrigatória' })}
              label="Descrição"
              fullWidth
              margin="normal"
              required
            />
            <FormControl fullWidth margin="normal" required>
              <InputLabel>Linha de Negócio</InputLabel>
              <Select
                {...register('linhaNegocioId', { required: 'Linha de Negócio é obrigatória' })}
                label="Linha de Negócio"
              >
                {linhasNegocio.map((linhaNegocio) => (
                  <MenuItem key={linhaNegocio.id} value={linhaNegocio.id}>
                    {linhaNegocio.descricao}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </DialogContent>
          <DialogActions>
            <Button onClick={handleClose}>Cancelar</Button>
            <Button type="submit" variant="contained" color="primary">
              {selectedCentroCusto ? 'Atualizar' : 'Cadastrar'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
} 