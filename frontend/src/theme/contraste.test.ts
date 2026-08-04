import { describe, expect, it } from 'vitest';
import { criarTema, TEMAS } from './themes';
import { RAZAO_MINIMA_AA, razaoContraste } from './contraste';

const PARES_CONTRASTE = [
  {
    nome: 'text.primary / background.default',
    fg: (palette: ReturnType<typeof criarTema>['palette']) => palette.text.primary,
    bg: (palette: ReturnType<typeof criarTema>['palette']) => palette.background.default,
  },
  {
    nome: 'text.primary / background.paper',
    fg: (palette: ReturnType<typeof criarTema>['palette']) => palette.text.primary,
    bg: (palette: ReturnType<typeof criarTema>['palette']) => palette.background.paper,
  },
  {
    nome: 'text.secondary / background.paper',
    fg: (palette: ReturnType<typeof criarTema>['palette']) => palette.text.secondary,
    bg: (palette: ReturnType<typeof criarTema>['palette']) => palette.background.paper,
  },
  {
    nome: 'chrome.fg / chrome.bg',
    fg: (palette: ReturnType<typeof criarTema>['palette']) => palette.chrome.fg,
    bg: (palette: ReturnType<typeof criarTema>['palette']) => palette.chrome.bg,
  },
  {
    nome: 'chrome.fgAtivo / chrome.selecionado',
    fg: (palette: ReturnType<typeof criarTema>['palette']) => palette.chrome.fgAtivo,
    bg: (palette: ReturnType<typeof criarTema>['palette']) => palette.chrome.selecionado,
  },
  {
    nome: 'primary.contrastText / primary.main',
    fg: (palette: ReturnType<typeof criarTema>['palette']) => palette.primary.contrastText,
    bg: (palette: ReturnType<typeof criarTema>['palette']) => palette.primary.main,
  },
] as const;

/** TEMAF-03: os quatro papéis semânticos contra a superfície em que aparecem. */
const PAPEIS_SEMANTICOS = ['success', 'warning', 'error', 'info'] as const;

/**
 * SPEC_DEVIATION: o par `classico` × `warning.main` fica fora da varredura semântica.
 * Reason: spec.md AC4 (TEMAF-03) exige 4.5:1 em todos os temas, mas o edge case do
 * `classico` e DD-4 do design exigem que ele preserve as cores de antes. O
 * `warning.main` herdado (#f57c00, fixado em themes.test.ts) rende 2.70:1 contra
 * `background.paper` — o próprio default de fábrica do MUI (#ed6c02) renderia 3.11:1.
 * Os dois requisitos são inconciliáveis sem decisão do usuário; nenhuma cor do
 * `classico` foi alterada e a exceção é a mínima possível (1 par de 20).
 */
const EXCECOES_SEMANTICAS: ReadonlySet<string> = new Set(['classico/warning']);

describe('razaoContraste', () => {
  it('returns 21 for black on white', () => {
    expect(razaoContraste('#000000', '#ffffff')).toBeCloseTo(21, 0);
  });

  it('returns 1 for identical colors', () => {
    expect(razaoContraste('#ffffff', '#ffffff')).toBeCloseTo(1, 5);
  });

  it.each(TEMAS.map((tema) => [tema.id] as const))(
    'tema %s atende WCAG AA nos pares de contraste',
    (temaId) => {
      const palette = criarTema(temaId).palette;
      for (const par of PARES_CONTRASTE) {
        const fg = par.fg(palette);
        const bg = par.bg(palette);
        expect(
          razaoContraste(fg, bg),
          `${temaId}: ${par.nome}`,
        ).toBeGreaterThanOrEqual(RAZAO_MINIMA_AA);
      }
    },
  );

  it.each(TEMAS.map((tema) => [tema.id] as const))(
    'tema %s atende WCAG AA nas cores semânticas contra background.paper',
    (temaId) => {
      const palette = criarTema(temaId).palette;
      for (const papel of PAPEIS_SEMANTICOS) {
        if (EXCECOES_SEMANTICAS.has(`${temaId}/${papel}`)) {
          continue;
        }
        expect(
          razaoContraste(palette[papel].main, palette.background.paper),
          `${temaId}: ${papel}.main / background.paper`,
        ).toBeGreaterThanOrEqual(RAZAO_MINIMA_AA);
      }
    },
  );
});
