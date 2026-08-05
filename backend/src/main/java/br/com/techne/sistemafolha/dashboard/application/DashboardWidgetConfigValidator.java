package br.com.techne.sistemafolha.dashboard.application;

import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.port.CadastrosLookupPort;
import br.com.techne.sistemafolha.dashboard.api.WidgetQueryParams;
import br.com.techne.sistemafolha.dashboard.domain.DashboardAcessoNegadoException;
import br.com.techne.sistemafolha.dashboard.domain.WidgetCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DashboardWidgetConfigValidator {

    static final int TOP_N_MIN = 1;
    static final int TOP_N_MAX = 50;
    static final int MESES_MIN = 1;
    static final int MESES_MAX = 12;

    private static final Set<String> CONFIG_KEYS = Set.of(
        "competencia", "topN", "dimensao", "metrica", "tipoVisualizacao",
        "centroCustoId", "linhaNegocioId", "quantidadeMeses"
    );
    private static final Set<String> DIMENSOES = Set.of("CENTRO_CUSTO", "LINHA_NEGOCIO", "CARGO");
    private static final Set<String> METRICAS = Set.of("FUNCIONARIOS", "CUSTO");
    private static final Set<String> TIPOS_VISUALIZACAO = Set.of("PIE", "BAR");

    private final CadastrosLookupPort cadastrosLookupPort;

    public void validar(String widgetId, Map<String, Object> config, DashboardAccessGuard.ResolvedDashboardAccess access) {
        if (config == null || config.isEmpty()) {
            return;
        }
        WidgetCatalog entry = WidgetCatalog.findByWidgetId(widgetId)
            .orElseThrow(() -> new IllegalArgumentException("widgetId inválido: " + widgetId));

        for (String key : config.keySet()) {
            if (!CONFIG_KEYS.contains(key)) {
                throw new IllegalArgumentException("Parâmetro não permitido: " + key);
            }
        }

        WidgetQueryParams params = toParams(config);
        validarParamsPorWidget(entry, params);
        validarEscopoFiltros(access, params);
    }

    void validarParamsPorWidget(WidgetCatalog entry, WidgetQueryParams params) {
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

    void validarEscopoFiltros(DashboardAccessGuard.ResolvedDashboardAccess access, WidgetQueryParams params) {
        if (access.contexto().acessoTotal()) {
            return;
        }
        Set<Long> centros = access.centrosScoped();
        if (params.centroCustoId() != null && (centros == null || !centros.contains(params.centroCustoId()))) {
            throw new DashboardAcessoNegadoException();
        }
        if (params.linhaNegocioId() != null) {
            Set<Long> linhasPermitidas = linhasNegocioNoEscopo(centros);
            if (!linhasPermitidas.contains(params.linhaNegocioId())) {
                throw new DashboardAcessoNegadoException();
            }
        }
    }

    private Set<Long> linhasNegocioNoEscopo(Set<Long> centrosScoped) {
        Set<Long> linhas = new HashSet<>();
        if (centrosScoped == null) {
            return linhas;
        }
        for (Long centroId : centrosScoped) {
            cadastrosLookupPort.findCentroCustoById(centroId)
                .map(CentroCusto::getLinhaNegocio)
                .ifPresent(ln -> linhas.add(ln.getId()));
        }
        return linhas;
    }

    private WidgetQueryParams toParams(Map<String, Object> config) {
        return new WidgetQueryParams(
            asString(config.get("competencia")),
            asInteger(config.get("topN")),
            asString(config.get("dimensao")),
            asString(config.get("metrica")),
            asString(config.get("tipoVisualizacao")),
            asLong(config.get("centroCustoId")),
            asLong(config.get("linhaNegocioId")),
            asInteger(config.get("quantidadeMeses"))
        );
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(value.toString().trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Valor inválido para parâmetro numérico");
        }
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(value.toString().trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Valor inválido para parâmetro numérico");
        }
    }

    private YearMonth parseCompetencia(String competencia) {
        try {
            return YearMonth.parse(competencia, DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("competencia deve estar no formato yyyy-MM");
        }
    }
}
