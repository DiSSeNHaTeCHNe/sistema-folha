package br.com.techne.sistemafolha.cadastros.api;

import br.com.techne.sistemafolha.cadastros.application.FuncionarioService;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioNotFoundException;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioJaExisteException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/funcionarios")
@RequiredArgsConstructor
@Tag(name = "Funcionários", description = "API para gerenciamento de funcionários")
public class FuncionarioController {
    private final FuncionarioService funcionarioService;

    @GetMapping
    @Operation(summary = "Lista funcionários com filtros opcionais e status de ativação")
    public ResponseEntity<List<FuncionarioDTO>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Long cargoId,
            @RequestParam(required = false) Long centroCustoId,
            @RequestParam(required = false) Long linhaNegocioId,
            @Parameter(description = "Filtro de status: ATIVO (padrão), INATIVO ou TODOS")
            @RequestParam(defaultValue = "ATIVO") FuncionarioStatusFiltro status,
            Authentication authentication) {
        return ResponseEntity.ok(funcionarioService.listarParaUsuario(
            authentication.getName(), nome, cargoId, centroCustoId, linhaNegocioId, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um funcionário ativo pelo ID")
    public ResponseEntity<FuncionarioDTO> buscarPorId(@PathVariable Long id, Authentication authentication) {
        try {
            return ResponseEntity.ok(funcionarioService.buscarPorIdParaUsuario(authentication.getName(), id));
        } catch (FuncionarioNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "Cadastra um novo funcionário")
    public ResponseEntity<FuncionarioDTO> cadastrar(@Valid @RequestBody FuncionarioDTO dto) {
        try {
            return ResponseEntity.ok(funcionarioService.cadastrar(dto));
        } catch (FuncionarioJaExisteException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um funcionário existente")
    public ResponseEntity<FuncionarioDTO> atualizar(@PathVariable Long id, @Valid @RequestBody FuncionarioDTO dto) {
        try {
            return ResponseEntity.ok(funcionarioService.atualizar(id, dto));
        } catch (FuncionarioNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um funcionário (soft delete)")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        try {
            funcionarioService.remover(id);
            return ResponseEntity.noContent().build();
        } catch (FuncionarioNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
} 