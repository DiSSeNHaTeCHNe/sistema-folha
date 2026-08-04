package br.com.techne.sistemafolha.dashboard.api;

public record WidgetCatalogItemDTO(
    String widgetId,
    String titulo,
    String descricao,
    String categoria,
    Integer colSpanPadrao,
    Integer rowSpanPadrao
) {}
