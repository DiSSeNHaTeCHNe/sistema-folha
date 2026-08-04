import { describe, expect, it } from 'vitest';
import { createElement } from 'react';
import { render } from '@testing-library/react';
import { ThemeProvider } from '@mui/material/styles';
import Paper from '@mui/material/Paper';
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

/**
 * Quick 014: níveis de elevação que o código de aplicação realmente produz — não uma
 * matriz especulativa de 24. `Card` e `Paper` ficam no padrão (1), `Login/index.tsx:64`
 * pede 3, e `AppBar`, `Menu`/`Select`, `Drawer` e `Dialog` são os padrões do MUI para
 * esses componentes, todos presentes nas telas.
 */
const ELEVACOES_EM_USO = [
  { nome: 'Card / Paper (padrão)', elevation: 1 },
  { nome: 'Paper do Login', elevation: 3 },
  { nome: 'AppBar', elevation: 4 },
  { nome: 'Menu / Select', elevation: 8 },
  { nome: 'Drawer', elevation: 16 },
  { nome: 'Dialog', elevation: 24 },
] as const;

/** Superfícies em que `primary.main` é de fato pintado como texto ou ícone. */
const ELEVACOES_COM_PRIMARY = [1, 24] as const;

function paraHex(cor: string): string {
  const rgb = cor.match(/rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)/);
  if (rgb) {
    return `#${[rgb[1], rgb[2], rgb[3]]
      .map((canal) => Number(canal).toString(16).padStart(2, '0'))
      .join('')}`;
  }
  const hex = cor.replace('#', '').toLowerCase();
  return `#${hex.length === 3 ? [...hex].map((c) => c + c).join('') : hex}`;
}

function compor(camada: string, base: string): string {
  const rgba = camada.match(
    /rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)(?:\s*,\s*([\d.]+))?\s*\)/,
  );
  if (!rgba) {
    return base;
  }
  const alpha = rgba[4] !== undefined ? Number(rgba[4]) : 1;
  const canaisBase = [1, 3, 5].map((i) => parseInt(base.slice(i, i + 2), 16));
  return `#${[1, 2, 3]
    .map((i) =>
      Math.round(Number(rgba[i]) * alpha + canaisBase[i - 1] * (1 - alpha))
        .toString(16)
        .padStart(2, '0'),
    )
    .join('')}`;
}

/**
 * Fundo **renderizado** de um `Paper` na elevação pedida: lê o estilo computado do
 * elemento montado e compõe o overlay de elevação por cima de `background-color`
 * quando ele existe. É a rede que faltava — o teste media o token e o pixel era outro
 * (`#1C1C28` no token, `#272733` na tela; 4,53:1 medido, 3,95:1 renderizado).
 */
function fundoEfetivoDoPaper(temaId: (typeof TEMAS)[number]['id'], elevation: number): string {
  const { container, unmount } = render(
    createElement(
      ThemeProvider,
      { theme: criarTema(temaId) },
      createElement(Paper, { elevation }, 'superfície'),
    ),
  );
  const elemento = container.querySelector('.MuiPaper-root') as HTMLElement;
  const estilo = window.getComputedStyle(elemento);
  const fundo = paraHex(estilo.backgroundColor);
  const variavel = estilo.backgroundImage.match(/^var\((--[\w-]+)\)$/);
  const imagem = (
    variavel ? elemento.style.getPropertyValue(variavel[1]) : estilo.backgroundImage
  ).trim();
  unmount();

  if (!imagem || imagem === 'none') {
    return fundo;
  }
  return (imagem.match(/rgba?\([^)]*\)/g) ?? []).reduce(
    (base, camada) => compor(camada, base),
    fundo,
  );
}

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

  it.each(TEMAS.map((tema) => [tema.id] as const))(
    'tema %s renderiza background.paper sem overlay em toda elevação em uso',
    (temaId) => {
      const token = paraHex(criarTema(temaId).palette.background.paper);
      for (const nivel of ELEVACOES_EM_USO) {
        expect(
          fundoEfetivoDoPaper(temaId, nivel.elevation),
          `${temaId}: ${nivel.nome} (elevation ${nivel.elevation}) deveria pintar o token ${token}`,
        ).toBe(token);
      }
    },
  );

  it.each(TEMAS.map((tema) => [tema.id] as const))(
    'tema %s atende WCAG AA com primary.main sobre o fundo efetivo do Card e do Dialog',
    (temaId) => {
      const palette = criarTema(temaId).palette;
      for (const elevation of ELEVACOES_COM_PRIMARY) {
        expect(
          razaoContraste(palette.primary.main, fundoEfetivoDoPaper(temaId, elevation)),
          `${temaId}: primary.main / fundo efetivo em elevation ${elevation}`,
        ).toBeGreaterThanOrEqual(RAZAO_MINIMA_AA);
      }
    },
  );
});
