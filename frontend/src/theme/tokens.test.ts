import { describe, expect, it } from 'vitest';
import { createTheme } from '@mui/material/styles';
import { montarTema, type TokensTema } from './tokens';

const tokensExemplo: TokensTema = {
  primary: { main: '#3B82F6' },
  secondary: { main: '#dc004e' },
  success: { main: '#0F6E56' },
  warning: { main: '#854F0B' },
  error: { main: '#A32D2D' },
  info: { main: '#185FA5' },
  background: { default: '#F4F6F8' },
  charts: ['#4F46E5', '#10B981', '#F59E0B'],
  chrome: {
    bg: '#0F172A',
    fg: '#ffffff',
    fgAtivo: '#ffffff',
    selecionado: 'rgba(255, 255, 255, 0.12)',
  },
};

/** Variantes fora do escopo da escala (DD-5) — devem seguir o default do MUI. */
const VARIANTES_PRESERVADAS = [
  'body1',
  'body2',
  'subtitle1',
  'subtitle2',
  'caption',
  'h1',
  'h2',
  'h5',
] as const;

/** Defaults de fábrica do MUI para os quatro papéis semânticos. */
const DEFAULTS_MUI = {
  success: '#2e7d32',
  warning: '#ed6c02',
  error: '#d32f2f',
  info: '#0288d1',
} as const;

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

  it('populates palette.success.main from tokens instead of the MUI default', () => {
    const theme = montarTema(tokensExemplo);
    expect(theme.palette.success.main).toBe('#0F6E56');
    expect(theme.palette.success.main).not.toBe(DEFAULTS_MUI.success);
  });

  it('populates palette.warning.main from tokens instead of the MUI default', () => {
    const theme = montarTema(tokensExemplo);
    expect(theme.palette.warning.main).toBe('#854F0B');
    expect(theme.palette.warning.main).not.toBe(DEFAULTS_MUI.warning);
  });

  it('populates palette.error.main from tokens instead of the MUI default', () => {
    const theme = montarTema(tokensExemplo);
    expect(theme.palette.error.main).toBe('#A32D2D');
    expect(theme.palette.error.main).not.toBe(DEFAULTS_MUI.error);
  });

  it('populates palette.info.main from tokens instead of the MUI default', () => {
    const theme = montarTema(tokensExemplo);
    expect(theme.palette.info.main).toBe('#185FA5');
    expect(theme.palette.info.main).not.toBe(DEFAULTS_MUI.info);
  });

  it('forwards an explicit semantic light to the palette', () => {
    const theme = montarTema({
      ...tokensExemplo,
      success: { main: '#5DCAA5', light: '#1F3B34' },
    });
    expect(theme.palette.success.light).toBe('#1F3B34');
  });

  it('aplica a escala tipográfica dos mockups em h3, h4 e h6', () => {
    const theme = montarTema(tokensExemplo);
    expect(theme.typography.h3.fontSize).toBe('1.6875rem');
    expect(theme.typography.h3.fontWeight).toBe(600);
    expect(theme.typography.h4.fontSize).toBe('1.5rem');
    expect(theme.typography.h4.fontWeight).toBe(600);
    expect(theme.typography.h6.fontSize).toBe('1rem');
    expect(theme.typography.h6.fontWeight).toBe(600);
  });

  it('preserva o fontFamily do tema ao aplicar a escala', () => {
    const theme = montarTema({ ...tokensExemplo, typography: { fontFamily: 'Poppins,sans-serif' } });
    expect(theme.typography.fontFamily).toBe('Poppins,sans-serif');
    expect(theme.typography.h4.fontSize).toBe('1.5rem');
  });

  it('mantém as demais variantes no default do MUI', () => {
    const theme = montarTema(tokensExemplo);
    const padrao = createTheme().typography;
    for (const variante of VARIANTES_PRESERVADAS) {
      expect(theme.typography[variante].fontSize, variante).toBe(padrao[variante].fontSize);
      expect(theme.typography[variante].fontWeight, variante).toBe(padrao[variante].fontWeight);
    }
  });

  it('lets MUI derive light when the token omits it', () => {
    const theme = montarTema(tokensExemplo);
    expect(tokensExemplo.success?.light).toBeUndefined();
    expect(theme.palette.success.light).toBeTruthy();
    expect(theme.palette.success.light).not.toBe(theme.palette.success.main);
  });
});
