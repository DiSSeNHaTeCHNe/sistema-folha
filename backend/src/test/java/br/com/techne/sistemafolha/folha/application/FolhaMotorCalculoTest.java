package br.com.techne.sistemafolha.folha.application;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FolhaMotorCalculoTest {

    @Test
    void calcularPorLinhas_proventoEDesconto_aplicaOperadoresPadrao() {
        List<FolhaMotorCalculo.LinhaCalculoInput> linhas = List.of(
            linha(new BigDecimal("10000.00"), (short) 1, (short) 1, (short) 1),
            linha(new BigDecimal("1500.00"), (short) 0, (short) -1, (short) 0)
        );

        FolhaMotorCalculo.TotaisFuncionario totais = FolhaMotorCalculo.calcularPorLinhas(linhas);

        assertEquals(new BigDecimal("10000.00"), totais.bruto());
        assertEquals(new BigDecimal("8500.00"), totais.liquido());
        assertEquals(new BigDecimal("10000.00"), totais.custoFolha());
    }

    @Test
    void calcularPorLinhas_operadorCustom_impactaSomenteTotalizadorConfigurado() {
        List<FolhaMotorCalculo.LinhaCalculoInput> linhas = List.of(
            linha(new BigDecimal("500.00"), (short) 0, (short) -1, (short) 0)
        );

        FolhaMotorCalculo.TotaisFuncionario totais = FolhaMotorCalculo.calcularPorLinhas(linhas);

        assertEquals(new BigDecimal("0.00"), totais.bruto());
        assertEquals(new BigDecimal("-500.00"), totais.liquido());
        assertEquals(new BigDecimal("0.00"), totais.custoFolha());
    }

    @Test
    void calcularPorLinhas_arredondaHalfUpDuasCasas() {
        List<FolhaMotorCalculo.LinhaCalculoInput> linhas = List.of(
            linha(new BigDecimal("10.005"), (short) 1, (short) 1, (short) 1)
        );

        FolhaMotorCalculo.TotaisFuncionario totais = FolhaMotorCalculo.calcularPorLinhas(linhas);

        assertEquals(new BigDecimal("10.01"), totais.bruto());
        assertEquals(new BigDecimal("10.01"), totais.liquido());
        assertEquals(new BigDecimal("10.01"), totais.custoFolha());
    }

    @Test
    void contribuicao_retornaValorMultiplicadoPeloOperadorDoTotalizador() {
        FolhaMotorCalculo.LinhaCalculoInput linha =
            linha(new BigDecimal("200.00"), (short) 1, (short) -1, (short) 1);

        assertEquals(new BigDecimal("200.00"),
            FolhaMotorCalculo.contribuicao(linha, FolhaMotorCalculo.Totalizador.GROSS));
        assertEquals(new BigDecimal("-200.00"),
            FolhaMotorCalculo.contribuicao(linha, FolhaMotorCalculo.Totalizador.NET));
    }

    private FolhaMotorCalculo.LinhaCalculoInput linha(
            BigDecimal valor, short operadorBruto, short operadorLiquido, short operadorCusto) {
        return new FolhaMotorCalculo.LinhaCalculoInput(valor, operadorBruto, operadorLiquido, operadorCusto);
    }
}
