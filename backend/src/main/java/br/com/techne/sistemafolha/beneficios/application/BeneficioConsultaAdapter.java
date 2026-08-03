package br.com.techne.sistemafolha.beneficios.application;

import br.com.techne.sistemafolha.beneficios.port.BeneficioCcTipoSnapshot;
import br.com.techne.sistemafolha.beneficios.port.BeneficioConsultaPort;
import br.com.techne.sistemafolha.beneficios.port.BeneficioFuncionarioValorSnapshot;
import br.com.techne.sistemafolha.beneficios.port.BeneficioLinhaSnapshot;
import br.com.techne.sistemafolha.beneficios.port.BeneficioTipoResumoSnapshot;
import br.com.techne.sistemafolha.beneficios.domain.BeneficioMensal;
import br.com.techne.sistemafolha.beneficios.infrastructure.BeneficioMensalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        return beneficioMensalRepository.countByCompetenciaECentros(
            competenciaInicio, competenciaFim, centrosCustoIds);
    }

    @Override
    public Map<Long, BigDecimal> somarValorPorFuncionariosECompetencia(
            Set<Long> funcionarioIds, LocalDate competenciaInicio, LocalDate competenciaFim) {
        validarCompetencia(competenciaInicio, competenciaFim);
        if (funcionarioIds == null || funcionarioIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, BigDecimal> resultado = new HashMap<>();
        for (Object[] row : beneficioMensalRepository.sumValorPorFuncionariosECompetencia(
                funcionarioIds, competenciaInicio, competenciaFim)) {
            Long funcionarioId = (Long) row[0];
            BigDecimal total = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            resultado.put(funcionarioId, total);
        }
        return resultado;
    }

    @Override
    public List<BeneficioLinhaSnapshot> findLinhasPorFuncionarioECompetencia(
            Long funcionarioId, LocalDate competenciaInicio, LocalDate competenciaFim) {
        validarFuncionarioECompetencia(funcionarioId, competenciaInicio, competenciaFim);
        return beneficioMensalRepository
            .findByFuncionarioIdAndCompetenciaInicioAndCompetenciaFimAndAtivoTrue(
                funcionarioId, competenciaInicio, competenciaFim)
            .stream()
            .map(b -> new BeneficioLinhaSnapshot(
                b.getId(),
                b.getTipoBeneficio().getCodigo(),
                b.getTipoBeneficio().getDescricao(),
                b.getValor()))
            .toList();
    }

    @Override
    public BigDecimal somarValorPorCompetenciaECentros(
            LocalDate competenciaInicio, LocalDate competenciaFim, Set<Long> centrosCustoIds) {
        validarCompetencia(competenciaInicio, competenciaFim);
        if (centrosCustoIds == null || centrosCustoIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = beneficioMensalRepository.sumValorPorCompetenciaECentros(
            competenciaInicio, competenciaFim, centrosCustoIds);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public List<BeneficioTipoResumoSnapshot> resumoPorTipo(
            LocalDate competenciaInicio, LocalDate competenciaFim, Set<Long> centrosCustoIds) {
        validarCompetencia(competenciaInicio, competenciaFim);
        if (centrosCustoIds != null && centrosCustoIds.isEmpty()) {
            return List.of();
        }
        var projections = centrosCustoIds == null
            ? beneficioMensalRepository.resumoPorTipoGlobal(competenciaInicio, competenciaFim)
            : beneficioMensalRepository.resumoPorTipoCentros(
                competenciaInicio, competenciaFim, centrosCustoIds);
        return projections.stream().map(this::toTipoResumoSnapshot).toList();
    }

    @Override
    public List<BeneficioFuncionarioValorSnapshot> topFuncionariosPorTipo(
            Long tipoBeneficioId,
            LocalDate competenciaInicio,
            LocalDate competenciaFim,
            Set<Long> centrosCustoIds,
            int limit) {
        validarCompetencia(competenciaInicio, competenciaFim);
        if (tipoBeneficioId == null || limit <= 0) {
            return List.of();
        }
        if (centrosCustoIds != null && centrosCustoIds.isEmpty()) {
            return List.of();
        }
        var projections = centrosCustoIds == null
            ? beneficioMensalRepository.topFuncionariosPorTipoGlobal(
                tipoBeneficioId, competenciaInicio, competenciaFim)
            : beneficioMensalRepository.topFuncionariosPorTipoCentros(
                tipoBeneficioId, competenciaInicio, competenciaFim, centrosCustoIds);
        return projections.stream()
            .limit(limit)
            .map(p -> new BeneficioFuncionarioValorSnapshot(
                p.getFuncionarioId(), p.getFuncionarioNome(),
                p.getValor() != null ? p.getValor() : BigDecimal.ZERO))
            .toList();
    }

    @Override
    public List<BeneficioCcTipoSnapshot> matrizCentroCustoPorTipo(
            LocalDate competenciaInicio,
            LocalDate competenciaFim,
            Set<Long> centrosCustoIds,
            int topCc,
            int topTipos) {
        validarCompetencia(competenciaInicio, competenciaFim);
        if (centrosCustoIds != null && centrosCustoIds.isEmpty()) {
            return List.of();
        }
        var projections = centrosCustoIds == null
            ? beneficioMensalRepository.matrizCcTipoGlobal(competenciaInicio, competenciaFim)
            : beneficioMensalRepository.matrizCcTipoCentros(
                competenciaInicio, competenciaFim, centrosCustoIds);

        Map<Long, BigDecimal> totalPorCc = new HashMap<>();
        Map<Long, BigDecimal> totalPorTipo = new HashMap<>();
        for (var p : projections) {
            BigDecimal total = p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO;
            totalPorCc.merge(p.getCentroCustoId(), total, BigDecimal::add);
            totalPorTipo.merge(p.getTipoBeneficioId(), total, BigDecimal::add);
        }

        Set<Long> topCcIds = totalPorCc.entrySet().stream()
            .sorted(Map.Entry.<Long, BigDecimal>comparingByValue(Comparator.reverseOrder()))
            .limit(topCc)
            .map(Map.Entry::getKey)
            .collect(Collectors.toCollection(HashSet::new));
        Set<Long> topTipoIds = totalPorTipo.entrySet().stream()
            .sorted(Map.Entry.<Long, BigDecimal>comparingByValue(Comparator.reverseOrder()))
            .limit(topTipos)
            .map(Map.Entry::getKey)
            .collect(Collectors.toCollection(HashSet::new));

        return projections.stream()
            .filter(p -> topCcIds.contains(p.getCentroCustoId())
                && topTipoIds.contains(p.getTipoBeneficioId()))
            .map(p -> new BeneficioCcTipoSnapshot(
                p.getCentroCustoId(),
                p.getCentroCustoDescricao(),
                p.getTipoBeneficioId(),
                p.getTipoCodigo(),
                p.getTipoDescricao(),
                p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO))
            .toList();
    }

    private BeneficioTipoResumoSnapshot toTipoResumoSnapshot(
            br.com.techne.sistemafolha.beneficios.infrastructure.BeneficioMensalTipoResumoProjection p) {
        return new BeneficioTipoResumoSnapshot(
            p.getTipoBeneficioId(),
            p.getCodigo(),
            p.getDescricao(),
            p.getTotal() != null ? p.getTotal() : BigDecimal.ZERO,
            p.getQtdLancamentos() != null ? p.getQtdLancamentos() : 0L
        );
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
