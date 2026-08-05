package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.api.DatasetAuditTimelineEntryDTO;
import br.com.techne.sistemafolha.workspace.api.DatasetRowAuditEntryDTO;
import br.com.techne.sistemafolha.workspace.domain.DatasetRowAuditAction;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetRow;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetRowAudit;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceDatasetRowAuditRepository;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceDatasetRowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DatasetAuditService {

    private final WorkspaceDatasetRowAuditRepository auditRepository;
    private final WorkspaceDatasetRowRepository rowRepository;
    private final DatasetService datasetService;
    private final WorkspaceAccessGuard workspaceAccessGuard;

    @Transactional
    public void registrarCriacao(Long rowId, Long autorUsuarioId, Map<String, Object> valoresNovos) {
        salvar(rowId, autorUsuarioId, DatasetRowAuditAction.CREATE, null, copyMap(valoresNovos));
    }

    @Transactional
    public void registrarAtualizacao(Long rowId, Long autorUsuarioId,
                                     Map<String, Object> valoresAnteriores,
                                     Map<String, Object> valoresNovos) {
        salvar(rowId, autorUsuarioId, DatasetRowAuditAction.UPDATE,
            copyMap(valoresAnteriores), copyMap(valoresNovos));
    }

    @Transactional
    public void registrarExclusao(Long rowId, Long autorUsuarioId, Map<String, Object> valoresAnteriores) {
        salvar(rowId, autorUsuarioId, DatasetRowAuditAction.DELETE, copyMap(valoresAnteriores), null);
    }

    @Transactional(readOnly = true)
    public List<DatasetRowAuditEntryDTO> listarHistorico(Long rowId) {
        return auditRepository.findByRowIdOrderByDataEventoAscIdAsc(rowId).stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<DatasetAuditTimelineEntryDTO> listarHistoricoDataset(String login, Long datasetId) {
        workspaceAccessGuard.assertEscopo(login);
        Long usuarioId = workspaceAccessGuard.resolve(login).usuarioId();
        datasetService.findOwnedDataset(usuarioId, datasetId);

        List<Long> rowIds = rowRepository.findByDatasetIdOrderByOrdemAscIdAsc(datasetId).stream()
            .map(WorkspaceDatasetRow::getId)
            .toList();
        if (rowIds.isEmpty()) {
            return List.of();
        }

        return auditRepository.findByRowIdInOrderByDataEventoDescIdDesc(rowIds).stream()
            .map(this::toTimelineDto)
            .toList();
    }

    private void salvar(Long rowId, Long autorUsuarioId, DatasetRowAuditAction acao,
                        Map<String, Object> valoresAnteriores, Map<String, Object> valoresNovos) {
        WorkspaceDatasetRowAudit entry = new WorkspaceDatasetRowAudit();
        entry.setRowId(rowId);
        entry.setAutorUsuarioId(autorUsuarioId);
        entry.setAcao(acao);
        entry.setValoresAnteriores(valoresAnteriores);
        entry.setValoresNovos(valoresNovos);
        auditRepository.save(entry);
    }

    private Map<String, Object> copyMap(Map<String, Object> source) {
        return source != null ? Map.copyOf(source) : null;
    }

    private DatasetRowAuditEntryDTO toDto(WorkspaceDatasetRowAudit entry) {
        return new DatasetRowAuditEntryDTO(
            entry.getId(),
            entry.getRowId(),
            entry.getAutorUsuarioId(),
            entry.getAcao(),
            entry.getValoresAnteriores(),
            entry.getValoresNovos(),
            entry.getDataEvento()
        );
    }

    private DatasetAuditTimelineEntryDTO toTimelineDto(WorkspaceDatasetRowAudit entry) {
        return new DatasetAuditTimelineEntryDTO(
            entry.getRowId(),
            entry.getAcao(),
            entry.getAutorUsuarioId(),
            entry.getDataEvento(),
            resumirAlteracao(entry)
        );
    }

    private String resumirAlteracao(WorkspaceDatasetRowAudit entry) {
        return switch (entry.getAcao()) {
            case CREATE -> "Linha criada";
            case DELETE -> "Linha excluída";
            case UPDATE -> {
                Map<String, Object> antes = entry.getValoresAnteriores();
                Map<String, Object> depois = entry.getValoresNovos();
                if (antes == null || depois == null) {
                    yield "Linha atualizada";
                }
                List<String> campos = depois.keySet().stream()
                    .filter(campo -> !Objects.equals(antes.get(campo), depois.get(campo)))
                    .sorted()
                    .toList();
                yield campos.isEmpty() ? "Linha atualizada" : "Campos alterados: " + String.join(", ", campos);
            }
        };
    }
}
