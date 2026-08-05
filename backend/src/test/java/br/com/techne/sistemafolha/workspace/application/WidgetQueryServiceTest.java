package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.folha.port.FolhaConsultaPort;
import br.com.techne.sistemafolha.folha.port.FolhaResumoSnapshot;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.workspace.api.CreateWidgetDefinitionRequest;
import br.com.techne.sistemafolha.workspace.api.WorkspaceWidgetDataDTO;
import br.com.techne.sistemafolha.workspace.api.WorkspaceWidgetQueryParams;
import br.com.techne.sistemafolha.workspace.domain.DatasetFieldSchema;
import br.com.techne.sistemafolha.workspace.domain.DatasetFieldType;
import br.com.techne.sistemafolha.workspace.domain.WidgetSourceKind;
import br.com.techne.sistemafolha.workspace.domain.WidgetSourceRef;
import br.com.techne.sistemafolha.workspace.domain.Workspace;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDataset;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceWidgetDefinition;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceWidgetPayload;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceDatasetRowRepository;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceWidgetDefinitionRepository;
import br.com.techne.sistemafolha.workspace.port.OrcamentoConsultaPort;
import br.com.techne.sistemafolha.workspace.port.OrcamentoConsultaPort.OrcamentoCentroCustoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WidgetQueryServiceTest {

    private static final String LOGIN = "gestor";
    private static final Long USUARIO_ID = 10L;
    private static final Long WORKSPACE_ID = 1L;
    private static final Long WIDGET_DEF_ID = 20L;

    @Mock
    private WorkspaceAccessGuard workspaceAccessGuard;
    @Mock
    private WorkspaceService workspaceService;
    @Mock
    private WorkspaceWidgetDefinitionRepository widgetDefinitionRepository;
    @Mock
    private WorkspaceDatasetRowRepository rowRepository;
    @Mock
    private DatasetService datasetService;
    @Mock
    private WidgetDefinitionService widgetDefinitionService;
    @Mock
    private OrcamentoConsultaPort orcamentoConsultaPort;
    @Mock
    private FolhaConsultaPort folhaConsultaPort;

    private FormulaEngine formulaEngine;
    private WidgetQueryService service;

    @BeforeEach
    void setUp() {
        formulaEngine = new FormulaEngine();
        service = new WidgetQueryService(
            workspaceAccessGuard,
            workspaceService,
            widgetDefinitionRepository,
            rowRepository,
            datasetService,
            widgetDefinitionService,
            formulaEngine,
            orcamentoConsultaPort,
            folhaConsultaPort);
    }

    @Test
    void obterDados_widgetInvalido_retornaInvalido() {
        stubAcesso(acessoTotal());
        stubWorkspace(widgetPayload("inst1", WIDGET_DEF_ID));
        WorkspaceWidgetDefinition def = widgetDef("KPI", false);
        def.setInvalido(true);
        when(widgetDefinitionRepository.findByUsuarioIdAndId(USUARIO_ID, WIDGET_DEF_ID)).thenReturn(Optional.of(def));

        WorkspaceWidgetDataDTO result = service.obterDados(
            LOGIN, WORKSPACE_ID, "inst1", new WorkspaceWidgetQueryParams(null));

        assertTrue(result.invalido());
        assertTrue(result.semDados());
    }

    @Test
    void obterDados_semEscopoSistema_retornaSemDados() {
        AccessContextDTO ctx = new AccessContextDTO(
            true, true, false, Set.of(), null, 1L, "Nó", 2);
        stubAcesso(ctx);
        stubWorkspace(widgetPayload("inst1", WIDGET_DEF_ID));
        when(widgetDefinitionRepository.findByUsuarioIdAndId(USUARIO_ID, WIDGET_DEF_ID))
            .thenReturn(Optional.of(widgetDef("KPI", false)));

        WorkspaceWidgetDataDTO result = service.obterDados(
            LOGIN, WORKSPACE_ID, "inst1", new WorkspaceWidgetQueryParams("2025-06"));

        assertTrue(result.semDados());
        assertFalse(result.invalido());
    }

    @Test
    void obterDados_kpiComFormula_formataMoedaPtBr() {
        stubAcesso(acessoTotal());
        stubWorkspace(widgetPayload("inst1", WIDGET_DEF_ID));
        WorkspaceWidgetDefinition def = widgetDef("KPI", true);
        def.setFormula("SOMA(valor_orcado)*2");
        when(widgetDefinitionRepository.findByUsuarioIdAndId(USUARIO_ID, WIDGET_DEF_ID)).thenReturn(Optional.of(def));
        stubDatasetSeries();

        WorkspaceWidgetDataDTO result = service.obterDados(
            LOGIN, WORKSPACE_ID, "inst1", new WorkspaceWidgetQueryParams("2025-06"));

        assertFalse(result.semDados());
        assertTrue(result.valores().get("valor").startsWith("R$"));
        assertTrue(result.valores().get("valor").contains(","));
    }

    @Test
    void obterDados_tabelaOrcamento_montaLinhasComRealizado() {
        stubAcesso(acessoTotal());
        stubWorkspace(widgetPayload("tbl", WIDGET_DEF_ID));
        WorkspaceWidgetDefinition def = widgetDef("TABELA", true);
        def.setFontes(List.of(
            new WidgetSourceRef(WidgetSourceKind.DATASET, "5"),
            new WidgetSourceRef(WidgetSourceKind.SISTEMA, "ORCAMENTO")));
        when(widgetDefinitionRepository.findByUsuarioIdAndId(USUARIO_ID, WIDGET_DEF_ID)).thenReturn(Optional.of(def));
        stubOrcamentoDataset();
        when(rowRepository.findByDatasetIdOrderByOrdemAscIdAsc(5L)).thenReturn(List.of());
        when(orcamentoConsultaPort.obterRealizadoPorCentroCusto(any(), any())).thenReturn(List.of(
            new OrcamentoCentroCustoDTO(10L, "CC A", new BigDecimal("5000"), 2)));

        WorkspaceWidgetDataDTO result = service.obterDados(
            LOGIN, WORKSPACE_ID, "tbl", new WorkspaceWidgetQueryParams("2025-06"));

        assertEquals(1, result.linhas().size());
        assertTrue(result.linhas().get(0).get("realizado").startsWith("R$"));
    }

    @Test
    void obterDados_widgetCatalogo_retornaSemDados() {
        stubAcesso(acessoTotal());
        Workspace ws = new Workspace();
        ws.setWidgets(new ArrayList<>(List.of(
            new WorkspaceWidgetPayload("cat1", 0, 3, 1, "kpi-total-funcionarios", null, Map.of()))));
        when(workspaceService.findOwnedWorkspace(USUARIO_ID, WORKSPACE_ID)).thenReturn(ws);
        stubCompetencia();

        WorkspaceWidgetDataDTO result = service.obterDados(
            LOGIN, WORKSPACE_ID, "cat1", new WorkspaceWidgetQueryParams(null));

        assertTrue(result.semDados());
        assertEquals("kpi-total-funcionarios", result.widgetId());
    }

    @Test
    void obterDados_instanceInexistente_lanca400() {
        stubAcesso(acessoTotal());
        Workspace ws = new Workspace();
        ws.setWidgets(new ArrayList<>());
        when(workspaceService.findOwnedWorkspace(USUARIO_ID, WORKSPACE_ID)).thenReturn(ws);

        assertThrows(IllegalArgumentException.class,
            () -> service.obterDados(LOGIN, WORKSPACE_ID, "missing", new WorkspaceWidgetQueryParams(null)));
    }

    @Test
    void formatMoney_usaLocalePtBr() {
        String formatted = WidgetQueryService.formatMoney(new BigDecimal("1234.56"));
        assertTrue(formatted.startsWith("R$"));
        assertTrue(formatted.contains("1.234,56"));
    }

    @Test
    void obterDados_competenciaInvalida_lanca400() {
        stubAcesso(acessoTotal());
        stubWorkspace(widgetPayload("inst1", WIDGET_DEF_ID));
        when(widgetDefinitionRepository.findByUsuarioIdAndId(USUARIO_ID, WIDGET_DEF_ID))
            .thenReturn(Optional.of(widgetDef("KPI", false)));

        assertThrows(IllegalArgumentException.class,
            () -> service.obterDados(LOGIN, WORKSPACE_ID, "inst1", new WorkspaceWidgetQueryParams("06-2025")));
    }

    @Test
    void obterDados_kpiSemFormula_retornaSemDados() {
        stubAcesso(acessoTotal());
        stubWorkspace(widgetPayload("inst1", WIDGET_DEF_ID));
        when(widgetDefinitionRepository.findByUsuarioIdAndId(USUARIO_ID, WIDGET_DEF_ID))
            .thenReturn(Optional.of(widgetDef("KPI", false)));

        WorkspaceWidgetDataDTO result = service.obterDados(
            LOGIN, WORKSPACE_ID, "inst1", new WorkspaceWidgetQueryParams("2025-06"));

        assertTrue(result.semDados());
    }

    @Test
    void obterDados_tabelaSemRealizado_retornaSemDados() {
        stubAcesso(acessoTotal());
        stubWorkspace(widgetPayload("tbl", WIDGET_DEF_ID));
        WorkspaceWidgetDefinition def = widgetDef("TABELA", true);
        when(widgetDefinitionRepository.findByUsuarioIdAndId(USUARIO_ID, WIDGET_DEF_ID)).thenReturn(Optional.of(def));
        stubOrcamentoDataset();
        when(rowRepository.findByDatasetIdOrderByOrdemAscIdAsc(5L)).thenReturn(List.of());
        when(orcamentoConsultaPort.obterRealizadoPorCentroCusto(any(), any())).thenReturn(List.of());

        WorkspaceWidgetDataDTO result = service.obterDados(
            LOGIN, WORKSPACE_ID, "tbl", new WorkspaceWidgetQueryParams("2025-06"));

        assertTrue(result.semDados());
    }

    @Test
    void obterDados_graficoLinha_montaLinhasComRealizado() {
        stubAcesso(acessoTotal());
        stubWorkspace(widgetPayload("chart", WIDGET_DEF_ID));
        WorkspaceWidgetDefinition def = widgetDef("GRAFICO_LINHA", true);
        def.setFontes(List.of(new WidgetSourceRef(WidgetSourceKind.SISTEMA, "ORCAMENTO")));
        when(widgetDefinitionRepository.findByUsuarioIdAndId(USUARIO_ID, WIDGET_DEF_ID)).thenReturn(Optional.of(def));
        when(orcamentoConsultaPort.obterRealizadoPorCentroCusto(any(), any())).thenReturn(List.of(
            new OrcamentoCentroCustoDTO(10L, "CC A", new BigDecimal("5000"), 2)));

        WorkspaceWidgetDataDTO result = service.obterDados(
            LOGIN, WORKSPACE_ID, "chart", new WorkspaceWidgetQueryParams("2025-06"));

        assertFalse(result.semDados());
        assertEquals("GRAFICO_LINHA", result.tipo());
        assertEquals(1, result.linhas().size());
        assertEquals("CC A", result.linhas().get(0).get("label"));
        assertTrue(result.linhas().get(0).get("valor").startsWith("R$"));
    }

    @Test
    void preview_graficoBarra_retornaLinhasFormatadasPtBr() {
        stubAcesso(acessoTotal());
        stubCompetencia();
        when(orcamentoConsultaPort.obterRealizadoPorCentroCusto(any(), any())).thenReturn(List.of(
            new OrcamentoCentroCustoDTO(10L, "CC B", new BigDecimal("1234.56"), 1)));

        CreateWidgetDefinitionRequest request = new CreateWidgetDefinitionRequest(
            "Gráfico Preview",
            "GRAFICO_BARRA",
            List.of(new WidgetSourceRef(WidgetSourceKind.SISTEMA, "ORCAMENTO")),
            null,
            Map.of());

        WorkspaceWidgetDataDTO result = service.preview(LOGIN, request);

        assertFalse(result.semDados());
        assertEquals("GRAFICO_BARRA", result.tipo());
        assertEquals("preview", result.instanceId());
        assertEquals("CC B", result.linhas().get(0).get("label"));
        assertTrue(result.linhas().get(0).get("valor").contains("1.234,56"));
    }

    private void stubOrcamentoDataset() {
        WorkspaceDataset dataset = new WorkspaceDataset();
        dataset.setSchema(List.of(
            new DatasetFieldSchema("centro_custo_id", DatasetFieldType.REFERENCIA, null, true),
            new DatasetFieldSchema("valor_orcado", DatasetFieldType.MOEDA, null, true)));
        when(datasetService.findOwnedDataset(eq(USUARIO_ID), eq(5L))).thenReturn(dataset);
    }

    private void stubAcesso(AccessContextDTO ctx) {
        when(workspaceAccessGuard.resolve(LOGIN))
            .thenReturn(new WorkspaceAccessGuard.ResolvedWorkspaceAccess(
                false, USUARIO_ID, ctx, ctx.acessoTotal() ? null : ctx.centrosCustoIds()));
    }

    private AccessContextDTO acessoTotal() {
        return new AccessContextDTO(true, true, true, Set.of(), null, 1L, "Raiz", 0);
    }

    private void stubWorkspace(WorkspaceWidgetPayload payload) {
        Workspace ws = new Workspace();
        ws.setWidgets(new ArrayList<>(List.of(payload)));
        when(workspaceService.findOwnedWorkspace(USUARIO_ID, WORKSPACE_ID)).thenReturn(ws);
    }

    private WorkspaceWidgetPayload widgetPayload(String instanceId, Long defId) {
        return new WorkspaceWidgetPayload(instanceId, 0, 3, 1, null, defId, Map.of());
    }

    private WorkspaceWidgetDefinition widgetDef(String tipo, boolean withDataset) {
        WorkspaceWidgetDefinition def = new WorkspaceWidgetDefinition();
        def.setId(WIDGET_DEF_ID);
        def.setTipo(tipo);
        def.setInvalido(false);
        if (withDataset) {
            def.setFontes(List.of(new WidgetSourceRef(WidgetSourceKind.DATASET, "5")));
        } else {
            def.setFontes(List.of());
        }
        return def;
    }

    private void stubCompetencia() {
        when(folhaConsultaPort.findResumoMaisRecente()).thenReturn(Optional.of(
            new FolhaResumoSnapshot(
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 6, 30),
                BigDecimal.TEN,
                1,
                false,
                BigDecimal.ZERO)));
    }

    private void stubDatasetSeries() {
        WorkspaceDataset dataset = new WorkspaceDataset();
        dataset.setSchema(List.of(new DatasetFieldSchema("valor_orcado", DatasetFieldType.MOEDA, null, true)));
        when(datasetService.findOwnedDataset(eq(USUARIO_ID), eq(5L))).thenReturn(dataset);
        when(rowRepository.findByDatasetIdOrderByOrdemAscIdAsc(5L)).thenReturn(List.of());
    }
}
