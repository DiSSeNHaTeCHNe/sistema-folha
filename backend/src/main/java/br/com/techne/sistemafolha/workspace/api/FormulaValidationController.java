package br.com.techne.sistemafolha.workspace.api;

import br.com.techne.sistemafolha.workspace.application.FormulaValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workspace/formulas")
@RequiredArgsConstructor
@Tag(name = "Workspace Formulas", description = "Validação de fórmulas de widget")
public class FormulaValidationController {

    private final FormulaValidationService formulaValidationService;

    @PostMapping("/validate")
    @Operation(summary = "Valida fórmula contra whitelist de campos (WKS2-18)")
    public ResponseEntity<FormulaValidationResponseDTO> validar(
            Authentication authentication,
            @Valid @RequestBody FormulaValidationRequest request) {
        FormulaValidationResponseDTO result = formulaValidationService.validar(
            authentication.getName(), request);
        if (!result.valid()) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }
}
