package br.com.techne.sistemafolha.service;

import br.com.techne.sistemafolha.model.*;
import br.com.techne.sistemafolha.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Serviço responsável por gerenciar o controle de acesso hierárquico
 * baseado no organograma da empresa.
 * 
 * Regras de Negócio:
 * 1. Usuário tem acesso aos centros de custo do seu nó e todos os descendentes
 * 2. Um funcionário só pode estar vinculado a 1 nó do organograma
 * 3. Se usuário não tem funcionário → Sem acesso
 * 4. Se funcionário não está em nenhum nó → Sem filtro (acesso total)
 * 5. Nós sem centro de custo não bloqueiam a travessia da árvore
 */
@Service
@RequiredArgsConstructor
public class OrganogramaAcessoService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrganogramaAcessoService.class);
    
    private final UsuarioRepository usuarioRepository;
    private final FuncionarioOrganogramaRepository funcionarioOrganogramaRepository;
    private final NoOrganogramaRepository noOrganogramaRepository;
    private final CentroCustoOrganogramaRepository centroCustoOrganogramaRepository;
    
    /**
     * Obtém o nó do organograma onde o usuário está posicionado.
     * 
     * @param usuarioId ID do usuário
     * @return Optional com o nó do organograma, ou empty se não encontrado
     */
    @Transactional(readOnly = true)
    public Optional<NoOrganograma> obterNoDoUsuario(Long usuarioId) {
        logger.debug("Obtendo nó do organograma para usuário ID: {}", usuarioId);
        
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        
        if (usuario == null) {
            logger.warn("Usuário ID {} não encontrado", usuarioId);
            return Optional.empty();
        }
        
        if (usuario.getFuncionario() == null) {
            logger.warn("Usuário ID {} não tem funcionário vinculado", usuarioId);
            return Optional.empty();
        }
        
        List<FuncionarioOrganograma> vinculos = funcionarioOrganogramaRepository
            .findByFuncionarioWithNoAtivo(usuario.getFuncionario());
        
        if (vinculos.isEmpty()) {
            logger.info("Funcionário do usuário ID {} não está vinculado a nenhum nó do organograma - acesso total", usuarioId);
            return Optional.empty();
        }
        
        if (vinculos.size() > 1) {
            logger.warn("Funcionário do usuário ID {} está vinculado a múltiplos nós. Usando o primeiro.", usuarioId);
        }
        
        NoOrganograma no = vinculos.get(0).getNoOrganograma();
        logger.info("Usuário ID {} está no nó '{}' (ID: {})", usuarioId, no.getNome(), no.getId());
        
        return Optional.of(no);
    }
    
    /**
     * Obtém todos os IDs de centros de custo que o usuário pode acessar.
     * Inclui o nó atual e todos os descendentes na hierarquia.
     * 
     * @param usuarioId ID do usuário
     * @return Set com os IDs dos centros de custo acessíveis
     */
    @Transactional(readOnly = true)
    public Set<Long> obterCentrosCustoAcessiveis(Long usuarioId) {
        logger.info("Calculando centros de custo acessíveis para usuário ID: {}", usuarioId);
        
        Optional<NoOrganograma> noOpt = obterNoDoUsuario(usuarioId);
        
        // Se não tem nó, significa acesso total (regra 4)
        if (noOpt.isEmpty()) {
            logger.info("Usuário ID {} tem acesso total (sem restrições)", usuarioId);
            return Collections.emptySet(); // Empty = sem filtro
        }
        
        NoOrganograma noInicial = noOpt.get();
        Set<Long> centrosAcessiveis = new HashSet<>();
        
        // Coletar centros de custo recursivamente
        coletarCentrosCustoRecursivo(noInicial, centrosAcessiveis);
        
        logger.info("Usuário ID {} tem acesso a {} centros de custo: {}", 
                    usuarioId, centrosAcessiveis.size(), centrosAcessiveis);
        
        return centrosAcessiveis;
    }
    
    /**
     * Coleta recursivamente todos os centros de custo do nó atual e seus descendentes.
     * Atravessa nós sem centro de custo para coletar dos descendentes.
     * 
     * @param no Nó atual
     * @param centrosAcessiveis Set acumulador de IDs de centros de custo
     */
    private void coletarCentrosCustoRecursivo(NoOrganograma no, Set<Long> centrosAcessiveis) {
        logger.debug("Coletando centros de custo do nó '{}' (ID: {})", no.getNome(), no.getId());
        
        // Coletar centros de custo deste nó
        List<CentroCustoOrganograma> centros = centroCustoOrganogramaRepository
            .findByNoOrganogramaWithCentroCustoAtivo(no);
        
        for (CentroCustoOrganograma centro : centros) {
            centrosAcessiveis.add(centro.getCentroCusto().getId());
            logger.debug("  + Centro de custo: {} (ID: {})", 
                        centro.getCentroCusto().getDescricao(), 
                        centro.getCentroCusto().getId());
        }
        
        // Buscar filhos ativos e processar recursivamente
        List<NoOrganograma> filhos = noOrganogramaRepository
            .findByParentAndAtivoTrueOrderByPosicao(no);
        
        logger.debug("Nó '{}' tem {} filho(s)", no.getNome(), filhos.size());
        
        for (NoOrganograma filho : filhos) {
            coletarCentrosCustoRecursivo(filho, centrosAcessiveis);
        }
    }
    
    /**
     * Verifica se o usuário tem acesso a um centro de custo específico.
     * 
     * @param usuarioId ID do usuário
     * @param centroCustoId ID do centro de custo
     * @return true se tem acesso, false caso contrário
     */
    @Transactional(readOnly = true)
    public boolean usuarioPodeAcessarCentroCusto(Long usuarioId, Long centroCustoId) {
        Set<Long> centrosAcessiveis = obterCentrosCustoAcessiveis(usuarioId);
        
        // Empty set = acesso total
        if (centrosAcessiveis.isEmpty()) {
            return true;
        }
        
        return centrosAcessiveis.contains(centroCustoId);
    }
    
    /**
     * Obtém informações resumidas sobre o acesso do usuário.
     * 
     * @param usuarioId ID do usuário
     * @return Map com informações de acesso
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obterInformacoesAcesso(Long usuarioId) {
        Map<String, Object> info = new HashMap<>();
        
        Optional<NoOrganograma> noOpt = obterNoDoUsuario(usuarioId);
        Set<Long> centros = obterCentrosCustoAcessiveis(usuarioId);
        
        info.put("temFuncionario", noOpt.isPresent());
        info.put("acessoTotal", centros.isEmpty());
        info.put("quantidadeCentrosAcessiveis", centros.size());
        
        if (noOpt.isPresent()) {
            NoOrganograma no = noOpt.get();
            info.put("noOrganogramaId", no.getId());
            info.put("noOrganogramaNome", no.getNome());
            info.put("nivel", no.getNivel());
        }
        
        info.put("centrosCustoIds", new ArrayList<>(centros));
        
        return info;
    }
    
    /**
     * Verifica se o usuário tem acesso total (sem restrições).
     * 
     * @param usuarioId ID do usuário
     * @return true se tem acesso total, false se tem restrições
     */
    @Transactional(readOnly = true)
    public boolean usuarioTemAcessoTotal(Long usuarioId) {
        Set<Long> centrosAcessiveis = obterCentrosCustoAcessiveis(usuarioId);
        return centrosAcessiveis.isEmpty();
    }
}

