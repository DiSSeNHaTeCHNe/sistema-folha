package br.com.techne.sistemafolha.folha.port;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FolhaEvolucaoSnapshot(
    LocalDate competenciaInicio,
    BigDecimal totalLiquido,
    Integer totalEmpregados
) {}
