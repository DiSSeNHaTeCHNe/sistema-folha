package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.api.CreateWorkspaceRequest;
import br.com.techne.sistemafolha.workspace.api.SaveWorkspaceLayoutRequest;
import br.com.techne.sistemafolha.workspace.api.UpdateWorkspaceRequest;
import br.com.techne.sistemafolha.workspace.api.WorkspaceDTO;
import br.com.techne.sistemafolha.workspace.api.WorkspaceSummaryDTO;
import br.com.techne.sistemafolha.workspace.api.WorkspaceWidgetDTO;
import br.com.techne.sistemafolha.workspace.domain.Workspace;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceAcessoNegadoException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceNameConflictException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceNotFoundException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceQuotaExceededException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceWidgetPayload;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    private static final String LOGIN = "gestor";
    private static final Long USUARIO_ID = 10L;
    private static final Long WORKSPACE_ID = 5L;

    @Mock
    private WorkspaceAccessGuard workspaceAccessGuard;

    @Mock
    private WorkspaceRepository workspaceRepository;

    private WorkspaceLayoutValidator layoutValidator;
    private WorkspaceService workspaceService;

    @BeforeEach
    void setUp() {
        layoutValidator = new WorkspaceLayoutValidator();
        workspaceService = new WorkspaceService(
            workspaceAccessGuard,
            workspaceRepository,
            layoutValidator,
            new DatasetQuotaPolicy());
    }

    @Test
    void listar_retornaWorkspacesDoUsuario() {
        stubAcesso();
        LocalDateTime updatedAt = LocalDateTime.parse("2026-08-01T14:30:00");
        Workspace ws = workspaceEntity("Planejamento", List.of());
        ws.setDataAtualizacao(updatedAt);
        when(workspaceRepository.findByUsuarioIdOrderByNomeAsc(USUARIO_ID)).thenReturn(List.of(ws));

        List<WorkspaceSummaryDTO> result = workspaceService.listar(LOGIN);

        assertEquals(1, result.size());
        assertEquals("Planejamento", result.get(0).nome());
        assertEquals(0, result.get(0).totalWidgets());
        assertEquals(updatedAt, result.get(0).dataAtualizacao());
    }

    @Test
    void criar_persisteWorkspaceVazio() {
        stubAcesso();
        when(workspaceRepository.countByUsuarioId(USUARIO_ID)).thenReturn(0L);
        when(workspaceRepository.existsByUsuarioIdAndNome(USUARIO_ID, "Anual")).thenReturn(false);
        when(workspaceRepository.save(any())).thenAnswer(inv -> {
            Workspace ws = inv.getArgument(0);
            ws.setId(WORKSPACE_ID);
            return ws;
        });

        WorkspaceDTO result = workspaceService.criar(LOGIN, new CreateWorkspaceRequest("Anual"));

        assertEquals(WORKSPACE_ID, result.id());
        assertEquals("Anual", result.nome());
        assertTrue(result.widgets().isEmpty());
    }

    @Test
    void criar_nomeDuplicado_lanca409() {
        stubAcesso();
        when(workspaceRepository.countByUsuarioId(USUARIO_ID)).thenReturn(1L);
        when(workspaceRepository.existsByUsuarioIdAndNome(USUARIO_ID, "Anual")).thenReturn(true);
        Workspace existing = workspaceEntity("Anual", List.of());
        existing.setId(99L);
        when(workspaceRepository.findByUsuarioIdOrderByNomeAsc(USUARIO_ID)).thenReturn(List.of(existing));

        assertThrows(WorkspaceNameConflictException.class,
            () -> workspaceService.criar(LOGIN, new CreateWorkspaceRequest("Anual")));
    }

    @Test
    void criar_quotaExcedida_lanca400() {
        stubAcesso();
        when(workspaceRepository.countByUsuarioId(USUARIO_ID)).thenReturn(10L);

        assertThrows(WorkspaceQuotaExceededException.class,
            () -> workspaceService.criar(LOGIN, new CreateWorkspaceRequest("Novo")));
    }

    @Test
    void obter_workspaceDeOutroUsuario_lanca404() {
        stubAcesso();
        when(workspaceRepository.findByUsuarioIdAndId(USUARIO_ID, WORKSPACE_ID)).thenReturn(Optional.empty());

        assertThrows(WorkspaceNotFoundException.class,
            () -> workspaceService.obter(LOGIN, WORKSPACE_ID));
    }

    @Test
    void salvarLayout_normalizaOrdemEPersiste() {
        stubAcesso();
        Workspace workspace = workspaceEntity("Main", new ArrayList<>());
        workspace.setId(WORKSPACE_ID);
        when(workspaceRepository.findByUsuarioIdAndId(USUARIO_ID, WORKSPACE_ID)).thenReturn(Optional.of(workspace));
        when(workspaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SaveWorkspaceLayoutRequest request = new SaveWorkspaceLayoutRequest(List.of(
            new WorkspaceWidgetDTO("b", 5, 3, 1, null, 2L, Map.of()),
            new WorkspaceWidgetDTO("a", 1, 6, 2, "kpi-total-funcionarios", null, Map.of())));

        WorkspaceDTO result = workspaceService.salvarLayout(LOGIN, WORKSPACE_ID, request);

        assertEquals(2, result.widgets().size());
        assertEquals("a", result.widgets().get(0).instanceId());
        assertEquals(0, result.widgets().get(0).ordem());
        assertEquals("b", result.widgets().get(1).instanceId());
        assertEquals(1, result.widgets().get(1).ordem());
    }

    @Test
    void salvarLayout_layoutInvalido_propagaErro() {
        stubAcesso();
        Workspace workspace = workspaceEntity("Main", new ArrayList<>());
        workspace.setId(WORKSPACE_ID);
        when(workspaceRepository.findByUsuarioIdAndId(USUARIO_ID, WORKSPACE_ID)).thenReturn(Optional.of(workspace));

        SaveWorkspaceLayoutRequest request = new SaveWorkspaceLayoutRequest(List.of(
            new WorkspaceWidgetDTO("a", 0, 13, 1, null, 1L, Map.of())));

        assertThrows(IllegalArgumentException.class,
            () -> workspaceService.salvarLayout(LOGIN, WORKSPACE_ID, request));
    }

    @Test
    void atualizarNome_alteraNomeUnico() {
        stubAcesso();
        Workspace workspace = workspaceEntity("Antigo", List.of());
        workspace.setId(WORKSPACE_ID);
        when(workspaceRepository.findByUsuarioIdAndId(USUARIO_ID, WORKSPACE_ID)).thenReturn(Optional.of(workspace));
        when(workspaceRepository.existsByUsuarioIdAndNome(USUARIO_ID, "Novo")).thenReturn(false);
        when(workspaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkspaceDTO result = workspaceService.atualizarNome(LOGIN, WORKSPACE_ID, new UpdateWorkspaceRequest("Novo"));

        assertEquals("Novo", result.nome());
    }

    @Test
    void excluir_removeSomenteWorkspace() {
        stubAcesso();
        Workspace workspace = workspaceEntity("Temp", List.of(widgetPayload("x", 0)));
        workspace.setId(WORKSPACE_ID);
        when(workspaceRepository.findByUsuarioIdAndId(USUARIO_ID, WORKSPACE_ID)).thenReturn(Optional.of(workspace));

        workspaceService.excluir(LOGIN, WORKSPACE_ID);

        verify(workspaceRepository).delete(workspace);
    }

    @Test
    void excluir_semAcesso_lanca403() {
        doThrow(new WorkspaceAcessoNegadoException()).when(workspaceAccessGuard).assertEscopo(LOGIN);

        assertThrows(WorkspaceAcessoNegadoException.class,
            () -> workspaceService.excluir(LOGIN, WORKSPACE_ID));
        verify(workspaceRepository, never()).delete(any());
    }

    @Test
    void findOwnedWorkspace_retornaEntidadeDoUsuario() {
        Workspace workspace = workspaceEntity("Owned", List.of());
        workspace.setId(WORKSPACE_ID);
        when(workspaceRepository.findByUsuarioIdAndId(USUARIO_ID, WORKSPACE_ID)).thenReturn(Optional.of(workspace));

        Workspace result = workspaceService.findOwnedWorkspace(USUARIO_ID, WORKSPACE_ID);

        assertEquals(WORKSPACE_ID, result.getId());
    }

    private void stubAcesso() {
        when(workspaceAccessGuard.resolve(LOGIN))
            .thenReturn(new WorkspaceAccessGuard.ResolvedWorkspaceAccess(
                false, USUARIO_ID, null, null));
    }

    private Workspace workspaceEntity(String nome, List<WorkspaceWidgetPayload> widgets) {
        Workspace ws = new Workspace();
        ws.setUsuarioId(USUARIO_ID);
        ws.setNome(nome);
        ws.setWidgets(new ArrayList<>(widgets));
        return ws;
    }

    private WorkspaceWidgetPayload widgetPayload(String instanceId, int ordem) {
        return new WorkspaceWidgetPayload(instanceId, ordem, 3, 1, null, 1L, Map.of());
    }
}
