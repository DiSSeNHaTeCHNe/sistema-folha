package br.com.techne.sistemafolha.folha.application;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FolhaCustoEmpresaComposerTest {

    @Test
    void compor_somaCustoFolhaEncargosBeneficios() {
        BigDecimal resultado = FolhaCustoEmpresaComposer.compor(
            new BigDecimal("8000.00"),
            new BigDecimal("1000.00"),
            new BigDecimal("700.00")
        );

        assertEquals(new BigDecimal("9700.00"), resultado);
    }

    @Test
    void compor_trataNulosComoZero() {
        BigDecimal resultado = FolhaCustoEmpresaComposer.compor(
            new BigDecimal("5000.00"),
            null,
            null
        );

        assertEquals(new BigDecimal("5000.00"), resultado);
    }

    @Test
    void compor_arredondaHalfUpDuasCasas() {
        BigDecimal resultado = FolhaCustoEmpresaComposer.compor(
            new BigDecimal("10.004"),
            new BigDecimal("0.001"),
            BigDecimal.ZERO
        );

        assertEquals(new BigDecimal("10.01"), resultado);
    }

    @Test
    void compor_custoFolhaNull_trataComoZero() {
        BigDecimal resultado = FolhaCustoEmpresaComposer.compor(
            null,
            new BigDecimal("100.00"),
            new BigDecimal("50.00")
        );

        assertEquals(new BigDecimal("150.00"), resultado);
    }
}
