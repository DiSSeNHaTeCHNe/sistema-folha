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
  TextField,
  Typography,
  FormHelperText,
} from '@mui/material';
import { Add as AddIcon, Edit as EditIcon, Delete as DeleteIcon } from '@mui/icons-material';
import { useForm, Controller } from 'react-hook-form';
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
  ativo: boolean;
}

interface CentroCusto {
  id: number;
  descricao: string;
  ativo: boolean;
  linhaNegocioId: number;
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
  const [formValues, setFormValues] = useState({
    nome: '',
    cpf: '',
    dataAdmissao: '',
    linhaNegocioId: '',
    cargoId: '',
    centroCustoId: ''
  });

  // Para novo funcionário: mostrar todos os centros de custo
  // Para edição: também mostrar todos (a linha de negócio será atualizada automaticamente)
  const centrosCustoFiltrados = centrosCusto;
  const { register, handleSubmit, reset, setValue, watch } = useForm<Filtros>({
    defaultValues: {
      nome: '',
      cpf: '',
      dataAdmissao: '',
      cargoId: '',
      centroCustoId: '',
      linhaNegocioId: ''
    }
  });
  const { register: registerEdit, handleSubmit: handleSubmitEdit, reset: resetEdit, control: controlEdit } = useForm({
    defaultValues: {
      nome: '',
      cpf: '',
      dataAdmissao: '',
      cargoId: '',
      centroCustoId: '',
      linhaNegocioId: ''
    }
  });
  const linhaNegocioId = watch('linhaNegocioId');

  useEffect(() => {
    carregarDados();
    carregarTodosCargos(); // Carregar todos os cargos uma vez
    carregarTodosCentrosCusto(); // Carregar todos os centros de custo uma vez
  }, []);



  // useEffect para sincronizar o formulário com os valores do estado
  useEffect(() => {
    if (formValues.nome) {
      resetEdit(formValues);
    }
  }, [formValues, centrosCusto, resetEdit]);



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

  const carregarTodosCargos = async () => {
    try {
      const response = await api.get('/cargos');
      setCargos(response.data);
    } catch (error) {
      toast.error('Erro ao carregar cargos');
    }
  };

  const carregarTodosCentrosCusto = async () => {
    try {
      const response = await api.get('/centros-custo');
      setCentrosCusto(response.data);
    } catch (error) {
      toast.error('Erro ao carregar centros de custo');
    }
  };



  const handleOpen = (funcionario?: FuncionarioLocal) => {
    setOpen(true);
    
    if (funcionario) {
      setSelectedFuncionario(funcionario);
      
      const dadosFuncionario = {
        nome: funcionario.nome || '',
        cpf: funcionario.cpf || '',
        dataAdmissao: funcionario.dataAdmissao || '',
        linhaNegocioId: funcionario.linhaNegocioId?.toString() || '',
        cargoId: funcionario.cargoId?.toString() || '',
        centroCustoId: funcionario.centroCustoId?.toString() || ''
      };
      
      // Definir valores no estado local
      setFormValues(dadosFuncionario);
    } else {
      setSelectedFuncionario(null);
      setFormValues({
        nome: '',
        cpf: '',
        dataAdmissao: '',
        linhaNegocioId: '',
        cargoId: '',
        centroCustoId: ''
      });
    }
  };

