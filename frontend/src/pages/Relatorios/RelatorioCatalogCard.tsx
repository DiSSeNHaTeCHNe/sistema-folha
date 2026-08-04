import {
  Box,
  Button,
  Card,
  CardActions,
  CardContent,
  CircularProgress,
  Typography,
} from '@mui/material';
import { PictureAsPdf as PdfIcon } from '@mui/icons-material';
import type { ReactNode } from 'react';
import { RelatorioStatusBadge, type RelatorioStatus } from './RelatorioStatusBadge';
import { formatMoneyDisplay } from '../../utils/money';

export interface RelatorioCatalogCardProps {
  title: string;
  description: string;
  icon: ReactNode;
  status?: RelatorioStatus;
  erro?: string;
  totalLabel?: string;
  totalValue?: number | string;
  onGenerate: () => void;
  onDownload?: () => void;
  onRetry?: () => void;
  generating?: boolean;
  downloading?: boolean;
  stale?: boolean;
}

export function RelatorioCatalogCard({
  title,
  description,
  icon,
  status,
  erro,
  totalLabel,
  totalValue,
  onGenerate,
  onDownload,
  onRetry,
  generating = false,
  downloading = false,
  stale = false,
}: RelatorioCatalogCardProps) {
  const isPending = status === 'PENDENTE';
  const isPendingActive = isPending && !stale;
  const isPendingStale = isPending && stale;
  const isProcessed = status === 'PROCESSADO';
  const isError = status === 'ERRO';

  return (
    <Card
      component="article"
      aria-labelledby={`relatorio-card-title-${title.replace(/\s+/g, '-').toLowerCase()}`}
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        borderRadius: 2,
        boxShadow: '0 4px 20px rgba(0,0,0,0.08)',
      }}
    >
      <CardContent sx={{ flexGrow: 1, p: 3 }}>
        <Box display="flex" alignItems="flex-start" justifyContent="space-between" mb={2}>
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              width: 56,
              height: 56,
              borderRadius: 2,
              bgcolor: 'primary.main',
              color: 'primary.contrastText',
            }}
            aria-hidden="true"
          >
            {icon}
          </Box>
          {status && <RelatorioStatusBadge status={status} />}
        </Box>

        <Typography
          id={`relatorio-card-title-${title.replace(/\s+/g, '-').toLowerCase()}`}
          variant="h6"
          component="h2"
          gutterBottom
          fontWeight={600}
        >
          {title}
        </Typography>

        <Typography variant="body2" color="text.secondary" mb={2}>
          {description}
        </Typography>

        {totalLabel && totalValue !== undefined && isProcessed && (
          <Typography variant="body2" color="text.primary">
            {totalLabel}: <strong>{formatMoneyDisplay(totalValue)}</strong>
          </Typography>
        )}

        {isPendingActive && (
          <Box display="flex" alignItems="center" gap={1} mt={2} role="status" aria-live="polite">
            <CircularProgress size={20} aria-hidden="true" />
            <Typography variant="body2" color="text.secondary">
              Gerando relatório…
            </Typography>
          </Box>
        )}

        {isPendingStale && (
          <Typography variant="body2" color="warning.main" mt={2} role="status">
            Geração travada — tente novamente.
          </Typography>
        )}

        {isError && erro && (
          <Typography variant="body2" color="error" mt={2} role="alert">
            {erro}
          </Typography>
        )}

        {isProcessed && (
          <Box
            display="flex"
            alignItems="center"
            justifyContent="center"
            mt={2}
            p={2}
            sx={{
              bgcolor: 'grey.50',
              borderRadius: 1,
              border: '1px dashed',
              borderColor: 'grey.300',
            }}
            aria-label="Pré-visualização do relatório PDF"
          >
            <PdfIcon sx={{ fontSize: 48, color: 'error.main' }} aria-hidden="true" />
          </Box>
        )}
      </CardContent>

      <CardActions sx={{ p: 2, pt: 0, gap: 1, flexWrap: 'wrap' }}>
        {!status && (
          <Button
            variant="contained"
            onClick={onGenerate}
            disabled={generating}
            aria-label={`Gerar ${title}`}
            startIcon={generating ? <CircularProgress size={16} color="inherit" /> : undefined}
          >
            Gerar
          </Button>
        )}

        {isPendingActive && (
          <Button variant="outlined" disabled aria-label={`Gerar ${title} — aguardando processamento`}>
            Gerando…
          </Button>
        )}

        {isPendingStale && onRetry && (
          <Button
            variant="contained"
            color="warning"
            onClick={onRetry}
            disabled={generating}
            aria-label={`Tentar novamente ${title}`}
          >
            Tentar novamente
          </Button>
        )}

        {isProcessed && onDownload && (
          <Button
            variant="contained"
            onClick={onDownload}
            disabled={downloading}
            aria-label={`Baixar PDF ${title}`}
            startIcon={downloading ? <CircularProgress size={16} color="inherit" /> : <PdfIcon />}
          >
            Baixar PDF
          </Button>
        )}

        {isError && onRetry && (
          <Button
            variant="contained"
            color="warning"
            onClick={onRetry}
            disabled={generating}
            aria-label={`Tentar novamente ${title}`}
          >
            Tentar novamente
          </Button>
        )}

        {(isProcessed || isError) && (
          <Button
            variant="outlined"
            onClick={onGenerate}
            disabled={generating || isPendingActive}
            aria-label={`Gerar novamente ${title}`}
          >
            Gerar novamente
          </Button>
        )}
      </CardActions>
    </Card>
  );
}
