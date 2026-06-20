package br.com.techne.sistemafolha.controller;

import br.com.techne.sistemafolha.dto.CentroCustoDTO;
import br.com.techne.sistemafolha.service.CentroCustoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/centros-custo")
@RequiredArgsConstructor
@Tag(name = "Centros de Custo", description = "API para gerenciamento de centros de custo")
public class CentroCustoController {
    private final CentroCustoService centroCustoService;

    @GetMapping
    @Operation(summary = "Lista todos os centros de custo ativos")
    public ResponseEntity<List<CentroCustoDTO>> listarTodos() {
        return ResponseEntity.ok(centroCustoService.listarTodas());
    }

    @GetMapping("/linha-negocio/{linhaNegocioId}")
    @Operation(summary = "Lista centros de custo por linha de negócio")
    public ResponseEntity<List<CentroCustoDTO>> listarPorLinhaNegocio(
            @Parameter(description = "ID da linha de negócio") @PathVariable Long linhaNegocioId) {
        return ResponseEntity.ok(centroCustoService.listarPorLinhaNegocio(linhaNegocioId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um centro de custo pelo ID")
    public ResponseEntity<CentroCustoDTO> buscarPorId(
            @Parameter(description = "ID do centro de custo") @PathVariable Long id) {
        return ResponseEntity.ok(centroCustoService.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cadastra um novo centro de custo")
    public ResponseEntity<CentroCustoDTO> cadastrar(
            @Parameter(description = "Dados do centro de custo") @Valid @RequestBody CentroCustoDTO centroCusto) {
        return ResponseEntity.ok(centroCustoService.cadastrar(centroCusto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um centro de custo existente")
    public ResponseEntity<CentroCustoDTO> atualizar(
            @Parameter(description = "ID do centro de custo") @PathVariable Long id,
            @Parameter(description = "Dados atualizados do centro de custo") @Valid @RequestBody CentroCustoDTO centroCusto) {
        return ResponseEntity.ok(centroCustoService.atualizar(id, centroCusto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um centro de custo")
    public ResponseEntity<Void> remover(
            @Parameter(description = "ID do centro de custo") @PathVariable Long id) {
        centroCustoService.remover(id);
        return ResponseEntity.noContent().build();
    }
} 