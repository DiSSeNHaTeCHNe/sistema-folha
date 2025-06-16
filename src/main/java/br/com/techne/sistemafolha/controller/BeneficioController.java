package br.com.techne.sistemafolha.controller;

import br.com.techne.sistemafolha.dto.BeneficioDTO;
import br.com.techne.sistemafolha.service.BeneficioService;
import br.com.techne.sistemafolha.exception.BeneficioNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/beneficios")
@RequiredArgsConstructor
@Tag(name = "Benefícios", description = "API para consulta de benefícios")
public class BeneficioController {
    private final BeneficioService beneficioService;

    @GetMapping("/funcionario/{funcionarioId}")
    @Operation(summary = "Consulta benefícios ativos por funcionário")
    public ResponseEntity<List<BeneficioDTO>> consultarPorFuncionario(
            @PathVariable Long funcionarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(beneficioService.consultarPorFuncionario(funcionarioId, data));
    }

    @GetMapping("/centro-custo/{centroCusto}")
    @Operation(summary = "Consulta benefícios ativos por centro de custo")
    public ResponseEntity<List<BeneficioDTO>> consultarPorCentroCusto(
            @PathVariable Long centroCusto,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(beneficioService.consultarPorCentroCusto(centroCusto, data));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um benefício (soft delete)")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        try {
            beneficioService.remover(id);
            return ResponseEntity.noContent().build();
        } catch (BeneficioNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
} 