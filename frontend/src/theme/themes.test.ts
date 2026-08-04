import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import { createTheme } from '@mui/material/styles';
import {
  CLASSICO_CHARTS,
  TEMA_IDS,
  TEMA_PADRAO,
  criarTema,
  isTemaId,
} from './themes';
import { RAZAO_MINIMA_AA, razaoContraste } from './contraste';

function lerPrimaryColorRelatorios(): string {
  const ymlPath = resolve(
    dirname(fileURLToPath(import.meta.url)),
    '../../../backend/src/main/resources/application.yml',
  );
  const content = readFileSync(ymlPath, 'utf-8');
  const match = content.match(/primary-color:\s*"([^"]+)"/);
  if (!match?.[1]) {
    throw new Error('relatorios.branding.primary-color não encontrado em application.yml');
  }
  return match[1].toLowerCase();
}

/** Defaults de fábrica do MUI para os quatro papéis semânticos. */
const DEFAULTS_MUI = {
  success: '#2e7d32',
  warning: '#ed6c02',
  error: '#d32f2f',
  info: '#0288d1',
} as const;

/**
 * Valores esperados por tema (design.md, tabela "Valores por tema").
 * `techne.info` usa a variante escurecida por contraste AA — ver nota em themes.ts.
 */
const SEMANTICAS_ESPERADAS = {
  corporate: { success: '#0F6E56', warning: '#854F0B', error: '#A32D2D', info: '#185FA5' },
  soft: { success: '#0F6E56', warning: '#854F0B', error: '#993C1D', info: '#5F5E5A' },
  indigo: { success: '#5DCAA5', warning: '#EF9F27', error: '#F09595', info: '#AFA9EC' },
  techne: { success: '#0F6E56', warning: '#8A5200', error: '#A32D2D', info: '#0A7AB0' },
} as const;

const PAPEIS = ['success', 'warning', 'error', 'info'] as const;

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

