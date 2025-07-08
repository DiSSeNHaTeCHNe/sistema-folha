import { TextField } from '@mui/material';
import type { TextFieldProps } from '@mui/material';
import { NumericFormat } from 'react-number-format';

interface NumberFieldProps extends Omit<TextFieldProps, 'value' | 'onChange'> {
  value: number | string;
  onChange: (value: number | string) => void;
  label: string;
}

export function NumberField({ value, onChange, label, color, size, ...props }: NumberFieldProps) {
  // Garantir que size seja do tipo aceito pelo MUI
  const muiSize = typeof size === 'string' ? size : undefined;
  return (
    <NumericFormat
      value={value}
      onValueChange={(values: { floatValue?: number; value: string }) => {
        onChange(values.floatValue ?? values.value);
      }}
      customInput={(inputProps) => (
        <TextField {...inputProps} label={label} fullWidth color={color as TextFieldProps['color']} size={muiSize} {...props} />
      )}
    />
  );
} 