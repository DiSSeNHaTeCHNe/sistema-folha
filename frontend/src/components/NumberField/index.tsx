import { TextField } from '@mui/material';
import type { TextFieldProps } from '@mui/material';
import { NumericFormat } from 'react-number-format';
import type { NumericFormatProps } from 'react-number-format';

interface NumberFieldProps extends Omit<TextFieldProps, 'value' | 'onChange'> {
  value: number;
  onChange: (value: number) => void;
  decimalScale?: number;
  prefix?: string;
  suffix?: string;
}

export function NumberField({
  value,
  onChange,
  decimalScale = 0,
  prefix,
  suffix,
  ...props
}: NumberFieldProps) {
  return (
    <NumericFormat
      value={value}
      onValueChange={(values: { floatValue: number | undefined }) => {
        onChange(values.floatValue || 0);
      }}
      thousandSeparator="."
      decimalSeparator=","
      decimalScale={decimalScale}
      fixedDecimalScale={decimalScale > 0}
      prefix={prefix}
      suffix={suffix}
      customInput={TextField as any}
      fullWidth
      {...props}
    />
  );
} 