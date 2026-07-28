package br.com.techne.sistemafolha.folha.api;

import br.com.techne.sistemafolha.folha.application.FolhaFichaConsultaService;
import br.com.techne.sistemafolha.folha.domain.FichaMensalNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/folha-pagamento/fichas")
@RequiredArgsConstructor
@Tag(name = "Folha de Pagamento", description = "Detalhe de ficha mensal por totalizador")
public class FolhaFichaController {

    private final FolhaFichaConsultaService folhaFichaConsultaService;

    @GetMapping("/por-funcionario")
    @Operation(summary = "Resolve ficha mensal por funcionário e competência")
    public ResponseEntity<Map<String, Long>> buscarFichaPorFuncionario(
            @RequestParam Long funcionarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @RequestParam(required = false) Boolean decimoTerceiro,
            Authentication authentication) {
        try {
            Long fichaId = folhaFichaConsultaService.buscarFichaIdPorFuncionario(
                authentication.getName(),
                funcionarioId,
                dataInicio,
                dataFim,
                Boolean.TRUE.equals(decimoTerceiro));
            return ResponseEntity.ok(Map.of("id", fichaId));
        } catch (FichaMensalNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/linhas")
    @Operation(summary = "Lista linhas da ficha filtradas por totalizador (Bruto/Líquido/Custo)")
    public ResponseEntity<List<FichaLinhaDetalheDTO>> listarLinhasPorTotalizador(
            @PathVariable Long id,
            @RequestParam Totalizador totalizer,
            Authentication authentication) {
        try {
            return ResponseEntity.ok(folhaFichaConsultaService.listarLinhasPorTotalizador(
                authentication.getName(), id, totalizer));
        } catch (FichaMensalNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
