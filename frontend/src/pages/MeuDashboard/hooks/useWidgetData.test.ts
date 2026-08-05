import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { createElement, type ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useWidgetData } from './useWidgetData';
import { getWidgetData } from '../../../services/dashboardWidgetService';
import type { WidgetData } from '../types';

vi.mock('../../../services/dashboardWidgetService', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../../services/dashboardWidgetService')>();
  return {
    ...actual,
    getWidgetData: vi.fn(),
  };
});

const mockData: WidgetData = {
  widgetId: 'kpi-total-funcionarios',
  competencia: '2026-06',
  semDados: false,
  totalFuncionarios: 42,
};

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return function Wrapper({ children }: { children: ReactNode }) {
    return createElement(QueryClientProvider, { client: queryClient }, children);
  };
}

describe('useWidgetData', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getWidgetData).mockResolvedValue(mockData);
  });

  it('fetches data for visible widget with global competencia', async () => {
    const { result } = renderHook(
      () => useWidgetData('kpi-total-funcionarios', 'inst-1', {}, '2026-06'),
      { wrapper: createWrapper() },
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(getWidgetData).toHaveBeenCalledWith('kpi-total-funcionarios', { competencia: '2026-06' });
    expect(result.current.data?.totalFuncionarios).toBe(42);
  });

  it('does not fetch when disabled (widget absent from layout)', async () => {
    const { result } = renderHook(
      () => useWidgetData('kpi-total-funcionarios', 'inst-1', {}, '2026-06', { enabled: false }),
      { wrapper: createWrapper() },
    );

    await waitFor(() => expect(result.current.fetchStatus).toBe('idle'));
    expect(getWidgetData).not.toHaveBeenCalled();
  });

  it('uses config competencia override over global', async () => {
    const { result } = renderHook(
      () =>
        useWidgetData(
          'kpi-total-funcionarios',
          'inst-1',
          { competencia: '2025-01' },
          '2026-06',
        ),
      { wrapper: createWrapper() },
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(getWidgetData).toHaveBeenCalledWith('kpi-total-funcionarios', { competencia: '2025-01' });
  });

  it('exposes error state for isolated widget handling', async () => {
    vi.mocked(getWidgetData).mockRejectedValue(new Error('network'));
    const { result } = renderHook(
      () => useWidgetData('kpi-total-funcionarios', 'inst-1', {}, null),
      { wrapper: createWrapper() },
    );

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.error).toBeTruthy();
  });

  it('passes CC/LN filter config to widget data API (DASHC-33)', async () => {
    const { result } = renderHook(
      () =>
        useWidgetData(
          'grafico-funcionarios-por-cc',
          'inst-1',
          { centroCustoId: 2, linhaNegocioId: 5 },
          '2026-06',
        ),
      { wrapper: createWrapper() },
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(getWidgetData).toHaveBeenCalledWith('grafico-funcionarios-por-cc', {
      competencia: '2026-06',
      centroCustoId: 2,
      linhaNegocioId: 5,
    });
  });

  it('refetches when competencia global changes', async () => {
    const { result, rerender } = renderHook(
      ({ competencia }: { competencia: string | null }) =>
        useWidgetData('kpi-total-funcionarios', 'inst-1', {}, competencia),
      {
        wrapper: createWrapper(),
        initialProps: { competencia: '2026-05' },
      },
    );

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(getWidgetData).toHaveBeenCalledTimes(1);

    rerender({ competencia: '2026-06' });
    await waitFor(() => expect(getWidgetData).toHaveBeenCalledTimes(2));
    expect(getWidgetData).toHaveBeenLastCalledWith('kpi-total-funcionarios', { competencia: '2026-06' });
  });
});
