package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.domain.TemplateStructurePayload;
import br.com.techne.sistemafolha.workspace.domain.TemplateTipo;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceTemplate;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceTemplateVersion;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceTemplateRepository;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceTemplateVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DatasetPublicationLookup {

    private final WorkspaceTemplateRepository templateRepository;
    private final WorkspaceTemplateVersionRepository versionRepository;

    public Map<Long, Integer> buildIndex(Long publicadorUsuarioId) {
        Map<Long, Integer> index = new HashMap<>();
        List<WorkspaceTemplate> templates = templateRepository
            .findByPublicadorUsuarioIdAndAtivoTrueOrderByNomeAsc(publicadorUsuarioId);

        for (WorkspaceTemplate template : templates) {
            if (template.getTipo() != TemplateTipo.DATASET) {
                continue;
            }
            versionRepository.findFirstByTemplateIdOrderByVersaoDesc(template.getId())
                .ifPresent(version -> registerDatasetPublication(index, version));
        }
        return index;
    }

    private void registerDatasetPublication(Map<Long, Integer> index, WorkspaceTemplateVersion version) {
        TemplateStructurePayload estrutura = version.getEstrutura();
        if (estrutura == null) {
            return;
        }
        if (!"DATASET".equals(estrutura.getKind())) {
            return;
        }
        Long sourceId = estrutura.getSourceId();
        if (sourceId == null) {
            return;
        }
        index.merge(sourceId, version.getVersao(), Math::max);
    }
}
