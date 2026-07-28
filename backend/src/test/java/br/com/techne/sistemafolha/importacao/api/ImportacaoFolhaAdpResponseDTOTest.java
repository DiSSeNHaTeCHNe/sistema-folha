package br.com.techne.sistemafolha.importacao.api;

import br.com.techne.sistemafolha.folha.api.FolhaPagamentoDTO;
import br.com.techne.sistemafolha.folha.api.ProcessamentoResultadoDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportacaoFolhaAdpResponseDTOTest {

    private static final LocalDate COMPETENCIA = LocalDate.of(2024, 10, 1);

    @Test
    void success_factorySetsSuccessTrueAndCompositeMessage() {
        FolhaPagamentoDTO folha = folhaPagamento(1L);
        FolhaPagamentoDTO folha2 = folhaPagamento(2L);
        ProcessamentoResultadoDTO processamento = new ProcessamentoResultadoDTO(3, 15, 2);

        ImportacaoFolhaAdpResponseDTO response = ImportacaoFolhaAdpResponseDTO.success(
            "folha.txt", 1024L, List.of(folha, folha2), processamento);

        assertTrue(response.success());
        assertEquals(
            "Importação concluída: 2 registros ADP; ficha processada: 3 fichas, 15 linhas",
            response.message());
        assertEquals("folha.txt", response.arquivo());
        assertEquals(1024L, response.tamanho());
        assertEquals(2, response.registrosProcessados());
        assertEquals(3, response.fichasProcessadas());
        assertEquals(15, response.linhasProcessadas());
        assertFalse(response.conflict());
        assertEquals(2, response.folhasPagamento().size());
    }

    @Test
    void error_factorySetsSuccessFalseAndNullProcessingStats() {
        ImportacaoFolhaAdpResponseDTO response =
            ImportacaoFolhaAdpResponseDTO.error("Erro de teste", "folha.txt");

        assertFalse(response.success());
        assertEquals("Erro de teste", response.message());
        assertNull(response.fichasProcessadas());
        assertNull(response.linhasProcessadas());
        assertEquals(0, response.registrosProcessados());
        assertTrue(response.folhasPagamento().isEmpty());
    }

    private FolhaPagamentoDTO folhaPagamento(long id) {
        return new FolhaPagamentoDTO(
            id, 10L, "João", 2L, "001", "Salário", "PROVENTO",
            3L, "Analista", 4L, "CC", 5L, "LN",
            COMPETENCIA, COMPETENCIA.withDayOfMonth(31),
            new BigDecimal("1000"), BigDecimal.ONE, new BigDecimal("1000"), false
        );
    }
}
