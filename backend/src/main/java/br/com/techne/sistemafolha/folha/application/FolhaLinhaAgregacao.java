package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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

    record TotaisResumo(
        long empregados,
        BigDecimal totalBruto,
        BigDecimal totalLiquido,
        BigDecimal totalCustoFolha,
        BigDecimal totalCustoBeneficios,
        BigDecimal totalCustoEmpresa,
        BigDecimal totalEncargos
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

    TotaisResumo agregar(
            List<FolhaLinhaSnapshot> linhas,
            Map<Long, BigDecimal> custoBeneficiosPorFuncionario,
            Map<Long, BigDecimal> encargosPorFuncionario) {
        if (linhas == null || linhas.isEmpty()) {
            return zerosResumo();
        }

        Map<Long, List<FolhaLinhaSnapshot>> porFuncionario = linhas.stream()
            .filter(l -> l.funcionarioId() != null)
            .collect(Collectors.groupingBy(FolhaLinhaSnapshot::funcionarioId));

        Map<Long, BigDecimal> beneficios = custoBeneficiosPorFuncionario != null
            ? custoBeneficiosPorFuncionario : Map.of();
        Map<Long, BigDecimal> encargos = encargosPorFuncionario != null
            ? encargosPorFuncionario : Map.of();

        BigDecimal totalBruto = BigDecimal.ZERO;
        BigDecimal totalLiquido = BigDecimal.ZERO;
        BigDecimal totalCustoFolha = BigDecimal.ZERO;
        BigDecimal totalCustoBeneficios = BigDecimal.ZERO;
        BigDecimal totalEncargos = BigDecimal.ZERO;
        BigDecimal totalCustoEmpresa = BigDecimal.ZERO;

        for (Map.Entry<Long, List<FolhaLinhaSnapshot>> entry : porFuncionario.entrySet()) {
            Long funcionarioId = entry.getKey();
            List<FolhaMotorCalculo.LinhaCalculoInput> inputs = entry.getValue().stream()
                .map(this::toInput)
                .toList();
            FolhaMotorCalculo.TotaisFuncionario totais = FolhaMotorCalculo.calcularPorLinhas(inputs);

            BigDecimal custoBeneficios = FolhaMotorCalculo.arredondar(
                beneficios.getOrDefault(funcionarioId, BigDecimal.ZERO));
            BigDecimal encargosFuncionario = FolhaMotorCalculo.arredondar(
                encargos.getOrDefault(funcionarioId, BigDecimal.ZERO));
            BigDecimal custoEmpresa = FolhaCustoEmpresaComposer.compor(
                totais.custoFolha(), encargosFuncionario, custoBeneficios);

            totalBruto = totalBruto.add(totais.bruto());
            totalLiquido = totalLiquido.add(totais.liquido());
            totalCustoFolha = totalCustoFolha.add(totais.custoFolha());
            totalCustoBeneficios = totalCustoBeneficios.add(custoBeneficios);
            totalEncargos = totalEncargos.add(encargosFuncionario);
            totalCustoEmpresa = totalCustoEmpresa.add(custoEmpresa);
        }

        return new TotaisResumo(
            porFuncionario.size(),
            FolhaMotorCalculo.arredondar(totalBruto),
            FolhaMotorCalculo.arredondar(totalLiquido),
            FolhaMotorCalculo.arredondar(totalCustoFolha),
            FolhaMotorCalculo.arredondar(totalCustoBeneficios),
            FolhaMotorCalculo.arredondar(totalCustoEmpresa),
            FolhaMotorCalculo.arredondar(totalEncargos)
        );
    }

    private FolhaMotorCalculo.LinhaCalculoInput toInput(FolhaLinhaSnapshot linha) {
        BigDecimal valor = linha.valor() != null ? linha.valor() : BigDecimal.ZERO;
        return new FolhaMotorCalculo.LinhaCalculoInput(
            valor, linha.operadorBruto(), linha.operadorLiquido(), linha.operadorCusto());
    }

    private TotaisResumo zerosResumo() {
        return new TotaisResumo(
            0L,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO
        );
    }
}
