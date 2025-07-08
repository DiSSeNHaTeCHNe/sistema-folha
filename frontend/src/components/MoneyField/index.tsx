import { TextField } from '@mui/material';
import { NumericFormat } from 'react-number-format';

interface MoneyFieldProps {
  value: number;
  onChange: (value: number) => void;
  label?: string;
  error?: boolean;
  helperText?: string;
  disabled?: boolean;
  required?: boolean;
  fullWidth?: boolean;
}

export function MoneyField({ value, onChange, ...props }: MoneyFieldProps) {
  return (
    <NumericFormat
      value={value}
      onValueChange={(values: { floatValue: number | undefined }) => {
        onChange(values.floatValue || 0);
      }}
      thousandSeparator="."
      decimalSeparator=","
      prefix="R$ "
      decimalScale={2}
      fixedDecimalScale
      customInput={TextField as any}
      fullWidth
      {...props}
    />
  );
} 