package br.com.techne.sistemafolha.beneficios.port;

import java.math.BigDecimal;

public record BeneficioFuncionarioValorSnapshot(
    Long funcionarioId,
    String funcionarioNome,
    BigDecimal valor,
    String centroCustoCodigo,
    String centroCustoDescricao
) {}
