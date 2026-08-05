package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.workspace.application.WidgetQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workspace/workspaces/{workspaceId}/widgets/{instanceId}/data")
@RequiredArgsConstructor
@Tag(name = "Workspace Widget Data", description = "Dados para renderização de widgets do workspace")
public class WorkspaceWidgetDataController {

    private final WidgetQueryService widgetQueryService;

    @GetMapping
    @Operation(summary = "Obtém dados do widget no workspace")
    public ResponseEntity<WorkspaceWidgetDataDTO> obterDados(
            Authentication authentication,
            @PathVariable Long workspaceId,
            @PathVariable String instanceId,
            @RequestParam(required = false) String competencia) {
        WorkspaceWidgetQueryParams params = new WorkspaceWidgetQueryParams(competencia);
        return ResponseEntity.ok(
            widgetQueryService.obterDados(authentication.getName(), workspaceId, instanceId, params));
    }
}
