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
  {
    /**
     * Quick 013 / D-5 (code-review R-2): `primary.main` também é usado como **cor de
     * texto** sobre `background.paper` — valores monetários, `Button variant="text"`,
     * chips "Normal"/"Filtrar"/"Ver Funcionários" em /folha-pagamento, /relatorios,
     * /usuarios, /rubricas, /rubricas-fixas e /importacao. É par diferente do
     * `primary.contrastText / primary.main` acima (texto *dentro* do botão preenchido)
     * e ninguém o media: reprovava em soft (3,39), corporate (3,68), classico (4,37)
     * e indigo (4,48).
     */
    nome: 'primary.main / background.paper',
    fg: (palette: ReturnType<typeof criarTema>['palette']) => palette.primary.main,
    bg: (palette: ReturnType<typeof criarTema>['palette']) => palette.background.paper,
  },
] as const;

/** TEMAF-03: os quatro papéis semânticos contra a superfície em que aparecem. */
const PAPEIS_SEMANTICOS = ['success', 'warning', 'error', 'info'] as const;

/**
 * Papéis cujo par `main × light` é renderizado lado a lado. `primary` entra por
 * quick 013: `primary.light` é fundo em Funcionarios/index.tsx:534 e
 * Organograma/index.tsx:617, com `primary.main` como cor de primeiro plano.
 */
const PAPEIS_COM_TINT = ['primary', ...PAPEIS_SEMANTICOS] as const;

/**
 * WCAG 1.4.11 (componente gráfico não textual): o ícone do avatar de KPI é pintado
 * em `X.main` sobre `X.light` (Dashboard/index.tsx:206,234,262,290,500,550), par que
 * a varredura AA contra `background.paper` não enxerga (code-review R-2).
 */
const RAZAO_MINIMA_GRAFICA = 3;

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
        expect(
          razaoContraste(palette[papel].main, palette.background.paper),
          `${temaId}: ${papel}.main / background.paper`,
        ).toBeGreaterThanOrEqual(RAZAO_MINIMA_AA);
      }
    },
  );

  it.each(TEMAS.map((tema) => [tema.id] as const))(
    'tema %s atende WCAG 1.4.11 no par ícone × fundo do avatar de KPI',
    (temaId) => {
      const palette = criarTema(temaId).palette;
      for (const papel of PAPEIS_COM_TINT) {
        expect(
          razaoContraste(palette[papel].main, palette[papel].light),
          `${temaId}: ${papel}.main / ${papel}.light`,
        ).toBeGreaterThanOrEqual(RAZAO_MINIMA_GRAFICA);
      }
    },
  );
});
