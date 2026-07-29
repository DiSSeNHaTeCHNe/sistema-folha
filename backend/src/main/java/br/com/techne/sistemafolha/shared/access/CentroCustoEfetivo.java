package br.com.techne.sistemafolha.shared.access;

import java.util.Set;

/**
 * Regra canônica A2: CC efetivo da competência = COALESCE(linha, funcionário).
 */
public final class CentroCustoEfetivo {

    private CentroCustoEfetivo() {
    }

    public static Long idOf(Long linhaCentroCustoId, Long funcionarioCentroCustoId) {
        if (linhaCentroCustoId != null) {
            return linhaCentroCustoId;
        }
        return funcionarioCentroCustoId;
    }

    public static boolean pertenceAoEscopo(Long centroCustoEfetivoId, Set<Long> centrosCustoIds) {
        if (centroCustoEfetivoId == null || centrosCustoIds == null || centrosCustoIds.isEmpty()) {
            return false;
        }
        return centrosCustoIds.contains(centroCustoEfetivoId);
    }
}
