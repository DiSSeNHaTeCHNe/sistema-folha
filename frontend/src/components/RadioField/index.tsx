import { FormControl, FormLabel, RadioGroup, FormControlLabel, Radio, RadioGroupProps } from '@mui/material';

interface Option {
  value: string;
  label: string;
}

interface RadioFieldProps extends Omit<RadioGroupProps, 'value' | 'onChange'> {
  label: string;
  value: string;
  onChange: (value: string) => void;
  options: Option[];
  row?: boolean;
}

export function RadioField({ label, value, onChange, options, row = false, ...props }: RadioFieldProps) {
  return (
    <FormControl>
      <FormLabel>{label}</FormLabel>
      <RadioGroup
        value={value}
        onChange={(event) => onChange(event.target.value)}
        row={row}
        {...props}
      >
        {options.map((option) => (
          <FormControlLabel
            key={option.value}
            value={option.value}
            control={<Radio />}
            label={option.label}
          />
        ))}
      </RadioGroup>
    </FormControl>
  );
} 