import { describe, expect, it } from 'vitest';
import { montarTema, type TokensTema } from './tokens';

const tokensExemplo: TokensTema = {
  primary: { main: '#3B82F6' },
  secondary: { main: '#dc004e' },
  background: { default: '#F4F6F8' },
  charts: ['#4F46E5', '#10B981', '#F59E0B'],
  chrome: {
    bg: '#0F172A',
    fg: '#ffffff',
    fgAtivo: '#ffffff',
    selecionado: 'rgba(255, 255, 255, 0.12)',
  },
};

describe('montarTema', () => {
  it('populates palette.charts from tokens', () => {
    const theme = montarTema(tokensExemplo);
    expect(theme.palette.charts.length).toBeGreaterThan(0);
    expect(theme.palette.charts).toEqual(tokensExemplo.charts);
  });

  it('populates palette.chrome.bg from tokens', () => {
    const theme = montarTema(tokensExemplo);
    expect(theme.palette.chrome.bg).toBe(tokensExemplo.chrome.bg);
  });

  it('applies component overrides parameterized by chrome tokens', () => {
    const theme = montarTema(tokensExemplo);
    const appBar = theme.components?.MuiAppBar?.styleOverrides?.root;
    const drawer = theme.components?.MuiDrawer?.styleOverrides?.paper;

    expect(appBar).toMatchObject({
      backgroundColor: tokensExemplo.chrome.bg,
      color: tokensExemplo.chrome.fg,
    });
    expect(drawer).toMatchObject({
      backgroundColor: tokensExemplo.chrome.bg,
      color: tokensExemplo.chrome.fg,
    });
  });
});
