import SettingsIcon from '@mui/icons-material/Settings';
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import type { SelectChangeEvent } from '@mui/material';
import { useEffect, useState } from 'react';
import { resumoFolhaPagamentoService } from '../../services/resumoFolhaPagamentoService';
import type { WidgetConfig } from './types';
import {
  getConfigFieldsForWidget,
  METRICA_OPTIONS,
  TIPO_VISUALIZACAO_OPTIONS,
  TOP_N_MAX,
  TOP_N_MIN,
} from './widgetConfigOptions';

interface WidgetConfigPanelProps {
  widgetId: string;
  config: WidgetConfig | null | undefined;
  onChange: (config: WidgetConfig | null) => void;
  validationErrors?: string[];
}

function toCompetenciaKey(competenciaInicio: string): string {
  return competenciaInicio.slice(0, 7);
}

function formatCompetenciaLabel(competencia: string): string {
  const [year, month] = competencia.split('-');
  const monthNames = [
    'Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun',
    'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez',
  ];
  const monthIndex = Number(month) - 1;
  return `${monthNames[monthIndex] ?? month}/${year}`;
}

export function WidgetConfigPanel({
  widgetId,
  config,
  onChange,
  validationErrors = [],
}: WidgetConfigPanelProps) {
  const fields = getConfigFieldsForWidget(widgetId);
  const [competenciaOpcoes, setCompetenciaOpcoes] = useState<string[]>([]);

  useEffect(() => {
    let cancelled = false;
    async function carregar() {
      try {
        const resumos = await resumoFolhaPagamentoService.listarMaisRecentes();
        if (cancelled) {
          return;
        }
        const unique = [...new Set(resumos.map((r) => toCompetenciaKey(r.competenciaInicio)))];
        setCompetenciaOpcoes(unique.sort((a, b) => b.localeCompare(a)));
      } catch {
        if (!cancelled) {
          setCompetenciaOpcoes([]);
        }
      }
    }
    void carregar();
    return () => {
      cancelled = true;
    };
  }, []);

  if (fields.length === 0) {
    return null;
  }

  const current = config ?? {};

  const updateConfig = (patch: Partial<WidgetConfig>) => {
    const merged = { ...current, ...patch };
    const cleaned = Object.fromEntries(
      Object.entries(merged).filter(([, value]) => value != null && value !== ''),
    ) as WidgetConfig;
    onChange(Object.keys(cleaned).length > 0 ? cleaned : null);
  };

  const handleTopNChange = (value: string) => {
    if (value === '') {
      const { topN: _removed, ...rest } = current;
      onChange(Object.keys(rest).length > 0 ? rest : null);
      return;
    }
    updateConfig({ topN: Number(value) });
  };

  const handleCompetenciaChange = (event: SelectChangeEvent<string>) => {
    const value = event.target.value;
    if (value === '') {
      const { competencia: _removed, ...rest } = current;
      onChange(Object.keys(rest).length > 0 ? rest : null);
      return;
    }
    updateConfig({ competencia: value });
  };

  const handleSelectChange =
    (field: keyof WidgetConfig) => (event: SelectChangeEvent<string>) => {
      const value = event.target.value;
      if (value === '') {
        const rest = { ...current };
        delete rest[field];
        onChange(Object.keys(rest).length > 0 ? rest : null);
        return;
      }
      updateConfig({ [field]: value } as Partial<WidgetConfig>);
    };

  return (
    <Accordion disableGutters elevation={0} sx={{ mb: 1, '&:before': { display: 'none' } }}>
      <AccordionSummary expandIcon={<SettingsIcon fontSize="small" />} aria-label="Configurações do widget">
        <Typography variant="caption">Configurações</Typography>
      </AccordionSummary>
      <AccordionDetails sx={{ pt: 0 }}>
        <Stack spacing={2}>
          {fields.includes('competencia') && (
            <FormControl size="small" fullWidth>
              <InputLabel id={`competencia-${widgetId}-label`}>Competência fixa</InputLabel>
              <Select
                labelId={`competencia-${widgetId}-label`}
                value={current.competencia ?? ''}
                label="Competência fixa"
                displayEmpty
                onChange={handleCompetenciaChange}
                renderValue={(selected) => {
                  if (!selected) {
                    return 'Usar global';
                  }
                  return formatCompetenciaLabel(selected);
                }}
              >
                <MenuItem value="">Usar global</MenuItem>
                {competenciaOpcoes.map((competencia) => (
                  <MenuItem key={competencia} value={competencia}>
                    {formatCompetenciaLabel(competencia)}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          )}

          {fields.includes('topN') && (
            <TextField
              size="small"
              type="number"
              label="Quantidade de itens (topN)"
              value={current.topN ?? ''}
              inputProps={{ min: TOP_N_MIN, max: TOP_N_MAX, 'aria-label': 'Quantidade de itens topN' }}
              onChange={(event) => handleTopNChange(event.target.value)}
              helperText={`Entre ${TOP_N_MIN} e ${TOP_N_MAX}`}
            />
          )}

          {fields.includes('metrica') && (
            <FormControl size="small" fullWidth>
              <InputLabel id={`metrica-${widgetId}-label`}>Métrica</InputLabel>
              <Select
                labelId={`metrica-${widgetId}-label`}
                value={current.metrica ?? ''}
                label="Métrica"
                displayEmpty
                onChange={handleSelectChange('metrica')}
              >
                <MenuItem value="">Padrão</MenuItem>
                {METRICA_OPTIONS.map((option) => (
                  <MenuItem key={option.value} value={option.value}>
                    {option.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          )}

          {fields.includes('tipoVisualizacao') && (
            <FormControl size="small" fullWidth>
              <InputLabel id={`tipo-${widgetId}-label`}>Visualização</InputLabel>
              <Select
                labelId={`tipo-${widgetId}-label`}
                value={current.tipoVisualizacao ?? ''}
                label="Visualização"
                displayEmpty
                onChange={handleSelectChange('tipoVisualizacao')}
              >
                <MenuItem value="">Padrão (pizza)</MenuItem>
                {TIPO_VISUALIZACAO_OPTIONS.map((option) => (
                  <MenuItem key={option.value} value={option.value}>
                    {option.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          )}

          {validationErrors.length > 0 && (
            <Typography variant="caption" color="error" role="alert">
              {validationErrors.join('; ')}
            </Typography>
          )}
        </Stack>
      </AccordionDetails>
    </Accordion>
  );
}
