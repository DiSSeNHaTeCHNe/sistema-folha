package br.com.techne.sistemafolha.controller;

import br.com.techne.sistemafolha.dto.CargoDTO;
import br.com.techne.sistemafolha.service.CargoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cargos")
@RequiredArgsConstructor
@Tag(name = "Cargos", description = "API para gerenciamento de cargos")
public class CargoController {
    private final CargoService cargoService;

    @GetMapping
    @Operation(summary = "Lista todos os cargos ativos")
    public ResponseEntity<List<CargoDTO>> listarTodas() {
        return ResponseEntity.ok(cargoService.listarTodas());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um cargo pelo ID")
    public ResponseEntity<CargoDTO> buscarPorId(
            @Parameter(description = "ID do cargo") @PathVariable Long id) {
        return ResponseEntity.ok(cargoService.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cadastra um novo cargo")
    public ResponseEntity<CargoDTO> cadastrar(
            @Parameter(description = "Dados do cargo") @Valid @RequestBody CargoDTO cargo) {
        return ResponseEntity.ok(cargoService.cadastrar(cargo));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um cargo existente")
    public ResponseEntity<CargoDTO> atualizar(
            @Parameter(description = "ID do cargo") @PathVariable Long id,
            @Parameter(description = "Dados atualizados do cargo") @Valid @RequestBody CargoDTO cargo) {
        return ResponseEntity.ok(cargoService.atualizar(id, cargo));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um cargo")
    public ResponseEntity<Void> remover(
            @Parameter(description = "ID do cargo") @PathVariable Long id) {
        cargoService.remover(id);
        return ResponseEntity.noContent().build();
    }
} 