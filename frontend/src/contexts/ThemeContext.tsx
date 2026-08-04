import {
  createContext,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react';
import { CssBaseline, ThemeProvider } from '@mui/material';
import { criarTema, TEMAS, type TemaDefinicao, type TemaId } from '../theme/themes';
import { gravarTema, lerTemaSalvo } from '../theme/storage';

interface AppThemeContextData {
  temaId: TemaId;
  setTemaId: (id: TemaId) => void;
  temas: readonly TemaDefinicao[];
}

const AppThemeContext = createContext<AppThemeContextData | null>(null);

export function AppThemeProvider({
  children,
  initialTemaId,
}: {
  children: ReactNode;
  initialTemaId?: TemaId;
}) {
  const [temaId, setTemaIdState] = useState<TemaId>(
    () => initialTemaId ?? lerTemaSalvo(),
  );

  const theme = useMemo(() => criarTema(temaId), [temaId]);

  const setTemaId = (id: TemaId) => {
    gravarTema(id);
    setTemaIdState(id);
  };

  return (
    <AppThemeContext.Provider value={{ temaId, setTemaId, temas: TEMAS }}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        {children}
      </ThemeProvider>
    </AppThemeContext.Provider>
  );
}

export function useAppTheme(): AppThemeContextData {
  const context = useContext(AppThemeContext);
  if (!context) {
    throw new Error('useAppTheme must be used within an AppThemeProvider');
  }
  return context;
}
