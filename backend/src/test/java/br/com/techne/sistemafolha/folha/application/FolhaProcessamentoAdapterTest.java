package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.folha.api.ProcessamentoOpcoes;
import br.com.techne.sistemafolha.folha.api.ProcessamentoResultadoDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FolhaProcessamentoAdapterTest {

    private static final LocalDate COMPETENCIA_INICIO = LocalDate.of(2024, 10, 1);
    private static final LocalDate COMPETENCIA_FIM = LocalDate.of(2024, 10, 31);

    @Mock
    private FolhaProcessamentoService folhaProcessamentoService;

    @InjectMocks
    private FolhaProcessamentoAdapter folhaProcessamentoAdapter;

    @Test
    void processar_delegaComRecalcularFeriasFalse() {
        ProcessamentoResultadoDTO esperado = new ProcessamentoResultadoDTO(2, 5, 2);
        when(folhaProcessamentoService.processar(
            eq(COMPETENCIA_INICIO),
            eq(COMPETENCIA_FIM),
            eq(false),
            eq(new ProcessamentoOpcoes(false))))
            .thenReturn(esperado);

        ProcessamentoResultadoDTO resultado = folhaProcessamentoAdapter.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, false);

        assertEquals(esperado, resultado);
        verify(folhaProcessamentoService).processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, false, new ProcessamentoOpcoes(false));
    }

    @Test
    void processar_delegaComRecalcularFeriasTrue() {
        ProcessamentoResultadoDTO esperado = new ProcessamentoResultadoDTO(1, 3, 1);
        when(folhaProcessamentoService.processar(
            eq(COMPETENCIA_INICIO),
            eq(COMPETENCIA_FIM),
            eq(true),
            eq(new ProcessamentoOpcoes(true))))
            .thenReturn(esperado);

        ProcessamentoResultadoDTO resultado = folhaProcessamentoAdapter.processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, true, true);

        assertEquals(esperado, resultado);
        verify(folhaProcessamentoService).processar(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, true, new ProcessamentoOpcoes(true));
    }
}
