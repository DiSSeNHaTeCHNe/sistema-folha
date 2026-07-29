package br.com.techne.sistemafolha.folha.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

final class FolhaMotorCalculo {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal CEM = new BigDecimal("100");

    private FolhaMotorCalculo() {
    }

    record LinhaCalculoInput(
        BigDecimal valor,
        short operadorBruto,
        short operadorLiquido,
        short operadorCusto,
        BigDecimal porcentagem
    ) {
        LinhaCalculoInput(BigDecimal valor, short operadorBruto, short operadorLiquido, short operadorCusto) {
            this(valor, operadorBruto, operadorLiquido, operadorCusto, null);
        }
    }

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
            custoFolha = custoFolha.add(contribuicaoCusto(valor, linha.operadorCusto(), linha.porcentagem()));
        }

        return new TotaisFuncionario(arredondar(bruto), arredondar(liquido), arredondar(custoFolha));
    }

    static BigDecimal contribuicao(LinhaCalculoInput linha, Totalizador totalizador) {
        BigDecimal valor = linha.valor() != null ? linha.valor() : BigDecimal.ZERO;
        return switch (totalizador) {
            case GROSS -> arredondar(valor.multiply(BigDecimal.valueOf(linha.operadorBruto())));
            case NET -> arredondar(valor.multiply(BigDecimal.valueOf(linha.operadorLiquido())));
            case COMPANY_COST -> arredondar(contribuicaoCusto(valor, linha.operadorCusto(), linha.porcentagem()));
        };
    }

    static BigDecimal porcentagemEfetiva(BigDecimal porcentagem) {
        return porcentagem != null ? porcentagem : CEM;
    }

    static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(SCALE, ROUNDING);
    }

    private static BigDecimal contribuicaoCusto(BigDecimal valor, short operadorCusto, BigDecimal porcentagem) {
        BigDecimal pct = porcentagemEfetiva(porcentagem);
        return valor
            .multiply(BigDecimal.valueOf(operadorCusto))
            .multiply(pct)
            .divide(CEM, SCALE + 4, ROUNDING);
    }
}
