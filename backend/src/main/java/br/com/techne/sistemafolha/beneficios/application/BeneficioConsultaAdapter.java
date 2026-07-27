package br.com.techne.sistemafolha.beneficios.application;

import br.com.techne.sistemafolha.beneficios.port.BeneficioConsultaPort;
import br.com.techne.sistemafolha.beneficios.domain.BeneficioMensal;
import br.com.techne.sistemafolha.beneficios.infrastructure.BeneficioMensalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BeneficioConsultaAdapter implements BeneficioConsultaPort {

    private final BeneficioMensalRepository beneficioMensalRepository;

    @Override
    public BigDecimal somarValorPorFuncionarioECompetencia(
            Long funcionarioId, LocalDate competenciaInicio, LocalDate competenciaFim) {
        validarFuncionarioECompetencia(funcionarioId, competenciaInicio, competenciaFim);
        return beneficioMensalRepository
            .findByFuncionarioIdAndCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                funcionarioId, competenciaInicio, competenciaFim)
            .stream()
            .map(BeneficioMensal::getValor)
            .filter(v -> v != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public int contarLancamentosPorFuncionarioECompetencia(
            Long funcionarioId, LocalDate competenciaInicio, LocalDate competenciaFim) {
        validarFuncionarioECompetencia(funcionarioId, competenciaInicio, competenciaFim);
        return beneficioMensalRepository
            .findByFuncionarioIdAndCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                funcionarioId, competenciaInicio, competenciaFim)
            .size();
    }

    @Override
    public boolean existeDadosMensaisNaCompetencia(LocalDate competenciaInicio, LocalDate competenciaFim) {
        validarCompetencia(competenciaInicio, competenciaFim);
        return beneficioMensalRepository.existsByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
            competenciaInicio, competenciaFim);
    }

    @Override
    public long contarLancamentosAtivosNaCompetencia(LocalDate competenciaInicio, LocalDate competenciaFim) {
        validarCompetencia(competenciaInicio, competenciaFim);
        return beneficioMensalRepository
            .findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(competenciaInicio, competenciaFim)
            .size();
    }

    @Override
    public long contarLancamentosAtivosNaCompetenciaPorCentros(
            LocalDate competenciaInicio, LocalDate competenciaFim, Set<Long> centrosCustoIds) {
        validarCompetencia(competenciaInicio, competenciaFim);
        if (centrosCustoIds == null || centrosCustoIds.isEmpty()) {
            return 0L;
        }
        return beneficioMensalRepository
            .findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(competenciaInicio, competenciaFim)
            .stream()
            .filter(b -> b.getFuncionario() != null
                && b.getFuncionario().getCentroCusto() != null
                && centrosCustoIds.contains(b.getFuncionario().getCentroCusto().getId()))
            .count();
    }

    private void validarFuncionarioECompetencia(
            Long funcionarioId, LocalDate competenciaInicio, LocalDate competenciaFim) {
        if (funcionarioId == null) {
            throw new IllegalArgumentException("funcionarioId não pode ser nulo");
        }
        validarCompetencia(competenciaInicio, competenciaFim);
    }

    private void validarCompetencia(LocalDate competenciaInicio, LocalDate competenciaFim) {
        if (competenciaInicio == null || competenciaFim == null) {
            throw new IllegalArgumentException("competência não pode ser nula");
        }
    }
}
