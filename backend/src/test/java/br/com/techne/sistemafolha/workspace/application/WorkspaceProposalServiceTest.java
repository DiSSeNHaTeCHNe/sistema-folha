package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.auth.domain.Usuario;
import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.workspace.api.ConfirmProposalRequest;
import br.com.techne.sistemafolha.workspace.api.CreateDatasetRequest;
import br.com.techne.sistemafolha.workspace.api.CreateProposalRequest;
import br.com.techne.sistemafolha.workspace.api.CreateWidgetDefinitionRequest;
import br.com.techne.sistemafolha.workspace.api.DatasetDTO;
import br.com.techne.sistemafolha.workspace.api.DatasetFieldSchemaDTO;
import br.com.techne.sistemafolha.workspace.api.ProposalDTO;
import br.com.techne.sistemafolha.workspace.api.TemplateCatalogItemDTO;
import br.com.techne.sistemafolha.workspace.api.TemplateInstallResultDTO;
import br.com.techne.sistemafolha.workspace.api.WidgetDefinitionDTO;
import br.com.techne.sistemafolha.workspace.domain.DatasetFieldType;
import br.com.techne.sistemafolha.workspace.domain.ProposalPayload;
import br.com.techne.sistemafolha.workspace.domain.ProposalStatus;
import br.com.techne.sistemafolha.workspace.domain.WidgetSourceKind;
import br.com.techne.sistemafolha.workspace.domain.WidgetSourceRef;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceDataset;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceIaPermissaoNegadaException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceIaProposal;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceLimits;
import br.com.techne.sistemafolha.workspace.domain.WorkspacePermissions;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceProposalAlreadyAppliedException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceProposalExpiredException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceProposalNotFoundException;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceQuotaExceededException;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceDatasetRepository;
import br.com.techne.sistemafolha.workspace.infrastructure.WorkspaceIaProposalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceProposalServiceTest {

    private static final String LOGIN = "gestor";
    private static final Long USUARIO_ID = 10L;
    private static final Instant FIXED = Instant.parse("2026-08-05T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(FIXED, ZoneId.of("America/Sao_Paulo"));

    @Mock private WorkspaceAccessGuard workspaceAccessGuard;
    @Mock private UsuarioLookupPort usuarioLookupPort;
    @Mock private WorkspaceIaProposalRepository proposalRepository;
    @Mock private TemplatePublishService templatePublishService;
    @Mock private DatasetService datasetService;
    @Mock private WidgetDefinitionService widgetDefinitionService;
    @Mock private TemplateInstallService templateInstallService;
    @Mock private WorkspaceDatasetRepository datasetRepository;

    private ProposalContentBuilder contentBuilder;
    private WorkspaceProposalService service;

    @BeforeEach
    void setUp() {
        contentBuilder = new ProposalContentBuilder();
        service = new WorkspaceProposalService(
            workspaceAccessGuard,
            usuarioLookupPort,
            proposalRepository,
            templatePublishService,
            datasetService,
            widgetDefinitionService,
            templateInstallService,
            contentBuilder,
            datasetRepository,
            CLOCK
        );
    }

    @Test
    void criarProposta_semPermissaoIa_lanca403() {
        stubUsuarioSemPermissaoIa();

        assertThrows(WorkspaceIaPermissaoNegadaException.class,
            () -> service.criarProposta(LOGIN, new CreateProposalRequest("DATASET", "dataset teste")));
    }

    @Test
    void criarProposta_datasetPersisteSomentePendente_naoChamaDatasetService() {
        stubPermissaoEAcesso();
        when(proposalRepository.countBySolicitanteUsuarioIdAndStatus(USUARIO_ID, ProposalStatus.PENDENTE)).thenReturn(0L);
        when(templatePublishService.listarCatalogo(LOGIN)).thenReturn(List.of());
        when(proposalRepository.findPendingByDedupHash(eq(USUARIO_ID), eq("PENDENTE"), any())).thenReturn(Optional.empty());
        when(proposalRepository.save(any())).thenAnswer(inv -> {
            WorkspaceIaProposal p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProposalDTO result = service.criarProposta(LOGIN,
            new CreateProposalRequest("DATASET", "previsão de contratações com competencia e cargo"));

        assertEquals(ProposalStatus.PENDENTE, result.status());
        assertEquals("DATASET", result.payload().getKind());
        assertTrue(result.payload().getCampos().stream().anyMatch(c -> "competencia".equals(c.nome())));
        verify(datasetService, never()).criar(any(), any());
    }

    @Test
    void criarProposta_templateSimilar_sugereInstalacao() {
        stubPermissaoEAcesso();
        when(proposalRepository.countBySolicitanteUsuarioIdAndStatus(USUARIO_ID, ProposalStatus.PENDENTE)).thenReturn(0L);
        when(templatePublishService.listarCatalogo(LOGIN)).thenReturn(List.of(
            new TemplateCatalogItemDTO(5L, "Orçamento CC", br.com.techne.sistemafolha.workspace.domain.TemplateTipo.DATASET,
                1, 1, false, 2L, null, null)
        ));
        when(proposalRepository.findPendingByDedupHash(eq(USUARIO_ID), eq("PENDENTE"), any())).thenReturn(Optional.empty());
        when(proposalRepository.save(any())).thenAnswer(inv -> {
            WorkspaceIaProposal p = inv.getArgument(0);
            p.setId(2L);
            return p;
        });

        ProposalDTO result = service.criarProposta(LOGIN,
            new CreateProposalRequest("DATASET", "crie um orçamento por centro de custo"));

        assertEquals("TEMPLATE_INSTALL", result.payload().getKind());
        assertEquals(5L, result.payload().getTemplateId());
    }

    @Test
    void criarProposta_dedupRetornaExistente() {
        stubPermissaoEAcesso();
        when(proposalRepository.countBySolicitanteUsuarioIdAndStatus(USUARIO_ID, ProposalStatus.PENDENTE)).thenReturn(1L);
        when(templatePublishService.listarCatalogo(LOGIN)).thenReturn(List.of());
        WorkspaceIaProposal existente = pendingProposal(99L, contentBuilder.montarDeDescricao("DATASET", "teste"));
        when(proposalRepository.findPendingByDedupHash(eq(USUARIO_ID), eq("PENDENTE"), any()))
            .thenReturn(Optional.of(existente));

        ProposalDTO result = service.criarProposta(LOGIN, new CreateProposalRequest("DATASET", "teste"));

        assertEquals(99L, result.id());
        verify(proposalRepository, never()).save(any());
    }

    @Test
    void criarProposta_quotaPendenteExcedida_lancaErro() {
        stubPermissaoEAcesso();
        when(proposalRepository.countBySolicitanteUsuarioIdAndStatus(USUARIO_ID, ProposalStatus.PENDENTE))
            .thenReturn((long) WorkspaceLimits.MAX_PENDING_IA_PROPOSALS);

        assertThrows(WorkspaceQuotaExceededException.class,
            () -> service.criarProposta(LOGIN, new CreateProposalRequest("DATASET", "novo")));
    }

    @Test
    void criarProposta_sugestaoAnalisaDatasetsExistentes() {
        stubPermissaoEAcesso();
        when(proposalRepository.countBySolicitanteUsuarioIdAndStatus(USUARIO_ID, ProposalStatus.PENDENTE)).thenReturn(0L);
        when(templatePublishService.listarCatalogo(LOGIN)).thenReturn(List.of());
        WorkspaceDataset ds = new WorkspaceDataset();
        ds.setNome("Orçamento mensal");
        when(datasetRepository.findByUsuarioIdOrderByNomeAsc(USUARIO_ID)).thenReturn(List.of(ds));
        when(proposalRepository.findPendingByDedupHash(eq(USUARIO_ID), eq("PENDENTE"), any())).thenReturn(Optional.empty());
        when(proposalRepository.save(any())).thenAnswer(inv -> {
            WorkspaceIaProposal p = inv.getArgument(0);
            p.setId(3L);
            return p;
        });

        ProposalDTO result = service.criarProposta(LOGIN, new CreateProposalRequest("SUGESTAO", null));

        assertEquals("WIDGET", result.payload().getKind());
        assertEquals("Variação mês a mês", result.payload().getNome());
    }

    @Test
    void confirmar_datasetPersisteViaDatasetService() {
        stubAcesso();
        ProposalPayload payload = contentBuilder.montarDeDescricao("DATASET", "planilha");
        WorkspaceIaProposal proposal = pendingProposal(7L, payload);
        when(proposalRepository.findByIdAndSolicitanteUsuarioId(7L, USUARIO_ID)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(datasetService.criar(eq(LOGIN), any())).thenReturn(
            new DatasetDTO(1L, "Planilha", List.of(), 1, 0));

        ProposalDTO result = service.confirmar(LOGIN, 7L, null);

        assertEquals(ProposalStatus.APLICADA, result.status());
        verify(datasetService).criar(eq(LOGIN), any(CreateDatasetRequest.class));
    }

    @Test
    void confirmar_widgetPersisteViaWidgetService() {
        stubAcesso();
        ProposalPayload payload = contentBuilder.montarDeDescricao("WIDGET", "kpi folha");
        WorkspaceIaProposal proposal = pendingProposal(8L, payload);
        when(proposalRepository.findByIdAndSolicitanteUsuarioId(8L, USUARIO_ID)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(widgetDefinitionService.criar(eq(LOGIN), any())).thenReturn(
            new WidgetDefinitionDTO(1L, "KPI", "KPI", List.of(), "SOMA(x)", Map.of(), false));

        service.confirmar(LOGIN, 8L, null);

        verify(widgetDefinitionService).criar(eq(LOGIN), any(CreateWidgetDefinitionRequest.class));
    }

    @Test
    void confirmar_templateInstall_chamaTemplateInstallService() {
        stubAcesso();
        ProposalPayload payload = contentBuilder.montarInstalacaoTemplate(
            new TemplateCatalogItemDTO(2L, "Tpl", br.com.techne.sistemafolha.workspace.domain.TemplateTipo.DATASET,
                1, 1, false, 1L, null, null),
            "instalar");
        WorkspaceIaProposal proposal = pendingProposal(9L, payload);
        when(proposalRepository.findByIdAndSolicitanteUsuarioId(9L, USUARIO_ID)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(templateInstallService.instalar(LOGIN, 2L, 4L))
            .thenReturn(new TemplateInstallResultDTO(10L, 2L, 1, 4L, 1L, List.of(), Map.of()));

        service.confirmar(LOGIN, 9L, new ConfirmProposalRequest(null, null, null, null, null, null, 4L));

        verify(templateInstallService).instalar(LOGIN, 2L, 4L);
    }

    @Test
    void confirmar_propostaJaAplicada_lancaErro() {
        stubAcesso();
        WorkspaceIaProposal proposal = pendingProposal(10L, contentBuilder.montarDeDescricao("DATASET", "x"));
        proposal.setStatus(ProposalStatus.APLICADA);
        when(proposalRepository.findByIdAndSolicitanteUsuarioId(10L, USUARIO_ID)).thenReturn(Optional.of(proposal));

        assertThrows(WorkspaceProposalAlreadyAppliedException.class,
            () -> service.confirmar(LOGIN, 10L, null));
    }

    @Test
    void confirmar_propostaExpirada_lanca410() {
        stubAcesso();
        WorkspaceIaProposal proposal = pendingProposal(11L, contentBuilder.montarDeDescricao("DATASET", "x"));
        proposal.setDataExpiracao(LocalDateTime.now(CLOCK).minusHours(1));
        when(proposalRepository.findByIdAndSolicitanteUsuarioId(11L, USUARIO_ID)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(WorkspaceProposalExpiredException.class,
            () -> service.confirmar(LOGIN, 11L, null));
    }

    @Test
    void descartar_marcaComoDescartada() {
        stubAcesso();
        WorkspaceIaProposal proposal = pendingProposal(12L, contentBuilder.montarDeDescricao("DATASET", "x"));
        when(proposalRepository.findByIdAndSolicitanteUsuarioId(12L, USUARIO_ID)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.descartar(LOGIN, 12L);

        ArgumentCaptor<WorkspaceIaProposal> captor = ArgumentCaptor.forClass(WorkspaceIaProposal.class);
        verify(proposalRepository).save(captor.capture());
        assertEquals(ProposalStatus.DESCARTADA, captor.getValue().getStatus());
    }

    @Test
    void descartar_naoEncontrada_lanca404() {
        stubAcesso();
        when(proposalRepository.findByIdAndSolicitanteUsuarioId(404L, USUARIO_ID)).thenReturn(Optional.empty());

        assertThrows(WorkspaceProposalNotFoundException.class, () -> service.descartar(LOGIN, 404L));
    }

    @Test
    void expirarPropostasPendentes_marcaExpiradas() {
        WorkspaceIaProposal expirada = pendingProposal(20L, contentBuilder.montarDeDescricao("DATASET", "old"));
        expirada.setDataExpiracao(LocalDateTime.now(CLOCK).minusHours(1));
        when(proposalRepository.findByStatusAndDataExpiracaoBefore(eq(ProposalStatus.PENDENTE), any()))
            .thenReturn(new ArrayList<>(List.of(expirada)));
        when(proposalRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        service.expirarPropostasPendentes();

        assertEquals(ProposalStatus.EXPIRADA, expirada.getStatus());
        verify(proposalRepository).saveAll(any());
    }

    @Test
    void criarProposta_registraAutoriaWks29() {
        stubPermissaoEAcesso();
        when(proposalRepository.countBySolicitanteUsuarioIdAndStatus(USUARIO_ID, ProposalStatus.PENDENTE)).thenReturn(0L);
        when(templatePublishService.listarCatalogo(LOGIN)).thenReturn(List.of());
        when(proposalRepository.findPendingByDedupHash(eq(USUARIO_ID), eq("PENDENTE"), any())).thenReturn(Optional.empty());
        when(proposalRepository.save(any())).thenAnswer(inv -> {
            WorkspaceIaProposal p = inv.getArgument(0);
            p.setId(30L);
            return p;
        });

        ProposalDTO result = service.criarProposta(LOGIN, new CreateProposalRequest("DATASET", "novo dataset"));

        assertEquals(USUARIO_ID, result.solicitanteUsuarioId());
        assertTrue(result.dataExpiracao().isAfter(result.dataCriacao()));
    }

    @Test
    void confirmar_comAjustes_aplicaOverrides() {
        stubAcesso();
        ProposalPayload payload = contentBuilder.montarDeDescricao("DATASET", "original");
        WorkspaceIaProposal proposal = pendingProposal(13L, payload);
        when(proposalRepository.findByIdAndSolicitanteUsuarioId(13L, USUARIO_ID)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(datasetService.criar(eq(LOGIN), any())).thenReturn(
            new DatasetDTO(2L, "Renomeado", List.of(), 1, 0));

        ConfirmProposalRequest ajustes = new ConfirmProposalRequest(
            "Renomeado",
            List.of(new DatasetFieldSchemaDTO("campo1", DatasetFieldType.TEXTO, null, true)),
            null, null, null, null, null);

        service.confirmar(LOGIN, 13L, ajustes);

        ArgumentCaptor<CreateDatasetRequest> captor = ArgumentCaptor.forClass(CreateDatasetRequest.class);
        verify(datasetService).criar(eq(LOGIN), captor.capture());
        assertEquals("Renomeado", captor.getValue().nome());
        assertEquals(1, captor.getValue().campos().size());
    }

    private void stubPermissaoEAcesso() {
        stubUsuarioComPermissaoIa();
        stubAcesso();
    }

    private void stubAcesso() {
        when(workspaceAccessGuard.resolve(LOGIN)).thenReturn(
            new WorkspaceAccessGuard.ResolvedWorkspaceAccess(false, USUARIO_ID, accessContext(), Set.of(1L)));
    }

    private void stubUsuarioComPermissaoIa() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setLogin(LOGIN);
        usuario.setPermissoes(List.of(WorkspacePermissions.WORKSPACE_IA_CRIAR));
        when(usuarioLookupPort.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
    }

    private void stubUsuarioSemPermissaoIa() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setLogin(LOGIN);
        usuario.setPermissoes(List.of("USER"));
        when(usuarioLookupPort.findByLoginAndAtivoTrue(LOGIN)).thenReturn(Optional.of(usuario));
    }

    private AccessContextDTO accessContext() {
        return new AccessContextDTO(true, false, false, Set.of(1L), null, 1L, "Nó", 1);
    }

    private WorkspaceIaProposal pendingProposal(Long id, ProposalPayload payload) {
        WorkspaceIaProposal proposal = new WorkspaceIaProposal();
        proposal.setId(id);
        proposal.setSolicitanteUsuarioId(USUARIO_ID);
        proposal.setStatus(ProposalStatus.PENDENTE);
        proposal.setPayload(payload);
        proposal.setDataCriacao(LocalDateTime.now(CLOCK));
        proposal.setDataExpiracao(LocalDateTime.now(CLOCK).plusHours(WorkspaceLimits.IA_PROPOSAL_TTL_HOURS));
        return proposal;
    }
}
