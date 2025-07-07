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
  FormControl,
  Grid,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  TextField,
  Typography,
} from '@mui/material';
import { Add as AddIcon, Edit as EditIcon, Delete as DeleteIcon } from '@mui/icons-material';
import { useForm } from 'react-hook-form';
import api from "../../services/api";
import { toast } from 'react-toastify';

interface FuncionarioLocal {
  id: number;
  nome: string;
  cpf: string;
  dataAdmissao: string;
  cargoId: number;
  cargoDescricao: string;
  centroCustoId: number;
  centroCustoDescricao: string;
  linhaNegocioId: number;
  linhaNegocioDescricao: string;
  idExterno?: string;
  ativo: boolean;
}

interface Cargo {
  id: number;
  descricao: string;
  linhaNegocio: {
    id: number;
    descricao: string;
  };
}

interface CentroCusto {
  id: number;
  descricao: string;
  linhaNegocio: {
    id: number;
    descricao: string;
  };
}

interface LinhaNegocio {
  id: number;
  descricao: string;
}

interface Filtros {
  nome: string;
  cpf: string;
  dataAdmissao: string;
  cargoId: number | '';
  centroCustoId: number | '';
  linhaNegocioId: number | '';
}

// Função utilitária para formatar datas do backend (formato ISO)
const formatarDataCompetencia = (dataString: string): string => {
  if (!dataString) return '';
  if (dataString.includes('-')) {
    const [ano, mes, dia] = dataString.split('-');
    return `${dia}/${mes}/${ano}`;
  }
  return dataString;
};

