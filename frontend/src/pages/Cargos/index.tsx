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
  IconButton,
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
import { cargoService } from '../../services/cargoService';
import type { Cargo } from '../../types';

interface CargoFormData {
  descricao: string;
}

export default function Cargos() {
  const [cargos, setCargos] = useState<Cargo[]>([]);
  const [open, setOpen] = useState(false);
  const [selectedCargo, setSelectedCargo] = useState<Cargo | null>(null);
  const { register, handleSubmit, reset, setValue } = useForm<CargoFormData>();

  useEffect(() => {
    carregarCargos();
  }, []);

  const carregarCargos = async () => {
    try {
      const data = await cargoService.listarTodos();
      setCargos(data);
    } catch (error) {
      toast.error('Erro ao carregar cargos');
    }
  };

  const handleOpen = (cargo?: Cargo) => {
    if (cargo) {
      setSelectedCargo(cargo);
      setValue('descricao', cargo.descricao);
    } else {
      setSelectedCargo(null);
      reset();
    }
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
    reset();
  };

  const onSubmit = async (data: CargoFormData) => {
    try {
      if (selectedCargo) {
        await cargoService.atualizar(selectedCargo.id, data);
        toast.success('Cargo atualizado com sucesso');
      } else {
        await cargoService.cadastrar(data);
        toast.success('Cargo cadastrado com sucesso');
      }
      handleClose();
      carregarCargos();
    } catch (error) {
      toast.error('Erro ao salvar cargo');
    }
  };

  const handleDelete = async (id: number) => {
    if (window.confirm('Tem certeza que deseja excluir este cargo?')) {
      try {
        await cargoService.remover(id);
        toast.success('Cargo excluído com sucesso');
        carregarCargos();
      } catch (error) {
        toast.error('Erro ao excluir cargo');
      }
    }
  };

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Cargos</Typography>
        <Button
          variant="contained"
          color="primary"
          startIcon={<AddIcon />}
          onClick={() => handleOpen()}
        >
          Novo Cargo
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
                  <TableCell>Status</TableCell>
                  <TableCell align="center">Ações</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {cargos.map((cargo) => (
                  <TableRow key={cargo.id}>
                    <TableCell>{cargo.id}</TableCell>
                    <TableCell>{cargo.descricao}</TableCell>
                    <TableCell>{cargo.ativo ? 'Ativo' : 'Inativo'}</TableCell>
                    <TableCell align="center">
                      <IconButton
                        color="primary"
                        onClick={() => handleOpen(cargo)}
                      >
                        <EditIcon />
                      </IconButton>
                      <IconButton
                        color="error"
                        onClick={() => handleDelete(cargo.id)}
                      >
                        <DeleteIcon />
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
        <DialogTitle>
          {selectedCargo ? 'Editar Cargo' : 'Novo Cargo'}
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
          </DialogContent>
          <DialogActions>
            <Button onClick={handleClose}>Cancelar</Button>
            <Button type="submit" variant="contained" color="primary">
              {selectedCargo ? 'Atualizar' : 'Cadastrar'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
} 