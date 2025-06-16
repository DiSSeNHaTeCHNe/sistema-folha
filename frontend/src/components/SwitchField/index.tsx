import { FormControlLabel, Switch } from '@mui/material';
import type { SwitchProps } from '@mui/material';

interface SwitchFieldProps extends Omit<SwitchProps, 'value' | 'onChange'> {
  label: string;
  value: boolean;
  onChange: (value: boolean) => void;
}

export function SwitchField({ label, value, onChange, ...props }: SwitchFieldProps) {
  return (
    <FormControlLabel
      control={
        <Switch
          checked={value}
          onChange={(event) => onChange(event.target.checked)}
          {...props}
        />
      }
      label={label}
    />
  );
} 