package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.api.CreateDatasetRequest;
import br.com.techne.sistemafolha.workspace.api.CreateWidgetDefinitionRequest;
import br.com.techne.sistemafolha.workspace.api.DatasetFieldSchemaDTO;
import br.com.techne.sistemafolha.workspace.api.OrcamentoInstallResultDTO;
import br.com.techne.sistemafolha.workspace.api.TemplateInstallResultDTO;
import br.com.techne.sistemafolha.workspace.api.UpdateDatasetSchemaRequest;
import br.com.techne.sistemafolha.workspace.domain.DatasetFieldSchema;
import br.com.techne.sistemafolha.workspace.domain.TemplateStructurePayload;
import br.com.techne.sistemafolha.workspace.domain.TemplateTipo;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceTemplate;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceTemplateInstallation;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceTemplateInstallationNotFoundException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceTemplateVersion;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceDatasetRepository;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceTemplateInstallationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TemplateInstallService {

    private final WorkspaceAccessGuard workspaceAccessGuard;
    private final TemplatePublishService templatePublishService;
    private final OrcamentoTemplateInstaller orcamentoTemplateInstaller;
    private final DatasetService datasetService;
    private final WidgetDefinitionService widgetDefinitionService;
    private final WorkspaceService workspaceService;
    private final WorkspaceTemplateInstallationRepository installationRepository;
    private final WorkspaceDatasetRepository datasetRepository;

    @Transactional
    public TemplateInstallResultDTO instalar(String login, Long templateId, Long workspaceId) {
        if (TemplatePublishService.NATIVE_ORCAMENTO_PADRAO_TEMPLATE_ID == templateId) {
            OrcamentoInstallResultDTO orcamento = orcamentoTemplateInstaller.instalarOrcamentoPadrao(login, workspaceId);
            return fromOrcamentoInstall(orcamento, templateId);
        }

        workspaceAccessGuard.assertEscopo(login);
        Long usuarioId = workspaceAccessGuard.resolve(login).usuarioId();
        workspaceService.findOwnedWorkspace(usuarioId, workspaceId);

        WorkspaceTemplate template = templatePublishService.findVisibleTemplate(login, templateId);
        WorkspaceTemplateVersion version = templatePublishService.findLatestVersion(templateId);

        return criarInstalacao(login, usuarioId, template, version, workspaceId);
    }

    @Transactional
    public TemplateInstallResultDTO atualizarVersao(String login, Long installationId) {
        workspaceAccessGuard.assertEscopo(login);
        Long usuarioId = workspaceAccessGuard.resolve(login).usuarioId();

        WorkspaceTemplateInstallation installation = installationRepository
            .findByIdAndUsuarioId(installationId, usuarioId)
            .orElseThrow(() -> new WorkspaceTemplateInstallationNotFoundException(installationId));

        WorkspaceTemplate template = templatePublishService.findVisibleTemplate(
            login, installation.getTemplateId());
        WorkspaceTemplateVersion latest = templatePublishService.findLatestVersion(template.getId());

        if (installation.getVersaoInstalada() >= latest.getVersao()) {
            return toResult(installation, template.getTipo());
        }

        aplicarUpgradeSchema(login, installation, latest);
        installation.setVersaoInstalada(latest.getVersao());
        installationRepository.save(installation);

        return toResult(installation, template.getTipo());
    }

    private TemplateInstallResultDTO criarInstalacao(String login, Long usuarioId,
                                                       WorkspaceTemplate template,
                                                       WorkspaceTemplateVersion version,
                                                       Long workspaceId) {
        var existing = installationRepository.findByUsuarioIdAndTemplateId(usuarioId, template.getId());
        if (existing.isPresent()) {
            WorkspaceTemplateInstallation inst = existing.get();
            return toResult(inst, template.getTipo());
        }

        Map<String, Long> entityMap = copiarEstrutura(login, version.getEstrutura(), template.getTipo());

        WorkspaceTemplateInstallation installation = new WorkspaceTemplateInstallation();
        installation.setUsuarioId(usuarioId);
        installation.setTemplateId(template.getId());
        installation.setVersaoInstalada(version.getVersao());
        installation.setWorkspaceId(workspaceId);
        if (template.getTipo() == TemplateTipo.DATASET) {
            installation.setDatasetIds(Map.of("primary", entityMap.get("datasetId")));
        } else if (template.getTipo() == TemplateTipo.WIDGET) {
            installation.setWidgetDefinitionIds(Map.of("primary", entityMap.get("widgetDefinitionId")));
        }
        WorkspaceTemplateInstallation saved = installationRepository.save(installation);

        return toResult(saved, template.getTipo());
    }

    private Map<String, Long> copiarEstrutura(String login, TemplateStructurePayload estrutura,
                                              TemplateTipo tipo) {
        Map<String, Long> result = new HashMap<>();
        if ("DATASET".equals(estrutura.getKind()) || tipo == TemplateTipo.DATASET) {
            List<DatasetFieldSchemaDTO> campos = estrutura.getSchema().stream()
                .map(f -> new DatasetFieldSchemaDTO(
                    f.nome(), f.tipo(), f.referenciaEntidade(), f.obrigatorio(), f.observacao()))
                .toList();
            var created = datasetService.criar(login, new CreateDatasetRequest(estrutura.getNome(), campos));
            result.put("datasetId", created.id());
        } else {
            var created = widgetDefinitionService.criar(login, new CreateWidgetDefinitionRequest(
                estrutura.getNome(),
                estrutura.getTipo(),
                estrutura.getFontes(),
                estrutura.getFormula(),
                estrutura.getConfig()));
            result.put("widgetDefinitionId", created.id());
        }
        return result;
    }

    /**
     * WKS-21: upgrade adiciona campos compatíveis sem apagar dados existentes.
     */
    private void aplicarUpgradeSchema(String login, WorkspaceTemplateInstallation installation,
                                      WorkspaceTemplateVersion latest) {
        TemplateStructurePayload estrutura = latest.getEstrutura();
        if (!"DATASET".equals(estrutura.getKind())) {
            return;
        }
        Long datasetId = installation.getDatasetIds() != null
            ? installation.getDatasetIds().get("primary") : null;
        if (datasetId == null) {
            return;
        }
        var dataset = datasetRepository.findById(datasetId).orElse(null);
        if (dataset == null) {
            return;
        }
        List<DatasetFieldSchemaDTO> novosCampos = mergeSchema(
            dataset.getSchema(), estrutura.getSchema());
        datasetService.atualizarSchema(
            login, datasetId,
            new UpdateDatasetSchemaRequest(
                novosCampos,
                dataset.getSchemaVersion(),
                false));
    }

    private List<DatasetFieldSchemaDTO> mergeSchema(List<DatasetFieldSchema> existing,
                                                    List<DatasetFieldSchema> incoming) {
        Map<String, DatasetFieldSchema> merged = new HashMap<>();
        for (DatasetFieldSchema field : existing) {
            merged.put(field.nome(), field);
        }
        for (DatasetFieldSchema field : incoming) {
            merged.putIfAbsent(field.nome(), field);
        }
        return merged.values().stream()
            .map(f -> new DatasetFieldSchemaDTO(
                f.nome(), f.tipo(), f.referenciaEntidade(), f.obrigatorio(), f.observacao()))
            .toList();
    }

    private TemplateInstallResultDTO fromOrcamentoInstall(OrcamentoInstallResultDTO orcamento, Long templateId) {
        Map<String, Long> entityMap = new HashMap<>();
        if (orcamento.datasetId() != null) {
            entityMap.put("datasetId", orcamento.datasetId());
        }
        return new TemplateInstallResultDTO(
            null,
            templateId,
            1,
            orcamento.workspaceId(),
            orcamento.datasetId(),
            orcamento.widgetDefinitionIds(),
            entityMap
        );
    }

    private TemplateInstallResultDTO toResult(WorkspaceTemplateInstallation installation, TemplateTipo tipo) {
        Map<String, Long> entityMap = new HashMap<>();
        Long datasetId = null;
        List<Long> widgetIds = new ArrayList<>();
        if (installation.getDatasetIds() != null) {
            entityMap.putAll(installation.getDatasetIds());
            datasetId = installation.getDatasetIds().get("primary");
        }
        if (installation.getWidgetDefinitionIds() != null) {
            entityMap.putAll(installation.getWidgetDefinitionIds());
            installation.getWidgetDefinitionIds().values().forEach(widgetIds::add);
        }
        return new TemplateInstallResultDTO(
            installation.getId(),
            installation.getTemplateId(),
            installation.getVersaoInstalada(),
            installation.getWorkspaceId(),
            datasetId,
            widgetIds,
            entityMap
        );
    }
}
