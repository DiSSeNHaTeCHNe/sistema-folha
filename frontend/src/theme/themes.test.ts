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

/** Tints de avatar declarados na quick task 012 (main misturado a 88% de branco). */
const LIGHTS_ESPERADOS = {
  corporate: { success: '#E2EEEB', warning: '#F0EAE2', error: '#F4E6E6', info: '#E3ECF4' },
  soft: { success: '#E2EEEB', warning: '#F0EAE2', error: '#F3E8E4', info: '#ECECEB' },
  techne: { success: '#E2EEEB', warning: '#F1EAE0', error: '#F4E6E6', info: '#E2EFF6' },
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
  // Quick 012 (2026-08-04): DD-4 revisado por decisão do usuário — o `classico`
  // preserva as cores herdadas do tema inline, exceto onde a acessibilidade obriga.
  // `warning.main` deixou de ser #f57c00 (2.70:1 contra background.paper) e passou a
  // #b05900. Quick 013 acrescentou `primary.main` (#1976d2 → #1873cd, 4.37:1 contra
  // background.default). São as duas únicas cores do `classico` alteradas; as demais
  // seguem fixadas aqui.
  it('preserves classico palette from main.tsx inline theme', () => {
    const theme = criarTema('classico');
    expect(theme.palette.secondary.main).toBe('#dc004e');
    expect(theme.palette.background.default).toBe('#f8f9fa');
    expect(theme.palette.info.light).toBe('#e3f2fd');
    expect(theme.palette.info.main).toBe('#1976d2');
    expect(theme.palette.success.light).toBe('#e8f5e8');
    expect(theme.palette.success.main).toBe('#2e7d32');
    expect(theme.palette.warning.light).toBe('#fff3e0');
    expect(theme.palette.error.light).toBe('#ffebee');
    expect(theme.palette.error.main).toBe('#c62828');
    expect(theme.palette.divider).toBe('#e9ecef');
  });

  it('corrige warning.main do classico por acessibilidade (quick 012)', () => {
    const palette = criarTema('classico').palette;
    expect(palette.warning.main).toBe('#b05900');
    expect(palette.warning.main).not.toBe('#f57c00');
    expect(razaoContraste(palette.warning.main, palette.background.paper)).toBeGreaterThanOrEqual(
      RAZAO_MINIMA_AA,
    );
    expect(
      razaoContraste(palette.warning.main, palette.background.default),
    ).toBeGreaterThanOrEqual(RAZAO_MINIMA_AA);
  });

  it('corrige primary.main do classico por acessibilidade (quick 013)', () => {
    const palette = criarTema('classico').palette;
    expect(palette.primary.main).toBe('#1873cd');
    expect(palette.primary.main).not.toBe('#1976d2');
    expect(palette.primary.light).toBe('#e3eef9');
    // D-5: `primary.main` é cor de texto sobre as duas superfícies.
    expect(razaoContraste(palette.primary.main, palette.background.paper)).toBeGreaterThanOrEqual(
      RAZAO_MINIMA_AA,
    );
    expect(
      razaoContraste(palette.primary.main, palette.background.default),
    ).toBeGreaterThanOrEqual(RAZAO_MINIMA_AA);
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
    // Quick 013 / D-5: #3B82F6 rendia 3.68:1 como cor de texto sobre paper.
    expect(theme.palette.primary.main).toBe('#1167F4');
    expect(theme.palette.primary.main).not.toBe('#3B82F6');
    expect(theme.palette.primary.contrastText).toBe('#FFFFFF');
    expect(theme.palette.chrome.bg).toBe('#0F172A');
    expect(theme.palette.background.default).toBe('#F4F6F8');
    expect(theme.palette.charts.length).toBeGreaterThan(0);
  });

  it('registers soft theme with study palette tokens', () => {
    const theme = criarTema('soft');
    // Quick 013 / D-5: #1D9E75 rendia 3.39:1 como cor de texto sobre paper.
    expect(theme.palette.primary.main).toBe('#188361');
    expect(theme.palette.primary.main).not.toBe('#1D9E75');
    expect(theme.palette.primary.contrastText).toBe('#FFFFFF');
    expect(theme.palette.chrome.bg).toBe('#F4F2EC');
    expect(theme.palette.background.default).toBe('#FBFAF7');
    expect(theme.palette.charts.length).toBeGreaterThan(0);
  });

  it('registers indigo dark theme with study palette tokens', () => {
    const theme = criarTema('indigo');
    expect(theme.palette.mode).toBe('dark');
    // Quick 013 / D-5: tema escuro — #7F77DD foi clareado, não escurecido (4.48:1
    // sobre background.paper #1C1C28).
    expect(theme.palette.primary.main).toBe('#8078DD');
    expect(theme.palette.primary.main).not.toBe('#7F77DD');
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

  // Quick 012: tints explícitos nos temas claros (main misturado a 88% de branco).
  // Sem eles o MUI deriva `lighten(main, 0.2)`, um meio-tom que deixa o ícone do
  // avatar de KPI em ~1.5:1 sobre o próprio fundo. A razão mínima é verificada na
  // varredura de contraste; aqui ficam fixados os valores da tabela do design.
  it.each(['corporate', 'soft', 'techne'] as const)(
    'tema %s declara light explícito nas quatro semânticas (quick 012)',
    (temaId) => {
      const palette = criarTema(temaId).palette;
      const esperado = LIGHTS_ESPERADOS[temaId];
      for (const papel of PAPEIS) {
        expect(palette[papel].light, `${temaId}: ${papel}.light`).toBe(esperado[papel]);
      }
    },
  );

  // Quick 013 / D-5: `primary.light` explícito nos cinco temas. Mesmo critério e
  // mesmo estilo de tint da quick 012 (main misturado a 88% de branco), exceto no
  // `indigo`, que por DD-3 usa um `light` mais escuro que o `main`. Sem valor
  // explícito o MUI deriva `lighten(main, 0.2)` e o par `main × light` fica em ~1,4:1.
  it.each([
    ['classico', '#1873cd', '#e3eef9'],
    ['corporate', '#1167F4', '#E2EDFE'],
    ['soft', '#188361', '#E3F0EC'],
    ['indigo', '#8078DD', '#2E2B50'],
    ['techne', '#7836FC', '#EFE7FF'],
  ] as const)(
    'tema %s declara primary.light explícito e ≥ 3:1 contra primary.main (quick 013)',
    (temaId, mainEsperado, lightEsperado) => {
      const palette = criarTema(temaId).palette;
      expect(palette.primary.main, `${temaId}: primary.main`).toBe(mainEsperado);
      expect(palette.primary.light, `${temaId}: primary.light`).toBe(lightEsperado);
      // D-5: `primary.main` como cor de texto sobre a superfície do card.
      expect(
        razaoContraste(palette.primary.main, palette.background.paper),
        `${temaId}: primary.main / background.paper`,
      ).toBeGreaterThanOrEqual(RAZAO_MINIMA_AA);
      // `primary.contrastText` (texto dentro do botão preenchido) segue AA.
      expect(
        razaoContraste(palette.primary.contrastText, palette.primary.main),
        `${temaId}: primary.contrastText / primary.main`,
      ).toBeGreaterThanOrEqual(RAZAO_MINIMA_AA);
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
