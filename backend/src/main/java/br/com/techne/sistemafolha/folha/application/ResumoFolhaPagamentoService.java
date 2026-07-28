package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.folha.api.ResumoFolhaPagamentoDTO;
import br.com.techne.sistemafolha.folha.domain.ResumoFolhaPagamento;
import br.com.techne.sistemafolha.folha.infrastructure.ResumoFolhaPagamentoRepository;
import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResumoFolhaPagamentoService {

    private final ResumoFolhaPagamentoRepository resumoFolhaPagamentoRepository;
    private final FolhaConsultaPort folhaConsultaPort;
    private final OrganogramaAcessoPort organogramaAcessoPort;
    private final UsuarioLookupPort usuarioLookupPort;
    private final FolhaLinhaAgregacao folhaLinhaAgregacao = new FolhaLinhaAgregacao();

    public List<ResumoFolhaPagamentoDTO> listarTodos(String login, Integer ano, Integer mes) {
        PeriodoCompetencia periodo = periodoDe(ano, mes);
        return consultarPorPeriodo(login, periodo.inicio(), periodo.fim());
    }

    private record PeriodoCompetencia(LocalDate inicio, LocalDate fim) {}

    private PeriodoCompetencia periodoDe(Integer ano, Integer mes) {
        int anoEfetivo = ano != null ? ano : LocalDate.now().getYear();
        if (anoEfetivo < 2000 || anoEfetivo > 2100) {
            throw new IllegalArgumentException("Ano deve estar entre 2000 e 2100");
        }
        if (mes != null) {
            LocalDate inicio = LocalDate.of(anoEfetivo, mes, 1);
            LocalDate fim = inicio.withDayOfMonth(inicio.lengthOfMonth());
            return new PeriodoCompetencia(inicio, fim);
        }
        return new PeriodoCompetencia(
            LocalDate.of(anoEfetivo, 1, 1),
            LocalDate.of(anoEfetivo, 12, 31)
        );
    }

    public List<ResumoFolhaPagamentoDTO> consultarPorPeriodo(
            String login, LocalDate dataInicio, LocalDate dataFim) {
        AccessContextDTO contexto = obterContextoAcesso(login);
        if (acessoNegado(contexto)) {
            return Collections.emptyList();
        }
        return resumoFolhaPagamentoRepository
            .findByCompetenciaInicioBetweenAndAtivoTrue(dataInicio, dataFim)
            .stream()
            .map(resumo -> mapear(resumo, contexto))
            .collect(Collectors.toList());
    }

    public Optional<ResumoFolhaPagamentoDTO> consultarPorCompetencia(
            String login, LocalDate competenciaInicio, LocalDate competenciaFim) {
        AccessContextDTO contexto = obterContextoAcesso(login);
        if (acessoNegado(contexto)) {
            return Optional.empty();
        }
        return resumoFolhaPagamentoRepository
            .findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(competenciaInicio, competenciaFim)
            .map(resumo -> mapear(resumo, contexto));
    }

    public List<ResumoFolhaPagamentoDTO> listarMaisRecentes(String login) {
        AccessContextDTO contexto = obterContextoAcesso(login);
        if (acessoNegado(contexto)) {
            return Collections.emptyList();
        }
        return resumoFolhaPagamentoRepository.findLatestResumos()
            .stream()
            .map(resumo -> mapear(resumo, contexto))
            .collect(Collectors.toList());
    }

    private ResumoFolhaPagamentoDTO mapear(ResumoFolhaPagamento resumo, AccessContextDTO contexto) {
        if (contexto.acessoTotal()) {
            return toDtoSnapshot(resumo);
        }
        return toDtoScoped(resumo, contexto.centrosCustoIds());
    }

    private ResumoFolhaPagamentoDTO toDtoSnapshot(ResumoFolhaPagamento resumo) {
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

    private ResumoFolhaPagamentoDTO toDtoScoped(ResumoFolhaPagamento resumo, Set<Long> centros) {
        List<FolhaLinhaSnapshot> linhas = folhaConsultaPort.findLinhasAtivasPorCompetencia(
            resumo.getCompetenciaInicio(), resumo.getCompetenciaFim(),
            Boolean.TRUE.equals(resumo.getDecimoTerceiro()), centros);
        FolhaLinhaAgregacao.Totais totais = folhaLinhaAgregacao.agregar(linhas);
        return new ResumoFolhaPagamentoDTO(
            resumo.getId(),
            Math.toIntExact(totais.empregados()),
            BigDecimal.ZERO,
            totais.pagamentos(),
            totais.descontos(),
            totais.liquido(),
            resumo.getCompetenciaInicio(),
            resumo.getCompetenciaFim(),
            resumo.getDataImportacao(),
            resumo.getDecimoTerceiro(),
            resumo.getAtivo()
        );
    }

    private AccessContextDTO obterContextoAcesso(String login) {
        Usuario usuario = usuarioLookupPort.findByLoginAndAtivoTrue(login)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        return organogramaAcessoPort.obterContextoAcesso(usuario.getId());
    }

    private boolean acessoNegado(AccessContextDTO contexto) {
        return !contexto.acessoTotal()
            && (!contexto.temFuncionarioVinculado()
                || !contexto.temNoOrganograma()
                || contexto.centrosCustoIds() == null
                || contexto.centrosCustoIds().isEmpty());
    }
}
