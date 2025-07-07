package br.com.techne.sistemafolha.controller;

import br.com.techne.sistemafolha.dto.ResumoFolhaPagamentoDTO;
import br.com.techne.sistemafolha.model.ResumoFolhaPagamento;
import br.com.techne.sistemafolha.repository.ResumoFolhaPagamentoRepository;
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
@RequestMapping("/resumo-folha-pagamento")
@RequiredArgsConstructor
@Tag(name = "Resumo Folha de Pagamento", description = "API para consulta de resumos de folha de pagamento")
public class ResumoFolhaPagamentoController {
    private final ResumoFolhaPagamentoRepository resumoFolhaPagamentoRepository;

    @GetMapping
    @Operation(summary = "Lista todos os resumos de folha de pagamento ativos")
    public ResponseEntity<List<ResumoFolhaPagamentoDTO>> listarTodos() {
        List<ResumoFolhaPagamentoDTO> resumos = resumoFolhaPagamentoRepository.findByAtivoTrue()
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(resumos);
    }

    @GetMapping("/periodo")
    @Operation(summary = "Consulta resumos por período")
    public ResponseEntity<List<ResumoFolhaPagamentoDTO>> consultarPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        List<ResumoFolhaPagamentoDTO> resumos = resumoFolhaPagamentoRepository
            .findByCompetenciaInicioBetweenAndAtivoTrue(dataInicio, dataFim)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(resumos);
    }

    @GetMapping("/competencia")
    @Operation(summary = "Consulta resumo por competência específica")
    public ResponseEntity<ResumoFolhaPagamentoDTO> consultarPorCompetencia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competenciaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate competenciaFim) {
        return resumoFolhaPagamentoRepository
            .findByCompetenciaInicioAndCompetenciaFimAndAtivoTrue(competenciaInicio, competenciaFim)
            .map(this::toDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/latest")
    @Operation(summary = "Lista os resumos mais recentes")
    public ResponseEntity<List<ResumoFolhaPagamentoDTO>> listarMaisRecentes() {
        List<ResumoFolhaPagamentoDTO> resumos = resumoFolhaPagamentoRepository.findLatestResumos()
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(resumos);
    }

    private ResumoFolhaPagamentoDTO toDTO(ResumoFolhaPagamento resumo) {
        return new ResumoFolhaPagamentoDTO(
            resumo.getId(),
            resumo.getTotalEmpregados(),
            resumo.getTotalEncargos(),
            resumo.getTotalPagamentos(),
            resumo.getTotalDescontos(),
            resumo.getTotalLiquido(),
            resumo.getCompetenciaInicio(),
            resumo.getCompetenciaFim(),
            resumo.getDataImportacao(),
            resumo.getAtivo()
        );
    }
} 