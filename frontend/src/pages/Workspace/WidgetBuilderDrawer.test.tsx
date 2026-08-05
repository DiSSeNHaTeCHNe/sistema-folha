import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useState } from 'react';
import { fireEvent, screen, waitFor } from '@testing-library/react';
import { FormulaEditor } from './FormulaEditor';
import { WidgetBuilderDrawer } from './WidgetBuilderDrawer';
import { renderWithProviders } from '../../test/renderWithProviders';
import {
  createWidgetDefinition,
  updateWidgetDefinition,
  WorkspaceApiError,
} from '../../services/workspaceService';

vi.mock('../../services/workspaceService', () => ({
  createWidgetDefinition: vi.fn(),
  updateWidgetDefinition: vi.fn(),
  WorkspaceApiError: class WorkspaceApiError extends Error {
    status: number;
    errors?: { field: string; message: string }[];
    constructor(status: number, message: string, errors?: { field: string; message: string }[]) {
      super(message);
      this.status = status;
      this.errors = errors;
    }
  },
}));

describe('FormulaEditor', () => {
  function ControlledFormulaEditor({ onValidate }: { onValidate: (v: string) => Promise<void> }) {
    const [value, setValue] = useState('');
    return <FormulaEditor value={value} onChange={setValue} onValidate={onValidate} />;
  }

  it('debounces async validation success', async () => {
    const onValidate = vi.fn().mockResolvedValue(undefined);
    renderWithProviders(<ControlledFormulaEditor onValidate={onValidate} />);
    fireEvent.change(screen.getByLabelText('Fórmula'), { target: { value: 'SOMA(a)' } });
    await waitFor(() => expect(onValidate).toHaveBeenCalledWith('SOMA(a)'), { timeout: 800 });
  });

  it('shows server validation error for formula field', async () => {
    const onValidate = vi
      .fn()
      .mockRejectedValue(new WorkspaceApiError(400, 'Inválida', [{ field: 'formula', message: 'Campo desconhecido' }]));
    renderWithProviders(<ControlledFormulaEditor onValidate={onValidate} />);
    fireEvent.change(screen.getByLabelText('Fórmula'), { target: { value: 'SOMA(x)' } });
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Campo desconhecido'), { timeout: 800 });
  });

  it('clears error when formula emptied', () => {
    renderWithProviders(<FormulaEditor value="" onChange={vi.fn()} onValidate={vi.fn()} />);
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});

describe('WidgetBuilderDrawer', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(createWidgetDefinition).mockResolvedValue({
      id: 1,
      nome: 'KPI Teste',
      tipo: 'KPI',
      fontes: [{ kind: 'DATASET', ref: '1' }],
      formula: 'SOMA(valor)',
      config: {},
      invalido: false,
    });
    vi.mocked(updateWidgetDefinition).mockResolvedValue({
      id: 2,
      nome: 'Editado',
      tipo: 'TABELA',
      fontes: [{ kind: 'SISTEMA', ref: 'ORCAMENTO' }],
      formula: null,
      config: {},
      invalido: false,
    });
  });

  it('renders create form when open', () => {
    renderWithProviders(<WidgetBuilderDrawer open onClose={vi.fn()} onSaved={vi.fn()} />);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByLabelText('Nome')).toBeInTheDocument();
  });

  it('creates widget on save', async () => {
    const onSaved = vi.fn();
    renderWithProviders(<WidgetBuilderDrawer open onClose={vi.fn()} onSaved={onSaved} />);
    fireEvent.change(screen.getByLabelText('Nome'), { target: { value: 'KPI Teste' } });
    fireEvent.change(screen.getByLabelText(/Fonte/i), { target: { value: '1' } });
    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }));
    await waitFor(() => expect(createWidgetDefinition).toHaveBeenCalled());
    expect(onSaved).toHaveBeenCalled();
  });

  it('updates existing widget', async () => {
    renderWithProviders(
      <WidgetBuilderDrawer
        open
        onClose={vi.fn()}
        onSaved={vi.fn()}
        definition={{
          id: 2,
          nome: 'Antigo',
          tipo: 'KPI',
          fontes: [{ kind: 'DATASET', ref: '1' }],
          formula: null,
          config: {},
          invalido: false,
        }}
      />,
    );
    fireEvent.change(screen.getByLabelText('Nome'), { target: { value: 'Editado' } });
    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }));
    await waitFor(() => expect(updateWidgetDefinition).toHaveBeenCalledWith(2, expect.objectContaining({ nome: 'Editado' })));
  });

  it('shows submit error from API', async () => {
    vi.mocked(createWidgetDefinition).mockRejectedValue(
      new WorkspaceApiError(400, 'Erro', [{ field: 'formula', message: 'Função inválida' }]),
    );
    renderWithProviders(<WidgetBuilderDrawer open onClose={vi.fn()} onSaved={vi.fn()} />);
    fireEvent.change(screen.getByLabelText('Nome'), { target: { value: 'X' } });
    fireEvent.click(screen.getByRole('button', { name: 'Salvar' }));
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Função inválida'));
  });

  it('disables save without nome', () => {
    renderWithProviders(<WidgetBuilderDrawer open onClose={vi.fn()} onSaved={vi.fn()} />);
    expect(screen.getByRole('button', { name: 'Salvar' })).toBeDisabled();
  });

  it('calls onClose when cancel clicked', () => {
    const onClose = vi.fn();
    renderWithProviders(<WidgetBuilderDrawer open onClose={onClose} onSaved={vi.fn()} />);
    fireEvent.click(screen.getByRole('button', { name: 'Cancelar' }));
    expect(onClose).toHaveBeenCalled();
  });

  it('pre-fills fields when editing definition', () => {
    renderWithProviders(
      <WidgetBuilderDrawer
        open
        onClose={vi.fn()}
        onSaved={vi.fn()}
        definition={{
          id: 3,
          nome: 'Widget KPI',
          tipo: 'GRAFICO_LINHA',
          fontes: [{ kind: 'SISTEMA', ref: 'ORCAMENTO' }],
          formula: 'SOMA(x)',
          config: {},
          invalido: false,
        }}
      />,
    );
    expect(screen.getByDisplayValue('Widget KPI')).toBeInTheDocument();
    expect(screen.getByDisplayValue('SOMA(x)')).toBeInTheDocument();
  });
});
