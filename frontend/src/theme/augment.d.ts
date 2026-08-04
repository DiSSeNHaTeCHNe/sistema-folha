declare module '@mui/material/styles' {
  interface Palette {
    charts: string[];
    chrome: {
      bg: string;
      fg: string;
      fgAtivo: string;
      selecionado: string;
    };
  }

  interface PaletteOptions {
    charts?: string[];
    chrome?: {
      bg: string;
      fg: string;
      fgAtivo: string;
      selecionado: string;
    };
  }
}

export {};
