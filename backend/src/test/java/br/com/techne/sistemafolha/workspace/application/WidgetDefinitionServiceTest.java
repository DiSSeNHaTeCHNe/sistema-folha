package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.api.CreateWidgetDefinitionRequest;
import br.com.techne.sistemafolha.workspace.api.UpdateWidgetDefinitionRequest;
import br.com.techne.sistemafolha.workspace.api.WidgetDefinitionDTO;
import br.com.techne.sistemafolha.workspace.domain.DatasetFieldSchema;
import br.com.techne.sistemafolha.workspace.domain.DatasetFieldType;
import br.com.techne.sistemafolha.workspace.domain.InvalidFormulaException;
import br.com.techne.sistemafolha.workspace.domain.WidgetSourceKind;
import br.com.techne.sistemafolha.workspace.domain.WidgetSourceRef;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceAcessoNegadoException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDataset;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceLimits;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceQuotaExceededException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceWidgetDefinition;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceWidgetDefinitionNotFoundException;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceWidgetDefinitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WidgetDefinitionServiceTest {

    private static final String LOGIN = "gestor";
    private static final Long USUARIO_ID = 10L;
    private static final Long DATASET_ID = 1L;

    @Mock
    private WorkspaceAccessGuard workspaceAccessGuard;

    @Mock
    private WorkspaceWidgetDefinitionRepository widgetRepository;

    @Mock
    private DatasetService datasetService;

    private WidgetDefinitionService widgetDefinitionService;

    @BeforeEach
    void setUp() {
        widgetDefinitionService = new WidgetDefinitionService(
            workspaceAccessGuard,
            widgetRepository,
            datasetService,
            new FormulaEngine(),
            new SystemFieldCatalog(),
            new DatasetQuotaPolicy()
        );
    }

    @Test
    void criar_formulaValida_persiste() {
        stubAcesso();
        when(widgetRepository.countByUsuarioId(USUARIO_ID)).thenReturn(0L);
        when(datasetService.findOwnedDataset(USUARIO_ID, DATASET_ID)).thenReturn(sampleDataset());
        when(widgetRepository.save(any())).thenAnswer(inv -> {
            WorkspaceWidgetDefinition w = inv.getArgument(0);
            w.setId(5L);
            return w;
        });

        var request = new CreateWidgetDefinitionRequest(
            "KPI Vendas",
            "KPI",
            List.of(new WidgetSourceRef(WidgetSourceKind.DATASET, String.valueOf(DATASET_ID))),
            "SOMA(quantidade)",
            Map.of()
        );

        WidgetDefinitionDTO result = widgetDefinitionService.criar(LOGIN, request);

        assertEquals(5L, result.id());
        assertEquals("KPI Vendas", result.nome());
        assertEquals(false, result.invalido());
    }

    @Test
    void criar_formulaInvalida_lanca400() {
        stubAcesso();
        when(widgetRepository.countByUsuarioId(USUARIO_ID)).thenReturn(0L);
        when(datasetService.findOwnedDataset(USUARIO_ID, DATASET_ID)).thenReturn(sampleDataset());

        var request = new CreateWidgetDefinitionRequest(
            "KPI Ruim",
            "KPI",
            List.of(new WidgetSourceRef(WidgetSourceKind.DATASET, String.valueOf(DATASET_ID))),
            "SOMA(campo_inexistente)",
            Map.of()
        );

        assertThrows(InvalidFormulaException.class, () ->
            widgetDefinitionService.criar(LOGIN, request));
    }

    @Test
    void criar_quotaExcedida_lancaQuota() {
        stubAcesso();
        when(widgetRepository.countByUsuarioId(USUARIO_ID))
            .thenReturn((long) WorkspaceLimits.MAX_USER_WIDGET_DEFINITIONS);

        assertThrows(WorkspaceQuotaExceededException.class, () ->
            widgetDefinitionService.criar(LOGIN, sampleCreateRequest()));
    }

    @Test
    void criar_tipoInvalido_lanca400() {
        stubAcesso();
        when(widgetRepository.countByUsuarioId(USUARIO_ID)).thenReturn(0L);

        var request = new CreateWidgetDefinitionRequest(
            "Widget",
            "INVALIDO",
            List.of(new WidgetSourceRef(WidgetSourceKind.SISTEMA, "FOLHA")),
            null,
            Map.of()
        );

        assertThrows(IllegalArgumentException.class, () ->
            widgetDefinitionService.criar(LOGIN, request));
    }

    @Test
    void criar_semEscopo_lanca403() {
        org.mockito.Mockito.doThrow(new WorkspaceAcessoNegadoException())
            .when(workspaceAccessGuard).assertEscopo(LOGIN);

        assertThrows(WorkspaceAcessoNegadoException.class, () ->
            widgetDefinitionService.criar(LOGIN, sampleCreateRequest()));
    }

    @Test
    void atualizar_formulaValida_atualiza() {
        stubAcesso();
        WorkspaceWidgetDefinition existing = sampleWidget();
        when(widgetRepository.findByUsuarioIdAndId(USUARIO_ID, 5L)).thenReturn(Optional.of(existing));
        when(datasetService.findOwnedDataset(USUARIO_ID, DATASET_ID)).thenReturn(sampleDataset());
        when(widgetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new UpdateWidgetDefinitionRequest(
            "KPI Atualizado",
            "KPI",
            List.of(new WidgetSourceRef(WidgetSourceKind.DATASET, String.valueOf(DATASET_ID))),
            "MÉDIA(quantidade)",
            Map.of()
        );

        WidgetDefinitionDTO result = widgetDefinitionService.atualizar(LOGIN, 5L, request);

        assertEquals("KPI Atualizado", result.nome());
        assertEquals(false, result.invalido());
    }

    @Test
    void atualizar_naoEncontrado_lanca404() {
        stubAcesso();
        when(widgetRepository.findByUsuarioIdAndId(USUARIO_ID, 99L)).thenReturn(Optional.empty());

        assertThrows(WorkspaceWidgetDefinitionNotFoundException.class, () ->
            widgetDefinitionService.atualizar(LOGIN, 99L, sampleUpdateRequest()));
    }

    @Test
    void listar_retornaWidgetsDoUsuario() {
        stubAcesso();
        when(widgetRepository.findByUsuarioIdOrderByNomeAsc(USUARIO_ID)).thenReturn(List.of(sampleWidget()));

        List<WidgetDefinitionDTO> result = widgetDefinitionService.listar(LOGIN);

        assertEquals(1, result.size());
        assertEquals("KPI Vendas", result.get(0).nome());
    }

    @Test
    void excluir_existente_remove() {
        stubAcesso();
        WorkspaceWidgetDefinition existing = sampleWidget();
        when(widgetRepository.findByUsuarioIdAndId(USUARIO_ID, 5L)).thenReturn(Optional.of(existing));

        widgetDefinitionService.excluir(LOGIN, 5L);

        verify(widgetRepository).delete(existing);
    }

    @Test
    void marcarInvalidoSeFormulaQuebrada_marcaWidget() {
        WorkspaceWidgetDefinition widget = sampleWidget();
        widget.setFormula("SOMA(campo_removido)");
        when(widgetRepository.findByUsuarioIdOrderByNomeAsc(USUARIO_ID)).thenReturn(List.of(widget));
        when(datasetService.findOwnedDataset(USUARIO_ID, DATASET_ID)).thenReturn(sampleDataset());
        when(widgetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        widgetDefinitionService.marcarInvalidoSeFormulaQuebrada(USUARIO_ID, DATASET_ID);

        verify(widgetRepository).save(widget);
        assertTrue(widget.getInvalido());
    }

    @Test
    void criar_semFormula_permite() {
        stubAcesso();
        when(widgetRepository.countByUsuarioId(USUARIO_ID)).thenReturn(0L);
        when(widgetRepository.save(any())).thenAnswer(inv -> {
            WorkspaceWidgetDefinition w = inv.getArgument(0);
            w.setId(6L);
            return w;
        });

        var request = new CreateWidgetDefinitionRequest(
            "Tabela",
            "TABELA",
            List.of(new WidgetSourceRef(WidgetSourceKind.SISTEMA, "FOLHA")),
            null,
            Map.of()
        );

        WidgetDefinitionDTO result = widgetDefinitionService.criar(LOGIN, request);

        assertEquals(6L, result.id());
        verify(datasetService, never()).findOwnedDataset(any(), any());
    }

    private void stubAcesso() {
        when(workspaceAccessGuard.resolve(LOGIN)).thenReturn(
            new WorkspaceAccessGuard.ResolvedWorkspaceAccess(false, USUARIO_ID, null, null));
    }

    private CreateWidgetDefinitionRequest sampleCreateRequest() {
        return new CreateWidgetDefinitionRequest(
            "KPI",
            "KPI",
            List.of(new WidgetSourceRef(WidgetSourceKind.SISTEMA, "FOLHA")),
            null,
            Map.of()
        );
    }

    private UpdateWidgetDefinitionRequest sampleUpdateRequest() {
        return new UpdateWidgetDefinitionRequest(
            "KPI",
            "KPI",
            List.of(new WidgetSourceRef(WidgetSourceKind.SISTEMA, "FOLHA")),
            null,
            Map.of()
        );
    }

    private WorkspaceDataset sampleDataset() {
        WorkspaceDataset ds = new WorkspaceDataset();
        ds.setId(DATASET_ID);
        ds.setSchema(new ArrayList<>(List.of(
            new DatasetFieldSchema("quantidade", DatasetFieldType.NUMERO, null, true, null)
        )));
        return ds;
    }

    private WorkspaceWidgetDefinition sampleWidget() {
        WorkspaceWidgetDefinition widget = new WorkspaceWidgetDefinition();
        widget.setId(5L);
        widget.setUsuarioId(USUARIO_ID);
        widget.setNome("KPI Vendas");
        widget.setTipo("KPI");
        widget.setFontes(new ArrayList<>(List.of(
            new WidgetSourceRef(WidgetSourceKind.DATASET, String.valueOf(DATASET_ID))
        )));
        widget.setFormula("SOMA(quantidade)");
        widget.setInvalido(false);
        return widget;
    }
}
