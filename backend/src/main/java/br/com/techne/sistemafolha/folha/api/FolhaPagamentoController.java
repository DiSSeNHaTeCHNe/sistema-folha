package br.com.techne.sistemafolha.folha.api;

import br.com.techne.sistemafolha.folha.api.FolhaPagamentoDTO;
import br.com.techne.sistemafolha.folha.api.FolhaTotaisFuncionarioDTO;
import br.com.techne.sistemafolha.folha.application.FolhaPagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/folha-pagamento")
@RequiredArgsConstructor
@Tag(name = "Folha de Pagamento", description = "API para consulta de folha de pagamento")
public class FolhaPagamentoController {

    private final FolhaPagamentoService folhaPagamentoService;

    @GetMapping("/funcionario/{funcionarioId}")
    @Operation(summary = "Consulta folha de pagamento ativa por funcionário")
    public ResponseEntity<List<FolhaPagamentoDTO>> consultarPorFuncionario(
            @PathVariable Long funcionarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            Authentication authentication) {
        return ResponseEntity.ok(folhaPagamentoService.consultarPorFuncionario(
            authentication.getName(), funcionarioId, dataInicio, dataFim));
    }

    @GetMapping("/centro-custo/{centroCustoId}")
    @Operation(summary = "Consulta folha de pagamento ativa por centro de custo")
    public ResponseEntity<List<FolhaPagamentoDTO>> consultarPorCentroCusto(
            @PathVariable Long centroCustoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            Authentication authentication) {
        return ResponseEntity.ok(folhaPagamentoService.consultarPorCentroCusto(
            authentication.getName(), centroCustoId, dataInicio, dataFim));
    }

    @GetMapping("/linha-negocio/{linhaNegocioId}")
    @Operation(summary = "Consulta folha de pagamento ativa por linha de negócio")
    public ResponseEntity<List<FolhaPagamentoDTO>> consultarPorLinhaNegocio(
            @PathVariable Long linhaNegocioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            Authentication authentication) {
        return ResponseEntity.ok(folhaPagamentoService.consultarPorLinhaNegocio(
            authentication.getName(), linhaNegocioId, dataInicio, dataFim));
    }

    @GetMapping
    @Operation(summary = "Consulta folha de pagamento ativa por período (mês/ano) - Filtra automaticamente pelos centros de custo acessíveis")
    public ResponseEntity<List<FolhaPagamentoDTO>> consultarPorPeriodo(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            Authentication authentication) {
        return ResponseEntity.ok(folhaPagamentoService.consultarPorPeriodo(
            authentication.getName(), dataInicio, dataFim));
    }

    @GetMapping("/totais-funcionarios")
    @Operation(summary = "Totaliza bruto, líquido e custo Techne por funcionário no período")
    public ResponseEntity<List<FolhaTotaisFuncionarioDTO>> consultarTotaisPorFuncionario(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            Authentication authentication) {
        return ResponseEntity.ok(folhaPagamentoService.consultarTotaisPorFuncionario(
            authentication.getName(), dataInicio, dataFim));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um registro de folha de pagamento (soft delete)")
    public ResponseEntity<Void> remover(@PathVariable Long id, Authentication authentication) {
        return folhaPagamentoService.removerSeAutorizado(authentication.getName(), id)
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }
}
