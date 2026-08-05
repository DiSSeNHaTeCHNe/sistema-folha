package br.com.techne.sistemafolha.dashboard.api;

import java.math.BigDecimal;
import java.util.List;

public record WidgetDataDTO(
    String widgetId,
    String competencia,
    boolean semDados,
    Long totalFuncionarios,
    BigDecimal custoMensalFolha,
    Long totalBeneficiosAtivos,
    BigDecimal totalProventos,
    BigDecimal totalDescontos,
    List<LinhaNegocioStatsDTO> porLinhaNegocio,
    List<CentroCustoStatsDTO> porCentroCusto,
    List<CargoStatsDTO> porCargo,
    List<RubricaStatsDTO> topProventos,
    List<RubricaStatsDTO> topDescontos,
    List<EvolucaoMensalDTO> evolucaoMensal
) {

    public static WidgetDataDTO semDados(String widgetId, String competencia) {
        return new WidgetDataDTO(
            widgetId, competencia, true,
            null, null, null, null, null,
            null, null, null, null, null, null
        );
    }
}
