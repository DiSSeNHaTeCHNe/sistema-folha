package br.com.techne.sistemafolha.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardStatsDTO(
    Long totalFuncionarios,
    BigDecimal custoMensalFolha,
    Long totalBeneficiosAtivos,
    List<LinhaNegocioStatsDTO> porLinhaNegocio,
    List<CentroCustoStatsDTO> porCentroCusto,
    List<CargoStatsDTO> porCargo,
    BigDecimal totalProventos,
    BigDecimal totalDescontos,
    List<RubricaStatsDTO> topProventos,
    List<RubricaStatsDTO> topDescontos,
    List<EvolucaoMensalDTO> evolucaoMensal
) {}

 