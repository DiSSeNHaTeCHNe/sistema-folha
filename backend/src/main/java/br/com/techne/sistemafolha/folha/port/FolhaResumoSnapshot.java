package br.com.techne.sistemafolha.folha.port;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FolhaResumoSnapshot(
    LocalDate competenciaInicio,
    LocalDate competenciaFim,
    BigDecimal totalLiquido,
    Integer totalEmpregados,
    boolean decimoTerceiro,
    BigDecimal totalEncargos
) {}
