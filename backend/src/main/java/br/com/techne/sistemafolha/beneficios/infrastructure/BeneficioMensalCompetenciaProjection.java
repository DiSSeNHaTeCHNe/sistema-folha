package br.com.techne.sistemafolha.beneficios.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface BeneficioMensalCompetenciaProjection {
    LocalDate getCompetenciaInicio();
    LocalDate getCompetenciaFim();
    Long getTotalFuncionarios();
    BigDecimal getTotalBeneficios();
    Long getQtdLancamentos();
}
