import type { DashboardLayout } from './types';

const CHAVE = 'sistema-folha:meu-dashboard-layout';

export function lerLayoutCache(): DashboardLayout | null {
  try {
    const valor = window.localStorage.getItem(CHAVE);
    if (!valor) {
      return null;
    }
    return JSON.parse(valor) as DashboardLayout;
  } catch {
    return null;
  }
}

export function gravarLayoutCache(layout: DashboardLayout): void {
  try {
    window.localStorage.setItem(CHAVE, JSON.stringify(layout));
  } catch {
    // Falha silenciosa: cota, modo privado ou storage indisponível
  }
}

export function limparLayoutCache(): void {
  try {
    window.localStorage.removeItem(CHAVE);
  } catch {
    // Falha silenciosa
  }
}
