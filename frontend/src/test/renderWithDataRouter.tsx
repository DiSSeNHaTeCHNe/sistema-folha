import { act, type ReactElement, type ReactNode } from 'react';
import { render, type RenderOptions } from '@testing-library/react';
import {
  createMemoryRouter,
  RouterProvider,
  type RouteObject,
  type MemoryRouterProps,
} from 'react-router-dom';
import { ThemeProvider } from '@mui/material';
import { criarTema, TEMA_PADRAO, type TemaId } from '../theme/themes';
import type { Theme } from '@mui/material/styles';
import {
  TestAuthProvider,
  defaultMockAuth,
  type MockAuthContextValue,
} from './renderWithProviders';

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

export interface RenderWithDataRouterOptions extends Omit<RenderOptions, 'wrapper'> {
  routes?: RouteObject[];
  route?: string;
  initialEntries?: MemoryRouterProps['initialEntries'];
  authContext?: Partial<MockAuthContextValue>;
  temaId?: TemaId;
}

export function renderWithDataRouter(
  ui: ReactElement,
  {
    routes,
    route = '/',
    initialEntries,
    authContext,
    temaId = TEMA_PADRAO,
    ...renderOptions
  }: RenderWithDataRouterOptions = {},
) {
  const authValue: MockAuthContextValue = {
    ...defaultMockAuth,
    ...authContext,
  };
  const theme = temaParaTestes(temaId);
  const routeTree = routes ?? [{ path: route, element: ui }];
  const entries = initialEntries ?? [route];
  const router = createMemoryRouter(routeTree, { initialEntries: entries });

  function Wrapper({ children }: { children: ReactNode }) {
    return (
      <ThemeProvider theme={theme}>
        <TestAuthProvider value={authValue}>{children}</TestAuthProvider>
      </ThemeProvider>
    );
  }

  let rendered: ReturnType<typeof render>;
  act(() => {
    rendered = render(<RouterProvider router={router} />, { wrapper: Wrapper, ...renderOptions });
  });

  return {
    ...rendered!,
    authValue,
    router,
  };
}
