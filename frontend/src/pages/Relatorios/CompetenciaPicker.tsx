import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { ptBR } from 'date-fns/locale';

export interface Competencia {
  mes: number;
  ano: number;
}

interface CompetenciaPickerProps {
  value: Competencia;
  onChange: (competencia: Competencia) => void;
  disabled?: boolean;
}

function competenciaToDate({ mes, ano }: Competencia): Date {
  return new Date(ano, mes - 1, 1);
}

function dateToCompetencia(date: Date | null): Competencia | null {
  if (!date) return null;
  return { mes: date.getMonth() + 1, ano: date.getFullYear() };
}

/** @internal exported for unit tests — maps DatePicker value to API payload */
export function competenciaFromDate(date: Date | null): Competencia | null {
  return dateToCompetencia(date);
}

export function CompetenciaPicker({ value, onChange, disabled = false }: CompetenciaPickerProps) {
  return (
    <LocalizationProvider dateAdapter={AdapterDateFns} adapterLocale={ptBR}>
      <DatePicker
        label="Competência"
        views={['year', 'month']}
        openTo="month"
        value={competenciaToDate(value)}
        onChange={(date) => {
          const competencia = dateToCompetencia(date);
          if (competencia) {
            onChange(competencia);
          }
        }}
        disabled={disabled}
        slotProps={{
          textField: {
            'aria-label': 'Selecionar competência mês e ano',
            fullWidth: true,
          },
        }}
      />
    </LocalizationProvider>
  );
}
