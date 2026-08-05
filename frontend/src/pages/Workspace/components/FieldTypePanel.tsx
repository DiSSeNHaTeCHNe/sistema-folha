import type { DatasetFieldType } from '../types';
import { Box, Paper, Typography } from '@mui/material';
import { colors } from '../workspaceTheme';

export interface FieldTypeOption {
  tipo: DatasetFieldType;
  label: string;
  description: string;
}

export const FIELD_TYPE_OPTIONS: FieldTypeOption[] = [
  { tipo: 'TEXTO', label: 'Texto', description: 'Valores alfanuméricos livres.' },
  { tipo: 'NUMERO', label: 'Número', description: 'Inteiros ou decimais para cálculos.' },
  { tipo: 'DATA', label: 'Data', description: 'Datas no formato dd/MM/yyyy.' },
  { tipo: 'MOEDA', label: 'Moeda', description: 'Valores monetários em BRL.' },
  {
    tipo: 'REFERENCIA',
    label: 'Referência',
    description:
      'Vincula a entidades do sistema (centro de custo, funcionário ou cargo). Use para cruzar dados internos sem digitar IDs manualmente.',
  },
];

export interface FieldTypePanelProps {
  selectedType: DatasetFieldType | null;
  onSelect: (tipo: DatasetFieldType) => void;
}

export function FieldTypePanel({ selectedType, onSelect }: FieldTypePanelProps) {
  return (
    <Paper variant="outlined" sx={{ p: 2, borderColor: colors.line, bgcolor: colors.card }}>
      <Typography variant="subtitle2" sx={{ color: colors.navy, mb: 1.5 }}>
        Tipos de campo
      </Typography>
      <Box component="ul" sx={{ listStyle: 'none', m: 0, p: 0 }}>
        {FIELD_TYPE_OPTIONS.map((option) => {
          const selected = selectedType === option.tipo;
          return (
            <Box component="li" key={option.tipo} sx={{ mb: 1.5 }}>
              <Box
                component="button"
                type="button"
                onClick={() => onSelect(option.tipo)}
                aria-pressed={selected}
                aria-label={`Tipo ${option.label}`}
                sx={{
                  width: '100%',
                  textAlign: 'left',
                  border: `1px solid ${selected ? colors.violet : colors.line}`,
                  borderRadius: 1,
                  p: 1.5,
                  bgcolor: selected ? colors.aiSoft : colors.card,
                  cursor: 'pointer',
                }}
              >
                <Typography variant="body2" sx={{ fontWeight: 600, color: colors.navy }}>
                  {option.label}
                </Typography>
                <Typography variant="caption" sx={{ color: colors.soft, display: 'block', mt: 0.5 }}>
                  {option.description}
                </Typography>
              </Box>
            </Box>
          );
        })}
      </Box>
    </Paper>
  );
}
