package br.com.techne.sistemafolha.beneficios.port;

import java.math.BigDecimal;

public record BeneficioCcTipoSnapshot(
    Long centroCustoId,
    String centroCustoDescricao,
    Long tipoBeneficioId,
    String tipoCodigo,
    String tipoDescricao,
    BigDecimal total
) {}
