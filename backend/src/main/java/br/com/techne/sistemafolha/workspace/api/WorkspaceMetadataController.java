package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.workspace.port.WorkspaceConsultaPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/workspace/metadata")
@RequiredArgsConstructor
@Tag(name = "Workspace Metadata", description = "Metadados ACL-scoped para IA (WKS-28)")
public class WorkspaceMetadataController {

    private final WorkspaceConsultaPort workspaceConsultaPort;

    @GetMapping("/templates")
    @Operation(summary = "Lista templates visíveis ao usuário", operationId = "listarTemplatesVisiveis")
    public ResponseEntity<List<TemplateCatalogItemDTO>> listarTemplates(Authentication authentication) {
        return ResponseEntity.ok(workspaceConsultaPort.listarTemplatesVisiveis(authentication.getName()));
    }

    @GetMapping("/system-fields")
    @Operation(summary = "Lista campos de fontes sistema acessíveis", operationId = "listarCamposSistema")
    public ResponseEntity<List<SystemFieldDescriptorDTO>> listarCamposSistema(Authentication authentication) {
        return ResponseEntity.ok(workspaceConsultaPort.listarCamposSistema(authentication.getName()));
    }
}
