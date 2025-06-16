import { FormControl, InputLabel, Select, MenuItem, SelectProps } from '@mui/material';

interface Option {
  value: string | number;
  label: string;
}

interface SelectFieldProps extends Omit<SelectProps, 'value' | 'onChange'> {
  label: string;
  value: string | number;
  onChange: (value: string | number) => void;
  options: Option[];
}

export function SelectField({ label, value, onChange, options, ...props }: SelectFieldProps) {
  return (
    <FormControl fullWidth>
      <InputLabel>{label}</InputLabel>
      <Select
        value={value}
        label={label}
        onChange={(event) => onChange(event.target.value)}
        {...props}
      >
        {options.map((option) => (
          <MenuItem key={option.value} value={option.value}>
            {option.label}
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
} 