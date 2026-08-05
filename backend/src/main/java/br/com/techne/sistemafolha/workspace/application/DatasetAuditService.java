package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.api.DatasetRowAuditEntryDTO;
import br.com.techne.sistemafolha.workspace.domain.DatasetRowAuditAction;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetRowAudit;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceDatasetRowAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DatasetAuditService {

    private final WorkspaceDatasetRowAuditRepository auditRepository;

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
}
