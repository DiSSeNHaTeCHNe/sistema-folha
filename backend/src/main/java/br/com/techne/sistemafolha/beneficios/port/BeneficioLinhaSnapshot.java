package br.com.techne.sistemafolha.beneficios.port;

import java.math.BigDecimal;

public record BeneficioLinhaSnapshot(
    Long id,
    String tipoCodigo,
    String tipoDescricao,
    BigDecimal valor
) {}
