package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.workspace.application.DatasetAuditService;
import br.com.techne.sistemafolha.workspace.application.DatasetRowService;
import br.com.techne.sistemafolha.workspace.application.DatasetService;
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
@RequestMapping("/workspace/datasets")
@RequiredArgsConstructor
@Tag(name = "Workspace Datasets", description = "CRUD de datasets e linhas do workspace")
public class DatasetController {

    private final DatasetService datasetService;
    private final DatasetRowService datasetRowService;
    private final DatasetAuditService datasetAuditService;

    @GetMapping
    @Operation(summary = "Lista datasets do usuário autenticado")
    public ResponseEntity<List<DatasetSummaryDTO>> listar(Authentication authentication) {
        return ResponseEntity.ok(datasetService.listar(authentication.getName()));
    }

    @PostMapping
    @Operation(summary = "Cria dataset com esquema tipado")
    public ResponseEntity<DatasetDTO> criar(
            Authentication authentication,
            @Valid @RequestBody CreateDatasetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(datasetService.criar(authentication.getName(), request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtém dataset por id")
    public ResponseEntity<DatasetDTO> obter(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(datasetService.obter(authentication.getName(), id));
    }

    @PutMapping("/{id}/schema")
    @Operation(summary = "Atualiza esquema do dataset")
    public ResponseEntity<DatasetDTO> atualizarSchema(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateDatasetSchemaRequest request) {
        return ResponseEntity.ok(datasetService.atualizarSchema(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui dataset")
    public ResponseEntity<Void> excluir(Authentication authentication, @PathVariable Long id) {
        datasetService.excluir(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/rows")
    @Operation(summary = "Lista linhas do dataset")
    public ResponseEntity<List<DatasetRowDTO>> listarLinhas(
            Authentication authentication,
            @PathVariable Long id) {
        return ResponseEntity.ok(datasetRowService.listarLinhas(authentication.getName(), id));
    }

    @PostMapping("/{id}/rows")
    @Operation(summary = "Adiciona linha ao dataset")
    public ResponseEntity<DatasetRowDTO> adicionarLinha(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody DatasetRowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(datasetRowService.adicionarLinha(authentication.getName(), id, request));
    }

    @GetMapping("/{id}/rows/{rowId}")
    @Operation(summary = "Obtém linha do dataset")
    public ResponseEntity<DatasetRowDTO> obterLinha(
            Authentication authentication,
            @PathVariable Long id,
            @PathVariable Long rowId) {
        return ResponseEntity.ok(datasetRowService.obterLinha(authentication.getName(), id, rowId));
    }

    @PutMapping("/{id}/rows/{rowId}")
    @Operation(summary = "Atualiza linha do dataset")
    public ResponseEntity<DatasetRowDTO> atualizarLinha(
            Authentication authentication,
            @PathVariable Long id,
            @PathVariable Long rowId,
            @Valid @RequestBody DatasetRowRequest request) {
        return ResponseEntity.ok(datasetRowService.atualizarLinha(authentication.getName(), id, rowId, request));
    }

    @DeleteMapping("/{id}/rows/{rowId}")
    @Operation(summary = "Remove linha do dataset")
    public ResponseEntity<Void> removerLinha(
            Authentication authentication,
            @PathVariable Long id,
            @PathVariable Long rowId) {
        datasetRowService.removerLinha(authentication.getName(), id, rowId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/audit")
    @Operation(summary = "Timeline agregada de auditoria do dataset (WKS2-28)")
    public ResponseEntity<List<DatasetAuditTimelineEntryDTO>> listarAuditoriaDataset(
            Authentication authentication,
            @PathVariable Long id) {
        return ResponseEntity.ok(datasetAuditService.listarHistoricoDataset(authentication.getName(), id));
    }

    @GetMapping("/{id}/rows/{rowId}/audit")
    @Operation(summary = "Histórico de auditoria da linha (WKS-23)")
    public ResponseEntity<List<DatasetRowAuditEntryDTO>> listarAuditoriaLinha(
            Authentication authentication,
            @PathVariable Long id,
            @PathVariable Long rowId) {
        datasetRowService.obterLinha(authentication.getName(), id, rowId);
        return ResponseEntity.ok(datasetAuditService.listarHistorico(rowId));
    }
}
