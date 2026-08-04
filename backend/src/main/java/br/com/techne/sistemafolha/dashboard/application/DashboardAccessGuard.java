package br.com.techne.sistemafolha.dashboard.application;

import br.com.techne.sistemafolha.auth.port.UsuarioLookupPort;
import br.com.techne.sistemafolha.dashboard.domain.DashboardAcessoNegadoException;
import br.com.techne.sistemafolha.organograma.acesso.port.AccessContextDTO;
import br.com.techne.sistemafolha.organograma.acesso.port.OrganogramaAcessoPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class DashboardAccessGuard {

    private final UsuarioLookupPort usuarioLookupPort;
    private final OrganogramaAcessoPort organogramaAcessoPort;

    public ResolvedDashboardAccess resolve(String login) {
        if (login == null || login.isBlank()) {
            return ResolvedDashboardAccess.accessDenied();
        }
        var usuarioOpt = usuarioLookupPort.findByLoginAndAtivoTrue(login);
        if (usuarioOpt.isEmpty()) {
            return ResolvedDashboardAccess.accessDenied();
        }
        Long usuarioId = usuarioOpt.get().getId();
        AccessContextDTO contexto = organogramaAcessoPort.obterContextoAcesso(usuarioId);
        if (deveNegarAcesso(contexto)) {
            return ResolvedDashboardAccess.accessDenied();
        }
        Set<Long> centrosScoped = contexto.acessoTotal() ? null : contexto.centrosCustoIds();
        return new ResolvedDashboardAccess(false, usuarioId, contexto, centrosScoped);
    }

    public void assertEscopo(String login) {
        if (resolve(login).denied()) {
            throw new DashboardAcessoNegadoException();
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

    public record ResolvedDashboardAccess(
        boolean denied,
        Long usuarioId,
        AccessContextDTO contexto,
        Set<Long> centrosScoped
    ) {
        static ResolvedDashboardAccess accessDenied() {
            return new ResolvedDashboardAccess(true, null, null, null);
        }
    }
}
