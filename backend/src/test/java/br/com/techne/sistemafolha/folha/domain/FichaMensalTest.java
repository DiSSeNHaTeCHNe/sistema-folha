package br.com.techne.sistemafolha.folha.domain;

import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FichaMensalTest {

    @Test
    void accessors_coverEntityState() {
        var funcionario = new Funcionario();
        funcionario.setId(10L);
        var centro = new CentroCusto();
        centro.setId(3L);

        var ficha = new FichaMensal();
        ficha.setId(1L);
        ficha.setFuncionario(funcionario);
        ficha.setCentroCusto(centro);
        ficha.setCompetenciaInicio(LocalDate.of(2026, 1, 1));
        ficha.setCompetenciaFim(LocalDate.of(2026, 1, 31));
        ficha.setDecimoTerceiro(false);
        ficha.setBruto(new BigDecimal("5000.00"));
        ficha.setLiquido(new BigDecimal("4000.00"));
        ficha.setCustoFolha(new BigDecimal("4500.00"));
        ficha.setAtivo(true);

        assertEquals(1L, ficha.getId());
        assertEquals(funcionario, ficha.getFuncionario());
        assertEquals(centro, ficha.getCentroCusto());
        assertEquals(LocalDate.of(2026, 1, 31), ficha.getCompetenciaFim());
        assertFalse(ficha.getDecimoTerceiro());
        assertEquals(new BigDecimal("5000.00"), ficha.getBruto());
        assertTrue(ficha.getAtivo());
    }

    @Test
    void equalsAndHashCode_useGeneratedContract() {
        var left = new FichaMensal();
        left.setId(1L);
        left.setCompetenciaInicio(LocalDate.of(2026, 1, 1));

        var right = new FichaMensal();
        right.setId(1L);
        right.setCompetenciaInicio(LocalDate.of(2026, 1, 1));

        var other = new FichaMensal();
        other.setId(2L);

        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
        assertNotEquals(left, other);
    }
}
