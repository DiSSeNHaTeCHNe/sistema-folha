package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.workspace.application.WorkspaceProposalService;
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

@RestController
@RequestMapping("/workspace/proposals")
@RequiredArgsConstructor
@Tag(name = "Workspace Proposals", description = "Propostas IA propor-e-confirmar (WKS-24…26)")
public class ProposalController {

    private final WorkspaceProposalService proposalService;

    @PostMapping
    @Operation(summary = "Cria proposta IA pendente", operationId = "criarPropostaWorkspace")
    public ResponseEntity<ProposalDTO> criar(
            Authentication authentication,
            @Valid @RequestBody CreateProposalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(proposalService.criarProposta(authentication.getName(), request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtém proposta para revisão", operationId = "obterPropostaWorkspace")
    public ResponseEntity<ProposalDTO> obter(
            Authentication authentication,
            @PathVariable Long id) {
        return ResponseEntity.ok(proposalService.obter(authentication.getName(), id));
    }

    @PostMapping("/{id}/confirmar")
    @Operation(summary = "Confirma e persiste proposta", operationId = "confirmarPropostaWorkspace")
    public ResponseEntity<ProposalDTO> confirmar(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ConfirmProposalRequest ajustes) {
        return ResponseEntity.ok(proposalService.confirmar(authentication.getName(), id, ajustes));
    }

    @PostMapping("/{id}/descartar")
    @Operation(summary = "Descarta proposta pendente", operationId = "descartarPropostaWorkspace")
    public ResponseEntity<Void> descartar(
            Authentication authentication,
            @PathVariable Long id) {
        proposalService.descartar(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
