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
@RequestMapping("/relatorios/beneficio")
@RequiredArgsConstructor
@Tag(name = "Relatórios Benefício", description = "Geração e download de relatório de custo benefício + folha")
public class RelatorioBeneficioController {

    private final RelatorioGeracaoService relatorioGeracaoService;

    @PostMapping
    @Operation(summary = "Gera relatório de custo benefício + folha para a competência informada")
    public ResponseEntity<RelatorioBeneficioDTO> gerar(
            @Valid @RequestBody GerarRelatorioRequest request,
            Authentication authentication) {
        RelatorioBeneficioDTO dto = relatorioGeracaoService.gerarBeneficio(
            authentication.getName(), request.mes(), request.ano());
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    @Operation(summary = "Lista relatórios de benefício gerados pelo usuário")
    public ResponseEntity<List<RelatorioBeneficioDTO>> listar(Authentication authentication) {
        return ResponseEntity.ok(relatorioGeracaoService.listarBeneficio(authentication.getName()));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download do PDF do relatório de benefício")
    public ResponseEntity<byte[]> download(
            @PathVariable Long id,
            Authentication authentication) {
        byte[] pdf = relatorioGeracaoService.downloadPdf(
            authentication.getName(), id, RelatorioTipo.BENEFICIO);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"relatorio-beneficio-" + id + ".pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
}
