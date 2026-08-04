import {
  Box,
  Dialog,
  DialogContent,
  DialogTitle,
  Typography,
} from '@mui/material';
import { Check as CheckIcon } from '@mui/icons-material';
import { useAppTheme } from '../../contexts/ThemeContext';
import type { TemaId } from '../../theme/themes';

interface AparenciaDialogProps {
  readonly open: boolean;
  readonly onClose: () => void;
}

export function AparenciaDialog({ open, onClose }: AparenciaDialogProps) {
  const { temaId, setTemaId, temas } = useAppTheme();

  const handleSelect = (id: TemaId) => {
    setTemaId(id);
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Aparência</DialogTitle>
      <DialogContent>
        <Box role="radiogroup" aria-label="Selecionar tema visual">
          {temas.map((tema) => {
            const isActive = temaId === tema.id;
            return (
              <Box
                key={tema.id}
                role="radio"
                aria-checked={isActive}
                tabIndex={0}
                onClick={() => handleSelect(tema.id)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    handleSelect(tema.id);
                  }
                }}
                sx={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  gap: 2,
                  p: 2,
                  mb: 1,
                  borderRadius: 1,
                  border: 1,
                  borderColor: isActive ? 'primary.main' : 'divider',
                  cursor: 'pointer',
                  bgcolor: isActive ? 'action.selected' : 'background.paper',
                  '&:focus-visible': {
                    outline: 2,
                    outlineColor: 'primary.main',
                    outlineOffset: 2,
                  },
                  '&:last-child': {
                    mb: 0,
                  },
                }}
              >
                <Box flex={1}>
                  <Typography variant="subtitle1" fontWeight="bold">
                    {tema.nome}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                    {tema.descricao}
                  </Typography>
                  <Box display="flex" gap={0.75} mt={1.5}>
                    {tema.amostras.map((cor, index) => (
                      <Box
                        key={`${tema.id}-amostra-${index}`}
                        sx={{
                          bgcolor: cor,
                          width: 28,
                          height: 28,
                          borderRadius: 0.5,
                          border: 1,
                          borderColor: 'divider',
                        }}
                        aria-label={`Amostra de cor ${index + 1}`}
                      />
                    ))}
                  </Box>
                </Box>
                {isActive && (
                  <CheckIcon color="primary" aria-hidden sx={{ mt: 0.25 }} />
                )}
              </Box>
            );
          })}
        </Box>
      </DialogContent>
    </Dialog>
  );
}
