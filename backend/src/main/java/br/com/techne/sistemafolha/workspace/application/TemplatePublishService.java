package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.workspace.api.PublishTemplateRequest;
import br.com.techne.sistemafolha.workspace.api.TemplateCatalogItemDTO;
import br.com.techne.sistemafolha.workspace.api.TemplateDTO;
import br.com.techne.sistemafolha.workspace.domain.TemplatePublishException;
import br.com.techne.sistemafolha.workspace.domain.TemplateStructurePayload;
import br.com.techne.sistemafolha.workspace.domain.TemplateTipo;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDataset;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceTemplate;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceTemplateNotFoundException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceTemplateVersion;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceWidgetDefinition;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceTemplateInstallationRepository;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceTemplateRepository;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceTemplateVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TemplatePublishService {

    /** Sentinel id for platform-native orçamento template (WKS-14). */
    public static final long NATIVE_ORCAMENTO_PADRAO_TEMPLATE_ID = 0L;
    public static final String NATIVE_ORCAMENTO_PADRAO_SLUG = "orcamento-padrao";

    private final WorkspaceAccessGuard workspaceAccessGuard;
    private final DatasetService datasetService;
    private final WidgetDefinitionService widgetDefinitionService;
    private final WorkspaceTemplateRepository templateRepository;
    private final WorkspaceTemplateVersionRepository versionRepository;
    private final WorkspaceTemplateInstallationRepository installationRepository;
    private final TemplateStructureHasher structureHasher;

    @Transactional
    public TemplateDTO publicar(String login, PublishTemplateRequest request) {
        var access = workspaceAccessGuard.resolve(login);
        workspaceAccessGuard.assertEscopo(login);
        Long usuarioId = access.usuarioId();
        AccessContextDTO contexto = access.contexto();

        TemplateStructurePayload estrutura;
        TemplateTipo tipo;
        String nome;
        Long sourceId;

        if (request.datasetId() != null) {
            WorkspaceDataset dataset = datasetService.findOwnedDataset(usuarioId, request.datasetId());
            if (dataset.getId() == null) {
                throw new TemplatePublishException("Dataset deve estar salvo antes de publicar (WKS-18)");
            }
            sourceId = dataset.getId();
            tipo = TemplateTipo.DATASET;
            nome = dataset.getNome();
            estrutura = buildDatasetStructure(dataset);
        } else {
            var widgetDto = widgetDefinitionService.obter(login, request.widgetDefinitionId());
            sourceId = widgetDto.id();
            tipo = TemplateTipo.WIDGET;
            nome = widgetDto.nome();
            estrutura = buildWidgetStructure(widgetDto);
        }

        estrutura.setSourceId(sourceId);
        String hash = structureHasher.hash(estrutura);

        WorkspaceTemplate template = findOrCreateTemplate(usuarioId, nome, tipo, contexto.noOrganogramaId());

        Optional<WorkspaceTemplateVersion> hashMatch =
            versionRepository.findByTemplateIdAndEstruturaHash(template.getId(), hash);
        if (hashMatch.isPresent()) {
            WorkspaceTemplateVersion existing = hashMatch.get();
            return toDto(template, existing, false);
        }

        int nextVersion = versionRepository.findFirstByTemplateIdOrderByVersaoDesc(template.getId())
            .map(v -> v.getVersao() + 1)
            .orElse(1);

        WorkspaceTemplateVersion version = new WorkspaceTemplateVersion();
        version.setTemplateId(template.getId());
        version.setVersao(nextVersion);
        version.setEstrutura(estrutura);
        version.setEstruturaHash(hash);
        WorkspaceTemplateVersion saved = versionRepository.save(version);

        return toDto(template, saved, true);
    }

    @Transactional(readOnly = true)
    public List<TemplateCatalogItemDTO> listarCatalogo(String login) {
        workspaceAccessGuard.assertEscopo(login);
        var access = workspaceAccessGuard.resolve(login);
        Long usuarioId = access.usuarioId();
        AccessContextDTO contexto = access.contexto();

        List<TemplateCatalogItemDTO> items = new ArrayList<>();
        for (WorkspaceTemplate template : templateRepository.findByAtivoTrueOrderByNomeAsc()) {
            if (!workspaceAccessGuard.podeVerTemplate(
                contexto, template.getPublicadorUsuarioId(), template.getOrganogramaNoId())) {
                continue;
            }
            WorkspaceTemplateVersion latest = versionRepository
                .findFirstByTemplateIdOrderByVersaoDesc(template.getId())
                .orElse(null);
            if (latest == null) {
                continue;
            }
            var installation = installationRepository
                .findByUsuarioIdAndTemplateId(usuarioId, template.getId())
                .orElse(null);
            Integer versaoInstalada = installation != null ? installation.getVersaoInstalada() : null;
            Long installationId = installation != null ? installation.getId() : null;
            boolean atualizacaoDisponivel = versaoInstalada != null
                && versaoInstalada < latest.getVersao();

            items.add(new TemplateCatalogItemDTO(
                template.getId(),
                template.getNome(),
                template.getTipo(),
                latest.getVersao(),
                latest.getVersao(),
                atualizacaoDisponivel,
                template.getPublicadorUsuarioId(),
                installationId,
                versaoInstalada
            ));
        }
        items.add(0, nativeOrcamentoCatalogItem());
        return items;
    }

    private TemplateCatalogItemDTO nativeOrcamentoCatalogItem() {
        return new TemplateCatalogItemDTO(
            NATIVE_ORCAMENTO_PADRAO_TEMPLATE_ID,
            OrcamentoTemplateInstaller.DATASET_NOME,
            TemplateTipo.PACOTE,
            1,
            1,
            false,
            null,
            null,
            null
        );
    }

    @Transactional(readOnly = true)
    public WorkspaceTemplate findVisibleTemplate(String login, Long templateId) {
        workspaceAccessGuard.assertEscopo(login);
        var access = workspaceAccessGuard.resolve(login);
        WorkspaceTemplate template = templateRepository.findByIdAndAtivoTrue(templateId)
            .orElseThrow(() -> new WorkspaceTemplateNotFoundException(templateId));
        workspaceAccessGuard.assertPodeVerTemplate(
            access.contexto(), template.getPublicadorUsuarioId(), template.getOrganogramaNoId());
        return template;
    }

    @Transactional(readOnly = true)
    public WorkspaceTemplateVersion findLatestVersion(Long templateId) {
        return versionRepository.findFirstByTemplateIdOrderByVersaoDesc(templateId)
            .orElseThrow(() -> new WorkspaceTemplateNotFoundException(templateId));
    }

    @Transactional(readOnly = true)
    public WorkspaceTemplateVersion findVersion(Long templateId, Integer versao) {
        return versionRepository.findByTemplateIdAndVersao(templateId, versao)
            .orElseThrow(() -> new WorkspaceTemplateNotFoundException(templateId));
    }

    private WorkspaceTemplate findOrCreateTemplate(Long usuarioId, String nome, TemplateTipo tipo,
                                                   Long organogramaNoId) {
        List<WorkspaceTemplate> existing = templateRepository
            .findByPublicadorUsuarioIdAndAtivoTrueOrderByNomeAsc(usuarioId);
        Optional<WorkspaceTemplate> match = existing.stream()
            .filter(t -> t.getNome().equals(nome) && t.getTipo() == tipo)
            .findFirst();
        if (match.isPresent()) {
            return match.get();
        }
        WorkspaceTemplate template = new WorkspaceTemplate();
        template.setPublicadorUsuarioId(usuarioId);
        template.setNome(nome);
        template.setTipo(tipo);
        template.setOrganogramaNoId(organogramaNoId);
        template.setAtivo(true);
        return templateRepository.save(template);
    }

    private TemplateStructurePayload buildDatasetStructure(WorkspaceDataset dataset) {
        TemplateStructurePayload payload = new TemplateStructurePayload();
        payload.setKind("DATASET");
        payload.setNome(dataset.getNome());
        payload.setSchema(new ArrayList<>(dataset.getSchema()));
        return payload;
    }

    private TemplateStructurePayload buildWidgetStructure(
            br.com.techne.sistemafolha.workspace.api.WidgetDefinitionDTO widget) {
        TemplateStructurePayload payload = new TemplateStructurePayload();
        payload.setKind("WIDGET");
        payload.setNome(widget.nome());
        payload.setTipo(widget.tipo());
        payload.setFontes(new ArrayList<>(widget.fontes()));
        payload.setFormula(widget.formula());
        payload.setConfig(widget.config() != null ? new HashMap<>(widget.config()) : new HashMap<>());
        return payload;
    }

    private TemplateDTO toDto(WorkspaceTemplate template, WorkspaceTemplateVersion version,
                              boolean novaVersaoCriada) {
        return new TemplateDTO(
            template.getId(),
            template.getNome(),
            template.getTipo(),
            version.getVersao(),
            version.getEstruturaHash(),
            version.getDataPublicacao(),
            template.getPublicadorUsuarioId(),
            novaVersaoCriada
        );
    }
}
