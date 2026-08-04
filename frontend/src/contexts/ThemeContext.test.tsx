import { afterEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { useEffect, useState } from 'react';
import { useTheme } from '@mui/material/styles';
import { AppThemeProvider, useAppTheme } from './ThemeContext';
import * as storage from '../theme/storage';
import { TEMAS, TEMA_PADRAO } from '../theme/themes';

function ThemeProbe() {
  const { temaId, setTemaId, temas } = useAppTheme();
  const muiTheme = useTheme();
  const [mountCount, setMountCount] = useState(0);

  useEffect(() => {
    setMountCount((count) => count + 1);
  }, []);

  return (
    <div>
      <div>tema-id:{temaId}</div>
      <div>temas-count:{temas.length}</div>
      <div>primary:{muiTheme.palette.primary.main}</div>
      <div>mount-count:{mountCount}</div>
      <button type="button" onClick={() => setTemaId('classico')}>
        apply-classico
      </button>
    </div>
  );
}

describe('ThemeContext', () => {
  afterEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('exposes temaId, setTemaId and temas from useAppTheme', () => {
    render(
      <AppThemeProvider>
        <ThemeProbe />
      </AppThemeProvider>,
    );

    expect(screen.getByText(`tema-id:${TEMA_PADRAO}`)).toBeInTheDocument();
    expect(screen.getByText(`temas-count:${TEMAS.length}`)).toBeInTheDocument();
  });

  it('initializes with techne when no preference is stored', () => {
    render(
      <AppThemeProvider>
        <ThemeProbe />
      </AppThemeProvider>,
    );

    expect(screen.getByText('tema-id:techne')).toBeInTheDocument();
    expect(screen.getByText('primary:#7836FC')).toBeInTheDocument();
  });

  it('respects stored preference instead of default tema', () => {
    vi.spyOn(storage, 'lerTemaSalvo').mockReturnValue('corporate');

    render(
      <AppThemeProvider>
        <ThemeProbe />
      </AppThemeProvider>,
    );

    expect(screen.getByText('tema-id:corporate')).toBeInTheDocument();
    expect(screen.getByText('primary:#3B82F6')).toBeInTheDocument();
  });

  it('initializes from lerTemaSalvo', () => {
    vi.spyOn(storage, 'lerTemaSalvo').mockReturnValue('classico');

    render(
      <AppThemeProvider>
        <ThemeProbe />
      </AppThemeProvider>,
    );

    expect(storage.lerTemaSalvo).toHaveBeenCalled();
    expect(screen.getByText('tema-id:classico')).toBeInTheDocument();
  });

  it('setTemaId persists preference and applies theme without remounting children', async () => {
    const gravarSpy = vi.spyOn(storage, 'gravarTema');

    render(
      <AppThemeProvider>
        <ThemeProbe />
      </AppThemeProvider>,
    );

    await waitFor(() => {
      expect(screen.getByText('mount-count:1')).toBeInTheDocument();
    });
    expect(screen.getByText('primary:#7836FC')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'apply-classico' }));

    expect(gravarSpy).toHaveBeenCalledWith('classico');
    expect(screen.getByText('tema-id:classico')).toBeInTheDocument();
    expect(screen.getByText('primary:#1976d2')).toBeInTheDocument();
    expect(screen.getByText('mount-count:1')).toBeInTheDocument();
  });

  it('throws when useAppTheme is used outside the provider', () => {
    function OrphanHook() {
      useAppTheme();
      return null;
    }

    expect(() => render(<OrphanHook />)).toThrow(
      'useAppTheme must be used within an AppThemeProvider',
    );
  });

  it('gravarTema is invoked when tema changes via setTemaId', () => {
    const gravarSpy = vi.spyOn(storage, 'gravarTema');

    render(
      <AppThemeProvider initialTemaId="classico">
        <ThemeProbe />
      </AppThemeProvider>,
    );

    fireEvent.click(screen.getByRole('button', { name: 'apply-classico' }));
    expect(gravarSpy).toHaveBeenCalledWith('classico');
  });
});
