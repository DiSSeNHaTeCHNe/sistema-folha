package br.com.techne.sistemafolha.dashboard.application;

import br.com.techne.sistemafolha.dashboard.api.WidgetCatalogItemDTO;
import br.com.techne.sistemafolha.dashboard.domain.WidgetCatalog;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardWidgetCatalogService {

    private final DashboardAccessGuard dashboardAccessGuard;

    public List<WidgetCatalogItemDTO> listarParaUsuario(String login) {
        dashboardAccessGuard.assertEscopo(login);
        DashboardAccessGuard.ResolvedDashboardAccess access = dashboardAccessGuard.resolve(login);
        return Arrays.stream(WidgetCatalog.values())
            .filter(entry -> isPermitidoParaContexto(entry, access.contexto()))
            .map(this::toDto)
            .toList();
    }

    public boolean isWidgetPermitido(String login, String widgetId) {
        return WidgetCatalog.findByWidgetId(widgetId)
            .map(entry -> {
                DashboardAccessGuard.ResolvedDashboardAccess access = dashboardAccessGuard.resolve(login);
                return !access.denied() && isPermitidoParaContexto(entry, access.contexto());
            })
            .orElse(false);
    }

    public Set<String> widgetIdsValidos(String login) {
        return listarParaUsuario(login).stream()
            .map(WidgetCatalogItemDTO::widgetId)
            .collect(Collectors.toSet());
    }

    private boolean isPermitidoParaContexto(WidgetCatalog entry, AccessContextDTO contexto) {
        if (contexto.acessoTotal()) {
            return true;
        }
        return contexto.centrosCustoIds() != null && !contexto.centrosCustoIds().isEmpty();
    }

    private WidgetCatalogItemDTO toDto(WidgetCatalog entry) {
        return new WidgetCatalogItemDTO(
            entry.widgetId(),
            entry.titulo(),
            entry.descricao(),
            entry.categoria(),
            entry.colSpanPadrao(),
            entry.rowSpanPadrao()
        );
    }
}
