import { createTheme, type Theme } from '@mui/material/styles';
import { ESCALA_TIPOGRAFICA, montarTema, type TokensTema } from './tokens';

/** Paleta de gráficos em uso no Dashboard (pieColors). */
export const CLASSICO_CHARTS = [
  '#4F46E5',
  '#10B981',
  '#F59E0B',
  '#EF4444',
  '#8B5CF6',
  '#06B6D4',
  '#84CC16',
  '#F97316',
] as const;

const CORPORATE_CHARTS = [
  '#3B82F6',
  '#0F6E56',
  '#F59E0B',
  '#EF4444',
  '#8B5CF6',
  '#06B6D4',
  '#64748B',
  '#1E3A5F',
] as const;

const TOKENS_CORPORATE: TokensTema = {
  primary: { main: '#3B82F6', contrastText: '#0F172A' },
  secondary: { main: '#0F6E56' },
  // Quick 012 / DD-3: `light` explícito como tint (main misturado a 88% de branco).
  // A derivação do MUI (`lighten(main, 0.2)`) produz um meio-tom e deixa o par
  // ícone × fundo do avatar de KPI em ~1.5:1, abaixo dos 3:1 do WCAG 1.4.11.
  success: { main: '#0F6E56', light: '#E2EEEB' },
  warning: { main: '#854F0B', light: '#F0EAE2' },
  error: { main: '#A32D2D', light: '#F4E6E6' },
  info: { main: '#185FA5', light: '#E3ECF4' },
  background: { default: '#F4F6F8', paper: '#FFFFFF' },
  divider: '#DDE3EA',
  charts: [...CORPORATE_CHARTS],
  chrome: {
    bg: '#0F172A',
    fg: '#94A3B8',
    fgAtivo: '#FFFFFF',
    selecionado: '#1E3A5F',
  },
};

const SOFT_CHARTS = [
  '#1D9E75',
  '#D85A30',
  '#2C2C2A',
  '#854F0B',
  '#0F6E56',
  '#888780',
  '#993C1D',
  '#5F5E5A',
] as const;

const TOKENS_SOFT: TokensTema = {
  primary: { main: '#1D9E75', contrastText: '#0F172A' },
  secondary: { main: '#D85A30' },
  // Quick 012 / DD-3: tints explícitos — ver nota em TOKENS_CORPORATE.
  success: { main: '#0F6E56', light: '#E2EEEB' },
  warning: { main: '#854F0B', light: '#F0EAE2' },
  error: { main: '#993C1D', light: '#F3E8E4' },
  info: { main: '#5F5E5A', light: '#ECECEB' },
  background: { default: '#FBFAF7', paper: '#FFFFFF' },
  divider: '#E3E0D6',
  charts: [...SOFT_CHARTS],
  chrome: {
    bg: '#F4F2EC',
    fg: '#5F5E5A',
    fgAtivo: '#2C2C2A',
    selecionado: '#E4E0D3',
  },
};

const INDIGO_CHARTS = [
  '#7F77DD',
  '#5DCAA5',
  '#F0997B',
  '#EF9F27',
  '#AFA9EC',
  '#F09595',
  '#8A88A3',
  '#5DCAA5',
] as const;

const TOKENS_INDIGO: TokensTema = {
  mode: 'dark',
  primary: { main: '#7F77DD', contrastText: '#12121A' },
  secondary: { main: '#5DCAA5' },
  // DD-3: `light` explícito no tema escuro — a derivação do MUI clareia `main` e
  // produziria fundo de avatar quase branco sobre o paper #1C1C28.
  success: { main: '#5DCAA5', light: '#23473C' },
  warning: { main: '#EF9F27', light: '#4A3616' },
  error: { main: '#F09595', light: '#4A2C2C' },
  info: { main: '#AFA9EC', light: '#2E2C4A' },
  background: { default: '#12121A', paper: '#1C1C28' },
  divider: '#2A2A38',
  charts: [...INDIGO_CHARTS],
  chrome: {
    bg: '#0C0C12',
    fg: '#8A88A3',
    fgAtivo: '#EDECF7',
    selecionado: '#231F3E',
  },
};

const TECHNE_CHARTS = [
  '#7836FC',
  '#3661FC',
  '#4C57D6',
  '#20284E',
  '#10B981',
  '#F59E0B',
  '#EF4444',
  '#06B6D4',
] as const;

const POPPINS_FONT_FAMILY = [
  'Poppins',
  '-apple-system',
  'BlinkMacSystemFont',
  '"Segoe UI"',
  'Roboto',
  '"Helvetica Neue"',
  'Arial',
  'sans-serif',
].join(',');

