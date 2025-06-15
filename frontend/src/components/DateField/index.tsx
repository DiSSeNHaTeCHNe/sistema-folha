import { TextField, TextFieldProps } from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { ptBR } from 'date-fns/locale';

interface DateFieldProps extends Omit<TextFieldProps, 'value' | 'onChange'> {
  value: Date | null;
  onChange: (date: Date | null) => void;
}

export function DateField({ value, onChange, ...props }: DateFieldProps) {
  return (
    <LocalizationProvider dateAdapter={AdapterDateFns} adapterLocale={ptBR}>
      <DatePicker
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