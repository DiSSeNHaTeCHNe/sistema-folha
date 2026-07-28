package br.com.techne.sistemafolha.folha.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

final class FolhaMotorCalculo {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private FolhaMotorCalculo() {
    }

    record LinhaCalculoInput(
        BigDecimal valor,
        short operadorBruto,
        short operadorLiquido,
        short operadorCusto
    ) {}

    record TotaisFuncionario(
        BigDecimal bruto,
        BigDecimal liquido,
        BigDecimal custoFolha
    ) {}

    enum Totalizador {
        GROSS,
        NET,
        COMPANY_COST
    }

    static TotaisFuncionario calcularPorLinhas(List<LinhaCalculoInput> linhas) {
        if (linhas == null || linhas.isEmpty()) {
            return new TotaisFuncionario(
                arredondar(BigDecimal.ZERO),
                arredondar(BigDecimal.ZERO),
                arredondar(BigDecimal.ZERO)
            );
        }

        BigDecimal bruto = BigDecimal.ZERO;
        BigDecimal liquido = BigDecimal.ZERO;
        BigDecimal custoFolha = BigDecimal.ZERO;

        for (LinhaCalculoInput linha : linhas) {
            BigDecimal valor = linha.valor() != null ? linha.valor() : BigDecimal.ZERO;
            bruto = bruto.add(valor.multiply(BigDecimal.valueOf(linha.operadorBruto())));
            liquido = liquido.add(valor.multiply(BigDecimal.valueOf(linha.operadorLiquido())));
            custoFolha = custoFolha.add(valor.multiply(BigDecimal.valueOf(linha.operadorCusto())));
        }

        return new TotaisFuncionario(arredondar(bruto), arredondar(liquido), arredondar(custoFolha));
    }

    static BigDecimal contribuicao(LinhaCalculoInput linha, Totalizador totalizador) {
        BigDecimal valor = linha.valor() != null ? linha.valor() : BigDecimal.ZERO;
        short operador = switch (totalizador) {
            case GROSS -> linha.operadorBruto();
            case NET -> linha.operadorLiquido();
            case COMPANY_COST -> linha.operadorCusto();
        };
        return arredondar(valor.multiply(BigDecimal.valueOf(operador)));
    }

    static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(SCALE, ROUNDING);
    }
}
