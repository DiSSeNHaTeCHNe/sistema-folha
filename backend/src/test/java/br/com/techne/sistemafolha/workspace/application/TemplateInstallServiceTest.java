package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.workspace.api.DatasetDTO;
import br.com.techne.sistemafolha.workspace.api.DatasetFieldSchemaDTO;
import br.com.techne.sistemafolha.workspace.api.TemplateInstallResultDTO;
import br.com.techne.sistemafolha.workspace.domain.DatasetFieldType;
import br.com.techne.sistemafolha.workspace.domain.TemplateStructurePayload;
import br.com.techne.sistemafolha.workspace.domain.TemplateTipo;
import br.com.techne.sistemafolha.workspace.domain.Workspace;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDataset;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceTemplate;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceTemplateInstallation;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceTemplateInstallationNotFoundException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceTemplateVersion;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceDatasetRepository;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceTemplateInstallationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateInstallServiceTest {

    private static final String LOGIN = "gestor";
    private static final Long USUARIO_ID = 10L;

    @Mock private WorkspaceAccessGuard workspaceAccessGuard;
    @Mock private TemplatePublishService templatePublishService;
    @Mock private DatasetService datasetService;
    @Mock private WidgetDefinitionService widgetDefinitionService;
    @Mock private WorkspaceService workspaceService;
    @Mock private WorkspaceTemplateInstallationRepository installationRepository;
    @Mock private WorkspaceDatasetRepository datasetRepository;

    private TemplateInstallService service;

    @BeforeEach
    void setUp() {
        service = new TemplateInstallService(
            workspaceAccessGuard,
            templatePublishService,
            datasetService,
            widgetDefinitionService,
            workspaceService,
            installationRepository,
            datasetRepository
        );
    }

    @Test
    void instalar_criaCopiaIndependente() {
        stubAcesso();
        stubWorkspace(1L);
        WorkspaceTemplate template = template(5L, TemplateTipo.DATASET);
        WorkspaceTemplateVersion version = datasetVersion();
        when(templatePublishService.findVisibleTemplate(LOGIN, 5L)).thenReturn(template);
        when(templatePublishService.findLatestVersion(5L)).thenReturn(version);
        when(installationRepository.findByUsuarioIdAndTemplateId(USUARIO_ID, 5L)).thenReturn(Optional.empty());
        when(datasetService.criar(eq(LOGIN), any())).thenReturn(
            new DatasetDTO(99L, "Vendas", List.of(), 1, 0));
        when(installationRepository.save(any())).thenAnswer(inv -> {
            WorkspaceTemplateInstallation i = inv.getArgument(0);
            i.setId(20L);
            return i;
        });

        TemplateInstallResultDTO result = service.instalar(LOGIN, 5L, 1L);

        assertEquals(99L, result.datasetId());
        assertEquals(1, result.versaoInstalada());
        verify(datasetService).criar(eq(LOGIN), any());
    }

    @Test
    void instalar_jaInstalado_retornaExistente() {
        stubAcesso();
        stubWorkspace(1L);
        WorkspaceTemplate template = template(5L, TemplateTipo.DATASET);
        when(templatePublishService.findVisibleTemplate(LOGIN, 5L)).thenReturn(template);
        when(templatePublishService.findLatestVersion(5L)).thenReturn(datasetVersion());
        WorkspaceTemplateInstallation existing = new WorkspaceTemplateInstallation();
        existing.setId(30L);
        existing.setVersaoInstalada(1);
        existing.setTemplateId(5L);
        existing.setWorkspaceId(1L);
        existing.setDatasetIds(Map.of("primary", 77L));
        when(installationRepository.findByUsuarioIdAndTemplateId(USUARIO_ID, 5L))
            .thenReturn(Optional.of(existing));

        TemplateInstallResultDTO result = service.instalar(LOGIN, 5L, 1L);

        assertEquals(30L, result.installationId());
        assertEquals(77L, result.datasetId());
        verify(datasetService, never()).criar(any(), any());
    }

    @Test
    void atualizarVersao_adicionaCampoCompativel_preservaDados() {
        stubAcesso();
        WorkspaceTemplateInstallation installation = new WorkspaceTemplateInstallation();
        installation.setId(40L);
        installation.setUsuarioId(USUARIO_ID);
        installation.setTemplateId(5L);
        installation.setVersaoInstalada(1);
        installation.setDatasetIds(Map.of("primary", 88L));
        when(installationRepository.findByIdAndUsuarioId(40L, USUARIO_ID))
            .thenReturn(Optional.of(installation));
        when(templatePublishService.findVisibleTemplate(LOGIN, 5L))
            .thenReturn(template(5L, TemplateTipo.DATASET));

        TemplateStructurePayload estrutura = new TemplateStructurePayload();
        estrutura.setKind("DATASET");
        estrutura.setSchema(new ArrayList<>(List.of(
            new br.com.techne.sistemafolha.workspace.domain.DatasetFieldSchema(
                "valor", DatasetFieldType.NUMERO, null, true),
            new br.com.techne.sistemafolha.workspace.domain.DatasetFieldSchema(
                "novoCampo", DatasetFieldType.TEXTO, null, false))));
        WorkspaceTemplateVersion v2 = new WorkspaceTemplateVersion();
        v2.setVersao(2);
        v2.setEstrutura(estrutura);
        when(templatePublishService.findLatestVersion(5L)).thenReturn(v2);

        WorkspaceDataset dataset = new WorkspaceDataset();
        dataset.setId(88L);
        dataset.setUsuarioId(USUARIO_ID);
        dataset.setSchemaVersion(1);
        dataset.setSchema(new ArrayList<>(List.of(
            new br.com.techne.sistemafolha.workspace.domain.DatasetFieldSchema(
                "valor", DatasetFieldType.NUMERO, null, true))));
        when(datasetRepository.findById(88L)).thenReturn(Optional.of(dataset));
        when(installationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TemplateInstallResultDTO result = service.atualizarVersao(LOGIN, 40L);

        assertEquals(2, result.versaoInstalada());
        verify(datasetService).atualizarSchema(eq(LOGIN), eq(88L), any());
    }

    @Test
    void atualizarVersao_jaNaUltima_naoAltera() {
        stubAcesso();
        WorkspaceTemplateInstallation installation = new WorkspaceTemplateInstallation();
        installation.setId(41L);
        installation.setTemplateId(5L);
        installation.setVersaoInstalada(2);
        installation.setDatasetIds(Map.of("primary", 88L));
        when(installationRepository.findByIdAndUsuarioId(41L, USUARIO_ID))
            .thenReturn(Optional.of(installation));
        when(templatePublishService.findVisibleTemplate(LOGIN, 5L))
            .thenReturn(template(5L, TemplateTipo.DATASET));
        WorkspaceTemplateVersion v2 = new WorkspaceTemplateVersion();
        v2.setVersao(2);
        when(templatePublishService.findLatestVersion(5L)).thenReturn(v2);

        TemplateInstallResultDTO result = service.atualizarVersao(LOGIN, 41L);

        assertEquals(2, result.versaoInstalada());
        verify(datasetService, never()).atualizarSchema(any(), any(), any());
    }

    @Test
    void atualizarVersao_instalacaoInexistente_lanca404() {
        stubAcesso();
        when(installationRepository.findByIdAndUsuarioId(999L, USUARIO_ID)).thenReturn(Optional.empty());

        assertThrows(WorkspaceTemplateInstallationNotFoundException.class,
            () -> service.atualizarVersao(LOGIN, 999L));
    }

    @Test
    void instalar_widget_criaWidgetDefinition() {
        stubAcesso();
        stubWorkspace(2L);
        WorkspaceTemplate template = template(6L, TemplateTipo.WIDGET);
        TemplateStructurePayload estrutura = new TemplateStructurePayload();
        estrutura.setKind("WIDGET");
        estrutura.setNome("KPI");
        estrutura.setTipo("KPI");
        WorkspaceTemplateVersion version = new WorkspaceTemplateVersion();
        version.setVersao(1);
        version.setEstrutura(estrutura);
        when(templatePublishService.findVisibleTemplate(LOGIN, 6L)).thenReturn(template);
        when(templatePublishService.findLatestVersion(6L)).thenReturn(version);
        when(installationRepository.findByUsuarioIdAndTemplateId(USUARIO_ID, 6L)).thenReturn(Optional.empty());
        when(widgetDefinitionService.criar(eq(LOGIN), any())).thenReturn(
            new br.com.techne.sistemafolha.workspace.api.WidgetDefinitionDTO(
                55L, "KPI", "KPI", List.of(), null, Map.of(), false));
        when(installationRepository.save(any())).thenAnswer(inv -> {
            WorkspaceTemplateInstallation i = inv.getArgument(0);
            i.setId(21L);
            return i;
        });

        TemplateInstallResultDTO result = service.instalar(LOGIN, 6L, 2L);

        assertEquals(1, result.widgetDefinitionIds().size());
        assertEquals(55L, result.widgetDefinitionIds().get(0));
    }

    @Test
    void instalar_registraVersaoInstalada() {
        stubAcesso();
        stubWorkspace(1L);
        WorkspaceTemplate template = template(7L, TemplateTipo.DATASET);
        WorkspaceTemplateVersion version = datasetVersion();
        version.setVersao(3);
        when(templatePublishService.findVisibleTemplate(LOGIN, 7L)).thenReturn(template);
        when(templatePublishService.findLatestVersion(7L)).thenReturn(version);
        when(installationRepository.findByUsuarioIdAndTemplateId(USUARIO_ID, 7L)).thenReturn(Optional.empty());
        when(datasetService.criar(eq(LOGIN), any())).thenReturn(
            new DatasetDTO(100L, "Vendas", List.of(), 1, 0));
        when(installationRepository.save(any())).thenAnswer(inv -> {
            WorkspaceTemplateInstallation i = inv.getArgument(0);
            assertEquals(3, i.getVersaoInstalada());
            i.setId(22L);
            return i;
        });

        service.instalar(LOGIN, 7L, 1L);
    }

    @Test
    void atualizarVersao_widget_naoAlteraSchemaDataset() {
        stubAcesso();
        WorkspaceTemplateInstallation installation = new WorkspaceTemplateInstallation();
        installation.setId(42L);
        installation.setTemplateId(8L);
        installation.setVersaoInstalada(1);
        installation.setWidgetDefinitionIds(Map.of("primary", 66L));
        when(installationRepository.findByIdAndUsuarioId(42L, USUARIO_ID))
            .thenReturn(Optional.of(installation));
        when(templatePublishService.findVisibleTemplate(LOGIN, 8L))
            .thenReturn(template(8L, TemplateTipo.WIDGET));
        TemplateStructurePayload estrutura = new TemplateStructurePayload();
        estrutura.setKind("WIDGET");
        WorkspaceTemplateVersion v2 = new WorkspaceTemplateVersion();
        v2.setVersao(2);
        v2.setEstrutura(estrutura);
        when(templatePublishService.findLatestVersion(8L)).thenReturn(v2);
        when(installationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.atualizarVersao(LOGIN, 42L);

        verify(datasetService, never()).atualizarSchema(any(), any(), any());
    }

    @Test
    void instalar_validaWorkspaceDoUsuario() {
        stubAcesso();
        when(workspaceService.findOwnedWorkspace(USUARIO_ID, 1L))
            .thenThrow(new br.com.techne.sistemafolha.workspace.domain.WorkspaceNotFoundException(1L));

        assertThrows(br.com.techne.sistemafolha.workspace.domain.WorkspaceNotFoundException.class,
            () -> service.instalar(LOGIN, 5L, 1L));
    }

    @Test
    void atualizarVersao_semDatasetId_naoChamaAtualizarSchema() {
        stubAcesso();
        WorkspaceTemplateInstallation installation = new WorkspaceTemplateInstallation();
        installation.setId(43L);
        installation.setTemplateId(5L);
        installation.setVersaoInstalada(1);
        when(installationRepository.findByIdAndUsuarioId(43L, USUARIO_ID))
            .thenReturn(Optional.of(installation));
        when(templatePublishService.findVisibleTemplate(LOGIN, 5L))
            .thenReturn(template(5L, TemplateTipo.DATASET));
        WorkspaceTemplateVersion v2 = datasetVersion();
        v2.setVersao(2);
        when(templatePublishService.findLatestVersion(5L)).thenReturn(v2);
        when(installationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.atualizarVersao(LOGIN, 43L);

        verify(datasetService, never()).atualizarSchema(any(), any(), any());
    }

    private void stubAcesso() {
        when(workspaceAccessGuard.resolve(LOGIN)).thenReturn(
            new WorkspaceAccessGuard.ResolvedWorkspaceAccess(false, USUARIO_ID, null, null));
        org.mockito.Mockito.doNothing().when(workspaceAccessGuard).assertEscopo(LOGIN);
    }

    private void stubWorkspace(Long id) {
        Workspace ws = new Workspace();
        ws.setId(id);
        when(workspaceService.findOwnedWorkspace(USUARIO_ID, id)).thenReturn(ws);
    }

    private WorkspaceTemplate template(Long id, TemplateTipo tipo) {
        WorkspaceTemplate t = new WorkspaceTemplate();
        t.setId(id);
        t.setTipo(tipo);
        t.setNome("Test");
        return t;
    }

    private WorkspaceTemplateVersion datasetVersion() {
        TemplateStructurePayload estrutura = new TemplateStructurePayload();
        estrutura.setKind("DATASET");
        estrutura.setNome("Vendas");
        estrutura.setSchema(new ArrayList<>(List.of(
            new br.com.techne.sistemafolha.workspace.domain.DatasetFieldSchema(
                "valor", DatasetFieldType.NUMERO, null, true))));
        WorkspaceTemplateVersion v = new WorkspaceTemplateVersion();
        v.setVersao(1);
        v.setEstrutura(estrutura);
        return v;
    }
}
