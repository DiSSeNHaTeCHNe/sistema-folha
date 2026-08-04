import { describe, expect, it } from 'vitest';
import { podeAcessarMeuDashboard } from './dashboardAccess';
import type { AcessoUsuario } from '../types';

const baseAcesso: AcessoUsuario = {
  temFuncionarioVinculado: true,
  temNoOrganograma: true,
  acessoTotal: false,
  centrosCustoIds: [1, 2],
  quantidadeCentrosAcessiveis: 2,
};

describe('podeAcessarMeuDashboard', () => {
  it('returns false when acesso is null', () => {
    expect(podeAcessarMeuDashboard(null)).toBe(false);
  });

  it('returns true for acesso total', () => {
    expect(podeAcessarMeuDashboard({ ...baseAcesso, acessoTotal: true, centrosCustoIds: [] })).toBe(true);
  });

  it('returns false when motivoNegacao is set', () => {
    expect(
      podeAcessarMeuDashboard({ ...baseAcesso, motivoNegacao: 'SEM_FUNCIONARIO' }),
    ).toBe(false);
  });

  it('returns false without funcionario vinculado', () => {
    expect(podeAcessarMeuDashboard({ ...baseAcesso, temFuncionarioVinculado: false })).toBe(false);
  });

  it('returns false without no organograma', () => {
    expect(podeAcessarMeuDashboard({ ...baseAcesso, temNoOrganograma: false })).toBe(false);
  });

  it('returns false when centros de custo are empty', () => {
    expect(
      podeAcessarMeuDashboard({ ...baseAcesso, centrosCustoIds: [], quantidadeCentrosAcessiveis: 0 }),
    ).toBe(false);
  });

  it('returns true for scoped user with centros', () => {
    expect(podeAcessarMeuDashboard(baseAcesso)).toBe(true);
  });
});
