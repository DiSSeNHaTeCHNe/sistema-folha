package br.com.techne.sistemafolha.dashboard.api;

import br.com.techne.sistemafolha.dashboard.application.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "APIs para estatísticas do dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "Retorna estatísticas para o dashboard")
    public ResponseEntity<DashboardStatsDTO> getStats(Authentication authentication) {
        return ResponseEntity.ok(dashboardService.getStats(authentication.getName()));
    }
}
