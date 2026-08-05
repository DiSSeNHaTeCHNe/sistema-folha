package br.com.techne.sistemafolha.organograma.acesso.application;

import br.com.techne.sistemafolha.organograma.domain.CentroCustoOrganograma;
import br.com.techne.sistemafolha.cadastros.domain.Funcionario;
import br.com.techne.sistemafolha.organograma.domain.FuncionarioOrganograma;
import br.com.techne.sistemafolha.organograma.domain.NoOrganograma;
import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.MotivoNegacaoAcesso;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import br.com.techne.sistemafolha.organograma.infrastructure.CentroCustoOrganogramaRepository;
import br.com.techne.sistemafolha.organograma.infrastructure.FuncionarioOrganogramaRepository;
import br.com.techne.sistemafolha.organograma.infrastructure.NoOrganogramaRepository;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.shared.logging.DomainLogging;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Serviço responsável pelo controle de acesso hierárquico baseado no organograma.
 *
 * <p>Regras de negócio:
 * <ul>
 *   <li>Usuário tem acesso aos centros de custo do seu nó e todos os descendentes</li>
 *   <li>Um funcionário só pode estar vinculado a 1 nó do organograma</li>
 *   <li>Usuário sem funcionário vinculado → acesso negado</li>
 *   <li>Funcionário sem nó no organograma → acesso negado</li>
 *   <li>Conjunto vazio de centros nunca implica acesso total</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class OrganogramaAcessoService implements OrganogramaAcessoPort {

    private static final Logger logger = LoggerFactory.getLogger(OrganogramaAcessoService.class);
    private static final String DOMAIN = "organograma";
    private static final String DOMAIN_PREFIX = DomainLogging.prefix(DOMAIN);
    public static final String PERMISSAO_ACESSO_TOTAL = "ACESSO_TOTAL";

    private final UsuarioLookupPort usuarioLookupPort;
    private final FuncionarioOrganogramaRepository funcionarioOrganogramaRepository;
    private final NoOrganogramaRepository noOrganogramaRepository;
    private final CentroCustoOrganogramaRepository centroCustoOrganogramaRepository;

    @Override
    @Transactional(readOnly = true)
    public Set<Long> obterCentrosCustoAcessiveis(Long usuarioId) {
        return resolverContextoAcesso(usuarioId).centrosCustoIds();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean usuarioPodeAcessarCentroCusto(Long usuarioId, Long centroCustoId) {
        AccessContextDTO contexto = resolverContextoAcesso(usuarioId);
        if (contexto.acessoTotal()) {
            return true;
        }
        if (!contexto.temFuncionarioVinculado() || !contexto.temNoOrganograma()) {
            return false;
        }
        return contexto.centrosCustoIds().contains(centroCustoId);
    }

    @Override
    @Transactional(readOnly = true)
    public AccessContextDTO obterContextoAcesso(Long usuarioId) {
        return resolverContextoAcesso(usuarioId);
    }

    private AccessContextDTO resolverContextoAcesso(Long usuarioId) {
        logger.debug("{}Calculando contexto de acesso para usuário ID: {}", DOMAIN_PREFIX, usuarioId);

        Usuario usuario = usuarioLookupPort.findById(usuarioId).orElse(null);
        if (usuario == null) {
            logAcessoNegado(usuarioId, MotivoNegacaoAcesso.SEM_FUNCIONARIO);
            return negar(MotivoNegacaoAcesso.SEM_FUNCIONARIO);
        }

        if (temPermissao(usuario, PERMISSAO_ACESSO_TOTAL)) {
            logger.info("{}Usuário ID {} com permissão {} — concedendo acesso total",
                DOMAIN_PREFIX, usuarioId, PERMISSAO_ACESSO_TOTAL);
            return contextoAcessoTotal(usuario);
        }

        Funcionario funcionario = usuario.getFuncionario();
        if (funcionario == null) {
            logAcessoNegado(usuarioId, MotivoNegacaoAcesso.SEM_FUNCIONARIO);
            return negar(MotivoNegacaoAcesso.SEM_FUNCIONARIO);
        }

        List<FuncionarioOrganograma> vinculos = funcionarioOrganogramaRepository
            .findByFuncionarioWithNoAtivo(funcionario);

        if (vinculos.isEmpty()) {
            logAcessoNegado(usuarioId, MotivoNegacaoAcesso.SEM_NO_ORGANOGRAMA);
            return negarComFuncionario(MotivoNegacaoAcesso.SEM_NO_ORGANOGRAMA);
        }

        if (vinculos.size() > 1) {
            logger.warn("{}Funcionário do usuário ID {} está vinculado a múltiplos nós. Usando o primeiro.",
                DOMAIN_PREFIX, usuarioId);
        }

        NoOrganograma no = vinculos.get(0).getNoOrganograma();
        Set<Long> centrosAcessiveis = new HashSet<>();
        coletarCentrosCustoRecursivo(no, centrosAcessiveis);

        logger.info("{}Usuário ID {} tem acesso a {} centros de custo no nó '{}' (ID: {})",
            DOMAIN_PREFIX, usuarioId, centrosAcessiveis.size(), no.getNome(), no.getId());

        return new AccessContextDTO(
            true,
            true,
            false,
            Set.copyOf(centrosAcessiveis),
            null,
            no.getId(),
            no.getNome(),
            no.getNivel()
        );
    }

    private void logAcessoNegado(Long usuarioId, MotivoNegacaoAcesso motivoNegacao) {
        logger.warn("{}ACL negado usuarioId={} motivoNegacao={}",
            DOMAIN_PREFIX, usuarioId, motivoNegacao);
    }

    private boolean temPermissao(Usuario usuario, String permissao) {
        List<String> permissoes = usuario.getPermissoes();
        return permissoes != null && permissoes.contains(permissao);
    }

    private AccessContextDTO contextoAcessoTotal(Usuario usuario) {
        return new AccessContextDTO(
            usuario.getFuncionario() != null,
            false,
            true,
            Collections.emptySet(),
            null,
            null,
            null,
            null
        );
    }

    private AccessContextDTO negar(MotivoNegacaoAcesso motivo) {
        return new AccessContextDTO(
            false,
            false,
            false,
            Collections.emptySet(),
            motivo,
            null,
            null,
            null
        );
    }

    private AccessContextDTO negarComFuncionario(MotivoNegacaoAcesso motivo) {
        return new AccessContextDTO(
            true,
            false,
            false,
            Collections.emptySet(),
            motivo,
            null,
            null,
            null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean noEstaNaSubarvore(Long noDescendenteId, Long noRaizId) {
        if (noDescendenteId == null || noRaizId == null) {
            return false;
        }
        if (noDescendenteId.equals(noRaizId)) {
            return true;
        }
        NoOrganograma atual = noOrganogramaRepository.findByIdAndAtivoTrue(noDescendenteId).orElse(null);
        while (atual != null && atual.getParent() != null) {
            if (atual.getParent().getId().equals(noRaizId)) {
                return true;
            }
            atual = noOrganogramaRepository.findByIdAndAtivoTrue(atual.getParent().getId()).orElse(null);
        }
        return false;
    }

    private void coletarCentrosCustoRecursivo(NoOrganograma no, Set<Long> centrosAcessiveis) {
        List<CentroCustoOrganograma> centros = centroCustoOrganogramaRepository
            .findByNoOrganogramaWithCentroCustoAtivo(no);

        for (CentroCustoOrganograma centro : centros) {
            centrosAcessiveis.add(centro.getCentroCusto().getId());
        }

        List<NoOrganograma> filhos = noOrganogramaRepository
            .findByParentAndAtivoTrueOrderByPosicao(no);

        for (NoOrganograma filho : filhos) {
            coletarCentrosCustoRecursivo(filho, centrosAcessiveis);
        }
    }
}
