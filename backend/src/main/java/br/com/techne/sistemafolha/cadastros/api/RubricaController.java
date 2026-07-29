package br.com.techne.sistemafolha.cadastros.api;

import br.com.techne.sistemafolha.cadastros.application.RubricaService;
import br.com.techne.sistemafolha.cadastros.domain.RubricaNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rubricas")
@RequiredArgsConstructor
@Tag(name = "Rubricas", description = "API para gerenciamento de rubricas")
public class RubricaController {
    private final RubricaService rubricaService;

    @GetMapping
    @Operation(summary = "Lista rubricas com filtros opcionais")
    public ResponseEntity<List<RubricaDTO>> listar(
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String descricao,
            @Parameter(description = "Filtro de status: ATIVO (padrão), INATIVO ou TODOS")
            @RequestParam(defaultValue = "ATIVO") RubricaStatusFiltro status) {
        return ResponseEntity.ok(rubricaService.listar(codigo, descricao, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma rubrica ativa pelo ID")
    public ResponseEntity<RubricaDTO> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(rubricaService.buscarPorId(id));
        } catch (RubricaNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "Cadastra uma nova rubrica")
    public ResponseEntity<RubricaDTO> cadastrar(@Valid @RequestBody RubricaDTO dto) {
        try {
            return ResponseEntity.ok(rubricaService.cadastrar(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma rubrica existente")
    public ResponseEntity<RubricaDTO> atualizar(@PathVariable Long id, @Valid @RequestBody RubricaDTO dto) {
        try {
            return ResponseEntity.ok(rubricaService.atualizar(id, dto));
        } catch (RubricaNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma rubrica (soft delete)")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        try {
            rubricaService.remover(id);
            return ResponseEntity.noContent().build();
        } catch (RubricaNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
} 