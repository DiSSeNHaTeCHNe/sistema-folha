import { useEffect, useState } from 'react';
import { useAuth } from '../../../contexts/AuthContext';
import { centroCustoService } from '../../../services/centroCustoService';
import { linhaNegocioService } from '../../../services/linhaNegocioService';
import type { CentroCusto, LinhaNegocio } from '../../../types';

export interface ScopedFilterOptions {
  centrosCusto: CentroCusto[];
  linhasNegocio: LinhaNegocio[];
  loading: boolean;
}

export function useScopedFilterOptions(): ScopedFilterOptions {
  const { acessoUsuario } = useAuth();
  const [centrosCusto, setCentrosCusto] = useState<CentroCusto[]>([]);
  const [linhasNegocio, setLinhasNegocio] = useState<LinhaNegocio[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    async function carregar() {
      setLoading(true);
      try {
        const [todosCentros, todasLinhas] = await Promise.all([
          centroCustoService.listarTodos(),
          linhaNegocioService.listarTodos(),
        ]);
        if (cancelled) {
          return;
        }

        const centrosAtivos = todosCentros.filter((centro) => centro.ativo);
        const linhasAtivas = todasLinhas.filter((linha) => linha.ativo);

        if (acessoUsuario?.acessoTotal) {
          setCentrosCusto(centrosAtivos);
          setLinhasNegocio(linhasAtivas);
          return;
        }

        const scopedIds = new Set(acessoUsuario?.centrosCustoIds ?? []);
        const scopedCentros = centrosAtivos.filter((centro) => scopedIds.has(centro.id));
        const linhaIds = new Set(scopedCentros.map((centro) => centro.linhaNegocioId));
        setCentrosCusto(scopedCentros);
        setLinhasNegocio(linhasAtivas.filter((linha) => linhaIds.has(linha.id)));
      } catch {
        if (!cancelled) {
          setCentrosCusto([]);
          setLinhasNegocio([]);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void carregar();
    return () => {
      cancelled = true;
    };
  }, [acessoUsuario]);

  return { centrosCusto, linhasNegocio, loading };
}
