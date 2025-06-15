package br.com.techne.sistemafolha.controller;

import br.com.techne.sistemafolha.dto.BeneficioDTO;
import br.com.techne.sistemafolha.model.Beneficio;
import br.com.techne.sistemafolha.repository.BeneficioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/beneficios")
@RequiredArgsConstructor
@Tag(name = "Benefícios", description = "API para consulta de benefícios")
public class BeneficioController {
    private final BeneficioRepository beneficioRepository;

    @GetMapping("/funcionario/{funcionarioId}")
    @Operation(summary = "Consulta benefícios ativos por funcionário")
    public ResponseEntity<List<BeneficioDTO>> consultarPorFuncionario(
            @PathVariable Long funcionarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        List<BeneficioDTO> beneficios = beneficioRepository.findByFuncionarioIdAndDataAndAtivoTrue(funcionarioId, data)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(beneficios);
    }

    @GetMapping("/centro-custo/{centroCusto}")
    @Operation(summary = "Consulta benefícios ativos por centro de custo")
    public ResponseEntity<List<BeneficioDTO>> consultarPorCentroCusto(
            @PathVariable String centroCusto,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        List<BeneficioDTO> beneficios = beneficioRepository.findByFuncionarioCentroCustoAndDataAndAtivoTrue(centroCusto, data)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(beneficios);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um benefício (soft delete)")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        return beneficioRepository.findById(id)
            .map(beneficio -> {
                beneficioRepository.softDelete(id);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    private BeneficioDTO toDTO(Beneficio beneficio) {
        return new BeneficioDTO(
            beneficio.getId(),
            beneficio.getFuncionario().getId(),
            beneficio.getFuncionario().getNome(),
            beneficio.getDescricao(),
            beneficio.getValor(),
            beneficio.getDataInicio(),
            beneficio.getDataFim(),
            beneficio.getObservacao()
        );
    }
} 