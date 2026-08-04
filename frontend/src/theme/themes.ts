import { createTheme, type Theme } from '@mui/material/styles';
import { montarTema, type TokensTema } from './tokens';

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
export const TEMA_PADRAO: TemaId = 'classico';

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
      background: {
        default: '#f5f5f5',
      },
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
    amostras: ['#1976d2', '#dc004e', '#f5f5f5'],
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
