package br.com.techne.sistemafolha.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FolhaTotaisFuncionarioDTO(
    Long funcionarioId,
    String funcionarioNome,
    LocalDate competenciaInicio,
    LocalDate competenciaFim,
    Long cargoId,
    String cargoDescricao,
    Long centroCustoId,
    String centroCustoDescricao,
    Long linhaNegocioId,
    String linhaNegocioDescricao,
    int totalRubricas,
    int totalBeneficios,
    BigDecimal salBruto,
    BigDecimal salLiquido,
    BigDecimal salCustoFolha,
    BigDecimal salCustoBeneficios,
    BigDecimal salCustoTechne
) {}
