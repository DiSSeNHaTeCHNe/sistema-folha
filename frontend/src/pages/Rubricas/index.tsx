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
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import { Add as AddIcon, Edit as EditIcon, Delete as DeleteIcon } from '@mui/icons-material';
import { useForm, Controller } from 'react-hook-form';
import { toast } from 'react-toastify';
import {
  rubricaService,
  type RubricaFiltros,
  type RubricaFormData,
  type OperadorRubrica,
} from '../../services/rubricaService';
import type { Rubrica } from '../../types';

const DEFAULT_OPERADORES: Pick<RubricaFormData, 'operadorBruto' | 'operadorLiquido' | 'operadorCusto'> = {
  operadorBruto: 1,
  operadorLiquido: 1,
  operadorCusto: 1,
};

const operadorOptions: { value: OperadorRubrica; label: string }[] = [
  { value: 1, label: '+1 (soma)' },
  { value: 0, label: '0 (ignora)' },
  { value: -1, label: '−1 (subtrai)' },
];

const DEFAULT_FILTROS: RubricaFiltros = {
  codigo: '',
  descricao: '',
  status: 'ATIVO',
};

const tiposRubrica = [
  { value: 'PROVENTO', label: 'Provento' },
  { value: 'DESCONTO', label: 'Desconto' },
  { value: 'INFORMATIVO', label: 'Informativo' },
];

