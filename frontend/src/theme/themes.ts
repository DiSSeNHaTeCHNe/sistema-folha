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

export const TEMA_IDS = ['classico', 'corporate'] as const;
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
