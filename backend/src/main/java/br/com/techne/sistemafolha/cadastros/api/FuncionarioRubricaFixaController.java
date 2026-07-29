package br.com.techne.sistemafolha.cadastros.api;

import br.com.techne.sistemafolha.cadastros.application.FuncionarioRubricaFixaService;
import br.com.techne.sistemafolha.cadastros.domain.FuncionarioRubricaFixaNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/funcionario-rubrica-fixa")
@RequiredArgsConstructor
@Tag(name = "Rubricas Fixas", description = "Custos fixos Techne por funcionário (INT-2)")
public class FuncionarioRubricaFixaController {

    private final FuncionarioRubricaFixaService funcionarioRubricaFixaService;

    @GetMapping
    @Operation(summary = "Lista rubricas fixas com filtros opcionais")
    public ResponseEntity<List<FuncionarioRubricaFixaDTO>> listar(
            @RequestParam(required = false) Long funcionarioId,
            @RequestParam(required = false) Long rubricaId) {
        return ResponseEntity.ok(funcionarioRubricaFixaService.listar(funcionarioId, rubricaId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca rubrica fixa por ID")
    public ResponseEntity<FuncionarioRubricaFixaDTO> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(funcionarioRubricaFixaService.buscarPorId(id));
        } catch (FuncionarioRubricaFixaNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "Cadastra rubrica fixa")
    public ResponseEntity<FuncionarioRubricaFixaDTO> criar(@Valid @RequestBody FuncionarioRubricaFixaDTO dto) {
        try {
            return ResponseEntity.ok(funcionarioRubricaFixaService.criar(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza rubrica fixa")
    public ResponseEntity<FuncionarioRubricaFixaDTO> atualizar(
            @PathVariable Long id, @Valid @RequestBody FuncionarioRubricaFixaDTO dto) {
        try {
            return ResponseEntity.ok(funcionarioRubricaFixaService.atualizar(id, dto));
        } catch (FuncionarioRubricaFixaNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove rubrica fixa (soft delete)")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        try {
            funcionarioRubricaFixaService.remover(id);
            return ResponseEntity.noContent().build();
        } catch (FuncionarioRubricaFixaNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
