package br.com.techne.sistemafolha.beneficios.infrastructure;

import java.math.BigDecimal;

public interface BeneficioCcTipoProjection {
    Long getCentroCustoId();
    String getCentroCustoDescricao();
    Long getTipoBeneficioId();
    String getTipoCodigo();
    String getTipoDescricao();
    BigDecimal getTotal();
}
