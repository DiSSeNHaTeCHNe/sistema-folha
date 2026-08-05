package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.workspace.application.OrcamentoTemplateInstaller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workspace/templates")
@RequiredArgsConstructor
@Tag(name = "Workspace Templates", description = "Instalação de templates nativos")
public class TemplateController {

    private final OrcamentoTemplateInstaller orcamentoTemplateInstaller;

    @PostMapping("/orcamento-padrao/install")
    @Operation(summary = "Instala template nativo de orçamento no workspace")
    public ResponseEntity<OrcamentoInstallResultDTO> instalarOrcamentoPadrao(
            Authentication authentication,
            @Valid @RequestBody OrcamentoInstallRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(orcamentoTemplateInstaller.instalarOrcamentoPadrao(
                authentication.getName(), request.workspaceId()));
    }
}
