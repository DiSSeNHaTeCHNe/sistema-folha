import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest';
import { http, HttpResponse } from 'msw';
import { resumoFolhaPagamentoService } from './resumoFolhaPagamentoService';
import { createAuthMswServer } from '../test/mswServer';
import { API_BASE_URL } from '../test/handlers/authHandlers';

const server = createAuthMswServer();

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const sampleResumo = {
  id: 1,
  totalEmpregados: 5,
  totalEncargos: 100,
  totalPagamentos: 1000,
  totalDescontos: 200,
  totalLiquido: 800,
  totalBruto: 1000,
  totalCustoEmpresa: 1200,
  competenciaInicio: '2026-01-01',
  competenciaFim: '2026-01-31',
  dataImportacao: '2026-02-01',
  decimoTerceiro: false,
  ativo: true,
};

describe('resumoFolhaPagamentoService', () => {
  it('lists resumos by year and optional month', async () => {
    server.use(
      http.get(`${API_BASE_URL}/resumo-folha-pagamento`, ({ request }) => {
        const params = new URL(request.url).searchParams;
        expect(params.get('ano')).toBe('2026');
        expect(params.get('mes')).toBe('1');
        return HttpResponse.json([sampleResumo]);
      }),
    );

    const result = await resumoFolhaPagamentoService.listarPorAno(2026, 1);

    expect(result).toHaveLength(1);
    expect(result[0].totalEmpregados).toBe(5);
  });

  it('lists resumos by year without month', async () => {
    server.use(
      http.get(`${API_BASE_URL}/resumo-folha-pagamento`, ({ request }) => {
        expect(new URL(request.url).searchParams.get('mes')).toBeNull();
        return HttpResponse.json([]);
      }),
    );

    await resumoFolhaPagamentoService.listarPorAno(2026);
  });

  it('searches by period and competencia', async () => {
    server.use(
      http.get(`${API_BASE_URL}/resumo-folha-pagamento/periodo`, ({ request }) => {
        const params = new URL(request.url).searchParams;
        expect(params.get('dataInicio')).toBe('2026-01-01');
        expect(params.get('dataFim')).toBe('2026-01-31');
        return HttpResponse.json([sampleResumo]);
      }),
      http.get(`${API_BASE_URL}/resumo-folha-pagamento/competencia`, ({ request }) => {
        const params = new URL(request.url).searchParams;
        expect(params.get('competenciaInicio')).toBe('2026-01-01');
        expect(params.get('competenciaFim')).toBe('2026-01-31');
        return HttpResponse.json(sampleResumo);
      }),
    );

    expect(await resumoFolhaPagamentoService.buscarPorPeriodo('2026-01-01', '2026-01-31')).toHaveLength(1);
    expect(
      await resumoFolhaPagamentoService.buscarPorCompetencia('2026-01-01', '2026-01-31'),
    ).toEqual(sampleResumo);
  });

  it('lists most recent resumos', async () => {
    server.use(
      http.get(`${API_BASE_URL}/resumo-folha-pagamento/latest`, () =>
        HttpResponse.json([sampleResumo]),
      ),
    );

    const result = await resumoFolhaPagamentoService.listarMaisRecentes();

    expect(result).toHaveLength(1);
    expect(result[0].id).toBe(1);
  });
});
