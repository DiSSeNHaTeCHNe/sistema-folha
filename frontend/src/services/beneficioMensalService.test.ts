import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest';
import { http, HttpResponse } from 'msw';
import { beneficioMensalService } from './beneficioMensalService';
import { createAuthMswServer } from '../test/mswServer';
import { API_BASE_URL } from '../test/handlers/authHandlers';

const server = createAuthMswServer();

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const sampleCompetencia = {
  competenciaInicio: '2026-01-01',
  competenciaFim: '2026-01-31',
  totalFuncionarios: 3,
  totalBeneficios: 1500,
  qtdLancamentos: 5,
};

const sampleBeneficio = {
  id: 1,
  funcionarioId: 10,
  funcionarioNome: 'Maria',
  competenciaInicio: '2026-01-01',
  competenciaFim: '2026-01-31',
  valor: 500,
  tipoBeneficioCodigo: 'VR',
  tipoBeneficioDescricao: 'Vale Refeição',
  observacao: null,
};

describe('beneficioMensalService', () => {
  it('lists competencias by year and optional month', async () => {
    server.use(
      http.get(`${API_BASE_URL}/beneficio-mensal/competencias`, ({ request }) => {
        const params = new URL(request.url).searchParams;
        expect(params.get('ano')).toBe('2026');
        expect(params.get('mes')).toBe('1');
        return HttpResponse.json([sampleCompetencia]);
      }),
    );

    const result = await beneficioMensalService.listarCompetencias(2026, 1);

    expect(result).toHaveLength(1);
    expect(result[0].totalFuncionarios).toBe(3);
  });

  it('lists competencias without month filter', async () => {
    server.use(
      http.get(`${API_BASE_URL}/beneficio-mensal/competencias`, ({ request }) => {
        const params = new URL(request.url).searchParams;
        expect(params.get('ano')).toBe('2026');
        expect(params.get('mes')).toBeNull();
        return HttpResponse.json([]);
      }),
    );

    await beneficioMensalService.listarCompetencias(2026);
  });

  it('lists, summarizes and fetches by funcionario', async () => {
    const params = { competenciaInicio: '2026-01-01', competenciaFim: '2026-01-31' };

    server.use(
      http.get(`${API_BASE_URL}/beneficio-mensal`, ({ request }) => {
        expect(new URL(request.url).searchParams.get('competenciaInicio')).toBe('2026-01-01');
        return HttpResponse.json([sampleBeneficio]);
      }),
      http.get(`${API_BASE_URL}/beneficio-mensal/resumo`, () =>
        HttpResponse.json([
          {
            funcionarioId: 10,
            funcionarioNome: 'Maria',
            totalBeneficios: 500,
            qtdLancamentos: 1,
          },
        ]),
      ),
      http.get(`${API_BASE_URL}/beneficio-mensal/funcionario/10`, () =>
        HttpResponse.json([sampleBeneficio]),
      ),
    );

    expect(await beneficioMensalService.listar(params)).toHaveLength(1);
    expect(await beneficioMensalService.resumo(params)).toHaveLength(1);
    expect(await beneficioMensalService.porFuncionario(10, params)).toHaveLength(1);
  });

  it('creates, removes and imports beneficios', async () => {
    server.use(
      http.post(`${API_BASE_URL}/beneficio-mensal`, async ({ request }) => {
        const body = await request.json() as Record<string, unknown>;
        expect(body.funcionarioId).toBe(10);
        return HttpResponse.json(sampleBeneficio);
      }),
      http.delete(`${API_BASE_URL}/beneficio-mensal/1`, () => new HttpResponse(null, { status: 204 })),
      http.post(`${API_BASE_URL}/importacao/beneficios-mensais`, () =>
        HttpResponse.json({ totalImportados: 2, totalErros: 0, erros: [] }),
      ),
    );

    const created = await beneficioMensalService.criar({
      funcionarioId: 10,
      tipoBeneficioId: 1,
      competenciaInicio: '2026-01-01',
      competenciaFim: '2026-01-31',
      valor: 500,
    });
    expect(created.id).toBe(1);

    await expect(beneficioMensalService.remover(1)).resolves.toBeUndefined();

    const file = new File(['csv'], 'beneficios.csv', { type: 'text/csv' });
    const importResult = await beneficioMensalService.importar(
      file,
      '2026-01-01',
      '2026-01-31',
      true,
    );
    expect(importResult.totalImportados).toBe(2);
  });
});
