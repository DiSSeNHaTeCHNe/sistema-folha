package br.com.techne.sistemafolha.cadastros.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RubricaDTOTest {

    @Test
    void gettersSetters_cobremTodosOsCampos() {
        RubricaDTO dto = new RubricaDTO(null, null, null, null, null, null, null, null, null, null);
        dto.setId(1L);
        dto.setCodigo("R001");
        dto.setDescricao("Salário");
        dto.setTipoRubricaDescricao("Provento");
        dto.setTipo("PROVENTO");
        dto.setPorcentagem(100.0);
        dto.setOperadorBruto((short) 1);
        dto.setOperadorLiquido((short) 1);
        dto.setOperadorCusto((short) 1);
        dto.setAtivo(true);

        assertEquals(1L, dto.getId());
        assertEquals("R001", dto.getCodigo());
        assertEquals("Salário", dto.getDescricao());
        assertEquals("Provento", dto.getTipoRubricaDescricao());
        assertEquals("PROVENTO", dto.getTipo());
        assertEquals(100.0, dto.getPorcentagem());
        assertEquals((short) 1, dto.getOperadorBruto());
        assertEquals((short) 1, dto.getOperadorLiquido());
        assertEquals((short) 1, dto.getOperadorCusto());
        assertTrue(dto.getAtivo());
    }
}
