package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.domain.TemplateStructurePayload;
import br.com.techne.sistemafolha.workspace.domain.TemplateTipo;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceTemplate;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceTemplateVersion;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceTemplateRepository;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceTemplateVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatasetPublicationLookupTest {

    private static final Long USUARIO_ID = 10L;
    private static final Long DATASET_ID = 42L;

    @Mock
    private WorkspaceTemplateRepository templateRepository;

    @Mock
    private WorkspaceTemplateVersionRepository versionRepository;

    private DatasetPublicationLookup lookup;

    @BeforeEach
    void setUp() {
        lookup = new DatasetPublicationLookup(templateRepository, versionRepository);
    }

    @Test
    void buildIndex_datasetTemplateAtivo_mapeiaVersaoMaisRecente() {
        WorkspaceTemplate template = datasetTemplate(100L, true);
        WorkspaceTemplateVersion version = versionWithDatasetSource(100L, 2, DATASET_ID);
        when(templateRepository.findByPublicadorUsuarioIdAndAtivoTrueOrderByNomeAsc(USUARIO_ID))
            .thenReturn(List.of(template));
        when(versionRepository.findFirstByTemplateIdOrderByVersaoDesc(100L))
            .thenReturn(Optional.of(version));

        Map<Long, Integer> index = lookup.buildIndex(USUARIO_ID);

        assertEquals(1, index.size());
        assertEquals(2, index.get(DATASET_ID));
    }

    @Test
    void buildIndex_templateInativo_excluidoDoIndice() {
        when(templateRepository.findByPublicadorUsuarioIdAndAtivoTrueOrderByNomeAsc(USUARIO_ID))
            .thenReturn(List.of());

        Map<Long, Integer> index = lookup.buildIndex(USUARIO_ID);

        assertTrue(index.isEmpty());
    }

    @Test
    void buildIndex_multiplasVersoes_usaMaxVersao() {
        WorkspaceTemplate template = datasetTemplate(100L, true);
        WorkspaceTemplateVersion v1 = versionWithDatasetSource(100L, 1, DATASET_ID);
        WorkspaceTemplateVersion v3 = versionWithDatasetSource(100L, 3, DATASET_ID);
        when(templateRepository.findByPublicadorUsuarioIdAndAtivoTrueOrderByNomeAsc(USUARIO_ID))
            .thenReturn(List.of(template));
        when(versionRepository.findFirstByTemplateIdOrderByVersaoDesc(100L))
            .thenReturn(Optional.of(v3));

        Map<Long, Integer> index = lookup.buildIndex(USUARIO_ID);

        assertEquals(3, index.get(DATASET_ID));
        assertTrue(v3.getVersao() > v1.getVersao());
    }

    @Test
    void buildIndex_estruturaWidget_naoMarcaDataset() {
        WorkspaceTemplate template = datasetTemplate(100L, true);
        WorkspaceTemplateVersion widgetVersion = versionWithWidgetSource(100L, 1, DATASET_ID);
        when(templateRepository.findByPublicadorUsuarioIdAndAtivoTrueOrderByNomeAsc(USUARIO_ID))
            .thenReturn(List.of(template));
        when(versionRepository.findFirstByTemplateIdOrderByVersaoDesc(100L))
            .thenReturn(Optional.of(widgetVersion));

        Map<Long, Integer> index = lookup.buildIndex(USUARIO_ID);

        assertTrue(index.isEmpty());
    }

    private WorkspaceTemplate datasetTemplate(Long id, boolean ativo) {
        WorkspaceTemplate template = new WorkspaceTemplate();
        template.setId(id);
        template.setPublicadorUsuarioId(USUARIO_ID);
        template.setNome("Template dataset");
        template.setTipo(TemplateTipo.DATASET);
        template.setAtivo(ativo);
        return template;
    }

    private WorkspaceTemplateVersion versionWithDatasetSource(Long templateId, int versao, Long sourceId) {
        TemplateStructurePayload estrutura = new TemplateStructurePayload();
        estrutura.setKind("DATASET");
        estrutura.setSourceId(sourceId);
        return version(templateId, versao, estrutura);
    }

    private WorkspaceTemplateVersion versionWithWidgetSource(Long templateId, int versao, Long sourceId) {
        TemplateStructurePayload estrutura = new TemplateStructurePayload();
        estrutura.setKind("WIDGET");
        estrutura.setSourceId(sourceId);
        return version(templateId, versao, estrutura);
    }

    private WorkspaceTemplateVersion version(Long templateId, int versao, TemplateStructurePayload estrutura) {
        WorkspaceTemplateVersion version = new WorkspaceTemplateVersion();
        version.setTemplateId(templateId);
        version.setVersao(versao);
        version.setEstrutura(estrutura);
        version.setEstruturaHash("hash-" + versao);
        return version;
    }
}
