package br.com.techne.sistemafolha.beneficios.port;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public interface BeneficioConsultaPort {

    BigDecimal somarValorPorFuncionarioECompetencia(
        Long funcionarioId, LocalDate competenciaInicio, LocalDate competenciaFim);

    int contarLancamentosPorFuncionarioECompetencia(
        Long funcionarioId, LocalDate competenciaInicio, LocalDate competenciaFim);

    boolean existeDadosMensaisNaCompetencia(LocalDate competenciaInicio, LocalDate competenciaFim);

    long contarLancamentosAtivosNaCompetencia(LocalDate competenciaInicio, LocalDate competenciaFim);

    long contarLancamentosAtivosNaCompetenciaPorCentros(
        LocalDate competenciaInicio, LocalDate competenciaFim, Set<Long> centrosCustoIds);

    Map<Long, BigDecimal> somarValorPorFuncionariosECompetencia(
        Set<Long> funcionarioIds, LocalDate competenciaInicio, LocalDate competenciaFim);

    BigDecimal somarValorPorCompetenciaECentros(
        LocalDate competenciaInicio, LocalDate competenciaFim, Set<Long> centrosCustoIds);
}
