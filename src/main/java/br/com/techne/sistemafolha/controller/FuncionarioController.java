package br.com.techne.sistemafolha.controller;

import br.com.techne.sistemafolha.dto.FuncionarioDTO;
import br.com.techne.sistemafolha.service.FuncionarioService;
import br.com.techne.sistemafolha.exception.FuncionarioNotFoundException;
import br.com.techne.sistemafolha.exception.FuncionarioJaExisteException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/funcionarios")
@RequiredArgsConstructor
@Tag(name = "Funcionários", description = "API para gerenciamento de funcionários")
public class FuncionarioController {
    private final FuncionarioService funcionarioService;

    @GetMapping
    @Operation(summary = "Lista todos os funcionários ativos")
    public ResponseEntity<List<FuncionarioDTO>> listar() {
        return ResponseEntity.ok(funcionarioService.listarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um funcionário ativo pelo ID")
    public ResponseEntity<FuncionarioDTO> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(funcionarioService.buscarPorId(id));
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