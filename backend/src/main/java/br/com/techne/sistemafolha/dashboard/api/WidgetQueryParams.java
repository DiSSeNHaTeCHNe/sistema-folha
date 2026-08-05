package br.com.techne.sistemafolha.dashboard.api;

import java.util.Map;
import java.util.Set;

public record WidgetQueryParams(
    String competencia,
    Integer topN,
    String dimensao,
    String metrica,
    String tipoVisualizacao,
    Long centroCustoId,
    Long linhaNegocioId,
    Integer quantidadeMeses
) {

    private static final Set<String> PARAMS_GLOBAIS = Set.of(
        "competencia", "topN", "dimensao", "metrica", "tipoVisualizacao",
        "centroCustoId", "linhaNegocioId", "quantidadeMeses"
    );

    public static WidgetQueryParams fromQueryMap(Map<String, String> query) {
        for (String key : query.keySet()) {
            if (!PARAMS_GLOBAIS.contains(key)) {
                throw new IllegalArgumentException("Parâmetro não permitido: " + key);
            }
        }
        return new WidgetQueryParams(
            blankToNull(query.get("competencia")),
            parseInteger(query.get("topN")),
            blankToNull(query.get("dimensao")),
            blankToNull(query.get("metrica")),
            blankToNull(query.get("tipoVisualizacao")),
            parseLong(query.get("centroCustoId")),
            parseLong(query.get("linhaNegocioId")),
            parseInteger(query.get("quantidadeMeses"))
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Valor inválido para parâmetro numérico");
        }
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Valor inválido para parâmetro numérico");
        }
    }
}
