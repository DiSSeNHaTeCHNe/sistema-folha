package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.workspace.api.ConfirmProposalRequest;
import br.com.techne.sistemafolha.workspace.api.CreateDatasetRequest;
import br.com.techne.sistemafolha.workspace.api.CreateProposalRequest;
import br.com.techne.sistemafolha.workspace.api.CreateWidgetDefinitionRequest;
import br.com.techne.sistemafolha.workspace.api.DatasetFieldSchemaDTO;
import br.com.techne.sistemafolha.workspace.api.ProposalDTO;
import br.com.techne.sistemafolha.workspace.api.TemplateCatalogItemDTO;
import br.com.techne.sistemafolha.workspace.domain.ProposalPayload;
import br.com.techne.sistemafolha.workspace.domain.ProposalStatus;
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
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WorkspaceProposalService {

    private final WorkspaceAccessGuard workspaceAccessGuard;
    private final UsuarioLookupPort usuarioLookupPort;
    private final WorkspaceIaProposalRepository proposalRepository;
    private final TemplatePublishService templatePublishService;
    private final DatasetService datasetService;
    private final WidgetDefinitionService widgetDefinitionService;
    private final TemplateInstallService templateInstallService;
    private final ProposalContentBuilder contentBuilder;
    private final WorkspaceDatasetRepository datasetRepository;
    private final Clock clock;

    @Transactional
    public ProposalDTO criarProposta(String login, CreateProposalRequest request) {
        assertPermissaoIa(login);
        workspaceAccessGuard.assertEscopo(login);
        Long usuarioId = workspaceAccessGuard.resolve(login).usuarioId();

        assertQuotaPendente(usuarioId);

        ProposalPayload payload = montarPayload(login, request);
        Optional<WorkspaceIaProposal> existente = proposalRepository.findPendingByDedupHash(
            usuarioId, ProposalStatus.PENDENTE.name(), payload.getDedupHash());
        if (existente.isPresent()) {
            return toDto(existente.get());
        }

        WorkspaceIaProposal proposal = new WorkspaceIaProposal();
        proposal.setSolicitanteUsuarioId(usuarioId);
        proposal.setStatus(ProposalStatus.PENDENTE);
        proposal.setPayload(payload);
        proposal.setDataCriacao(LocalDateTime.now(clock));
        proposal.setDataExpiracao(
            LocalDateTime.now(clock).plusHours(WorkspaceLimits.IA_PROPOSAL_TTL_HOURS));

        return toDto(proposalRepository.save(proposal));
    }

    @Transactional(readOnly = true)
    public ProposalDTO obter(String login, Long proposalId) {
        workspaceAccessGuard.assertEscopo(login);
        Long usuarioId = workspaceAccessGuard.resolve(login).usuarioId();
        WorkspaceIaProposal proposal = findOwned(usuarioId, proposalId);
        assertNaoExpirada(proposal);
        return toDto(proposal);
    }

    @Transactional
    public ProposalDTO confirmar(String login, Long proposalId, ConfirmProposalRequest ajustes) {
        workspaceAccessGuard.assertEscopo(login);
        Long usuarioId = workspaceAccessGuard.resolve(login).usuarioId();
        WorkspaceIaProposal proposal = findOwned(usuarioId, proposalId);
        assertPendente(proposal);
        assertNaoExpirada(proposal);

        aplicarConfirmacao(login, proposal, ajustes);

        proposal.setStatus(ProposalStatus.APLICADA);
        proposal.setDataResolucao(LocalDateTime.now(clock));
        return toDto(proposalRepository.save(proposal));
    }

    @Transactional
    public void descartar(String login, Long proposalId) {
        workspaceAccessGuard.assertEscopo(login);
        Long usuarioId = workspaceAccessGuard.resolve(login).usuarioId();
        WorkspaceIaProposal proposal = findOwned(usuarioId, proposalId);
        assertPendente(proposal);

        proposal.setStatus(ProposalStatus.DESCARTADA);
        proposal.setDataResolucao(LocalDateTime.now(clock));
        proposalRepository.save(proposal);
    }

    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void expirarPropostasPendentes() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<WorkspaceIaProposal> expiradas =
            proposalRepository.findByStatusAndDataExpiracaoBefore(ProposalStatus.PENDENTE, now);
        for (WorkspaceIaProposal proposal : expiradas) {
            proposal.setStatus(ProposalStatus.EXPIRADA);
            proposal.setDataResolucao(now);
        }
        if (!expiradas.isEmpty()) {
            proposalRepository.saveAll(expiradas);
        }
    }

    private ProposalPayload montarPayload(String login, CreateProposalRequest request) {
        List<TemplateCatalogItemDTO> catalogo = templatePublishService.listarCatalogo(login);
        if ("SUGESTAO".equalsIgnoreCase(request.tipo())) {
            List<String> nomesDatasets = datasetRepository
                .findByUsuarioIdOrderByNomeAsc(workspaceAccessGuard.resolve(login).usuarioId())
                .stream().map(d -> d.getNome()).toList();
            return contentBuilder.montarSugestao(nomesDatasets, catalogo);
        }

        String descricao = request.descricaoNatural();
        Optional<TemplateCatalogItemDTO> similar = contentBuilder.buscarTemplateSimilar(descricao, catalogo);
        if (similar.isPresent()) {
            return contentBuilder.montarInstalacaoTemplate(similar.get(), descricao);
        }
        return contentBuilder.montarDeDescricao(request.tipo(), descricao);
    }

    private void aplicarConfirmacao(String login, WorkspaceIaProposal proposal, ConfirmProposalRequest ajustes) {
        ProposalPayload payload = proposal.getPayload();
        ConfirmProposalRequest effective = ajustes != null ? ajustes : emptyAjustes();

        switch (payload.getKind()) {
            case "DATASET" -> {
                String nome = coalesce(effective.nome(), payload.getNome());
                List<DatasetFieldSchemaDTO> campos = effective.campos() != null && !effective.campos().isEmpty()
                    ? effective.campos()
                    : contentBuilder.toFieldDtos(payload.getCampos());
                datasetService.criar(login, new CreateDatasetRequest(nome, campos));
            }
            case "WIDGET" -> {
                widgetDefinitionService.criar(login, new CreateWidgetDefinitionRequest(
                    coalesce(effective.nome(), payload.getNome()),
                    coalesce(effective.tipoWidget(), payload.getTipoWidget()),
                    effective.fontes() != null && !effective.fontes().isEmpty()
                        ? effective.fontes() : payload.getFontes(),
                    coalesce(effective.formula(), payload.getFormula()),
                    effective.config() != null ? effective.config() : payload.getConfig()
                ));
            }
            case "TEMPLATE_INSTALL" -> {
                Long workspaceId = effective.workspaceId() != null
                    ? effective.workspaceId() : payload.getWorkspaceId();
                if (workspaceId == null) {
                    throw new IllegalArgumentException("workspaceId é obrigatório para instalar template");
                }
                templateInstallService.instalar(login, payload.getTemplateId(), workspaceId);
            }
            default -> throw new IllegalArgumentException("Tipo de proposta não suportado: " + payload.getKind());
        }
    }

    private void assertPermissaoIa(String login) {
        var usuario = usuarioLookupPort.findByLoginAndAtivoTrue(login).orElseThrow(WorkspaceIaPermissaoNegadaException::new);
        List<String> permissoes = usuario.getPermissoes();
        if (permissoes == null || !permissoes.contains(WorkspacePermissions.WORKSPACE_IA_CRIAR)) {
            throw new WorkspaceIaPermissaoNegadaException();
        }
    }

    private void assertQuotaPendente(Long usuarioId) {
        long pendentes = proposalRepository.countBySolicitanteUsuarioIdAndStatus(
            usuarioId, ProposalStatus.PENDENTE);
        if (pendentes >= WorkspaceLimits.MAX_PENDING_IA_PROPOSALS) {
            throw new WorkspaceQuotaExceededException(
                "Limite de propostas pendentes atingido (" + pendentes + "/"
                    + WorkspaceLimits.MAX_PENDING_IA_PROPOSALS + ")");
        }
    }

    private WorkspaceIaProposal findOwned(Long usuarioId, Long proposalId) {
        return proposalRepository.findByIdAndSolicitanteUsuarioId(proposalId, usuarioId)
            .orElseThrow(() -> new WorkspaceProposalNotFoundException(proposalId));
    }

    private void assertPendente(WorkspaceIaProposal proposal) {
        if (proposal.getStatus() != ProposalStatus.PENDENTE) {
            throw new WorkspaceProposalAlreadyAppliedException();
        }
    }

    private void assertNaoExpirada(WorkspaceIaProposal proposal) {
        if (proposal.getStatus() == ProposalStatus.PENDENTE
            && proposal.getDataExpiracao().isBefore(LocalDateTime.now(clock))) {
            proposal.setStatus(ProposalStatus.EXPIRADA);
            proposal.setDataResolucao(LocalDateTime.now(clock));
            proposalRepository.save(proposal);
            throw new WorkspaceProposalExpiredException();
        }
        if (proposal.getStatus() == ProposalStatus.EXPIRADA) {
            throw new WorkspaceProposalExpiredException();
        }
    }

    private ConfirmProposalRequest emptyAjustes() {
        return new ConfirmProposalRequest(null, null, null, null, null, null, null);
    }

    private String coalesce(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }

    private ProposalDTO toDto(WorkspaceIaProposal proposal) {
        return new ProposalDTO(
            proposal.getId(),
            proposal.getStatus(),
            proposal.getPayload(),
            proposal.getSolicitanteUsuarioId(),
            proposal.getDataCriacao(),
            proposal.getDataExpiracao(),
            proposal.getDataResolucao()
        );
    }
}
