package br.com.techne.sistemafolha.controller;

import br.com.techne.sistemafolha.dto.RubricaDTO;
import br.com.techne.sistemafolha.service.RubricaService;
import br.com.techne.sistemafolha.exception.RubricaNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Lista todas as rubricas ativas")
    public ResponseEntity<List<RubricaDTO>> listar() {
        return ResponseEntity.ok(rubricaService.listarTodas());
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