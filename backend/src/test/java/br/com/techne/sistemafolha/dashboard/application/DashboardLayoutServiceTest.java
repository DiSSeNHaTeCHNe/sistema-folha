package br.com.techne.sistemafolha.dashboard.application;

import br.com.techne.sistemafolha.dashboard.api.DashboardLayoutDTO;
import br.com.techne.sistemafolha.dashboard.api.WidgetInstanceDTO;
import br.com.techne.sistemafolha.dashboard.domain.DashboardAcessoNegadoException;
import br.com.techne.sistemafolha.dashboard.domain.DashboardLayout;
import br.com.techne.sistemafolha.dashboard.domain.WidgetCatalog;
import br.com.techne.sistemafolha.dashboard.domain.WidgetInstancePayload;
import br.com.techne.sistemafolha.dashboard.infrastructure.DashboardLayoutRepository;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardLayoutServiceTest {

    private static final String LOGIN = "gestor";
    private static final Long USUARIO_ID = 1L;
    private static final Long USUARIO_B_ID = 2L;

    @Mock
    private DashboardLayoutRepository dashboardLayoutRepository;

    @Mock
    private DashboardAccessGuard dashboardAccessGuard;

    @Mock
    private DashboardWidgetCatalogService dashboardWidgetCatalogService;

    @Mock
    private DashboardWidgetConfigValidator dashboardWidgetConfigValidator;

    @org.mockito.Spy
    private LayoutValidator layoutValidator = new LayoutValidator();

    @InjectMocks
    private DashboardLayoutService dashboardLayoutService;

    @Test
    void obterOuCriarPadrao_primeiroAcesso_persiste11WidgetsNaOrdem() {
        stubAccess(LOGIN, USUARIO_ID);
        when(dashboardLayoutRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());
        when(dashboardLayoutRepository.save(any(DashboardLayout.class))).thenAnswer(inv -> {
            DashboardLayout l = inv.getArgument(0);
            l.setId(10L);
            return l;
        });

        DashboardLayoutDTO dto = dashboardLayoutService.obterOuCriarPadrao(LOGIN);

        assertEquals(11, dto.widgets().size());
        assertEquals("kpi-total-funcionarios", dto.widgets().get(0).widgetId());
        assertEquals("lista-top-descontos", dto.widgets().get(10).widgetId());
        assertFalse(dto.widgets().stream().anyMatch(w -> "grafico-funcionarios-por-cargo".equals(w.widgetId())));

        ArgumentCaptor<DashboardLayout> captor = ArgumentCaptor.forClass(DashboardLayout.class);
        verify(dashboardLayoutRepository).save(captor.capture());
        assertEquals(11, captor.getValue().getWidgets().size());
        assertEquals(USUARIO_ID, captor.getValue().getUsuarioId());
    }

    @Test
    void obterOuCriarPadrao_layoutExistente_naoRecria() {
        stubAccess(LOGIN, USUARIO_ID);
        DashboardLayout existente = layoutSalvo(USUARIO_ID, List.of(
            payload("kpi-total-funcionarios", "abc12345", 0, 3, 1)));
        when(dashboardLayoutRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(existente));

        DashboardLayoutDTO dto = dashboardLayoutService.obterOuCriarPadrao(LOGIN);

        assertEquals(1, dto.widgets().size());
        verify(dashboardLayoutRepository, never()).save(any());
    }

    @Test
    void obterOuCriarPadrao_semEscopo_lanca403() {
        doThrow(new DashboardAcessoNegadoException()).when(dashboardAccessGuard).assertEscopo(LOGIN);

        assertThrows(DashboardAcessoNegadoException.class,
            () -> dashboardLayoutService.obterOuCriarPadrao(LOGIN));
    }

    @Test
    void salvar_configTopNInvalido_lanca400() {
        stubAccess(LOGIN, USUARIO_ID);
        when(dashboardWidgetCatalogService.widgetIdsValidos(LOGIN)).thenReturn(Set.of("grafico-custo-por-cc"));
        org.mockito.Mockito.doThrow(new IllegalArgumentException("topN deve estar entre 1 e 50"))
            .when(dashboardWidgetConfigValidator)
            .validar(eq("grafico-custo-por-cc"), any(), any());

        DashboardLayoutDTO input = new DashboardLayoutDTO(null, "Meu dashboard", List.of(
            new WidgetInstanceDTO("grafico-custo-por-cc", "inst1", 0, 3, 2, Map.of("topN", 99))));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> dashboardLayoutService.salvar(LOGIN, input));
        assertTrue(ex.getMessage().contains("topN"));
        verify(dashboardLayoutRepository, never()).save(any());
    }

    @Test
    void salvar_widgetIdInvalido_lanca400() {
        stubAccess(LOGIN, USUARIO_ID);
        when(dashboardWidgetCatalogService.widgetIdsValidos(LOGIN)).thenReturn(Set.of("kpi-total-funcionarios"));

        DashboardLayoutDTO input = new DashboardLayoutDTO(null, "Meu dashboard", List.of(
            widget("widget-inexistente", "inst1", 0, 3, 1)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> dashboardLayoutService.salvar(LOGIN, input));
        assertTrue(ex.getMessage().contains("widgetId inválido"));
        verify(dashboardLayoutRepository, never()).save(any());
    }

    @Test
    void salvar_maisDe30Widgets_lanca400() {
        stubAccess(LOGIN, USUARIO_ID);
        Set<String> todos = WidgetCatalog.allWidgetIds();
        when(dashboardWidgetCatalogService.widgetIdsValidos(LOGIN)).thenReturn(todos);
        List<WidgetInstanceDTO> widgets = IntStream.range(0, 31)
            .mapToObj(i -> widget("kpi-total-funcionarios", "inst" + i, i, 3, 1))
            .toList();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> dashboardLayoutService.salvar(LOGIN, new DashboardLayoutDTO(null, "x", widgets)));
        assertTrue(ex.getMessage().contains("30"));
    }

    @Test
    void salvar_colSpanMaiorQue12_lanca400() {
        stubAccess(LOGIN, USUARIO_ID);
        when(dashboardWidgetCatalogService.widgetIdsValidos(LOGIN)).thenReturn(Set.of("kpi-total-funcionarios"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> dashboardLayoutService.salvar(LOGIN, new DashboardLayoutDTO(null, "x", List.of(
                widget("kpi-total-funcionarios", "inst1", 0, 13, 1)))));
        assertTrue(ex.getMessage().contains("colSpan"));
    }

    @Test
    void salvar_normalizaOrdem() {
        stubAccess(LOGIN, USUARIO_ID);
        when(dashboardWidgetCatalogService.widgetIdsValidos(LOGIN)).thenReturn(Set.of(
            "kpi-total-funcionarios", "kpi-custo-empresa"));
        when(dashboardLayoutRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());
        when(dashboardLayoutRepository.save(any(DashboardLayout.class))).thenAnswer(inv -> inv.getArgument(0));

        DashboardLayoutDTO input = new DashboardLayoutDTO(null, "Meu dashboard", List.of(
            widget("kpi-custo-empresa", "b", 5, 3, 1),
            widget("kpi-total-funcionarios", "a", 2, 3, 1)));

        DashboardLayoutDTO salvo = dashboardLayoutService.salvar(LOGIN, input);

        assertEquals("kpi-total-funcionarios", salvo.widgets().get(0).widgetId());
        assertEquals("kpi-custo-empresa", salvo.widgets().get(1).widgetId());
        assertEquals(0, salvo.widgets().get(0).ordem());
        assertEquals(1, salvo.widgets().get(1).ordem());
    }

    @Test
    void salvar_preservaJsonbRoundTrip() {
        stubAccess(LOGIN, USUARIO_ID);
        when(dashboardWidgetCatalogService.widgetIdsValidos(LOGIN)).thenReturn(Set.of("kpi-total-funcionarios"));
        when(dashboardLayoutRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());
        when(dashboardLayoutRepository.save(any(DashboardLayout.class))).thenAnswer(inv -> {
            DashboardLayout l = inv.getArgument(0);
            l.setId(99L);
            return l;
        });

        Map<String, Object> config = Map.of("topN", 10);
        DashboardLayoutDTO input = new DashboardLayoutDTO(null, "Custom", List.of(
            new WidgetInstanceDTO("kpi-total-funcionarios", "abc12345", 0, 6, 2, config)));

        DashboardLayoutDTO salvo = dashboardLayoutService.salvar(LOGIN, input);

        assertEquals(1, salvo.widgets().size());
        assertEquals("abc12345", salvo.widgets().get(0).instanceId());
        assertEquals(6, salvo.widgets().get(0).colSpan());
        assertEquals(config, salvo.widgets().get(0).config());
    }

    @Test
    void leitura_widgetIdDesconhecido_ignorado() {
        stubAccess(LOGIN, USUARIO_ID);
        DashboardLayout layout = layoutSalvo(USUARIO_ID, List.of(
            payload("kpi-total-funcionarios", "a", 0, 3, 1),
            payload("widget-removido-do-catalogo", "b", 1, 3, 1)));
        when(dashboardLayoutRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.of(layout));

        DashboardLayoutDTO dto = dashboardLayoutService.obterOuCriarPadrao(LOGIN);

        assertEquals(1, dto.widgets().size());
        assertEquals("kpi-total-funcionarios", dto.widgets().get(0).widgetId());
    }

    @Test
    void restaurarPadrao_removeLayoutDoUsuario() {
        stubAccess(LOGIN, USUARIO_ID);

        dashboardLayoutService.restaurarPadrao(LOGIN);

        verify(dashboardLayoutRepository).deleteByUsuarioId(USUARIO_ID);
    }

    @Test
    void criarWidgetsPadrao_contem11WidgetsOrdemDesign() {
        List<WidgetInstancePayload> padrao = dashboardLayoutService.criarWidgetsPadrao();

        assertEquals(11, padrao.size());
        List<String> ids = padrao.stream().map(WidgetInstancePayload::widgetId).toList();
        assertEquals(List.of(
            "kpi-total-funcionarios", "kpi-custo-empresa", "kpi-beneficios-ativos", "kpi-relacao-pd",
            "grafico-evolucao-mensal", "grafico-funcionarios-por-cc", "grafico-funcionarios-por-linha",
            "grafico-custo-por-cc", "grafico-custo-por-linha", "lista-top-proventos", "lista-top-descontos"
        ), ids);
        assertEquals(IntStream.range(0, 11).boxed().collect(Collectors.toList()),
            padrao.stream().map(WidgetInstancePayload::ordem).toList());
    }

    @Test
    void salvar_usuarioIdDerivadoDaAutenticacao() {
        stubAccess(LOGIN, USUARIO_ID);
        when(dashboardWidgetCatalogService.widgetIdsValidos(LOGIN)).thenReturn(Set.of("kpi-total-funcionarios"));
        when(dashboardLayoutRepository.findByUsuarioId(USUARIO_ID)).thenReturn(Optional.empty());
        when(dashboardLayoutRepository.save(any(DashboardLayout.class))).thenAnswer(inv -> inv.getArgument(0));

        dashboardLayoutService.salvar(LOGIN, new DashboardLayoutDTO(null, "x", List.of(
            widget("kpi-total-funcionarios", "inst1", 0, 3, 1))));

        ArgumentCaptor<DashboardLayout> captor = ArgumentCaptor.forClass(DashboardLayout.class);
        verify(dashboardLayoutRepository).save(captor.capture());
        assertEquals(USUARIO_ID, captor.getValue().getUsuarioId());
        verify(dashboardLayoutRepository, never()).findByUsuarioId(USUARIO_B_ID);
    }

    private void stubAccess(String login, Long usuarioId) {
        when(dashboardAccessGuard.resolve(login)).thenReturn(
            new DashboardAccessGuard.ResolvedDashboardAccess(
                false, usuarioId, contextoAcessoTotal(), null));
    }

    private AccessContextDTO contextoAcessoTotal() {
        return new AccessContextDTO(true, true, true, Set.of(), null, 1L, "Raiz", 0);
    }

    private WidgetInstanceDTO widget(String widgetId, String instanceId, int ordem, int colSpan, int rowSpan) {
        return new WidgetInstanceDTO(widgetId, instanceId, ordem, colSpan, rowSpan, null);
    }

    private WidgetInstancePayload payload(String widgetId, String instanceId, int ordem, int colSpan, int rowSpan) {
        return new WidgetInstancePayload(widgetId, instanceId, ordem, colSpan, rowSpan, null);
    }

    private DashboardLayout layoutSalvo(Long usuarioId, List<WidgetInstancePayload> widgets) {
        DashboardLayout layout = new DashboardLayout();
        layout.setId(1L);
        layout.setUsuarioId(usuarioId);
        layout.setWidgets(new ArrayList<>(widgets));
        return layout;
    }
}
