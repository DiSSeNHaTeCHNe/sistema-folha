import { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
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
import { Add as AddIcon, Edit as EditIcon, Block as BlockIcon } from '@mui/icons-material';
import { useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { tipoBeneficioService } from '../../services/tipoBeneficioService';
import type { TipoBeneficio } from '../../types';

const getApiErrorMessage = (error: unknown, fallback: string): string => {
  if (typeof error !== 'object' || error === null || !('response' in error)) {
    return fallback;
  }

  const response = (error as { response?: { data?: unknown } }).response;
  if (response?.data && typeof response.data === 'object') {
    const data = response.data as { message?: string };
    if (data.message) {
      return data.message;
    }
  }
  return fallback;
};

interface TipoBeneficioFormData {
  codigo: string;
  descricao: string;
}

export default function TiposBeneficio() {
  const [tipos, setTipos] = useState<TipoBeneficio[]>([]);
  const [open, setOpen] = useState(false);
  const [selectedTipo, setSelectedTipo] = useState<TipoBeneficio | null>(null);
  const { register, handleSubmit, reset, setValue } = useForm<TipoBeneficioFormData>();

  useEffect(() => {
    carregarTipos();
  }, []);

  const carregarTipos = async () => {
    try {
      const data = await tipoBeneficioService.listar();
      setTipos(data);
    } catch {
      toast.error('Erro ao carregar tipos de benefício');
    }
  };

  const handleOpen = (tipo?: TipoBeneficio) => {
    if (tipo) {
      setSelectedTipo(tipo);
      setValue('codigo', tipo.codigo);
      setValue('descricao', tipo.descricao);
    } else {
      setSelectedTipo(null);
      reset();
    }
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
    reset();
    setSelectedTipo(null);
  };

  const onSubmit = async (data: TipoBeneficioFormData) => {
    try {
      if (selectedTipo) {
        await tipoBeneficioService.atualizar(selectedTipo.id, {
          codigo: selectedTipo.codigo,
          descricao: data.descricao,
        });
        toast.success('Tipo de benefício atualizado com sucesso');
      } else {
        await tipoBeneficioService.criar({
          codigo: data.codigo,
          descricao: data.descricao,
          ativo: true,
        });
        toast.success('Tipo de benefício cadastrado com sucesso');
      }
      handleClose();
      carregarTipos();
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Erro ao salvar tipo de benefício'));
    }
  };

  const handleDesativar = async (id: number) => {
    if (window.confirm('Tem certeza que deseja desativar este tipo de benefício?')) {
      try {
        await tipoBeneficioService.remover(id);
        toast.success('Tipo de benefício desativado com sucesso');
        carregarTipos();
      } catch (error) {
        toast.error(getApiErrorMessage(error, 'Erro ao desativar tipo de benefício'));
      }
    }
  };

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Tipos de Benefício</Typography>
        <Button
          variant="contained"
          color="primary"
          startIcon={<AddIcon />}
          onClick={() => handleOpen()}
        >
          Novo Tipo
        </Button>
      </Box>

      <Card>
        <CardContent>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Código</TableCell>
                  <TableCell>Descrição</TableCell>
                  <TableCell>Ativo</TableCell>
                  <TableCell align="center">Ações</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {tipos.map((tipo) => (
                  <TableRow key={tipo.id}>
                    <TableCell>{tipo.codigo}</TableCell>
                    <TableCell>{tipo.descricao}</TableCell>
                    <TableCell>
                      <Chip
                        label={tipo.ativo ? 'Ativo' : 'Inativo'}
                        size="small"
                        color={tipo.ativo ? 'success' : 'default'}
                        variant={tipo.ativo ? 'filled' : 'outlined'}
                      />
                    </TableCell>
                    <TableCell align="center">
                      <IconButton
                        color="primary"
                        title="Editar"
                        onClick={() => handleOpen(tipo)}
                      >
                        <EditIcon />
                      </IconButton>
                      {tipo.ativo && (
                        <IconButton
                          color="error"
                          title="Desativar"
                          onClick={() => handleDesativar(tipo.id)}
                        >
                          <BlockIcon />
                        </IconButton>
                      )}
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
          {selectedTipo ? 'Editar Tipo de Benefício' : 'Novo Tipo de Benefício'}
        </DialogTitle>
        <form onSubmit={handleSubmit(onSubmit)}>
          <DialogContent>
            <TextField
              {...register('codigo', { required: 'Código é obrigatório' })}
              label="Código"
              fullWidth
              margin="normal"
              required
              disabled={!!selectedTipo}
            />
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
              {selectedTipo ? 'Atualizar' : 'Cadastrar'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
}
