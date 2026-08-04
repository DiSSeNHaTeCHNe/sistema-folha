import { act, createContext, useContext, type ReactElement, type ReactNode } from 'react';
import { render, type RenderOptions } from '@testing-library/react';
import { MemoryRouter, type MemoryRouterProps } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { criarTema, TEMA_PADRAO, type TemaId } from '../theme/themes';
import type { Theme } from '@mui/material/styles';
import type { AcessoUsuario, LoginRequest, Usuario } from '../types';

const temaCache = new Map<TemaId, Theme>();

function temaParaTestes(temaId: TemaId): Theme {
  const cached = temaCache.get(temaId);
  if (cached) {
    return cached;
  }
  const theme = criarTema(temaId);
  temaCache.set(temaId, theme);
  return theme;
}

export interface MockAuthContextValue {
  user: Usuario | null;
  loading: boolean;
  login: (data: LoginRequest) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
  acessoUsuario: AcessoUsuario | null;
  podeAcessarCentroCusto: (centroCustoId: number) => boolean;
}

export const defaultMockAuth: MockAuthContextValue = {
  user: null,
  loading: false,
  login: async () => {},
  logout: () => {},
  isAuthenticated: false,
  acessoUsuario: null,
  podeAcessarCentroCusto: () => false,
};

const TestAuthContext = createContext<MockAuthContextValue>(defaultMockAuth);

export function TestAuthProvider({
  children,
  value = defaultMockAuth,
}: {
  children: ReactNode;
  value?: MockAuthContextValue;
}) {
  return <TestAuthContext.Provider value={value}>{children}</TestAuthContext.Provider>;
}

/** Use in vi.mock('../../contexts/AuthContext') so page tests share the harness auth shape. */
export function useTestAuth() {
  return useContext(TestAuthContext);
}

export interface RenderWithProvidersOptions extends Omit<RenderOptions, 'wrapper'> {
  route?: string;
  routerProps?: MemoryRouterProps;
  authContext?: Partial<MockAuthContextValue>;
  temaId?: TemaId;
}

export function renderWithProviders(
  ui: ReactElement,
  {
    route = '/',
    routerProps,
    authContext,
    temaId = TEMA_PADRAO,
    ...renderOptions
  }: RenderWithProvidersOptions = {},
) {
  const authValue: MockAuthContextValue = {
    ...defaultMockAuth,
    ...authContext,
  };
  const theme = temaParaTestes(temaId);

  function Wrapper({ children }: { children: ReactNode }) {
    return (
      <MemoryRouter initialEntries={[route]} {...routerProps}>
        <ThemeProvider theme={theme}>
          <TestAuthProvider value={authValue}>{children}</TestAuthProvider>
        </ThemeProvider>
      </MemoryRouter>
    );
  }

  let rendered: ReturnType<typeof render>;
  act(() => {
    rendered = render(ui, { wrapper: Wrapper, ...renderOptions });
  });

  return {
    ...rendered!,
    authValue,
  };
}
