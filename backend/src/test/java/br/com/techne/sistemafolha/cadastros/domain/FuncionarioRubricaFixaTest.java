package br.com.techne.sistemafolha.cadastros.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FuncionarioRubricaFixaTest {

    @Test
    void lifecycleCallbacks_setAuditTimestamps() {
        var entity = new FuncionarioRubricaFixa();
        entity.setVigenciaInicio(LocalDate.of(2026, 1, 1));

        entity.onCreate();

        assertNotNull(entity.getDataCriacao());
        assertNotNull(entity.getDataAtualizacao());

        entity.onUpdate();

        assertNotNull(entity.getDataAtualizacao());
    }

    @Test
    void accessors_coverEntityState() {
        var funcionario = new Funcionario();
        funcionario.setId(10L);
        var rubrica = new Rubrica();
        rubrica.setId(20L);

        var entity = new FuncionarioRubricaFixa();
        entity.setId(1L);
        entity.setFuncionario(funcionario);
        entity.setRubrica(rubrica);
        entity.setValor(new BigDecimal("1500.50"));
        entity.setVigenciaInicio(LocalDate.of(2026, 1, 1));
        entity.setVigenciaFim(LocalDate.of(2026, 12, 31));
        entity.setComentario("fixa global");
        entity.setAtivo(true);
        entity.setDataCriacao(LocalDateTime.of(2026, 1, 1, 10, 0));
        entity.setDataAtualizacao(LocalDateTime.of(2026, 1, 2, 10, 0));

        assertEquals(1L, entity.getId());
        assertEquals(funcionario, entity.getFuncionario());
        assertEquals(rubrica, entity.getRubrica());
        assertEquals(new BigDecimal("1500.50"), entity.getValor());
        assertEquals(LocalDate.of(2026, 12, 31), entity.getVigenciaFim());
        assertEquals("fixa global", entity.getComentario());
        assertTrue(entity.getAtivo());
    }

    @Test
    void equalsAndHashCode_useGeneratedContract() {
        var left = new FuncionarioRubricaFixa();
        left.setId(1L);
        left.setVigenciaInicio(LocalDate.of(2026, 1, 1));

        var right = new FuncionarioRubricaFixa();
        right.setId(1L);
        right.setVigenciaInicio(LocalDate.of(2026, 1, 1));

        var other = new FuncionarioRubricaFixa();
        other.setId(2L);

        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
        assertNotEquals(left, other);
    }
}
