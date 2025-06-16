import { TextField } from '@mui/material';
import type { TextFieldProps } from '@mui/material';
import { NumericFormat } from 'react-number-format';
import type { NumericFormatProps } from 'react-number-format';

interface MoneyFieldProps extends Omit<TextFieldProps, 'value' | 'onChange'> {
  value: number;
  onChange: (value: number) => void;
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