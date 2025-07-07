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
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import { Add as AddIcon, Edit as EditIcon, Delete as DeleteIcon } from '@mui/icons-material';
import { useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import { rubricaService } from '../../services/rubricaService';
import type { Rubrica } from '../../types';

interface RubricaFormData {
  codigo: string;
  descricao: string;
  tipo: 'PROVENTO' | 'DESCONTO' | 'INFORMATIVO';
  porcentagem?: number;
}

const tiposRubrica = [
  { value: 'PROVENTO', label: 'Provento' },
  { value: 'DESCONTO', label: 'Desconto' },
  { value: 'INFORMATIVO', label: 'Informativo' },
];

export default function Rubricas() {
  const [rubricas, setRubricas] = useState<Rubrica[]>([]);
  const [open, setOpen] = useState(false);
  const [selectedRubrica, setSelectedRubrica] = useState<Rubrica | null>(null);
  const { register, handleSubmit, reset, setValue, watch } = useForm<RubricaFormData>();
  const tipoSelecionado = watch('tipo');

  useEffect(() => {
    carregarRubricas();
  }, []);

  const carregarRubricas = async () => {
    try {
      const data = await rubricaService.listarTodos();
      setRubricas(data);
    } catch (error) {
      toast.error('Erro ao carregar rubricas');
    }
  };

  const handleOpen = (rubrica?: Rubrica) => {
    if (rubrica) {
      setSelectedRubrica(rubrica);
      setValue('codigo', rubrica.codigo);
      setValue('descricao', rubrica.descricao);
      setValue('tipo', rubrica.tipo);
      setValue('porcentagem', rubrica.porcentagem);
    } else {
      setSelectedRubrica(null);
      reset();
      // Define valor padrão de 100 para nova rubrica
      setValue('porcentagem', 100);
    }
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
    reset();
  };

  const onSubmit = async (data: RubricaFormData) => {
    try {
      if (selectedRubrica) {
        await rubricaService.atualizar(selectedRubrica.id, data);
        toast.success('Rubrica atualizada com sucesso');
      } else {
        await rubricaService.cadastrar(data);
        toast.success('Rubrica cadastrada com sucesso');
      }
      handleClose();
      carregarRubricas();
    } catch (error) {
      toast.error('Erro ao salvar rubrica');
    }
  };

  const handleDelete = async (id: number) => {
    if (window.confirm('Tem certeza que deseja excluir esta rubrica?')) {
      try {
        await rubricaService.remover(id);
        toast.success('Rubrica excluída com sucesso');
        carregarRubricas();
      } catch (error) {
        toast.error('Erro ao excluir rubrica');
      }
    }
  };

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Rubricas</Typography>
        <Button
          variant="contained"
          color="primary"
          startIcon={<AddIcon />}
          onClick={() => handleOpen()}
        >
          Nova Rubrica
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
                  <TableCell>Tipo</TableCell>
                  <TableCell>Porcentagem</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="center">Ações</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rubricas.map((rubrica) => (
                  <TableRow key={rubrica.id}>
                    <TableCell>{rubrica.codigo}</TableCell>
                    <TableCell>{rubrica.descricao}</TableCell>
                    <TableCell>
                      {rubrica.tipo ? tiposRubrica.find(t => t.value === rubrica.tipo)?.label || rubrica.tipo : '-'}
                    </TableCell>
                    <TableCell>
                      {rubrica.porcentagem ? `${rubrica.porcentagem}%` : '-'}
                    </TableCell>
                    <TableCell>{rubrica.ativo ? 'Ativo' : 'Inativo'}</TableCell>
                    <TableCell align="center">
                      <IconButton
                        color="primary"
                        onClick={() => handleOpen(rubrica)}
                      >
                        <EditIcon />
                      </IconButton>
                      <IconButton
                        color="error"
                        onClick={() => handleDelete(rubrica.id)}
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
          {selectedRubrica ? 'Editar Rubrica' : 'Nova Rubrica'}
        </DialogTitle>
        <form onSubmit={handleSubmit(onSubmit)}>
          <DialogContent>
            <TextField
              {...register('codigo', { required: 'Código é obrigatório' })}
              label="Código"
              fullWidth
              margin="normal"
              required
            />
            <TextField
              {...register('descricao', { required: 'Descrição é obrigatória' })}
              label="Descrição"
              fullWidth
              margin="normal"
              required
            />
            <FormControl fullWidth margin="normal" required>
              <InputLabel>Tipo</InputLabel>
              <Select
                {...register('tipo', { required: 'Tipo é obrigatório' })}
                label="Tipo"
              >
                {tiposRubrica.map((tipo) => (
                  <MenuItem key={tipo.value} value={tipo.value}>
                    {tipo.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            {(tipoSelecionado === 'PROVENTO' || tipoSelecionado === 'DESCONTO') && (
              <TextField
                {...register('porcentagem', {
                  min: { value: 0, message: 'Porcentagem deve ser maior ou igual a 0' },
                  max: { value: 100, message: 'Porcentagem deve ser menor ou igual a 100' }
                })}
                label="Porcentagem (%)"
                type="number"
                fullWidth
                margin="normal"
                inputProps={{ min: 0, max: 100, step: 0.01 }}
              />
            )}
          </DialogContent>
          <DialogActions>
            <Button onClick={handleClose}>Cancelar</Button>
            <Button type="submit" variant="contained" color="primary">
              {selectedRubrica ? 'Atualizar' : 'Cadastrar'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
} 