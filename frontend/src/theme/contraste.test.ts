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
});
