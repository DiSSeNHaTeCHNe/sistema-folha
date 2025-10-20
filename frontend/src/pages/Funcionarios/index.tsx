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
  cargoId: string;
  centroCustoId: string;
  linhaNegocioId: string;
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
  const [hoveredCardId, setHoveredCardId] = useState<number | null>(null);

  // Para novo funcionário: mostrar todos os centros de custo
  // Para edição: também mostrar todos (a linha de negócio será atualizada automaticamente)
  const centrosCustoFiltrados = centrosCusto;
  const { register, handleSubmit, reset, control } = useForm<Filtros>({
    defaultValues: {
      nome: '',
      cpf: '',
      dataAdmissao: '',
      cargoId: '',
      centroCustoId: '',
      linhaNegocioId: ''
    }
  });
  const { register: registerEdit, handleSubmit: handleSubmitEdit, reset: resetEdit, control: controlEdit, setValue: setValueEdit } = useForm({
    defaultValues: {
      nome: '',
      cpf: '',
      dataAdmissao: '',
      cargoId: '',
      centroCustoId: '',
      linhaNegocioId: ''
    }
  });

  useEffect(() => {
    carregarDados();
    carregarTodosCargos(); // Carregar todos os cargos uma vez
    carregarTodosCentrosCusto(); // Carregar todos os centros de custo uma vez
  }, []);

  // Remover useEffect problemático - a sincronização será feita no handleOpen
  
  // useEffect para inicializar o formulário quando um funcionário é selecionado
  useEffect(() => {
    if (open && selectedFuncionario) {
      const dadosFuncionario = {
        nome: selectedFuncionario.nome || '',
        cpf: selectedFuncionario.cpf || '',
        dataAdmissao: selectedFuncionario.dataAdmissao || '',
        linhaNegocioId: selectedFuncionario.linhaNegocioId?.toString() || '',
        cargoId: selectedFuncionario.cargoId?.toString() || '',
        centroCustoId: selectedFuncionario.centroCustoId?.toString() || ''
      };
      resetEdit(dadosFuncionario);
    } else if (open && !selectedFuncionario) {
      const dadosVazios = {
        nome: '',
        cpf: '',
        dataAdmissao: '',
        linhaNegocioId: '',
        cargoId: '',
        centroCustoId: ''
      };
      resetEdit(dadosVazios);
    }
  }, [open, selectedFuncionario]);


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
    setSelectedFuncionario(funcionario || null);
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
    setSelectedFuncionario(null);
    resetEdit({
      nome: '',
      cpf: '',
      dataAdmissao: '',
      linhaNegocioId: '',
      cargoId: '',
      centroCustoId: ''
    });
  };

  const onSubmit = async (data: any) => {
    try {
      // Converter strings para números onde necessário
      const dadosParaEnvio = {
        ...data,
        cargoId: data.cargoId ? Number(data.cargoId) : undefined,
        centroCustoId: data.centroCustoId ? Number(data.centroCustoId) : undefined,
        linhaNegocioId: data.linhaNegocioId ? Number(data.linhaNegocioId) : undefined
      };

      if (selectedFuncionario) {
        await api.put(`/funcionarios/${selectedFuncionario.id}`, dadosParaEnvio);
        toast.success('Funcionário atualizado com sucesso');
      } else {
        await api.post('/funcionarios', dadosParaEnvio);
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
      if (filtros.cargoId && filtros.cargoId !== '') params.append('cargoId', filtros.cargoId);
      if (filtros.centroCustoId && filtros.centroCustoId !== '') params.append('centroCustoId', filtros.centroCustoId);
      if (filtros.linhaNegocioId && filtros.linhaNegocioId !== '') params.append('linhaNegocioId', filtros.linhaNegocioId);

      const response = await api.get(`/funcionarios?${params.toString()}`);
      setFuncionarios(response.data);
      toast.success(`${response.data.length} funcionário(s) encontrado(s)`);
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
                <Controller
                  name="linhaNegocioId"
                  control={control}
                  render={({ field }) => (
                    <FormControl fullWidth>
                      <InputLabel>Linha de Negócio</InputLabel>
                      <Select
                        {...field}
                        label="Linha de Negócio"
                      >
                        <MenuItem value="">Todas</MenuItem>
                        {linhasNegocio.map((linha) => (
                          <MenuItem key={linha.id} value={linha.id.toString()}>
                            {linha.descricao}
                          </MenuItem>
                        ))}
                      </Select>
                    </FormControl>
                  )}
                />
              </Box>
              <Box flex="1" minWidth="250px">
                <Controller
                  name="cargoId"
                  control={control}
                  render={({ field }) => (
                    <FormControl fullWidth>
                      <InputLabel>Cargo</InputLabel>
                      <Select
                        {...field}
                        label="Cargo"
                      >
                        <MenuItem value="">Todos</MenuItem>
                        {cargos.map((cargo) => (
                          <MenuItem key={cargo.id} value={cargo.id.toString()}>
                            {cargo.descricao}
                          </MenuItem>
                        ))}
                      </Select>
                    </FormControl>
                  )}
                />
              </Box>
              <Box flex="1" minWidth="250px">
                <Controller
                  name="centroCustoId"
                  control={control}
                  render={({ field }) => (
                    <FormControl fullWidth>
                      <InputLabel>Centro de Custo</InputLabel>
                      <Select
                        {...field}
                        label="Centro de Custo"
                      >
                        <MenuItem value="">Todos</MenuItem>
                        {centrosCusto.map((centro) => (
                          <MenuItem key={centro.id} value={centro.id.toString()}>
                            {centro.descricao}
                          </MenuItem>
                        ))}
                      </Select>
                    </FormControl>
                  )}
                />
              </Box>
              <Box width="100%">
                <Button type="submit" variant="contained" color="primary" sx={{ mr: 2 }}>
                  Filtrar
                </Button>
                <Button 
                  variant="outlined" 
                  color="secondary"
                  onClick={() => {
                    reset();
                    carregarDados(); // Recarregar todos os funcionários
                  }}
                >
                  Limpar Filtros
                </Button>
              </Box>
            </Box>
          </form>
        </CardContent>
      </Card>

      <Box 
        sx={{ 
          display: 'grid',
          gridTemplateColumns: 'repeat(3, 1fr)',
          gap: 2,
          '@media (max-width: 1200px)': {
            gridTemplateColumns: 'repeat(2, 1fr)',
          },
          '@media (max-width: 768px)': {
            gridTemplateColumns: '1fr',
          },
        }}
      >
        {funcionarios.map((funcionario) => {
          const isHovered = hoveredCardId === funcionario.id;
          const showDetails = isHovered;

          return (
            <Box 
              key={funcionario.id}
              sx={{
                position: 'relative',
                height: '85px', // Espaço fixo reservado no grid
              }}
            >
              <Card
                onMouseEnter={() => setHoveredCardId(funcionario.id)}
                onMouseLeave={() => setHoveredCardId(null)}
                sx={{
                  position: 'absolute', // Sempre absolute para nunca afetar o layout
                  top: 0,
                  left: 0,
                  right: showDetails ? 'auto' : 0,
                  transition: 'all 0.3s ease-in-out',
                  cursor: 'pointer',
                  zIndex: showDetails ? 1000 : 1,
                  width: showDetails ? '350px' : '100%',
                  height: showDetails ? 'auto' : '85px',
                  minHeight: showDetails ? '280px' : '85px',
                  transform: showDetails ? 'scale(1.05)' : 'scale(1)',
                  boxShadow: showDetails 
                    ? '0 8px 24px rgba(0,0,0,0.15)' 
                    : '0 2px 4px rgba(0,0,0,0.1)',
                  '&:hover': {
                    boxShadow: '0 8px 24px rgba(0,0,0,0.15)',
                  },
                }}
              >
                <CardContent>
                  {/* Modo Compacto - Sempre visível */}
                  <Box
                    sx={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      minHeight: '48px',
                    }}
                  >
                    <Typography 
                      variant="h6" 
                      sx={{ 
                        fontWeight: 600,
                        fontSize: '1rem',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: showDetails ? 'normal' : 'nowrap',
                        flex: 1,
                        pr: 1,
                      }}
                    >
                      {funcionario.nome}
                    </Typography>
                    
                    <Box display="flex" gap={0.5}>
                      <IconButton 
                        size="small"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleOpen(funcionario);
                        }}
                        title="Editar"
                        sx={{
                          color: 'primary.main',
                          '&:hover': {
                            backgroundColor: 'primary.light',
                            color: 'white',
                          },
                        }}
                      >
                        <EditIcon fontSize="small" />
                      </IconButton>
                      <IconButton 
                        size="small"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDelete(funcionario.id);
                        }}
                        title="Excluir"
                        sx={{
                          color: 'error.main',
                          '&:hover': {
                            backgroundColor: 'error.light',
                            color: 'white',
                          },
                        }}
                      >
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Box>
                  </Box>

                  {/* Detalhes - Só aparecem no hover */}
                  {showDetails && (
                    <Box 
                      sx={{ 
                        mt: 2,
                        pt: 2,
                        borderTop: '1px solid #e0e0e0',
                        animation: 'fadeIn 0.3s ease-in-out',
                        '@keyframes fadeIn': {
                          '0%': {
                            opacity: 0,
                            transform: 'translateY(-10px)',
                          },
                          '100%': {
                            opacity: 1,
                            transform: 'translateY(0)',
                          },
                        },
                      }}
                    >
                      <Typography 
                        color="textSecondary" 
                        variant="body2"
                        sx={{ mb: 1 }}
                      >
                        <strong>CPF:</strong> {funcionario.cpf}
                      </Typography>
                      <Typography 
                        color="textSecondary"
                        variant="body2"
                        sx={{ mb: 1 }}
                      >
                        <strong>Data de Admissão:</strong> {formatarDataCompetencia(funcionario.dataAdmissao)}
                      </Typography>
                      <Typography 
                        color="textSecondary"
                        variant="body2"
                        sx={{ mb: 1 }}
                      >
                        <strong>Cargo:</strong> {funcionario.cargoDescricao || 'N/A'}
                      </Typography>
                      <Typography 
                        color="textSecondary"
                        variant="body2"
                        sx={{ mb: 1 }}
                      >
                        <strong>Centro de Custo:</strong> {funcionario.centroCustoDescricao || 'N/A'}
                      </Typography>
                      <Typography 
                        color="textSecondary"
                        variant="body2"
                      >
                        <strong>Linha de Negócio:</strong> {funcionario.linhaNegocioDescricao || 'N/A'}
                      </Typography>
                    </Box>
                  )}
                </CardContent>
              </Card>
            </Box>
          );
        })}
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
              />
              <TextField
                fullWidth
                label="CPF"
                {...registerEdit('cpf', { required: true })}
              />
              <TextField
                fullWidth
                label="Data de Admissão"
                type="date"
                InputLabelProps={{ shrink: true }}
                {...registerEdit('dataAdmissao', { required: true })}
              />
              <Controller
                name="cargoId"
                control={controlEdit}
                rules={{ required: 'Cargo é obrigatório' }}
                render={({ field }) => (
                  <FormControl fullWidth>
                    <InputLabel>Cargo</InputLabel>
                    <Select
                      {...field}
                      label="Cargo"
                      onChange={(e) => {
                        field.onChange(e);
                        // Quando mudar o cargo, limpar centro de custo e linha de negócio
                        if (!selectedFuncionario) {
                          setValueEdit('centroCustoId', '');
                          setValueEdit('linhaNegocioId', '');
                        }
                      }}
                    >
                      {cargos.map((cargo) => (
                        <MenuItem key={cargo.id} value={cargo.id.toString()}>
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
                rules={{ required: 'Centro de Custo é obrigatório' }}
                render={({ field }) => (
                  <FormControl fullWidth>
                    <InputLabel>Centro de Custo</InputLabel>
                    <Select
                      {...field}
                      label="Centro de Custo"
                      onChange={(e) => {
                        field.onChange(e);
                        // Atualizar linha de negócio automaticamente
                        const centroSelecionado = centrosCusto.find(c => c.id === Number(e.target.value));
                        if (centroSelecionado && centroSelecionado.linhaNegocioId) {
                          setValueEdit('linhaNegocioId', centroSelecionado.linhaNegocioId.toString());
                        }
                      }}
                    >
                      {centrosCustoFiltrados.map((centro) => (
                        <MenuItem key={centro.id} value={centro.id.toString()}>
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
                render={({ field }) => (
                  <FormControl fullWidth>
                    <InputLabel>Linha de Negócio (Informativo)</InputLabel>
                    <Select
                      {...field}
                      label="Linha de Negócio (Informativo)"
                      disabled={true}
                    >
                      {linhasNegocio.map((linha) => (
                        <MenuItem key={linha.id} value={linha.id.toString()}>
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