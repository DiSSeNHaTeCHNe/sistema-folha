import React, { useEffect, useState } from 'react';
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
import { linhaNegocioService } from '../../services/linhaNegocioService';
import type { LinhaNegocio } from '../../types';

interface LinhaNegocioFormData {
  descricao: string;
}

export default function LinhasNegocio() {
  const [linhasNegocio, setLinhasNegocio] = useState<LinhaNegocio[]>([]);
  const [open, setOpen] = useState(false);
  const [selectedLinhaNegocio, setSelectedLinhaNegocio] = useState<LinhaNegocio | null>(null);
  const { register, handleSubmit, reset, setValue } = useForm<LinhaNegocioFormData>();

  useEffect(() => {
    carregarLinhasNegocio();
  }, []);

  const carregarLinhasNegocio = async () => {
    try {
      const data = await linhaNegocioService.listarTodos();
      setLinhasNegocio(data);
    } catch (error) {
      toast.error('Erro ao carregar linhas de negócio');
    }
  };

  const handleOpen = (linhaNegocio?: LinhaNegocio) => {
    if (linhaNegocio) {
      setSelectedLinhaNegocio(linhaNegocio);
      setValue('descricao', linhaNegocio.descricao);
    } else {
      setSelectedLinhaNegocio(null);
      reset();
    }
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
    reset();
  };

  const onSubmit = async (data: LinhaNegocioFormData) => {
    try {
      if (selectedLinhaNegocio) {
        await linhaNegocioService.atualizar(selectedLinhaNegocio.id, data);
        toast.success('Linha de Negócio atualizada com sucesso');
      } else {
        await linhaNegocioService.cadastrar(data);
        toast.success('Linha de Negócio cadastrada com sucesso');
      }
      handleClose();
      carregarLinhasNegocio();
    } catch (error) {
      toast.error('Erro ao salvar linha de negócio');
    }
  };

  const handleDelete = async (id: number) => {
    if (window.confirm('Tem certeza que deseja excluir esta linha de negócio?')) {
      try {
        await linhaNegocioService.remover(id);
        toast.success('Linha de Negócio excluída com sucesso');
        carregarLinhasNegocio();
      } catch (error) {
        toast.error('Erro ao excluir linha de negócio');
      }
    }
  };

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Linhas de Negócio</Typography>
        <Button
          variant="contained"
          color="primary"
          startIcon={<AddIcon />}
          onClick={() => handleOpen()}
        >
          Nova Linha de Negócio
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
                {linhasNegocio.map((linhaNegocio) => (
                  <TableRow key={linhaNegocio.id}>
                    <TableCell>{linhaNegocio.id}</TableCell>
                    <TableCell>{linhaNegocio.descricao}</TableCell>
                    <TableCell>{linhaNegocio.ativo ? 'Ativo' : 'Inativo'}</TableCell>
                    <TableCell align="center">
                      <IconButton
                        color="primary"
                        onClick={() => handleOpen(linhaNegocio)}
                      >
                        <EditIcon />
                      </IconButton>
                      <IconButton
                        color="error"
                        onClick={() => handleDelete(linhaNegocio.id)}
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
          {selectedLinhaNegocio ? 'Editar Linha de Negócio' : 'Nova Linha de Negócio'}
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
              {selectedLinhaNegocio ? 'Atualizar' : 'Cadastrar'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
} 