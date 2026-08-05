import { useEffect, useState } from 'react';
import { Alert, Box, TextField } from '@mui/material';
import { WorkspaceApiError } from '../../services/workspaceService';

interface FormulaEditorProps {
  value: string;
  onChange: (value: string) => void;
  onValidate: (formula: string) => Promise<void>;
  disabled?: boolean;
}

export function FormulaEditor({ value, onChange, onValidate, disabled = false }: FormulaEditorProps) {
  const [error, setError] = useState<string | null>(null);
  const [validating, setValidating] = useState(false);

  useEffect(() => {
    if (!value.trim()) {
      setError(null);
      return;
    }

    const timer = window.setTimeout(() => {
      setValidating(true);
      void onValidate(value)
        .then(() => setError(null))
        .catch((err: unknown) => {
          if (err instanceof WorkspaceApiError) {
            const formulaError = err.errors?.find((item) => item.field === 'formula');
            setError(formulaError?.message ?? err.message);
          } else {
            setError('Fórmula inválida');
          }
        })
        .finally(() => setValidating(false));
    }, 400);

    return () => window.clearTimeout(timer);
  }, [value, onValidate]);

  return (
    <Box>
      <TextField
        label="Fórmula"
        fullWidth
        multiline
        minRows={2}
        value={value}
        disabled={disabled}
        error={Boolean(error)}
        helperText={validating ? 'Validando fórmula…' : error ?? 'Ex.: SOMA(campo) * MÉDIA(outro)'}
        onChange={(event) => onChange(event.target.value)}
      />
      {error && (
        <Alert severity="error" sx={{ mt: 1 }} role="alert">
          {error}
        </Alert>
      )}
    </Box>
  );
}
