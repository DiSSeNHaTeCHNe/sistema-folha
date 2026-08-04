package br.com.techne.sistemafolha.dashboard.application;

import br.com.techne.sistemafolha.dashboard.api.WidgetCatalogItemDTO;
import br.com.techne.sistemafolha.dashboard.domain.WidgetCatalog;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardWidgetCatalogServiceTest {

    private static final String LOGIN = "gestor";

    @Mock
    private DashboardAccessGuard dashboardAccessGuard;

    @InjectMocks
    private DashboardWidgetCatalogService catalogService;

    @Test
    void listarParaUsuario_acessoTotal_retorna12Widgets() {
        when(dashboardAccessGuard.resolve(LOGIN)).thenReturn(
            new DashboardAccessGuard.ResolvedDashboardAccess(
                false, 1L, contextoAcessoTotal(), null));

        List<WidgetCatalogItemDTO> catalogo = catalogService.listarParaUsuario(LOGIN);

        assertEquals(12, catalogo.size());
        assertTrue(catalogo.stream().anyMatch(w -> "grafico-funcionarios-por-cargo".equals(w.widgetId())));
        assertTrue(catalogo.stream().anyMatch(w -> "kpi-total-funcionarios".equals(w.widgetId())));
    }

    @Test
    void listarParaUsuario_escopoRestrito_retorna12Widgets() {
        when(dashboardAccessGuard.resolve(LOGIN)).thenReturn(
            new DashboardAccessGuard.ResolvedDashboardAccess(
                false, 1L, contextoRestrito(Set.of(10L)), Set.of(10L)));

        List<WidgetCatalogItemDTO> catalogo = catalogService.listarParaUsuario(LOGIN);

        assertEquals(12, catalogo.size());
    }

    @Test
    void listarParaUsuario_acessoNegado_retornaVazio() {
        when(dashboardAccessGuard.resolve(LOGIN))
            .thenReturn(DashboardAccessGuard.ResolvedDashboardAccess.accessDenied());

        assertTrue(catalogService.listarParaUsuario(LOGIN).isEmpty());
    }

    @Test
    void isWidgetPermitido_widgetValidoComEscopo_retornaTrue() {
        when(dashboardAccessGuard.resolve(LOGIN)).thenReturn(
            new DashboardAccessGuard.ResolvedDashboardAccess(
                false, 1L, contextoRestrito(Set.of(10L)), Set.of(10L)));

        assertTrue(catalogService.isWidgetPermitido(LOGIN, "kpi-total-funcionarios"));
        assertTrue(catalogService.isWidgetPermitido(LOGIN, "grafico-funcionarios-por-cargo"));
    }

    @Test
    void isWidgetPermitido_widgetInvalido_retornaFalse() {
        assertFalse(catalogService.isWidgetPermitido(LOGIN, "widget-inexistente"));
    }

    @Test
    void isWidgetPermitido_acessoNegado_retornaFalse() {
        when(dashboardAccessGuard.resolve(LOGIN))
            .thenReturn(DashboardAccessGuard.ResolvedDashboardAccess.accessDenied());

        assertFalse(catalogService.isWidgetPermitido(LOGIN, "kpi-total-funcionarios"));
    }

    @Test
    void widgetIdsValidos_retornaIdsDoCatalogoFiltrado() {
        when(dashboardAccessGuard.resolve(LOGIN)).thenReturn(
            new DashboardAccessGuard.ResolvedDashboardAccess(
                false, 1L, contextoAcessoTotal(), null));

        Set<String> ids = catalogService.widgetIdsValidos(LOGIN);

        assertEquals(WidgetCatalog.allWidgetIds(), ids);
    }

    @Test
    void listarParaUsuario_contemMetadadosDoCatalogo() {
        when(dashboardAccessGuard.resolve(LOGIN)).thenReturn(
            new DashboardAccessGuard.ResolvedDashboardAccess(
                false, 1L, contextoAcessoTotal(), null));

        WidgetCatalogItemDTO item = catalogService.listarParaUsuario(LOGIN).stream()
            .filter(w -> "grafico-evolucao-mensal".equals(w.widgetId()))
            .findFirst()
            .orElseThrow();

        assertEquals("Evolução da Folha", item.titulo());
        assertEquals("GRAFICO", item.categoria());
        assertEquals(12, item.colSpanPadrao());
        assertEquals(2, item.rowSpanPadrao());
    }

    private AccessContextDTO contextoAcessoTotal() {
        return new AccessContextDTO(true, true, true, Set.of(), null, 1L, "Raiz", 0);
    }

    private AccessContextDTO contextoRestrito(Set<Long> centros) {
        return new AccessContextDTO(true, true, false, centros, null, 2L, "TI", 1);
    }
}
