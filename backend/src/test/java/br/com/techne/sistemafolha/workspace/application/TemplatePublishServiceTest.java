package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.MotivoNegacaoAcesso;
import br.com.techne.sistemafolha.workspace.api.PublishTemplateRequest;
import br.com.techne.sistemafolha.workspace.api.WidgetDefinitionDTO;
import br.com.techne.sistemafolha.workspace.domain.DatasetFieldSchema;
import br.com.techne.sistemafolha.workspace.domain.DatasetFieldType;
import br.com.techne.sistemafolha.workspace.domain.TemplateTipo;
import br.com.techne.sistemafolha.workspace.domain.WidgetSourceKind;
import br.com.techne.sistemafolha.workspace.domain.WidgetSourceRef;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDataset;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceTemplate;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceTemplateInstallation;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceTemplateVersion;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceTemplateInstallationRepository;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceTemplateRepository;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceTemplateVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplatePublishServiceTest {

    private static final String LOGIN = "gestor";
    private static final Long USUARIO_ID = 10L;
    private static final Long NO_ORGANOGRAMA = 5L;

    @Mock private WorkspaceAccessGuard workspaceAccessGuard;
    @Mock private DatasetService datasetService;
    @Mock private WidgetDefinitionService widgetDefinitionService;
    @Mock private WorkspaceTemplateRepository templateRepository;
    @Mock private WorkspaceTemplateVersionRepository versionRepository;
    @Mock private WorkspaceTemplateInstallationRepository installationRepository;

    private TemplatePublishService service;

    @BeforeEach
    void setUp() {
        service = new TemplatePublishService(
            workspaceAccessGuard,
            datasetService,
            widgetDefinitionService,
            templateRepository,
            versionRepository,
            installationRepository,
            new TemplateStructureHasher()
        );
    }

    @Test
    void publicarDataset_extraiSomenteSchema_semDadosDeLinha() {
        stubAcesso();
        WorkspaceDataset dataset = sampleDataset();
        when(datasetService.findOwnedDataset(USUARIO_ID, 1L)).thenReturn(dataset);
        when(templateRepository.findByPublicadorUsuarioIdAndAtivoTrueOrderByNomeAsc(USUARIO_ID))
            .thenReturn(List.of());
        when(templateRepository.save(any())).thenAnswer(inv -> {
            WorkspaceTemplate t = inv.getArgument(0);
            t.setId(100L);
            return t;
        });
        when(versionRepository.findByTemplateIdAndEstruturaHash(eq(100L), any())).thenReturn(Optional.empty());
        when(versionRepository.findFirstByTemplateIdOrderByVersaoDesc(100L)).thenReturn(Optional.empty());
        when(versionRepository.save(any())).thenAnswer(inv -> {
            WorkspaceTemplateVersion v = inv.getArgument(0);
            v.setId(200L);
            v.setDataPublicacao(LocalDateTime.now());
            return v;
        });

        var result = service.publicar(LOGIN, new PublishTemplateRequest(1L, null));

        ArgumentCaptor<WorkspaceTemplateVersion> captor = ArgumentCaptor.forClass(WorkspaceTemplateVersion.class);
        verify(versionRepository).save(captor.capture());
        assertEquals("DATASET", captor.getValue().getEstrutura().getKind());
        assertEquals(1, captor.getValue().getEstrutura().getSchema().size());
        assertTrue(result.novaVersaoCriada());
        assertEquals(TemplateTipo.DATASET, result.tipo());
    }

    @Test
    void publicarWidget_extraiDefinicaoSemDados() {
        stubAcesso();
        when(widgetDefinitionService.obter(LOGIN, 20L)).thenReturn(sampleWidget());
        when(templateRepository.findByPublicadorUsuarioIdAndAtivoTrueOrderByNomeAsc(USUARIO_ID))
            .thenReturn(List.of());
        when(templateRepository.save(any())).thenAnswer(inv -> {
            WorkspaceTemplate t = inv.getArgument(0);
            t.setId(101L);
            return t;
        });
        when(versionRepository.findByTemplateIdAndEstruturaHash(eq(101L), any())).thenReturn(Optional.empty());
        when(versionRepository.findFirstByTemplateIdOrderByVersaoDesc(101L)).thenReturn(Optional.empty());
        when(versionRepository.save(any())).thenAnswer(inv -> {
            WorkspaceTemplateVersion v = inv.getArgument(0);
            v.setId(201L);
            v.setDataPublicacao(LocalDateTime.now());
            return v;
        });

         var result = service.publicar(LOGIN, new PublishTemplateRequest(null, 20L));

        assertEquals(TemplateTipo.WIDGET, result.tipo());
        assertTrue(result.novaVersaoCriada());
    }

    @Test
    void publicar_hashDuplicado_noOpSemNovaVersao() {
        stubAcesso();
        WorkspaceDataset dataset = sampleDataset();
        when(datasetService.findOwnedDataset(USUARIO_ID, 1L)).thenReturn(dataset);
        WorkspaceTemplate existing = template(100L);
        when(templateRepository.findByPublicadorUsuarioIdAndAtivoTrueOrderByNomeAsc(USUARIO_ID))
            .thenReturn(List.of(existing));
        WorkspaceTemplateVersion sameHash = version(1, "abc123");
        when(versionRepository.findByTemplateIdAndEstruturaHash(eq(100L), any()))
            .thenReturn(Optional.of(sameHash));

        var result = service.publicar(LOGIN, new PublishTemplateRequest(1L, null));

        verify(versionRepository, never()).save(any());
        assertFalse(result.novaVersaoCriada());
        assertEquals(1, result.versaoAtual());
    }

    @Test
    void publicar_alteracaoEstrutura_incrementaVersao() {
        stubAcesso();
        when(datasetService.findOwnedDataset(USUARIO_ID, 1L)).thenReturn(sampleDataset());
        WorkspaceTemplate existing = template(100L);
        when(templateRepository.findByPublicadorUsuarioIdAndAtivoTrueOrderByNomeAsc(USUARIO_ID))
            .thenReturn(List.of(existing));
        when(versionRepository.findByTemplateIdAndEstruturaHash(eq(100L), any())).thenReturn(Optional.empty());
        when(versionRepository.findFirstByTemplateIdOrderByVersaoDesc(100L))
            .thenReturn(Optional.of(version(1, "old")));
        when(versionRepository.save(any())).thenAnswer(inv -> {
            WorkspaceTemplateVersion v = inv.getArgument(0);
            v.setId(300L);
            v.setDataPublicacao(LocalDateTime.now());
            return v;
        });

        var result = service.publicar(LOGIN, new PublishTemplateRequest(1L, null));

        assertTrue(result.novaVersaoCriada());
        assertEquals(2, result.versaoAtual());
    }

    @Test
    void listarCatalogo_usuarioNaHierarquia_veTemplate() {
        stubAcesso();
        WorkspaceTemplate template = template(50L);
        template.setOrganogramaNoId(NO_ORGANOGRAMA);
        when(templateRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(template));
        when(versionRepository.findFirstByTemplateIdOrderByVersaoDesc(50L))
            .thenReturn(Optional.of(version(1, "h1")));
        when(installationRepository.findByUsuarioIdAndTemplateId(USUARIO_ID, 50L))
            .thenReturn(Optional.empty());
        when(workspaceAccessGuard.podeVerTemplate(any(), eq(USUARIO_ID), eq(NO_ORGANOGRAMA)))
            .thenReturn(true);

        var items = service.listarCatalogo(LOGIN);

        assertEquals(2, items.size());
        assertEquals(TemplatePublishService.NATIVE_ORCAMENTO_PADRAO_TEMPLATE_ID, items.get(0).id());
        assertEquals(OrcamentoTemplateInstaller.DATASET_NOME, items.get(0).nome());
        assertEquals(TemplateTipo.PACOTE, items.get(0).tipo());
        assertEquals(50L, items.get(1).id());
    }

    @Test
    void listarCatalogo_semTemplatesDb_incluiOrcamentoNativo() {
        stubAcesso();
        when(templateRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of());

        var items = service.listarCatalogo(LOGIN);

        assertEquals(1, items.size());
        assertEquals(TemplatePublishService.NATIVE_ORCAMENTO_PADRAO_TEMPLATE_ID, items.get(0).id());
        assertEquals(OrcamentoTemplateInstaller.DATASET_NOME, items.get(0).nome());
        assertEquals(TemplatePublishService.NATIVE_ORCAMENTO_PADRAO_SLUG, "orcamento-padrao");
    }

    @Test
    void listarCatalogo_usuarioForaHierarquia_naoVeTemplate() {
        stubAcesso();
        WorkspaceTemplate template = template(51L);
        template.setOrganogramaNoId(99L);
        when(templateRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(template));
        when(workspaceAccessGuard.podeVerTemplate(any(), eq(USUARIO_ID), eq(99L)))
            .thenReturn(false);

        var items = service.listarCatalogo(LOGIN);

        assertEquals(1, items.size());
        assertEquals(TemplatePublishService.NATIVE_ORCAMENTO_PADRAO_TEMPLATE_ID, items.get(0).id());
    }

    @Test
    void listarCatalogo_comInstalacao_indicaAtualizacaoDisponivel() {
        stubAcesso();
        WorkspaceTemplate template = template(52L);
        template.setOrganogramaNoId(NO_ORGANOGRAMA);
        when(templateRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(template));
        when(versionRepository.findFirstByTemplateIdOrderByVersaoDesc(52L))
            .thenReturn(Optional.of(version(2, "h2")));
        WorkspaceTemplateInstallation inst = new WorkspaceTemplateInstallation();
        inst.setId(7L);
        inst.setVersaoInstalada(1);
        when(installationRepository.findByUsuarioIdAndTemplateId(USUARIO_ID, 52L))
            .thenReturn(Optional.of(inst));
        when(workspaceAccessGuard.podeVerTemplate(any(), eq(USUARIO_ID), eq(NO_ORGANOGRAMA)))
            .thenReturn(true);

        var items = service.listarCatalogo(LOGIN);

        assertEquals(2, items.size());
        assertTrue(items.get(1).atualizacaoDisponivel());
        assertEquals(1, items.get(1).versaoInstalada());
    }

    @Test
    void listarCatalogo_templateSemVersao_omitido() {
        stubAcesso();
        WorkspaceTemplate template = template(53L);
        when(templateRepository.findByAtivoTrueOrderByNomeAsc()).thenReturn(List.of(template));
        when(versionRepository.findFirstByTemplateIdOrderByVersaoDesc(53L)).thenReturn(Optional.empty());
        when(workspaceAccessGuard.podeVerTemplate(any(), any(), any())).thenReturn(true);

        var items = service.listarCatalogo(LOGIN);

        assertEquals(1, items.size());
        assertEquals(TemplatePublishService.NATIVE_ORCAMENTO_PADRAO_TEMPLATE_ID, items.get(0).id());
    }

    @Test
    void findVisibleTemplate_foraHierarquia_lanca403() {
        stubAcesso();
        WorkspaceTemplate template = template(60L);
        template.setOrganogramaNoId(88L);
        when(templateRepository.findByIdAndAtivoTrue(60L)).thenReturn(Optional.of(template));
        org.mockito.Mockito.doThrow(new br.com.techne.sistemafolha.workspace.domain.WorkspaceAcessoNegadoException())
            .when(workspaceAccessGuard)
            .assertPodeVerTemplate(any(), eq(USUARIO_ID), eq(88L));

        org.junit.jupiter.api.Assertions.assertThrows(
            br.com.techne.sistemafolha.workspace.domain.WorkspaceAcessoNegadoException.class,
            () -> service.findVisibleTemplate(LOGIN, 60L));
    }

    @Test
    void publicar_reutilizaTemplateExistenteMesmoNomeETipo() {
        stubAcesso();
        when(datasetService.findOwnedDataset(USUARIO_ID, 1L)).thenReturn(sampleDataset());
        WorkspaceTemplate existing = template(100L);
        existing.setNome("Vendas");
        existing.setTipo(TemplateTipo.DATASET);
        when(templateRepository.findByPublicadorUsuarioIdAndAtivoTrueOrderByNomeAsc(USUARIO_ID))
            .thenReturn(List.of(existing));
        when(versionRepository.findByTemplateIdAndEstruturaHash(eq(100L), any())).thenReturn(Optional.empty());
        when(versionRepository.findFirstByTemplateIdOrderByVersaoDesc(100L)).thenReturn(Optional.empty());
        when(versionRepository.save(any())).thenAnswer(inv -> {
            WorkspaceTemplateVersion v = inv.getArgument(0);
            v.setId(400L);
            v.setDataPublicacao(LocalDateTime.now());
            return v;
        });

        var result = service.publicar(LOGIN, new PublishTemplateRequest(1L, null));

        verify(templateRepository, never()).save(any());
        assertEquals(100L, result.id());
    }

    @Test
    void publicar_defineOrganogramaNoIdDoPublicador() {
        stubAcesso();
        when(datasetService.findOwnedDataset(USUARIO_ID, 1L)).thenReturn(sampleDataset());
        when(templateRepository.findByPublicadorUsuarioIdAndAtivoTrueOrderByNomeAsc(USUARIO_ID))
            .thenReturn(List.of());
        when(templateRepository.save(any())).thenAnswer(inv -> {
            WorkspaceTemplate t = inv.getArgument(0);
            assertEquals(NO_ORGANOGRAMA, t.getOrganogramaNoId());
            t.setId(102L);
            return t;
        });
        when(versionRepository.findByTemplateIdAndEstruturaHash(eq(102L), any())).thenReturn(Optional.empty());
        when(versionRepository.findFirstByTemplateIdOrderByVersaoDesc(102L)).thenReturn(Optional.empty());
        when(versionRepository.save(any())).thenAnswer(inv -> {
            WorkspaceTemplateVersion v = inv.getArgument(0);
            v.setId(402L);
            v.setDataPublicacao(LocalDateTime.now());
            return v;
        });

        service.publicar(LOGIN, new PublishTemplateRequest(1L, null));
        verify(templateRepository).save(any());
    }

    @Test
    void publicar_primeiraVersao_numero1() {
        stubAcesso();
        when(datasetService.findOwnedDataset(USUARIO_ID, 1L)).thenReturn(sampleDataset());
        when(templateRepository.findByPublicadorUsuarioIdAndAtivoTrueOrderByNomeAsc(USUARIO_ID))
            .thenReturn(List.of());
        when(templateRepository.save(any())).thenAnswer(inv -> {
            WorkspaceTemplate t = inv.getArgument(0);
            t.setId(103L);
            return t;
        });
        when(versionRepository.findByTemplateIdAndEstruturaHash(eq(103L), any())).thenReturn(Optional.empty());
        when(versionRepository.findFirstByTemplateIdOrderByVersaoDesc(103L)).thenReturn(Optional.empty());
        when(versionRepository.save(any())).thenAnswer(inv -> {
            WorkspaceTemplateVersion v = inv.getArgument(0);
            assertEquals(1, v.getVersao());
            v.setId(403L);
            v.setDataPublicacao(LocalDateTime.now());
            return v;
        });

        var result = service.publicar(LOGIN, new PublishTemplateRequest(1L, null));
        assertEquals(1, result.versaoAtual());
    }

    private void stubAcesso() {
        AccessContextDTO contexto = new AccessContextDTO(
            true, true, false, Set.of(1L), null, NO_ORGANOGRAMA, "Dir", 1);
        when(workspaceAccessGuard.resolve(LOGIN)).thenReturn(
            new WorkspaceAccessGuard.ResolvedWorkspaceAccess(false, USUARIO_ID, contexto, Set.of(1L)));
        org.mockito.Mockito.doNothing().when(workspaceAccessGuard).assertEscopo(LOGIN);
    }

    private WorkspaceDataset sampleDataset() {
        WorkspaceDataset ds = new WorkspaceDataset();
        ds.setId(1L);
        ds.setNome("Vendas");
        ds.setSchema(new ArrayList<>(List.of(
            new DatasetFieldSchema("valor", DatasetFieldType.NUMERO, null, true))));
        return ds;
    }

    private WidgetDefinitionDTO sampleWidget() {
        return new WidgetDefinitionDTO(
            20L, "KPI Vendas", "KPI",
            List.of(new WidgetSourceRef(WidgetSourceKind.SISTEMA, "folha.custo")),
            "SOMA(custo)", Map.of(), false);
    }

    private WorkspaceTemplate template(Long id) {
        WorkspaceTemplate t = new WorkspaceTemplate();
        t.setId(id);
        t.setPublicadorUsuarioId(USUARIO_ID);
        t.setNome("Vendas");
        t.setTipo(TemplateTipo.DATASET);
        t.setAtivo(true);
        return t;
    }

    private WorkspaceTemplateVersion version(int num, String hash) {
        WorkspaceTemplateVersion v = new WorkspaceTemplateVersion();
        v.setVersao(num);
        v.setEstruturaHash(hash);
        v.setDataPublicacao(LocalDateTime.now());
        return v;
    }
}
