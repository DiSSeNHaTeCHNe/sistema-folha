package br.com.techne.sistemafolha.folha.api;

import br.com.techne.sistemafolha.folha.api.ResumoFolhaPagamentoDTO;
import br.com.techne.sistemafolha.folha.application.ResumoFolhaPagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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
    @Operation(summary = "Lista todos os resumos de folha de pagamento ativos")
    public ResponseEntity<List<ResumoFolhaPagamentoDTO>> listarTodos() {
        return ResponseEntity.ok(resumoFolhaPagamentoService.listarTodos());
    }

    @GetMapping("/periodo")
    @Operation(summary = "Consulta resumos por período")
    public ResponseEntity<List<ResumoFolhaPagamentoDTO>> consultarPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return ResponseEntity.ok(resumoFolhaPagamentoService.consultarPorPeriodo(dataInicio, dataFim));
    }

    @GetMapping("/competencia")
    @Operation(summary = "Consulta resumo por competência específica")
    public ResponseEntity<ResumoFolhaPagamentoDTO> consultarPorCompetencia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competenciaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competenciaFim) {
        return resumoFolhaPagamentoService.consultarPorCompetencia(competenciaInicio, competenciaFim)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/latest")
    @Operation(summary = "Lista os resumos mais recentes")
    public ResponseEntity<List<ResumoFolhaPagamentoDTO>> listarMaisRecentes() {
        return ResponseEntity.ok(resumoFolhaPagamentoService.listarMaisRecentes());
    }
}
