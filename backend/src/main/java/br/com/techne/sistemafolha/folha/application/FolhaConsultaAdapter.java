package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.folha.domain.FichaLinha;
import br.com.techne.sistemafolha.folha.domain.FolhaPagamento;
import br.com.techne.sistemafolha.folha.domain.OrigemLinha;
import br.com.techne.sistemafolha.folha.domain.ResumoFolhaPagamento;
import br.com.techne.sistemafolha.folha.infrastructure.FichaLinhaRepository;
import br.com.techne.sistemafolha.folha.infrastructure.FichaMensalRepository;
import br.com.techne.sistemafolha.folha.infrastructure.FolhaPagamentoRepository;
import br.com.techne.sistemafolha.folha.infrastructure.ResumoFolhaPagamentoRepository;
import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaEvolucaoSnapshot;
import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import br.com.techne.sistemafolha.folha.port.FolhaResumoSnapshot;
import br.com.techne.sistemafolha.shared.access.CentroCustoEfetivo;
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
    private final FichaMensalRepository fichaMensalRepository;
    private final FichaLinhaRepository fichaLinhaRepository;

    @Override
    public Optional<FolhaResumoSnapshot> findResumoMaisRecente() {
        List<ResumoFolhaPagamento> resumos = resumoFolhaPagamentoRepository.findByCompetenciaMaisRecente();
        return resumos.isEmpty()
            ? Optional.empty()
            : Optional.of(toResumoSnapshot(resumos.get(0)));
    }

    @Override
    public List<FolhaLinhaSnapshot> findLinhasAtivasPorCompetencia(
            LocalDate competenciaInicio, LocalDate competenciaFim, boolean decimoTerceiro,
            Set<Long> centrosCustoIds) {
        if (fichaMensalRepository.existsByCompetencia(competenciaInicio, competenciaFim, decimoTerceiro)) {
            return linhasDeFicha(competenciaInicio, competenciaFim, decimoTerceiro, centrosCustoIds);
        }
        return linhasDeFolhaPagamento(competenciaInicio, competenciaFim, decimoTerceiro, centrosCustoIds);
    }

    private List<FolhaLinhaSnapshot> linhasDeFicha(
            LocalDate competenciaInicio, LocalDate competenciaFim, boolean decimoTerceiro,
            Set<Long> centrosCustoIds) {
        List<FichaLinha> linhas;
        if (centrosCustoIds != null && !centrosCustoIds.isEmpty()) {
            linhas = fichaLinhaRepository.findByCompetenciaAndCentrosCustoIds(
                competenciaInicio, competenciaFim, decimoTerceiro, centrosCustoIds);
        } else {
            linhas = fichaLinhaRepository.findByCompetenciaWithFetch(
                competenciaInicio, competenciaFim, decimoTerceiro);
        }
        return linhas.stream()
            .map(this::toLinhaSnapshotFromFicha)
            .toList();
    }

    private List<FolhaLinhaSnapshot> linhasDeFolhaPagamento(
            LocalDate competenciaInicio, LocalDate competenciaFim, boolean decimoTerceiro,
            Set<Long> centrosCustoIds) {
        List<FolhaPagamento> linhas = folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroWithFetch(
            competenciaInicio, competenciaFim, decimoTerceiro);

        return linhas.stream()
            .filter(l -> centrosCustoIds == null || pertenceAosCentros(l, centrosCustoIds))
            .map(this::toLinhaSnapshotFromFolhaPagamento)
            .toList();
    }

    @Override
    public List<FolhaEvolucaoSnapshot> findEvolucaoUltimos12Meses(LocalDate dataInicio) {
        return resumoFolhaPagamentoRepository.findUltimos12MesesRegulares(dataInicio).stream()
            .map(this::toEvolucaoSnapshot)
            .toList();
    }

    @Override
    public boolean existsResumoAtivo(LocalDate inicio, LocalDate fim, boolean decimoTerceiro) {
        return !resumoFolhaPagamentoRepository
            .findByCompetenciaInicioAndCompetenciaFimAndDecimoTerceiroAndAtivoTrue(inicio, fim, decimoTerceiro)
            .isEmpty();
    }

    @Override
    public boolean existsAtivaByCpfAndCompetenciaExcludingFuncionario(
            String cpf, Long funcionarioId, LocalDate inicio, LocalDate fim, boolean decimoTerceiro) {
        return folhaPagamentoRepository.existsAtivaByCpfAndCompetenciaExcludingFuncionario(
            cpf, funcionarioId, inicio, fim, decimoTerceiro);
    }

    @Override
    public boolean existsByFuncionarioIdAndRubricaIdAndPeriodo(
            Long funcionarioId, Long rubricaId, LocalDate inicio, LocalDate fim, boolean decimoTerceiro) {
        return folhaPagamentoRepository.existsByFuncionarioIdAndRubricaIdAndPeriodoAndDecimoTerceiro(
            funcionarioId, rubricaId, inicio, fim, decimoTerceiro);
    }

    private boolean pertenceAosCentros(FolhaPagamento folha, Set<Long> centrosCustoIds) {
        Long linhaCcId = folha.getCentroCusto() != null ? folha.getCentroCusto().getId() : null;
        Long funcCcId = folha.getFuncionario() != null && folha.getFuncionario().getCentroCusto() != null
            ? folha.getFuncionario().getCentroCusto().getId() : null;
        return CentroCustoEfetivo.pertenceAoEscopo(
            CentroCustoEfetivo.idOf(linhaCcId, funcCcId), centrosCustoIds);
    }

    private FolhaResumoSnapshot toResumoSnapshot(ResumoFolhaPagamento resumo) {
        return new FolhaResumoSnapshot(
            resumo.getCompetenciaInicio(),
            resumo.getCompetenciaFim(),
            resumo.getTotalLiquido(),
            resumo.getTotalEmpregados(),
            Boolean.TRUE.equals(resumo.getDecimoTerceiro()),
            resumo.getTotalEncargos() != null ? resumo.getTotalEncargos() : BigDecimal.ZERO
        );
    }

    private FolhaEvolucaoSnapshot toEvolucaoSnapshot(ResumoFolhaPagamento resumo) {
        return new FolhaEvolucaoSnapshot(
            resumo.getCompetenciaInicio(),
            resumo.getCompetenciaFim(),
            resumo.getTotalLiquido(),
            resumo.getTotalEmpregados(),
            Boolean.TRUE.equals(resumo.getDecimoTerceiro())
        );
    }

    private FolhaLinhaSnapshot toLinhaSnapshotFromFolhaPagamento(FolhaPagamento folha) {
        var funcionario = folha.getFuncionario();
        var centroCusto = folha.getCentroCusto() != null ? folha.getCentroCusto() : funcionario.getCentroCusto();
        var linhaNegocio = folha.getLinhaNegocio();
        if (linhaNegocio == null && centroCusto != null) {
            linhaNegocio = centroCusto.getLinhaNegocio();
        }
        var cargo = folha.getCargo() != null ? folha.getCargo() : funcionario.getCargo();
        var rubrica = folha.getRubrica();

        return new FolhaLinhaSnapshot(
            funcionario.getId(),
            funcionario.getNome(),
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
            folha.getValor(),
            operadorOuZero(rubrica.getOperadorBruto()),
            operadorOuZero(rubrica.getOperadorLiquido()),
            operadorOuZero(rubrica.getOperadorCusto()),
            OrigemLinha.FOLHA_ADP,
            rubrica.getPorcentagem() != null ? BigDecimal.valueOf(rubrica.getPorcentagem()) : null
        );
    }

    private FolhaLinhaSnapshot toLinhaSnapshotFromFicha(FichaLinha linha) {
        var fichaMensal = linha.getFichaMensal();
        var funcionario = fichaMensal.getFuncionario();
        var centroCusto = fichaMensal.getCentroCusto() != null
            ? fichaMensal.getCentroCusto()
            : funcionario.getCentroCusto();
        var linhaNegocio = centroCusto != null ? centroCusto.getLinhaNegocio() : null;
        var cargo = funcionario.getCargo();
        var rubrica = linha.getRubrica();

        return new FolhaLinhaSnapshot(
            funcionario.getId(),
            funcionario.getNome(),
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
            linha.getValor(),
            operadorOuZero(linha.getOperadorBruto()),
            operadorOuZero(linha.getOperadorLiquido()),
            operadorOuZero(linha.getOperadorCusto()),
            linha.getOrigemLinha() != null ? linha.getOrigemLinha() : OrigemLinha.FOLHA_ADP,
            linha.getPorcentagem()
        );
    }

    private short operadorOuZero(Short operador) {
        return operador != null ? operador : 0;
    }
}
