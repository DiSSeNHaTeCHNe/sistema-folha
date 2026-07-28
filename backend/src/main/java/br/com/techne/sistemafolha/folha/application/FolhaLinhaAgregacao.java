package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Agrega totais a partir de linhas de folha (RSF-01 foundation).
 * Package-visible — uso interno de {@code folha.application}; sem Spring.
 */
class FolhaLinhaAgregacao {

    record Totais(
        long empregados,
        BigDecimal pagamentos,
        BigDecimal descontos,
        BigDecimal liquido
    ) {}

    Totais agregar(List<FolhaLinhaSnapshot> linhas) {
        if (linhas == null || linhas.isEmpty()) {
            return new Totais(0L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        long empregados = linhas.stream()
            .map(FolhaLinhaSnapshot::funcionarioId)
            .filter(Objects::nonNull)
            .distinct()
            .count();

        BigDecimal pagamentos = linhas.stream()
            .filter(fp -> "PROVENTO".equals(fp.tipoRubricaDescricao()))
            .map(FolhaLinhaSnapshot::valor)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal descontos = linhas.stream()
            .filter(fp -> "DESCONTO".equals(fp.tipoRubricaDescricao()))
            .map(FolhaLinhaSnapshot::valor)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new Totais(empregados, pagamentos, descontos, pagamentos.subtract(descontos));
    }
}
