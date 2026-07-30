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
  FormHelperText,
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
import { Controller, useForm } from 'react-hook-form';
import { toast } from 'react-toastify';
import {
  funcionarioRubricaFixaService,
  type FuncionarioRubricaFixa,
  type FuncionarioRubricaFixaFiltros,
  type FuncionarioRubricaFixaFormData,
} from '../../services/funcionarioRubricaFixaService';
import { funcionarioService } from '../../services/funcionarioService';
import { rubricaService } from '../../services/rubricaService';
import type { Funcionario, Rubrica } from '../../types';
import { formatMoneyDisplay } from '../../utils/money';

const getApiErrorMessage = (error: unknown, fallback: string): string => {
  if (typeof error !== 'object' || error === null || !('response' in error)) {
    return fallback;
  }

  const response = (error as { response?: { status?: number; data?: unknown } }).response;
  if (response?.data && typeof response.data === 'object') {
    const data = response.data as { message?: string };
    if (data.message) {
      return data.message;
    }
  }
  return fallback;
};

const formatPercentualFixa = (porcentagem: number | null | undefined = 100): string => {
  const valor = porcentagem ?? 100;
  return `${valor.toLocaleString('pt-BR', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}%`;
};

const formatFuncionarioFixa = (item: FuncionarioRubricaFixa): string => {
  if (item.funcionarioId == null) {
    return 'Todos';
  }
  return item.funcionarioNome ?? String(item.funcionarioId);
};

