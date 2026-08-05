package br.com.techne.sistemafolha.dashboard.application;

import br.com.techne.sistemafolha.dashboard.api.WidgetDataDTO;
import br.com.techne.sistemafolha.dashboard.api.WidgetQueryParams;
import br.com.techne.sistemafolha.dashboard.domain.DashboardAcessoNegadoException;
import br.com.techne.sistemafolha.dashboard.domain.WidgetCatalog;
import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import br.com.techne.sistemafolha.folha.port.FolhaResumoSnapshot;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DashboardWidgetQueryService {

    static final int TOP_N_MIN = 1;
    static final int TOP_N_MAX = 50;
    static final int MESES_MIN = 1;
    static final int MESES_MAX = 12;

    private static final Set<String> DIMENSOES = Set.of("CENTRO_CUSTO", "LINHA_NEGOCIO", "CARGO");
    private static final Set<String> METRICAS = Set.of("FUNCIONARIOS", "CUSTO");
    private static final Set<String> TIPOS_VISUALIZACAO = Set.of("PIE", "BAR");

    private final DashboardAccessGuard dashboardAccessGuard;
    private final DashboardWidgetCatalogService dashboardWidgetCatalogService;
    private final DashboardStatsAggregator dashboardStatsAggregator;
    private final FolhaConsultaPort folhaConsultaPort;

    public WidgetDataDTO consultar(String login, String widgetId, WidgetQueryParams params) {
        dashboardAccessGuard.assertEscopo(login);
        if (!dashboardWidgetCatalogService.isWidgetPermitido(login, widgetId)) {
            throw new DashboardAcessoNegadoException();
        }

        WidgetCatalog catalogEntry = WidgetCatalog.findByWidgetId(widgetId)
            .orElseThrow(() -> new IllegalArgumentException("widgetId inválido: " + widgetId));

        validarParamsPorWidget(catalogEntry, params);

        DashboardAccessGuard.ResolvedDashboardAccess access = dashboardAccessGuard.resolve(login);
        validarEscopoFiltros(access, params);

        CompetenciaResolvida competencia = resolverCompetencia(params.competencia());
        if (competencia.semDados()) {
            return WidgetDataDTO.semDados(widgetId, competencia.competenciaLabel());
        }

        List<FolhaLinhaSnapshot> linhas = dashboardStatsAggregator.linhasCompetencia(
            access.centrosScoped(),
            competencia.inicio(),
            competencia.fim(),
            competencia.decimoTerceiro());
        linhas = filtrarLinhas(linhas, params);

        return mapearDados(
            widgetId,
            competencia.competenciaLabel(),
            access,
            linhas,
            competencia,
            catalogEntry,
            params);
    }

    private void validarParamsPorWidget(WidgetCatalog entry, WidgetQueryParams params) {
        validarParamPermitido(entry, "competencia", params.competencia() != null);
        validarParamPermitido(entry, "topN", params.topN() != null);
        validarParamPermitido(entry, "dimensao", params.dimensao() != null);
        validarParamPermitido(entry, "metrica", params.metrica() != null);
        validarParamPermitido(entry, "tipoVisualizacao", params.tipoVisualizacao() != null);
        validarParamPermitido(entry, "centroCustoId", params.centroCustoId() != null);
        validarParamPermitido(entry, "linhaNegocioId", params.linhaNegocioId() != null);
        validarParamPermitido(entry, "quantidadeMeses", params.quantidadeMeses() != null);

        if (params.topN() != null && (params.topN() < TOP_N_MIN || params.topN() > TOP_N_MAX)) {
            throw new IllegalArgumentException("topN deve estar entre 1 e 50");
        }
        if (params.quantidadeMeses() != null
            && (params.quantidadeMeses() < MESES_MIN || params.quantidadeMeses() > MESES_MAX)) {
            throw new IllegalArgumentException("quantidadeMeses deve estar entre 1 e 12");
        }
        if (params.dimensao() != null && !DIMENSOES.contains(params.dimensao())) {
            throw new IllegalArgumentException("dimensao inválida");
        }
        if (params.metrica() != null && !METRICAS.contains(params.metrica())) {
            throw new IllegalArgumentException("metrica inválida");
        }
        if (params.tipoVisualizacao() != null && !TIPOS_VISUALIZACAO.contains(params.tipoVisualizacao())) {
            throw new IllegalArgumentException("tipoVisualizacao inválido");
        }
        if (params.competencia() != null) {
            parseCompetencia(params.competencia());
        }
    }

    private void validarParamPermitido(WidgetCatalog entry, String param, boolean presente) {
        if (!presente) {
            return;
        }
        if (!parametrosPermitidos(entry).contains(param)) {
            throw new IllegalArgumentException("Parâmetro não permitido para widget: " + param);
        }
    }

    private Set<String> parametrosPermitidos(WidgetCatalog entry) {
        return switch (entry) {
            case KPI_TOTAL_FUNCIONARIOS, KPI_CUSTO_EMPRESA, KPI_BENEFICIOS_ATIVOS, KPI_RELACAO_PD ->
                Set.of("competencia", "centroCustoId", "linhaNegocioId");
            case GRAFICO_EVOLUCAO_MENSAL ->
                Set.of("competencia", "quantidadeMeses", "centroCustoId", "linhaNegocioId");
            case GRAFICO_FUNCIONARIOS_POR_CC, GRAFICO_CUSTO_POR_CC ->
                Set.of("competencia", "topN", "dimensao", "metrica", "tipoVisualizacao", "centroCustoId", "linhaNegocioId");
            case GRAFICO_FUNCIONARIOS_POR_LINHA, GRAFICO_CUSTO_POR_LINHA ->
                Set.of("competencia", "topN", "dimensao", "metrica", "tipoVisualizacao", "linhaNegocioId", "centroCustoId");
            case LISTA_TOP_PROVENTOS, LISTA_TOP_DESCONTOS ->
                Set.of("competencia", "topN", "centroCustoId", "linhaNegocioId");
            case GRAFICO_FUNCIONARIOS_POR_CARGO ->
                Set.of("competencia", "topN", "centroCustoId", "linhaNegocioId");
        };
    }

    private void validarEscopoFiltros(DashboardAccessGuard.ResolvedDashboardAccess access, WidgetQueryParams params) {
        if (access.contexto().acessoTotal()) {
            return;
        }
        Set<Long> centros = access.centrosScoped();
        if (params.centroCustoId() != null && (centros == null || !centros.contains(params.centroCustoId()))) {
            throw new DashboardAcessoNegadoException();
        }
    }

    private CompetenciaResolvida resolverCompetencia(String competenciaParam) {
        if (competenciaParam != null) {
            YearMonth ym = parseCompetencia(competenciaParam);
            LocalDate inicio = ym.atDay(1);
            LocalDate fim = ym.atEndOfMonth();
            boolean existe = folhaConsultaPort.existsResumoAtivo(inicio, fim, false);
            String label = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            return new CompetenciaResolvida(inicio, fim, false, label, !existe);
        }

        Optional<FolhaResumoSnapshot> resumo = folhaConsultaPort.findResumoMaisRecente();
        if (resumo.isEmpty()) {
            String label = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            return new CompetenciaResolvida(null, null, false, label, true);
        }

        FolhaResumoSnapshot snap = resumo.get();
        String label = YearMonth.from(snap.competenciaInicio()).format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return new CompetenciaResolvida(
            snap.competenciaInicio(), snap.competenciaFim(), snap.decimoTerceiro(), label, false);
    }

    private YearMonth parseCompetencia(String competencia) {
        try {
            return YearMonth.parse(competencia, DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("competencia deve estar no formato yyyy-MM");
        }
    }

    private WidgetDataDTO mapearDados(
            String widgetId,
            String competencia,
            DashboardAccessGuard.ResolvedDashboardAccess access,
            List<FolhaLinhaSnapshot> linhas,
            CompetenciaResolvida competenciaResolvida,
            WidgetCatalog entry,
            WidgetQueryParams params) {

        int topN = params.topN() != null ? params.topN() : topNPadrao(entry);
        int meses = params.quantidadeMeses() != null ? params.quantidadeMeses() : 12;
        AccessContextDTO contexto = access.contexto();

        return switch (entry) {
            case KPI_TOTAL_FUNCIONARIOS -> new WidgetDataDTO(
                widgetId, competencia, false,
                dashboardStatsAggregator.contarFuncionarios(linhas),
                null, null, null, null, null, null, null, null, null, null);
            case KPI_CUSTO_EMPRESA -> new WidgetDataDTO(
                widgetId, competencia, false, null,
                dashboardStatsAggregator.calcularCustoEmpresa(
                    linhas, competenciaResolvida.inicio(), competenciaResolvida.fim(), contexto),
                null, null, null, null, null, null, null, null, null);
            case KPI_BENEFICIOS_ATIVOS -> new WidgetDataDTO(
                widgetId, competencia, false, null, null,
                dashboardStatsAggregator.contarBeneficiosAtivos(
                    competenciaResolvida.inicio(), competenciaResolvida.fim(), access.centrosScoped()),
                null, null, null, null, null, null, null, null);
            case KPI_RELACAO_PD -> new WidgetDataDTO(
                widgetId, competencia, false, null, null, null,
                dashboardStatsAggregator.calcularTotalProventos(linhas),
                dashboardStatsAggregator.calcularTotalDescontos(linhas),
                null, null, null, null, null, null);
            case GRAFICO_EVOLUCAO_MENSAL -> new WidgetDataDTO(
                widgetId, competencia, false, null, null, null, null, null, null, null, null, null, null,
                dashboardStatsAggregator.evolucaoMeses(
                    contexto, access.centrosScoped(), competenciaResolvida.fim(), meses, competenciaResolvida.decimoTerceiro()));
            case GRAFICO_FUNCIONARIOS_POR_CC, GRAFICO_CUSTO_POR_CC -> new WidgetDataDTO(
                widgetId, competencia, false, null, null, null, null, null, null,
                dashboardStatsAggregator.porCentroCusto(linhas, topN), null, null, null, null);
            case GRAFICO_FUNCIONARIOS_POR_LINHA, GRAFICO_CUSTO_POR_LINHA -> new WidgetDataDTO(
                widgetId, competencia, false, null, null, null, null, null,
                dashboardStatsAggregator.porLinhaNegocio(linhas, topN), null, null, null, null, null);
            case LISTA_TOP_PROVENTOS -> new WidgetDataDTO(
                widgetId, competencia, false, null, null, null, null, null, null, null, null,
                dashboardStatsAggregator.topProventos(linhas, topN), null, null);
            case LISTA_TOP_DESCONTOS -> new WidgetDataDTO(
                widgetId, competencia, false, null, null, null, null, null, null, null, null, null,
                dashboardStatsAggregator.topDescontos(linhas, topN), null);
            case GRAFICO_FUNCIONARIOS_POR_CARGO -> new WidgetDataDTO(
                widgetId, competencia, false, null, null, null, null, null, null, null,
                dashboardStatsAggregator.porCargo(linhas, topN), null, null, null);
        };
    }

    private List<FolhaLinhaSnapshot> filtrarLinhas(List<FolhaLinhaSnapshot> linhas, WidgetQueryParams params) {
        List<FolhaLinhaSnapshot> filtradas = linhas;
        if (params.centroCustoId() != null) {
            filtradas = filtradas.stream()
                .filter(l -> params.centroCustoId().equals(l.centroCustoId()))
                .toList();
        }
        if (params.linhaNegocioId() != null) {
            filtradas = filtradas.stream()
                .filter(l -> params.linhaNegocioId().equals(l.linhaNegocioId()))
                .toList();
        }
        return filtradas;
    }

    private int topNPadrao(WidgetCatalog entry) {
        return switch (entry) {
            case GRAFICO_FUNCIONARIOS_POR_CC -> 5;
            case GRAFICO_FUNCIONARIOS_POR_LINHA, GRAFICO_CUSTO_POR_CC, GRAFICO_CUSTO_POR_LINHA,
                 GRAFICO_FUNCIONARIOS_POR_CARGO -> 6;
            case LISTA_TOP_PROVENTOS, LISTA_TOP_DESCONTOS -> 5;
            default -> TOP_N_MAX;
        };
    }

    private record CompetenciaResolvida(
        LocalDate inicio,
        LocalDate fim,
        boolean decimoTerceiro,
        String competenciaLabel,
        boolean semDados
    ) {}
}
