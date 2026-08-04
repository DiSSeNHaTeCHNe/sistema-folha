import { isTemaId, TEMA_PADRAO, type TemaId } from './themes';

const CHAVE = 'sistema-folha:tema';

export function lerTemaSalvo(): TemaId {
  try {
    const valor = window.localStorage.getItem(CHAVE);
    return isTemaId(valor) ? valor : TEMA_PADRAO;
  } catch {
    return TEMA_PADRAO;
  }
}

export function gravarTema(id: TemaId): void {
  try {
    window.localStorage.setItem(CHAVE, id);
  } catch {
    // Falha silenciosa: cota, modo privado ou storage indisponível
  }
}
