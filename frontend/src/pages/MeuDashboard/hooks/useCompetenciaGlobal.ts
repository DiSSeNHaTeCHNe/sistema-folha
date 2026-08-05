import { useCallback, useState } from 'react';
import { gravarCompetenciaGlobal, lerCompetenciaGlobal } from '../competenciaStorage';

export function useCompetenciaGlobal() {
  const [competenciaGlobal, setState] = useState<string | null>(() => lerCompetenciaGlobal());

  const setCompetenciaGlobal = useCallback((competencia: string | null) => {
    setState(competencia);
    gravarCompetenciaGlobal(competencia);
  }, []);

  return { competenciaGlobal, setCompetenciaGlobal };
}
