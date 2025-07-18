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
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  FormControlLabel,
  Checkbox,
  FormGroup,
  Alert,
  InputAdornment,
  Divider,
} from '@mui/material';
import { 
  Add as AddIcon, 
  Edit as EditIcon, 
  Delete as DeleteIcon, 
  Search as SearchIcon,
  Visibility,
  VisibilityOff,
  Person,
  Badge,
  Security,
} from '@mui/icons-material';
import { useForm, Controller } from 'react-hook-form';
import usuarioService, { type UsuarioForm, type UsuarioFiltros } from '../../services/usuarioService';
import type { Usuario } from '../../types';
import { useNotification } from '../../hooks/useNotification';

const permissoesDisponiveis = [
  'ADMIN',
  'GESTOR',
  'OPERADOR',
  'CONSULTA',
  'FOLHA_PAGAMENTO',
  'BENEFICIOS',
  'RELATORIOS',
  'IMPORTACAO',
  'CADASTROS'
];

export default function Usuarios() {
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [funcionarios, setFuncionarios] = useState<{id: number, nome: string, cpf: string}[]>([]);
  const [open, setOpen] = useState(false);
  const [selectedUsuario, setSelectedUsuario] = useState<Usuario | null>(null);
  const [loading, setLoading] = useState(true);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const { showNotification } = useNotification();

  const { 
    control, 
    handleSubmit, 
    reset, 
    formState: { errors },
    watch
  } = useForm<UsuarioForm>({
    defaultValues: {
      login: '',
      nome: '',
      senha: '',
      funcionarioId: undefined,
      permissoes: []
    }
  });

  const {
    control: filterControl,
    handleSubmit: handleFilterSubmit,
    reset: resetFilter
  } = useForm<UsuarioFiltros>({
    defaultValues: {
      nome: '',
      login: '',
      funcionarioId: undefined
    }
  });

  const [confirmPassword, setConfirmPassword] = useState('');
  const senha = watch('senha');

  useEffect(() => {
    carregarDados();
  }, []);

  const carregarDados = async () => {
    try {
      setLoading(true);
      const [usuariosData, funcionariosData] = await Promise.all([
        usuarioService.listar(),
        usuarioService.listarFuncionarios()
      ]);
      setUsuarios(usuariosData);
      setFuncionarios(funcionariosData);
    } catch (error) {
      showNotification('Erro ao carregar dados', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleOpen = (usuario?: Usuario) => {
    if (usuario) {
      setSelectedUsuario(usuario);
      reset({
        login: usuario.login,
        nome: usuario.nome,
        senha: '',
        funcionarioId: usuario.funcionarioId,
        permissoes: usuario.permissoes
      });
    } else {
      setSelectedUsuario(null);
      reset({
        login: '',
        nome: '',
        senha: '',
        funcionarioId: undefined,
        permissoes: []
      });
    }
    setConfirmPassword('');
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
    setSelectedUsuario(null);
    reset();
    setConfirmPassword('');
  };

  const onSubmit = async (data: UsuarioForm) => {
    try {
      // Validar senhas apenas se for novo usuário ou se senha foi informada
      if (!selectedUsuario || data.senha) {
        if (data.senha !== confirmPassword) {
          showNotification('As senhas não coincidem', 'error');
          return;
        }
        if (!data.senha || data.senha.length < 6) {
          showNotification('A senha deve ter pelo menos 6 caracteres', 'error');
          return;
        }
      }

      // Remover senha se estiver vazia (para edição)
      if (selectedUsuario && !data.senha) {
        delete data.senha;
      }

      if (selectedUsuario) {
        await usuarioService.atualizar(selectedUsuario.id, data);
        showNotification('Usuário atualizado com sucesso', 'success');
      } else {
        await usuarioService.criar(data);
        showNotification('Usuário cadastrado com sucesso', 'success');
      }
      
      handleClose();
      carregarDados();
    } catch (error: any) {
      showNotification(
        error.response?.data?.message || 'Erro ao salvar usuário',
        'error'
      );
    }
  };

  const handleDelete = async (id: number) => {
    if (window.confirm('Tem certeza que deseja excluir este usuário?')) {
      try {
        await usuarioService.excluir(id);
        showNotification('Usuário excluído com sucesso', 'success');
        carregarDados();
      } catch (error) {
        showNotification('Erro ao excluir usuário', 'error');
      }
    }
  };

  const handleFilter = async (filtros: UsuarioFiltros) => {
    try {
      const usuariosData = await usuarioService.listar(filtros);
      setUsuarios(usuariosData);
    } catch (error) {
      showNotification('Erro ao filtrar usuários', 'error');
    }
  };

  const handleClearFilter = () => {
    resetFilter();
    carregarDados();
  };

  const getPermissaoColor = (permissao: string): "default" | "primary" | "secondary" | "error" | "info" | "success" | "warning" => {
    const colors: Record<string, "default" | "primary" | "secondary" | "error" | "info" | "success" | "warning"> = {
      'ADMIN': 'error',
      'GESTOR': 'warning',
      'OPERADOR': 'primary',
      'CONSULTA': 'info',
      'FOLHA_PAGAMENTO': 'success',
      'BENEFICIOS': 'secondary',
      'RELATORIOS': 'info',
      'IMPORTACAO': 'warning',
      'CADASTROS': 'primary'
    };
    return colors[permissao] || 'default';
  };

  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box mb={3}>
        <Typography variant="h4" fontWeight="bold" color="primary" gutterBottom>
          Manutenção de Usuários
        </Typography>
        <Typography variant="subtitle1" color="text.secondary">
          Gerencie os usuários do sistema
        </Typography>
      </Box>

      {/* Filtros */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Filtros
          </Typography>
          <form onSubmit={handleFilterSubmit(handleFilter)}>
            <Box display="flex" gap={2} flexWrap="wrap" alignItems="center">
              <Box flex="1" minWidth={200}>
                <Controller
                  name="nome"
                  control={filterControl}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      label="Nome"
                      fullWidth
                      size="small"
                      InputProps={{
                        startAdornment: (
                          <InputAdornment position="start">
                            <SearchIcon />
                          </InputAdornment>
                        ),
                      }}
                    />
                  )}
                />
              </Box>
              <Box flex="1" minWidth={200}>
                <Controller
                  name="login"
                  control={filterControl}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      label="Login"
                      fullWidth
                      size="small"
                    />
                  )}
                />
              </Box>
              <Box flex="1" minWidth={200}>
                <Controller
                  name="funcionarioId"
                  control={filterControl}
                  render={({ field }) => (
                    <FormControl fullWidth size="small">
                      <InputLabel>Funcionário</InputLabel>
                      <Select
                        {...field}
                        label="Funcionário"
                        value={field.value || ''}
                      >
                        <MenuItem value="">Todos</MenuItem>
                        {funcionarios.map((func) => (
                          <MenuItem key={func.id} value={func.id}>
                            {func.nome} - {func.cpf}
                          </MenuItem>
                        ))}
                      </Select>
                    </FormControl>
                  )}
                />
              </Box>
              <Box display="flex" gap={1}>
                <Button
                  type="submit"
                  variant="contained"
                  startIcon={<SearchIcon />}
                >
                  Filtrar
                </Button>
                <Button
                  variant="outlined"
                  onClick={handleClearFilter}
                >
                  Limpar
                </Button>
              </Box>
            </Box>
          </form>
        </CardContent>
      </Card>

      {/* Botão Adicionar */}
      <Box display="flex" justifyContent="flex-end" mb={2}>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => handleOpen()}
          size="large"
        >
          Novo Usuário
        </Button>
      </Box>

      {/* Tabela de Usuários */}
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Login</TableCell>
              <TableCell>Nome</TableCell>
              <TableCell>Funcionário</TableCell>
              <TableCell>Permissões</TableCell>
              <TableCell align="center">Ações</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={5} align="center">
                  Carregando...
                </TableCell>
              </TableRow>
            ) : usuarios.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} align="center">
                  Nenhum usuário encontrado
                </TableCell>
              </TableRow>
            ) : (
              usuarios.map((usuario) => (
                <TableRow key={usuario.id}>
                  <TableCell>
                    <Box display="flex" alignItems="center">
                      <Person sx={{ mr: 1, color: 'text.secondary' }} />
                      {usuario.login}
                    </Box>
                  </TableCell>
                  <TableCell>{usuario.nome}</TableCell>
                  <TableCell>
                    {usuario.funcionarioNome ? (
                      <Box>
                        <Typography variant="body2">
                          {usuario.funcionarioNome}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {usuario.funcionarioCpf}
                        </Typography>
                      </Box>
                    ) : (
                      <Typography variant="body2" color="text.secondary">
                        Não vinculado
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell>
                    <Box display="flex" flexWrap="wrap" gap={0.5}>
                      {usuario.permissoes.map((permissao) => (
                        <Chip
                          key={permissao}
                          label={permissao}
                          size="small"
                          color={getPermissaoColor(permissao)}
                        />
                      ))}
                    </Box>
                  </TableCell>
                  <TableCell align="center">
                    <IconButton
                      onClick={() => handleOpen(usuario)}
                      color="primary"
                    >
                      <EditIcon />
                    </IconButton>
                    <IconButton
                      onClick={() => handleDelete(usuario.id)}
                      color="error"
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

      {/* Dialog de Cadastro/Edição */}
      <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
        <DialogTitle>
          <Box display="flex" alignItems="center">
            {selectedUsuario ? <EditIcon sx={{ mr: 1 }} /> : <AddIcon sx={{ mr: 1 }} />}
            {selectedUsuario ? 'Editar Usuário' : 'Novo Usuário'}
          </Box>
        </DialogTitle>
        <form onSubmit={handleSubmit(onSubmit)}>
          <DialogContent>
            <Box display="flex" flexDirection="column" gap={2}>
              {/* Dados Básicos */}
              <Box>
                <Typography variant="h6" gutterBottom>
                  <Badge sx={{ mr: 1 }} />
                  Dados Básicos
                </Typography>
                <Divider sx={{ mb: 2 }} />
              </Box>
              
              <Box display="flex" gap={2} flexWrap="wrap">
                <Box flex="1" minWidth={200}>
                  <Controller
                    name="login"
                    control={control}
                    rules={{ required: 'Login é obrigatório' }}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        label="Login"
                        fullWidth
                        error={!!errors.login}
                        helperText={errors.login?.message}
                      />
                    )}
                  />
                </Box>
                
                <Box flex="1" minWidth={200}>
                  <Controller
                    name="nome"
                    control={control}
                    rules={{ required: 'Nome é obrigatório' }}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        label="Nome"
                        fullWidth
                        error={!!errors.nome}
                        helperText={errors.nome?.message}
                      />
                    )}
                  />
                </Box>
              </Box>

              <Box>
                <Controller
                  name="funcionarioId"
                  control={control}
                  render={({ field }) => (
                    <FormControl fullWidth>
                      <InputLabel>Funcionário (Opcional)</InputLabel>
                      <Select
                        {...field}
                        label="Funcionário (Opcional)"
                        value={field.value || ''}
                      >
                        <MenuItem value="">Não vincular</MenuItem>
                        {funcionarios.map((func) => (
                          <MenuItem key={func.id} value={func.id}>
                            {func.nome} - {func.cpf}
                          </MenuItem>
                        ))}
                      </Select>
                    </FormControl>
                  )}
                />
              </Box>

              {/* Senha */}
              <Box>
                <Typography variant="h6" gutterBottom>
                  <Security sx={{ mr: 1 }} />
                  Senha
                </Typography>
                <Divider sx={{ mb: 2 }} />
              </Box>

              {selectedUsuario && (
                <Box>
                  <Alert severity="info">
                    Deixe os campos de senha em branco para manter a senha atual
                  </Alert>
                </Box>
              )}

              <Box display="flex" gap={2} flexWrap="wrap">
                <Box flex="1" minWidth={200}>
                  <Controller
                    name="senha"
                    control={control}
                    rules={!selectedUsuario ? { required: 'Senha é obrigatória' } : {}}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        label="Senha"
                        type={showPassword ? 'text' : 'password'}
                        fullWidth
                        error={!!errors.senha}
                        helperText={errors.senha?.message}
                        InputProps={{
                          endAdornment: (
                            <InputAdornment position="end">
                              <IconButton
                                onClick={() => setShowPassword(!showPassword)}
                                edge="end"
                              >
                                {showPassword ? <VisibilityOff /> : <Visibility />}
                              </IconButton>
                            </InputAdornment>
                          ),
                        }}
                      />
                    )}
                  />
                </Box>

                <Box flex="1" minWidth={200}>
                  <TextField
                    label="Confirmar Senha"
                    type={showConfirmPassword ? 'text' : 'password'}
                    fullWidth
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    error={senha !== confirmPassword && confirmPassword !== ''}
                    helperText={
                      senha !== confirmPassword && confirmPassword !== '' 
                        ? 'As senhas não coincidem' 
                        : ''
                    }
                    InputProps={{
                      endAdornment: (
                        <InputAdornment position="end">
                          <IconButton
                            onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                            edge="end"
                          >
                            {showConfirmPassword ? <VisibilityOff /> : <Visibility />}
                          </IconButton>
                        </InputAdornment>
                      ),
                    }}
                  />
                </Box>
              </Box>

              {/* Permissões */}
              <Box>
                <Typography variant="h6" gutterBottom>
                  <Security sx={{ mr: 1 }} />
                  Permissões
                </Typography>
                <Divider sx={{ mb: 2 }} />
              </Box>

              <Box>
                <Controller
                  name="permissoes"
                  control={control}
                  rules={{ required: 'Selecione pelo menos uma permissão' }}
                  render={({ field }) => (
                    <FormControl component="fieldset" error={!!errors.permissoes}>
                      <FormGroup>
                        <Box display="flex" flexWrap="wrap" gap={1}>
                          {permissoesDisponiveis.map((permissao) => (
                            <Box key={permissao} minWidth={200}>
                              <FormControlLabel
                                control={
                                  <Checkbox
                                    checked={field.value.includes(permissao)}
                                    onChange={(e) => {
                                      if (e.target.checked) {
                                        field.onChange([...field.value, permissao]);
                                      } else {
                                        field.onChange(
                                          field.value.filter((p) => p !== permissao)
                                        );
                                      }
                                    }}
                                  />
                                }
                                label={permissao}
                              />
                            </Box>
                          ))}
                        </Box>
                      </FormGroup>
                      {errors.permissoes && (
                        <Typography color="error" variant="caption">
                          {errors.permissoes.message}
                        </Typography>
                      )}
                    </FormControl>
                  )}
                />
              </Box>
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={handleClose}>Cancelar</Button>
            <Button type="submit" variant="contained">
              {selectedUsuario ? 'Atualizar' : 'Cadastrar'}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
} 