  const handleClose = () => {
    setOpen(false);
    setSelectedFuncionario(null);
    setFormValues({
      nome: '',
      cpf: '',
      dataAdmissao: '',
      linhaNegocioId: '',
      cargoId: '',
      centroCustoId: ''
    });
    resetEdit();
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
            <Box display="flex" flexWrap="wrap" gap={2}>
              <Box flex="1" minWidth="250px">
                <TextField
                  fullWidth
                  label="Nome"
                  {...register('nome')}
                />
              </Box>
              <Box flex="1" minWidth="250px">
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
                    <FormHelperText>
                      Campo informativo - preenchido automaticamente pelo centro de custo
                    </FormHelperText>
                  </FormControl>
              </Box>
              <Box flex="1" minWidth="250px">
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
              </Box>
              <Box flex="1" minWidth="250px">
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
              </Box>
              <Box width="100%">
                <Button type="submit" variant="contained" color="primary">
                  Filtrar
                </Button>
              </Box>
            </Box>
          </form>
        </CardContent>
      </Card>

      <Box display="flex" flexWrap="wrap" gap={2}>
        {funcionarios.map((funcionario) => (
          <Box key={funcionario.id} flex="1" minWidth="300px" maxWidth="400px">
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
          </Box>
        ))}
      </Box>

      <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
        <DialogTitle>
          {selectedFuncionario ? 'Editar Funcionário' : 'Novo Funcionário'}
        </DialogTitle>
        <form onSubmit={handleSubmitEdit(onSubmit)}>
          <DialogContent>
            <Box display="flex" flexDirection="column" gap={2}>
              <TextField
                fullWidth
                label="Nome"
                {...registerEdit('nome', { required: true })}
                defaultValue={formValues.nome}
              />
              <TextField
                fullWidth
                label="CPF"
                {...registerEdit('cpf', { required: true })}
                defaultValue={formValues.cpf}
              />
              <TextField
                fullWidth
                label="Data de Admissão"
                type="date"
                InputLabelProps={{ shrink: true }}
                {...registerEdit('dataAdmissao', { required: true })}
                defaultValue={formValues.dataAdmissao}
              />
              <Controller
                name="cargoId"
                control={controlEdit}
                defaultValue={formValues.cargoId}
                rules={{ required: 'Cargo é obrigatório' }}
                render={({ field }) => (
                  <FormControl fullWidth>
                    <InputLabel>Cargo</InputLabel>
                    <Select
                      {...field}
                      label="Cargo"
                      value={field.value || formValues.cargoId || ''}
                      onChange={(e) => {
                        field.onChange(e);
                        // Quando mudar o cargo, limpar centro de custo e linha de negócio
                        if (!selectedFuncionario) {
                          const novoFormValues = {
                            ...formValues,
                            cargoId: e.target.value.toString(),
                            centroCustoId: '',
                            linhaNegocioId: ''
                          };
                          setFormValues(novoFormValues);
                        }
                      }}
                    >
                      {cargos.map((cargo) => (
                        <MenuItem key={cargo.id} value={cargo.id}>
                          {cargo.descricao}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                )}
              />
              <Controller
                name="centroCustoId"
                control={controlEdit}
                defaultValue={formValues.centroCustoId}
                rules={{ required: 'Centro de Custo é obrigatório' }}
                render={({ field }) => (
                  <FormControl fullWidth>
                    <InputLabel>Centro de Custo</InputLabel>
                    <Select
                      {...field}
                      label="Centro de Custo"
                      value={field.value || formValues.centroCustoId || ''}
                      onChange={(e) => {
                        field.onChange(e);
                        // Atualizar linha de negócio automaticamente
                        const centroSelecionado = centrosCusto.find(c => c.id === Number(e.target.value));
                        if (centroSelecionado && centroSelecionado.linhaNegocioId) {
                          const novoFormValues = {
                            ...formValues,
                            centroCustoId: e.target.value.toString(),
                            linhaNegocioId: centroSelecionado.linhaNegocioId.toString()
                          };
                          setFormValues(novoFormValues);
                        }
                      }}
                    >
                      {centrosCustoFiltrados.map((centro) => (
                        <MenuItem key={centro.id} value={centro.id}>
                          {centro.descricao}
                        </MenuItem>
                      ))}
                    </Select>
                    <FormHelperText>
                      A seleção do centro de custo determina automaticamente a linha de negócio
                    </FormHelperText>
                  </FormControl>
                )}
              />
              <Controller
                name="linhaNegocioId"
                control={controlEdit}
                defaultValue={formValues.linhaNegocioId}
                render={({ field }) => (
                  <FormControl fullWidth>
                    <InputLabel>Linha de Negócio (Informativo)</InputLabel>
                    <Select
                      {...field}
                      label="Linha de Negócio (Informativo)"
                      value={field.value || formValues.linhaNegocioId || ''}
                      disabled={true}
                    >
                      {linhasNegocio.map((linha) => (
                        <MenuItem key={linha.id} value={linha.id}>
                          {linha.descricao}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                )}
              />
            </Box>
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