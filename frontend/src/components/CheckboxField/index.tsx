import { FormControlLabel, Checkbox } from '@mui/material';
import type { CheckboxProps } from '@mui/material';

interface CheckboxFieldProps extends Omit<CheckboxProps, 'value' | 'onChange'> {
  label: string;
  value: boolean;
  onChange: (value: boolean) => void;
}

export function CheckboxField({ label, value, onChange, ...props }: CheckboxFieldProps) {
  return (
    <FormControlLabel
      control={
        <Checkbox
          checked={value}
          onChange={(event) => onChange(event.target.checked)}
          {...props}
        />
      }
      label={label}
    />
  );
} 