export default function Funcionarios() {
  const [funcionarios, setFuncionarios] = useState<FuncionarioLocal[]>([]);
  const [cargos, setCargos] = useState<Cargo[]>([]);
  const [centrosCusto, setCentrosCusto] = useState<CentroCusto[]>([]);
  const [linhasNegocio, setLinhasNegocio] = useState<LinhaNegocio[]>([]);
  const [open, setOpen] = useState(false);
  const [selectedFuncionario, setSelectedFuncionario] = useState<FuncionarioLocal | null>(null);
  const { register, handleSubmit, reset, setValue, watch } = useForm<Filtros>();
  const linhaNegocioId = watch('linhaNegocioId');

  useEffect(() => {
    carregarDados();
  }, []);

  useEffect(() => {
    if (linhaNegocioId) {
      carregarCargosPorLinhaNegocio(linhaNegocioId);
      carregarCentrosCustoPorLinhaNegocio(linhaNegocioId);
    } else {
      setCargos([]);
      setCentrosCusto([]);
    }
  }, [linhaNegocioId]);

  const carregarDados = async () => {
    try {
      const [funcionariosRes, linhasNegocioRes] = await Promise.all([
        api.get('/funcionarios'),
        api.get('/linhas-negocio'),
      ]);
      setFuncionarios(funcionariosRes.data);
      setLinhasNegocio(linhasNegocioRes.data);
    } catch (error) {
      toast.error('Erro ao carregar dados');
    }
  };

  const carregarCargosPorLinhaNegocio = async (linhaNegocioId: number) => {
    try {
      const response = await api.get(`/cargos/linha-negocio/${linhaNegocioId}`);
      setCargos(response.data);
    } catch (error) {
      toast.error('Erro ao carregar cargos');
    }
  };

  const carregarCentrosCustoPorLinhaNegocio = async (linhaNegocioId: number) => {
    try {
      const response = await api.get(`/centros-custo/linha-negocio/${linhaNegocioId}`);
      setCentrosCusto(response.data);
    } catch (error) {
      toast.error('Erro ao carregar centros de custo');
    }
  };

  const handleOpen = (funcionario?: FuncionarioLocal) => {
    if (funcionario) {
      setSelectedFuncionario(funcionario);
      setValue('nome', funcionario.nome);
      setValue('cpf', funcionario.cpf);
      setValue('dataAdmissao', funcionario.dataAdmissao);
      setValue('cargoId', funcionario.cargoId);
      setValue('centroCustoId', funcionario.centroCustoId);
    } else {
      setSelectedFuncionario(null);
      reset();
    }
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
    reset();
  };

  const onSubmit = async (data: any) => {
    try {
      if (selectedFuncionario) {
        await api.put(`/funcionarios/${selectedFuncionario.id}`, data);
        toast.success('Funcionário atualizado com sucesso');
      } else {
        await api.post('/funcionarios', data);
        toast.success('Funcionário cadastrado com sucesso');
      }
      handleClose();
      carregarDados();
    } catch (error) {
      toast.error('Erro ao salvar funcionário');
    }
  };

  const handleDelete = async (id: number) => {
    if (window.confirm('Tem certeza que deseja excluir este funcionário?')) {
      try {
        await api.delete(`/funcionarios/${id}`);
        toast.success('Funcionário excluído com sucesso');
        carregarDados();
      } catch (error) {
        toast.error('Erro ao excluir funcionário');
      }
    }
  };

  const handleFiltros = async (filtros: Filtros) => {
    try {
      const params = new URLSearchParams();
      if (filtros.nome) params.append('nome', filtros.nome);
      if (filtros.cargoId) params.append('cargoId', filtros.cargoId.toString());
      if (filtros.centroCustoId) params.append('centroCustoId', filtros.centroCustoId.toString());
      if (filtros.linhaNegocioId) params.append('linhaNegocioId', filtros.linhaNegocioId.toString());

      const response = await api.get(`/funcionarios?${params.toString()}`);
      setFuncionarios(response.data);
    } catch (error) {
      toast.error('Erro ao filtrar funcionários');
    }
  };

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Funcionários</Typography>
        <Button
          variant="contained"
          color="primary"
          startIcon={<AddIcon />}
          onClick={() => handleOpen()}
        >
          Novo Funcionário
        </Button>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <form onSubmit={handleSubmit(handleFiltros)}>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6} md={3}>
                <TextField
                  fullWidth
                  label="Nome"
                  {...register('nome')}
                />
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <FormControl fullWidth>
                  <InputLabel>Linha de Negócio</InputLabel>
                  <Select
                    label="Linha de Negócio"
                    {...register('linhaNegocioId')}
                  >
                    <MenuItem value="">Todas</MenuItem>
                    {linhasNegocio.map((linha) => (
                      <MenuItem key={linha.id} value={linha.id}>
                        {linha.descricao}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <FormControl fullWidth>
                  <InputLabel>Cargo</InputLabel>
                  <Select
                    label="Cargo"
                    {...register('cargoId')}
                    disabled={!linhaNegocioId}
                  >
                    <MenuItem value="">Todos</MenuItem>
                    {cargos.map((cargo) => (
                      <MenuItem key={cargo.id} value={cargo.id}>
                        {cargo.descricao}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <FormControl fullWidth>
                  <InputLabel>Centro de Custo</InputLabel>
                  <Select
                    label="Centro de Custo"
                    {...register('centroCustoId')}
                    disabled={!linhaNegocioId}
                  >
                    <MenuItem value="">Todos</MenuItem>
                    {centrosCusto.map((centro) => (
                      <MenuItem key={centro.id} value={centro.id}>
                        {centro.descricao}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12}>
                <Button type="submit" variant="contained" color="primary">
                  Filtrar
                </Button>
              </Grid>
            </Grid>
          </form>
        </CardContent>
      </Card>

      <Grid container spacing={2}>
        {funcionarios.map((funcionario) => (
          <Grid item xs={12} sm={6} md={4} key={funcionario.id}>
            <Card>
              <CardContent>
                <Typography variant="h6">{funcionario.nome}</Typography>
                <Typography color="textSecondary">CPF: {funcionario.cpf}</Typography>
                <Typography color="textSecondary">
                  Data de Admissão: {formatarDataCompetencia(funcionario.dataAdmissao)}
                </Typography>
                <Typography color="textSecondary">Cargo: {funcionario.cargoDescricao || 'N/A'}</Typography>
                <Typography color="textSecondary">
                  Centro de Custo: {funcionario.centroCustoDescricao || 'N/A'}
                </Typography>
                <Typography color="textSecondary">
                  Linha de Negócio: {funcionario.linhaNegocioDescricao || 'N/A'}
                </Typography>
                <Box display="flex" justifyContent="flex-end" mt={2}>
                  <IconButton onClick={() => handleOpen(funcionario)}>
                    <EditIcon />
                  </IconButton>
                  <IconButton onClick={() => handleDelete(funcionario.id)}>
                    <DeleteIcon />
                  </IconButton>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
        <DialogTitle>
          {selectedFuncionario ? 'Editar Funcionário' : 'Novo Funcionário'}
        </DialogTitle>
        <form onSubmit={handleSubmit(onSubmit)}>
          <DialogContent>
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Nome"
                  {...register('nome', { required: true })}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="CPF"
                  {...register('cpf', { required: true })}
                />
              </Grid>
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Data de Admissão"
                  type="date"
                  InputLabelProps={{ shrink: true }}
                  {...register('dataAdmissao', { required: true })}
                />
              </Grid>
              <Grid item xs={12}>
                <FormControl fullWidth>
                  <InputLabel>Linha de Negócio</InputLabel>
                  <Select
                    label="Linha de Negócio"
                    {...register('linhaNegocioId', { required: true })}
                  >
                    {linhasNegocio.map((linha) => (
                      <MenuItem key={linha.id} value={linha.id}>
                        {linha.descricao}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12}>
                <FormControl fullWidth>
                  <InputLabel>Cargo</InputLabel>
                  <Select
                    label="Cargo"
                    {...register('cargoId', { required: true })}
                    disabled={!linhaNegocioId}
                  >
                    {cargos.map((cargo) => (
                      <MenuItem key={cargo.id} value={cargo.id}>
                        {cargo.descricao}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
              <Grid item xs={12}>
                <FormControl fullWidth>
                  <InputLabel>Centro de Custo</InputLabel>
                  <Select
                    label="Centro de Custo"
                    {...register('centroCustoId', { required: true })}
                    disabled={!linhaNegocioId}
                  >
                    {centrosCusto.map((centro) => (
                      <MenuItem key={centro.id} value={centro.id}>
                        {centro.descricao}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
            </Grid>
          </DialogContent>
          <DialogActions>
            <Button onClick={handleClose}>Cancelar</Button>
            <Button type="submit" variant="contained" color="primary">
              Salvar
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
} 