import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest';
import { http, HttpResponse } from 'msw';
import { folhaPagamentoService } from './folhaPagamentoService';
import { createAuthMswServer } from '../test/mswServer';
import { API_BASE_URL } from '../test/handlers/authHandlers';

const server = createAuthMswServer();

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const sampleFolha = {
  id: 1,
  funcionarioId: 10,
  funcionarioNome: 'João',
  rubricaId: 1,
  rubricaCodigo: '001',
  rubricaDescricao: 'Salário',
  rubricaTipo: 'PROVENTO',
  dataInicio: '2026-01-01',
  dataFim: '2026-01-31',
  valor: 1000,
  quantidade: 1,
  baseCalculo: 1000,
  ativo: true,
};

describe('folhaPagamentoService', () => {
  it('lists folha pagamento entries', async () => {
    server.use(
      http.get(`${API_BASE_URL}/folha-pagamento`, () => HttpResponse.json([sampleFolha])),
    );

    const rows = await folhaPagamentoService.listar();
    expect(rows[0].funcionarioNome).toBe('João');
  });

  it('queries folha by funcionario, centro de custo and linha de negocio', async () => {
    server.use(
      http.get(`${API_BASE_URL}/folha-pagamento/funcionario/10`, () => HttpResponse.json([sampleFolha])),
      http.get(`${API_BASE_URL}/folha-pagamento/centro-custo/CC1`, () => HttpResponse.json([sampleFolha])),
      http.get(`${API_BASE_URL}/folha-pagamento/linha-negocio/LN1`, () => HttpResponse.json([sampleFolha])),
    );

    expect(await folhaPagamentoService.buscarPorFuncionario(10, '2026-01-01', '2026-01-31')).toHaveLength(1);
    expect(await folhaPagamentoService.buscarPorCentroCusto('CC1', '2026-01-01', '2026-01-31')).toHaveLength(1);
    expect(await folhaPagamentoService.buscarPorLinhaNegocio('LN1', '2026-01-01', '2026-01-31')).toHaveLength(1);
  });

  it('returns null when ficha lookup fails', async () => {
    server.use(
      http.get(`${API_BASE_URL}/folha-pagamento/fichas/por-funcionario`, () =>
        HttpResponse.json({ message: 'not found' }, { status: 404 }),
      ),
    );

    const fichaId = await folhaPagamentoService.buscarFichaPorFuncionario(10, '2026-01-01', '2026-01-31');
    expect(fichaId).toBeNull();
  });

  it('creates, updates, removes and processes competencia', async () => {
    server.use(
      http.post(`${API_BASE_URL}/folha-pagamento`, () => HttpResponse.json(sampleFolha)),
      http.put(`${API_BASE_URL}/folha-pagamento/1`, () => HttpResponse.json(sampleFolha)),
      http.delete(`${API_BASE_URL}/folha-pagamento/1`, () => new HttpResponse(null, { status: 204 })),
      http.post(`${API_BASE_URL}/folha-pagamento/processar`, () =>
        HttpResponse.json({ totalFichas: 1, totalLinhas: 5, totalFuncionarios: 1 }),
      ),
    );

    await folhaPagamentoService.criar(sampleFolha);
    await folhaPagamentoService.atualizar(1, { valor: 2000 });
    await folhaPagamentoService.remover(1);
    const result = await folhaPagamentoService.processarCompetencia({
      competenciaInicio: '2026-01-01',
      competenciaFim: '2026-01-31',
      decimoTerceiro: false,
      recalcularFerias: false,
    });
    expect(result.totalFichas).toBe(1);
  });
});
