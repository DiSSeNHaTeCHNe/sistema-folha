package br.com.techne.sistemafolha.folha.api;

import br.com.techne.sistemafolha.folha.application.ResumoFolhaPagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/resumo-folha-pagamento")
@RequiredArgsConstructor
@Tag(name = "Resumo Folha de Pagamento", description = "API para consulta de resumos de folha de pagamento")
public class ResumoFolhaPagamentoController {

    private final ResumoFolhaPagamentoService resumoFolhaPagamentoService;

    @GetMapping
    @Operation(summary = "Lista resumos de folha de pagamento ativos por ano")
    public ResponseEntity<List<ResumoFolhaPagamentoDTO>> listarTodos(
            @Parameter(description = "Ano de competência (2000–2100). Default: ano corrente quando omitido.")
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer ano,
            @Parameter(description = "Mês de competência (1–12). Opcional; restringe ao mês dentro do ano.")
            @RequestParam(required = false) @Min(1) @Max(12) Integer mes,
            Authentication authentication) {
        return ResponseEntity.ok(resumoFolhaPagamentoService.listarTodos(authentication.getName(), ano, mes));
    }

    @GetMapping("/periodo")
    @Operation(summary = "Consulta resumos por período")
    public ResponseEntity<List<ResumoFolhaPagamentoDTO>> consultarPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            Authentication authentication) {
        return ResponseEntity.ok(resumoFolhaPagamentoService.consultarPorPeriodo(
            authentication.getName(), dataInicio, dataFim));
    }

    @GetMapping("/competencia")
    @Operation(summary = "Consulta resumo por competência específica")
    public ResponseEntity<ResumoFolhaPagamentoDTO> consultarPorCompetencia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competenciaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competenciaFim,
            Authentication authentication) {
        return resumoFolhaPagamentoService.consultarPorCompetencia(
                authentication.getName(), competenciaInicio, competenciaFim)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/latest")
    @Operation(summary = "Lista os resumos mais recentes")
    public ResponseEntity<List<ResumoFolhaPagamentoDTO>> listarMaisRecentes(Authentication authentication) {
        return ResponseEntity.ok(resumoFolhaPagamentoService.listarMaisRecentes(authentication.getName()));
    }
}
