package br.com.techne.sistemafolha.dashboard.api;

import br.com.techne.sistemafolha.dashboard.application.DashboardLayoutService;
import br.com.techne.sistemafolha.dashboard.application.DashboardWidgetCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard Layout", description = "APIs para layout customizável do dashboard")
public class DashboardLayoutController {

    private final DashboardLayoutService dashboardLayoutService;
    private final DashboardWidgetCatalogService dashboardWidgetCatalogService;

    @GetMapping("/layout")
    @Operation(summary = "Obtém ou cria o layout do usuário autenticado")
    public ResponseEntity<DashboardLayoutDTO> obterLayout(Authentication authentication) {
        return ResponseEntity.ok(dashboardLayoutService.obterOuCriarPadrao(authentication.getName()));
    }

    @PutMapping("/layout")
    @Operation(summary = "Salva o layout completo do usuário autenticado")
    public ResponseEntity<DashboardLayoutDTO> salvarLayout(
            Authentication authentication,
            @Valid @RequestBody DashboardLayoutDTO dto) {
        return ResponseEntity.ok(dashboardLayoutService.salvar(authentication.getName(), dto));
    }

    @DeleteMapping("/layout")
    @Operation(summary = "Restaura o layout padrão do usuário autenticado")
    public ResponseEntity<Void> restaurarLayout(Authentication authentication) {
        dashboardLayoutService.restaurarPadrao(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/widgets/catalog")
    @Operation(summary = "Lista widgets disponíveis para o usuário autenticado")
    public ResponseEntity<List<WidgetCatalogItemDTO>> listarCatalogo(Authentication authentication) {
        return ResponseEntity.ok(dashboardWidgetCatalogService.listarParaUsuario(authentication.getName()));
    }
}
