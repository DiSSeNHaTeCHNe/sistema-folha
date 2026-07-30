package br.com.techne.sistemafolha.folha.domain;

import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FichaLinhaTest {

    @Test
    void accessors_coverEntityState() {
        var ficha = new FichaMensal();
        ficha.setId(5L);
        var rubrica = new Rubrica();
        rubrica.setId(7L);

        var linha = new FichaLinha();
        linha.setId(1L);
        linha.setFichaMensal(ficha);
        linha.setRubrica(rubrica);
        linha.setValor(new BigDecimal("2500.00"));
        linha.setOrigemLinha(OrigemLinha.FOLHA_ADP);
        linha.setOperadorBruto((short) 1);
        linha.setOperadorLiquido((short) 1);
        linha.setOperadorCusto((short) 1);
        linha.setPorcentagem(new BigDecimal("100.0000"));
        linha.setAtivo(true);

        assertEquals(1L, linha.getId());
        assertEquals(ficha, linha.getFichaMensal());
        assertEquals(rubrica, linha.getRubrica());
        assertEquals(OrigemLinha.FOLHA_ADP, linha.getOrigemLinha());
        assertEquals(new BigDecimal("2500.00"), linha.getValor());
        assertTrue(linha.getAtivo());
    }

    @Test
    void equalsAndHashCode_useGeneratedContract() {
        var left = new FichaLinha();
        left.setId(1L);
        left.setOrigemLinha(OrigemLinha.CALCULADO);

        var right = new FichaLinha();
        right.setId(1L);
        right.setOrigemLinha(OrigemLinha.CALCULADO);

        var other = new FichaLinha();
        other.setId(2L);

        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
        assertNotEquals(left, other);
    }
}
