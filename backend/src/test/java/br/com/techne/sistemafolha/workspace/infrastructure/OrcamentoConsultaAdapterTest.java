package br.com.techne.sistemafolha.workspace.infrastructure;

import br.com.techne.sistemafolha.dashboard.api.CentroCustoStatsDTO;
import br.com.techne.sistemafolha.dashboard.application.DashboardStatsAggregator;
import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.MotivoNegacaoAcesso;
import br.com.techne.sistemafolha.workspace.port.OrcamentoConsultaPort;
import br.com.techne.sistemafolha.workspace.port.OrcamentoConsultaPort.OrcamentoCentroCustoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrcamentoConsultaAdapterTest {

    private static final YearMonth COMPETENCIA = YearMonth.of(2025, 6);

    @Mock
    private FolhaConsultaPort folhaConsultaPort;

    @Mock
    private DashboardStatsAggregator dashboardStatsAggregator;

    private OrcamentoConsultaAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OrcamentoConsultaAdapter(folhaConsultaPort, dashboardStatsAggregator);
    }

    @Test
    void obterRealizado_semEscopo_retornaVazio() {
        AccessContextDTO ctx = new AccessContextDTO(
            true, true, false, Set.of(), MotivoNegacaoAcesso.SEM_NO_ORGANOGRAMA, 1L, "Nó", 2);

        List<OrcamentoCentroCustoDTO> result = adapter.obterRealizadoPorCentroCusto(ctx, COMPETENCIA);

        assertTrue(result.isEmpty());
        verify(folhaConsultaPort, never()).findLinhasAtivasPorCompetencia(any(), any(), eq(false), any());
    }

    @Test
    void obterRealizado_semResumoAtivo_retornaVazio() {
        AccessContextDTO ctx = acessoTotal();
        when(folhaConsultaPort.existsResumoAtivo(any(), any(), eq(false))).thenReturn(false);

        List<OrcamentoCentroCustoDTO> result = adapter.obterRealizadoPorCentroCusto(ctx, COMPETENCIA);

        assertTrue(result.isEmpty());
    }

    @Test
    void obterRealizado_comDados_agregaPorCentroCusto() {
        AccessContextDTO ctx = acessoScoped(Set.of(10L));
        LocalDate inicio = COMPETENCIA.atDay(1);
        LocalDate fim = COMPETENCIA.atEndOfMonth();
        when(folhaConsultaPort.existsResumoAtivo(inicio, fim, false)).thenReturn(true);
        List<FolhaLinhaSnapshot> linhas = List.of();
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(inicio, fim, false, Set.of(10L))).thenReturn(linhas);
        when(dashboardStatsAggregator.porCentroCusto(linhas, Integer.MAX_VALUE)).thenReturn(List.of(
            new CentroCustoStatsDTO(10L, "CC Alpha", 3L, new BigDecimal("15000.50"))));

        List<OrcamentoCentroCustoDTO> result = adapter.obterRealizadoPorCentroCusto(ctx, COMPETENCIA);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).centroCustoId());
        assertEquals(new BigDecimal("15000.50"), result.get(0).realizado());
        assertEquals(3L, result.get(0).quantidadeFuncionarios());
    }

    @Test
    void obterRealizado_acessoTotal_consultaSemFiltroCentro() {
        AccessContextDTO ctx = acessoTotal();
        LocalDate inicio = COMPETENCIA.atDay(1);
        LocalDate fim = COMPETENCIA.atEndOfMonth();
        when(folhaConsultaPort.existsResumoAtivo(inicio, fim, false)).thenReturn(true);
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(inicio, fim, false, null)).thenReturn(List.of());
        when(dashboardStatsAggregator.porCentroCusto(any(), anyInt())).thenReturn(List.of());

        adapter.obterRealizadoPorCentroCusto(ctx, COMPETENCIA);

        verify(folhaConsultaPort).findLinhasAtivasPorCompetencia(inicio, fim, false, null);
    }

    @Test
    void consolidarHierarquia_mapeiaNodesFlat() {
        AccessContextDTO ctx = acessoTotal();
        LocalDate inicio = COMPETENCIA.atDay(1);
        LocalDate fim = COMPETENCIA.atEndOfMonth();
        when(folhaConsultaPort.existsResumoAtivo(inicio, fim, false)).thenReturn(true);
        when(folhaConsultaPort.findLinhasAtivasPorCompetencia(inicio, fim, false, null)).thenReturn(List.of());
        when(dashboardStatsAggregator.porCentroCusto(any(), anyInt())).thenReturn(List.of(
            new CentroCustoStatsDTO(5L, "CC Beta", 1L, new BigDecimal("1000"))));

        List<OrcamentoConsultaPort.OrcamentoNodeDTO> nodes =
            adapter.consolidarHierarquia(ctx, COMPETENCIA);

        assertEquals(1, nodes.size());
        assertEquals("CC Beta", nodes.get(0).noNome());
        assertTrue(nodes.get(0).filhos().isEmpty());
    }

    @Test
    void obterRealizado_ctxNull_retornaVazio() {
        assertTrue(adapter.obterRealizadoPorCentroCusto(null, COMPETENCIA).isEmpty());
    }

    private AccessContextDTO acessoTotal() {
        return new AccessContextDTO(true, true, true, Set.of(), null, 1L, "Raiz", 0);
    }

    private AccessContextDTO acessoScoped(Set<Long> centros) {
        return new AccessContextDTO(true, true, false, centros, null, 2L, "Gestor", 3);
    }
}
