package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.cadastros.domain.Cargo;
import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocio;
import br.com.techne.sistemafolha.cadastros.domain.Rubrica;
import br.com.techne.sistemafolha.folha.api.FolhaPagamentoDTO;
import br.com.techne.sistemafolha.folha.domain.FolhaPagamento;
import br.com.techne.sistemafolha.folha.domain.ResumoFolhaPagamento;
import br.com.techne.sistemafolha.folha.infrastructure.FolhaPagamentoRepository;
import br.com.techne.sistemafolha.folha.infrastructure.ResumoFolhaPagamentoRepository;
import br.com.techne.sistemafolha.folha.port.FolhaImportacaoCommand;
import br.com.techne.sistemafolha.folha.port.FolhaImportacaoLinhaCommand;
import br.com.techne.sistemafolha.folha.port.FolhaImportacaoPort;
import br.com.techne.sistemafolha.folha.port.FolhaImportacaoResumoCommand;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolhaImportacaoAdapter implements FolhaImportacaoPort {

    private final FolhaPagamentoRepository folhaPagamentoRepository;
    private final ResumoFolhaPagamentoRepository resumoFolhaPagamentoRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public List<FolhaPagamentoDTO> persistirImportacao(FolhaImportacaoCommand command) {
        if (command.substituirExistente()) {
            substituirCompetenciaExistente(
                command.competenciaInicio(), command.competenciaFim(), command.decimoTerceiro());
        }

        List<FolhaPagamento> persistidas = new ArrayList<>();
        for (FolhaImportacaoLinhaCommand linha : command.linhas()) {
            FolhaPagamento folha = montarFolha(
                linha, command.competenciaInicio(), command.competenciaFim(), command.decimoTerceiro());
            persistidas.add(folhaPagamentoRepository.save(folha));
        }

        if (command.resumo() != null) {
            resumoFolhaPagamentoRepository.save(montarResumo(command));
        }

        return persistidas.stream().map(this::toDTO).toList();
    }

    private void substituirCompetenciaExistente(LocalDate dataInicio, LocalDate dataFim, boolean decimoTerceiro) {
        var resumosExistentes = resumoFolhaPagamentoRepository
            .findByCompetenciaInicioAndCompetenciaFimAndDecimoTerceiroAndAtivoTrue(
                dataInicio, dataFim, decimoTerceiro);

        List<FolhaPagamento> folhasAntigas = folhaPagamentoRepository.findByDataInicioAndDataFimAndDecimoTerceiro(
            dataInicio, dataFim, decimoTerceiro);
        folhaPagamentoRepository.deleteAll(folhasAntigas);

        for (ResumoFolhaPagamento resumoAntigo : resumosExistentes) {
            resumoFolhaPagamentoRepository.delete(resumoAntigo);
        }
    }

    private FolhaPagamento montarFolha(
            FolhaImportacaoLinhaCommand linha, LocalDate dataInicio, LocalDate dataFim, boolean decimoTerceiro) {
        FolhaPagamento folha = new FolhaPagamento();
        folha.setFuncionario(entityManager.getReference(Funcionario.class, linha.funcionarioId()));
        folha.setRubrica(entityManager.getReference(Rubrica.class, linha.rubricaId()));
        if (linha.cargoId() != null) {
            folha.setCargo(entityManager.getReference(Cargo.class, linha.cargoId()));
        }
        if (linha.centroCustoId() != null) {
            folha.setCentroCusto(entityManager.getReference(CentroCusto.class, linha.centroCustoId()));
        }
        if (linha.linhaNegocioId() != null) {
            folha.setLinhaNegocio(entityManager.getReference(LinhaNegocio.class, linha.linhaNegocioId()));
        }
        folha.setDataInicio(dataInicio);
        folha.setDataFim(dataFim);
        folha.setDecimoTerceiro(decimoTerceiro);
        folha.setValor(linha.valor());
        folha.setValorTotal(linha.valor());
        folha.setQuantidade(linha.quantidade());
        folha.setBaseCalculo(linha.baseCalculo());
        folha.setAtivo(true);
        return folha;
    }

    private ResumoFolhaPagamento montarResumo(FolhaImportacaoCommand command) {
        FolhaImportacaoResumoCommand resumo = command.resumo();
        ResumoFolhaPagamento entity = new ResumoFolhaPagamento();
        entity.setTotalEmpregados(resumo.totalEmpregados());
        entity.setTotalEncargos(resumo.totalEncargos());
        entity.setTotalPagamentos(resumo.totalPagamentos());
        entity.setTotalDescontos(resumo.totalDescontos());
        entity.setTotalLiquido(resumo.totalLiquido());
        entity.setCompetenciaInicio(command.competenciaInicio());
        entity.setCompetenciaFim(command.competenciaFim());
        entity.setDataImportacao(LocalDateTime.now(Clock.systemDefaultZone()));
        entity.setDecimoTerceiro(command.decimoTerceiro());
        entity.setAtivo(true);
        return entity;
    }

    private FolhaPagamentoDTO toDTO(FolhaPagamento folha) {
        return new FolhaPagamentoDTO(
            folha.getId(),
            folha.getFuncionario().getId(),
            folha.getFuncionario().getNome(),
            folha.getRubrica().getId(),
            folha.getRubrica().getCodigo(),
            folha.getRubrica().getDescricao(),
            folha.getRubrica().getTipoRubrica().getDescricao(),
            folha.getCargo() != null ? folha.getCargo().getId() : null,
            folha.getCargo() != null ? folha.getCargo().getDescricao() : null,
            folha.getCentroCusto() != null ? folha.getCentroCusto().getId() : null,
            folha.getCentroCusto() != null ? folha.getCentroCusto().getDescricao() : null,
            folha.getLinhaNegocio() != null ? folha.getLinhaNegocio().getId() : null,
            folha.getLinhaNegocio() != null ? folha.getLinhaNegocio().getDescricao() : null,
            folha.getDataInicio(),
            folha.getDataFim(),
            folha.getValor(),
            folha.getQuantidade(),
            folha.getBaseCalculo(),
            folha.getDecimoTerceiro()
        );
    }
}
