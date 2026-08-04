import { afterEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { AparenciaDialog } from './index';
import { AppThemeProvider } from '../../contexts/ThemeContext';
import * as storage from '../../theme/storage';
import { TEMAS } from '../../theme/themes';

const mockOnClose = vi.fn();

function renderDialog(open = true, initialTemaId?: 'classico') {
  return render(
    <AppThemeProvider initialTemaId={initialTemaId}>
      <AparenciaDialog open={open} onClose={mockOnClose} />
    </AppThemeProvider>,
  );
}

describe('AparenciaDialog', () => {
  afterEach(() => {
    mockOnClose.mockClear();
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('renders dialog title and lists all registered themes with name and description', () => {
    renderDialog();

    expect(screen.getByRole('heading', { name: 'Aparência' })).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: /Clássico/i })).toBeInTheDocument();
    expect(
      screen.getByText('Tema azul MUI padrão, idêntico à aparência original do sistema.'),
    ).toBeInTheDocument();
  });

  it('marks the active theme with aria-checked true', () => {
    renderDialog(true, 'classico');

    const activeOption = screen.getByRole('radio', { name: /Clássico/i });
    expect(activeOption).toHaveAttribute('aria-checked', 'true');
  });

  it('applies theme immediately when a theme is selected', () => {
    const gravarSpy = vi.spyOn(storage, 'gravarTema');

    renderDialog(true, 'classico');

    fireEvent.click(screen.getByRole('radio', { name: /Clássico/i }));

    expect(gravarSpy).toHaveBeenCalledWith('classico');
    expect(screen.getByRole('radio', { name: /Clássico/i })).toHaveAttribute(
      'aria-checked',
      'true',
    );
  });

  it('selects theme via Enter and Space keyboard navigation', () => {
    const gravarSpy = vi.spyOn(storage, 'gravarTema');
    renderDialog(true, 'classico');

    const option = screen.getByRole('radio', { name: /Clássico/i });
    option.focus();

    fireEvent.keyDown(option, { key: 'Enter' });
    expect(gravarSpy).toHaveBeenCalledWith('classico');

    gravarSpy.mockClear();
    fireEvent.keyDown(option, { key: ' ' });
    expect(gravarSpy).toHaveBeenCalledWith('classico');
  });

  it('preserves active theme when closed without selecting a different theme', () => {
    const gravarSpy = vi.spyOn(storage, 'gravarTema');
    renderDialog(true, 'classico');

    fireEvent.keyDown(screen.getByRole('dialog'), { key: 'Escape' });

    expect(mockOnClose).toHaveBeenCalled();
    expect(gravarSpy).not.toHaveBeenCalled();
  });

  it('displays color samples for each theme', () => {
    renderDialog();

    const totalAmostras = TEMAS.reduce((total, tema) => total + tema.amostras.length, 0);
    expect(screen.getAllByLabelText(/Amostra de cor/)).toHaveLength(totalAmostras);
  });
});
