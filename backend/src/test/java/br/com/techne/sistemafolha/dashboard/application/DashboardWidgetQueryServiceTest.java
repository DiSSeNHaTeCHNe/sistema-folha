package br.com.techne.sistemafolha.dashboard.application;

import br.com.techne.sistemafolha.dashboard.api.CentroCustoStatsDTO;
import br.com.techne.sistemafolha.dashboard.api.DashboardStatsDTO;
import br.com.techne.sistemafolha.dashboard.api.WidgetDataDTO;
import br.com.techne.sistemafolha.dashboard.api.WidgetQueryParams;
import br.com.techne.sistemafolha.dashboard.domain.DashboardAcessoNegadoException;
import br.com.techne.sistemafolha.dashboard.domain.WidgetCatalog;
import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaResumoSnapshot;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardWidgetQueryServiceTest {

    private static final LocalDate COMPETENCIA_INICIO = LocalDate.of(2024, 6, 1);
    private static final LocalDate COMPETENCIA_FIM = LocalDate.of(2024, 6, 30);

    @Mock
    private DashboardAccessGuard dashboardAccessGuard;

    @Mock
    private DashboardWidgetCatalogService dashboardWidgetCatalogService;

    @Mock
    private DashboardWidgetConfigValidator dashboardWidgetConfigValidator;

    @Mock
    private DashboardStatsAggregator dashboardStatsAggregator;

    @Mock
    private FolhaConsultaPort folhaConsultaPort;

    private DashboardWidgetQueryService service;

    @BeforeEach
    void setUp() {
        service = new DashboardWidgetQueryService(
            dashboardAccessGuard,
            dashboardWidgetCatalogService,
            dashboardWidgetConfigValidator,
            dashboardStatsAggregator,
            folhaConsultaPort
        );
    }

    @Test
    void consultar_paramDesconhecido_retorna400() {
        assertThrows(IllegalArgumentException.class, () ->
            WidgetQueryParams.fromQueryMap(Map.of("foo", "bar")));
    }

    @Test
    void consultar_topNForaDoIntervalo_retorna400() {
        when(dashboardWidgetCatalogService.isWidgetPermitido("gestor", "lista-top-proventos")).thenReturn(true);
        doThrow(new IllegalArgumentException("topN deve estar entre 1 e 50"))
            .when(dashboardWidgetConfigValidator)
            .validarParamsPorWidget(eq(WidgetCatalog.LISTA_TOP_PROVENTOS), any());

        WidgetQueryParams params = WidgetQueryParams.fromQueryMap(Map.of("topN", "99"));

        assertThrows(IllegalArgumentException.class, () ->
            service.consultar("gestor", "lista-top-proventos", params));
    }

    @Test
    void consultar_paramNaoPermitidoParaWidget_retorna400() {
        when(dashboardWidgetCatalogService.isWidgetPermitido("gestor", "kpi-total-funcionarios")).thenReturn(true);
        doThrow(new IllegalArgumentException("Parâmetro não permitido para widget: topN"))
            .when(dashboardWidgetConfigValidator)
            .validarParamsPorWidget(eq(WidgetCatalog.KPI_TOTAL_FUNCIONARIOS), any());

        WidgetQueryParams params = WidgetQueryParams.fromQueryMap(Map.of("topN", "5"));

        assertThrows(IllegalArgumentException.class, () ->
            service.consultar("gestor", "kpi-total-funcionarios", params));
    }

    @Test
    void consultar_widgetForaDoCatalogo_retorna403() {
        when(dashboardWidgetCatalogService.isWidgetPermitido("gestor", "grafico-funcionarios-por-cargo"))
            .thenReturn(false);

        assertThrows(DashboardAcessoNegadoException.class, () ->
            service.consultar("gestor", "grafico-funcionarios-por-cargo", new WidgetQueryParams(
                null, null, null, null, null, null, null, null)));
    }

    @Test
    void consultar_centroCustoForaDoEscopo_retorna403() {
        AccessContextDTO contexto = new AccessContextDTO(
            true, true, false, Set.of(10L), null, 2L, "TI", 1);
        when(dashboardAccessGuard.resolve("scoped")).thenReturn(
            new DashboardAccessGuard.ResolvedDashboardAccess(false, 2L, contexto, Set.of(10L)));
        when(dashboardWidgetCatalogService.isWidgetPermitido("scoped", "kpi-total-funcionarios")).thenReturn(true);
        doThrow(new DashboardAcessoNegadoException())
            .when(dashboardWidgetConfigValidator)
            .validarEscopoFiltros(any(), any());

        WidgetQueryParams params = WidgetQueryParams.fromQueryMap(Map.of("centroCustoId", "99"));

        assertThrows(DashboardAcessoNegadoException.class, () ->
            service.consultar("scoped", "kpi-total-funcionarios", params));
    }

    @Test
    void consultar_competenciaSemFolha_retornaSemDados() {
        mockAcessoTotal("gestor");
        when(dashboardWidgetCatalogService.isWidgetPermitido("gestor", "kpi-total-funcionarios")).thenReturn(true);
        when(folhaConsultaPort.existsResumoAtivo(COMPETENCIA_INICIO, COMPETENCIA_FIM, false)).thenReturn(false);

        WidgetQueryParams params = WidgetQueryParams.fromQueryMap(Map.of("competencia", "2024-06"));
        WidgetDataDTO result = service.consultar("gestor", "kpi-total-funcionarios", params);

        assertTrue(result.semDados());
        assertEquals("2024-06", result.competencia());
        verify(dashboardStatsAggregator, never()).linhasCompetencia(any(), any(), any(), eq(false));
    }

    @Test
    void consultar_acessoTotal_retornaKpi() {
        mockAcessoTotal("gestor");
        when(dashboardWidgetCatalogService.isWidgetPermitido("gestor", "kpi-total-funcionarios")).thenReturn(true);
        when(folhaConsultaPort.findResumoMaisRecente()).thenReturn(Optional.of(resumoAtual()));
        when(dashboardStatsAggregator.linhasCompetencia(any(), any(), any(), eq(false)))
            .thenReturn(List.of());
        when(dashboardStatsAggregator.contarFuncionarios(any())).thenReturn(42L);

        WidgetDataDTO result = service.consultar("gestor", "kpi-total-funcionarios", new WidgetQueryParams(
            null, null, null, null, null, null, null, null));

        assertEquals(42L, result.totalFuncionarios());
        assertEquals(false, result.semDados());
    }

    @Test
    void consultar_listaTopProventos_respeitaTopN() {
        mockAcessoTotal("gestor");
        when(dashboardWidgetCatalogService.isWidgetPermitido("gestor", "lista-top-proventos")).thenReturn(true);
        when(folhaConsultaPort.findResumoMaisRecente()).thenReturn(Optional.of(resumoAtual()));
        when(dashboardStatsAggregator.linhasCompetencia(any(), any(), any(), eq(false)))
            .thenReturn(List.of());
        when(dashboardStatsAggregator.topProventos(any(), eq(3)))
            .thenReturn(List.of(
                rubrica(0), rubrica(1), rubrica(2)));

        WidgetQueryParams params = WidgetQueryParams.fromQueryMap(Map.of("topN", "3"));
        WidgetDataDTO result = service.consultar("gestor", "lista-top-proventos", params);

        assertEquals(3, result.topProventos().size());
    }

    @Test
    void consultar_semEscopo_lanca403() {
        doThrow(new DashboardAcessoNegadoException()).when(dashboardAccessGuard).assertEscopo("negado");

        assertThrows(DashboardAcessoNegadoException.class, () ->
            service.consultar("negado", "kpi-total-funcionarios", new WidgetQueryParams(
                null, null, null, null, null, null, null, null)));
    }

    private void mockAcessoTotal(String login) {
        AccessContextDTO contexto = new AccessContextDTO(true, true, true, Set.of(), null, 1L, "Raiz", 0);
        when(dashboardAccessGuard.resolve(login)).thenReturn(
            new DashboardAccessGuard.ResolvedDashboardAccess(false, 1L, contexto, null));
    }

    private FolhaResumoSnapshot resumoAtual() {
        return new FolhaResumoSnapshot(
            COMPETENCIA_INICIO, COMPETENCIA_FIM, BigDecimal.ZERO, 0, false, BigDecimal.ZERO);
    }

    private br.com.techne.sistemafolha.dashboard.api.RubricaStatsDTO rubrica(int i) {
        return new br.com.techne.sistemafolha.dashboard.api.RubricaStatsDTO(
            (long) i, "00" + i, "Rubrica " + i, BigDecimal.TEN, 1L);
    }
}
