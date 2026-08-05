package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.api.CreateWorkspaceRequest;
import br.com.techne.sistemafolha.workspace.api.SaveWorkspaceLayoutRequest;
import br.com.techne.sistemafolha.workspace.api.UpdateWorkspaceRequest;
import br.com.techne.sistemafolha.workspace.api.WorkspaceDTO;
import br.com.techne.sistemafolha.workspace.api.WorkspaceSummaryDTO;
import br.com.techne.sistemafolha.workspace.api.WorkspaceWidgetDTO;
import br.com.techne.sistemafolha.workspace.domain.Workspace;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceNameConflictException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceNotFoundException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceQuotaExceededException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceWidgetPayload;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceAccessGuard workspaceAccessGuard;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceLayoutValidator layoutValidator;
    private final DatasetQuotaPolicy quotaPolicy;

    @Transactional(readOnly = true)
    public List<WorkspaceSummaryDTO> listar(String login) {
        workspaceAccessGuard.assertEscopo(login);
        Long usuarioId = workspaceAccessGuard.resolve(login).usuarioId();
        return workspaceRepository.findByUsuarioIdOrderByNomeAsc(usuarioId).stream()
            .map(this::toSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceDTO obter(String login, Long id) {
        Workspace workspace = findOwnedWorkspace(login, id);
        return toDto(workspace);
    }

    @Transactional
    public WorkspaceDTO criar(String login, CreateWorkspaceRequest request) {
        workspaceAccessGuard.assertEscopo(login);
        Long usuarioId = workspaceAccessGuard.resolve(login).usuarioId();
        long currentCount = workspaceRepository.countByUsuarioId(usuarioId);
        if (!quotaPolicy.canCreateWorkspace(currentCount)) {
            throw new WorkspaceQuotaExceededException(quotaPolicy.workspaceQuotaMessage(currentCount));
        }
        String nome = request.nome().trim();
        assertNomeUnico(usuarioId, nome, null);

        Workspace workspace = new Workspace();
        workspace.setUsuarioId(usuarioId);
        workspace.setNome(nome);
        workspace.setWidgets(new ArrayList<>());
        return toDto(workspaceRepository.save(workspace));
    }

    @Transactional
    public WorkspaceDTO atualizarNome(String login, Long id, UpdateWorkspaceRequest request) {
        Workspace workspace = findOwnedWorkspace(login, id);
        String nome = request.nome().trim();
        assertNomeUnico(workspace.getUsuarioId(), nome, workspace.getId());
        workspace.setNome(nome);
        return toDto(workspaceRepository.save(workspace));
    }

    @Transactional
    public WorkspaceDTO salvarLayout(String login, Long id, SaveWorkspaceLayoutRequest request) {
        Workspace workspace = findOwnedWorkspace(login, id);
        List<WorkspaceWidgetPayload> payloads = toPayloads(request.widgets());
        layoutValidator.validar(payloads);
        List<WorkspaceWidgetPayload> normalizados = layoutValidator.normalizarOrdem(payloads);
        workspace.setWidgets(new ArrayList<>(normalizados));
        return toDto(workspaceRepository.save(workspace));
    }

    @Transactional
    public void excluir(String login, Long id) {
        Workspace workspace = findOwnedWorkspace(login, id);
        workspaceRepository.delete(workspace);
    }

    @Transactional(readOnly = true)
    public Workspace findOwnedWorkspace(Long usuarioId, Long id) {
        return workspaceRepository.findByUsuarioIdAndId(usuarioId, id)
            .orElseThrow(() -> new WorkspaceNotFoundException(id));
    }

    private Workspace findOwnedWorkspace(String login, Long id) {
        workspaceAccessGuard.assertEscopo(login);
        Long usuarioId = workspaceAccessGuard.resolve(login).usuarioId();
        return findOwnedWorkspace(usuarioId, id);
    }

    private void assertNomeUnico(Long usuarioId, String nome, Long ignoreId) {
        if (workspaceRepository.existsByUsuarioIdAndNome(usuarioId, nome)) {
            Workspace existing = workspaceRepository.findByUsuarioIdOrderByNomeAsc(usuarioId).stream()
                .filter(w -> nome.equalsIgnoreCase(w.getNome()))
                .findFirst()
                .orElse(null);
            if (existing != null && (ignoreId == null || !ignoreId.equals(existing.getId()))) {
                throw new WorkspaceNameConflictException(nome);
            }
        }
    }

    private List<WorkspaceWidgetPayload> toPayloads(List<WorkspaceWidgetDTO> widgets) {
        return widgets.stream()
            .map(w -> new WorkspaceWidgetPayload(
                w.instanceId(),
                w.ordem(),
                w.colSpan(),
                w.rowSpan(),
                w.widgetId(),
                w.userWidgetDefinitionId(),
                w.config() != null ? w.config() : new HashMap<>()))
            .toList();
    }

    private WorkspaceSummaryDTO toSummary(Workspace workspace) {
        int total = workspace.getWidgets() != null ? workspace.getWidgets().size() : 0;
        return new WorkspaceSummaryDTO(
            workspace.getId(),
            workspace.getNome(),
            total,
            workspace.getDataAtualizacao());
    }

    private WorkspaceDTO toDto(Workspace workspace) {
        List<WorkspaceWidgetDTO> widgets = workspace.getWidgets() == null
            ? List.of()
            : workspace.getWidgets().stream()
                .map(w -> new WorkspaceWidgetDTO(
                    w.instanceId(),
                    w.ordem(),
                    w.colSpan(),
                    w.rowSpan(),
                    w.widgetId(),
                    w.userWidgetDefinitionId(),
                    w.config()))
                .toList();
        return new WorkspaceDTO(workspace.getId(), workspace.getNome(), widgets);
    }
}
