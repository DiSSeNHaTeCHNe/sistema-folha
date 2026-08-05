package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.api.CreateWidgetDefinitionRequest;
import br.com.techne.sistemafolha.workspace.api.UpdateWidgetDefinitionRequest;
import br.com.techne.sistemafolha.workspace.api.WidgetDefinitionDTO;
import br.com.techne.sistemafolha.workspace.domain.DatasetFieldSchema;
import br.com.techne.sistemafolha.workspace.domain.InvalidFormulaException;
import br.com.techne.sistemafolha.workspace.domain.WidgetSourceKind;
import br.com.techne.sistemafolha.workspace.domain.WidgetSourceRef;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDataset;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceQuotaExceededException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceWidgetDefinition;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceWidgetDefinitionNotFoundException;
import br.com.techne.sistemafolha.workspace.domain.formula.AvailableField;
import br.com.techne.sistemafolha.workspace.domain.formula.FormulaValidationResult;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceWidgetDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WidgetDefinitionService {

    private static final Set<String> VALID_TIPOS = Set.of(
        "KPI", "TABELA", "GRAFICO_LINHA", "GRAFICO_BARRA"
    );

    private final WorkspaceAccessGuard workspaceAccessGuard;
    private final WorkspaceWidgetDefinitionRepository widgetRepository;
    private final DatasetService datasetService;
    private final FormulaEngine formulaEngine;
    private final SystemFieldCatalog systemFieldCatalog;
    private final DatasetQuotaPolicy quotaPolicy;

    @Transactional
    public WidgetDefinitionDTO criar(String login, CreateWidgetDefinitionRequest request) {
        workspaceAccessGuard.assertEscopo(login);
        Long usuarioId = workspaceAccessGuard.resolve(login).usuarioId();

        long currentCount = widgetRepository.countByUsuarioId(usuarioId);
        if (!quotaPolicy.canCreateWidgetDefinition(currentCount)) {
            throw new WorkspaceQuotaExceededException(quotaPolicy.widgetDefinitionQuotaMessage(currentCount));
        }

        validarTipo(request.tipo());
        validarFormula(usuarioId, request.fontes(), request.formula());

        WorkspaceWidgetDefinition entity = new WorkspaceWidgetDefinition();
        entity.setUsuarioId(usuarioId);
        entity.setNome(request.nome().trim());
        entity.setTipo(request.tipo());
        entity.setFontes(new ArrayList<>(request.fontes()));
        entity.setFormula(blankToNull(request.formula()));
        entity.setConfig(request.config() != null ? new HashMap<>(request.config()) : new HashMap<>());
        entity.setInvalido(false);

        return toDto(widgetRepository.save(entity));
    }

    @Transactional
    public WidgetDefinitionDTO atualizar(String login, Long id, UpdateWidgetDefinitionRequest request) {
        workspaceAccessGuard.assertEscopo(login);
        Long usuarioId = workspaceAccessGuard.resolve(login).usuarioId();
        WorkspaceWidgetDefinition entity = findOwned(usuarioId, id);

        validarTipo(request.tipo());
        validarFormula(usuarioId, request.fontes(), request.formula());

        entity.setNome(request.nome().trim());
        entity.setTipo(request.tipo());
        entity.setFontes(new ArrayList<>(request.fontes()));
        entity.setFormula(blankToNull(request.formula()));
        entity.setConfig(request.config() != null ? new HashMap<>(request.config()) : new HashMap<>());
        entity.setInvalido(false);

        return toDto(widgetRepository.save(entity));
    }

    @Transactional
    public void excluir(String login, Long id) {
        workspaceAccessGuard.assertEscopo(login);
        Long usuarioId = workspaceAccessGuard.resolve(login).usuarioId();
        widgetRepository.delete(findOwned(usuarioId, id));
    }

    @Transactional(readOnly = true)
    public List<WidgetDefinitionDTO> listar(String login) {
        workspaceAccessGuard.assertEscopo(login);
        Long usuarioId = workspaceAccessGuard.resolve(login).usuarioId();
        return widgetRepository.findByUsuarioIdOrderByNomeAsc(usuarioId).stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public WidgetDefinitionDTO obter(String login, Long id) {
        workspaceAccessGuard.assertEscopo(login);
        Long usuarioId = workspaceAccessGuard.resolve(login).usuarioId();
        return toDto(findOwned(usuarioId, id));
    }

    @Transactional
    public void marcarInvalidoSeFormulaQuebrada(Long usuarioId, Long datasetId) {
        WorkspaceDataset dataset = datasetService.findOwnedDataset(usuarioId, datasetId);
        List<AvailableField> datasetFields = dataset.getSchema().stream()
            .map(DatasetFieldSchema::nome)
            .map(AvailableField::new)
            .toList();

        for (WorkspaceWidgetDefinition widget : widgetRepository.findByUsuarioIdOrderByNomeAsc(usuarioId)) {
            if (widget.getFormula() == null || widget.getFormula().isBlank()) {
                continue;
            }
            if (!widgetReferencesDataset(widget, datasetId)) {
                continue;
            }
            List<AvailableField> fields = resolveAvailableFields(usuarioId, widget.getFontes(), datasetFields);
            FormulaValidationResult result = formulaEngine.validate(widget.getFormula(), fields);
            if (!result.valid()) {
                widget.setInvalido(true);
                widgetRepository.save(widget);
            }
        }
    }

    private boolean widgetReferencesDataset(WorkspaceWidgetDefinition widget, Long datasetId) {
        String datasetRef = String.valueOf(datasetId);
        return widget.getFontes().stream()
            .anyMatch(fonte -> fonte.kind() == WidgetSourceKind.DATASET && datasetRef.equals(fonte.ref()));
    }

    private WorkspaceWidgetDefinition findOwned(Long usuarioId, Long id) {
        return widgetRepository.findByUsuarioIdAndId(usuarioId, id)
            .orElseThrow(() -> new WorkspaceWidgetDefinitionNotFoundException(id));
    }

    private void validarTipo(String tipo) {
        if (tipo == null || !VALID_TIPOS.contains(tipo)) {
            throw new IllegalArgumentException("tipo: inválido — use KPI, TABELA, GRAFICO_LINHA ou GRAFICO_BARRA");
        }
    }

    private void validarFormula(Long usuarioId, List<WidgetSourceRef> fontes, String formula) {
        if (formula == null || formula.isBlank()) {
            return;
        }
        List<AvailableField> fields = resolveAvailableFields(usuarioId, fontes, List.of());
        FormulaValidationResult result = formulaEngine.validate(formula, fields);
        if (!result.valid()) {
            throw new InvalidFormulaException(result.errors());
        }
    }

    private List<AvailableField> resolveAvailableFields(
            Long usuarioId,
            List<WidgetSourceRef> fontes,
            List<AvailableField> extraDatasetFields) {
        Set<String> names = new HashSet<>();
        List<AvailableField> fields = new ArrayList<>();
        for (WidgetSourceRef fonte : fontes) {
            if (fonte.kind() == WidgetSourceKind.DATASET) {
                Long datasetId = Long.parseLong(fonte.ref());
                WorkspaceDataset dataset = datasetService.findOwnedDataset(usuarioId, datasetId);
                for (DatasetFieldSchema schemaField : dataset.getSchema()) {
                    if (names.add(schemaField.nome())) {
                        fields.add(new AvailableField(schemaField.nome()));
                    }
                }
            } else if (fonte.kind() == WidgetSourceKind.SISTEMA) {
                for (AvailableField systemField : systemFieldCatalog.fieldsForSource(fonte.ref())) {
                    if (names.add(systemField.name())) {
                        fields.add(systemField);
                    }
                }
            }
        }
        for (AvailableField extra : extraDatasetFields) {
            if (names.add(extra.name())) {
                fields.add(extra);
            }
        }
        return fields;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private WidgetDefinitionDTO toDto(WorkspaceWidgetDefinition entity) {
        return new WidgetDefinitionDTO(
            entity.getId(),
            entity.getNome(),
            entity.getTipo(),
            entity.getFontes(),
            entity.getFormula(),
            entity.getConfig(),
            Boolean.TRUE.equals(entity.getInvalido())
        );
    }
}
