package br.com.techne.sistemafolha.service;

import br.com.techne.sistemafolha.dto.*;
import br.com.techne.sistemafolha.exception.*;
import br.com.techne.sistemafolha.model.*;
import br.com.techne.sistemafolha.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganogramaService {
    private static final Logger logger = LoggerFactory.getLogger(OrganogramaService.class);

    private final NoOrganogramaRepository noOrganogramaRepository;
    private final FuncionarioOrganogramaRepository funcionarioOrganogramaRepository;
    private final CentroCustoOrganogramaRepository centroCustoOrganogramaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final CentroCustoRepository centroCustoRepository;

    // ========================= OPERAÇÕES BÁSICAS =========================

    public List<NoOrganogramaDTO> listarTodos() {
        logger.info("Listando todos os nós do organograma");
        return noOrganogramaRepository.findByAtivoTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public NoOrganogramaDTO buscarPorId(Long id) {
        logger.info("Buscando nó do organograma por ID: {}", id);
        return noOrganogramaRepository.findByIdAndAtivoTrue(id)
                .map(this::toDTOCompleto)
                .orElseThrow(() -> new NoOrganogramaNotFoundException(id));
    }

    @Transactional
    public NoOrganogramaDTO cadastrar(NoOrganogramaDTO dto) {
        logger.info("Cadastrando novo nó do organograma: {}", dto.nome());
        
        NoOrganograma no = toEntity(dto);
        
        // Se tem parent, buscar e validar
        if (dto.parentId() != null) {
            NoOrganograma parent = noOrganogramaRepository.findByIdAndAtivoTrue(dto.parentId())
                    .orElseThrow(() -> new NoOrganogramaNotFoundException("Nó pai não encontrado com ID: " + dto.parentId()));
            no.setParent(parent);
            no.setNivel(parent.getNivel() + 1);
        }
        
        no = noOrganogramaRepository.save(no);
        return toDTOCompleto(no);
    }

    @Transactional
    public NoOrganogramaDTO atualizar(Long id, NoOrganogramaDTO dto) {
        logger.info("Atualizando nó do organograma ID: {}", id);
        
        NoOrganograma no = noOrganogramaRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new NoOrganogramaNotFoundException(id));

        // Validar se mudança de parent não cria ciclo
        if (dto.parentId() != null && !Objects.equals(dto.parentId(), no.getParent() != null ? no.getParent().getId() : null)) {
            validarCicloHierarquico(id, dto.parentId());
        }

        no.setNome(dto.nome());
        no.setDescricao(dto.descricao());
        no.setPosicao(dto.posicao());

        // Atualizar parent se necessário
        if (dto.parentId() != null) {
            NoOrganograma parent = noOrganogramaRepository.findByIdAndAtivoTrue(dto.parentId())
                    .orElseThrow(() -> new NoOrganogramaNotFoundException("Nó pai não encontrado com ID: " + dto.parentId()));
            no.setParent(parent);
            no.setNivel(parent.getNivel() + 1);
        } else {
            no.setParent(null);
            no.setNivel(0);
        }

        no = noOrganogramaRepository.save(no);
        return toDTOCompleto(no);
    }

    @Transactional
    public void remover(Long id) {
        logger.info("Removendo nó do organograma ID: {}", id);
        
        NoOrganograma no = noOrganogramaRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new NoOrganogramaNotFoundException(id));

        // Verificar se tem filhos ativos
        if (noOrganogramaRepository.existsByParentAndAtivoTrue(no)) {
            throw new IllegalStateException("Não é possível remover nó que possui filhos ativos");
        }

        // Remover associações
        funcionarioOrganogramaRepository.deleteByNoOrganograma(no);
        centroCustoOrganogramaRepository.deleteByNoOrganograma(no);

        // Soft delete
        noOrganogramaRepository.softDelete(id);
    }

    @Transactional
    public void removerComFilhos(Long id) {
        logger.info("Removendo nó do organograma ID: {} e todos os filhos", id);
        
        NoOrganograma no = noOrganogramaRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new NoOrganogramaNotFoundException(id));

        // Executar soft delete em cascata de forma recursiva
        softDeleteRecursivo(no);
    }

    private void softDeleteRecursivo(NoOrganograma no) {
        // Buscar todos os filhos ativos
        List<NoOrganograma> filhos = noOrganogramaRepository.findByParentAndAtivoTrueOrderByPosicao(no);
        
        // Primeiro, remover todos os filhos recursivamente
        for (NoOrganograma filho : filhos) {
            softDeleteRecursivo(filho);
        }
        
        // Depois remover o próprio nó
        // Remover associações
        funcionarioOrganogramaRepository.deleteByNoOrganograma(no);
        centroCustoOrganogramaRepository.deleteByNoOrganograma(no);
        
        // Soft delete do nó
        noOrganogramaRepository.softDelete(no.getId());
        
        logger.debug("Nó ID: {} removido com soft delete", no.getId());
    }

    // ========================= OPERAÇÕES HIERÁRQUICAS =========================

    public List<NoOrganogramaDTO> obterArvoreCompleta() {
        logger.info("Obtendo árvore completa do organograma ativo");
        
        List<NoOrganograma> nos = noOrganogramaRepository.findByOrganogramaAtivoTrueAndAtivoTrue();
        return construirArvore(nos);
    }

    public List<NoOrganogramaDTO> obterFilhos(Long parentId) {
        logger.info("Obtendo filhos do nó ID: {}", parentId);
        
        if (parentId == null) {
            // Retornar nós raiz
            return noOrganogramaRepository.findByParentIsNullAndAtivoTrueOrderByPosicao().stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
        }
        
        return noOrganogramaRepository.findByParentIdAndAtivoTrueOrderByPosicao(parentId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public NoOrganogramaDTO moverNo(Long noId, Long novoParentId, Integer novaPosicao) {
        logger.info("Movendo nó ID: {} para parent ID: {} na posição: {}", noId, novoParentId, novaPosicao);
        
        NoOrganograma no = noOrganogramaRepository.findByIdAndAtivoTrue(noId)
                .orElseThrow(() -> new NoOrganogramaNotFoundException(noId));

        // Validar se mudança não cria ciclo
        if (novoParentId != null) {
            validarCicloHierarquico(noId, novoParentId);
        }

        // Atualizar parent
        if (novoParentId != null) {
            NoOrganograma novoParent = noOrganogramaRepository.findByIdAndAtivoTrue(novoParentId)
                    .orElseThrow(() -> new NoOrganogramaNotFoundException("Novo parent não encontrado com ID: " + novoParentId));
            no.setParent(novoParent);
            no.setNivel(novoParent.getNivel() + 1);
        } else {
            no.setParent(null);
            no.setNivel(0);
        }

        // Atualizar posição
        if (novaPosicao != null) {
            no.setPosicao(novaPosicao);
        }

        // Atualizar níveis dos filhos recursivamente
        atualizarNiveisFilhos(no);

        no = noOrganogramaRepository.save(no);
        return toDTOCompleto(no);
    }

    // ========================= GESTÃO DO ORGANOGRAMA ATIVO =========================

    public NoOrganogramaDTO obterOrganogramaAtivo() {
        logger.info("Obtendo organograma ativo");
        
        return noOrganogramaRepository.findByOrganogramaAtivoTrue()
                .map(this::toDTOCompleto)
                .orElse(null);
    }

    @Transactional
    public void ativarOrganograma(Long noRaizId) {
        logger.info("Ativando organograma com nó raiz ID: {}", noRaizId);
        
        NoOrganograma noRaiz = noOrganogramaRepository.findByIdAndAtivoTrue(noRaizId)
                .orElseThrow(() -> new NoOrganogramaNotFoundException(noRaizId));

        // Verificar se é realmente um nó raiz
        if (noRaiz.getParent() != null) {
            throw new IllegalArgumentException("Apenas nós raiz podem ser ativados como organograma");
        }

        // Desativar organograma atual
        noOrganogramaRepository.desativarTodosOrganogramas();

        // Ativar todos os nós da árvore do nó raiz
        ativarArvoreRecursivamente(noRaiz);
    }

    @Transactional
    public void desativarOrganograma() {
        logger.info("Desativando organograma atual");
        noOrganogramaRepository.desativarTodosOrganogramas();
    }

    // ========================= ASSOCIAÇÕES COM FUNCIONÁRIOS =========================

    @Transactional
    public FuncionarioOrganogramaDTO associarFuncionario(Long noId, Long funcionarioId) {
        logger.info("Associando funcionário ID: {} ao nó ID: {}", funcionarioId, noId);
        
        NoOrganograma no = noOrganogramaRepository.findByIdAndAtivoTrue(noId)
                .orElseThrow(() -> new NoOrganogramaNotFoundException(noId));
        
        Funcionario funcionario = funcionarioRepository.findByIdAndAtivoTrue(funcionarioId)
                .orElseThrow(() -> new FuncionarioNotFoundException(funcionarioId));

        // Verificar se já existe associação
        if (funcionarioOrganogramaRepository.existsByFuncionarioAndNoOrganograma(funcionario, no)) {
            throw new IllegalArgumentException("Funcionário já está associado a este nó");
        }

        FuncionarioOrganograma associacao = new FuncionarioOrganograma();
        associacao.setFuncionario(funcionario);
        associacao.setNoOrganograma(no);

        associacao = funcionarioOrganogramaRepository.save(associacao);
        return toFuncionarioOrganogramaDTO(associacao);
    }

    @Transactional
    public void desassociarFuncionario(Long noId, Long funcionarioId) {
        logger.info("Desassociando funcionário ID: {} do nó ID: {}", funcionarioId, noId);
        
        NoOrganograma no = noOrganogramaRepository.findByIdAndAtivoTrue(noId)
                .orElseThrow(() -> new NoOrganogramaNotFoundException(noId));
        
        Funcionario funcionario = funcionarioRepository.findByIdAndAtivoTrue(funcionarioId)
                .orElseThrow(() -> new FuncionarioNotFoundException(funcionarioId));

        funcionarioOrganogramaRepository.deleteByFuncionarioAndNoOrganograma(funcionario, no);
    }

    public List<FuncionarioOrganogramaDTO> listarFuncionariosPorNo(Long noId) {
        logger.info("Listando funcionários do nó ID: {}", noId);
        
        NoOrganograma no = noOrganogramaRepository.findByIdAndAtivoTrue(noId)
                .orElseThrow(() -> new NoOrganogramaNotFoundException(noId));

        return funcionarioOrganogramaRepository.findByNoOrganogramaWithFuncionarioAtivo(no).stream()
                .map(this::toFuncionarioOrganogramaDTO)
                .collect(Collectors.toList());
    }

    // ========================= ASSOCIAÇÕES COM CENTROS DE CUSTO =========================

    @Transactional
    public CentroCustoOrganogramaDTO associarCentroCusto(Long noId, Long centroCustoId) {
        logger.info("Associando centro de custo ID: {} ao nó ID: {}", centroCustoId, noId);
        
        NoOrganograma no = noOrganogramaRepository.findByIdAndAtivoTrue(noId)
                .orElseThrow(() -> new NoOrganogramaNotFoundException(noId));
        
        CentroCusto centroCusto = centroCustoRepository.findById(centroCustoId)
                .filter(cc -> cc.getAtivo())
                .orElseThrow(() -> new CentroCustoNotFoundException(centroCustoId));

        // Verificar se já existe associação
        if (centroCustoOrganogramaRepository.existsByCentroCustoAndNoOrganograma(centroCusto, no)) {
            throw new IllegalArgumentException("Centro de custo já está associado a este nó");
        }

        CentroCustoOrganograma associacao = new CentroCustoOrganograma();
        associacao.setCentroCusto(centroCusto);
        associacao.setNoOrganograma(no);

        associacao = centroCustoOrganogramaRepository.save(associacao);
        return toCentroCustoOrganogramaDTO(associacao);
    }

    @Transactional
    public void desassociarCentroCusto(Long noId, Long centroCustoId) {
        logger.info("Desassociando centro de custo ID: {} do nó ID: {}", centroCustoId, noId);
        
        NoOrganograma no = noOrganogramaRepository.findByIdAndAtivoTrue(noId)
                .orElseThrow(() -> new NoOrganogramaNotFoundException(noId));
        
        CentroCusto centroCusto = centroCustoRepository.findById(centroCustoId)
                .filter(cc -> cc.getAtivo())
                .orElseThrow(() -> new CentroCustoNotFoundException(centroCustoId));

        centroCustoOrganogramaRepository.deleteByCentroCustoAndNoOrganograma(centroCusto, no);
    }

    public List<CentroCustoOrganogramaDTO> listarCentrosCustoPorNo(Long noId) {
        logger.info("Listando centros de custo do nó ID: {}", noId);
        
        NoOrganograma no = noOrganogramaRepository.findByIdAndAtivoTrue(noId)
                .orElseThrow(() -> new NoOrganogramaNotFoundException(noId));

        return centroCustoOrganogramaRepository.findByNoOrganogramaWithCentroCustoAtivo(no).stream()
                .map(this::toCentroCustoOrganogramaDTO)
                .collect(Collectors.toList());
    }

    // ========================= MÉTODOS AUXILIARES =========================

    private void validarCicloHierarquico(Long noId, Long novoParentId) {
        // Verificar se o novo parent é descendente do nó atual (criaria ciclo)
        NoOrganograma candidatoParent = noOrganogramaRepository.findById(novoParentId).orElse(null);
        
        while (candidatoParent != null) {
            if (Objects.equals(candidatoParent.getId(), noId)) {
                throw new IllegalArgumentException("Operação criaria um ciclo na hierarquia");
            }
            candidatoParent = candidatoParent.getParent();
        }
    }

    private void atualizarNiveisFilhos(NoOrganograma no) {
        List<NoOrganograma> filhos = noOrganogramaRepository.findByParentAndAtivoTrueOrderByPosicao(no);
        
        for (NoOrganograma filho : filhos) {
            filho.setNivel(no.getNivel() + 1);
            noOrganogramaRepository.save(filho);
            atualizarNiveisFilhos(filho); // Recursão para netos
        }
    }

    private void ativarArvoreRecursivamente(NoOrganograma no) {
        no.setOrganogramaAtivo(true);
        noOrganogramaRepository.save(no);
        
        List<NoOrganograma> filhos = noOrganogramaRepository.findByParentAndAtivoTrueOrderByPosicao(no);
        for (NoOrganograma filho : filhos) {
            ativarArvoreRecursivamente(filho);
        }
    }

    private List<NoOrganogramaDTO> construirArvore(List<NoOrganograma> nos) {
        Map<Long, NoOrganogramaDTO> noMap = new HashMap<>();
        List<NoOrganogramaDTO> raizes = new ArrayList<>();

        // Primeiro, converter todos os nós para DTO
        for (NoOrganograma no : nos) {
            NoOrganogramaDTO dto = toDTOCompleto(no);
            noMap.put(dto.id(), dto);
        }

        // Depois, construir a árvore
        for (NoOrganogramaDTO dto : noMap.values()) {
            if (dto.parentId() == null) {
                raizes.add(dto);
            } else {
                NoOrganogramaDTO parent = noMap.get(dto.parentId());
                if (parent != null && parent.children() != null) {
                    parent.children().add(dto);
                }
            }
        }

        return raizes;
    }

    // ========================= CONVERSÕES DTO/ENTITY =========================

    private NoOrganogramaDTO toDTO(NoOrganograma entity) {
        if (entity == null) return null;

        return new NoOrganogramaDTO(
                entity.getId(),
                entity.getNome(),
                entity.getDescricao(),
                entity.getNivel(),
                entity.getParent() != null ? entity.getParent().getId() : null,
                entity.getParent() != null ? entity.getParent().getNome() : null,
                entity.getPosicao(),
                entity.getAtivo(),
                entity.getOrganogramaAtivo(),
                null, // funcionarioIds - carregado apenas no método completo
                null, // funcionarios - carregado apenas no método completo
                null, // centroCustoIds - carregado apenas no método completo
                null, // centrosCusto - carregado apenas no método completo
                new ArrayList<>(), // children - inicializado vazio
                entity.getDataCriacao(),
                entity.getDataAtualizacao(),
                entity.getCriadoPor(),
                entity.getAtualizadoPor()
        );
    }

    private NoOrganogramaDTO toDTOCompleto(NoOrganograma entity) {
        if (entity == null) return null;

        // Carregar funcionários
        List<FuncionarioOrganograma> funcionarios = funcionarioOrganogramaRepository.findByNoOrganogramaWithFuncionarioAtivo(entity);
        List<Long> funcionarioIds = funcionarios.stream()
                .map(fo -> fo.getFuncionario().getId())
                .collect(Collectors.toList());

        // Carregar centros de custo
        List<CentroCustoOrganograma> centrosCusto = centroCustoOrganogramaRepository.findByNoOrganogramaWithCentroCustoAtivo(entity);
        List<Long> centroCustoIds = centrosCusto.stream()
                .map(cco -> cco.getCentroCusto().getId())
                .collect(Collectors.toList());

        return new NoOrganogramaDTO(
                entity.getId(),
                entity.getNome(),
                entity.getDescricao(),
                entity.getNivel(),
                entity.getParent() != null ? entity.getParent().getId() : null,
                entity.getParent() != null ? entity.getParent().getNome() : null,
                entity.getPosicao(),
                entity.getAtivo(),
                entity.getOrganogramaAtivo(),
                funcionarioIds,
                null, // funcionarios completos - pode ser carregado conforme necessário
                centroCustoIds,
                null, // centrosCusto completos - pode ser carregado conforme necessário
                new ArrayList<>(), // children - inicializado vazio
                entity.getDataCriacao(),
                entity.getDataAtualizacao(),
                entity.getCriadoPor(),
                entity.getAtualizadoPor()
        );
    }

    private NoOrganograma toEntity(NoOrganogramaDTO dto) {
        if (dto == null) return null;

        NoOrganograma entity = new NoOrganograma();
        entity.setId(dto.id());
        entity.setNome(dto.nome());
        entity.setDescricao(dto.descricao());
        entity.setNivel(dto.nivel() != null ? dto.nivel() : 0);
        entity.setPosicao(dto.posicao() != null ? dto.posicao() : 0);
        entity.setAtivo(dto.ativo() != null ? dto.ativo() : true);
        entity.setOrganogramaAtivo(dto.organogramaAtivo() != null ? dto.organogramaAtivo() : false);

        return entity;
    }

    private FuncionarioOrganogramaDTO toFuncionarioOrganogramaDTO(FuncionarioOrganograma entity) {
        if (entity == null) return null;

        return new FuncionarioOrganogramaDTO(
                entity.getId(),
                entity.getFuncionario().getId(),
                null, // funcionario completo - carregado conforme necessário
                entity.getNoOrganograma().getId(),
                null, // noOrganograma completo - carregado conforme necessário
                entity.getDataCriacao(),
                entity.getCriadoPor()
        );
    }

    private CentroCustoOrganogramaDTO toCentroCustoOrganogramaDTO(CentroCustoOrganograma entity) {
        if (entity == null) return null;

        return new CentroCustoOrganogramaDTO(
                entity.getId(),
                entity.getCentroCusto().getId(),
                null, // centroCusto completo - carregado conforme necessário
                entity.getNoOrganograma().getId(),
                null, // noOrganograma completo - carregado conforme necessário
                entity.getDataCriacao(),
                entity.getCriadoPor()
        );
    }
} 