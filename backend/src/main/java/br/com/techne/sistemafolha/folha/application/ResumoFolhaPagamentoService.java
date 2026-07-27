package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.folha.api.ResumoFolhaPagamentoDTO;
import br.com.techne.sistemafolha.folha.domain.ResumoFolhaPagamento;
import br.com.techne.sistemafolha.folha.infrastructure.ResumoFolhaPagamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResumoFolhaPagamentoService {

    private final ResumoFolhaPagamentoRepository resumoFolhaPagamentoRepository;

    public List<ResumoFolhaPagamentoDTO> listarTodos() {
        return resumoFolhaPagamentoRepository.findByAtivoTrue()
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public List<ResumoFolhaPagamentoDTO> consultarPorPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        return resumoFolhaPagamentoRepository
            .findByCompetenciaInicioBetweenAndAtivoTrue(dataInicio, dataFim)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public Optional<ResumoFolhaPagamentoDTO> consultarPorCompetencia(
            LocalDate competenciaInicio, LocalDate competenciaFim) {
        return resumoFolhaPagamentoRepository
            .findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(competenciaInicio, competenciaFim)
            .map(this::toDTO);
    }

    public List<ResumoFolhaPagamentoDTO> listarMaisRecentes() {
        return resumoFolhaPagamentoRepository.findLatestResumos()
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    private ResumoFolhaPagamentoDTO toDTO(ResumoFolhaPagamento resumo) {
        return new ResumoFolhaPagamentoDTO(
            resumo.getId(),
            resumo.getTotalEmpregados(),
            resumo.getTotalEncargos(),
            resumo.getTotalPagamentos(),
            resumo.getTotalDescontos(),
            resumo.getTotalLiquido(),
            resumo.getCompetenciaInicio(),
            resumo.getCompetenciaFim(),
            resumo.getDataImportacao(),
            resumo.getDecimoTerceiro(),
            resumo.getAtivo()
        );
    }
}
