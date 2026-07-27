package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.folha.api.ResumoFolhaPagamentoDTO;
import br.com.techne.sistemafolha.folha.domain.ResumoFolhaPagamento;
import br.com.techne.sistemafolha.folha.infrastructure.ResumoFolhaPagamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumoFolhaPagamentoServiceTest {

    private static final LocalDate COMPETENCIA_INICIO = LocalDate.of(2024, 10, 1);
    private static final LocalDate COMPETENCIA_FIM = LocalDate.of(2024, 10, 31);

    @Mock
    private ResumoFolhaPagamentoRepository resumoFolhaPagamentoRepository;

    @InjectMocks
    private ResumoFolhaPagamentoService resumoFolhaPagamentoService;

    @Test
    void listarTodos_mapeia_resumos_ativos() {
        ResumoFolhaPagamento resumo = resumoAtivo(1L);
        when(resumoFolhaPagamentoRepository.findByAtivoTrue()).thenReturn(List.of(resumo));

        List<ResumoFolhaPagamentoDTO> result = resumoFolhaPagamentoService.listarTodos();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals(new BigDecimal("50000.00"), result.get(0).totalLiquido());
    }

    @Test
    void consultarPorCompetencia_retorna_optional_quando_encontrado() {
        ResumoFolhaPagamento resumo = resumoAtivo(2L);
        when(resumoFolhaPagamentoRepository.findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                COMPETENCIA_INICIO, COMPETENCIA_FIM))
            .thenReturn(Optional.of(resumo));

        Optional<ResumoFolhaPagamentoDTO> result = resumoFolhaPagamentoService.consultarPorCompetencia(
            COMPETENCIA_INICIO, COMPETENCIA_FIM);

        assertTrue(result.isPresent());
        assertEquals(2L, result.get().id());
    }

    private ResumoFolhaPagamento resumoAtivo(Long id) {
        ResumoFolhaPagamento resumo = new ResumoFolhaPagamento();
        resumo.setId(id);
        resumo.setTotalEmpregados(100);
        resumo.setTotalEncargos(new BigDecimal("10000.00"));
        resumo.setTotalPagamentos(new BigDecimal("60000.00"));
        resumo.setTotalDescontos(new BigDecimal("10000.00"));
        resumo.setTotalLiquido(new BigDecimal("50000.00"));
        resumo.setCompetenciaInicio(COMPETENCIA_INICIO);
        resumo.setCompetenciaFim(COMPETENCIA_FIM);
        resumo.setDataImportacao(LocalDateTime.of(2024, 11, 1, 8, 0));
        resumo.setDecimoTerceiro(false);
        resumo.setAtivo(true);
        return resumo;
    }
}
