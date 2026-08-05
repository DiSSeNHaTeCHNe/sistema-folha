const STORAGE_KEY = 'sistema-folha:meu-dashboard-competencia-global';

export function lerCompetenciaGlobal(): string | null {
  try {
    const value = sessionStorage.getItem(STORAGE_KEY);
    return value && value.length > 0 ? value : null;
  } catch {
    return null;
  }
}

export function gravarCompetenciaGlobal(competencia: string | null): void {
  try {
    if (competencia) {
      sessionStorage.setItem(STORAGE_KEY, competencia);
    } else {
      sessionStorage.removeItem(STORAGE_KEY);
    }
  } catch {
    // sessionStorage indisponível — ignora
  }
}

export function limparCompetenciaGlobal(): void {
  gravarCompetenciaGlobal(null);
}
