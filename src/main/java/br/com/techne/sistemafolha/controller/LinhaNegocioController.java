package br.com.techne.sistemafolha.controller;

import br.com.techne.sistemafolha.dto.LinhaNegocioDTO;
import br.com.techne.sistemafolha.service.LinhaNegocioService;
import br.com.techne.sistemafolha.exception.LinhaNegocioNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/linhas-negocio")
@RequiredArgsConstructor
@Tag(name = "Linhas de Negócio", description = "API para gerenciamento de linhas de negócio")
public class LinhaNegocioController {
    private final LinhaNegocioService linhaNegocioService;

    @GetMapping
    @Operation(summary = "Lista todas as linhas de negócio ativas")
    public ResponseEntity<List<LinhaNegocioDTO>> listarTodos() {
        return ResponseEntity.ok(linhaNegocioService.listarTodas());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma linha de negócio pelo ID")
    public ResponseEntity<LinhaNegocioDTO> buscarPorId(
            @Parameter(description = "ID da linha de negócio") @PathVariable Long id) {
        try {
            return ResponseEntity.ok(linhaNegocioService.buscarPorId(id));
        } catch (LinhaNegocioNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Operation(summary = "Cadastra uma nova linha de negócio")
    public ResponseEntity<LinhaNegocioDTO> cadastrar(
            @Parameter(description = "Dados da linha de negócio") @Valid @RequestBody LinhaNegocioDTO linhaNegocio) {
        try {
            return ResponseEntity.ok(linhaNegocioService.cadastrar(linhaNegocio));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza uma linha de negócio existente")
    public ResponseEntity<LinhaNegocioDTO> atualizar(
            @Parameter(description = "ID da linha de negócio") @PathVariable Long id,
            @Parameter(description = "Dados atualizados da linha de negócio") @Valid @RequestBody LinhaNegocioDTO linhaNegocio) {
        try {
            return ResponseEntity.ok(linhaNegocioService.atualizar(id, linhaNegocio));
        } catch (LinhaNegocioNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma linha de negócio")
    public ResponseEntity<Void> remover(
            @Parameter(description = "ID da linha de negócio") @PathVariable Long id) {
        try {
            linhaNegocioService.remover(id);
            return ResponseEntity.noContent().build();
        } catch (LinhaNegocioNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
} 