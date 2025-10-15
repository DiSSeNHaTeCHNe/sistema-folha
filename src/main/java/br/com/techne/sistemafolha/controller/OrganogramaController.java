package br.com.techne.sistemafolha.controller;

import br.com.techne.sistemafolha.dto.*;
import br.com.techne.sistemafolha.service.OrganogramaService;
import br.com.techne.sistemafolha.exception.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/organograma")
@RequiredArgsConstructor
@Tag(name = "Organograma", description = "API para gerenciamento do organograma")
public class OrganogramaController {
    
    private final OrganogramaService organogramaService;

    // ========================= OPERAÇÕES BÁSICAS =========================

    @GetMapping
    @Operation(summary = "Lista todos os nós do organograma")
    public ResponseEntity<List<NoOrganogramaDTO>> listarTodos() {
        return ResponseEntity.ok(organogramaService.listarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um nó do organograma pelo ID")
    public ResponseEntity<NoOrganogramaDTO> buscarPorId(
            @Parameter(description = "ID do nó") @PathVariable Long id) {
        try {
            return ResponseEntity.ok(organogramaService.buscarPorId(id));
        } catch (NoOrganogramaNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "Cadastra um novo nó do organograma")
    public ResponseEntity<NoOrganogramaDTO> cadastrar(
            @Parameter(description = "Dados do nó") @Valid @RequestBody NoOrganogramaCreateDTO dto) {
        try {
            System.out.println("Recebendo requisição para criar nó: " + dto);
            NoOrganogramaDTO resultado = organogramaService.cadastrar(dto);
            System.out.println("Nó criado com sucesso: " + resultado.id());
            return ResponseEntity.ok(resultado);
        } catch (NoOrganogramaNotFoundException | IllegalArgumentException e) {
            System.err.println("Erro ao criar nó: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Erro inesperado ao criar nó: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um nó do organograma existente")
    public ResponseEntity<NoOrganogramaDTO> atualizar(
            @Parameter(description = "ID do nó") @PathVariable Long id,
            @Parameter(description = "Dados atualizados do nó") @Valid @RequestBody NoOrganogramaDTO dto) {
        try {
            return ResponseEntity.ok(organogramaService.atualizar(id, dto));
        } catch (NoOrganogramaNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um nó do organograma (apenas se não tiver filhos)")
    public ResponseEntity<Void> remover(
            @Parameter(description = "ID do nó") @PathVariable Long id) {
        try {
            organogramaService.remover(id);
            return ResponseEntity.noContent().build();
        } catch (NoOrganogramaNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}/cascata")
    @Operation(summary = "Remove um nó do organograma e todos os seus filhos")
    public ResponseEntity<Void> removerComFilhos(
            @Parameter(description = "ID do nó") @PathVariable Long id) {
        try {
            organogramaService.removerComFilhos(id);
            return ResponseEntity.noContent().build();
        } catch (NoOrganogramaNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ========================= OPERAÇÕES HIERÁRQUICAS =========================

    @GetMapping("/arvore")
    @Operation(summary = "Obtém a árvore completa do organograma ativo")
    public ResponseEntity<List<NoOrganogramaDTO>> obterArvoreCompleta() {
        return ResponseEntity.ok(organogramaService.obterArvoreCompleta());
    }

    @GetMapping("/filhos")
    @Operation(summary = "Obtém os nós filhos de um nó pai")
    public ResponseEntity<List<NoOrganogramaDTO>> obterFilhos(
            @Parameter(description = "ID do nó pai (null para nós raiz)") @RequestParam(required = false) Long parentId) {
        return ResponseEntity.ok(organogramaService.obterFilhos(parentId));
    }

    @PutMapping("/{id}/mover")
    @Operation(summary = "Move um nó para outra posição na hierarquia")
    public ResponseEntity<NoOrganogramaDTO> moverNo(
            @Parameter(description = "ID do nó a ser movido") @PathVariable Long id,
            @Parameter(description = "ID do novo nó pai") @RequestParam(required = false) Long novoParentId,
            @Parameter(description = "Nova posição entre os irmãos") @RequestParam(required = false) Integer novaPosicao) {
        try {
            return ResponseEntity.ok(organogramaService.moverNo(id, novoParentId, novaPosicao));
        } catch (NoOrganogramaNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ========================= GESTÃO DO ORGANOGRAMA ATIVO =========================

    @GetMapping("/ativo")
    @Operation(summary = "Obtém o organograma atualmente ativo")
    public ResponseEntity<NoOrganogramaDTO> obterOrganogramaAtivo() {
        NoOrganogramaDTO organograma = organogramaService.obterOrganogramaAtivo();
        if (organograma != null) {
            return ResponseEntity.ok(organograma);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/ativar")
    @Operation(summary = "Ativa um organograma (nó raiz)")
    public ResponseEntity<Void> ativarOrganograma(
            @Parameter(description = "ID do nó raiz a ser ativado") @PathVariable Long id) {
        try {
            organogramaService.ativarOrganograma(id);
            return ResponseEntity.ok().build();
        } catch (NoOrganogramaNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/desativar")
    @Operation(summary = "Desativa o organograma atual")
    public ResponseEntity<Void> desativarOrganograma() {
        organogramaService.desativarOrganograma();
        return ResponseEntity.ok().build();
    }

    // ========================= ASSOCIAÇÕES COM FUNCIONÁRIOS =========================

    @PostMapping("/{noId}/funcionarios/{funcionarioId}")
    @Operation(summary = "Associa um funcionário a um nó do organograma")
    public ResponseEntity<FuncionarioOrganogramaDTO> associarFuncionario(
            @Parameter(description = "ID do nó") @PathVariable Long noId,
            @Parameter(description = "ID do funcionário") @PathVariable Long funcionarioId) {
        try {
            return ResponseEntity.ok(organogramaService.associarFuncionario(noId, funcionarioId));
        } catch (NoOrganogramaNotFoundException | FuncionarioNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{noId}/funcionarios/{funcionarioId}")
    @Operation(summary = "Desassocia um funcionário de um nó do organograma")
    public ResponseEntity<Void> desassociarFuncionario(
            @Parameter(description = "ID do nó") @PathVariable Long noId,
            @Parameter(description = "ID do funcionário") @PathVariable Long funcionarioId) {
        try {
            organogramaService.desassociarFuncionario(noId, funcionarioId);
            return ResponseEntity.noContent().build();
        } catch (NoOrganogramaNotFoundException | FuncionarioNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{noId}/funcionarios")
    @Operation(summary = "Lista funcionários associados a um nó")
    public ResponseEntity<List<FuncionarioOrganogramaDTO>> listarFuncionariosPorNo(
            @Parameter(description = "ID do nó") @PathVariable Long noId) {
        try {
            return ResponseEntity.ok(organogramaService.listarFuncionariosPorNo(noId));
        } catch (NoOrganogramaNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ========================= ASSOCIAÇÕES COM CENTROS DE CUSTO =========================

    @PostMapping("/{noId}/centros-custo/{centroCustoId}")
    @Operation(summary = "Associa um centro de custo a um nó do organograma")
    public ResponseEntity<CentroCustoOrganogramaDTO> associarCentroCusto(
            @Parameter(description = "ID do nó") @PathVariable Long noId,
            @Parameter(description = "ID do centro de custo") @PathVariable Long centroCustoId) {
        try {
            return ResponseEntity.ok(organogramaService.associarCentroCusto(noId, centroCustoId));
        } catch (NoOrganogramaNotFoundException | CentroCustoNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{noId}/centros-custo/{centroCustoId}")
    @Operation(summary = "Desassocia um centro de custo de um nó do organograma")
    public ResponseEntity<Void> desassociarCentroCusto(
            @Parameter(description = "ID do nó") @PathVariable Long noId,
            @Parameter(description = "ID do centro de custo") @PathVariable Long centroCustoId) {
        try {
            organogramaService.desassociarCentroCusto(noId, centroCustoId);
            return ResponseEntity.noContent().build();
        } catch (NoOrganogramaNotFoundException | CentroCustoNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{noId}/centros-custo")
    @Operation(summary = "Lista centros de custo associados a um nó")
    public ResponseEntity<List<CentroCustoOrganogramaDTO>> listarCentrosCustoPorNo(
            @Parameter(description = "ID do nó") @PathVariable Long noId) {
        try {
            return ResponseEntity.ok(organogramaService.listarCentrosCustoPorNo(noId));
        } catch (NoOrganogramaNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
} 