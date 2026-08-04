import { afterEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { AparenciaDialog } from './index';
import { AppThemeProvider } from '../../contexts/ThemeContext';
import * as storage from '../../theme/storage';
import { TEMAS } from '../../theme/themes';

const mockOnClose = vi.fn();

function renderDialog(open = true, initialTemaId?: Parameters<typeof AppThemeProvider>[0]['initialTemaId']) {
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

  it('renders one radio option per registered theme', () => {
    renderDialog();

    expect(screen.getAllByRole('radio')).toHaveLength(TEMAS.length);
    for (const tema of TEMAS) {
      expect(screen.getByRole('radio', { name: new RegExp(tema.nome, 'i') })).toBeInTheDocument();
    }
  });

  it('marks the active theme with aria-checked true', () => {
    renderDialog(true, 'classico');

    const activeOption = screen.getByRole('radio', { name: /Clássico/i });
    expect(activeOption).toHaveAttribute('aria-checked', 'true');
  });

  it('applies theme immediately when switching to a different theme', () => {
    const gravarSpy = vi.spyOn(storage, 'gravarTema');

    renderDialog(true, 'classico');

    expect(screen.getByRole('radio', { name: /Clássico/i })).toHaveAttribute('aria-checked', 'true');
    expect(screen.getByRole('radio', { name: /Techne brand/i })).toHaveAttribute('aria-checked', 'false');

    fireEvent.click(screen.getByRole('radio', { name: /Techne brand/i }));

    expect(gravarSpy).toHaveBeenCalledWith('techne');
    expect(screen.getByRole('radio', { name: /Techne brand/i })).toHaveAttribute('aria-checked', 'true');
    expect(screen.getByRole('radio', { name: /Clássico/i })).toHaveAttribute('aria-checked', 'false');
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
