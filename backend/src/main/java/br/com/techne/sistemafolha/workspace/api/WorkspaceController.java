package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.workspace.application.WorkspaceService;
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
@RequestMapping("/workspace/workspaces")
@RequiredArgsConstructor
@Tag(name = "Workspace", description = "CRUD de workspaces do usuário")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @GetMapping
    @Operation(summary = "Lista workspaces do usuário")
    public ResponseEntity<List<WorkspaceSummaryDTO>> listar(Authentication authentication) {
        return ResponseEntity.ok(workspaceService.listar(authentication.getName()));
    }

    @PostMapping
    @Operation(summary = "Cria workspace")
    public ResponseEntity<WorkspaceDTO> criar(
            Authentication authentication,
            @Valid @RequestBody CreateWorkspaceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(workspaceService.criar(authentication.getName(), request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtém workspace com layout")
    public ResponseEntity<WorkspaceDTO> obter(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(workspaceService.obter(authentication.getName(), id));
    }

    @PutMapping("/{id}/layout")
    @Operation(summary = "Salva layout do workspace")
    public ResponseEntity<WorkspaceDTO> salvarLayout(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody SaveWorkspaceLayoutRequest request) {
        return ResponseEntity.ok(workspaceService.salvarLayout(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui workspace (datasets preservados)")
    public ResponseEntity<Void> excluir(Authentication authentication, @PathVariable Long id) {
        workspaceService.excluir(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