export default function RubricasFixas() {
  const [registros, setRegistros] = useState<FuncionarioRubricaFixa[]>([]);
  const [funcionarios, setFuncionarios] = useState<Funcionario[]>([]);
  const [rubricas, setRubricas] = useState<Rubrica[]>([]);
  const [open, setOpen] = useState(false);
  const [selected, setSelected] = useState<FuncionarioRubricaFixa | null>(null);

  const { register, handleSubmit, reset, setValue, control } = useForm<FuncionarioRubricaFixaFormData>();
  const {
    control: filterControl,
    handleSubmit: handleFilterSubmit,
    reset: resetFilter,
  } = useForm<FuncionarioRubricaFixaFiltros>({
    defaultValues: { funcionarioId: '', rubricaId: '' },
  });

  useEffect(() => {
    const carregarDados = async () => {
      try {
        const [funcionariosData, rubricasData] = await Promise.all([
          funcionarioService.listar(),
          rubricaService.listar({ status: 'ATIVO' }),
        ]);
        setFuncionarios(funcionariosData);
        setRubricas(rubricasData);
        await carregarRegistros();
      } catch {
        toast.error('Erro ao carregar dados iniciais');
      }
    };
    carregarDados();
  }, []);

  const carregarRegistros = async (filtros?: FuncionarioRubricaFixaFiltros) => {
    try {
      const data = await funcionarioRubricaFixaService.listar(filtros);
      setRegistros(data);
    } catch {
      toast.error('Erro ao carregar rubricas fixas');
    }
  };

  const handleOpen = (item?: FuncionarioRubricaFixa) => {
    if (item) {
      setSelected(item);
      setValue('funcionarioId', item.funcionarioId ?? '');
      setValue('rubricaId', item.rubricaId);
      setValue('valor', item.valor != null ? String(item.valor) : '');
      setValue('vigenciaInicio', item.vigenciaInicio);
      setValue('vigenciaFim', item.vigenciaFim ?? '');
      setValue('comentario', item.comentario ?? '');
    } else {
      setSelected(null);
      reset({
        funcionarioId: '',
        valor: '',
        vigenciaInicio: '',
        vigenciaFim: '',
        comentario: '',
      });
    }
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
    reset();
    setSelected(null);
  };

  const onSubmit = async (data: FuncionarioRubricaFixaFormData) => {
    try {
      if (selected?.id) {
        await funcionarioRubricaFixaService.atualizar(selected.id, data);
        toast.success('Rubrica fixa atualizada com sucesso');
      } else {
        await funcionarioRubricaFixaService.criar(data);
        toast.success('Rubrica fixa cadastrada com sucesso');
      }
      handleClose();
      await carregarRegistros();
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Erro ao salvar rubrica fixa'));
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('Tem certeza que deseja excluir esta rubrica fixa?')) {
      return;
    }
    try {
      await funcionarioRubricaFixaService.remover(id);
      toast.success('Rubrica fixa excluída com sucesso');
      await carregarRegistros();
    } catch {
      toast.error('Erro ao excluir rubrica fixa');
    }
  };

  return (
    <Box p={3}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h4">Rubricas Fixas</Typography>
        <Button variant="contained" startIcon={<AddIcon />} onClick={() => handleOpen()}>
          Nova Rubrica Fixa
        </Button>
      </Box>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Filtros
          </Typography>
          <form onSubmit={handleFilterSubmit(carregarRegistros)}>
            <Box display="flex" gap={2} flexWrap="wrap" alignItems="center">
              <FormControl sx={{ minWidth: 240 }} size="small">
                <InputLabel id="filtro-funcionario-label">Funcionário</InputLabel>
                <Controller
                  name="funcionarioId"
                  control={filterControl}
                  render={({ field }) => (
                    <Select
                      labelId="filtro-funcionario-label"
                      label="Funcionário"
                      value={field.value ?? ''}
                      onChange={field.onChange}
                    >
                      <MenuItem value="">Todos</MenuItem>
                      {funcionarios.map((funcionario) => (
                        <MenuItem key={funcionario.id} value={funcionario.id}>
                          {funcionario.nome}
                        </MenuItem>
                      ))}
                    </Select>
                  )}
                />
              </FormControl>
              <FormControl sx={{ minWidth: 240 }} size="small">
                <InputLabel id="filtro-rubrica-label">Rubrica</InputLabel>
                <Controller
                  name="rubricaId"
                  control={filterControl}
                  render={({ field }) => (
                    <Select
                      labelId="filtro-rubrica-label"
                      label="Rubrica"
                      value={field.value ?? ''}
                      onChange={field.onChange}
                    >
                      <MenuItem value="">Todas</MenuItem>
                      {rubricas.map((rubrica) => (
                        <MenuItem key={rubrica.id} value={rubrica.id}>
                          {rubrica.codigo} - {rubrica.descricao}
                        </MenuItem>
                      ))}
                    </Select>
                  )}
                />
              </FormControl>
              <Button type="submit" variant="contained">
                Filtrar
              </Button>
              <Button variant="outlined" onClick={() => { resetFilter(); carregarRegistros(); }}>
                Limpar
              </Button>
            </Box>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <TableContainer>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Funcionário</TableCell>
                  <TableCell>Rubrica</TableCell>
                  <TableCell>Valor</TableCell>
                  <TableCell>Percentual</TableCell>
                  <TableCell>Vigência</TableCell>
                  <TableCell>Comentário</TableCell>
                  <TableCell align="center">Ações</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {registros.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} align="center">
                      Nenhuma rubrica fixa encontrada
                    </TableCell>
                  </TableRow>
                ) : (
                  registros.map((item) => (
                    <TableRow key={item.id}>
                      <TableCell>{formatFuncionarioFixa(item)}</TableCell>
                      <TableCell>
                        {item.rubricaCodigo} - {item.rubricaDescricao}
                      </TableCell>
                      <TableCell>{formatMoneyDisplay(item.valor)}</TableCell>
                      <TableCell>{formatPercentualFixa(item.porcentagem ?? undefined)}</TableCell>
                      <TableCell>
                        {item.vigenciaInicio}
                        {item.vigenciaFim ? ` a ${item.vigenciaFim}` : ' (aberta)'}
                      </TableCell>
                      <TableCell>{item.comentario || '-'}</TableCell>
                      <TableCell align="center">
                        <IconButton color="primary" onClick={() => handleOpen(item)} aria-label="Editar rubrica fixa">
                          <EditIcon />
                        </IconButton>
                        <IconButton
                          color="error"
                          onClick={() => item.id && handleDelete(item.id)}
                          aria-label="Excluir rubrica fixa"
                        >
                          <DeleteIcon />
                        </IconButton>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
        <DialogTitle>{selected ? 'Editar Rubrica Fixa' : 'Nova Rubrica Fixa'}</DialogTitle>
        <form onSubmit={handleSubmit(onSubmit)}>
          <DialogContent>
            <FormControl fullWidth margin="normal" required>
              <InputLabel id="form-rubrica-label">Rubrica</InputLabel>
              <Controller
                name="rubricaId"
                control={control}
                rules={{ required: 'Rubrica é obrigatória' }}
                render={({ field }) => (
                  <Select
                    labelId="form-rubrica-label"
                    id="form-rubrica"
                    label="Rubrica"
                    value={field.value ?? ''}
                    onChange={(event) => field.onChange(Number(event.target.value))}
                  >
                    {rubricas.map((rubrica) => (
                      <MenuItem key={rubrica.id} value={rubrica.id}>
                        {rubrica.codigo} - {rubrica.descricao}
                      </MenuItem>
                    ))}
                  </Select>
                )}
              />
            </FormControl>
            <TextField
              {...register('valor')}
              id="valor-rubrica-fixa"
              label="Valor"
              type="text"
              inputMode="decimal"
              fullWidth
              margin="normal"
              helperText="Obrigatório se a rubrica não for calculada"
            />
            <TextField
              {...register('vigenciaInicio', { required: 'Vigência de início é obrigatória' })}
              id="vigencia-inicio"
              label="Vigência início"
              type="date"
              fullWidth
              margin="normal"
              required
              slotProps={{ inputLabel: { shrink: true } }}
            />
            <TextField
              {...register('vigenciaFim')}
              id="vigencia-fim"
              label="Vigência fim"
              type="date"
              fullWidth
              margin="normal"
              slotProps={{ inputLabel: { shrink: true } }}
              helperText="Deixe em branco para vigência aberta"
            />
            <FormControl fullWidth margin="normal">
              <InputLabel id="form-funcionario-label">Funcionário</InputLabel>
              <Controller
                name="funcionarioId"
                control={control}
                render={({ field }) => (
                  <Select
                    labelId="form-funcionario-label"
                    id="form-funcionario"
                    label="Funcionário"
                    value={field.value === '' || field.value == null ? '' : String(field.value)}
                    onChange={(event) => {
                      const raw = event.target.value;
                      field.onChange(raw === '' ? '' : Number(raw));
                    }}
                  >
                    <MenuItem value="">Todos os funcionários (mesmo valor)</MenuItem>
                    {funcionarios.map((funcionario) => (
                      <MenuItem key={funcionario.id} value={String(funcionario.id)}>
                        {funcionario.nome}
                      </MenuItem>
                    ))}
                  </Select>
                )}
              />
              <FormHelperText>Todos os funcionários (mesmo valor)</FormHelperText>
            </FormControl>
            <TextField
              {...register('comentario')}
              id="comentario-rubrica-fixa"
              label="Comentário"
              fullWidth
              margin="normal"
              multiline
              minRows={2}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={handleClose}>Cancelar</Button>
            <Button type="submit" variant="contained">
              {selected ? 'Atualizar' : 'Cadastrar'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
}
