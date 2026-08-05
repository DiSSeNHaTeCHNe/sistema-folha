package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.api.CreateDatasetRequest;
import br.com.techne.sistemafolha.workspace.api.CreateWidgetDefinitionRequest;
import br.com.techne.sistemafolha.workspace.api.DatasetDTO;
import br.com.techne.sistemafolha.workspace.api.DatasetFieldSchemaDTO;
import br.com.techne.sistemafolha.workspace.api.DatasetSummaryDTO;
import br.com.techne.sistemafolha.workspace.api.OrcamentoInstallResultDTO;
import br.com.techne.sistemafolha.workspace.api.SaveWorkspaceLayoutRequest;
import br.com.techne.sistemafolha.workspace.api.WidgetDefinitionDTO;
import br.com.techne.sistemafolha.workspace.api.WorkspaceDTO;
import br.com.techne.sistemafolha.workspace.domain.DatasetFieldType;
import br.com.techne.sistemafolha.workspace.domain.ReferenciaEntidade;
import br.com.techne.sistemafolha.workspace.domain.WidgetSourceKind;
import br.com.techne.sistemafolha.workspace.domain.WidgetSourceRef;
import br.com.techne.sistemafolha.workspace.domain.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrcamentoTemplateInstallerTest {

    private static final String LOGIN = "gestor";
    private static final Long USUARIO_ID = 10L;
    private static final Long WORKSPACE_ID = 1L;

    @Mock
    private WorkspaceAccessGuard workspaceAccessGuard;
    @Mock
    private DatasetService datasetService;
    @Mock
    private WidgetDefinitionService widgetDefinitionService;
    @Mock
    private WorkspaceService workspaceService;

    private OrcamentoTemplateInstaller installer;

    @BeforeEach
    void setUp() {
        installer = new OrcamentoTemplateInstaller(
            workspaceAccessGuard, datasetService, widgetDefinitionService, workspaceService);
        when(workspaceAccessGuard.resolve(LOGIN))
            .thenReturn(new WorkspaceAccessGuard.ResolvedWorkspaceAccess(
                false, USUARIO_ID, null, null));
        when(workspaceService.findOwnedWorkspace(USUARIO_ID, WORKSPACE_ID)).thenReturn(new Workspace());
    }

    @Test
    void instalar_criaDatasetWidgetsELayout() {
        when(datasetService.listar(LOGIN)).thenReturn(List.of());
        when(datasetService.criar(eq(LOGIN), any(CreateDatasetRequest.class)))
            .thenReturn(datasetDto(100L));
        when(widgetDefinitionService.listar(LOGIN)).thenReturn(List.of());
        when(widgetDefinitionService.criar(eq(LOGIN), any(CreateWidgetDefinitionRequest.class)))
            .thenReturn(widgetDto(201L, OrcamentoTemplateInstaller.WIDGET_TABELA_NOME))
            .thenReturn(widgetDto(202L, OrcamentoTemplateInstaller.WIDGET_KPI_NOME));
        when(workspaceService.salvarLayout(eq(LOGIN), eq(WORKSPACE_ID), any(SaveWorkspaceLayoutRequest.class)))
            .thenReturn(new WorkspaceDTO(WORKSPACE_ID, "WS", List.of()));

        OrcamentoInstallResultDTO result = installer.instalarOrcamentoPadrao(LOGIN, WORKSPACE_ID);

        assertEquals(WORKSPACE_ID, result.workspaceId());
        assertEquals(100L, result.datasetId());
        assertEquals(2, result.widgetDefinitionIds().size());
    }

    @Test
    void instalar_reutilizaDatasetExistente() {
        when(datasetService.listar(LOGIN)).thenReturn(List.of(
            new DatasetSummaryDTO(
                50L, OrcamentoTemplateInstaller.DATASET_NOME, 1, 0, 3,
                LocalDateTime.now(), false, null)));
        when(datasetService.obter(LOGIN, 50L)).thenReturn(datasetDto(50L));
        when(widgetDefinitionService.listar(LOGIN)).thenReturn(List.of(
            widgetDto(201L, OrcamentoTemplateInstaller.WIDGET_TABELA_NOME),
            widgetDto(202L, OrcamentoTemplateInstaller.WIDGET_KPI_NOME)));
        when(workspaceService.salvarLayout(eq(LOGIN), eq(WORKSPACE_ID), any()))
            .thenReturn(new WorkspaceDTO(WORKSPACE_ID, "WS", List.of()));

        OrcamentoInstallResultDTO result = installer.instalarOrcamentoPadrao(LOGIN, WORKSPACE_ID);

        assertEquals(50L, result.datasetId());
        verify(datasetService).obter(LOGIN, 50L);
    }

    @Test
    void instalar_datasetSchemaContemCamposOrcamento() {
        when(datasetService.listar(LOGIN)).thenReturn(List.of());
        ArgumentCaptor<CreateDatasetRequest> captor = ArgumentCaptor.forClass(CreateDatasetRequest.class);
        when(datasetService.criar(eq(LOGIN), captor.capture())).thenReturn(datasetDto(1L));
        when(widgetDefinitionService.listar(LOGIN)).thenReturn(List.of());
        when(widgetDefinitionService.criar(eq(LOGIN), any())).thenReturn(widgetDto(2L, "W"));
        when(workspaceService.salvarLayout(eq(LOGIN), eq(WORKSPACE_ID), any()))
            .thenReturn(new WorkspaceDTO(WORKSPACE_ID, "WS", List.of()));

        installer.instalarOrcamentoPadrao(LOGIN, WORKSPACE_ID);

        List<String> campos = captor.getValue().campos().stream().map(DatasetFieldSchemaDTO::nome).toList();
        assertTrue(campos.contains("competencia"));
        assertTrue(campos.contains("centro_custo_id"));
        assertTrue(campos.contains("valor_orcado"));
    }

    @Test
    void instalar_widgetTabelaReferenciaDatasetESistema() {
        when(datasetService.listar(LOGIN)).thenReturn(List.of());
        when(datasetService.criar(eq(LOGIN), any())).thenReturn(datasetDto(7L));
        when(widgetDefinitionService.listar(LOGIN)).thenReturn(List.of());
        ArgumentCaptor<CreateWidgetDefinitionRequest> captor =
            ArgumentCaptor.forClass(CreateWidgetDefinitionRequest.class);
        when(widgetDefinitionService.criar(eq(LOGIN), captor.capture()))
            .thenReturn(widgetDto(8L, OrcamentoTemplateInstaller.WIDGET_TABELA_NOME))
            .thenReturn(widgetDto(9L, OrcamentoTemplateInstaller.WIDGET_KPI_NOME));
        when(workspaceService.salvarLayout(eq(LOGIN), eq(WORKSPACE_ID), any()))
            .thenReturn(new WorkspaceDTO(WORKSPACE_ID, "WS", List.of()));

        installer.instalarOrcamentoPadrao(LOGIN, WORKSPACE_ID);

        CreateWidgetDefinitionRequest tabelaReq = captor.getAllValues().get(0);
        assertEquals("TABELA", tabelaReq.tipo());
        assertEquals(2, tabelaReq.fontes().size());
        assertTrue(tabelaReq.fontes().stream().anyMatch(f -> f.kind() == WidgetSourceKind.SISTEMA));
    }

    @Test
    void instalar_widgetKpiPossuiFormulaVariacao() {
        when(datasetService.listar(LOGIN)).thenReturn(List.of());
        when(datasetService.criar(eq(LOGIN), any())).thenReturn(datasetDto(7L));
        when(widgetDefinitionService.listar(LOGIN)).thenReturn(List.of());
        ArgumentCaptor<CreateWidgetDefinitionRequest> captor =
            ArgumentCaptor.forClass(CreateWidgetDefinitionRequest.class);
        when(widgetDefinitionService.criar(eq(LOGIN), captor.capture()))
            .thenReturn(widgetDto(8L, OrcamentoTemplateInstaller.WIDGET_TABELA_NOME))
            .thenReturn(widgetDto(9L, OrcamentoTemplateInstaller.WIDGET_KPI_NOME));
        when(workspaceService.salvarLayout(eq(LOGIN), eq(WORKSPACE_ID), any()))
            .thenReturn(new WorkspaceDTO(WORKSPACE_ID, "WS", List.of()));

        installer.instalarOrcamentoPadrao(LOGIN, WORKSPACE_ID);

        CreateWidgetDefinitionRequest kpiReq = captor.getAllValues().get(1);
        assertTrue(kpiReq.formula().contains("SOMA(valor_orcado)"));
        assertTrue(kpiReq.formula().contains("MÉDIA(realizado)"));
    }

    @Test
    void instalar_layoutDoisWidgets() {
        when(datasetService.listar(LOGIN)).thenReturn(List.of());
        when(datasetService.criar(eq(LOGIN), any())).thenReturn(datasetDto(1L));
        when(widgetDefinitionService.listar(LOGIN)).thenReturn(List.of());
        when(widgetDefinitionService.criar(eq(LOGIN), any()))
            .thenReturn(widgetDto(2L, "T"))
            .thenReturn(widgetDto(3L, "K"));
        ArgumentCaptor<SaveWorkspaceLayoutRequest> layoutCaptor =
            ArgumentCaptor.forClass(SaveWorkspaceLayoutRequest.class);
        when(workspaceService.salvarLayout(eq(LOGIN), eq(WORKSPACE_ID), layoutCaptor.capture()))
            .thenReturn(new WorkspaceDTO(WORKSPACE_ID, "WS", List.of()));

        installer.instalarOrcamentoPadrao(LOGIN, WORKSPACE_ID);

        assertEquals(2, layoutCaptor.getValue().widgets().size());
    }

    private DatasetDTO datasetDto(Long id) {
        return new DatasetDTO(id, OrcamentoTemplateInstaller.DATASET_NOME, List.of(
            new DatasetFieldSchemaDTO("competencia", DatasetFieldType.DATA, null, true, null),
            new DatasetFieldSchemaDTO(
                "centro_custo_id", DatasetFieldType.REFERENCIA, ReferenciaEntidade.CENTRO_CUSTO, true, null),
            new DatasetFieldSchemaDTO("valor_orcado", DatasetFieldType.MOEDA, null, true, null)), 1, 0L);
    }

    private WidgetDefinitionDTO widgetDto(Long id, String nome) {
        return new WidgetDefinitionDTO(id, nome, "KPI", List.of(
            new WidgetSourceRef(WidgetSourceKind.DATASET, "1")), null, Map.of(), false);
    }
}
