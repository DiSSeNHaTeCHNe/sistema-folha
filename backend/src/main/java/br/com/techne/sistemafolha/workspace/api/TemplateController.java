package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.workspace.application.OrcamentoTemplateInstaller;
import br.com.techne.sistemafolha.workspace.application.TemplateInstallService;
import br.com.techne.sistemafolha.workspace.application.TemplatePublishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/workspace/templates")
@RequiredArgsConstructor
@Tag(name = "Workspace Templates", description = "Marketplace de templates e instalação nativa")
public class TemplateController {

    private final OrcamentoTemplateInstaller orcamentoTemplateInstaller;
    private final TemplatePublishService templatePublishService;
    private final TemplateInstallService templateInstallService;

    @PostMapping("/orcamento-padrao/install")
    @Operation(summary = "Instala template nativo de orçamento no workspace")
    public ResponseEntity<OrcamentoInstallResultDTO> instalarOrcamentoPadrao(
            Authentication authentication,
            @Valid @RequestBody OrcamentoInstallRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(orcamentoTemplateInstaller.instalarOrcamentoPadrao(
                authentication.getName(), request.workspaceId()));
    }

    @PostMapping("/publish")
    @Operation(summary = "Publica dataset ou widget como template (estrutura apenas, WKS-15)")
    public ResponseEntity<TemplateDTO> publicar(
            Authentication authentication,
            @Valid @RequestBody PublishTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(templatePublishService.publicar(authentication.getName(), request));
    }

    @GetMapping("/catalog")
    @Operation(summary = "Lista catálogo de templates visíveis na hierarquia (WKS-16)")
    public ResponseEntity<List<TemplateCatalogItemDTO>> listarCatalogo(Authentication authentication) {
        return ResponseEntity.ok(templatePublishService.listarCatalogo(authentication.getName()));
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "Lista versões do template para diff (WKS2-26)")
    public ResponseEntity<List<TemplateVersionSummaryDTO>> listarVersoes(
            Authentication authentication,
            @PathVariable Long id) {
        return ResponseEntity.ok(templatePublishService.listarVersoes(authentication.getName(), id));
    }

    @PostMapping("/{id}/install")
    @Operation(summary = "Instala template publicado como cópia independente (WKS-17)")
    public ResponseEntity<TemplateInstallResultDTO> instalar(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody InstallTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(templateInstallService.instalar(authentication.getName(), id, request.workspaceId()));
    }

    @PostMapping("/installations/{installationId}/upgrade")
    @Operation(summary = "Atualiza instalação para versão mais recente (WKS-20/21)")
    public ResponseEntity<TemplateInstallResultDTO> atualizarVersao(
            Authentication authentication,
            @PathVariable Long installationId) {
        return ResponseEntity.ok(
            templateInstallService.atualizarVersao(authentication.getName(), installationId));
    }
}
