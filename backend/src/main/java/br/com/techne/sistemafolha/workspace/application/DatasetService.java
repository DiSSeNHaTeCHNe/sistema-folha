package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.api.CreateDatasetRequest;
import br.com.techne.sistemafolha.workspace.api.DatasetDTO;
import br.com.techne.sistemafolha.workspace.api.DatasetFieldSchemaDTO;
import br.com.techne.sistemafolha.workspace.api.DatasetSummaryDTO;
import br.com.techne.sistemafolha.workspace.api.UpdateDatasetSchemaRequest;
import br.com.techne.sistemafolha.workspace.domain.DatasetFieldSchema;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDataset;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetConflictException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetNotFoundException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDatasetRow;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceQuotaExceededException;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceDatasetRepository;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceDatasetRowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DatasetService {

    private final WorkspaceAccessGuard workspaceAccessGuard;
    private final WorkspaceDatasetRepository datasetRepository;
    private final WorkspaceDatasetRowRepository rowRepository;
    private final DatasetQuotaPolicy quotaPolicy;

    @Transactional(readOnly = true)
    public List<DatasetSummaryDTO> listar(String login) {
        workspaceAccessGuard.assertEscopo(login);
        Long usuarioId = workspaceAccessGuard.resolve(login).usuarioId();
        return datasetRepository.findByUsuarioIdOrderByNomeAsc(usuarioId).stream()
            .map(this::toSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public DatasetDTO obter(String login, Long id) {
        WorkspaceDataset dataset = findOwnedDataset(login, id);
        return toDto(dataset);
    }

    @Transactional
    public DatasetDTO criar(String login, CreateDatasetRequest request) {
        workspaceAccessGuard.assertEscopo(login);
        Long usuarioId = workspaceAccessGuard.resolve(login).usuarioId();
        long currentCount = datasetRepository.countByUsuarioId(usuarioId);
        if (!quotaPolicy.canCreateDataset(currentCount)) {
            throw new WorkspaceQuotaExceededException(quotaPolicy.datasetQuotaMessage(currentCount));
        }
        validateFieldCount(request.campos().size());
        validateUniqueFieldNames(request.campos());

        WorkspaceDataset dataset = new WorkspaceDataset();
        dataset.setUsuarioId(usuarioId);
        dataset.setNome(request.nome().trim());
        dataset.setSchema(toFieldSchemas(request.campos()));
        WorkspaceDataset saved = datasetRepository.save(dataset);
        return toDto(saved);
    }

    @Transactional
    public DatasetDTO atualizarSchema(String login, Long id, UpdateDatasetSchemaRequest request) {
        WorkspaceDataset dataset = findOwnedDataset(login, id);
        if (!dataset.getSchemaVersion().equals(request.schemaVersion())) {
            throw new WorkspaceDatasetConflictException(
                "Versão do esquema desatualizada (esperada: " + request.schemaVersion()
                    + ", atual: " + dataset.getSchemaVersion() + ")");
        }
        validateFieldCount(request.campos().size());
        validateUniqueFieldNames(request.campos());

        List<DatasetFieldSchema> newSchema = toFieldSchemas(request.campos());
        assertRemocaoSegura(dataset, newSchema, Boolean.TRUE.equals(request.confirmarRemocao()));

        dataset.setSchema(new ArrayList<>(newSchema));
        dataset.setSchemaVersion(dataset.getSchemaVersion() + 1);
        WorkspaceDataset saved = datasetRepository.save(dataset);
        return toDto(saved);
    }

    @Transactional
    public void excluir(String login, Long id) {
        WorkspaceDataset dataset = findOwnedDataset(login, id);
        rowRepository.deleteAll(rowRepository.findByDatasetIdOrderByOrdemAscIdAsc(dataset.getId()));
        datasetRepository.delete(dataset);
    }

    private WorkspaceDataset findOwnedDataset(String login, Long id) {
        workspaceAccessGuard.assertEscopo(login);
        Long usuarioId = workspaceAccessGuard.resolve(login).usuarioId();
        return findOwnedDataset(usuarioId, id);
    }

    public WorkspaceDataset findOwnedDataset(Long usuarioId, Long id) {
        return datasetRepository.findByUsuarioIdAndId(usuarioId, id)
            .orElseThrow(() -> new WorkspaceDatasetNotFoundException(id));
    }

    private void validateFieldCount(int count) {
        if (!quotaPolicy.canAddField(count)) {
            throw new WorkspaceQuotaExceededException(quotaPolicy.fieldQuotaMessage(count));
        }
    }

    private void validateUniqueFieldNames(List<DatasetFieldSchemaDTO> campos) {
        Set<String> names = new HashSet<>();
        for (DatasetFieldSchemaDTO campo : campos) {
            String nome = campo.nome().trim();
            if (!names.add(nome)) {
                throw new IllegalArgumentException("Nome de campo duplicado: " + nome);
            }
        }
    }

    private void assertRemocaoSegura(WorkspaceDataset dataset, List<DatasetFieldSchema> newSchema,
                                     boolean confirmarRemocao) {
        Set<String> oldNames = dataset.getSchema().stream()
            .map(DatasetFieldSchema::nome)
            .collect(Collectors.toSet());
        Set<String> newNames = newSchema.stream()
            .map(DatasetFieldSchema::nome)
            .collect(Collectors.toSet());
        Set<String> removed = new HashSet<>(oldNames);
        removed.removeAll(newNames);
        if (removed.isEmpty()) {
            return;
        }
        List<WorkspaceDatasetRow> rows = rowRepository.findByDatasetIdOrderByOrdemAscIdAsc(dataset.getId());
        for (String fieldName : removed) {
            boolean hasData = rows.stream().anyMatch(row -> hasValue(row.getValores(), fieldName));
            if (hasData && !confirmarRemocao) {
                throw new WorkspaceDatasetConflictException(
                    "Campo '" + fieldName + "' possui dados em linhas existentes; confirme a remoção");
            }
        }
    }

    private boolean hasValue(Map<String, Object> valores, String fieldName) {
        if (valores == null) {
            return false;
        }
        Object value = valores.get(fieldName);
        if (value == null) {
            return false;
        }
        if (value instanceof String s) {
            return !s.isBlank();
        }
        return true;
    }

    private List<DatasetFieldSchema> toFieldSchemas(List<DatasetFieldSchemaDTO> campos) {
        return campos.stream()
            .map(c -> new DatasetFieldSchema(
                c.nome().trim(),
                c.tipo(),
                c.referenciaEntidade(),
                c.obrigatorio()))
            .toList();
    }

    private DatasetDTO toDto(WorkspaceDataset dataset) {
        return new DatasetDTO(
            dataset.getId(),
            dataset.getNome(),
            toFieldSchemaDtos(dataset.getSchema()),
            dataset.getSchemaVersion(),
            rowRepository.countByDatasetId(dataset.getId()));
    }

    private DatasetSummaryDTO toSummary(WorkspaceDataset dataset) {
        return new DatasetSummaryDTO(
            dataset.getId(),
            dataset.getNome(),
            dataset.getSchemaVersion(),
            rowRepository.countByDatasetId(dataset.getId()),
            dataset.getSchema().size());
    }

    private List<DatasetFieldSchemaDTO> toFieldSchemaDtos(List<DatasetFieldSchema> schema) {
        return schema.stream()
            .map(f -> new DatasetFieldSchemaDTO(
                f.nome(),
                f.tipo(),
                f.referenciaEntidade(),
                f.obrigatorio()))
            .toList();
    }
}