const TOKENS_TECHNE: TokensTema = {
  primary: { main: '#7836FC', contrastText: '#FFFFFF' },
  secondary: { main: '#3661FC' },
  // Quick 012 / DD-3: tints explícitos — ver nota em TOKENS_CORPORATE.
  success: { main: '#0F6E56', light: '#E2EEEB' },
  warning: { main: '#8A5200', light: '#F1EAE0' },
  error: { main: '#A32D2D', light: '#F4E6E6' },
  // `info` é #0A7AB0, e não o azul institucional #0C8DCE: este rende 3.67:1 contra
  // background.paper (#FFFFFF), abaixo do mínimo AA de 4.5:1 exigido por spec.md AC4
  // (TEMAF-03). #0A7AB0 é a variante mais próxima em matiz e atinge 4.75:1.
  // Não é desvio: design.md já traz #0A7AB0 na tabela de valores por tema (QA-2).
  info: { main: '#0A7AB0', light: '#E2EFF6' },
  background: { default: '#EFF2F7', paper: '#FFFFFF' },
  divider: '#D8DCE6',
  charts: [...TECHNE_CHARTS],
  chrome: {
    bg: '#20284E',
    fg: '#B8C0D4',
    fgAtivo: '#FFFFFF',
    selecionado: '#4C57D6',
  },
  typography: {
    fontFamily: POPPINS_FONT_FAMILY,
  },
};

export const TEMA_IDS = ['classico', 'corporate', 'soft', 'indigo', 'techne'] as const;
export type TemaId = (typeof TEMA_IDS)[number];
export const TEMA_PADRAO: TemaId = 'techne';

export interface TemaDefinicao {
  id: TemaId;
  nome: string;
  descricao: string;
  amostras: string[];
  criar: () => Theme;
}

function criarClassico(): Theme {
  return createTheme({
    palette: {
      primary: {
        main: '#1976d2',
      },
      secondary: {
        main: '#dc004e',
      },
      info: {
        light: '#e3f2fd',
        main: '#1976d2',
      },
      success: {
        light: '#e8f5e8',
        main: '#2e7d32',
      },
      warning: {
        light: '#fff3e0',
        // Quick 012: era #f57c00, que rende 2.70:1 contra background.paper e 2.47:1
        // contra o próprio tint do avatar. Por decisão do usuário (2026-08-04) o
        // `classico` também é ajustado por acessibilidade — DD-4 revisado, QA-1
        // encerrada. #b05900 mantém a matiz laranja e rende 4.91:1 / 4.48:1.
        main: '#b05900',
      },
      error: {
        light: '#ffebee',
        main: '#c62828',
      },
      background: {
        default: '#f8f9fa',
      },
      divider: '#e9ecef',
      charts: [...CLASSICO_CHARTS],
      chrome: {
        bg: '#ffffff',
        fg: 'rgba(0, 0, 0, 0.87)',
        fgAtivo: '#0d47a1',
        selecionado: '#90caf9',
      },
    },
    typography: {
      fontFamily: [
        '-apple-system',
        'BlinkMacSystemFont',
        '"Segoe UI"',
        'Roboto',
        '"Helvetica Neue"',
        'Arial',
        'sans-serif',
        '"Apple Color Emoji"',
        '"Segoe UI Emoji"',
        '"Segoe UI Symbol"',
      ].join(','),
      // TEMAF-08: a escala é idêntica nos cinco temas; o `classico` preserva as cores, não a escala.
      ...ESCALA_TIPOGRAFICA,
    },
    components: {
      MuiButton: {
        styleOverrides: {
          root: {
            textTransform: 'none',
          },
        },
      },
    },
  });
}

function criarCorporate(): Theme {
  return montarTema(TOKENS_CORPORATE);
}

function criarSoft(): Theme {
  return montarTema(TOKENS_SOFT);
}

function criarIndigo(): Theme {
  return montarTema(TOKENS_INDIGO);
}

function criarTechne(): Theme {
  return montarTema(TOKENS_TECHNE);
}

export const TEMAS: readonly TemaDefinicao[] = [
  {
    id: 'classico',
    nome: 'Clássico',
    descricao: 'Tema azul MUI padrão, idêntico à aparência original do sistema.',
    amostras: ['#1976d2', '#dc004e', '#f8f9fa'],
    criar: criarClassico,
  },
  {
    id: 'corporate',
    nome: 'Corporate slate',
    descricao: 'Sidebar grafite, conteúdo claro e azul institucional. Denso e sóbrio.',
    amostras: ['#0F172A', '#3B82F6', '#0F6E56'],
    criar: criarCorporate,
  },
  {
    id: 'soft',
    nome: 'Soft neutral',
    descricao: 'Fundo quente off-white, verde como acento. Arejado e confortável para leitura.',
    amostras: ['#2C2C2A', '#1D9E75', '#D85A30'],
    criar: criarSoft,
  },
  {
    id: 'indigo',
    nome: 'Indigo dark',
    descricao: 'Tema escuro com acento índigo. Alto contraste, ideal para dashboards.',
    amostras: ['#12121A', '#7F77DD', '#5DCAA5'],
    criar: criarIndigo,
  },
  {
    id: 'techne',
    nome: 'Techne brand',
    descricao:
      'Identidade Techne: violeta institucional, chrome marinho e tipografia Poppins. Alinhado aos relatórios PDF.',
    amostras: ['#7836FC', '#20284E', '#EFF2F7'],
    criar: criarTechne,
  },
];

export function isTemaId(value: unknown): value is TemaId {
  return typeof value === 'string' && (TEMA_IDS as readonly string[]).includes(value);
}

export function criarTema(id: TemaId): Theme {
  const definicao = TEMAS.find((tema) => tema.id === id);
  if (!definicao) {
    throw new Error(`Tema desconhecido: ${id}`);
  }
  return definicao.criar();
}
