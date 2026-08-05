package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.api.DatasetRowDTO;
import br.com.techne.sistemafolha.workspace.api.DatasetRowRequest;
import br.com.techne.sistemafolha.workspace.domain.DatasetRowValidationException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDataset;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetRow;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetRowNotFoundException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceQuotaExceededException;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceDatasetRowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DatasetRowService {

    private final WorkspaceAccessGuard workspaceAccessGuard;
    private final DatasetService datasetService;
    private final WorkspaceDatasetRowRepository rowRepository;
    private final DatasetQuotaPolicy quotaPolicy;
    private final br.com.techne.sistemafolha.workspace.domain.DatasetRowValidator rowValidator =
        new br.com.techne.sistemafolha.workspace.domain.DatasetRowValidator();

    @Transactional
    public DatasetRowDTO adicionarLinha(String login, Long datasetId, DatasetRowRequest request) {
        workspaceAccessGuard.assertEscopo(login);
        Long usuarioId = workspaceAccessGuard.resolve(login).usuarioId();
        WorkspaceDataset dataset = datasetService.findOwnedDataset(usuarioId, datasetId);

        long rowCount = rowRepository.countByDatasetId(datasetId);
        if (!quotaPolicy.canAddRow(rowCount)) {
            throw new WorkspaceQuotaExceededException(quotaPolicy.rowQuotaMessage(rowCount));
        }

        validarValores(dataset, request.valores());
        WorkspaceDatasetRow row = new WorkspaceDatasetRow();
        row.setDatasetId(datasetId);
        row.setValores(Map.copyOf(request.valores()));
        row.setOrdem((int) rowCount);
        return toDto(rowRepository.save(row));
    }

    @Transactional
    public DatasetRowDTO atualizarLinha(String login, Long datasetId, Long rowId, DatasetRowRequest request) {
        workspaceAccessGuard.assertEscopo(login);
        Long usuarioId = workspaceAccessGuard.resolve(login).usuarioId();
        WorkspaceDataset dataset = datasetService.findOwnedDataset(usuarioId, datasetId);
        WorkspaceDatasetRow row = findOwnedRow(datasetId, rowId);

        validarValores(dataset, request.valores());
        row.setValores(Map.copyOf(request.valores()));
        return toDto(rowRepository.save(row));
    }

    @Transactional
    public void removerLinha(String login, Long datasetId, Long rowId) {
        workspaceAccessGuard.assertEscopo(login);
        Long usuarioId = workspaceAccessGuard.resolve(login).usuarioId();
        datasetService.findOwnedDataset(usuarioId, datasetId);
        WorkspaceDatasetRow row = findOwnedRow(datasetId, rowId);
        rowRepository.delete(row);
    }

    @Transactional(readOnly = true)
    public List<DatasetRowDTO> listarLinhas(String login, Long datasetId) {
        workspaceAccessGuard.assertEscopo(login);
        Long usuarioId = workspaceAccessGuard.resolve(login).usuarioId();
        datasetService.findOwnedDataset(usuarioId, datasetId);
        return rowRepository.findByDatasetIdOrderByOrdemAscIdAsc(datasetId).stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public DatasetRowDTO obterLinha(String login, Long datasetId, Long rowId) {
        workspaceAccessGuard.assertEscopo(login);
        Long usuarioId = workspaceAccessGuard.resolve(login).usuarioId();
        datasetService.findOwnedDataset(usuarioId, datasetId);
        return toDto(findOwnedRow(datasetId, rowId));
    }

    private WorkspaceDatasetRow findOwnedRow(Long datasetId, Long rowId) {
        return rowRepository.findByDatasetIdAndId(datasetId, rowId)
            .orElseThrow(() -> new WorkspaceDatasetRowNotFoundException(datasetId, rowId));
    }

    private void validarValores(WorkspaceDataset dataset, Map<String, Object> valores) {
        var errors = rowValidator.validate(dataset.getSchema(), valores);
        if (!errors.isEmpty()) {
            throw new DatasetRowValidationException(errors);
        }
    }

    private DatasetRowDTO toDto(WorkspaceDatasetRow row) {
        return new DatasetRowDTO(
            row.getId(),
            row.getDatasetId(),
            row.getValores(),
            row.getOrdem()
        );
    }
}
