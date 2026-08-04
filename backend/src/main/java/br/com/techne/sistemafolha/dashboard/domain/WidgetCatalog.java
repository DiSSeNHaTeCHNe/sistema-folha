package br.com.techne.sistemafolha.dashboard.domain;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public enum WidgetCatalog {

    KPI_TOTAL_FUNCIONARIOS(
        "kpi-total-funcionarios", "Total de Funcionários",
        "Quantidade de funcionários ativos no escopo", "KPI", 3, 1, true),
    KPI_CUSTO_EMPRESA(
        "kpi-custo-empresa", "Custo Empresa",
        "Custo mensal total da folha no escopo", "KPI", 3, 1, true),
    KPI_BENEFICIOS_ATIVOS(
        "kpi-beneficios-ativos", "Benefícios Ativos",
        "Quantidade de benefícios ativos no escopo", "KPI", 3, 1, true),
    KPI_RELACAO_PD(
        "kpi-relacao-pd", "Relação P/D",
        "Relação entre proventos e descontos", "KPI", 3, 1, true),
    GRAFICO_EVOLUCAO_MENSAL(
        "grafico-evolucao-mensal", "Evolução da Folha",
        "Gráfico de evolução mensal da folha", "GRAFICO", 12, 2, true),
    GRAFICO_FUNCIONARIOS_POR_CC(
        "grafico-funcionarios-por-cc", "Funcionários por Centro de Custo",
        "Distribuição de funcionários por centro de custo", "GRAFICO", 3, 2, true),
    GRAFICO_FUNCIONARIOS_POR_LINHA(
        "grafico-funcionarios-por-linha", "Funcionários por Linha de Negócio",
        "Distribuição de funcionários por linha de negócio", "GRAFICO", 3, 2, true),
    GRAFICO_CUSTO_POR_CC(
        "grafico-custo-por-cc", "Custo por Centro de Custo",
        "Distribuição de custo por centro de custo", "GRAFICO", 3, 2, true),
    GRAFICO_CUSTO_POR_LINHA(
        "grafico-custo-por-linha", "Custo por Linha de Negócio",
        "Distribuição de custo por linha de negócio", "GRAFICO", 3, 2, true),
    LISTA_TOP_PROVENTOS(
        "lista-top-proventos", "Top Proventos",
        "Maiores rubricas de provento", "LISTA", 6, 2, true),
    LISTA_TOP_DESCONTOS(
        "lista-top-descontos", "Top Descontos",
        "Maiores rubricas de desconto", "LISTA", 6, 2, true),
    GRAFICO_FUNCIONARIOS_POR_CARGO(
        "grafico-funcionarios-por-cargo", "Funcionários por Cargo",
        "Distribuição de funcionários por cargo", "GRAFICO", 6, 2, false);

    private final String widgetId;
    private final String titulo;
    private final String descricao;
    private final String categoria;
    private final int colSpanPadrao;
    private final int rowSpanPadrao;
    private final boolean noLayoutPadrao;

    WidgetCatalog(
            String widgetId, String titulo, String descricao, String categoria,
            int colSpanPadrao, int rowSpanPadrao, boolean noLayoutPadrao) {
        this.widgetId = widgetId;
        this.titulo = titulo;
        this.descricao = descricao;
        this.categoria = categoria;
        this.colSpanPadrao = colSpanPadrao;
        this.rowSpanPadrao = rowSpanPadrao;
        this.noLayoutPadrao = noLayoutPadrao;
    }

    public String widgetId() {
        return widgetId;
    }

    public String titulo() {
        return titulo;
    }

    public String descricao() {
        return descricao;
    }

    public String categoria() {
        return categoria;
    }

    public int colSpanPadrao() {
        return colSpanPadrao;
    }

    public int rowSpanPadrao() {
        return rowSpanPadrao;
    }

    public boolean noLayoutPadrao() {
        return noLayoutPadrao;
    }

    public static Optional<WidgetCatalog> findByWidgetId(String widgetId) {
        return Arrays.stream(values())
            .filter(entry -> entry.widgetId.equals(widgetId))
            .findFirst();
    }

    public static Set<String> allWidgetIds() {
        return Arrays.stream(values())
            .map(WidgetCatalog::widgetId)
            .collect(Collectors.toSet());
    }
}
