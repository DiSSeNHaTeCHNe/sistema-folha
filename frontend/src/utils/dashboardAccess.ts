import type { AcessoUsuario } from '../types';

export function podeAcessarMeuDashboard(acesso: AcessoUsuario | null | undefined): boolean {
  if (!acesso) {
    return false;
  }
  if (acesso.acessoTotal) {
    return true;
  }
  if (acesso.motivoNegacao) {
    return false;
  }
  if (!acesso.temFuncionarioVinculado || !acesso.temNoOrganograma) {
    return false;
  }
  return acesso.centrosCustoIds.length > 0;
}
