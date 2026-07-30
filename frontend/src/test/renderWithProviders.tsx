import { createContext, useContext, type ReactElement, type ReactNode } from 'react';
import { render, type RenderOptions } from '@testing-library/react';
import { MemoryRouter, type MemoryRouterProps } from 'react-router-dom';
import type { AcessoUsuario, LoginRequest, Usuario } from '../types';

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
}

export function renderWithProviders(
  ui: ReactElement,
  {
    route = '/',
    routerProps,
    authContext,
    ...renderOptions
  }: RenderWithProvidersOptions = {},
) {
  const authValue: MockAuthContextValue = {
    ...defaultMockAuth,
    ...authContext,
  };

  function Wrapper({ children }: { children: ReactNode }) {
    return (
      <MemoryRouter initialEntries={[route]} {...routerProps}>
        <TestAuthProvider value={authValue}>{children}</TestAuthProvider>
      </MemoryRouter>
    );
  }

  return {
    ...render(ui, { wrapper: Wrapper, ...renderOptions }),
    authValue,
  };
}
