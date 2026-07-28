package br.com.techne.sistemafolha.folha.application;

import br.com.techne.sistemafolha.folha.api.FolhaPagamentoDTO;
import br.com.techne.sistemafolha.folha.api.FolhaTotaisFuncionarioDTO;
import br.com.techne.sistemafolha.cadastros.domain.CentroCustoNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocioNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.CentroCusto;
import br.com.techne.sistemafolha.folha.domain.FolhaPagamento;
import br.com.techne.sistemafolha.folha.domain.ResumoFolhaPagamento;
import br.com.techne.sistemafolha.cadastros.domain.LinhaNegocio;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.cadastros.port.CadastrosLookupPort;
import br.com.techne.sistemafolha.folha.infrastructure.FolhaPagamentoRepository;
import br.com.techne.sistemafolha.folha.infrastructure.ResumoFolhaPagamentoRepository;
import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaLinhaSnapshot;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.techne.sistemafolha.shared.logging.DomainLogging;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolhaPagamentoService {

    private static final Logger logger = LoggerFactory.getLogger(FolhaPagamentoService.class);
    private static final String DOMAIN = "folha";

    private final FolhaPagamentoRepository folhaPagamentoRepository;
    private final CadastrosLookupPort cadastrosLookupPort;
    private final UsuarioLookupPort usuarioLookupPort;
    private final OrganogramaAcessoPort organogramaAcessoPort;
    private final FolhaTotalizacaoService folhaTotalizacaoService;
    private final FolhaConsultaPort folhaConsultaPort;
    private final ResumoFolhaPagamentoRepository resumoFolhaPagamentoRepository;

    public List<FolhaPagamentoDTO> consultarPorFuncionario(
            String login, Long funcionarioId, LocalDate dataInicio, LocalDate dataFim, Boolean decimoTerceiro) {
        AccessContextDTO contexto = obterContextoAcesso(login);

        List<FolhaPagamento> linhas;
        if (decimoTerceiro != null) {
            linhas = folhaPagamentoRepository.findByFuncionarioIdAndCompetenciaAndDecimoTerceiroAndAtivoTrue(
                funcionarioId, dataInicio, dataFim, decimoTerceiro);
        } else {
            linhas = folhaPagamentoRepository.findByFuncionarioIdAndDataInicioBetweenAndAtivoTrue(
                funcionarioId, dataInicio, dataFim);
        }

        return linhas.stream()
            .filter(f -> aplicarFiltroAcesso(f, contexto))
            .map(this::toDTO)
            .sorted(Comparator.comparing(FolhaPagamentoDTO::rubricaCodigo, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(FolhaPagamentoDTO::id, Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(Collectors.toList());
    }

    public List<FolhaPagamentoDTO> consultarPorCentroCusto(
            String login, Long centroCustoId, LocalDate dataInicio, LocalDate dataFim) {
        Usuario usuario = obterUsuario(login);
        if (!organogramaAcessoPort.usuarioPodeAcessarCentroCusto(usuario.getId(), centroCustoId)) {
            logger.warn("Usuário {} tentou acessar centro de custo {} sem permissão", login, centroCustoId);
            return List.of();
        }

        CentroCusto centroCusto = cadastrosLookupPort.findCentroCustoById(centroCustoId)
            .orElseThrow(() -> new CentroCustoNotFoundException(centroCustoId));

        return folhaPagamentoRepository
            .findByFuncionarioCentroCustoAndDataInicioBetweenAndAtivoTrue(centroCusto, dataInicio, dataFim)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public List<FolhaPagamentoDTO> consultarPorLinhaNegocio(
            String login, Long linhaNegocioId, LocalDate dataInicio, LocalDate dataFim) {
        AccessContextDTO contexto = obterContextoAcesso(login);

        LinhaNegocio linhaNegocio = cadastrosLookupPort.findLinhaNegocioById(linhaNegocioId)
            .orElseThrow(() -> new LinhaNegocioNotFoundException(linhaNegocioId));

        return folhaPagamentoRepository
            .findByLinhaNegocioAndDataInicioBetweenAndAtivoTrue(linhaNegocio, dataInicio, dataFim)
            .stream()
            .filter(f -> aplicarFiltroAcesso(f, contexto))
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    public List<FolhaPagamentoDTO> consultarPorPeriodo(
            String login, LocalDate dataInicio, LocalDate dataFim, Boolean decimoTerceiro) {
        AccessContextDTO contexto = obterContextoAcesso(login);

        List<FolhaPagamento> resultado;
        if (dataInicio != null && dataFim != null) {
            if (decimoTerceiro != null) {
                resultado = folhaPagamentoRepository.findByCompetenciaAndDecimoTerceiroAndAtivoTrue(
                    dataInicio, dataFim, decimoTerceiro);
            } else {
                resultado = folhaPagamentoRepository.findByDataInicioBetweenAndAtivoTrue(dataInicio, dataFim);
            }
        } else {
            resultado = folhaPagamentoRepository.findAll();
        }

        List<FolhaPagamentoDTO> folha = resultado.stream()
            .filter(f -> aplicarFiltroAcesso(f, contexto))
            .map(this::toDTO)
            .collect(Collectors.toList());

        logger.info("{}Usuário {} consultou folha com {} registros (após filtro de acesso)", DomainLogging.prefix(DOMAIN), login, folha.size());
        return folha;
    }

    public List<FolhaTotaisFuncionarioDTO> consultarTotaisPorFuncionario(
            String login, LocalDate dataInicio, LocalDate dataFim, Boolean decimoTerceiro) {
        AccessContextDTO contexto = obterContextoAcesso(login);
        boolean decimo = Boolean.TRUE.equals(decimoTerceiro);

        Set<Long> centrosFiltro = contexto.acessoTotal() ? null : contexto.centrosCustoIds();
        if (!contexto.acessoTotal()
            && (!contexto.temFuncionarioVinculado()
                || !contexto.temNoOrganograma()
                || centrosFiltro == null
                || centrosFiltro.isEmpty())) {
            return List.of();
        }

        List<FolhaLinhaSnapshot> linhas = folhaConsultaPort.findLinhasAtivasPorCompetencia(
            dataInicio, dataFim, decimo, centrosFiltro);

        BigDecimal totalEncargos = BigDecimal.ZERO;
        if (contexto.acessoTotal()) {
            totalEncargos = resumoFolhaPagamentoRepository
                .findByCompetenciaInicioAndCompetenciaFimAndDecimoTerceiroAndAtivoTrue(
                    dataInicio, dataFim, decimo)
                .stream()
                .findFirst()
                .map(ResumoFolhaPagamento::getTotalEncargos)
                .orElse(BigDecimal.ZERO);
        }

        List<FolhaTotaisFuncionarioDTO> totais = folhaTotalizacaoService.calcularTotaisPorFuncionario(
            linhas, contexto, totalEncargos, dataInicio, dataFim);

        logger.info("{}Usuário {} consultou totais de folha: {} funcionários no período {} a {} (decimoTerceiro={})",
            DomainLogging.prefix(DOMAIN), login, totais.size(), dataInicio, dataFim, decimo);

        return totais;
    }

    @Transactional
    public boolean removerSeAutorizado(String login, Long id) {
        AccessContextDTO contexto = obterContextoAcesso(login);
        return folhaPagamentoRepository.findById(id)
            .filter(folha -> Boolean.TRUE.equals(folha.getAtivo()))
            .filter(folha -> aplicarFiltroAcesso(folha, contexto))
            .map(folha -> {
                folhaPagamentoRepository.softDelete(id);
                return true;
            })
            .orElse(false);
    }

    private Usuario obterUsuario(String login) {
        return usuarioLookupPort.findByLoginAndAtivoTrue(login)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    private AccessContextDTO obterContextoAcesso(String login) {
        return organogramaAcessoPort.obterContextoAcesso(obterUsuario(login).getId());
    }

    boolean aplicarFiltroAcesso(FolhaPagamento folha, AccessContextDTO contexto) {
        if (contexto.acessoTotal()) {
            return true;
        }
        if (!contexto.temFuncionarioVinculado() || !contexto.temNoOrganograma()) {
            return false;
        }
        if (folha.getFuncionario() != null &&
            folha.getFuncionario().getCentroCusto() != null) {
            return contexto.centrosCustoIds().contains(folha.getFuncionario().getCentroCusto().getId());
        }
        return false;
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
