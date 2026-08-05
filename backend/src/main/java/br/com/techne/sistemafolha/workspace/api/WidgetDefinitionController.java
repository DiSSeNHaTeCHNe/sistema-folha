package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.workspace.application.WidgetDefinitionService;
import br.com.techne.sistemafolha.workspace.application.WidgetQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/workspace/widget-definitions")
@RequiredArgsConstructor
@Tag(name = "Workspace Widget Definitions", description = "CRUD de widgets definidos pelo usuário")
public class WidgetDefinitionController {

    private final WidgetDefinitionService widgetDefinitionService;
    private final WidgetQueryService widgetQueryService;

    @GetMapping
    @Operation(summary = "Lista definições de widget do usuário")
    public ResponseEntity<List<WidgetDefinitionDTO>> listar(Authentication authentication) {
        return ResponseEntity.ok(widgetDefinitionService.listar(authentication.getName()));
    }

    @PostMapping
    @Operation(summary = "Cria definição de widget")
    public ResponseEntity<WidgetDefinitionDTO> criar(
            Authentication authentication,
            @Valid @RequestBody CreateWidgetDefinitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(widgetDefinitionService.criar(authentication.getName(), request));
    }

    @PostMapping("/preview")
    @Operation(summary = "Pré-visualiza widget sem persistir (WKS2-19)")
    public ResponseEntity<WorkspaceWidgetDataDTO> preview(
            Authentication authentication,
            @Valid @RequestBody CreateWidgetDefinitionRequest request) {
        return ResponseEntity.ok(widgetQueryService.preview(authentication.getName(), request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtém definição de widget por id")
    public ResponseEntity<WidgetDefinitionDTO> obter(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(widgetDefinitionService.obter(authentication.getName(), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza definição de widget")
    public ResponseEntity<WidgetDefinitionDTO> atualizar(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateWidgetDefinitionRequest request) {
        return ResponseEntity.ok(widgetDefinitionService.atualizar(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui definição de widget")
    public ResponseEntity<Void> excluir(Authentication authentication, @PathVariable Long id) {
        widgetDefinitionService.excluir(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
