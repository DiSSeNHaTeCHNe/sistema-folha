import { createTheme, type Theme } from '@mui/material/styles';

export interface ChromeTokens {
  bg: string;
  fg: string;
  fgAtivo: string;
  selecionado: string;
}

/** Papel semântico (success/warning/error/info) declarado pelo tema. */
export interface TokensPapelSemantico {
  main: string;
  light?: string;
  contrastText?: string;
}

export interface TokensTema {
  mode?: 'light' | 'dark';
  primary: { main: string; contrastText?: string };
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
    typography: tokens.typography,
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
