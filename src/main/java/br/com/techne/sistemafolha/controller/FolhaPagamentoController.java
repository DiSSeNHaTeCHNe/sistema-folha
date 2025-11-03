package br.com.techne.sistemafolha.controller;

import br.com.techne.sistemafolha.dto.FolhaPagamentoDTO;
import br.com.techne.sistemafolha.model.FolhaPagamento;
import br.com.techne.sistemafolha.model.CentroCusto;
import br.com.techne.sistemafolha.model.LinhaNegocio;
import br.com.techne.sistemafolha.model.Usuario;
import br.com.techne.sistemafolha.repository.FolhaPagamentoRepository;
import br.com.techne.sistemafolha.repository.CentroCustoRepository;
import br.com.techne.sistemafolha.repository.LinhaNegocioRepository;
import br.com.techne.sistemafolha.repository.UsuarioRepository;
import br.com.techne.sistemafolha.service.OrganogramaAcessoService;
import br.com.techne.sistemafolha.exception.CentroCustoNotFoundException;
import br.com.techne.sistemafolha.exception.LinhaNegocioNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/folha-pagamento")
@RequiredArgsConstructor
@Tag(name = "Folha de Pagamento", description = "API para consulta de folha de pagamento")
public class FolhaPagamentoController {
    private static final Logger logger = LoggerFactory.getLogger(FolhaPagamentoController.class);
    
    private final FolhaPagamentoRepository folhaPagamentoRepository;
    private final CentroCustoRepository centroCustoRepository;
    private final LinhaNegocioRepository linhaNegocioRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrganogramaAcessoService organogramaAcessoService;

    @GetMapping("/funcionario/{funcionarioId}")
    @Operation(summary = "Consulta folha de pagamento ativa por funcionário")
    public ResponseEntity<List<FolhaPagamentoDTO>> consultarPorFuncionario(
            @PathVariable Long funcionarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            Authentication authentication) {
        
        Set<Long> centrosAcessiveis = obterCentrosAcessiveis(authentication);
        
        List<FolhaPagamentoDTO> folha = folhaPagamentoRepository
            .findByFuncionarioIdAndDataInicioBetweenAndAtivoTrue(funcionarioId, dataInicio, dataFim)
            .stream()
            .filter(f -> aplicarFiltroAcesso(f, centrosAcessiveis))
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(folha);
    }

    @GetMapping("/centro-custo/{centroCustoId}")
    @Operation(summary = "Consulta folha de pagamento ativa por centro de custo")
    public ResponseEntity<List<FolhaPagamentoDTO>> consultarPorCentroCusto(
            @PathVariable Long centroCustoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            Authentication authentication) {
        
        Set<Long> centrosAcessiveis = obterCentrosAcessiveis(authentication);
        
        // Verificar se o usuário tem acesso ao centro de custo solicitado
        if (!centrosAcessiveis.isEmpty() && !centrosAcessiveis.contains(centroCustoId)) {
            logger.warn("Usuário {} tentou acessar centro de custo {} sem permissão", 
                       authentication.getName(), centroCustoId);
            return ResponseEntity.ok(List.of()); // Retorna vazio se não tem acesso
        }
        
        CentroCusto centroCusto = centroCustoRepository.findById(centroCustoId)
            .orElseThrow(() -> new CentroCustoNotFoundException(centroCustoId));
            
        List<FolhaPagamentoDTO> folha = folhaPagamentoRepository
            .findByFuncionarioCentroCustoAndDataInicioBetweenAndAtivoTrue(centroCusto, dataInicio, dataFim)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(folha);
    }

    @GetMapping("/linha-negocio/{linhaNegocioId}")
    @Operation(summary = "Consulta folha de pagamento ativa por linha de negócio")
    public ResponseEntity<List<FolhaPagamentoDTO>> consultarPorLinhaNegocio(
            @PathVariable Long linhaNegocioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            Authentication authentication) {
        
        Set<Long> centrosAcessiveis = obterCentrosAcessiveis(authentication);
        
        LinhaNegocio linhaNegocio = linhaNegocioRepository.findById(linhaNegocioId)
            .orElseThrow(() -> new LinhaNegocioNotFoundException(linhaNegocioId));
            
        List<FolhaPagamentoDTO> folha = folhaPagamentoRepository
            .findByLinhaNegocioAndDataInicioBetweenAndAtivoTrue(linhaNegocio, dataInicio, dataFim)
            .stream()
            .filter(f -> aplicarFiltroAcesso(f, centrosAcessiveis))
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(folha);
    }

    @GetMapping
    @Operation(summary = "Consulta folha de pagamento ativa por período (mês/ano) - Filtra automaticamente pelos centros de custo acessíveis")
    public ResponseEntity<List<FolhaPagamentoDTO>> consultarPorPeriodo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            Authentication authentication) {
        
        Set<Long> centrosAcessiveis = obterCentrosAcessiveis(authentication);
        
        List<FolhaPagamento> resultado;
        
        if (dataInicio != null && dataFim != null) {
            resultado = folhaPagamentoRepository.findByDataInicioBetweenAndAtivoTrue(dataInicio, dataFim);
        } else {
            // Se não houver filtro de data, retorna todos os registros
            resultado = folhaPagamentoRepository.findAll();
        }
        
        // Aplicar filtro de acesso baseado em centros de custo
        List<FolhaPagamentoDTO> folha = resultado.stream()
            .filter(f -> aplicarFiltroAcesso(f, centrosAcessiveis))
            .map(this::toDTO)
            .collect(Collectors.toList());
            
        logger.info("Usuário {} consultou folha com {} registros (após filtro de acesso)", 
                   authentication.getName(), folha.size());
                   
        return ResponseEntity.ok(folha);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um registro de folha de pagamento (soft delete)")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        return folhaPagamentoRepository.findById(id)
            .map(folha -> {
                folhaPagamentoRepository.softDelete(id);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Obtém os centros de custo que o usuário autenticado pode acessar.
     * Empty set significa acesso total (sem restrições).
     */
    private Set<Long> obterCentrosAcessiveis(Authentication authentication) {
        String login = authentication.getName();
        Usuario usuario = usuarioRepository.findByLoginAndAtivoTrue(login)
            .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        
        return organogramaAcessoService.obterCentrosCustoAcessiveis(usuario.getId());
    }
    
    /**
     * Aplica o filtro de acesso baseado nos centros de custo.
     * Se centrosAcessiveis está vazio, significa acesso total (retorna true).
     * Caso contrário, verifica se o centro de custo do funcionário está na lista.
     */
    private boolean aplicarFiltroAcesso(FolhaPagamento folha, Set<Long> centrosAcessiveis) {
        // Empty set = acesso total, sem filtro
        if (centrosAcessiveis.isEmpty()) {
            return true;
        }
        
        // Verificar se o centro de custo do funcionário está nos centros acessíveis
        if (folha.getFuncionario() != null && 
            folha.getFuncionario().getCentroCusto() != null) {
            return centrosAcessiveis.contains(folha.getFuncionario().getCentroCusto().getId());
        }
        
        // Se não tem centro de custo, não permitir acesso (por segurança)
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
            folha.getBaseCalculo()
        );
    }
} 