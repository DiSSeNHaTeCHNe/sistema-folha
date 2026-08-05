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

@Service
@RequiredArgsConstructor
public class DashboardWidgetQueryService {

    private final DashboardAccessGuard dashboardAccessGuard;
    private final DashboardWidgetCatalogService dashboardWidgetCatalogService;
    private final DashboardWidgetConfigValidator dashboardWidgetConfigValidator;
    private final DashboardStatsAggregator dashboardStatsAggregator;
    private final FolhaConsultaPort folhaConsultaPort;

    public WidgetDataDTO consultar(String login, String widgetId, WidgetQueryParams params) {
        dashboardAccessGuard.assertEscopo(login);
        if (!dashboardWidgetCatalogService.isWidgetPermitido(login, widgetId)) {
            throw new DashboardAcessoNegadoException();
        }

        WidgetCatalog catalogEntry = WidgetCatalog.findByWidgetId(widgetId)
            .orElseThrow(() -> new IllegalArgumentException("widgetId inválido: " + widgetId));

        dashboardWidgetConfigValidator.validarParamsPorWidget(catalogEntry, params);

        DashboardAccessGuard.ResolvedDashboardAccess access = dashboardAccessGuard.resolve(login);
        dashboardWidgetConfigValidator.validarEscopoFiltros(access, params);

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
            default -> DashboardWidgetConfigValidator.TOP_N_MAX;
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