describe('themes', () => {
  it('preserves classico palette from main.tsx inline theme', () => {
    const theme = criarTema('classico');
    expect(theme.palette.primary.main).toBe('#1976d2');
    expect(theme.palette.secondary.main).toBe('#dc004e');
    expect(theme.palette.background.default).toBe('#f8f9fa');
    expect(theme.palette.info.light).toBe('#e3f2fd');
    expect(theme.palette.info.main).toBe('#1976d2');
    expect(theme.palette.success.light).toBe('#e8f5e8');
    expect(theme.palette.success.main).toBe('#2e7d32');
    expect(theme.palette.warning.light).toBe('#fff3e0');
    expect(theme.palette.warning.main).toBe('#f57c00');
    expect(theme.palette.error.light).toBe('#ffebee');
    expect(theme.palette.error.main).toBe('#c62828');
    expect(theme.palette.divider).toBe('#e9ecef');
  });

  it('exposes charts and chrome on classico', () => {
    const theme = criarTema('classico');
    expect(theme.palette.charts).toEqual([...CLASSICO_CHARTS]);
    expect(theme.palette.chrome.bg).toBe('#ffffff');
    expect(theme.palette.chrome.fgAtivo).toBe('#0d47a1');
  });

  it('validates tema ids with isTemaId', () => {
    expect(isTemaId('classico')).toBe(true);
    expect(isTemaId(null)).toBe(false);
    expect(isTemaId(undefined)).toBe(false);
    expect(isTemaId('')).toBe(false);
    expect(isTemaId(42)).toBe(false);
    expect(isTemaId('corporate')).toBe(true);
    expect(isTemaId('soft')).toBe(true);
    expect(isTemaId('indigo')).toBe(true);
    expect(isTemaId('techne')).toBe(true);
  });

  it('uses techne as default tema', () => {
    expect(TEMA_PADRAO).toBe('techne');
  });

  it('registers corporate theme with study palette tokens', () => {
    const theme = criarTema('corporate');
    expect(theme.palette.primary.main).toBe('#3B82F6');
    expect(theme.palette.chrome.bg).toBe('#0F172A');
    expect(theme.palette.background.default).toBe('#F4F6F8');
    expect(theme.palette.charts.length).toBeGreaterThan(0);
  });

  it('registers soft theme with study palette tokens', () => {
    const theme = criarTema('soft');
    expect(theme.palette.primary.main).toBe('#1D9E75');
    expect(theme.palette.chrome.bg).toBe('#F4F2EC');
    expect(theme.palette.background.default).toBe('#FBFAF7');
    expect(theme.palette.charts.length).toBeGreaterThan(0);
  });

  it('registers indigo dark theme with study palette tokens', () => {
    const theme = criarTema('indigo');
    expect(theme.palette.mode).toBe('dark');
    expect(theme.palette.primary.main).toBe('#7F77DD');
    expect(theme.palette.background.default).toBe('#12121A');
    expect(theme.palette.background.paper).toBe('#1C1C28');
    expect(theme.palette.charts.length).toBeGreaterThan(0);
  });

  it('registers techne brand theme with institutional palette tokens', () => {
    const theme = criarTema('techne');
    expect(theme.palette.primary.main).toBe('#7836FC');
    expect(theme.palette.chrome.bg).toBe('#20284E');
    expect(theme.palette.background.default).toBe('#EFF2F7');
    expect(theme.typography.fontFamily).toMatch(/^Poppins/);
    expect(theme.palette.charts.length).toBeGreaterThan(0);
  });

  it('keeps techne primary.main aligned with relatorios branding in application.yml', () => {
    const theme = criarTema('techne');
    expect(theme.palette.primary.main.toLowerCase()).toBe(lerPrimaryColorRelatorios());
  });

  it.each(TEMA_IDS)('tema %s aplica a escala tipográfica dos mockups (TEMAF-08)', (temaId) => {
    const typography = criarTema(temaId).typography;
    expect(typography.h3.fontSize).toBe('1.6875rem');
    expect(typography.h3.fontWeight).toBe(600);
    expect(typography.h4.fontSize).toBe('1.5rem');
    expect(typography.h4.fontWeight).toBe(600);
    expect(typography.h6.fontSize).toBe('1rem');
    expect(typography.h6.fontWeight).toBe(600);
  });

  // TEMAF-06 / P1-Escala AC6: só h3, h4 e h6 mudam; as demais variantes seguem o
  // default do MUI em todos os cinco temas, inclusive no `classico`, que é montado
  // fora de `montarTema` (DD-3) e por isso não é coberto por tokens.test.ts.
  it.each(TEMA_IDS)('tema %s preserva as demais variantes no default do MUI (TEMAF-06)', (temaId) => {
    const typography = criarTema(temaId).typography;
    const padrao = createTheme().typography;
    for (const variante of VARIANTES_PRESERVADAS) {
      expect(typography[variante].fontSize, `${temaId}: ${variante}.fontSize`).toBe(
        padrao[variante].fontSize,
      );
      expect(typography[variante].fontWeight, `${temaId}: ${variante}.fontWeight`).toBe(
        padrao[variante].fontWeight,
      );
      expect(typography[variante].lineHeight, `${temaId}: ${variante}.lineHeight`).toBe(
        padrao[variante].lineHeight,
      );
    }
  });

  it.each(['corporate', 'soft', 'indigo', 'techne'] as const)(
    'tema %s declara as quatro semânticas da tabela do design',
    (temaId) => {
      const palette = criarTema(temaId).palette;
      const esperado = SEMANTICAS_ESPERADAS[temaId];
      for (const papel of PAPEIS) {
        expect(palette[papel].main, `${temaId}: ${papel}.main`).toBe(esperado[papel]);
      }
    },
  );

  it.each(['corporate', 'soft', 'indigo', 'techne'] as const)(
    'tema %s não usa nenhuma semântica de fábrica do MUI',
    (temaId) => {
      const palette = criarTema(temaId).palette;
      for (const papel of PAPEIS) {
        expect(palette[papel].main, `${temaId}: ${papel}.main`).not.toBe(DEFAULTS_MUI[papel]);
      }
    },
  );

  it('declara light explícito nas quatro semânticas do indigo (DD-3)', () => {
    const palette = criarTema('indigo').palette;
    const esperado = {
      success: '#23473C',
      warning: '#4A3616',
      error: '#4A2C2C',
      info: '#2E2C4A',
    } as const;
    for (const papel of PAPEIS) {
      expect(palette[papel].light, `indigo: ${papel}.light`).toBe(esperado[papel]);
      // DD-3: fundo de avatar não pode ficar quase branco no tema escuro.
      expect(razaoContraste(palette[papel].light, '#FFFFFF'), `indigo: ${papel}.light`).toBeGreaterThan(
        RAZAO_MINIMA_AA,
      );
    }
  });
});
