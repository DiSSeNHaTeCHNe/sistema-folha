import { Alert, Button, CircularProgress } from '@mui/material';
import SystemUpdateAltIcon from '@mui/icons-material/SystemUpdateAlt';

export interface TemplateUpgradeBannerProps {
  templateName: string;
  versaoInstalada: number;
  versaoDisponivel: number;
  upgrading?: boolean;
  onUpgrade: () => void;
}

export function TemplateUpgradeBanner({
  templateName,
  versaoInstalada,
  versaoDisponivel,
  upgrading = false,
  onUpgrade,
}: TemplateUpgradeBannerProps) {
  return (
    <Alert
      severity="info"
      role="status"
      aria-label={`Atualização disponível para ${templateName}`}
      sx={{ mb: 1 }}
      action={
        <Button
          color="inherit"
          size="small"
          onClick={onUpgrade}
          disabled={upgrading}
          startIcon={upgrading ? <CircularProgress size={14} color="inherit" /> : <SystemUpdateAltIcon />}
          aria-label={`Atualizar template ${templateName} para versão ${versaoDisponivel}`}
        >
          {upgrading ? 'Atualizando…' : 'Atualizar'}
        </Button>
      }
    >
      Nova versão disponível para &quot;{templateName}&quot;: v{versaoInstalada} → v{versaoDisponivel}.
      A atualização é opcional e preserva dados compatíveis.
    </Alert>
  );
}
