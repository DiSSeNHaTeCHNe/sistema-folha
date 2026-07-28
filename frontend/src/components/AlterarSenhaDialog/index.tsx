import { useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  InputAdornment,
  TextField,
} from '@mui/material';
import { Visibility, VisibilityOff } from '@mui/icons-material';
import { useForm, Controller } from 'react-hook-form';
import usuarioService from '../../services/usuarioService';
import { useNotification } from '../../hooks/useNotification';
import { Notification } from '../Notification';

interface AlterarSenhaForm {
  senhaAtual: string;
  novaSenha: string;
  confirmarSenha: string;
}

interface AlterarSenhaDialogProps {
  open: boolean;
  onClose: () => void;
  userId: number;
}

export function AlterarSenhaDialog({ open, onClose, userId }: AlterarSenhaDialogProps) {
  const { notification, showNotification, hideNotification } = useNotification();
  const [senhaAtualIncorreta, setSenhaAtualIncorreta] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [showNovaSenha, setShowNovaSenha] = useState(false);
  const [showConfirmarSenha, setShowConfirmarSenha] = useState(false);

  const {
    control,
    handleSubmit,
    reset,
    watch,
    formState: { errors },
  } = useForm<AlterarSenhaForm>({
    defaultValues: {
      senhaAtual: '',
      novaSenha: '',
      confirmarSenha: '',
    },
  });

  const novaSenha = watch('novaSenha');

  useEffect(() => {
    if (open) {
      reset();
      setSenhaAtualIncorreta(false);
      setShowNovaSenha(false);
      setShowConfirmarSenha(false);
    }
  }, [open, reset]);

  const handleClose = () => {
    reset();
    setSenhaAtualIncorreta(false);
    setShowNovaSenha(false);
    setShowConfirmarSenha(false);
    onClose();
  };

  const onSubmit = async (data: AlterarSenhaForm) => {
    if (data.novaSenha.length < 6) {
      showNotification('A senha deve ter pelo menos 6 caracteres', 'error');
      return;
    }

    if (data.novaSenha !== data.confirmarSenha) {
      showNotification('As senhas não coincidem', 'error');
      return;
    }

    try {
      setSubmitting(true);
      setSenhaAtualIncorreta(false);
      await usuarioService.alterarSenha(userId, data.senhaAtual, data.novaSenha);
      showNotification('Senha alterada com sucesso', 'success');
      handleClose();
    } catch (error: unknown) {
      const axiosError = error as { response?: { status?: number } };
      if (axiosError.response?.status === 400) {
        setSenhaAtualIncorreta(true);
        return;
      }
      showNotification('Erro ao alterar senha', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
        <DialogTitle>Alterar senha</DialogTitle>
        <form onSubmit={handleSubmit(onSubmit)}>
          <DialogContent>
            {senhaAtualIncorreta && (
              <Alert severity="error" sx={{ mb: 2 }}>
                Senha atual incorreta
              </Alert>
            )}
            <Controller
              name="senhaAtual"
              control={control}
              rules={{ required: 'Senha atual é obrigatória' }}
              render={({ field }) => (
                <TextField
                  {...field}
                  label="Senha atual"
                  type="password"
                  fullWidth
                  margin="normal"
                  error={!!errors.senhaAtual}
                  helperText={errors.senhaAtual?.message}
                  autoComplete="current-password"
                />
              )}
            />
            <Controller
              name="novaSenha"
              control={control}
              rules={{
                required: 'Nova senha é obrigatória',
                minLength: { value: 6, message: 'A senha deve ter pelo menos 6 caracteres' },
              }}
              render={({ field }) => (
                <TextField
                  {...field}
                  label="Nova senha"
                  type={showNovaSenha ? 'text' : 'password'}
                  fullWidth
                  margin="normal"
                  error={!!errors.novaSenha}
                  helperText={errors.novaSenha?.message}
                  autoComplete="new-password"
                  InputProps={{
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton
                          onClick={() => setShowNovaSenha(!showNovaSenha)}
                          edge="end"
                        >
                          {showNovaSenha ? <VisibilityOff /> : <Visibility />}
                        </IconButton>
                      </InputAdornment>
                    ),
                  }}
                />
              )}
            />
            <Controller
              name="confirmarSenha"
              control={control}
              rules={{
                required: 'Confirmação de senha é obrigatória',
                validate: (value) => value === novaSenha || 'As senhas não coincidem',
              }}
              render={({ field }) => (
                <TextField
                  {...field}
                  label="Confirmar nova senha"
                  type={showConfirmarSenha ? 'text' : 'password'}
                  fullWidth
                  margin="normal"
                  error={!!errors.confirmarSenha}
                  helperText={errors.confirmarSenha?.message}
                  autoComplete="new-password"
                  InputProps={{
                    endAdornment: (
                      <InputAdornment position="end">
                        <IconButton
                          onClick={() => setShowConfirmarSenha(!showConfirmarSenha)}
                          edge="end"
                        >
                          {showConfirmarSenha ? <VisibilityOff /> : <Visibility />}
                        </IconButton>
                      </InputAdornment>
                    ),
                  }}
                />
              )}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={handleClose} disabled={submitting}>
              Cancelar
            </Button>
            <Button type="submit" variant="contained" disabled={submitting}>
              Alterar senha
            </Button>
          </DialogActions>
        </form>
      </Dialog>
      <Notification
        open={notification.open}
        message={notification.message}
        severity={notification.severity}
        onClose={hideNotification}
      />
    </>
  );
}
