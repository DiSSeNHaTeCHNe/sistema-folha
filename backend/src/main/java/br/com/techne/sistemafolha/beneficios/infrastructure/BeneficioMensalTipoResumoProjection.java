package br.com.techne.sistemafolha.beneficios.infrastructure;

import java.math.BigDecimal;

public interface BeneficioMensalTipoResumoProjection {
    Long getTipoBeneficioId();
    String getCodigo();
    String getDescricao();
    BigDecimal getTotal();
    Long getQtdLancamentos();
}
