import { TextField } from '@mui/material';
import type { TextFieldProps } from '@mui/material';

interface TextAreaFieldProps extends Omit<TextFieldProps, 'value' | 'onChange'> {
  value: string;
  onChange: (value: string) => void;
  rows?: number;
}

export function TextAreaField({ value, onChange, rows = 4, ...props }: TextAreaFieldProps) {
  return (
    <TextField
      value={value}
      onChange={(event) => onChange(event.target.value)}
      multiline
      rows={rows}
      fullWidth
      {...props}
    />
  );
} 