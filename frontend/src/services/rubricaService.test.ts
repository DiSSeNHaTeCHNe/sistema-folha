import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest';
import { http, HttpResponse } from 'msw';
import { rubricaService } from './rubricaService';
import { createAuthMswServer } from '../test/mswServer';
import { API_BASE_URL } from '../test/handlers/authHandlers';

const server = createAuthMswServer();

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const sampleRubrica = {
  id: 1,
  codigo: '001',
  descricao: 'Salário',
  tipo: 'PROVENTO' as const,
  tipoRubricaDescricao: 'PROVENTO',
  ativo: true,
  operadorBruto: 1,
  operadorLiquido: 1,
  operadorCusto: 1,
};

describe('rubricaService', () => {
  it('lists rubricas with default ATIVO status filter', async () => {
    server.use(
      http.get(`${API_BASE_URL}/rubricas`, ({ request }) => {
        expect(new URL(request.url).searchParams.get('status')).toBe('ATIVO');
        return HttpResponse.json([sampleRubrica]);
      }),
    );

    const result = await rubricaService.listar();

    expect(result).toHaveLength(1);
    expect(result[0].codigo).toBe('001');
  });

  it('lists rubricas with custom filters', async () => {
    server.use(
      http.get(`${API_BASE_URL}/rubricas`, ({ request }) => {
        const params = new URL(request.url).searchParams;
        expect(params.get('codigo')).toBe('001');
        expect(params.get('descricao')).toBe('Sal');
        expect(params.get('status')).toBe('TODOS');
        return HttpResponse.json([sampleRubrica]);
      }),
    );

    await rubricaService.listar({ codigo: '001', descricao: 'Sal', status: 'TODOS' });
  });

  it('fetches a rubrica by id', async () => {
    server.use(
      http.get(`${API_BASE_URL}/rubricas/1`, () => HttpResponse.json(sampleRubrica)),
    );

    const result = await rubricaService.buscarPorId(1);

    expect(result.id).toBe(1);
  });

  it('maps rubrica tipo when tipoRubricaDescricao is absent', async () => {
    server.use(
      http.get(`${API_BASE_URL}/rubricas`, () =>
        HttpResponse.json([
          {
            id: 2,
            codigo: '002',
            descricao: 'Desconto',
            tipo: 'DESCONTO',
            ativo: true,
          },
        ]),
      ),
    );

    const result = await rubricaService.listar();
    expect(result[0].tipo).toBe('DESCONTO');
  });

  it('creates a rubrica with tipoRubricaDescricao payload', async () => {
    server.use(
      http.post(`${API_BASE_URL}/rubricas`, async ({ request }) => {
        const body = await request.json() as Record<string, unknown>;
        expect(body.tipoRubricaDescricao).toBe('PROVENTO');
        return HttpResponse.json({ ...sampleRubrica, id: 2 });
      }),
    );

    const created = await rubricaService.cadastrar({
      codigo: '002',
      descricao: 'Novo',
      tipo: 'PROVENTO',
      operadorBruto: 1,
      operadorLiquido: 1,
      operadorCusto: 1,
    });

    expect(created.id).toBe(2);
  });

  it('updates and removes a rubrica', async () => {
    server.use(
      http.put(`${API_BASE_URL}/rubricas/1`, () => HttpResponse.json(sampleRubrica)),
      http.delete(`${API_BASE_URL}/rubricas/1`, () => new HttpResponse(null, { status: 204 })),
    );

    const updated = await rubricaService.atualizar(1, {
      codigo: '001',
      descricao: 'Atualizado',
      tipo: 'PROVENTO',
      operadorBruto: 1,
      operadorLiquido: 1,
      operadorCusto: 1,
    });
    expect(updated.descricao).toBe('Salário');

    await expect(rubricaService.remover(1)).resolves.toBeUndefined();
  });
});
