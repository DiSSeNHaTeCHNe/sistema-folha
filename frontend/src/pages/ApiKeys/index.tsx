import { useCallback, useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
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
  Tooltip,
  Typography,
} from '@mui/material';
import {
  Add as AddIcon,
  ContentCopy as CopyIcon,
  Delete as DeleteIcon,
  VpnKey as VpnKeyIcon,
} from '@mui/icons-material';
import { toast } from 'react-toastify';
import { useAuth } from '../../contexts/AuthContext';
import apiKeyService, {
  type ApiKeyCreated,
  type ApiKeyListItem,
} from '../../services/apiKeyService';
import usuarioService from '../../services/usuarioService';
import { isAdmin } from '../../utils/permissions';
import type { Usuario } from '../../types';

const getApiErrorMessage = (error: unknown, fallback: string): string => {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { data?: { message?: string } } }).response;
    if (response?.data?.message) {
      return response.data.message;
    }
  }
  return fallback;
};

const formatDateTime = (value: string | null): string => {
  if (!value) {
    return '—';
  }
  return new Date(value).toLocaleString('pt-BR');
};

export default function ApiKeys() {
  const { user } = useAuth();
  const userIsAdmin = isAdmin(user);
  const [keys, setKeys] = useState<ApiKeyListItem[]>([]);
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [selectedUsuarioId, setSelectedUsuarioId] = useState<number | ''>('');
  const [loading, setLoading] = useState(true);
  const [createOpen, setCreateOpen] = useState(false);
  const [secretOpen, setSecretOpen] = useState(false);
  const [createdKey, setCreatedKey] = useState<ApiKeyCreated | null>(null);
  const [nome, setNome] = useState('');
  const [diasValidade, setDiasValidade] = useState('365');
  const [creating, setCreating] = useState(false);

  const alvoUsuarioId = userIsAdmin && selectedUsuarioId !== ''
    ? selectedUsuarioId
    : user?.id;

  const carregarKeys = useCallback(async () => {
    if (alvoUsuarioId == null) {
      return;
    }
    setLoading(true);
    try {
      const data = await apiKeyService.listar(userIsAdmin ? alvoUsuarioId : undefined);
      setKeys(data);
    } catch {
      toast.error('Erro ao carregar API Keys');
    } finally {
      setLoading(false);
    }
  }, [alvoUsuarioId, userIsAdmin]);

  useEffect(() => {
    void carregarKeys();
  }, [carregarKeys]);

  useEffect(() => {
    if (!userIsAdmin) {
      return;
    }
    usuarioService.listar()
      .then(setUsuarios)
      .catch(() => toast.error('Erro ao carregar usuários'));
  }, [userIsAdmin]);

  const handleOpenCreate = () => {
    setNome('');
    setDiasValidade('365');
    setCreateOpen(true);
  };

  const handleCreate = async () => {
    const dias = Number.parseInt(diasValidade, 10);
    if (!nome.trim()) {
      toast.error('Informe um nome para a API Key');
      return;
    }
    if (Number.isNaN(dias) || dias < 1 || dias > 365) {
      toast.error('Validade deve estar entre 1 e 365 dias');
      return;
    }

    setCreating(true);
    try {
      const created = await apiKeyService.criar({ nome: nome.trim(), diasValidade: dias });
      setCreatedKey(created);
      setCreateOpen(false);
      setSecretOpen(true);
      await carregarKeys();
      toast.success('API Key criada com sucesso');
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Erro ao criar API Key'));
    } finally {
      setCreating(false);
    }
  };

  const handleCopySecret = async () => {
    if (!createdKey?.chave) {
      return;
    }
    try {
      await navigator.clipboard.writeText(createdKey.chave);
      toast.success('Secret copiado para a área de transferência');
    } catch {
      toast.error('Não foi possível copiar o secret');
    }
  };

  const handleRevogar = async (id: number) => {
    if (!window.confirm('Revogar esta API Key? Autenticações futuras serão bloqueadas.')) {
      return;
    }
    try {
      await apiKeyService.revogar(id);
      toast.success('API Key revogada');
      await carregarKeys();
    } catch (error) {
      toast.error(getApiErrorMessage(error, 'Erro ao revogar API Key'));
    }
  };

  return (
    <Box>
      <Box display="flex" alignItems="center" gap={1} mb={2}>
        <VpnKeyIcon color="primary" aria-hidden="true" />
        <Typography variant="h5" component="h1">
          API Keys
        </Typography>
        <Chip label="Somente leitura" color="info" size="small" aria-label="Escopo somente leitura" />
      </Box>

      <Alert severity="info" sx={{ mb: 2 }}>
        API Keys permitem autenticação em integrações e agentes em modo somente leitura.
        O secret completo é exibido apenas uma vez, na criação.
      </Alert>

      {userIsAdmin && (
        <FormControl sx={{ minWidth: 280, mb: 2 }} size="small">
          <InputLabel id="usuario-api-keys-label">Usuário</InputLabel>
          <Select
            labelId="usuario-api-keys-label"
            label="Usuário"
            value={selectedUsuarioId === '' ? user?.id ?? '' : selectedUsuarioId}
            onChange={(event) => setSelectedUsuarioId(Number(event.target.value))}
          >
            {usuarios.map((u) => (
              <MenuItem key={u.id} value={u.id}>
                {u.nome} ({u.login})
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      )}

      <Box mb={2}>
        <Button variant="contained" startIcon={<AddIcon />} onClick={handleOpenCreate}>
          Nova API Key
        </Button>
      </Box>

      <Card>
        <CardContent>
          <TableContainer>
            <Table aria-label="Lista de API Keys">
              <TableHead>
                <TableRow>
                  <TableCell>Nome</TableCell>
                  <TableCell>Prefixo</TableCell>
                  <TableCell>Escopo</TableCell>
                  <TableCell>Expira em</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Último uso</TableCell>
                  <TableCell align="right">Ações</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {loading && (
                  <TableRow>
                    <TableCell colSpan={7}>Carregando...</TableCell>
                  </TableRow>
                )}
                {!loading && keys.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={7}>Nenhuma API Key encontrada</TableCell>
                  </TableRow>
                )}
                {!loading && keys.map((key) => (
                  <TableRow key={key.id}>
                    <TableCell>{key.nome}</TableCell>
                    <TableCell>
                      <Typography variant="body2" fontFamily="monospace">
                        {key.prefixo}…
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Chip label="Somente leitura" size="small" variant="outlined" />
                    </TableCell>
                    <TableCell>{formatDateTime(key.dataExpiracao)}</TableCell>
                    <TableCell>
                      <Chip
                        label={key.revogado ? 'Revogada' : 'Ativa'}
                        color={key.revogado ? 'default' : 'success'}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>{formatDateTime(key.ultimoUsoEm)}</TableCell>
                    <TableCell align="right">
                      {!key.revogado && (
                        <Tooltip title="Revogar">
                          <IconButton
                            aria-label={`Revogar API Key ${key.nome}`}
                            color="error"
                            onClick={() => void handleRevogar(key.id)}
                          >
                            <DeleteIcon />
                          </IconButton>
                        </Tooltip>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>

      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Nova API Key</DialogTitle>
        <DialogContent>
          <Box display="flex" flexDirection="column" gap={2} mt={1}>
            <TextField
              label="Nome"
              value={nome}
              onChange={(event) => setNome(event.target.value)}
              inputProps={{ maxLength: 100 }}
              required
              fullWidth
            />
            <TextField
              label="Validade (dias)"
              type="number"
              value={diasValidade}
              onChange={(event) => setDiasValidade(event.target.value)}
              inputProps={{ min: 1, max: 365 }}
              helperText="Entre 1 e 365 dias (padrão: 365)"
              fullWidth
            />
            <Chip label="Somente leitura" color="info" size="small" sx={{ alignSelf: 'flex-start' }} />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateOpen(false)}>Cancelar</Button>
          <Button variant="contained" onClick={() => void handleCreate()} disabled={creating}>
            Criar
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={secretOpen} onClose={() => setSecretOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Secret da API Key — copie agora</DialogTitle>
        <DialogContent>
          <Alert severity="warning" sx={{ mb: 2 }}>
            Este secret não será exibido novamente. Guarde-o em local seguro.
          </Alert>
          <TextField
            label="Secret"
            value={createdKey?.chave ?? ''}
            fullWidth
            InputProps={{ readOnly: true }}
            aria-label="Secret da API Key"
          />
        </DialogContent>
        <DialogActions>
          <Button startIcon={<CopyIcon />} onClick={() => void handleCopySecret()}>
            Copiar
          </Button>
          <Button variant="contained" onClick={() => setSecretOpen(false)}>
            Fechar
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
