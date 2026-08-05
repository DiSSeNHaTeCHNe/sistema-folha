package br.com.techne.sistemafolha.dashboard.api;

import br.com.techne.sistemafolha.dashboard.application.DashboardWidgetQueryService;
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

import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard Widget Data", description = "APIs para dados parametrizados por widget")
public class DashboardWidgetController {

    private final DashboardWidgetQueryService dashboardWidgetQueryService;

    @GetMapping("/widgets/{widgetId}/data")
    @Operation(summary = "Retorna dados de um widget específico")
    public ResponseEntity<WidgetDataDTO> consultarDados(
            Authentication authentication,
            @PathVariable String widgetId,
            @RequestParam Map<String, String> params) {
        WidgetQueryParams queryParams = WidgetQueryParams.fromQueryMap(params);
        return ResponseEntity.ok(
            dashboardWidgetQueryService.consultar(authentication.getName(), widgetId, queryParams));
    }
}
