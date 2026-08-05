package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.workspace.api.CreateWorkspaceRequest;
import br.com.techne.sistemafolha.workspace.api.SaveWorkspaceLayoutRequest;
import br.com.techne.sistemafolha.workspace.api.WorkspaceWidgetDTO;
import br.com.techne.sistemafolha.workspace.domain.Workspace;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceAcessoNegadoException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceNameConflictException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceNotFoundException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceQuotaExceededException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceWidgetPayload;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceRepository;
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
    private static final Long USUARIO_ID = 1L;
    private static final Long WORKSPACE_ID = 10L;

    @Mock
    private WorkspaceAccessGuard workspaceAccessGuard;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private DatasetQuotaPolicy quotaPolicy;

    @org.mockito.Spy
    private WorkspaceLayoutValidator layoutValidator = new WorkspaceLayoutValidator();

    @InjectMocks
    private WorkspaceService workspaceService;

    @Test
    void criar_primeiroWorkspace_persiste() {
        stubAccess();
        when(workspaceRepository.countByUsuarioId(USUARIO_ID)).thenReturn(0L);
        when(quotaPolicy.canCreateWorkspace(0L)).thenReturn(true);
        when(workspaceRepository.existsByUsuarioIdAndNome(USUARIO_ID, "Financeiro")).thenReturn(false);
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> {
            Workspace w = inv.getArgument(0);
            w.setId(WORKSPACE_ID);
            return w;
        });

        var dto = workspaceService.criar(LOGIN, new CreateWorkspaceRequest("Financeiro"));

        assertEquals(WORKSPACE_ID, dto.id());
        assertEquals("Financeiro", dto.nome());
        assertTrue(dto.widgets().isEmpty());
    }

    @Test
    void criar_nomeDuplicado_lanca409() {
        stubAccess();
        when(workspaceRepository.countByUsuarioId(USUARIO_ID)).thenReturn(1L);
        when(quotaPolicy.canCreateWorkspace(1L)).thenReturn(true);
        when(workspaceRepository.existsByUsuarioIdAndNome(USUARIO_ID, "Financeiro")).thenReturn(true);

        assertThrows(WorkspaceNameConflictException.class,
            () -> workspaceService.criar(LOGIN, new CreateWorkspaceRequest("Financeiro")));
    }

    @Test
    void criar_quotaExcedida_lanca400() {
        stubAccess();
        when(workspaceRepository.countByUsuarioId(USUARIO_ID)).thenReturn(10L);
        when(quotaPolicy.canCreateWorkspace(10L)).thenReturn(false);
        when(quotaPolicy.workspaceQuotaMessage(10L)).thenReturn("Limite workspaces");

        assertThrows(WorkspaceQuotaExceededException.class,
            () -> workspaceService.criar(LOGIN, new CreateWorkspaceRequest("Novo")));
    }

    @Test
    void criar_semEscopo_lanca403() {
        doThrow(new WorkspaceAcessoNegadoException()).when(workspaceAccessGuard).assertEscopo(LOGIN);

        assertThrows(WorkspaceAcessoNegadoException.class,
            () -> workspaceService.criar(LOGIN, new CreateWorkspaceRequest("X")));
    }

    @Test
    void obter_workspaceDeOutroUsuario_lanca404() {
        stubAccess();
        when(workspaceRepository.findByUsuarioIdAndId(USUARIO_ID, 99L)).thenReturn(Optional.empty());

        assertThrows(WorkspaceNotFoundException.class, () -> workspaceService.obter(LOGIN, 99L));
    }

    @Test
    void listar_retornaResumoComContagemWidgets() {
        stubAccess();
        Workspace ws = workspace(WORKSPACE_ID, "A", List.of(
            new WorkspaceWidgetPayload("i1", 0, 3, 1, null, 1L, Map.of())));
        when(workspaceRepository.findByUsuarioIdOrderByNomeAsc(USUARIO_ID)).thenReturn(List.of(ws));

        var list = workspaceService.listar(LOGIN);

        assertEquals(1, list.size());
        assertEquals(1, list.get(0).widgetCount());
    }

    @Test
    void salvarLayout_normalizaOrdem() {
        stubAccess();
        Workspace ws = workspace(WORKSPACE_ID, "A", new ArrayList<>());
        when(workspaceRepository.findByUsuarioIdAndId(USUARIO_ID, WORKSPACE_ID)).thenReturn(Optional.of(ws));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new SaveWorkspaceLayoutRequest(List.of(
            widgetDto("b", 5, 3, 1, null, 2L),
            widgetDto("a", 2, 3, 1, null, 1L)));

        var salvo = workspaceService.salvarLayout(LOGIN, WORKSPACE_ID, request);

        assertEquals("a", salvo.widgets().get(0).instanceId());
        assertEquals(0, salvo.widgets().get(0).ordem());
        assertEquals("b", salvo.widgets().get(1).instanceId());
        assertEquals(1, salvo.widgets().get(1).ordem());
    }

    @Test
    void salvarLayout_maisDe30Widgets_lanca400() {
        stubAccess();
        Workspace ws = workspace(WORKSPACE_ID, "A", new ArrayList<>());
        when(workspaceRepository.findByUsuarioIdAndId(USUARIO_ID, WORKSPACE_ID)).thenReturn(Optional.of(ws));

        List<WorkspaceWidgetDTO> widgets = java.util.stream.IntStream.range(0, 31)
            .mapToObj(i -> widgetDto("w" + i, i, 3, 1, null, (long) i))
            .toList();

        assertThrows(IllegalArgumentException.class,
            () -> workspaceService.salvarLayout(LOGIN, WORKSPACE_ID, new SaveWorkspaceLayoutRequest(widgets)));
    }

    @Test
    void excluir_workspace_naoExcluiDatasets() {
        stubAccess();
        Workspace ws = workspace(WORKSPACE_ID, "A", new ArrayList<>());
        when(workspaceRepository.findByUsuarioIdAndId(USUARIO_ID, WORKSPACE_ID)).thenReturn(Optional.of(ws));

        workspaceService.excluir(LOGIN, WORKSPACE_ID);

        verify(workspaceRepository).delete(ws);
    }

    @Test
    void excluir_naoEncontrado_lanca404() {
        stubAccess();
        when(workspaceRepository.findByUsuarioIdAndId(USUARIO_ID, 99L)).thenReturn(Optional.empty());

        assertThrows(WorkspaceNotFoundException.class, () -> workspaceService.excluir(LOGIN, 99L));
    }

    @Test
    void findOwned_porUsuarioId_retornaEntidade() {
        Workspace ws = workspace(WORKSPACE_ID, "A", new ArrayList<>());
        when(workspaceRepository.findByUsuarioIdAndId(USUARIO_ID, WORKSPACE_ID)).thenReturn(Optional.of(ws));

        Workspace found = workspaceService.findOwned(USUARIO_ID, WORKSPACE_ID);

        assertEquals(WORKSPACE_ID, found.getId());
    }

    private void stubAccess() {
        when(workspaceAccessGuard.resolve(LOGIN)).thenReturn(
            new WorkspaceAccessGuard.ResolvedWorkspaceAccess(
                false, USUARIO_ID, contextoTotal(), Set.of()));
    }

    private AccessContextDTO contextoTotal() {
        return new AccessContextDTO(true, true, true, Set.of(), null, 1L, "Raiz", 0);
    }

    private Workspace workspace(Long id, String nome, List<WorkspaceWidgetPayload> widgets) {
        Workspace ws = new Workspace();
        ws.setId(id);
        ws.setUsuarioId(USUARIO_ID);
        ws.setNome(nome);
        ws.setWidgets(new ArrayList<>(widgets));
        return ws;
    }

    private WorkspaceWidgetDTO widgetDto(
            String instanceId, int ordem, int colSpan, int rowSpan,
            String widgetId, Long userWidgetDefinitionId) {
        return new WorkspaceWidgetDTO(instanceId, ordem, colSpan, rowSpan, widgetId, userWidgetDefinitionId, Map.of());
    }
}
