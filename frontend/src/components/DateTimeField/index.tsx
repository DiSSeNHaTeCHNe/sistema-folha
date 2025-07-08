import type { TextFieldProps } from '@mui/material';
import { DateTimePicker } from '@mui/x-date-pickers/DateTimePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { ptBR } from 'date-fns/locale';

interface DateTimeFieldProps extends Omit<TextFieldProps, 'value' | 'onChange'> {
  value: Date | null;
  onChange: (date: Date | null) => void;
}

export function DateTimeField({ value, onChange, ...props }: DateTimeFieldProps) {
  return (
    <LocalizationProvider dateAdapter={AdapterDateFns} adapterLocale={ptBR}>
      <DateTimePicker
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