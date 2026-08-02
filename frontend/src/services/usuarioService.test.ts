import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest';
import { http, HttpResponse } from 'msw';
import usuarioService from './usuarioService';
import { createAuthMswServer } from '../test/mswServer';
import { API_BASE_URL } from '../test/handlers/authHandlers';

const server = createAuthMswServer();

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

const sampleUsuario = {
  id: 1,
  login: 'admin',
  nome: 'Admin',
  permissoes: ['ADMIN'],
};

describe('usuarioService', () => {
  it('lists users with optional filters', async () => {
    server.use(
      http.get(`${API_BASE_URL}/usuarios`, ({ request }) => {
        const params = new URL(request.url).searchParams;
        expect(params.get('login')).toBe('admin');
        return HttpResponse.json([sampleUsuario]);
      }),
    );

    const users = await usuarioService.listar({ login: 'admin' });
    expect(users[0].login).toBe('admin');
  });

  it('lists users with nome and funcionarioId filters', async () => {
    server.use(
      http.get(`${API_BASE_URL}/usuarios`, ({ request }) => {
        const params = new URL(request.url).searchParams;
        expect(params.get('nome')).toBe('Maria');
        expect(params.get('funcionarioId')).toBe('10');
        return HttpResponse.json([sampleUsuario]);
      }),
    );

    await usuarioService.listar({ nome: 'Maria', funcionarioId: 10 });
  });

  it('lists users without filters', async () => {
    server.use(
      http.get(`${API_BASE_URL}/usuarios`, ({ request }) => {
        expect(new URL(request.url).search).toBe('');
        return HttpResponse.json([sampleUsuario]);
      }),
    );

    await usuarioService.listar();
  });

  it('fetches user by id and login', async () => {
    server.use(
      http.get(`${API_BASE_URL}/usuarios/1`, () => HttpResponse.json(sampleUsuario)),
      http.get(`${API_BASE_URL}/usuarios/login/admin`, () => HttpResponse.json(sampleUsuario)),
    );

    expect((await usuarioService.buscarPorId(1)).id).toBe(1);
    expect((await usuarioService.buscarPorLogin('admin')).login).toBe('admin');
  });

  it('creates, updates and deletes users', async () => {
    server.use(
      http.post(`${API_BASE_URL}/usuarios`, () => HttpResponse.json(sampleUsuario)),
      http.put(`${API_BASE_URL}/usuarios/1`, () => HttpResponse.json(sampleUsuario)),
      http.delete(`${API_BASE_URL}/usuarios/1`, () => new HttpResponse(null, { status: 204 })),
    );

    await usuarioService.criar({
      login: 'admin',
      nome: 'Admin',
      permissoes: ['ADMIN'],
    });
    await usuarioService.atualizar(1, {
      login: 'admin',
      nome: 'Admin Updated',
      permissoes: ['ADMIN'],
    });
    await expect(usuarioService.excluir(1)).resolves.toBeUndefined();
  });

  it('changes password and lists permissions and funcionarios', async () => {
    server.use(
      http.post(`${API_BASE_URL}/usuarios/1/alterar-senha`, ({ request }) => {
        const params = new URL(request.url).searchParams;
        expect(params.get('senhaAtual')).toBe('old');
        expect(params.get('novaSenha')).toBe('newsecret');
        return HttpResponse.json({});
      }),
      http.get(`${API_BASE_URL}/usuarios/permissoes`, () => HttpResponse.json(['ADMIN', 'USER'])),
      http.get(`${API_BASE_URL}/funcionarios`, () =>
        HttpResponse.json([{ id: 10, nome: 'João', cpf: '123' }]),
      ),
    );

    await usuarioService.alterarSenha(1, 'old', 'newsecret');
    expect(await usuarioService.listarPermissoes()).toEqual(['ADMIN', 'USER']);
    expect(await usuarioService.listarFuncionarios()).toEqual([
      { id: 10, nome: 'João', cpf: '123' },
    ]);
  });
});
