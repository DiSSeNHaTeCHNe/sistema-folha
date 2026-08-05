package br.com.techne.sistemafolha.workspace.application;

import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import br.com.techne.sistemafolha.workspace.domain.WorkspaceAcessoNegadoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class WorkspaceAccessGuard {

    private final UsuarioLookupPort usuarioLookupPort;
    private final OrganogramaAcessoPort organogramaAcessoPort;

    public ResolvedWorkspaceAccess resolve(String login) {
        if (login == null || login.isBlank()) {
            return ResolvedWorkspaceAccess.accessDenied();
        }
        var usuarioOpt = usuarioLookupPort.findByLoginAndAtivoTrue(login);
        if (usuarioOpt.isEmpty()) {
            return ResolvedWorkspaceAccess.accessDenied();
        }
        Long usuarioId = usuarioOpt.get().getId();
        AccessContextDTO contexto = organogramaAcessoPort.obterContextoAcesso(usuarioId);
        if (deveNegarAcesso(contexto)) {
            return ResolvedWorkspaceAccess.accessDenied();
        }
        Set<Long> centrosScoped = contexto.acessoTotal() ? null : contexto.centrosCustoIds();
        return new ResolvedWorkspaceAccess(false, usuarioId, contexto, centrosScoped);
    }

    public void assertEscopo(String login) {
        if (resolve(login).denied()) {
            throw new WorkspaceAcessoNegadoException();
        }
    }

    public boolean deveNegarAcesso(AccessContextDTO contexto) {
        if (contexto.acessoTotal()) {
            return false;
        }
        if (contexto.motivoNegacao() != null) {
            return true;
        }
        if (!contexto.temFuncionarioVinculado() || !contexto.temNoOrganograma()) {
            return true;
        }
        return contexto.centrosCustoIds() == null || contexto.centrosCustoIds().isEmpty();
    }

    public record ResolvedWorkspaceAccess(
        boolean denied,
        Long usuarioId,
        AccessContextDTO contexto,
        Set<Long> centrosScoped
    ) {
        static ResolvedWorkspaceAccess accessDenied() {
            return new ResolvedWorkspaceAccess(true, null, null, null);
        }
    }

    /**
     * WKS-16: template visível para usuários na subárvore do nó do publicador (ou acesso total).
     */
    public boolean podeVerTemplate(AccessContextDTO viewerContext, Long publicadorUsuarioId,
                                   Long templateOrganogramaNoId) {
        if (viewerContext.acessoTotal()) {
            return true;
        }
        if (deveNegarAcesso(viewerContext)) {
            return false;
        }
        if (templateOrganogramaNoId == null) {
            return false;
        }
        Long viewerNoId = viewerContext.noOrganogramaId();
        if (viewerNoId == null) {
            return false;
        }
        return organogramaAcessoPort.noEstaNaSubarvore(viewerNoId, templateOrganogramaNoId);
    }

    public void assertPodeVerTemplate(AccessContextDTO viewerContext, Long publicadorUsuarioId,
                                      Long templateOrganogramaNoId) {
        if (!podeVerTemplate(viewerContext, publicadorUsuarioId, templateOrganogramaNoId)) {
            throw new WorkspaceAcessoNegadoException();
        }
    }
}
