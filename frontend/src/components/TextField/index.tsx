import { TextField as MuiTextField } from '@mui/material';
import type { TextFieldProps } from '@mui/material';

interface CustomTextFieldProps extends Omit<TextFieldProps, 'value' | 'onChange'> {
  value: string;
  onChange: (value: string) => void;
}

export function TextField({ value, onChange, ...props }: CustomTextFieldProps) {
  return (
    <MuiTextField
      value={value}
      onChange={(event) => onChange(event.target.value)}
      fullWidth
      {...props}
    />
  );
} 