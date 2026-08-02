package br.com.techne.sistemafolha.folha.application;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FolhaMotorCalculoTest {

    @Test
    void calcularPorLinhas_proventoEDesconto_aplicaOperadoresPadrao() {
        List<FolhaMotorCalculo.LinhaCalculoInput> linhas = List.of(
            linha(new BigDecimal("10000.00"), (short) 1, (short) 1, (short) 1, null),
            linha(new BigDecimal("1500.00"), (short) 0, (short) -1, (short) 0, null)
        );

        FolhaMotorCalculo.TotaisFuncionario totais = FolhaMotorCalculo.calcularPorLinhas(linhas);

        assertEquals(new BigDecimal("10000.00"), totais.bruto());
        assertEquals(new BigDecimal("8500.00"), totais.liquido());
        assertEquals(new BigDecimal("10000.00"), totais.custoFolha());
    }

    @Test
    void calcularPorLinhas_operadorCustom_impactaSomenteTotalizadorConfigurado() {
        List<FolhaMotorCalculo.LinhaCalculoInput> linhas = List.of(
            linha(new BigDecimal("500.00"), (short) 0, (short) -1, (short) 0, null)
        );

        FolhaMotorCalculo.TotaisFuncionario totais = FolhaMotorCalculo.calcularPorLinhas(linhas);

        assertEquals(new BigDecimal("0.00"), totais.bruto());
        assertEquals(new BigDecimal("-500.00"), totais.liquido());
        assertEquals(new BigDecimal("0.00"), totais.custoFolha());
    }

    @Test
    void calcularPorLinhas_arredondaHalfUpDuasCasas() {
        List<FolhaMotorCalculo.LinhaCalculoInput> linhas = List.of(
            linha(new BigDecimal("10.005"), (short) 1, (short) 1, (short) 1, null)
        );

        FolhaMotorCalculo.TotaisFuncionario totais = FolhaMotorCalculo.calcularPorLinhas(linhas);

        assertEquals(new BigDecimal("10.01"), totais.bruto());
        assertEquals(new BigDecimal("10.01"), totais.liquido());
        assertEquals(new BigDecimal("10.01"), totais.custoFolha());
    }

    @Test
    void contribuicao_retornaValorMultiplicadoPeloOperadorDoTotalizador() {
        FolhaMotorCalculo.LinhaCalculoInput linha =
            linha(new BigDecimal("200.00"), (short) 1, (short) -1, (short) 1, null);

        assertEquals(new BigDecimal("200.00"),
            FolhaMotorCalculo.contribuicao(linha, FolhaMotorCalculo.Totalizador.GROSS));
        assertEquals(new BigDecimal("-200.00"),
            FolhaMotorCalculo.contribuicao(linha, FolhaMotorCalculo.Totalizador.NET));
    }

    @Test
    void calcularPorLinhas_porcentagem13863_custoAncora1006236() {
        List<FolhaMotorCalculo.LinhaCalculoInput> linhas = List.of(
            linha(new BigDecimal("7258.43"), (short) 1, (short) 1, (short) 1, new BigDecimal("138.63"))
        );

        FolhaMotorCalculo.TotaisFuncionario totais = FolhaMotorCalculo.calcularPorLinhas(linhas);

        assertEquals(new BigDecimal("7258.43"), totais.bruto());
        assertEquals(new BigDecimal("7258.43"), totais.liquido());
        assertEquals(new BigDecimal("10062.36"), totais.custoFolha());
    }

    @Test
    void calcularPorLinhas_porcentagemDiferenteDe100_brutoLiquidoIgnoramPorcentagem() {
        List<FolhaMotorCalculo.LinhaCalculoInput> linhas = List.of(
            linha(new BigDecimal("7258.43"), (short) 1, (short) 1, (short) 1, new BigDecimal("138.63"))
        );

        FolhaMotorCalculo.TotaisFuncionario totais = FolhaMotorCalculo.calcularPorLinhas(linhas);

        assertEquals(new BigDecimal("7258.43"), totais.bruto());
        assertEquals(new BigDecimal("7258.43"), totais.liquido());
    }

    @Test
    void calcularPorLinhas_porcentagemZero_custoZeroBrutoLiquidoInalterados() {
        List<FolhaMotorCalculo.LinhaCalculoInput> linhas = List.of(
            linha(new BigDecimal("5000.00"), (short) 1, (short) 1, (short) 1, BigDecimal.ZERO)
        );

        FolhaMotorCalculo.TotaisFuncionario totais = FolhaMotorCalculo.calcularPorLinhas(linhas);

        assertEquals(new BigDecimal("5000.00"), totais.bruto());
        assertEquals(new BigDecimal("5000.00"), totais.liquido());
        assertEquals(new BigDecimal("0.00"), totais.custoFolha());
    }

    @Test
    void calcularPorLinhas_porcentagemNull_trataComo100NoCusto() {
        List<FolhaMotorCalculo.LinhaCalculoInput> linhas = List.of(
            linha(new BigDecimal("688.00"), (short) 1, (short) 1, (short) 1, null)
        );

        FolhaMotorCalculo.TotaisFuncionario totais = FolhaMotorCalculo.calcularPorLinhas(linhas);

        assertEquals(new BigDecimal("688.00"), totais.custoFolha());
        assertEquals(new BigDecimal("688.00"), totais.bruto());
    }

    @Test
    void contribuicao_companyCost_aplicaPorcentagem() {
        FolhaMotorCalculo.LinhaCalculoInput linha =
            linha(new BigDecimal("7258.43"), (short) 1, (short) 1, (short) 1, new BigDecimal("138.63"));

        assertEquals(new BigDecimal("10062.36"),
            FolhaMotorCalculo.contribuicao(linha, FolhaMotorCalculo.Totalizador.COMPANY_COST));
        assertEquals(new BigDecimal("7258.43"),
            FolhaMotorCalculo.contribuicao(linha, FolhaMotorCalculo.Totalizador.GROSS));
    }

    @Test
    void porcentagemEfetiva_nullRetorna100() {
        assertEquals(new BigDecimal("100"), FolhaMotorCalculo.porcentagemEfetiva(null));
        assertEquals(new BigDecimal("138.63"), FolhaMotorCalculo.porcentagemEfetiva(new BigDecimal("138.63")));
    }

    @Test
    void calcularPorLinhas_listaNullOuVazia_retornaZeros() {
        FolhaMotorCalculo.TotaisFuncionario nullList = FolhaMotorCalculo.calcularPorLinhas(null);
        assertEquals(new BigDecimal("0.00"), nullList.bruto());
        assertEquals(new BigDecimal("0.00"), nullList.liquido());
        assertEquals(new BigDecimal("0.00"), nullList.custoFolha());

        FolhaMotorCalculo.TotaisFuncionario empty = FolhaMotorCalculo.calcularPorLinhas(List.of());
        assertEquals(new BigDecimal("0.00"), empty.bruto());
        assertEquals(new BigDecimal("0.00"), empty.liquido());
        assertEquals(new BigDecimal("0.00"), empty.custoFolha());
    }

    @Test
    void calcularPorLinhas_valorNull_trataComoZero() {
        FolhaMotorCalculo.LinhaCalculoInput linha =
            new FolhaMotorCalculo.LinhaCalculoInput(null, (short) 1, (short) 1, (short) 1, null);

        FolhaMotorCalculo.TotaisFuncionario totais = FolhaMotorCalculo.calcularPorLinhas(List.of(linha));

        assertEquals(new BigDecimal("0.00"), totais.bruto());
        assertEquals(new BigDecimal("0.00"), totais.liquido());
        assertEquals(new BigDecimal("0.00"), totais.custoFolha());
    }

    @Test
    void contribuicao_valorNull_trataComoZero() {
        FolhaMotorCalculo.LinhaCalculoInput linha =
            new FolhaMotorCalculo.LinhaCalculoInput(null, (short) 1, (short) 1, (short) 1, null);

        assertEquals(new BigDecimal("0.00"),
            FolhaMotorCalculo.contribuicao(linha, FolhaMotorCalculo.Totalizador.GROSS));
    }

    @Test
    void linhaCalculoInput_construtorSemPorcentagem_usaNull() {
        FolhaMotorCalculo.LinhaCalculoInput linha =
            new FolhaMotorCalculo.LinhaCalculoInput(new BigDecimal("100.00"), (short) 1, (short) 1, (short) 1);

        assertEquals(new BigDecimal("100.00"), linha.valor());
        assertEquals(null, linha.porcentagem());
    }

    @Test
    void arredondar_aplicaHalfUpDuasCasas() {
        assertEquals(new BigDecimal("1.23"), FolhaMotorCalculo.arredondar(new BigDecimal("1.234")));
    }

    private FolhaMotorCalculo.LinhaCalculoInput linha(
            BigDecimal valor, short operadorBruto, short operadorLiquido, short operadorCusto,
            BigDecimal porcentagem) {
        return new FolhaMotorCalculo.LinhaCalculoInput(valor, operadorBruto, operadorLiquido, operadorCusto, porcentagem);
    }
}
