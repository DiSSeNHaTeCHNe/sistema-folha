import type { TextFieldProps } from '@mui/material';
import { TimePicker } from '@mui/x-date-pickers/TimePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { ptBR } from 'date-fns/locale';

interface TimeFieldProps extends Omit<TextFieldProps, 'value' | 'onChange'> {
  value: Date | null;
  onChange: (date: Date | null) => void;
}

export function TimeField({ value, onChange, ...props }: TimeFieldProps) {
  return (
    <LocalizationProvider dateAdapter={AdapterDateFns} adapterLocale={ptBR}>
      <TimePicker
        value={value}
        onChange={onChange}
        slotProps={{
          textField: {
            fullWidth: true,
            ...props,
          },
        }}
      />
    </LocalizationProvider>
  );
} 