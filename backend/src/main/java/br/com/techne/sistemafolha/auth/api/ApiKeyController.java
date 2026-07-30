package br.com.techne.sistemafolha.auth.api;

import br.com.techne.sistemafolha.auth.application.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth/api-keys")
@RequiredArgsConstructor
@Tag(name = "API Keys", description = "Gerenciamento de API Keys somente leitura")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    @PostMapping
    @Operation(summary = "Cria uma API Key", description = "Retorna o secret completo apenas nesta resposta")
    public ResponseEntity<ApiKeyCreatedDTO> criar(
            @Valid @RequestBody ApiKeyCreateRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                apiKeyService.criar(
                        apiKeyService.resolverUsuarioPorLogin(authentication.getName()),
                        request));
    }

    @GetMapping
    @Operation(summary = "Lista API Keys", description = "ADMIN pode filtrar por usuarioId")
    public ResponseEntity<List<ApiKeyListDTO>> listar(
            @RequestParam(required = false) Long usuarioId,
            Authentication authentication) {
        return ResponseEntity.ok(apiKeyService.listar(
                apiKeyService.resolverUsuarioPorLogin(authentication.getName()),
                usuarioId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Revoga uma API Key")
    public ResponseEntity<Void> revogar(
            @Parameter(description = "ID da API Key") @PathVariable Long id,
            Authentication authentication) {
        apiKeyService.revogar(
                apiKeyService.resolverUsuarioPorLogin(authentication.getName()),
                id);
        return ResponseEntity.noContent().build();
    }
}
