/** @deprecated v2 uses WidgetBuilderPage at /workspace/:id/widgets/novo — kept for unit tests only (T28) */
import { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Drawer,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import {
  createWidgetDefinition,
  updateWidgetDefinition,
  WorkspaceApiError,
} from '../../services/workspaceService';
import type { UserWidgetDefinition, UserWidgetTipo, WidgetSourceRef } from './types';
import { FormulaEditor } from './FormulaEditor';

interface WidgetBuilderDrawerProps {
  open: boolean;
  onClose: () => void;
  definition?: UserWidgetDefinition | null;
  onSaved: (definition: UserWidgetDefinition) => void;
}

const TIPOS: UserWidgetTipo[] = ['KPI', 'TABELA', 'GRAFICO_LINHA', 'GRAFICO_BARRA'];

export function WidgetBuilderDrawer({ open, onClose, definition, onSaved }: WidgetBuilderDrawerProps) {
  const [nome, setNome] = useState('');
  const [tipo, setTipo] = useState<UserWidgetTipo>('KPI');
  const [fonteRef, setFonteRef] = useState('');
  const [formula, setFormula] = useState('');
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) {
      return;
    }
    setNome(definition?.nome ?? '');
    setTipo(definition?.tipo ?? 'KPI');
    setFonteRef(definition?.fontes[0]?.ref ?? '');
    setFormula(definition?.formula ?? '');
    setSubmitError(null);
  }, [open, definition]);

  const buildPayload = () => {
    const fontes: WidgetSourceRef[] = fonteRef.trim()
      ? [{ kind: fonteRef.startsWith('SISTEMA:') ? 'SISTEMA' : 'DATASET', ref: fonteRef.replace(/^SISTEMA:/, '') }]
      : [];
    return {
      nome: nome.trim(),
      tipo,
      fontes,
      formula: formula.trim() || null,
      config: definition?.config ?? {},
    };
  };

  const validateFormula = async (expression: string) => {
    if (!definition) {
      return;
    }
    const payload = buildPayload();
    payload.formula = expression;
    await updateWidgetDefinition(definition.id, payload);
  };

  const handleSave = async () => {
    setSaving(true);
    setSubmitError(null);
    try {
      const payload = buildPayload();
      const saved = definition
        ? await updateWidgetDefinition(definition.id, payload)
        : await createWidgetDefinition(payload);
      onSaved(saved);
      onClose();
    } catch (error) {
      if (error instanceof WorkspaceApiError) {
        const formulaError = error.errors?.find((item) => item.field === 'formula');
        setSubmitError(formulaError?.message ?? error.message);
      } else {
        setSubmitError('Erro ao salvar widget');
      }
    } finally {
      setSaving(false);
    }
  };

  return (
    <Drawer anchor="right" open={open} onClose={onClose} aria-labelledby="widget-builder-title">
      <Box sx={{ width: 420, p: 3 }} role="dialog" aria-modal="true">
        <Typography id="widget-builder-title" variant="h6" gutterBottom>
          {definition ? 'Editar widget' : 'Novo widget'}
        </Typography>
        <Stack spacing={2}>
          <TextField label="Nome" value={nome} onChange={(event) => setNome(event.target.value)} fullWidth />
          <FormControl fullWidth>
            <InputLabel id="widget-tipo-label">Tipo</InputLabel>
            <Select
              labelId="widget-tipo-label"
              label="Tipo"
              value={tipo}
              onChange={(event) => setTipo(event.target.value as UserWidgetTipo)}
            >
              {TIPOS.map((item) => (
                <MenuItem key={item} value={item}>
                  {item}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <TextField
            label="Fonte (dataset id ou SISTEMA:ORCAMENTO)"
            value={fonteRef}
            onChange={(event) => setFonteRef(event.target.value)}
            fullWidth
          />
          <FormulaEditor value={formula} onChange={setFormula} onValidate={validateFormula} disabled={saving} />
          {submitError && (
            <Typography color="error" role="alert">
              {submitError}
            </Typography>
          )}
          <Stack direction="row" spacing={1}>
            <Button variant="contained" onClick={() => void handleSave()} disabled={saving || !nome.trim()}>
              Salvar
            </Button>
            <Button onClick={onClose}>Cancelar</Button>
          </Stack>
        </Stack>
      </Box>
    </Drawer>
  );
}
