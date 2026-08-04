import { createTheme, type Theme } from '@mui/material/styles';

export interface ChromeTokens {
  bg: string;
  fg: string;
  fgAtivo: string;
  selecionado: string;
}

/** Papel de cor (primary/success/warning/error/info) declarado pelo tema. */
export interface TokensPapelSemantico {
  main: string;
  light?: string;
  contrastText?: string;
}

export interface TokensTema {
  mode?: 'light' | 'dark';
  /**
   * Quick 013: `light` passou a fazer parte do contrato de `primary`. Sem ele o MUI
   * deriva `lighten(main, 0.2)`, um meio-tom que rende no máximo ~1,4:1 no par
   * `primary.main × primary.light` — abaixo dos 3:1 do WCAG 1.4.11.
   */
  primary: TokensPapelSemantico;
  secondary: { main: string };
  success?: TokensPapelSemantico;
  warning?: TokensPapelSemantico;
  error?: TokensPapelSemantico;
  info?: TokensPapelSemantico;
  background: { default: string; paper?: string };
  text?: { primary?: string; secondary?: string };
  divider?: string;
  charts: string[];
  chrome: ChromeTokens;
  typography?: { fontFamily?: string };
}

/**
 * Escala tipográfica dos mockups (spec.md, Nota de escala: mockup × 1,415).
 * Compartilhada por todos os temas (DD-1): um tema muda cor e fonte, não hierarquia.
 * Demais variantes ficam no default do MUI (DD-5).
 */
export const ESCALA_TIPOGRAFICA = {
  h3: { fontSize: '1.6875rem', fontWeight: 600 },
  h4: { fontSize: '1.5rem', fontWeight: 600 },
  h6: { fontSize: '1rem', fontWeight: 600 },
} as const;

export function montarTema(tokens: TokensTema): Theme {
  return createTheme({
    palette: {
      mode: tokens.mode ?? 'light',
      primary: tokens.primary,
      secondary: tokens.secondary,
      ...(tokens.success ? { success: tokens.success } : {}),
      ...(tokens.warning ? { warning: tokens.warning } : {}),
      ...(tokens.error ? { error: tokens.error } : {}),
      ...(tokens.info ? { info: tokens.info } : {}),
      background: {
        default: tokens.background.default,
        paper: tokens.background.paper ?? '#ffffff',
      },
      ...(tokens.text ? { text: tokens.text } : {}),
      ...(tokens.divider ? { divider: tokens.divider } : {}),
      charts: tokens.charts,
      chrome: tokens.chrome,
    },
    typography: { ...tokens.typography, ...ESCALA_TIPOGRAFICA },
    components: {
      MuiButton: {
        styleOverrides: {
          root: {
            textTransform: 'none',
          },
        },
      },
      MuiAppBar: {
        styleOverrides: {
          root: {
            backgroundColor: tokens.chrome.bg,
            color: tokens.chrome.fg,
          },
        },
      },
      MuiDrawer: {
        styleOverrides: {
          paper: {
            backgroundColor: tokens.chrome.bg,
            color: tokens.chrome.fg,
          },
        },
      },
      MuiListItemButton: {
        styleOverrides: {
          root: {
            '&.Mui-selected': {
              backgroundColor: tokens.chrome.selecionado,
              color: tokens.chrome.fgAtivo,
              '&:hover': {
                backgroundColor: tokens.chrome.selecionado,
              },
            },
          },
        },
      },
      MuiCard: {
        styleOverrides: {
          root: {
            borderRadius: 8,
          },
        },
      },
      MuiTableCell: {
        styleOverrides: {
          head: {
            fontWeight: 600,
          },
        },
      },
    },
  });
}
