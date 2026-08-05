import { describe, expect, it } from 'vitest';
import { buildWidgetQueryParams } from './dashboardWidgetService';

describe('buildWidgetQueryParams CC/LN filter (DASHC-33)', () => {
  it('includes centroCustoId when configured', () => {
    expect(buildWidgetQueryParams({ centroCustoId: 5 }, '2026-06')).toEqual({
      competencia: '2026-06',
      centroCustoId: 5,
    });
  });

  it('includes linhaNegocioId when configured', () => {
    expect(buildWidgetQueryParams({ linhaNegocioId: 3 }, null)).toEqual({
      linhaNegocioId: 3,
    });
  });

  it('includes both CC and LN filters with competencia override', () => {
    expect(
      buildWidgetQueryParams(
        { centroCustoId: 2, linhaNegocioId: 7, competencia: '2025-12' },
        '2026-06',
      ),
    ).toEqual({
      competencia: '2025-12',
      centroCustoId: 2,
      linhaNegocioId: 7,
    });
  });

  it('omits CC/LN filters when not configured', () => {
    expect(buildWidgetQueryParams({ topN: 10 }, '2026-06')).toEqual({
      competencia: '2026-06',
      topN: 10,
    });
  });
});
