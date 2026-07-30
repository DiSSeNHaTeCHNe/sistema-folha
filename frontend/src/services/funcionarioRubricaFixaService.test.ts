import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest';
import { http, HttpResponse } from 'msw';
import { funcionarioRubricaFixaService } from './funcionarioRubricaFixaService';
import { createAuthMswServer } from '../test/mswServer';
import { API_BASE_URL } from '../test/handlers/authHandlers';

const server = createAuthMswServer();

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const sampleFixa = {
  id: 1,
  funcionarioId: 10,
  funcionarioNome: 'Maria',
  rubricaId: 2,
  rubricaCodigo: '001',
  rubricaDescricao: 'Salário',
  valor: '1000.00',
  porcentagem: 100,
  vigenciaInicio: '2026-01-01',
  vigenciaFim: null,
  comentario: 'Teste',
};

describe('funcionarioRubricaFixaService', () => {
  it('lists all rubricas fixas without filters', async () => {
    server.use(
      http.get(`${API_BASE_URL}/funcionario-rubrica-fixa`, ({ request }) => {
        expect(new URL(request.url).search).toBe('');
        return HttpResponse.json([sampleFixa]);
      }),
    );

    const result = await funcionarioRubricaFixaService.listar();

    expect(result).toHaveLength(1);
    expect(result[0].funcionarioNome).toBe('Maria');
  });

  it('lists rubricas fixas with filters', async () => {
    server.use(
      http.get(`${API_BASE_URL}/funcionario-rubrica-fixa`, ({ request }) => {
        const params = new URL(request.url).searchParams;
        expect(params.get('funcionarioId')).toBe('10');
        expect(params.get('rubricaId')).toBe('2');
        return HttpResponse.json([sampleFixa]);
      }),
    );

    await funcionarioRubricaFixaService.listar({ funcionarioId: 10, rubricaId: 2 });
  });

  it('creates a rubrica fixa with normalized payload', async () => {
    server.use(
      http.post(`${API_BASE_URL}/funcionario-rubrica-fixa`, async ({ request }) => {
        const body = await request.json() as Record<string, unknown>;
        expect(body.valor).toBe('1500.50');
        expect(body.funcionarioId).toBe(10);
        expect(body.vigenciaFim).toBeNull();
        return HttpResponse.json({ ...sampleFixa, id: 2 });
      }),
    );

    const created = await funcionarioRubricaFixaService.criar({
      funcionarioId: 10,
      rubricaId: 2,
      valor: '1500,50',
      vigenciaInicio: '2026-01-01',
      vigenciaFim: '',
      comentario: '',
    });

    expect(created.id).toBe(2);
  });

  it('creates a global rubrica fixa without funcionarioId', async () => {
    server.use(
      http.post(`${API_BASE_URL}/funcionario-rubrica-fixa`, async ({ request }) => {
        const body = await request.json() as Record<string, unknown>;
        expect(body.funcionarioId).toBeUndefined();
        return HttpResponse.json({ ...sampleFixa, id: 3, funcionarioId: null });
      }),
    );

    await funcionarioRubricaFixaService.criar({
      funcionarioId: '',
      rubricaId: 2,
      valor: '',
      vigenciaInicio: '2026-01-01',
    });
  });

  it('updates and removes a rubrica fixa', async () => {
    server.use(
      http.put(`${API_BASE_URL}/funcionario-rubrica-fixa/1`, async ({ request }) => {
        const body = await request.json() as Record<string, unknown>;
        expect(body.comentario).toBe('Atualizado');
        return HttpResponse.json(sampleFixa);
      }),
      http.delete(`${API_BASE_URL}/funcionario-rubrica-fixa/1`, () =>
        new HttpResponse(null, { status: 204 }),
      ),
    );

    const updated = await funcionarioRubricaFixaService.atualizar(1, {
      rubricaId: 2,
      vigenciaInicio: '2026-01-01',
      comentario: 'Atualizado',
    });
    expect(updated.rubricaCodigo).toBe('001');

    await expect(funcionarioRubricaFixaService.remover(1)).resolves.toBeUndefined();
  });
});
