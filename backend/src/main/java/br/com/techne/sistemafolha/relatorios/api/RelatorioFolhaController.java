package br.com.techne.sistemafolha.relatorios.api;

import br.com.techne.sistemafolha.relatorios.application.RelatorioGeracaoService;
import br.com.techne.sistemafolha.relatorios.domain.RelatorioTipo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/relatorios/folha")
@RequiredArgsConstructor
@Tag(name = "Relatórios Folha", description = "Geração e download de relatório executivo de folha")
public class RelatorioFolhaController {

    private final RelatorioGeracaoService relatorioGeracaoService;

    @PostMapping
    @Operation(summary = "Gera relatório executivo de folha para a competência informada")
    public ResponseEntity<RelatorioFolhaDTO> gerar(
            @Valid @RequestBody GerarRelatorioRequest request,
            Authentication authentication) {
        RelatorioFolhaDTO dto = relatorioGeracaoService.gerarFolha(
            authentication.getName(), request.mes(), request.ano());
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    @Operation(summary = "Lista relatórios de folha gerados pelo usuário")
    public ResponseEntity<List<RelatorioFolhaDTO>> listar(Authentication authentication) {
        return ResponseEntity.ok(relatorioGeracaoService.listarFolha(authentication.getName()));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download do PDF do relatório de folha")
    public ResponseEntity<byte[]> download(
            @PathVariable Long id,
            Authentication authentication) {
        byte[] pdf = relatorioGeracaoService.downloadPdf(
            authentication.getName(), id, RelatorioTipo.FOLHA);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"relatorio-folha-" + id + ".pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
}
