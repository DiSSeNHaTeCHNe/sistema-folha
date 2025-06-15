package br.com.techne.sistemafolha.controller;

import br.com.techne.sistemafolha.dto.FolhaPagamentoDTO;
import br.com.techne.sistemafolha.model.FolhaPagamento;
import br.com.techne.sistemafolha.repository.FolhaPagamentoRepository;
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
@RequestMapping("/folha-pagamento")
@RequiredArgsConstructor
@Tag(name = "Folha de Pagamento", description = "API para consulta de folha de pagamento")
public class FolhaPagamentoController {
    private final FolhaPagamentoRepository folhaPagamentoRepository;

    @GetMapping("/funcionario/{funcionarioId}")
    @Operation(summary = "Consulta folha de pagamento ativa por funcionário")
    public ResponseEntity<List<FolhaPagamentoDTO>> consultarPorFuncionario(
            @PathVariable Long funcionarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        List<FolhaPagamentoDTO> folha = folhaPagamentoRepository
            .findByFuncionarioIdAndDataInicioBetweenAndAtivoTrue(funcionarioId, dataInicio, dataFim)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(folha);
    }

    @GetMapping("/centro-custo/{centroCusto}")
    @Operation(summary = "Consulta folha de pagamento ativa por centro de custo")
    public ResponseEntity<List<FolhaPagamentoDTO>> consultarPorCentroCusto(
            @PathVariable String centroCusto,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        List<FolhaPagamentoDTO> folha = folhaPagamentoRepository
            .findByFuncionarioCentroCustoAndDataInicioBetweenAndAtivoTrue(centroCusto, dataInicio, dataFim)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(folha);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um registro de folha de pagamento (soft delete)")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        return folhaPagamentoRepository.findById(id)
            .map(folha -> {
                folhaPagamentoRepository.softDelete(id);
                return ResponseEntity.noContent().<Void>build();
            })
            .orElse(ResponseEntity.notFound().build());
    }

    private FolhaPagamentoDTO toDTO(FolhaPagamento folha) {
        return new FolhaPagamentoDTO(
            folha.getId(),
            folha.getFuncionario().getId(),
            folha.getFuncionario().getNome(),
            folha.getRubrica().getId(),
            folha.getRubrica().getCodigo(),
            folha.getRubrica().getDescricao(),
            folha.getDataInicio(),
            folha.getDataFim(),
            folha.getValor(),
            folha.getQuantidade(),
            folha.getBaseCalculo()
        );
    }
} 