export default function Rubricas() {
  const [rubricas, setRubricas] = useState<Rubrica[]>([]);
  const [open, setOpen] = useState(false);
  const [selectedRubrica, setSelectedRubrica] = useState<Rubrica | null>(null);
  const { register, handleSubmit, reset, setValue, control } = useForm<RubricaFormData>({
    defaultValues: {
      ...DEFAULT_OPERADORES,
    },
  });

  const {
    register: registerFilter,
    handleSubmit: handleFilterSubmit,
    reset: resetFilter,
    control: filterControl,
    getValues: getFilterValues,
  } = useForm<RubricaFiltros>({
    defaultValues: DEFAULT_FILTROS,
  });

  useEffect(() => {
    carregarRubricas(DEFAULT_FILTROS);
  }, []);

  const carregarRubricas = async (filtros?: RubricaFiltros) => {
    try {
      const applied = filtros ?? {
        codigo: getFilterValues('codigo'),
        descricao: getFilterValues('descricao'),
        status: getFilterValues('status') ?? 'ATIVO',
      };
      const data = await rubricaService.listar(applied);
      setRubricas(data);
    } catch {
      toast.error('Erro ao carregar rubricas');
    }
  };

  const handleFilter = async (filtros: RubricaFiltros) => {
    try {
      const data = await rubricaService.listar(filtros);
      setRubricas(data);
    } catch {
      toast.error('Erro ao filtrar rubricas');
    }
  };

  const handleClearFilter = () => {
    resetFilter(DEFAULT_FILTROS);
    carregarRubricas(DEFAULT_FILTROS);
  };

  const handleOpen = (rubrica?: Rubrica) => {
    if (rubrica) {
      setSelectedRubrica(rubrica);
      setValue('codigo', rubrica.codigo);
      setValue('descricao', rubrica.descricao);
      const tipoValue = rubrica.tipoRubricaDescricao || rubrica.tipo;
      let mappedTipo = 'INFORMATIVO';
      if (tipoValue === 'PROVENTO') mappedTipo = 'PROVENTO';
      else if (tipoValue === 'DESCONTO') mappedTipo = 'DESCONTO';
      else if (tipoValue === 'INFORMATIVO') mappedTipo = 'INFORMATIVO';
      setValue('tipo', mappedTipo);
      setValue('porcentagem', rubrica.porcentagem);
      setValue('operadorBruto', (rubrica.operadorBruto ?? 1) as OperadorRubrica);
      setValue('operadorLiquido', (rubrica.operadorLiquido ?? 1) as OperadorRubrica);
      setValue('operadorCusto', (rubrica.operadorCusto ?? 1) as OperadorRubrica);
    } else {
      setSelectedRubrica(null);
      reset({
        ...DEFAULT_OPERADORES,
        porcentagem: 100,
      });
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
    } catch {
      toast.error('Erro ao salvar rubrica');
    }
  };

  const handleDelete = async (id: number) => {
    if (window.confirm('Tem certeza que deseja excluir esta rubrica?')) {
      try {
        await rubricaService.remover(id);
        toast.success('Rubrica excluída com sucesso');
        carregarRubricas();
      } catch {
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

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Filtros
          </Typography>
          <form onSubmit={handleFilterSubmit(handleFilter)}>
            <Box display="flex" gap={2} flexWrap="wrap" alignItems="center">
              <Box flex="1" minWidth={200}>
                <TextField
                  {...registerFilter('codigo')}
                  label="Id/Código"
                  fullWidth
                  size="small"
                />
              </Box>
              <Box flex="1" minWidth={200}>
                <TextField
                  {...registerFilter('descricao')}
                  label="Descrição"
                  fullWidth
                  size="small"
                />
              </Box>
              <Box flex="1" minWidth={200}>
                <Controller
                  name="status"
                  control={filterControl}
                  render={({ field }) => (
                    <FormControl fullWidth size="small">
                      <InputLabel>Status</InputLabel>
                      <Select
                        {...field}
                        label="Status"
                        value={field.value ?? 'ATIVO'}
                      >
                        <MenuItem value="ATIVO">Ativo</MenuItem>
                        <MenuItem value="INATIVO">Inativo</MenuItem>
                        <MenuItem value="TODOS">Todos</MenuItem>
                      </Select>
                    </FormControl>
                  )}
                />
              </Box>
              <Box display="flex" gap={1}>
                <Button type="submit" variant="contained">
                  Filtrar
                </Button>
                <Button variant="outlined" onClick={handleClearFilter}>
                  Limpar
                </Button>
              </Box>
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
                  <TableCell>Código</TableCell>
                  <TableCell>Descrição</TableCell>
                  <TableCell>Tipo</TableCell>
                  <TableCell>Porcentagem</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="center">Ações</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rubricas.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} align="center">
                      Nenhuma rubrica encontrada
                    </TableCell>
                  </TableRow>
                ) : (
                  rubricas.map((rubrica) => (
                    <TableRow key={rubrica.id}>
                      <TableCell>{rubrica.codigo}</TableCell>
                      <TableCell>{rubrica.descricao}</TableCell>
                      <TableCell>
                        {rubrica.tipoRubricaDescricao || rubrica.tipo || '-'}
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
                  ))
                )}
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
              disabled={!!selectedRubrica}
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
              <Controller
                name="tipo"
                control={control}
                defaultValue=""
                render={({ field }) => (
                  <Select
                    label="Tipo"
                    {...field}
                    value={field.value || ''}
                  >
                    {tiposRubrica.map((tipo) => (
                      <MenuItem key={tipo.value} value={tipo.value}>
                        {tipo.label}
                      </MenuItem>
                    ))}
                  </Select>
                )}
              />
            </FormControl>
            <TextField
              {...register('porcentagem', {
                min: { value: 0, message: 'Porcentagem deve ser maior ou igual a 0' }
              })}
              label="Porcentagem (%)"
              type="number"
              fullWidth
              margin="normal"
              inputProps={{ min: 0, step: 0.01 }}
            />
            <Typography variant="subtitle2" sx={{ mt: 2, mb: 1 }}>
              Operadores de totalização
            </Typography>
            <FormControl fullWidth margin="normal" required>
              <InputLabel id="operador-bruto-label">Operador Bruto</InputLabel>
              <Controller
                name="operadorBruto"
                control={control}
                rules={{ required: 'Operador bruto é obrigatório' }}
                render={({ field }) => (
                  <Select
                    labelId="operador-bruto-label"
                    id="operador-bruto"
                    label="Operador Bruto"
                    {...field}
                    value={field.value ?? 1}
                  >
                    {operadorOptions.map((op) => (
                      <MenuItem key={`bruto-${op.value}`} value={op.value}>
                        {op.label}
                      </MenuItem>
                    ))}
                  </Select>
                )}
              />
            </FormControl>
            <FormControl fullWidth margin="normal" required>
              <InputLabel id="operador-liquido-label">Operador Líquido</InputLabel>
              <Controller
                name="operadorLiquido"
                control={control}
                rules={{ required: 'Operador líquido é obrigatório' }}
                render={({ field }) => (
                  <Select
                    labelId="operador-liquido-label"
                    id="operador-liquido"
                    label="Operador Líquido"
                    {...field}
                    value={field.value ?? 1}
                  >
                    {operadorOptions.map((op) => (
                      <MenuItem key={`liquido-${op.value}`} value={op.value}>
                        {op.label}
                      </MenuItem>
                    ))}
                  </Select>
                )}
              />
            </FormControl>
            <FormControl fullWidth margin="normal" required>
              <InputLabel id="operador-custo-label">Operador Custo</InputLabel>
              <Controller
                name="operadorCusto"
                control={control}
                rules={{ required: 'Operador custo é obrigatório' }}
                render={({ field }) => (
                  <Select
                    labelId="operador-custo-label"
                    id="operador-custo"
                    label="Operador Custo"
                    {...field}
                    value={field.value ?? 1}
                  >
                    {operadorOptions.map((op) => (
                      <MenuItem key={`custo-${op.value}`} value={op.value}>
                        {op.label}
                      </MenuItem>
                    ))}
                  </Select>
                )}
              />
            </FormControl>
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
