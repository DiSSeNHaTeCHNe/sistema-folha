import { describe, expect, it } from 'vitest';
import { podeAcessarWorkspace } from './workspaceAccess';
import type { AcessoUsuario } from '../types';

const baseAcesso: AcessoUsuario = {
  temFuncionarioVinculado: true,
  temNoOrganograma: true,
  acessoTotal: false,
  centrosCustoIds: [1, 2],
  quantidadeCentrosAcessiveis: 2,
};

describe('podeAcessarWorkspace', () => {
  it('returns false when acesso is null', () => {
    expect(podeAcessarWorkspace(null)).toBe(false);
  });

  it('returns false when acesso is undefined', () => {
    expect(podeAcessarWorkspace(undefined)).toBe(false);
  });

  it('returns true for acesso total', () => {
    expect(podeAcessarWorkspace({ ...baseAcesso, acessoTotal: true, centrosCustoIds: [] })).toBe(true);
  });

  it('returns false when motivoNegacao is set', () => {
    expect(podeAcessarWorkspace({ ...baseAcesso, motivoNegacao: 'SEM_FUNCIONARIO' })).toBe(false);
  });

  it('returns false without funcionario vinculado', () => {
    expect(podeAcessarWorkspace({ ...baseAcesso, temFuncionarioVinculado: false })).toBe(false);
  });

  it('returns false without no organograma', () => {
    expect(podeAcessarWorkspace({ ...baseAcesso, temNoOrganograma: false })).toBe(false);
  });

  it('returns false when centros de custo are empty', () => {
    expect(
      podeAcessarWorkspace({ ...baseAcesso, centrosCustoIds: [], quantidadeCentrosAcessiveis: 0 }),
    ).toBe(false);
  });

  it('returns true for scoped user with centros', () => {
    expect(podeAcessarWorkspace(baseAcesso)).toBe(true);
  });
});
