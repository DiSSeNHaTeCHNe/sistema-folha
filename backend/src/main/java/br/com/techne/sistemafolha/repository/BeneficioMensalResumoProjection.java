package br.com.techne.sistemafolha.repository;

import java.math.BigDecimal;

public interface BeneficioMensalResumoProjection {
    String getCodigo();
    String getDescricao();
    BigDecimal getTotal();
    Long getQtdLancamentos();
}
