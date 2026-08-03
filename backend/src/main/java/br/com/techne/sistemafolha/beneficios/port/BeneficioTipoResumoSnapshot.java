package br.com.techne.sistemafolha.beneficios.port;

import java.math.BigDecimal;

public record BeneficioTipoResumoSnapshot(
    Long tipoBeneficioId,
    String codigo,
    String descricao,
    BigDecimal total,
    long qtdLancamentos
) {}
