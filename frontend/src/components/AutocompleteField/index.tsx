import { Autocomplete, TextField } from '@mui/material';
import type { AutocompleteProps } from '@mui/material';

interface Option {
  value: string | number;
  label: string;
}

interface AutocompleteFieldProps extends Omit<AutocompleteProps<Option, false, false, false>, 'value' | 'onChange'> {
  label: string;
  value: Option | null;
  onChange: (value: Option | null) => void;
  options: Option[];
}

export function AutocompleteField({ label, value, onChange, options, ...props }: AutocompleteFieldProps) {
  return (
    <Autocomplete
      value={value}
      onChange={(_event, newValue) => onChange(newValue)}
      options={options}
      getOptionLabel={(option) => option.label}
      isOptionEqualToValue={(option, value) => option.value === value.value}
      renderInput={(params) => <TextField {...params} label={label} />}
      fullWidth
      {...props}
    />
  );
} 