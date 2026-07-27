package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.folha.domain.FolhaPagamento;
import br.com.techne.sistemafolha.folha.domain.ResumoFolhaPagamento;
import br.com.techne.sistemafolha.folha.infrastructure.FolhaPagamentoRepository;
import br.com.techne.sistemafolha.folha.infrastructure.ResumoFolhaPagamentoRepository;
import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaEvolucaoSnapshot;
import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import br.com.techne.sistemafolha.folha.port.FolhaResumoSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FolhaConsultaAdapter implements FolhaConsultaPort {

    private final FolhaPagamentoRepository folhaPagamentoRepository;
    private final ResumoFolhaPagamentoRepository resumoFolhaPagamentoRepository;

    @Override
    public Optional<FolhaResumoSnapshot> findResumoMaisRecente() {
        List<ResumoFolhaPagamento> resumos = resumoFolhaPagamentoRepository.findByCompetenciaMaisRecente();
        return resumos.isEmpty()
            ? Optional.empty()
            : Optional.of(toResumoSnapshot(resumos.get(0)));
    }

    @Override
    public List<FolhaLinhaSnapshot> findLinhasAtivasPorCompetencia(
            LocalDate competenciaInicio, LocalDate competenciaFim, Set<Long> centrosCustoIds) {
        List<FolhaPagamento> linhas = folhaPagamentoRepository.findByCompetenciaAndAtivoTrue(
            competenciaInicio, competenciaFim);

        return linhas.stream()
            .filter(l -> centrosCustoIds == null || pertenceAosCentros(l, centrosCustoIds))
            .map(this::toLinhaSnapshot)
            .collect(Collectors.toList());
    }

    @Override
    public List<FolhaEvolucaoSnapshot> findEvolucaoUltimos12Meses(LocalDate dataInicio) {
        return resumoFolhaPagamentoRepository.findUltimos12Meses(dataInicio).stream()
            .map(this::toEvolucaoSnapshot)
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsResumoAtivo(LocalDate inicio, LocalDate fim, boolean decimoTerceiro) {
        return !resumoFolhaPagamentoRepository
            .findByCompetenciaInicioAndCompetenciaFimAndDecimoTerceiroAndAtivoTrue(inicio, fim, decimoTerceiro)
            .isEmpty();
    }

    @Override
    public boolean existsAtivaByCpfAndCompetenciaExcludingFuncionario(
            String cpf, Long funcionarioId, LocalDate inicio, LocalDate fim) {
        return folhaPagamentoRepository.existsAtivaByCpfAndCompetenciaExcludingFuncionario(
            cpf, funcionarioId, inicio, fim);
    }

    @Override
    public boolean existsByFuncionarioIdAndRubricaIdAndPeriodo(
            Long funcionarioId, Long rubricaId, LocalDate inicio, LocalDate fim) {
        return folhaPagamentoRepository.existsByFuncionarioIdAndRubricaIdAndDataInicioAndDataFim(
            funcionarioId, rubricaId, inicio, fim);
    }

    private boolean pertenceAosCentros(FolhaPagamento folha, Set<Long> centrosCustoIds) {
        if (folha.getFuncionario() == null || folha.getFuncionario().getCentroCusto() == null) {
            return false;
        }
        return centrosCustoIds.contains(folha.getFuncionario().getCentroCusto().getId());
    }

    private FolhaResumoSnapshot toResumoSnapshot(ResumoFolhaPagamento resumo) {
        return new FolhaResumoSnapshot(
            resumo.getCompetenciaInicio(),
            resumo.getCompetenciaFim(),
            resumo.getTotalLiquido(),
            resumo.getTotalEmpregados(),
            Boolean.TRUE.equals(resumo.getDecimoTerceiro())
        );
    }

    private FolhaEvolucaoSnapshot toEvolucaoSnapshot(ResumoFolhaPagamento resumo) {
        return new FolhaEvolucaoSnapshot(
            resumo.getCompetenciaInicio(),
            resumo.getTotalLiquido(),
            resumo.getTotalEmpregados()
        );
    }

    private FolhaLinhaSnapshot toLinhaSnapshot(FolhaPagamento folha) {
        var funcionario = folha.getFuncionario();
        var centroCusto = funcionario.getCentroCusto();
        var linhaNegocio = centroCusto != null ? centroCusto.getLinhaNegocio() : null;
        var cargo = folha.getCargo() != null ? folha.getCargo() : funcionario.getCargo();
        var rubrica = folha.getRubrica();

        return new FolhaLinhaSnapshot(
            funcionario.getId(),
            centroCusto != null ? centroCusto.getId() : null,
            centroCusto != null ? centroCusto.getDescricao() : null,
            linhaNegocio != null ? linhaNegocio.getId() : null,
            linhaNegocio != null ? linhaNegocio.getDescricao() : null,
            cargo != null ? cargo.getId() : null,
            cargo != null ? cargo.getDescricao() : null,
            rubrica.getId(),
            rubrica.getCodigo(),
            rubrica.getDescricao(),
            rubrica.getTipoRubrica().getDescricao(),
            folha.getValor()
        );
    }
}
