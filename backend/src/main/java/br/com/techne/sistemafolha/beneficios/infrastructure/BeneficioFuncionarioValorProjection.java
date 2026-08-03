package br.com.techne.sistemafolha.beneficios.infrastructure;

import java.math.BigDecimal;

public interface BeneficioFuncionarioValorProjection {
    Long getFuncionarioId();
    String getFuncionarioNome();
    BigDecimal getValor();
}
