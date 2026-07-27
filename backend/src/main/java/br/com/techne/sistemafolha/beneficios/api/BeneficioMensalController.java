package br.com.techne.sistemafolha.beneficios.api;

import br.com.techne.sistemafolha.beneficios.api.BeneficioMensalDTO;
import br.com.techne.sistemafolha.beneficios.api.BeneficioMensalResumoDTO;
import br.com.techne.sistemafolha.beneficios.application.BeneficioMensalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/beneficio-mensal")
@RequiredArgsConstructor
@Tag(name = "Benefícios Mensais", description = "API para consulta e lançamento de benefícios mensais")
public class BeneficioMensalController {

    private final BeneficioMensalService beneficioMensalService;

    @GetMapping
    @Operation(summary = "Lista lançamentos de benefícios mensais por competência")
    public ResponseEntity<List<BeneficioMensalDTO>> listarPorCompetencia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competenciaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competenciaFim,
            Authentication authentication) {
        return ResponseEntity.ok(beneficioMensalService.listarPorCompetenciaParaUsuario(
            authentication.getName(), competenciaInicio, competenciaFim));
    }

    @GetMapping("/resumo")
    @Operation(summary = "Resumo de benefícios mensais agrupado por tipo na competência")
    public ResponseEntity<List<BeneficioMensalResumoDTO>> resumoPorCompetencia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competenciaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competenciaFim,
            Authentication authentication) {
        return ResponseEntity.ok(beneficioMensalService.resumoPorCompetenciaParaUsuario(
            authentication.getName(), competenciaInicio, competenciaFim));
    }

    @GetMapping("/funcionario/{id}")
    @Operation(summary = "Lista lançamentos de benefícios mensais de um funcionário na competência")
    public ResponseEntity<List<BeneficioMensalDTO>> listarPorFuncionario(
            @Parameter(description = "ID do funcionário") @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competenciaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competenciaFim,
            Authentication authentication) {
        return ResponseEntity.ok(beneficioMensalService.listarPorFuncionarioParaUsuario(
            authentication.getName(), id, competenciaInicio, competenciaFim));
    }

    @PostMapping
    @Operation(summary = "Cria um lançamento manual de benefício mensal")
    public ResponseEntity<BeneficioMensalDTO> criar(
            @Parameter(description = "Dados do lançamento") @Valid @RequestBody BeneficioMensalDTO dto,
            Authentication authentication) {
        return beneficioMensalService.criarParaUsuario(authentication.getName(), dto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um lançamento de benefício mensal (soft delete)")
    public ResponseEntity<Void> remover(
            @Parameter(description = "ID do lançamento") @PathVariable Long id,
            Authentication authentication) {
        return beneficioMensalService.removerSeAutorizado(authentication.getName(), id)
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }
}
